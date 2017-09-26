
#include <stdio.h>
#include <math.h> // compile with -lm

#define MAX_SAMPLES 10000
double a7samples[MAX_SAMPLES];
double a15samples[MAX_SAMPLES];
double dtSamples[MAX_SAMPLES];

int main(int argc, char* argv[])
{
	char* path;
	unsigned int start = 0;
	unsigned int end = 0;

	if(argc<=1) {
		printf("Input file?\n");
		printf("Usage: meanpwr inputfile [starttime [endtime]]\n");
		return 1;
	}
	else {
		path = argv[1];
	}
	if(argc>2) {
		start = atoi(argv[2]);
	}
	if(argc>3) {
		end = atoi(argv[3]);
	}

	FILE* fp;
	fp = fopen(path, "r");
	if(fp==NULL) {
		printf("Cannot read file %s\n", path);
		return 1;
	}
	
	unsigned int time;
	double a7v, a7a, a15v, a15a;
	int a7f, a15f;
	double a7power, a15power;
	double dt;
	
	double sumA7power = 0.0;
	double sumA15power = 0.0;
	double totalTime = 0;
	unsigned int prevTime = 0;
	unsigned int samples = 0;
	
	while(fscanf(fp, "%u %lf %lf %lf %lf %lf %lf %d %d",
			&time, &a7v, &a7a, &a7power, &a15v, &a15a, &a15power, &a7f, &a15f) == 9) {
		if(start<100000)
			start = time + start;
		if(end>0 && end<100000)
			end = start + end;
		if(prevTime==0)
			prevTime = time;
		
		if(time>=start && (time<end || end==0)) {
			dt = (double)(time - prevTime);
			sumA7power += a7power * dt;
			sumA15power += a15power * dt;
			totalTime += dt;
			
			dtSamples[samples] = dt;
			a7samples[samples] = a7power;
			a15samples[samples] = a15power;
			samples++;
			if(samples>=MAX_SAMPLES) {
				printf("Too many samples.\n");
				break;
			}
		}
		prevTime = time;
	}
	fclose(fp);

	if(totalTime>0 && samples>0) {
		sumA7power /= totalTime;
		sumA15power /= totalTime;
		
		double meanPower = sumA7power+sumA15power;
		double var7 = 0.0;
		double var15 = 0.0;
		double varTotal = 0.0;
		int i;
		for(i=0; i<samples; i++) {
			var7 += dtSamples[i] * (a7samples[i] - sumA7power) * (a7samples[i] - sumA7power);
			var15 += dtSamples[i] * (a15samples[i] - sumA15power) * (a15samples[i] - sumA15power);
			varTotal += dtSamples[i] * (a7samples[i] + a15samples[i] - meanPower) * (a7samples[i] + a15samples[i] - meanPower);
//			printf("%.2f%%\n", (a7samples[i] + a15samples[i] - meanPower) * 100.0 / meanPower);
		}
		var7 = sqrt(var7 / totalTime);
		var15 = sqrt(var15 / totalTime);
		varTotal = sqrt(varTotal / totalTime);
		
		printf("%.4lf\t%.4lf\t%.4lf\t%.2f%%\t%.2f%%\t%.2f%%\n", sumA7power, sumA15power, meanPower,
				var7*100.0/sumA7power, var15*100.0/sumA15power, varTotal*100.0/meanPower);
		return 0;
	}
	else {
		printf("No samples in range.\n");
		return 1;
	}
}
