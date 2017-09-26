PThreads
=========================

PThreads is a synthetic benchmark for testing parallelization factor and speedup in heterogeneous system.
Current version is designed for Odroid XU3 board and uses power monitors.

**odroid** directory contains executables for odroid, which include:

* **pthreads** the benchmark itself.
* **xu3log** power monitor logging into CSV format (tab-separated)
* **meanpwr** utility to calculate average power over CSV saved by xu3log

**procdata** directory contains Eclipse project in Java with useful data processing ulitities.
