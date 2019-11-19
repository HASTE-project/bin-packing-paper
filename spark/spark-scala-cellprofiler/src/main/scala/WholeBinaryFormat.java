

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.io.compress.SplittableCompressionCodec;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import com.google.common.base.Charsets;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

import java.io.IOException;

/**
 * A dummy {@link InputFormat} for whole binary files. DATA IS NOT ACTUALLY READ.
 * Intended to be used in cases where the filename is passed to an external process, but the file contents is not read by spark.
 */
public class WholeBinaryFormat extends FileInputFormat<LongWritable, BytesWritable> {

    @Override
    public RecordReader<LongWritable, BytesWritable> createRecordReader(InputSplit split,
                                                                        TaskAttemptContext context) {
        return new RecordReader<LongWritable, BytesWritable>() {
            private boolean next = true;

            @Override
            public void initialize(InputSplit split, TaskAttemptContext context) throws IOException, InterruptedException {

            }

            @Override
            public boolean nextKeyValue() throws IOException, InterruptedException {
                // One key for each file.
                boolean next = this.next;
                this.next = false;
                return next;
            }

            @Override
            public LongWritable getCurrentKey() throws IOException, InterruptedException {
                return new LongWritable(0);
            }

            @Override
            public BytesWritable getCurrentValue() throws IOException, InterruptedException {
                // DUMMY VALUE! - just to get it to do something.
                return new BytesWritable(new byte[]{0});
            }

            @Override
            public float getProgress() throws IOException, InterruptedException {
                return 0;
            }

            @Override
            public void close() throws IOException {

            }
        };

    }

    @Override
    protected boolean isSplitable(JobContext context, Path file) {
        return false;
    }

}
