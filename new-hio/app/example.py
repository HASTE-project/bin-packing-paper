from harmonicPE.daemon import listen_for_tasks
import sys, os
import uuid
import subprocess
import numpy as np
from PIL import Image
import io, time
import json

def get_metadata_and_data(message_bytes):
    
    sep_point = message_bytes.find(b';')
    b_metadata = message_bytes[0:sep_point-1]
    b_data = message_bytes[sep_point+2:]
    return b_metadata, b_data; 

def process_data(message_bytes):
    # Format of binary message representing task for distributed execution is specific to your application.
    print('message was bytes: ' + str(len(message_bytes)), flush=True)
    secs = 10
    time.sleep(secs)
    b_metadata, b_data = get_metadata_and_data(message_bytes)
     
    image_file =  b_data
     
    metadata_str = b_metadata.decode('utf-8')
    metadata = json.loads(metadata_str)

    print ('metadata: ', metadata) 
    print('type: ', type(image_file))
    not_unique_id = 1

    uuid_local_dir = None
    local_file = None
    path_to_data_file = None
    path_to_dir = None
    time.sleep(secs)
    while not_unique_id:
    
        uuid_local_dir = str(uuid.uuid1())
        local_file = metadata['name']
    
        print ("Local dir: ",uuid_local_dir)
        print ("Local file: ",local_file) 
        cwd = os.getcwd()
        print ('CWD: ',cwd) 
        path_to_dir = cwd+'/'+uuid_local_dir
        path_to_data_file = cwd+'/'+uuid_local_dir+'/'+local_file
        print('path_to_dir', path_to_dir)
        print('path_to_data_file', path_to_data_file)
        time.sleep(secs)
        if not os.path.exists(path_to_dir):
            os.makedirs(path_to_dir)
            print('directory created!')
            if not os.path.exists(path_to_data_file):
                #f = open(path_to_data_file, 'w+b')
                #f.write(str.decode(image_file))
                #f.close()
                #PILimage = Image.fromarray(image_file)
                #PILimage.save(path_to_data_file) 
                image = Image.open(io.BytesIO(image_file))
                image.save(path_to_data_file) 
                print('file created! ', os.path.getsize(path_to_data_file))
                
                not_unique_id = 0
                break
        else:
            continue
    time.sleep(secs)    
    status, output = subprocess.getstatusoutput('cellprofiler -p Salman_CellProfiler_cell_counter_no_specified_folders.cpproj -i '+ path_to_dir)  
    print('status: ', status)
    print('output: ', output)
    time.sleep(secs) 
    if status == 0:
        subprocess.getstatusoutput('rm -r '+path_to_dir)
        print('success') 
    else:
        print("failed")   

# Start the daemon to listen for tasks:
listen_for_tasks(process_data)
