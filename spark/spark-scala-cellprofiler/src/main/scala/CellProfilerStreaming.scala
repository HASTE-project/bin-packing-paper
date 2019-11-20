import java.io.File

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.sys.process._


object CellProfilerStreaming {

  val logger = Logger.getRootLogger()

  val BATCH_INTERVAL = 5 //5
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
    val fileListFilename = SparkUtil.writeToTempFile(imageFilenameURL.toString)
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



      // Implemented with: https://issues.apache.org/jira/browse/SPARK-12133
      // See: https://medium.com/@pmatpadi/spark-streaming-dynamic-scaling-and-backpressure-in-action-6ebdbc782a69
      // These don't seem to be documented officially? (yet?)
      //      .config("spark.streaming.dynamicAllocation.enabled", true)
      //      .config("spark.streaming.dynamicAllocation.scalingInterval", 10)
      //      .config("spark.streaming.dynamicAllocation.minExecutors", 1)


      // Scaling up is driven by ratio of batch interval to processing time.
      // But, it doesn't apply until an entire batch is copied. So, we want a pause between copying the files.
      // And we also use a short batch interval (of 1 second). this way, we complete the first batch within a few seconds, so
      // we can begin the scaling.

      // .. this doesn't really work -- because the jobs are processed sequentially, so we can never use all of the cores properly.
      // since we must have a small number of files in the batch interval in order for timely scale up,
      // but enough files in the batch interval to use all the cores when it has scaled up.

      // So instead, try with the traditional mechanism.
      .config("spark.shuffle.service.enabled", true)
      .config("spark.dynamicAllocation.enabled", true)
      .config("spark.dynamicAllocation.executorIdleTimeout", 10)


      .getOrCreate()

    //    NOTE: scaling in spark is based on executors. by default, one executor per node (with all cores)
    //    So, we instead set 1 core/executor, to get finer granularity. pay some overhead for this.
    // We configure this in /conf on the master
    // TODO: does this setting also work if set from here? should do, the application owns the executors.

    sparkSession.sparkContext.setLogLevel("INFO")

    logger.setLevel(Level.WARN)
    logger.warn("can you see me?")

    val ssc = new StreamingContext(sparkSession.sparkContext, Seconds(BATCH_INTERVAL))

    val dstream = SparkUtil.namedTextFileStream(ssc, PATH)

    val filenamesAndContentsDStream = dstream.transform(rdd => SparkUtil.transformByFile(rdd, SparkUtil.byFileTransformer))

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
