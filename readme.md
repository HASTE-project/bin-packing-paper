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


https://spark.apache.org/docs/latest/api/python/pyspark.streaming.html

"fileStream is not available in the Python API; only textFileStream is available."
> make a scala app..

# Copy 1 file
cd /mnt/images/Salman_Cell_profiler_data/Data ; cp Nuclear\ images/011001-1-001001001.tif src/

# Copy all files
cd /mnt/images/Salman_Cell_profiler_data/Data ; cp Nuclear\ images/* src/

