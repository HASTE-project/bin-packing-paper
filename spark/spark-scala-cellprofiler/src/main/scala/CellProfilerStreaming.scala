/* SimpleApp.scala */

import org.apache.hadoop.io.{BytesWritable, LongWritable, Text}
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat
import org.apache.log4j.Logger
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


  def byFileTransformer(filename: String)(rdd: RDD[String]): RDD[(String, String)] =
    rdd.map(line => (filename, line))

  val BATCH_INTERVAL = 5
  val SPARK_MASTER_URL: String = "spark://192.168.1.15:7077"
  val PATH: String = "/mnt/images/Salman_Cell_profiler_data/Data/one_image/"

  def main(args: Array[String]) {
    runFileStreamingApp
  }
  import java.nio.file.Files

  def runcp(filename: String): Unit = {
//    val fileListFilenmae = writeToTempFile(filename)
//    println("created file:" + fileListFilenmae)
//    println("image file:" + filename)

    val tempDir = Files.createTempDirectory("some-prefix").toFile

    Files.copy(Paths.get(filename), Paths.get(tempDir.getPath + "/foo.tiff"))
    //s"cellprofiler -p /mnt/images/Salman_Cell_profiler_data/Salman_CellProfiler_cell_counter_no_specified_folders.cpproj --file-list $fileListFilenmae".!!

    val commandline = s"cellprofiler -p /mnt/images/Salman_Cell_profiler_data/Salman_CellProfiler_cell_counter_no_specified_folders.cpproj -i ${tempDir.getAbsolutePath}"
    println(commandline)
    val output = commandline.!!

//    Files.deleteIfExists(tempDir.toPath)

    return output
  }

  private def runFileStreamingApp = {

    val filename = "/mnt/images/Salman_Cell_profiler_data/Data/one_image/18.tif"
    println(s"head $filename".!!)

    println(runcp("/mnt/images/Salman_Cell_profiler_data/Data/one_image/21.tif"))

    val sparkSession = SparkSession.builder
      .master(SPARK_MASTER_URL)
      .appName("CellProfiler")
      .getOrCreate()

    sparkSession.sparkContext.setLogLevel("INFO")

    val ssc = new StreamingContext(sparkSession.sparkContext, Seconds(BATCH_INTERVAL))

    val dstream = namedTextFileStream(ssc, PATH)

    val filenamesAndContentsDStream = dstream.transform(rdd => transformByFile(rdd, byFileTransformer))

    filenamesAndContentsDStream.foreachRDD(rdd => println("processed: " + rdd.count()))

    // Discard the file contents, leaving just the filenames.
    val filenamesDStream = filenamesAndContentsDStream.map(_._1.substring("file:".length))
    filenamesDStream.print(5)

    // TODO: create file for --file-list

    // The .!! runs an external process and gets all the string outout.

    val cellprofilerOutputDStream = filenamesDStream.map(runcp)

    // Force execution, (but don't collect)
    cellprofilerOutputDStream.count()
    cellprofilerOutputDStream.print(1)

    ssc.start()
    ssc.awaitTermination()
  }

}
