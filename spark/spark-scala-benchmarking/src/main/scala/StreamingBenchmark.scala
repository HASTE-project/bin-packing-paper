/* SimpleApp.scala */

import java.lang.management.{ManagementFactory, ThreadMXBean}
import java.time.Instant

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{LongWritable, Text}
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Seconds, StreamingContext}


import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe


object StreamingBenchmark {

  // NOTE: remember to update the matching const in the python code
  val BATCH_INTERVAL = 5
  val streamingSource: StreamingSource = StreamingSource.Kafka

  val HOSTNAME: String = "ben-stream-src"
  val TCP_STREAMING_PORT: Int = 9999 // See: streaming_server_socket.py
  val SPARK_MASTER_URL: String = "spark://ben-spark-master:7077"

  val threading: ThreadMXBean = ManagementFactory.getThreadMXBean
  val LOGGER_NAME: String = "file-streaming-app" // python code needs to match this to parse logs.

  def pause(pause_secs: Float) = {

    val pause_nano = pause_secs * 1000000000
    val start = threading.getCurrentThreadCpuTime

    var dummyResult: Float = 0
    while (start + pause_nano > threading.getCurrentThreadCpuTime) {
      // Keep the CPU busy for a little while...
      for (i <- 1 to 100) {
        dummyResult = i
      }
    }

    dummyResult
  }

  def process_line(line: String): Float = {
    val pause_secs = line.substring(1, 7).toFloat / 1000
    var dummyResult: Float = pause(pause_secs)
    dummyResult
  }

  def includePath(path: Path): Boolean = {
    // Exclude old files - so that the last modified time check is skipped, so we don't crash
    // if the file is deleted.
    // See: https://stackoverflow.com/questions/50058451/when-is-it-safe-to-delete-files-when-using-file-based-streaming/50062050#50062050
    //println("filter path")
    val name = path.getName

    //val localhostname = java.net.InetAddress.getLocalHost.getHostName
    // println(localhostname) 'ben-spark-master' -- is this the driver app ?

    // This is what the default filter does.
    if (name.startsWith(".")) return false

    val filename_parts = name.split("_")
    val unix_timestamp = filename_parts(0).toInt
    //println(filename_parts(0))

    // This means if file listing overruns significantly, we start to drop files.
    // This is OK - we throttle back in this case anyway
    Instant.now.getEpochSecond < unix_timestamp + BATCH_INTERVAL * 4
  }


  def main(args: Array[String]) {
    streamingSource match {
      case StreamingSource.File => runFileStreamingApp
      case StreamingSource.TCP => runTcpStreamingApp
      case StreamingSource.Kafka => runKafkaStreamingApp
    }
  }


  private def runFileStreamingApp = {
    val dir = "/mnt/nfs/" + HOSTNAME + "/bench2/"

    val sparkSession = SparkSession.builder
      .master(SPARK_MASTER_URL)
      .appName("File Streaming")
      .config("spark.streaming.unpersist", "True") // Not sure this matters, we arn't caching anyway
      .config("spark.streaming.fileStream.minRememberDuration", "60s") // big so file info gets cached.
      .getOrCreate()

    // Leaving this as default 60

    sparkSession.sparkContext.setLogLevel("INFO") // Need info to read "Finding new files took ...."

    val logger = Logger.getLogger(LOGGER_NAME)

    logger.info("test log message")

    if (true) {
      val ssc = new StreamingContext(sparkSession.sparkContext, Seconds(BATCH_INTERVAL))

      ssc.fileStream[LongWritable, Text, TextInputFormat](dir,
        filter = includePath(_), // Ignore IntelliJ suggestion to convert to method value. breaks with older compiler.
        newFilesOnly = true)
        .map(_._2.toString)
        .map(line => process_line(line))
        .foreachRDD(rdd => println("processed: " + rdd.count()))
      ssc.start()
      ssc.awaitTermination()
    }
  }


  private def runTcpStreamingApp = {

    if (!threading.isCurrentThreadCpuTimeSupported) {
      throw new Exception("thread CPU time not supported")
    }

    //mxBean.getCurrentThreadCpuTime


    val sparkSession = SparkSession.builder
      .master(SPARK_MASTER_URL)
      .appName("TCP Streaming Benchmark")
      .config("spark.streaming.blockInterval", "104ms") // 5 secs batch / 48 cores.
      //.config("spark.streaming.unpersist", "True") // Not sure this matters, we arn't caching anyway
      //.config("spark.streaming.fileStream.minRememberDuration", "60s") // big so file info gets cached.

      // TODO: block interval

      .getOrCreate()

    sparkSession.sparkContext.setLogLevel("INFO") // Need >=INFO to read "Finding new files took ...."

    val logger = Logger.getLogger(LOGGER_NAME)

    logger.info("test log message")

    if (true) {
      val ssc = new StreamingContext(sparkSession.sparkContext, Seconds(BATCH_INTERVAL))

      ssc.socketTextStream(
        HOSTNAME,
        TCP_STREAMING_PORT,
        storageLevel = StorageLevel.MEMORY_ONLY_SER_2) // 2x replication - seem to loose blocks else.

        .map(line => process_line(line))
        .foreachRDD(rdd => println("processed: " + rdd.count()))

      ssc.start()
      ssc.awaitTermination()
    }
  }


  private def runKafkaStreamingApp = {

    if (!threading.isCurrentThreadCpuTimeSupported) {
      throw new Exception("thread CPU time not supported")
    }

    val sparkSession = SparkSession.builder
      .master(SPARK_MASTER_URL)
      .appName("Kafka Streaming Benchmark")
      // This is irrelevant for a Direct DStream (no receiver, partitioning comes from kafka, not time steps)
//            .config("spark.streaming.blockInterval", "125ms") // 5 secs batch / 48 cores.
      //.config("spark.streaming.unpersist", "True") // Not sure this matters, we arn't caching anyway
      //.config("spark.streaming.fileStream.minRememberDuration", "60s") // big so file info gets cached.

      // TODO: block interval

      .getOrCreate()

    sparkSession.sparkContext.setLogLevel("INFO") // Need >=INFO to read "Finding new files took ...."

    val logger = Logger.getLogger(LOGGER_NAME)
    logger.info("test log message")


    val ssc = new StreamingContext(sparkSession.sparkContext, Seconds(BATCH_INTERVAL))

    // read here: https://spark.apache.org/docs/latest/streaming-kafka-0-10-integration.html
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "ben-kafka-server:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "foo",
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean),

      // This is unknown (for old ver?)
//      "fetch.message.max.bytes" -> (200000000: java.lang.Integer)

      "max.partition.fetch.bytes" -> (200000000: java.lang.Integer)
    )

    val topics = Array("bench")
    val stream = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )

    stream.map(record => process_line(record.value))
      .foreachRDD(rdd => println("processed: " + rdd.count()))

    ssc.start()
    ssc.awaitTermination()

  }

  def dumpKafkaLine(x: ConsumerRecord[String, String]): Unit = {
    // Because this is logged on the worker it won't be visible on the master...

    val logger = Logger.getLogger(LOGGER_NAME)
    logger.info("key: " + x.key + " value: " + x.value)
  }
}
