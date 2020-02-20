Scripts, Code, etc. for Bin Packing Paper


# Deployment

Master 1x ssc.xlarge 
Worker 5x ssc.xlarge
Source 1x ssc.small


# Dataset

See 'box'.

268 x 2.6MB = 696.8MB


# HIO
Docker Hub 
salmantoor/cellprofiler:3.1.9


# Spark

Worker:
130.238.28.97

Streaming Source: 
130.238.29.13

Can't use Python API.
https://spark.apache.org/docs/latest/api/python/pyspark.streaming.html
"fileStream is not available in the Python API; only textFileStream is available."
(instead made Scala app)


Sync the Clock:
sudo ntpdate -v 0.se.pool.ntp.org



# To run the benchamarking:

0. Start Spark, as appropriate, on the different machines:

./spark-2.4.4-bin-hadoop2.7/sbin/start-master.sh
./spark-2.4.4-bin-hadoop2.7/sbin/start-shuffle-service.sh 
./spark-2.4.4-bin-hadoop2.7/sbin/start-slave.sh spark://192.168.1.15:7077

Check in Web GUI that cluster and all workers are up.

0. Stop any existing app (kill via web UI)

0. Clear source directory. 

O
cd /mnt/images/Salman_Cell_profiler_data/Data ; rm src/* ; mkdir src 
 
0. Begin profiling:

```

# profile CPU (run this on each worker)
# gotcha, see: https://serverfault.com/questions/436446/top-showing-64-idle-on-first-screen-or-batch-run-while-there-is-no-idle-time-a
rm cpu.log ; while : ; do  echo "$(top -b -n2 -d 0.1 |grep "Cpu(s)"|tail -n 1) -  $(($(date +%s%N)/1000000))mS since epoch"; sleep 0.5; done >> cpu.log


# Poll the total number of cores (just do this on the master)
# Need to first get AppID from web interface
rm cores.log ; while : ; do  echo "$(curl -s http://localhost:4040/api/v1/applications/app-20200218180401-0003/executors | jq 'reduce .[] as $item (0; . + $item.totalCores)') -  $(($(date +%s%N)/1000000))mS since epoch"; sleep 1; done >> cores.log
# OBSELETE... do it like this to get per-core count:


# executors per machine
rm cores.log ; while : ; do  echo "$(curl -s http://localhost:8080 | grep -o -E '<td>8 \([0-9] Used\)</td>' | cut -c8 | tr '\n' ',') -  $(($(date +%s%N)/1000000))mS since epoch"; sleep 1; done >> cores.log
```

0. Launch the app.

See:
 spark/spark-scala-cellprofiler/deploy_spark_app.bash 


0. Copy in some files
On the source machine:


# Copy 1 file
rm src/* ; cd /mnt/images/Salman_Cell_profiler_data/Data ; cp Nuclear\ images/011001-1-001001001.tif src/

# Copy all files
rm src/* ; cd /mnt/images/Salman_Cell_profiler_data/Data ; cp Nuclear\ images/* src/


# Copy all files with a small pause (this helps spark to create smaller batches - so it can scale in a timely fashion
cd /mnt/images/Salman_Cell_profiler_data/Data ; rm src/* ; find ./Nuclear\ images -name "*.tif" -type f | xargs -I file sh -c "(cp \"file\" ./src; sleep 0.1)"

# copy 20 images
cd /mnt/images/Salman_Cell_profiler_data/Data ; rm src/* ; find ./Nuclear\ images -name "*.tif" -type f | head -n 20 | xargs -I file sh -c "(cp \"file\" ./src; sleep 0.1)"

0. Let it finish. Let it scale down. Stop the app.

0. Stop the profiling. Download the data.

Move files in data/spark to new dir.

Then:
```
mkdir spark/data/master
rsync -z 130.238.28.97:~/*.log spark/data/master
mkdir spark/data/worker1
rsync -z 130.238.28.106:~/*.log spark/data/worker1
mkdir spark/data/worker2
rsync -z 130.238.28.86:~/*.log spark/data/worker2
mkdir spark/data/worker3
rsync -z 130.238.29.54:~/*.log spark/data/worker3
mkdir spark/data/worker4
rsync -z 130.238.28.59:~/*.log spark/data/worker4
mkdir spark/data/worker5
# worker 5 doesn't have public IP
rsync -z ben-spark-worker-2-4:~/*.log spark/data/worker5
```



# Runs
2019-11-20 -- trial run?
2019-11-21 -- run for CCGrid submission.
2020-02-18 -- upped concurrent jobs setting from 3 to 40.
2020-02-20 -- recording per-node executor count also. 



# MISC NOTES

# TO test CP: run manually one image
cellprofiler -p /mnt/images/Salman_Cell_profiler_data/Salman_CellProfiler_cell_counter_no_specified_folders.cpproj -o ~ --file-list filelist


TODO: 
1. call Rest API to get number of cores, e.g.
http://localhost:4040/api/v1/applications
    > gives name of app

http://localhost:4040/api/v1/applications/app-20191119153056-0033/executors
    > lists number of cores for each executor


3. enable autoscaling 

TODO: scaling policy in Spark -- by default it will try to 'spread out' the executors (to maximize IO throughput, this is the opposite of what HIO does)
