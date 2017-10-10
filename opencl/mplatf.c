#include <math.h>
#include <time.h>
#include "multicl.h"

int verbose = 0;

double get_time() {
	return (double)clock() / (double)CLOCKS_PER_SEC;
}

double profile_time(double prev) {
	return get_time()-prev;
}

void print_time(double t, const char* caption) {
	printf("%s:\t%d\n", caption, (int)(t*1000.0));
}

//--------------------------------- main -----------------------------------

const char *kernelSource_sqrt = "\n" \
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

const char *kernelSource_float = "\n" \
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

const char *kernelSource_int = "\n" \
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

const char *kernelSource_long = "\n" \
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

const char *kernelSource_log = "\n" \
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

double do_seq_task(dev_context contexts[], int dev, int k, float* h_out) {
	double exec_time;

	// pass arguments
	int n = 1;
	clSetKernelArg(contexts[dev]->kernel, 0, sizeof(int), &n);
	clSetKernelArg(contexts[dev]->kernel, 1, sizeof(int), &k);
	cl_mem d_out = arg_floatbuff(contexts[dev], 0, 1, 2, h_out);

	// start
	double time = get_time();
	start_kernel_work(contexts[dev], 1, n);

	// wait until finished and get result
	finish_work(contexts[dev], 1, n);
	exec_time = profile_time(time);
	get_result(contexts[dev], 0, 1, d_out, h_out);

	// release arguments
	clReleaseMemObject(d_out);

	return exec_time;
}

double do_par_task(int m, dev_context contexts[], int n[], int k[], float* h_out) {
	// pass arguments
	m_arg_int(m, contexts, 0, n);
	m_arg_int(m, contexts, 1, k);
	cl_mem* d_out = m_arg_floatbuff(m, contexts, n, 2, h_out);

	// start
	double time = get_time();
	m_start_kernel(m, contexts, n);

	// wait until finished and get result
	m_finish(m, contexts, n);
	double exec_time = profile_time(time);
	m_get_result(m, contexts, n, d_out, h_out);

	// release arguments
	m_release_mem(m, d_out);

	return exec_time;
}

int main(int argc, char* argv[]) {
	const char *kernelSources[] = {kernelSource_sqrt, kernelSource_int, kernelSource_log, kernelSource_float};
	const char *kernelSource = kernelSources[0];
	const int max_n[] = { 1, 512, 1024 };

	int n_total;
	int n[] = { 0, 0, 0 };
	int seq_dev = 0;
	int dev;

	int k[] = { 0, 0, 0 };
	int k_seq = 0;

	int mode = 0;
	int repeat = 8;
	int ktotal = 1000000;
	float p = 0.5;
	float adjust = 1.0;
	int balanced = 0;

	int i;
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
						kernelSource = kernelSources[mode];
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
					if(seq_dev<0 || seq_dev>2) {
						fprintf(stderr, "Unknown device: %d\n", seq_dev);
						exit(1);
					}
					break;
				case 'n':
					dev = argv[i][2] - '0';
					if(dev<0 || dev>2) {
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
	n_total = n[0]+n[1]+n[2];
	if(n_total<=0) {
		fprintf(stderr, "No parallel cores\n");
		opt_err = 1;
	}
	if(opt_err) {
		fprintf(stderr, "Usage: %s ...\n", argv[0]);
		exit(1);
	}

	k_seq = (int)(ktotal*(1.0-p));
	printf("%d threads...\n", n_total);

	if(balanced) {
		/*printf("--balanced, alpha_A15=%.3lf\n", alpha_a15[mode]);
		// balance npar
		double alpha_total = 0.0;
		for(r = 0; r < num_threads; r++) {
			if(affinities[r]<'4')
				alpha_total += 1.0;
			else
				alpha_total += alpha_a15[mode];
		}
		for(r = 0; r < num_threads; r++) {
			if(affinities[r]<'4')
				tinfo[r].arg = (int)((ntotal*num_threads*p*adjust) / alpha_total);
			else
				tinfo[r].arg = (int)((ntotal*num_threads*p*adjust) * alpha_a15[mode] / alpha_total);
		}*/
	}
	else {
		printf("--equal share\n");
		int k_par = (int)(ktotal*p*adjust);
		for(dev=0; dev < 3; dev++)
			k[dev] = k_par;
	}

	size_t bytes = n_total * sizeof(float);
	float* h_out = (float*)malloc(bytes);

	// init devices
	dev_context contexts[3];
	contexts[0] = create_context(find_platform("Intel(R) OpenCL"), CL_DEVICE_TYPE_CPU, n[0]);
	contexts[1] = create_context(find_platform("Intel(R) OpenCL"), CL_DEVICE_TYPE_GPU, n[1]);
	contexts[2] = create_context(find_platform("NVIDIA_CUDA"), CL_DEVICE_TYPE_GPU, n[2]);

	// create program and kernel
	int id;
	if((id = m_create_kernel(3, contexts, kernelSource, "task"))>=0) {
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
		t = do_par_task(3, contexts, n, k, h_out);
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
	m_release(3, contexts);
	return 0;
}