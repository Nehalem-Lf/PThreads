package ncl.cs.prime.opencl;

public class GenRunAllCL {

	public static interface WorkloadOption {
		public String name();
		public String get(double p, int m, int n0, int n1, int n2);
	};
	
	public static final WorkloadOption AMDAHL = new WorkloadOption() {
		public String name() {
			return "Amdahl";
		};
		@Override
		public String get(double p, int m, int n0, int n1, int n2) {
			return String.format("%d %.6f %.1f",
					BASE_W, 1.0 / (double)(n0+n1+n2), p);
		}
	};
	
	public static final WorkloadOption GUSTAFSON_PROP = new WorkloadOption() {
		public String name() {
			return "Gustafson (proportional scaling)";
		};
		@Override
		public String get(double p, int m, int n0, int n1, int n2) {
			return String.format("%d %.6f %.1f",
					BASE_W, alphaMin / alphaS, p);
		}
	};
	
	public static final WorkloadOption GUSTAFSON_PAR = new WorkloadOption() {
		public String name() {
			return "Gustafson (parallel scaling)";
		};
		@Override
		public String get(double p, int m, int n0, int n1, int n2) {
			return String.format("%d %.6f %.1f",
					BASE_W, alphaMin * (1.0 - (1.0 - p) / alphaS) / p, p);
		}
	};

	// Experiment options:
	public static final WorkloadOption WORKLOAD = AMDAHL;
	public static final boolean BALANCED = false;
	public static final double[] P = {0.9}; //{0.3, 0.9};
	public static final int BASE_W = 40960000;
	
	public static final double[][] ALPHA = {
		{49.52, 49.52, 49.52, 49.52},
		{1.0, 1.0, 1.0, 1.0},
		{14.954, 14.954, 14.954, 14.954},
		{0.7674, 0.7674, 0.7674, 0.7674} // Intel GPU special case (n>8)
	};
	public static int[][] CORE_SETUP = {
		{0, 1},
		{0, 8, 64, 256},
		{0, 8, 64, 256, 1024}
	};

	public static final String[] MODE_NAMES = {"sqrt"}; // , "int", "log", "float"};
	
	private static int m, z;
	private static int[] n = new int[3];
	private static double p, alphaMin, alphaS;
	
	private static double getAlpha(int dev, int m) {
		if(dev==1 && n[1]>8)
			dev = 3;
		return ALPHA[dev][m];
	}
	
	public static void main(String[] args) {
		System.out.printf("REM %s, %s\nREM b m w j p z n0 n1 n2\n@ECHO OFF\n",
				WORKLOAD.name(), BALANCED ? "balanced" : "equal-share");
		int count = 0;
		for(m=0; m<MODE_NAMES.length; m++) {
			for(int pi=0; pi<P.length; pi++) {
				p = P[pi];
				System.out.printf("\nREM %s p=%.1f\n", MODE_NAMES[m], p);
				for(z=0; z<3; z++) {
					for(int d1=0; d1<CORE_SETUP[1].length; d1++)
						for(int d2=0; d2<CORE_SETUP[2].length; d2++) {
							n[0] = 0;
							n[1] = CORE_SETUP[1][d1];
							n[2] = CORE_SETUP[2][d2];
							
							if(n[1]==0 && n[2]==0)
								n[z] = 1;
							if(z==1 && n[1]==0)
								continue;
							if(z==2 && n[2]==0)
								continue;
							
							if(n[0]>0)
								alphaMin = getAlpha(0, m);
							if(n[1]>0)
								alphaMin = Math.min(getAlpha(1, m), alphaMin);
							if(n[2]>0)
								alphaMin = Math.min(getAlpha(2, m), alphaMin);
							alphaS = ALPHA[z][m];
							
							String opt = WORKLOAD.get(p, m, n[0], n[1], n[2]);
							
							System.out.printf("CALL run.bat %s %d %s %d %d %d %d\n", BALANCED ? "b" : "eq", m,
									opt, z, n[0], n[1], n[2]);
							count++;
						}
				}
			}
		}
		System.out.printf("\nREM num of experiments: %d\n", count);
	}

}
