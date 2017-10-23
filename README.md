PThreads
=========================

PThreads is a synthetic benchmark for testing parallelization factor and speedup in heterogeneous systems.

**odroid** directory contains executables for Odroid XU3 board, which include:

* **[pthreads](odroid/pthreads.c)** the benchmark itself.
* **[xu3log](odroid/xu3log.c)** power monitor logging into CSV format (tab-separated)
* **[meanpwr](odroid/meanpwr.c)** utility to calculate average power over CSV saved by xu3log

**opencl** directory contains executable for CPU-GPU experiments in OpenCL:

* **[mplatf](opencl/mplatf.c)** is the main file.
* **[multicl](opencl/multicl.h)** is a custom library for simultaneous kernel execution on multiple OpenCL devices.

**procdata** directory contains Eclipse project in Java with useful data processing ulitities.

* **[GenRunAll](procdata/src/ncl/cs/prime/odroid/GenRunAll.java)** generates a shell script for running pthreads benchmark in all core combinations.
* **[CollectTimes](procdata/src/ncl/cs/prime/odroid/CollectTimes.java)** collects execution times from pthreads.log generated during the experiment.
* **[CollectSpreadsheet](procdata/src/ncl/cs/prime/odroid/CollectSpreadsheet.java)** collects data from all experiments into a single MS Office XML spreadsheet with all relevant calculations and nice formatting.
* **[Diagrams](procdata/src/ncl/cs/prime/diagrams/Diagrams.java)** produces bar charts in SVG format from CSV input data.
* **...CL** versions of these files are tailored to OpenCL experiments.

