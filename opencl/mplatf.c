#include <math.h>
#include <time.h>
#include "multicl.h"

#define NDEVS 3
const char *dev_names[] = {"Intel(R) OpenCL", "Intel(R) OpenCL", "NVIDIA_CUDA"};
const cl_device_type dev_types[] = {CL_DEVICE_TYPE_CPU, CL_DEVICE_TYPE_GPU, CL_DEVICE_TYPE_GPU};
const int max_n[] = {1, 512, 1024};
const double boost[][NDEVS] = {{500.0, 1.0, 1.0}, {25.0, 1.0, 1.0}, {2.5, 1.0, 1.0}, {25.0, 1.0, 1.0}};

const double alpha[][NDEVS] = {
	{24.3514350982078, 1.0, 14.9801660268563, 0.768866737195047},
	{40.667109080911, 1.0, 7.88676615499091, 0.769127042087032},
	{42.6939047915327, 1.0, 0.246829649846585, 0.856323537072923}
};

int verbose = 0;

//--------------------------------- time profiling -----------------------------------

double get_time() {
	return (double)clock() / (double)CLOCKS_PER_SEC;
}

double profile_time(double prev) {
	return get_time()-prev;
}

void print_time(double t, const char* caption) {
	printf("%s:\t%.5f\n", caption, t*1000.0);
}

//--------------------------------- kernel code -----------------------------------

const char *kernel_source_sqrt = "\n" \
"__kernel void task(const int n, const int k, __global float *out) \n" \
"{ \n" \
"    int id = get_global_id(0); \n" \
"    float x = 0.0; \n" \
"    int i, j; \n" \
" \n" \
"    if(id<n) {\n" \
"        for(j=0; j<k; j++)\n" \
"            for(i=0; i<3000; i++) { \n" \
"                x = sqrt(3.0*x+4.0); \n" \
"            } \n" \
"        out[id] = j;\n" \
"    }\n" \
"} \n" \
"\n";

const char *kernel_source_float = "\n" \
"__kernel void task(const int n, const int k, __global float *out) \n" \
"{ \n" \
"    int id = get_global_id(0); \n" \
"    float x = 0; \n" \
"    int i, j; \n" \
" \n" \
"    if(id<n) {\n" \
"        for(j=0; j<k; j++)\n" \
"            for(i=0; i<2000; i++) { \n" \
"                x = (x+1)*(x+1)*0.142857; \n" \
"            } \n" \
"        out[id] = j;\n" \
"    }\n" \
"} \n" \
"\n";

const char *kernel_source_int = "\n" \
"__kernel void task(const int n, const int k, __global float *out) \n" \
"{ \n" \
"    int id = get_global_id(0); \n" \
"    int x = 0; \n" \
"    int i, j; \n" \
" \n" \
"    if(id<n) {\n" \
"        for(j=0; j<k; j++)\n" \
"            for(i=0; i<1000; i++) { \n" \
"                x = (i+1)*(x+1)/7; \n" \
"            } \n" \
"        out[id] = j;\n" \
"    }\n" \
"} \n" \
"\n";

const char *kernel_source_long = "\n" \
"__kernel void task(const int n, const int k, __global float *out) \n" \
"{ \n" \
"    int id = get_global_id(0); \n" \
"    long x = 0; \n" \
"    int i, j; \n" \
" \n" \
"    if(id<n) {\n" \
"        for(j=0; j<k; j++)\n" \
"            for(i=0; i<1000; i++) { \n" \
"                x = x+1;\n" \
"            } \n" \
"        x = x>>12;"
"        out[id] = x;\n" \
"    }\n" \
"} \n" \
"\n";

const char *kernel_source_log = "\n" \
"__kernel void task(const int n, const int k, __global float *out) \n" \
"{ \n" \
"    int id = get_global_id(0); \n" \
"    float x = 0; \n" \
"    int i, j; \n" \
" \n" \
"    if(id<n) {\n" \
"        for(j=0; j<k; j++)\n" \
"            for(i=0; i<3; i++) { \n" \
"                x = log(3.0*(x+1.0)+2.0); \n" \
"            } \n" \
"        out[id] = j;\n" \
"    }\n" \
"} \n" \
"\n";

//--------------------------------- kernel scheduling -----------------------------------

double do_seq_task(dev_context contexts[], int dev, int k, float* h_out) {
	double exec_time;

	// pass arguments
	int n = 1;
	clSetKernelArg(contexts[dev]->kernel, 0, sizeof(int), &n);
	clSetKernelArg(contexts[dev]->kernel, 1, sizeof(int), &k);
	cl_mem d_out = arg_floatbuff(contexts[dev], 0, 1, 2, h_out);

	// start
	double time = get_time();
	start_kernel(contexts[dev], 1, n);

	// wait until finished and get result
	finish(contexts[dev], 1, n);
	exec_time = profile_time(time);
	if(exec_time<3.2)
		exec_time = kernel_time(contexts[dev], 1, n);
	get_result(contexts[dev], 0, 1, d_out, h_out);

	// release arguments
	clReleaseMemObject(d_out);

	return exec_time;
}

double do_par_task(int m, dev_context contexts[], int n[], int k[], float* h_out) {
	double exec_time;

	// pass arguments
	m_arg_int(m, contexts, 0, n);
	m_arg_int(m, contexts, 1, k);
	cl_mem* d_out = m_arg_floatbuff(m, contexts, n, 2, h_out);

	// start
	double time = get_time();
	m_start_kernel(m, contexts, n);

	// wait until finished and get result
	m_finish(m, contexts, n);
	exec_time = profile_time(time);
	if(exec_time<3.2)
		exec_time = m_kernel_time(m, contexts, n);
	m_get_result(m, contexts, n, d_out, h_out);

	// release arguments
	m_release_mem(m, d_out);

	return exec_time;
}

//--------------------------------- main -----------------------------------

int main(int argc, char* argv[]) {
	const char *kernel_sources[] = {kernel_source_sqrt, kernel_source_int, kernel_source_log, kernel_source_float};
	const char *kernel_source = kernel_sources[0];

	int i;
	int n_total;
	int n[NDEVS];
	for(i=0; i<NDEVS; i++)
		n[i] = 0;
	int seq_dev = 0;
	int dev;

	int k[NDEVS];
	for(i=0; i<NDEVS; i++)
		k[i] = 0;
	int k_seq = 0;

	int mode = 0;
	int repeat = 8;
	int ktotal = 1000000;
	float p = 0.5;
	float adjust = 1.0;
	int balanced = 0;

	int opt_err = 0;
	for(i=1; i<argc && !opt_err; i++) {
		if(argv[i][0]=='-') {
			switch(argv[i][1]) {
				case 'b':
					balanced = 1;
					break;
				case 'v':
					verbose = 1;
					break;
				case 'm':
					mode = argv[++i][0] - '0';
					if(mode>=0 && mode<4) {
						kernel_source = kernel_sources[mode];
					}
					else {
						fprintf(stderr, "Unknown mode: %d\n", mode);
						exit(1);
					}
					break;
				case 'p':
					p = (float)atof(argv[++i]);
					break;
				case 'j':
					adjust = (float)atof(argv[++i]);
					break;
				case 'w':
					ktotal = atoi(argv[++i]);
					break;
				case 'r':
					repeat = atoi(argv[++i]);
					break;
				case 'z':
					seq_dev = argv[++i][0] - '0';
					if(seq_dev<0 || seq_dev>=NDEVS) {
						fprintf(stderr, "Unknown device: %d\n", seq_dev);
						exit(1);
					}
					break;
				case 'n':
					dev = argv[i][2] - '0';
					if(dev<0 || dev>=NDEVS) {
						fprintf(stderr, "Unknown device: %d\n", dev);
						exit(1);
					}
					n[dev] = atoi(argv[++i]);
					if(n[dev]<0 || n[dev]>max_n[dev]) {
						fprintf(stderr, "Bad number of cores for dev %d: %d\n", dev, n[dev]);
						exit(1);
					}
					break;
				default:
					opt_err = 1;
			}
		}
		else {
			opt_err = 1;
		}
	}
	n_total = 0;
	for(i=0; i<NDEVS; i++)
		n_total += n[i];
	if(n_total<=0) {
		fprintf(stderr, "No parallel cores\n");
		opt_err = 1;
	}
	if(opt_err) {
		fprintf(stderr, "Usage: %s ...\n", argv[0]);
		exit(1);
	}

	k_seq = (int)(ktotal*(1.0-p) / boost[mode][seq_dev]);
	printf("%d threads...\n", n_total);

	if(balanced) {
		printf("--balanced, alpha={");
		for(i=0; i<NDEVS; i++) {
			if(i>0)
				printf(", ");
			printf("%.3lf", alpha[mode][i]);
		}
		printf("}\n");
		// balance k_par
		double a;
		double alpha_total = 0.0;
		for(i=0; i<NDEVS; i++) {
			a = (i==1 && n[i]>=16) ? alpha[mode][3] : alpha[mode][i];
			alpha_total += a;
		}
		for(i=0; i<NDEVS; i++) {
			a = (i==1 && n[i]>=16) ? alpha[mode][3] : alpha[mode][i];
			double k_par = (ktotal*n_total*p*adjust) * a / alpha_total;
			k[i] = (int)(k_par / boost[mode][i]);
			if(verbose)
				printf("WLpar[%d]=%d\n", i, k[i]);
		}
	}
	else {
		printf("--equal share\n");
		double k_par = ktotal*p*adjust;
		for(i=0; i<NDEVS; i++)
			k[i] = (int)(k_par / boost[mode][i]);
	}

	size_t bytes = n_total * sizeof(float);
	float* h_out = (float*)malloc(bytes);

	// init devices
	dev_context contexts[NDEVS];
	for(i=0; i<NDEVS; i++)
		contexts[i] = create_context(find_platform(dev_names[i]), dev_types[i], n[i]);

	// create program and kernel
	int id;
	if((id = m_create_kernel(NDEVS, contexts, kernel_source, "task"))>=0) {
		char log[2048];
		clGetProgramBuildInfo(contexts[id]->program, contexts[id]->device, CL_PROGRAM_BUILD_LOG, 2048, log, NULL);
		printf("*error: Kernel build error.\n");
		puts(log);
		exit(1);
	}

	// execute kernel
	double avet_par = 0;
	double avet_seq = 0;
	for(i=0; i<repeat; i++) {
		double t;
		t = do_par_task(NDEVS, contexts, n, k, h_out);
		if(i>0)
			avet_par += t;
		if(verbose) {
			if(i==0)
				printf("(discarded) ");
			print_time(t, "Parallel");
		}
		t = do_seq_task(contexts, seq_dev, k_seq, h_out);
		if(i>0)
			avet_seq += t;
		if(verbose) {
			if(i==0)
				printf("(discarded) ");
			print_time(t, "Sequential");
		}
	}
	avet_par = avet_par / (double) (repeat-1);
	avet_seq = avet_seq / (double) (repeat-1);

	if(verbose) {
		printf("\nAVERAGE:\n");
		print_time(avet_par, "Parallel");
		print_time(avet_seq, "Sequential");
	}

	print_time(avet_par+avet_seq, "Total");

	if(verbose) {
		// check results
		if(fabsf(h_out[0] - k_seq) < 1e-3)
			printf("Seq correct\n");
		else
			printf("Seq incorrect\n");

		int correct = 0;
		id = 0;
		int nadd = 0;
		for(i = 1; i < n_total; i++)
		{
			if(fabsf(h_out[i] - k[id]) < 1e-3)
				correct++;
			if(i-nadd>=n[id]) {
				nadd += n[id];
				id++;
			}
		}
		printf("Par correct %d of %d\n", correct, n_total-1);
	}

	// release resources
	m_release(NDEVS, contexts);
	return 0;
}