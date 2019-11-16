#!/usr/bin/env bash

cd $(dirname $0)


rsync -v spark-scala-cellprofiler/target/scala-2.11/spark-scala-cellprofiler_2.11-0.1.jar 130.238.28.97:~/

# Deploy mode:
# cluster: run remotely, report back console output
# client: relay everything, run it locally.

# See spark-cellprofiler/build.sbt for --packages

ssh 130.238.28.97 -t -t 'SPARK_HOME=~/spark-2.4.4-bin-hadoop2.7 ; \
    $SPARK_HOME/bin/spark-submit \
    --master spark://192.168.1.15:6066 \
    --deploy-mode client \
    --supervise \
    --class "StreamingBenchmark" \
    spark-scala-cellprofiler_2.11-0.1.jar'



# Tried to reduce network traffic when running at home -- to avoid broken TCP connections (didnt work):
# | grep -E "(WARN)|(Finding)|(ERR)|(processed)


#"--supervise to make sure that the driver is automatically restarted if it fails with a non-zero exit code"

#    --verbose \
#    --conf spark.streaming.blockInterval=50ms \ # this is set from Scala

