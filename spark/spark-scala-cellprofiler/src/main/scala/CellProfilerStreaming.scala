/* SimpleApp.scala */

import org.apache.hadoop.io.{BytesWritable, LongWritable, Text}
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat
import org.apache.log4j.{Level, Logger}
import org.apache.spark.rdd.{RDD, UnionRDD}
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.{Seconds, StreamingContext}
import java.io.{File, PrintWriter}
import java.nio.file.Paths

import scala.sys.process._
import scala.reflect.ClassTag


object CellProfilerStreaming {
  // https://stackoverflow.com/questions/29031276/spark-streaming-dstream-rdd-to-get-file-name

  def namedTextFileStream(ssc: StreamingContext, directory: String): DStream[String] =
    ssc.fileStream[LongWritable, BytesWritable, WholeBinaryFormat](directory)
      .transform(rdd =>
        new UnionRDD(rdd.context,
          rdd.dependencies.map(dep =>
            dep.rdd.asInstanceOf[RDD[(LongWritable, BytesWritable)]].map(_._2.toString).setName(dep.rdd.name)
          )
        )
      )

  def transformByFile[U: ClassTag](unionrdd: RDD[String],
                                   transformFunc: String => RDD[String] => RDD[U]): RDD[U] = {
    new UnionRDD(unionrdd.context,
      unionrdd.dependencies.map { dep =>
        if (dep.rdd.isEmpty) None
        else {
          val filename = dep.rdd.name
          Some(
            transformFunc(filename)(dep.rdd.asInstanceOf[RDD[String]])
              .setName(filename)
          )
        }
      }.flatten
    )
  }

  // https://gist.github.com/malcolmgreaves/47a1ac470cd60cffe72ddcf1ea7b7df0

  /** Creates a temporary file, writes the input string to the file, and the file handle.
    *
    * NOTE: This funciton uses the createTempFile function from the File class. The prefix and
    * suffix must be at least 3 characters long, otherwise this function throws an
    * IllegalArgumentException.
    */
  def writeToTempFile(contents: String,
                      prefix: Option[String] = None,
                      suffix: Option[String] = None): File = {
    val tempFi = File.createTempFile(prefix.getOrElse("prefix-"),
      suffix.getOrElse("-suffix"))
    tempFi.deleteOnExit()
    new PrintWriter(tempFi) {
      // Any statements inside the body of a class in scala are executed on construction.
      // Therefore, the following try-finally block is executed immediately as we're creating
      // a standard PrinterWriter (with its implementation) and then using it.
      // Alternatively, we could have created the PrintWriter, assigned it a name,
      // then called .write() and .close() on it. Here, we're simply opting for a terser representation.
      try {
        write(contents)
      } finally {
        close()
      }
    }
    tempFi
  }

  val logger = Logger.getRootLogger()


  def byFileTransformer(filename: String)(rdd: RDD[String]): RDD[(String, String)] =
    rdd.map(line => (filename, line))

  val BATCH_INTERVAL = 5
  val SPARK_MASTER_URL: String = "spark://192.168.1.15:7077"
  val PATH: String = "/mnt/images/Salman_Cell_profiler_data/Data/src/"

  def main(args: Array[String]) {
    runFileStreamingApp
  }

  import java.nio.file.Files

  def runcp(imageFilename: String): String = {
    // Need to look at the worker logs (for the application) to see these:

    logger.warn("image file: " + imageFilename)

    val imageFilenameURL = new File(imageFilename).toURL
    logger.warn("image file: " + imageFilenameURL)

    // The file list needs to represent filenames as URIs:
    val fileListFilename = writeToTempFile(imageFilenameURL.toString)
    logger.warn("created file:" + fileListFilename)

    val cpOutputTempDir = Files.createTempDirectory("cp-output").toFile
    logger.warn("output dir" + cpOutputTempDir)

    // Run cellprofiler, as
    val commandline = s"cellprofiler -p /mnt/images/Salman_Cell_profiler_data/Salman_CellProfiler_cell_counter_no_specified_folders.cpproj -o $cpOutputTempDir --file-list $fileListFilename"
    logger.warn(commandline)

    // The .!! runs an external process and gets all the string outout.
    val output_cp = commandline.!!

    // The output is in CSV files, do a listing to confirm success.
    val output_ls = s"ls -l $cpOutputTempDir".!!
    return output_ls
  }

  private def runFileStreamingApp = {

    val sparkSession = SparkSession.builder
      .master(SPARK_MASTER_URL)
      .appName("CellProfiler")
      .getOrCreate()

    sparkSession.sparkContext.setLogLevel("INFO")

    logger.setLevel(Level.WARN)
    logger.warn("can you see me?")

    val ssc = new StreamingContext(sparkSession.sparkContext, Seconds(BATCH_INTERVAL))

    val dstream = namedTextFileStream(ssc, PATH)

    val filenamesAndContentsDStream = dstream.transform(rdd => transformByFile(rdd, byFileTransformer))

    // (Runs at the driver)
    filenamesAndContentsDStream.foreachRDD(rdd => logger.warn("new files: " + rdd.count()))

    // Discard the file contents, leaving just the filenames:
    val filenamesDStream = filenamesAndContentsDStream.map(_._1.substring("file:".length))
    filenamesDStream.print(5)




    // (Runs at the driver)
    filenamesDStream.foreachRDD(rdd => logger.warn(s"number partitions: ${rdd.getNumPartitions} number files: ${rdd.count}"))

    val cellprofilerOutputDStream = filenamesDStream.map(runcp)

    // Force execution, (but don't collect)
    cellprofilerOutputDStream.count().print()
    // Should print some CSV filenames for the output:
    cellprofilerOutputDStream.print(5)

    ssc.start()
    ssc.awaitTermination()
  }

}
