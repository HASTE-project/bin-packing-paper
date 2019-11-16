/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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

/** An {@link InputFormat} for plain text files.  Files are broken into lines.
 * Either linefeed or carriage-return are used to signal end of line.  Keys are
 * the position in the file, and values are the line of text.. */
@InterfaceAudience.Public
@InterfaceStability.Stable
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
                // Dummy value - just to get it to do something
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

//        String delimiter = context.getConfiguration().get(
//                "textinputformat.record.delimiter");
//        byte[] recordDelimiterBytes = null;
//        if (null != delimiter)
//            recordDelimiterBytes = delimiter.getBytes(Charsets.UTF_8);
//        return new LineRecordReader(recordDelimiterBytes);
    }

    @Override
    protected boolean isSplitable(JobContext context, Path file) {
        return false;
    }

}
