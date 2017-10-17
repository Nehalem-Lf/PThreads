PThreads
=========================

PThreads is a synthetic benchmark for testing parallelization factor and speedup in heterogeneous system.
Current version is designed for Odroid XU3 board and uses power monitors.

**odroid** directory contains executables for odroid, which include:

* **pthreads** the benchmark itself.
* **xu3log** power monitor logging into CSV format (tab-separated)
* **meanpwr** utility to calculate average power over CSV saved by xu3log

**opencl** directory contains executable for CPU-GPU experiments in OpenCL:

* **mplatf** is the main file.
* **multicl** is a custom library for simultaneous kernel execution on multiple OpenCL devices.

**procdata** directory contains Eclipse project in Java with useful data processing ulitities.

* **GenRunAll** generates a shell script for running pthreads benchmark in all core combinations.
* **CollectTimes** collects execution times from pthreads.log generated during the experiment (outdated, use CollectSpreadsheet instead).
* **CollectSpreadsheet** collects data from all experiments into a single MS Office XML spreadsheet with all relevant calculations and nice formatting.
* **...CL** versions of these files are tailored to OpenCL experiments.

