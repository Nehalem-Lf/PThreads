#include <stdio.h>                                                                                                                                               
#include <stdlib.h>
#include <CL/cl.h>

const char* const units[] = { "b", "kb", "Mb", "Gb" };

void print_device_info_str(cl_device_id device_id, cl_device_info info, const char* name) {
	char value[1024];
	clGetDeviceInfo(device_id, info, 1024, value, NULL);
	printf("\t%s: %s\n", name, value);
}

void print_platform_info_str(cl_platform_id platform_id, cl_platform_info info, const char* name) {
	char value[1024];
	clGetPlatformInfo(platform_id, info, 1024, value, NULL);
	printf("\t%s: %s\n", name, value);
}

void print_device_info_mem(cl_device_id device_id, cl_device_info info, const char* name) {
	cl_ulong value;
	clGetDeviceInfo(device_id, info, sizeof(cl_ulong), &value, NULL);
	double v = value;
	int unit = 0;
	while(v > 512.0L && unit<3) {
		v /= 1024.0L;
		unit++;
	}
//	printf("\t%s: %llu\n", name, value);
	printf("\t%s: %.2f%s\n", name, v, units[unit]);
}

void print_device_info_uint(cl_device_id device_id, cl_device_info info, const char* name) {
	cl_uint value;
	clGetDeviceInfo(device_id, info, sizeof(value), &value, NULL);
	printf("\t%s: %u\n", name, value);
}

int main() {

	int i, j;
	char* value;
	size_t valueSize;
	cl_uint platformCount;
	cl_platform_id* platforms;
	cl_uint deviceCount;
	cl_device_id* devices;
	cl_uint maxComputeUnits;

	// get all platforms
	clGetPlatformIDs(0, NULL, &platformCount);
	platforms = (cl_platform_id*)malloc(sizeof(cl_platform_id)* platformCount);
	clGetPlatformIDs(platformCount, platforms, NULL);

	for(i = 0; i < platformCount; i++) {
		printf("\n--- Platform #%d ---\n", i);
		print_platform_info_str(platforms[i], CL_PLATFORM_NAME, "Platform name");

		// get all devices
		clGetDeviceIDs(platforms[i], CL_DEVICE_TYPE_ALL, 0, NULL, &deviceCount);
		devices = (cl_device_id*)malloc(sizeof(cl_device_id)* deviceCount);
		clGetDeviceIDs(platforms[i], CL_DEVICE_TYPE_ALL, deviceCount, devices, NULL);

		// for each device print critical attributes
		for(j = 0; j < deviceCount; j++) {
			printf("Device #%d\n", j);

			print_device_info_str(devices[j], CL_DEVICE_NAME, "Name");
			print_device_info_str(devices[j], CL_DEVICE_VERSION, "Device version");
			print_device_info_str(devices[j], CL_DRIVER_VERSION, "Driver version");
			print_device_info_str(devices[j], CL_DEVICE_OPENCL_C_VERSION, "OpenCL C version");
			print_device_info_uint(devices[j], CL_DEVICE_MAX_COMPUTE_UNITS, "Max compute units");
			print_device_info_uint(devices[j], CL_DEVICE_MAX_WORK_GROUP_SIZE, "Max work group size");
			print_device_info_uint(devices[j], CL_DEVICE_MAX_CLOCK_FREQUENCY, "Max clock freq");

			print_device_info_mem(devices[j], CL_DEVICE_GLOBAL_MEM_SIZE, "Global memory size");
			print_device_info_mem(devices[j], CL_DEVICE_GLOBAL_MEM_CACHE_SIZE, "Global memory cache size");
			print_device_info_mem(devices[j], CL_DEVICE_LOCAL_MEM_SIZE, "Local memory size");
			print_device_info_mem(devices[j], CL_DEVICE_MAX_MEM_ALLOC_SIZE, "Max memory alloc size");

		}

		free(devices);

	}

	free(platforms);
	return 0;

}