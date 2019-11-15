FROM ubuntu:18.04
# Set the working directory to /app
WORKDIR /app

ENV TZ=Europe/Minsk
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apt-get update; apt-get -y upgrade

RUN apt-get -y install \
    build-essential    \
    cython             \
    git                \
    libmysqlclient-dev \
    libhdf5-dev        \
    libxml2-dev        \
    libxslt1-dev       \
    openjdk-8-jdk      \
    python-dev         \
    python-pip         \
    python-h5py        \
    python-matplotlib  \
    python-mysqldb     \
    python-scipy       \
    python-numpy       \
    python-pytest      \
    python-vigra       \
    python-wxgtk3.0    \
    python-zmq

RUN git clone https://github.com/CellProfiler/CellProfiler.git; cd /app/CellProfiler; git checkout v3.1.9; pip install .

# Checkout and install Harmonic PE:

RUN apt -y install python3-pip
RUN pip3 install Pillow
RUN pip3 install numpy

RUN git clone https://github.com/HASTE-project/HarmonicPE.git;cd /app/HarmonicPE; git checkout thesiswork_testing; pip3 install .

# Make port 80 available (required for the listening daemon)
EXPOSE 80



# Add the example sript (change this to your own:)
ADD example.py /app
ADD Salman_CellProfiler_cell_counter_no_specified_folders.cpproj /app
# Run your script on startup, to start the daemon:
CMD ["python3", "example.py"]
