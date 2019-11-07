from .stream_connector import StreamConnector
from harmonicIO.general.services import SysOut
import os
import json
# Example program
# The use case number can be defined by varying the number in use case variable
MASTER_DATA = {
    "MASTER_ADDR": "192.168.1.15",
    "MASTER_PORT": 8080
}

#"salmantoor/harmonicpe:metadata"
#salmantoor/cellprofiler:3.1.9"
PROCC_DATA = {
    "daemon_test":  "salmantoor/cellprofiler:3.1.9",
    "OS":    "ubuntu"
}

SETTING = {
    "IDLE_TIME": 30,
    "MAX_TRY":   3,
    "TOKEN": "None",
    "SOURCE_NAME": "demo_program"
}

ITEM_NUMBER = 100

def meta_data(path):

    directory, file_name = head, tail = os.path.split(path)
    file_size = os.path.getsize(path)
    meta_data = {'name': file_name, 'size': file_size}

    b_meta_data = json.dumps(meta_data).encode('utf-8')

    sep = ';'

    b_sep = json.dumps(sep).encode('utf-8')
    meta_data = bytearray()
    meta_data += b_meta_data

    meta_data += b_sep
    return meta_data

def get_random_data():
    def read_data_from_file(path):
        func_data = bytearray()

        b_meta_data = meta_data(path)
        func_data += b_meta_data         

        with open(path, 'rb') as f:
            lines = f.readlines()

            for line in lines:
                func_data += line
        
        return func_data
     
    # Define data to test
    # /home/ubuntu/old/cellprofile 
    # harmonicIO/stream_connector/lena512.bmp
    d_list = {
        'daemon_test': read_data_from_file('/home/ubuntu/old/cellprofile/001001-1-001001001.tif')
    }

    # Generate a sample stream order
    stream_order = [0] * ITEM_NUMBER
    import random
    for i in range(ITEM_NUMBER):
        stream_order[i] = (i, 'daemon_test' if (random.randrange(1, 100) % len(d_list)) == 0 else 'daemon_test')

    return stream_order, d_list

if __name__ == '__main__':

    # Initialize connector driver
    SysOut.out_string("Running Harmonic Stream Connector")

    sc = StreamConnector(MASTER_DATA["MASTER_ADDR"],
                         MASTER_DATA["MASTER_PORT"],
                         token=SETTING["TOKEN"],
                         std_idle_time=SETTING["IDLE_TIME"],
                         max_try=SETTING["MAX_TRY"],
                         source_name=SETTING["SOURCE_NAME"])

    if sc.is_master_alive():
        SysOut.out_string("Connection to the master ({0}:{1}) is successful.".format(MASTER_DATA["MASTER_ADDR"],
                                                                                     MASTER_DATA["MASTER_PORT"]))
    else:
        SysOut.terminate_string("Master at ({0}:{1}) is not alive!".format(MASTER_DATA["MASTER_ADDR"],
                                                                           MASTER_DATA["MASTER_PORT"]))

    SysOut.debug_string("Generating random order of data in {0} series.".format(ITEM_NUMBER))
    stream_order, d_list = get_random_data()

    # Stream according to the random order
    for _, obj_type in stream_order:

        d_container = sc.get_data_container()

        # Assign data to container
        d_container += d_list[obj_type]

        sc.send_data(PROCC_DATA[obj_type], PROCC_DATA["OS"], d_container)

    SysOut.out_string("Finish!")


