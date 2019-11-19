import java.io.{File, PrintWriter}

import org.apache.hadoop.io.{BytesWritable, LongWritable}
import org.apache.spark.rdd.{RDD, UnionRDD}
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream

import scala.reflect.ClassTag

object SparkUtil {

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

  def byFileTransformer(filename: String)(rdd: RDD[String]): RDD[(String, String)] =
    rdd.map(line => (filename, line))


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

}
