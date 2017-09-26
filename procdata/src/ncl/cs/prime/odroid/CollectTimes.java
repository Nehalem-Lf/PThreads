package ncl.cs.prime.odroid;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class CollectTimes {

	public static final String PATH = "../odroid/amd";
	public static final String SRC = PATH+"/pthreads.log";
	
	public static final String[] M = {"sqrt", "int", "log"};
	
	public static final int EXEC_TIME = 0;
	public static final int MEANPWR_SH = 1;
	public static final int MEANPWR_CALC = 2;
	public static int mode = MEANPWR_CALC;
	
	public static class BenchmarkResult {
		public int m;
		public double p;
		public int z;
		public String c;
		public double j;
		public int total;
		public int n7, n15;
		public boolean seqA15;
		
		public MeanPower power = null;
		public MeanPower calcPower(String path) {
			if(power==null) {
				try {
					power = new MeanPower(String.format("%s/meters%d_%.1f_%s.csv", path, m, p, c), 1500, total-200);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			return power;
		}
	}
	
	public ArrayList<BenchmarkResult> results = new ArrayList<>();
	
	private String getV(String si) {
		return si.split("\\s")[1];
	}
	
	public CollectTimes(String path) throws IOException {
		Scanner in = new Scanner(new File(path));
		BenchmarkResult res = null;
		while(in.hasNextLine()) {
			String line = in.nextLine();
			if(line.startsWith("Benchmark")) {
				if(res!=null)
					results.add(res);
				res = new BenchmarkResult();
				String[] s = line.split("-");
				for(int i=1; i<s.length; i++) {
					switch(s[i].charAt(0)) {
						case 'm':
							res.m = Integer.parseInt(getV(s[i]));
							break;
						case 'p':
							res.p = Double.parseDouble(getV(s[i]));
							break;
						case 'j':
							res.j = Double.parseDouble(getV(s[i]));
							break;
						case 'z':
							res.z = Integer.parseInt(getV(s[i]));
							res.seqA15 = (res.z>=4);
							break;
						case 'c':
							res.c = getV(s[i]);
							res.n7 = 0;
							res.n15 = 0;
							for(int j=0; j<res.c.length(); j++) {
								if(res.c.charAt(j)>='4')
									res.n15++;
								else
									res.n7++;
							}
							break;
					}
				}
			}
			else if(line.startsWith("Total")) {
				String[] s = line.split("\\s");
				res.total = Integer.parseInt(s[1]);
			}
		}
		results.add(res);
		in.close();
	}
	
	public void printResults() {
		for(BenchmarkResult res : results) {
			switch(mode) {
			case EXEC_TIME:
				System.out.printf("%s\t%.1f\t%d\t%s\t%.3f\t%d\n", M[res.m], res.p, res.z, res.c, res.j, res.total);
				break;
			case MEANPWR_SH:
				System.out.printf("./meanpwr meters%d_%.1f_%s.csv 1500 %d >> powers.csv\n", res.m, res.p, res.c, res.total-200);
				break;
			case MEANPWR_CALC:
				res.calcPower(PATH).print();
				break;
		}
		}
	}
	
	public static void main(String[] args) {
		try {
			new CollectTimes(SRC).printResults();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

}
