import matplotlib.pyplot as plt
import numpy as np

# RUN = '2019-11-21'
# start_timestamp = 0
# end_timestamp = 1200
from spark.lib import get_cores_new, get_cpu

RUN = '2020-02-20'
start_timestamp = 100
end_timestamp = 1000
TRIM = True

WORKER_DIR_NAMES = [f'worker{i}' for i in range(1, 6)]

# [  [ ts, [cores1,..,cores4] ]
cores_data = get_cores_new(RUN)

# [  [ [ts, cpu1], [ts, cpu2], ... ]
cpu_data = [get_cpu(RUN, dir) for dir in WORKER_DIR_NAMES]

cores_data_np = np.array([[row[0]] + row[1] for row in cores_data])

start_ts = cores_data_np[0, 0]

plt.rc('figure', figsize=(10, 5))
plt.title('CPU usage on Spark cluster')

# plt.subplots_adjust(left=0.1)


# Creates two subplots and unpacks the output array immediately
fig, axeses = plt.subplots(5, 1, sharey=True, sharex=True)

for worker_id, axes in enumerate(axeses):


    cpu_data_np = np.array(cpu_data[worker_id])

    axes.plot(cores_data_np[:, 0] - start_ts, cores_data_np[:, 1 + worker_id] / 8, label='measured CPU', linestyle="-", lw=0.5, color="black", alpha=0.5)
    # Spark Executor Cores (5x SSC.XLARGE)
    axes.plot(cpu_data_np[:, 0] - start_ts, cpu_data_np[:, 1], label='allocated CPU', linestyle="--", lw=0.5, color="black", alpha=0.9)

    plt.xlabel('Time (secs)')
    plt.yticks(np.arange(0, 1.01, step=0.25), np.arange(0, 101, step=25))

    # axes.set_yticks([0, 0.5, 1])
    # axes.set_ylabel('Cores')
    axes.set_ylabel(f'Worker {worker_id  +1 }')

    if TRIM:
        plt.xlim((start_timestamp, end_timestamp))

    if worker_id == 0:
        axes.legend(loc=1)


font = {'size': 16}

left, width = .04, .5
bottom, height = .25, .5
right = left + width
top = bottom + height

fig.text(left, 0.5*(bottom+top), 'CPU on workers (%)',
        horizontalalignment='left',
        verticalalignment='center',
        rotation='vertical',
        fontdict=font)

plt.savefig(f'spark_{RUN}_combined.png', dpi=600)

print()
