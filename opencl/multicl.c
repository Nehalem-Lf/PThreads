#include "multicl.h"

cl_platform_id find_platform(const char* platform_name) {
	char buffer[256];
	unsigned int i;
	cl_uint nplatforms;
	cl_platform_id* platforms;
	cl_platform_id platform = NULL;
	clGetPlatformIDs(0, 0, &nplatforms);
	platforms = (cl_platform_id*)malloc(nplatforms*sizeof(cl_platform_id));
	clGetPlatformIDs(nplatforms, platforms, 0);
	for(i = 0; i<nplatforms; i++) {
		platform = platforms[i];
		clGetPlatformInfo(platforms[i], CL_PLATFORM_NAME, 256, buffer, 0);
		if(!strcmp(buffer, platform_name)) break;
	}
	return platform;
}

dev_context create_context(cl_platform_id platform, cl_device_type device_type, size_t work_size) {
	cl_int err;
	dev_context context = (dev_context)malloc(sizeof(struct _dev_context));
	context->platform = platform;

	// find device
	cl_device_id dev_ids[16];
	err = clGetDeviceIDs(context->platform, device_type, 1, dev_ids, NULL);
	context->device = dev_ids[0];

	// create opencl context and queue
	context->context = clCreateContext(0, 1, &context->device, NULL, NULL, &err);
	context->queue = clCreateCommandQueue(context->context, context->device, CL_QUEUE_PROFILING_ENABLE, &err);
	context->work_size = work_size;
	return context;
}

cl_int create_kernel(dev_context context, const char* source, const char* func_name) {
	cl_int err;
	// build program from source and create kernel
	context->program = clCreateProgramWithSource(context->context, 1, (const char **)&source, NULL, &err);
	err = clBuildProgram(context->program, 0, NULL, NULL, NULL, NULL);
	context->kernel = clCreateKernel(context->program, func_name, &err);
	return err;
}

int m_create_kernel(int m, dev_context contexts[], const char* source, const char* func_name) {
	cl_int err;
	int i;
	for(i = 0; i < m; i++) {
		err = create_kernel(contexts[i], source, func_name);
		if(err!=CL_SUCCESS)
			return i;
	}
	return -1;
}

cl_mem arg_floatbuff(dev_context context, int start, int end, int arg_index, float* h_x) {
	cl_int err;
	size_t bytes = (end - start) * sizeof(float);
	cl_mem d_x = clCreateBuffer(context->context, CL_MEM_READ_ONLY, bytes, NULL, NULL);
	err = clEnqueueWriteBuffer(context->queue, d_x, CL_TRUE, 0, bytes, &h_x[start], 0, NULL, NULL);
	err = clSetKernelArg(context->kernel, arg_index, sizeof(cl_mem), &d_x);
	return d_x;
}

cl_mem* m_arg_floatbuff(int m, dev_context contexts[], int n[], int arg_index, float* h_x) {
	cl_mem* d_x = (cl_mem*)malloc(m*sizeof(cl_mem));
	int i;
	int start = 0;
	int end;
	for(i = 0; i < m; i++) {
		end = start + n[i];
		d_x[i] = arg_floatbuff(contexts[i], start, end, arg_index, h_x);
		start = end;
	}
	return d_x;
}

cl_mem result_floatbuff(dev_context context, int start, int end, int arg_index, float* h_x) {
	size_t bytes = (end - start) * sizeof(float);
	cl_mem d_x = clCreateBuffer(context->context, CL_MEM_WRITE_ONLY, bytes, NULL, NULL);
	return d_x;
}

cl_mem* m_result_floatbuff(int m, dev_context contexts[], int n[], int arg_index, float* h_x) {
	cl_mem* d_x = (cl_mem*)malloc(m*sizeof(cl_mem));
	int i;
	int start = 0;
	int end;
	for(i = 0; i < m; i++) {
		end = start + n[i];
		d_x[i] = result_floatbuff(contexts[i], start, end, arg_index, h_x);
		start = end;
	}
	return d_x;
}

void get_result(dev_context context, int start, int end, cl_mem d_x, float* h_x) {
	size_t bytes = (end - start) * sizeof(float);
	clEnqueueReadBuffer(context->queue, d_x, CL_TRUE, 0, bytes, &h_x[start], 0, NULL, NULL);
}

void m_get_result(int m, dev_context contexts[], int n[], cl_mem* d_x, float* h_x) {
	int i;
	int start = 0;
	int end;
	for(i = 0; i < m; i++) {
		end = start + n[i];
		get_result(contexts[i], start, end, d_x[i], h_x);
		start = end;
	}
}

void m_arg_int(int m, dev_context contexts[], int arg_index, int values[]) {
	cl_int err;
	int i;
	for(i = 0; i < m; i++) {
		err = clSetKernelArg(contexts[i]->kernel, arg_index, sizeof(int), &values[i]);
	}
}

void m_arg_long(int m, dev_context contexts[], int arg_index, long values[]) {
	cl_int err;
	int i;
	for(i = 0; i < m; i++) {
		err = clSetKernelArg(contexts[i]->kernel, arg_index, sizeof(long), &values[i]);
	}
}

void start_kernel(dev_context context, size_t work_size, int n) {
	if(work_size>0 && n>0)
		clEnqueueNDRangeKernel(context->queue, context->kernel, 1, NULL, &n, &work_size, 0, NULL, &context->event);
}

void m_start_kernel(int m, dev_context contexts[], int n[]) {
	int i;
	for(i = 0; i < m; i++) {
		start_kernel(contexts[i], contexts[i]->work_size, n[i]);
	}
}

void finish(dev_context context, size_t work_size, int n) {
	if(work_size>0 && n>0) {
		clFinish(context->queue);
		clWaitForEvents(1, &context->event);
	}
}

void m_finish(int m, dev_context contexts[], int n[]) {
	int i;
	for(i = 0; i < m; i++) {
		finish(contexts[i], contexts[i]->work_size, n[i]);
	}
}

double kernel_time(dev_context context, size_t work_size, int n) {
	if(work_size>0 && n>0) {
		cl_ulong time_start, time_end, exec_time;
		double total_time;
		clGetEventProfilingInfo(context->event, CL_PROFILING_COMMAND_START, sizeof(time_start), &time_start, NULL);
		clGetEventProfilingInfo(context->event, CL_PROFILING_COMMAND_END, sizeof(time_end), &time_end, NULL);
		exec_time = time_end - time_start;
		return ((double)((unsigned long)exec_time)) / 1E9;
	}
	else {
		return 0.0;
	}
}

double m_kernel_time(int m, dev_context contexts[], int n[]) {
	double max = 0.0;
	int i;
	for(i = 0; i < m; i++) {
		double t = kernel_time(contexts[i], contexts[i]->work_size, n[i]);
		if(t>max)
			max = t;
	}
	return max;
}

void m_release_mem(int m, cl_mem* d_x) {
	int i;
	for(i = 0; i < m; i++) {
		clReleaseMemObject(d_x[i]);
	}
}

void release(dev_context context) {
	clReleaseProgram(context->program);
	clReleaseKernel(context->kernel);
	clReleaseCommandQueue(context->queue);
	clReleaseContext(context->context);
}

void m_release(int m, dev_context contexts[]) {
	int i;
	for(i = 0; i < m; i++) {
		release(contexts[i]);
		free(contexts[i]);
	}
}
