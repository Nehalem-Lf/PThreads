package ncl.cs.prime.odroid;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Scanner;

public class CollectParsec {

	public static final String PATH = "../odroid/data_parsec3";
	
	public static class BenchmarkResult{
		public String app;
		public String c;
		public int total;
		public int n7, n15;
		
		public int charMode() {
			if(n15==0)
				return 0;
			else if(n7==0)
				return 1;
			else
				return 2;
		}
	}
	
	public static class ResultComparator implements Comparator<BenchmarkResult> {
		@Override
		public int compare(BenchmarkResult res1, BenchmarkResult res2) {
			int c = res1.app.compareTo(res2.app);
			if(c==0) {
				c = Integer.compare(res1.charMode(), res2.charMode());
				if(c==0) {
					c = Integer.compare(res1.n15, res2.n15);
					if(c==0) {
						c = Integer.compare(res1.n7, res2.n7);
					}
				}
			}
			return c;
		}
	}
	
	public ArrayList<BenchmarkResult> results = new ArrayList<>();
	
	public BenchmarkResult process(File file) throws IOException {
		BenchmarkResult res = new BenchmarkResult();
		String s[] = file.getName().split("[_\\.]");
		res.app = s[0].substring(3);
		res.c = s[2];
		res.n7 = 0;
		res.n15 = 0;
		for(int j=0; j<res.c.length(); j++) {
			if(res.c.charAt(j)!=',') {
				if(res.c.charAt(j)>='4')
					res.n15++;
				else
					res.n7++;
			}
		}
		if(res.n7+res.n15!=Integer.parseInt(s[1]))
			throw new RuntimeException("Number of cores doesn't match.");
		
		Scanner in = new Scanner(file);
		int total = 0;
		int count = 0;
		while(in.hasNextLine()) {
			String line = in.nextLine().trim();
			if(line.startsWith("real")) {
				String t = line.substring(line.lastIndexOf('m')+1, line.length()-1);
				total += (int)(Double.parseDouble(t)*1000.0);
				count++;
			}
		}
		res.total = total/count;
		in.close();
		return res;
	}
	
	public CollectParsec(String path) throws IOException {
		File dir = new File(path);
		File[] files = dir.listFiles();
		for(File file : files) {
			if(file.getName().endsWith(".txt")) {
				BenchmarkResult res = process(file);
				results.add(res);
			}
		}
		results.sort(new ResultComparator());
	}
	
	public void printResults() {
		for(BenchmarkResult res : results) {
			System.out.printf("%s\t%d\t%d\t%d\n", res.app, res.n7, res.n15, res.total);
		}
	}
	
	public static void main(String[] args) {
		try {
			new CollectParsec(PATH).printResults();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
}
