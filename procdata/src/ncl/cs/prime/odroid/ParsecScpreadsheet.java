package ncl.cs.prime.odroid;

import java.io.IOException;
import java.util.LinkedHashMap;

import ncl.cs.prime.odroid.CollectParsec.BenchmarkResult;
import ncl.cs.prime.oxml.SpreadsheetGenerator;
import ncl.cs.prime.oxml.SpreadsheetGenerator.CellStyle;

public class ParsecScpreadsheet {

	public static final String PATH = "../odroid/data_parsec3";
	public static final String DATA_PATH = PATH;
	public static final String OUTPUT = PATH+"/results/parsec.xml";

	private static class ModeCharacteristics {
		public int timeA7;
		public double a15;
		public double pA7, pA15;
		public double dpA7, dpA15;
		public double pmin, pmax;
	}
	
	public static void main(String[] args) {
		try {
			CollectParsec times = new CollectParsec(DATA_PATH);
			LinkedHashMap<String, ModeCharacteristics> modes = new LinkedHashMap<>();
			
			SpreadsheetGenerator out = new SpreadsheetGenerator(OUTPUT, "generated");
			
			CellStyle defStyle = new CellStyle(null).align("Bottom", "Right").font("Verdana", 8);
			out.beginStyles(defStyle);
			out.addStyle(new CellStyle("hdr", defStyle).bold());
			out.addStyle(new CellStyle("bgb").bgColor("#99CCFF"));
			out.addStyle(new CellStyle("bgp").bgColor("#DDDDFF"));
			out.addStyle(new CellStyle("bgs").bgColor("#DDDDDD"));
			out.addStyle(new CellStyle("bgy").bgColor("#FFFFCC"));
			out.addStyle(new CellStyle("num2").numFormat("0.00"));
			out.addStyle(new CellStyle("num4").numFormat("0.0000"));
			out.addStyle(new CellStyle("num4fade", defStyle).textColor("#999999").numFormat("0.0000"));
			out.addStyle(new CellStyle("num4bgb").bgColor("#99CCFF").numFormat("0.0000"));
			out.addStyle(new CellStyle("num4bgo").bgColor("#FFCC99").numFormat("0.0000"));
			out.addStyle(new CellStyle("num2bgp").bgColor("#DDDDFF").numFormat("0.00"));
			out.addStyle(new CellStyle("num4bgs").bgColor("#DDDDDD").numFormat("0.0000"));
			out.addStyle(new CellStyle("pc").numFormat("0.00%"));
			out.endStyles();
			
			out.beginSheet("experiments", 58);
			
			out.beginRow();
			out.addString("hdr", "app");
			out.addString("hdr", "n7");
			out.addString("hdr", "n15");
			out.addString("hdr", "n");
			out.addString("hdr", "TN");
			out.addString("hdr", "T1");
			out.addString("hdr", "SP1");
			out.addString("hdr", "p1");
			out.addString("hdr", "a15");
			out.addString("hdr", "p_ave");
			out.addString("hdr", "p_d");
			out.endRow();
			
			int n1row = 0;
			int a7row = 0;
			int a15row = 0;
			int row = 2;
			int t1 = 0;
			int timeA7 = 0;
			int timeA15 = 0;
			double[] p = new double[3];
			
			for(BenchmarkResult res : times.results) {
				int n = res.n7+res.n15;
				boolean hom = res.n7==0 || res.n15==0;
				if(n==1) {
					n1row = row;
					if(res.n7==1)
						a7row = row;
					else if(res.n15==1)
						a15row = row;
				}
				
				out.beginRow();
				out.addString(res.app);
				out.addNumber(res.n7);
				out.addNumber(res.n15);
				out.addFormula("=RC[-1]+RC[-2]"); // n
				out.addNumber("bgy", res.total);
				
				if(hom) {
					// T1
					if(res.n7==1)
						timeA7 = res.total;
					else if(res.n15==1)
						timeA15 = res.total;
					out.addFormula(String.format("=R%dC[-1]", n1row));
					
					out.addFormula("num4bgb", "=RC[-1]/RC[-2]"); // SP1
					// p1
					if(n==1) {
						out.skipCells(1);
						t1 = res.total;
					}
					else {
						double sp = (double)t1/(double)res.total;
						p[n-2] = (double)n * (1.0/sp-1.0)	/ (1.0-(double)n);
						System.out.printf("n=%d: SP=%.4f, p=%.4f\n", n, sp, p[n-2]);
						out.addFormula("num4", "=RC4*(1/RC7-1)/(1-RC4)"); // p1
					}
					// a15
					if(res.n15==0)
						out.addNumber("num4", 1.0);
					else {
						out.addFormula("num4", String.format("=R%dC5/R%dC5", a7row, a15row));
					}
					
					if(n==4) {
						out.addFormula("num4", "=AVERAGE(R[-2]C[-2]:RC[-2])"); // p_ave
						out.addFormula("num4", "=MAX(MAX(R[-2]C[-3]:RC[-3])-RC[-1], RC[-1]-MIN(R[-2]C[-3]:RC[-3]))"); // p_d
						
						double pave = 0.0;
						double pmin = 0.0;
						double pmax = 0.0;
						for(int i=0; i<3; i++) {
							pave += p[i];
							if(i==0 || p[i]<pmin)
								pmin = p[i];
							if(i==0 || p[i]<pmax)
								pmax = p[i];
						}
						pave /= 3.0;
						
						ModeCharacteristics mchar = modes.get(res.app);
						if(mchar==null) {
							mchar = new ModeCharacteristics();
							modes.put(res.app, mchar);
						}
						if(res.n7>0) {
							mchar.pA7 = pave;
							mchar.dpA7 = Math.max(pmax-pave, pave-pmin);
						}
						else if(res.n15>0) {
							mchar.timeA7 = timeA7;
							mchar.a15 = (double)timeA7 / (double)timeA15;
							mchar.pA15 = pave;
							mchar.dpA15 = Math.max(pmax-pave, pave-pmin);
							mchar.pmin = Math.min(mchar.pA7-mchar.dpA7, mchar.pA15-mchar.dpA15);
							mchar.pmax = Math.max(mchar.pA7+mchar.dpA7, mchar.pA15+mchar.dpA15);

							System.out.printf("\n%s:\na15: %.4f\n", res.app, mchar.a15);
							System.out.printf("A7: %.4f \u00b1 %.4f\n", mchar.pA7, mchar.dpA7);
							System.out.printf("A15: %.4f \u00b1 %.4f\n", mchar.pA15, mchar.dpA15);
							System.out.printf("p = [%.4f, %.4f]\n\n", mchar.pmin, mchar.pmax);
						}
					}
				}
				
				out.endRow();
				row++;
			}

			out.endSheet();
			
			out.beginSheet("minmax", 58);

			out.beginRow();
			out.skipCells(4);
			out.addString("min");
			out.skipCells(3);
			out.addString("max");
			out.endRow();

			out.beginRow();
			out.addString("hdr", "app");
			out.addString("hdr", "n7");
			out.addString("hdr", "n15");
			out.addString("hdr", "n");

			out.addString("hdr", "p");
			out.addString("hdr", "a15");
			out.addString("hdr", "Na");
			out.addString("hdr", "SP_min");
			out.addString("hdr", "p");
			out.addString("hdr", "a15");
			out.addString("hdr", "Na");
			out.addString("hdr", "SP_max");
			out.addString("hdr", "minmax");

			out.addString("hdr", "T1");
			out.addString("hdr", "TN");
			out.addString("hdr", "SP_meas");
			out.addString("hdr", "q_max");
			out.addString("hdr", "q_minmax");
			out.endRow();

			for(BenchmarkResult res : times.results) {
				if(res.n7==0 || res.n15==0)
					continue;
				ModeCharacteristics mchar = modes.get(res.app);
				
				out.beginRow();
				out.addString(res.app);
				out.addNumber(res.n7);
				out.addNumber(res.n15);
				out.addFormula("=RC[-1]+RC[-2]"); // n
				
				// min
				out.addNumber("num4", mchar.pmin);
				out.addNumber("num4", 1.0);
				out.addFormula("num4", "=RC4"); // Na
				out.addFormula("num4bgb", "=1/((1-RC[-3])/RC[-2]+RC[-3]/RC[-1])"); // SP

				// max
				out.addNumber("num4", mchar.pmax);
				out.addNumber("num4", mchar.a15);
				out.addFormula("num4", "=RC2+RC3*RC[-1]"); // Na
				out.addFormula("num4bgb", "=1/((1-RC[-3])/RC[-2]+RC[-3]/RC[-1])"); // SP
				out.addFormula("num2", "=RC8/RC12"); // minmax

				out.addNumber(mchar.timeA7);
				out.addNumber("bgy", res.total);
				out.addFormula("num4bgb", "=RC[-2]/RC[-1]"); // SP
				out.addFormula("num2", "=RC[-1]/RC12"); // q_max
				out.addFormula("num2bgp", "=(RC[-2]-RC8)/(RC12-RC8)"); // q_minmax

				out.endRow();
			}
			
			out.endSheet();
			
			out.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

}
