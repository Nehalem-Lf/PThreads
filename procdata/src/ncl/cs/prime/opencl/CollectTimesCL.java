package ncl.cs.prime.opencl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class CollectTimesCL {

	public static final String PATH = "../opencl/results";
	public static final String SRC = PATH+"/multicl_amd2.log";
	
	public static final String[] M = {"sqrt", "int", "log", "float"};
	
	public static final int EXEC_TIME = 0;
	public static int mode = EXEC_TIME;
	
	public static class BenchmarkResult {
		public int m;
		public double p;
		public int z;
		public int[] n = new int[3];
		public double j;
		public double total;
	}
	
	public ArrayList<BenchmarkResult> results = new ArrayList<>();
	
	private String getV(String si) {
		return si.split("\\s")[1];
	}
	
	public CollectTimesCL(String path) throws IOException {
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
							break;
						case 'n': {
								int dev = s[i].charAt(1) - '0';
								res.n[dev] = Integer.parseInt(getV(s[i]));
							}
							break;
					}
				}
			}
			else if(line.startsWith("Total")) {
				String[] s = line.split("\\s");
				res.total = Double.parseDouble(s[1]);
			}
		}
		results.add(res);
		in.close();
	}
	
	public void printResults() {
		for(BenchmarkResult res : results) {
			switch(mode) {
			case EXEC_TIME:
				System.out.printf("%s\t%.1f\t%.5f\t%d\t%d\t%d\t%d\t%.5f\n", M[res.m], res.p, res.j, res.z, res.n[0], res.n[1], res.n[2], res.total);
				break;
			}
		}
	}
	
	public static void main(String[] args) {
		try {
			new CollectTimesCL(SRC).printResults();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

}
