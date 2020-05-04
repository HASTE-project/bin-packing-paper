import math
import re

# 2 -  1582039274007mS since epoch
OLD_CORES_LOG_REGEX = re.compile('^([0-9]+)? -  ([0-9]+)mS')


# e.g.
# 6,7,6,6,6, -  1582207056333mS since epoch
NEW_CORES_LOG_REGEX = re.compile('^(([0-9]+,)*) -  ([0-9]+)mS')




CPU_LOG_REGEX = re.compile(
    "^%Cpu\(s\):\s*([0-9\.]+)\s*us,\s*([0-9\.]+)\s*sy,\s*([0-9\.]+)\s*ni,\s*([0-9\.]+)\s*id,\s*([0-9\.]+)\s*wa,\s*([0-9\.]+)\s*hi,\s*([0-9\.]+)\s*si,\s*([0-9\.]+)\s*st\s*-\s*([0-9]+)mS")


# %Cpu(s):  0.0 us,  1.1 sy,  0.0 ni, 98.9 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st -  1574330445538mS since epoch

# 2020-02-18 uses newer schema
def get_cores_old(run):
    results = []
    with open(f'data/{run}/{master}/cores.log') as cores:
        lines = cores.readlines()

    for line in lines:
        print(line)
        matches = OLD_CORES_LOG_REGEX.match(line)

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

# 2020-02-18 uses newer schema
def get_cores_new(run):
    results = []
    with open(f'data/{run}/master/cores.log') as cores:
        lines = cores.readlines()

    for line in lines:
        print(line)
        matches = NEW_CORES_LOG_REGEX.match(line)


        cores = [int(i) for i in matches.group(1).strip(',').split(',')]



        timestamp = int(matches.group(3)) / 1000
        # print (cores, timestamp)
        results.append((timestamp, cores))

    return results




def get_cpu(run, dir):
    results = []
    with open(f'data/{run}/{dir}/cpu.log') as cores:
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