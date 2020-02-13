import re
import math
import matplotlib.pyplot as plt

RUN = '2019-11-21'

CORES_LOG_REGEX = re.compile('^([0-9]+)? -  ([0-9]+)mS')
# %Cpu(s):  0.0 us,  1.1 sy,  0.0 ni, 98.9 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st -  1574330445538mS since epoch
CPU_LOG_REGEX = re.compile(
    "^%Cpu\(s\):\s*([0-9\.]+)\s*us,\s*([0-9\.]+)\s*sy,\s*([0-9\.]+)\s*ni,\s*([0-9\.]+)\s*id,\s*([0-9\.]+)\s*wa,\s*([0-9\.]+)\s*hi,\s*([0-9\.]+)\s*si,\s*([0-9\.]+)\s*st\s*-\s*([0-9]+)mS")


def get_cores():
    results = []
    with open(f'data/{RUN}/master/cores.log') as cores:
        lines = cores.readlines()

    for line in lines:
        print(line)
        matches = CORES_LOG_REGEX.match(line)

        cores = matches.group(1)
        if cores is None:
            # towards the end of the log, the application is not running.
            cores = math.nan
        else:
            cores = int(cores)
        timestamp = int(matches.group(2)) / 1000
        # print (cores, timestamp)
        results.append((timestamp, cores))

    return results


def get_cpu(dir):
    results = []
    with open(f'data/{RUN}/{dir}/cpu.log') as cores:
        lines = cores.readlines()

    for line in lines:
        print(line)
        matches = CPU_LOG_REGEX.match(line)

        cpu_user = float(matches.group(1))
        cp_system = float(matches.group(2))
        cpu_ni = float(matches.group(3))
        cpu_id = float(matches.group(4))
        cpu_wa = float(matches.group(5))
        cpu_hi = float(matches.group(6))
        cpu_si = float(matches.group(7))
        cpu_steal = float(matches.group(8))
        unix = int(matches.group(9)) / 1000

        results.append((unix, (cpu_user + cp_system) / (100)))

    return results


cores_data = get_cores()
cpu_data = [get_cpu(dir) for dir in [f'worker{i}' for i in range(1, 6)]]

spliced = []
for i, trace_for_worker_i in enumerate(cpu_data):
    for unix, cpu_worker_i in trace_for_worker_i:
        spliced.append((unix, cpu_worker_i, i))

# sort by timestamp
spliced = sorted(spliced, key=lambda x: x[0])

combined_cpus = []
cpus = [0] * 5
for unix, cpu_worker_i, i in spliced:
    cpus[i] = cpu_worker_i
    combined_cpus.append([unix, sum(cpus)])

import numpy

cpu_array = numpy.array(combined_cpus)

initial_timestamp = cpu_array[0,0]

cores_array = numpy.array(cores_data)

cpu_array[:,0] = cpu_array[:,0] - initial_timestamp
cores_array[:,0] = cores_array[:,0] - initial_timestamp

import numpy as np

TRIM = True
if TRIM:
    # manually 'trim' the start and end by looking at the plot
    start_timestamp = 100
    end_timestamp = 1200

    cpu_array_start = np.argmax(cpu_array[:,0] > start_timestamp)
    cpu_array_end = np.argmax(cpu_array[:,0] > end_timestamp)

    cpu_array = cpu_array[cpu_array_start:cpu_array_end,:]

    cores_array_start = np.argmax(cores_array[:,0] > start_timestamp)
    cores_array_end = np.argmax(cores_array[:,0] > end_timestamp)

    cores_array = cores_array[cores_array_start:cores_array_end,:]


plt.rc('figure', figsize=(10, 5))

plt.title('CPU usage on Spark cluster')



plt.plot(cpu_array[:, 0], cpu_array[:, 1] / 5 * 40, label='CPU usage', linestyle="-", lw= 0.5, color="black", alpha=0.7)
plt.plot(cores_array[:, 0], cores_array[:, 1], label='Spark Executor Cores (5x SSC.XLARGE)', linestyle="--", lw= 0.5, color="black", alpha=0.8)
plt.legend()
plt.xlabel('Timestamp (secs)')
plt.ylabel('Cores')

# for cons with oliver
ax = plt.axes()
ax.yaxis.grid(alpha=0.3) # horizontal lines
plt.box(False)

if TRIM:
    plt.xlim((start_timestamp, end_timestamp))

plt.tight_layout()
plt.savefig(f'spark_{RUN}_cpus.png', dpi=600)



print()
