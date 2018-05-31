#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <math.h> // compile with -lm
#include <pthread.h> // compile with -lpthread
#include <time.h> // compile with -lrt

#define MEM_SIZE 16000000
#define TOP (8*1700)

struct thread_info {
	pthread_t thread_id;
	int thread_num;
	int arg;
};

int verbose = 0;
struct thread_info *tinfo;
long n_mem = 0;

int mem[MEM_SIZE];
int indices[TOP*16];

static void* thread_sync(void *arg) {
	int n = ((struct thread_info*)arg)->arg;
	int x = 0;
	long i, j, index;
	long m;
	long offs = ((struct thread_info*)arg)->thread_num * TOP;
	for(j=0; j<n; j++) {
		m = n_mem;
		for(i=0; i<m; i++) {
			index = indices[offs+i]; //(long)rand() * ((long) MEM_SIZE) / RAND_MAX;
			mem[index] = x;
		}
		m = (long)TOP-n_mem;
		for(i=0; i<m; i++) {
//			x = sqrt(3.0*x+4.0);
			x = ((i+1)*(x+1))/7;
		}
	}
	return NULL;
}

unsigned long print_time(unsigned long prev_tstamp, const char* prompt) {
	struct timespec t;
	unsigned long tstamp;
	clock_gettime(CLOCK_REALTIME, &t);
	tstamp = (t.tv_sec)*1000L + (t.tv_nsec)/1000000L;
	printf("%s:\t%lu\n", prompt, tstamp - prev_tstamp);
	return tstamp;
}


void parallelize(int num_threads, char *affinities, void *(*start_routine) (void *)) {
	int tnum, err;
	pthread_attr_t attr;
	cpu_set_t cpus;
	
	pthread_attr_init(&attr);
	for(tnum = 0; tnum < num_threads; tnum++) {
		tinfo[tnum].thread_num = tnum + 1;
		
		// set affinity attribute
		CPU_ZERO(&cpus);
		CPU_SET((int)(affinities[tnum]-48), &cpus);
		err = pthread_attr_setaffinity_np(&attr, sizeof(cpu_set_t), &cpus);
		if(err)
			printf("*error(%d): pthread_attr_setaffinity_np, %d\n", __LINE__, err);
			
		// start thread
		err = pthread_create(&tinfo[tnum].thread_id, &attr, start_routine, &tinfo[tnum]);
		if(err)
			printf("*error(%d): pthread_create, %d\n", __LINE__, err);
	}
	
	// wait for all threads to finish
	for(tnum = 0; tnum < num_threads; tnum++) {
		pthread_join(tinfo[tnum].thread_id, NULL);
	}
}

int main(int argc, char *argv[])
{
	pthread_t t0;
	cpu_set_t cpus;
	int num_threads, err, opt;
	
	int r;
	int ntotal = 10000;
	char* affinities = "01";
	float mratio = 0.5;
	
	void *(*start_routine) (void *) = &thread_sync;

	unsigned long prev_tstamp = 0L;
	unsigned long start_tstamp;
	
	// parse arguments
	while((opt = getopt(argc, argv, "vm:w:r:z:c:")) != -1) {
		switch (opt) {
			case 'v':
				verbose = 1;
				break;
			case 'm':
				mratio = atof(optarg);
				break;
			case 'w':
				ntotal = atoi(optarg);
				break;
			case 'c':
				affinities = malloc(strlen(optarg));
				strcpy(affinities, optarg);
				break;
			default:
				fprintf(stderr, "Usage: %s [-v] [-m mem_ratio] [-w workload] [-c core_affinities]\n", argv[0]);
				exit(EXIT_FAILURE);
		}
	}

	// init
	num_threads = strlen(affinities);
	printf("%d threads...\n", num_threads);
	tinfo = calloc(num_threads, sizeof(struct thread_info));
	
	n_mem = (int)((double)TOP * mratio);
	for(r = 0; r < num_threads; r++) {
		tinfo[r].arg = ntotal;
	}
	
	for(r = 0; r < TOP*16; r++) {
		indices[r] = (long)rand() * ((long)MEM_SIZE) / RAND_MAX;
	}
	
	// set thread0 affinity to C0
	t0 = pthread_self();
	CPU_ZERO(&cpus);
	CPU_SET(0, &cpus);
	err = pthread_setaffinity_np(t0, sizeof(cpu_set_t), &cpus);
	if(err)
		printf("*error(%d): pthread_getaffinity_np, %d\n", __LINE__, err);
	
	prev_tstamp = print_time(prev_tstamp, "Start");
	start_tstamp = prev_tstamp;
	
	// execute parallel workload
	parallelize(num_threads, affinities, start_routine);
	
	print_time(start_tstamp, "Time");
	return 0;
}
