/* SimpleApp.scala */

import java.lang.management.{ManagementFactory, ThreadMXBean}
import java.time.Instant

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{BytesWritable, LongWritable, NullWritable, Text}
import org.apache.hadoop.mapreduce.{InputSplit, JobContext, RecordReader, TaskAttemptContext}
import org.apache.hadoop.mapreduce.lib.input.{CombineFileInputFormat, CombineFileRecordReader, CombineFileSplit, TextInputFormat}
import org.apache.log4j.Logger
import org.apache.spark.SparkContext
import org.apache.spark.input.{PortableDataStream, StreamBasedRecordReader, StreamFileInputFormat, StreamInputFormat, StreamRecordReader}
import org.apache.spark.internal.config
import org.apache.spark.rdd.{RDD, UnionRDD}
import org.apache.spark.sql.SparkSession
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.reflect.ClassTag


// https://stackoverflow.com/questions/29031276/spark-streaming-dstream-rdd-to-get-file-name

object StreamingBenchmark {


  def namedTextFileStream(ssc: StreamingContext, directory: String): DStream[String] =
    ssc.fileStream[LongWritable, Text, TextInputFormat](directory)
      .transform(rdd =>
        new UnionRDD(rdd.context,
          rdd.dependencies.map(dep =>
            dep.rdd.asInstanceOf[RDD[(LongWritable, Text)]].map(_._2.toString).setName(dep.rdd.name)
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


  // NOTE: remember to update the matching const in the python code
  val BATCH_INTERVAL = 5
  //  val streamingSource: StreamingSource = StreamingSource.Kafka

  //  val HOSTNAME: String = "ben-stream-src"
  //  val TCP_STREAMING_PORT: Int = 9999 // See: streaming_server_socket.py
  val SPARK_MASTER_URL: String = "spark://192.168.1.15:7077"

  val PATH: String = "/mnt/images/Salman_Cell_profiler_data/Data/one_image/"

  //  val threading: ThreadMXBean = ManagementFactory.getThreadMXBean
  val LOGGER_NAME: String = "file-streaming-app" // python code needs to match this to parse logs.


  def main(args: Array[String]) {
    runFileStreamingApp
  }


  private def runFileStreamingApp = {
    //    val dir = "/mnt/nfs/" + HOSTNAME + "/bench2/"

    val sparkSession = SparkSession.builder
      .master(SPARK_MASTER_URL)
      .appName("CellProfiler")
      //      .config("spark.streaming.unpersist", "True") // Not sure this matters, we arn't caching anyway
      //      .config("spark.streaming.fileStream.minRememberDuration", "60s") // big so file info gets cached.
      .getOrCreate()

    // Leaving this as default 60

    sparkSession.sparkContext.setLogLevel("INFO") // Need info to read "Finding new files took ...."


    val logger = Logger.getLogger(LOGGER_NAME)

    logger.info("test log message")

    val ssc = new StreamingContext(sparkSession.sparkContext, Seconds(BATCH_INTERVAL))


    val dstream = namedTextFileStream(ssc, PATH)

    def byFileTransformer(filename: String)(rdd: RDD[String]): RDD[(String, String)] =
      rdd.map(line => (filename, line))

    val foo = dstream.transform(rdd => transformByFile(rdd, byFileTransformer))


    //    sparkSession.sparkContext.binaryFiles(PATH)
    //      ssc.fileStream[StreamInputFormat, String, PortableDataStream](dir

    //      * @tparam K Key type for reading HDFS file
    //      * @tparam V Value type for reading HDFS file
    //      * @tparam F Input format for reading HDFS file

    //    val foo = ssc.fileStream[NullWritable, BytesWritable, WholeFileInputFormat](PATH)
    //    val foo = ssc.fileStream[LongWritable, Text, TextInputFormat](PATH)


    //        filter = includePath(_), // Ignore IntelliJ suggestion to convert to method value. breaks with older compiler.
    //        newFilesOnly = true)

    //        .map(line => process_line(line)

    foo.foreachRDD(rdd => println("processed: " + rdd.count()))
    //    foo.foreachRDD(rdd => println(rdd.values.toDebugString))

    //    foo.foreachRDD(rdd => println(rdd.))


    val filenames = foo.map(_._1.substring("file:".length))
    filenames.print(1)


    val scriptPath = "~/foo.sh"

    filenames.foreachRDD(rdd => println(rdd.pipe(scriptPath).collect()))


    ssc.start()
    ssc.awaitTermination()

  }


}
