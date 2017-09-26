package ncl.cs.prime.odroid;

public class GenRunAll {

	private static interface WorkloadOption {
		public String name();
		public String get(double p, int m, int na7, int na15);
	};
	
	public static final WorkloadOption AMDAHL = new WorkloadOption() {
		public String name() {
			return "Amdahl";
		};
		@Override
		public String get(double p, int m, int na7, int na15) {
			return String.format("%d %.3f %.1f",
					BASE_W, 1.0 / (double)(na7+na15), p);
		}
	};
	
	public static final WorkloadOption GUSTAFSON_PROP = new WorkloadOption() {
		public String name() {
			return "Gustafson (proportional scaling)";
		};
		@Override
		public String get(double p, int m, int na7, int na15) {
			return String.format("%d %.3f %.1f",
					BASE_W, alphaMin / alphaS, p);
		}
	};
	
	public static final WorkloadOption GUSTAFSON_PAR = new WorkloadOption() {
		public String name() {
			return "Gustafson (parallel scaling)";
		};
		@Override
		public String get(double p, int m, int na7, int na15) {
			return String.format("%d %.3f %.1f",
					BASE_W, alphaMin * (1.0 - (1.0 - p) / alphaS) / p, p);
		}
	};

	// Experiment options:
	public static final WorkloadOption WORKLOAD = GUSTAFSON_PAR;
	public static final boolean BALANCED = false;
	public static final double[] P = {0.3, 0.9};
	public static final int BASE_W = 10000;
	
	public static final double[] ALPHA15 = {0.9232, 1.2399, 1.7623};

	public static final int[] A7_CORES = {3, 2, 1}; 
	public static final int[] A15_CORES = {7, 6, 5, 4}; 
	public static final String[] MODE_NAMES = {"sqrt", "int", "log"};
	
	private static int m, na7, na15;
	private static double p, alphaMin, alphaS;
	
	
	private static String listCores(int na7, int na15) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<na15; i++)
			sb.append(A15_CORES[i]);
		for(int i=0; i<na7; i++)
			sb.append(A7_CORES[i]);
		return sb.toString();
	}
	
	public static void main(String[] args) {
		System.out.printf("# %s, %s\n# b m w j p z c\r\n",
				WORKLOAD.name(), BALANCED ? "balanced" : "equal-share");
		for(m=0; m<MODE_NAMES.length; m++) {
			for(int pi=0; pi<P.length; pi++) {
				p = P[pi];
				System.out.printf("\n# %s p=%.1f\n", MODE_NAMES[m], p);
				for(na7=0; na7<=A7_CORES.length; na7++)
					for(na15=0; na15<=A15_CORES.length; na15++) {
						if(na7==0 && na15==0)
							continue;
						
						if(na7==0)
							alphaMin = ALPHA15[m];
						else if(na15==0)
							alphaMin = 1.0;
						else
							alphaMin = Math.min(ALPHA15[m], 1.0);
						alphaS = (na15>0) ? ALPHA15[m] : 1.0;
						
						String opt = WORKLOAD.get(p, m, na7, na15);
						
						int z = (na15>0) ? A15_CORES[0] : A7_CORES[0];
						System.out.printf("../run.sh %s %d %s %d %s\n", BALANCED ? "b" : "eq", m,
								opt, z, listCores(na7, na15));
					}
			}
		}
	}

}
