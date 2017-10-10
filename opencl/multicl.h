#ifndef _MULTICL_H_
#define _MULTICL_H_

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <CL/opencl.h>

typedef struct _dev_context {
	cl_platform_id platform;
	cl_device_id device;
	cl_context context;
	cl_command_queue queue;
	cl_program program;
	cl_kernel kernel;
	size_t work_size;
};
typedef struct _dev_context* dev_context;

cl_platform_id find_platform(const char* platform_name);
dev_context create_context(cl_platform_id platform, cl_device_type device_type, size_t work_size);
cl_int create_kernel(dev_context context, const char* source, const char* func_name);
cl_int m_create_kernel(int m, dev_context contexts[], const char* source, const char* func_name);
cl_mem arg_floatbuff(dev_context context, int start, int end, int arg_index, float* h_x);
cl_mem* m_arg_floatbuff(int m, dev_context contexts[], int n[], int arg_index, float* h_x);
cl_mem result_floatbuff(dev_context context, int start, int end, int arg_index, float* h_x);
cl_mem* m_result_floatbuff(int m, dev_context contexts[], int n[], int arg_index, float* h_x);
void get_result(dev_context context, int start, int end, cl_mem d_x, float* h_x);

void m_get_result(int m, dev_context contexts[], int n[], cl_mem* d_x, float* h_x);
void m_arg_int(int m, dev_context contexts[], int arg_index, int values[]);
void m_arg_long(int m, dev_context contexts[], int arg_index, long values[]);
void start_kernel_work(dev_context context, size_t work_size, int n);
void start_kernel(dev_context context, int n);
void m_start_kernel(int m, dev_context contexts[], int n[]);
void finish_work(dev_context context, size_t work_size, int n);
void finish(dev_context context, int n);
void m_finish(int m, dev_context contexts[], int n[]);
void m_release_mem(int m, cl_mem* d_x);
void release(dev_context context);
void m_release(int m, dev_context contexts[]);

#endif