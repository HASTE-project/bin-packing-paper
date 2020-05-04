import matplotlib.pyplot as plt
import np as np

# RUN = '2019-11-21'
# start_timestamp = 0
# end_timestamp = 1200
from spark.lib import get_cores_old, get_cpu

RUN = '2020-02-18'
start_timestamp = 100
end_timestamp = 1000

cores_data = get_cores_old(RUN, 'master')
cpu_data = [get_cpu(RUN, dir) for dir in [f'worker{i}' for i in range(1, 6)]]

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

cpu_array = np.array(combined_cpus)

initial_timestamp = cpu_array[0, 0]

cores_array = np.array(cores_data)

cpu_array[:, 0] = cpu_array[:, 0] - initial_timestamp
cores_array[:, 0] = cores_array[:, 0] - initial_timestamp

TRIM = True
if TRIM:
    # manually 'trim' the start and end by looking at the plot

    cpu_array_start = np.argmax(cpu_array[:, 0] > start_timestamp)
    cpu_array_end = np.argmax(cpu_array[:, 0] > end_timestamp)

    cpu_array = cpu_array[cpu_array_start:cpu_array_end, :]

    cores_array_start = np.argmax(cores_array[:, 0] > start_timestamp)
    cores_array_end = np.argmax(cores_array[:, 0] > end_timestamp)

    cores_array = cores_array[cores_array_start:cores_array_end, :]

plt.rc('figure', figsize=(10, 5))

plt.title('CPU usage on Spark cluster')

plt.plot(cpu_array[:, 0], cpu_array[:, 1] / 5 * 40, label='CPU usage', linestyle="-", lw=0.5, color="black", alpha=0.7)
plt.plot(cores_array[:, 0], cores_array[:, 1], label='Spark Executor Cores (5x SSC.XLARGE)', linestyle="--", lw=0.5, color="black", alpha=0.8)
plt.legend()
plt.xlabel('Timestamp (secs)')
plt.ylabel('Cores')

# for cons with oliver
ax = plt.axes()
ax.yaxis.grid(alpha=0.3)  # horizontal lines
plt.box(False)

if TRIM:
    plt.xlim((start_timestamp, end_timestamp))

plt.tight_layout()
plt.savefig(f'spark_{RUN}_cpus.png', dpi=600)

print()
