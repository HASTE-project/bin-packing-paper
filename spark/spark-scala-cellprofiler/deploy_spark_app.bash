#!/usr/bin/env bash


cd $(dirname $0)

# Build the .jar
#cd spark-scala-cellprofiler
#rm target/scala-2.11/spark-scala-cellprofiler_2.11-0.1.jar

#set -e
sbt -v compile package

cd $(dirname $0)

rsync -v target/scala-2.11/spark-scala-cellprofiler_2.11-0.1.jar 130.238.28.97:~/

# Clear src dir on streaming src machine
ssh 130.238.29.13 -t -t 'cd /mnt/images/Salman_Cell_profiler_data/Data ; rm src/*'

# Deploy mode:
# cluster: run remotely, report back console output
# client: relay everything, run it locally.
ssh 130.238.28.97 -t -t 'SPARK_HOME=~/spark-2.4.4-bin-hadoop2.7 ; \
    $SPARK_HOME/bin/spark-submit \
    --master spark://192.168.1.15:6066 \
    --deploy-mode client \
    --supervise \
    --class "CellProfilerStreaming" \
    spark-scala-cellprofiler_2.11-0.1.jar'


# when the app has started, you copy some files into src dir...