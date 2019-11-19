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

TODO: 
1. call Rest API to get number of cores, e.g.
http://localhost:4040/api/v1/applications
    > gives name of app

http://localhost:4040/api/v1/applications/app-20191119153056-0033/executors
    > lists number of cores for each executor

2. start shuffle service (required for autoscaling)

3. enable autoscaling 

4. bash script:
    for loop > curl > python (parse json, compute total cores) > stdout (with time)
    
    

# Copy 1 file
cd /mnt/images/Salman_Cell_profiler_data/Data ; cp Nuclear\ images/011001-1-001001001.tif src/

# Copy all files
cd /mnt/images/Salman_Cell_profiler_data/Data ; cp Nuclear\ images/* src/

