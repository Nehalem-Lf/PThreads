package ncl.cs.prime.odroid;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class MeanPower {

	private static final int MAX_SAMPLES = 10000;
	
	public double a7mean, a15mean, totalMean;
	public double a7sdev, a15sdev, totalSdev;
	
	public MeanPower(String path, long start, long end) throws IOException {
		@SuppressWarnings("resource")
		Scanner in = new Scanner(new File(path));

		double[] a7samples = new double[MAX_SAMPLES];
		double[] a15samples = new double[MAX_SAMPLES];
		double[] dtSamples = new double[MAX_SAMPLES];
		
		long time;
		double a7power, a15power;
		double dt;
		
		double sumA7power = 0.0;
		double sumA15power = 0.0;
		double totalTime = 0;
		long prevTime = 0;
		int samples = 0;
		
		while(in.hasNext()) {
			time = in.nextLong();
			in.nextDouble(); // A7 V
			in.nextDouble(); // A7 A
			a7power = in.nextDouble();
			in.nextDouble(); // A15 V
			in.nextDouble(); // A15 A
			a15power = in.nextDouble();
			in.nextInt(); // A7 F
			in.nextInt(); // A15 F

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
					throw new IOException("Too many samples.\n");
				}
			}
			prevTime = time;
		}
		in.close();
	
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
			}
			var7 = Math.sqrt(var7 / totalTime);
			var15 = Math.sqrt(var15 / totalTime);
			varTotal = Math.sqrt(varTotal / totalTime);
			
			this.a7mean = sumA7power;
			this.a15mean = sumA15power;
			this.totalMean = meanPower;
			this.a7sdev = var7/sumA7power;
			this.a15sdev = var15/sumA15power;
			this.totalSdev = varTotal/meanPower;
		}
		else {
			throw new IOException("No samples in range.\n");
		}
	}
	
	public void print() {
		System.out.printf("%.4f\t%.4f\t%.4f\t%.2f%%\t%.2f%%\t%.2f%%\n",
				a7mean, a15mean, totalMean, a7sdev*100.0, a15sdev*100.0, totalSdev*100.0);
	}

}
