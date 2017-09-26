
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <time.h> // compile with -lrt

const int num_flags = 4;
const char* flag_paths[] = {
	"/sys/bus/i2c/drivers/INA231/3-0045/enable",
	"/sys/bus/i2c/drivers/INA231/3-0040/enable",
	"/sys/bus/i2c/drivers/INA231/3-0041/enable",
	"/sys/bus/i2c/drivers/INA231/3-0044/enable",
};

const int num_params = 8;
const char* param_paths[] = {
	"/sys/bus/i2c/drivers/INA231/3-0045/sensor_V", // A7 V
	"/sys/bus/i2c/drivers/INA231/3-0045/sensor_A",
	"/sys/bus/i2c/drivers/INA231/3-0045/sensor_W",
	"/sys/bus/i2c/drivers/INA231/3-0040/sensor_V", // A15 V
	"/sys/bus/i2c/drivers/INA231/3-0040/sensor_A",
	"/sys/bus/i2c/drivers/INA231/3-0040/sensor_W",
	"/sys/devices/system/cpu/cpu3/cpufreq/scaling_cur_freq",
	"/sys/devices/system/cpu/cpu7/cpufreq/scaling_cur_freq",
};

int read_param(const char* path, char* out) {
	FILE* fp;
	fp = fopen(path, "r");
	if(fp==NULL) {
		return 0;
	}
	if(fgets(out, 64, fp)==NULL) {
		fclose(fp);
		return 0;
	}
	else {
		out[strcspn(out, "\r\n")] = 0;
		fclose(fp);
		return 1;
	}
}

void set_flag(const char* path, const char* flag) {
	FILE* fp;
	fp = fopen(path, "w");
	if(fp==NULL)
		return;
	fputs(flag, fp);
	fclose(fp);
}

void main(int argc, char* argv[])
{
	// get delay from the argument or 100ms by default
	int sleep_int = 100;
	if(argc>1) {
		sleep_int = atoi(argv[1]);
		if(sleep_int<5) {
			printf("WARNING: Delay is too short. Set to minimum of 20ms.\n");
			sleep_int = 5;
		}
	}
	
	int i;
	for(i=0; i<num_flags; i++) {
		set_flag(flag_paths[i], "1");
	}
	
	struct timespec t;
	t.tv_sec  = sleep_int/1000;
	t.tv_nsec = (sleep_int%1000)*1000000L;
	
	// set logger affinity to CPU 0
	unsigned long mask = 1;
	sched_setaffinity(0, sizeof(mask), &mask);
	
	struct timespec t0;
	char p[64];
	for(;;) {
		// timestamp
		clock_gettime(CLOCK_REALTIME, &t0);
		unsigned long tstamp = (t0.tv_sec)*1000L + (t0.tv_nsec)/1000000L;
		printf("%lu\t", tstamp);

		// echo parameters
		for(i=0; i<num_params; i++) {
			if(read_param(param_paths[i], p))
				printf("%s\t", p);
			else
				printf("0\t");
		}
		printf("\n");
		fflush(stdout);
	
		// delay
		nanosleep(&t, NULL);
	}
}

