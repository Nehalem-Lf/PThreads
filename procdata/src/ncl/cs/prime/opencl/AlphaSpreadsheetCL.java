package ncl.cs.prime.opencl;

import java.io.IOException;

import ncl.cs.prime.opencl.CollectTimesCL.BenchmarkResult;
import ncl.cs.prime.oxml.SpreadsheetGenerator;
import ncl.cs.prime.oxml.SpreadsheetGenerator.CellStyle;

public class AlphaSpreadsheetCL {

	public static final String PATH = "../opencl/results";
	public static final String SRC = PATH+"/multicl_alpha3.log";
	public static final String OUTPUT = PATH+"/alpha3.xml";
	
	public static final String[] M = {"sqrt", "int", "log"};

	public static String[] DEVS = {"CPU", "IntGPU", "Nvidia", "IntGPU 16+"};
	
	public static void main(String[] args) {
		try {
			CollectTimesCL times = new CollectTimesCL(SRC);
			int[][] sortedWL = new int[M.length][DEVS.length];
			double[][][] sortedTimes = new double[M.length][DEVS.length][11];
			for(BenchmarkResult res : times.results) {
				int c = res.n[res.z];
				int cd = 0;
				while(c>1) {
					c >>= 1;
					cd++;
				}
				int dev = res.z;
				if(dev==1 && res.n[1]>=16)
					dev = 3;
				sortedTimes[res.m][dev][cd] = res.total;
				sortedWL[res.m][dev] = res.w;
			}
			
			SpreadsheetGenerator out = new SpreadsheetGenerator(OUTPUT, "generated");
			
			CellStyle defStyle = new CellStyle(null).align("Bottom", "Right").font("Verdana", 8);
			out.beginStyles(defStyle);
			out.addStyle(new CellStyle("hdr", defStyle).bold());
			out.addStyle(new CellStyle("hdry", defStyle).bold().bgColor("#FFCC22"));
			out.addStyle(new CellStyle("bgy").bgColor("#FFFFCC"));
			out.addStyle(new CellStyle("bgs0").bgColor("#DDDDDD"));
			out.addStyle(new CellStyle("bgs1").bgColor("#CCCCCC"));
			out.addStyle(new CellStyle("num5").numFormat("0.00000"));
			out.addStyle(new CellStyle("num5bgs0").bgColor("#DDDDDD").numFormat("0.00000"));
			out.addStyle(new CellStyle("num5bgs1").bgColor("#CCCCCC").numFormat("0.00000"));
			out.endStyles();
			
			out.beginSheet("alpha", 50);
			
			out.beginRow();
			out.addString("hdr", "m");
			for(int m=0; m<M.length; m++)
				for(int d=0; d<DEVS.length; d++)
					out.addString(m%2==0 ? "bgs0" : "bgs1", M[m]);
			out.endRow();
			
			out.beginRow();
			out.addString("hdr", "dev_id");
			for(int m=0; m<M.length; m++)
				for(int d=0; d<DEVS.length; d++) {
					if(d<=2)
						out.addNumber(d);
					else
						out.addString("1*");
				}
			out.endRow();
			
			out.beginRow();
			out.addString("hdr", "dev");
			for(int m=0; m<M.length; m++)
				for(int d=0; d<DEVS.length; d++)
					out.addString(DEVS[d]);
			out.endRow();
			
			out.beginRow();
			out.addString("hdr", "work");
			for(int m=0; m<M.length; m++)
				for(int d=0; d<DEVS.length; d++)
					out.addNumber(sortedWL[m][d]);
			out.endRow();
			
			out.beginRow();
			out.addString("hdr", "cores");
			out.endRow();

			int c = 1;
			for(int i=0; i<=10; i++) {
				out.beginRow();
				out.addNumber(c);
				for(int m=0; m<M.length; m++)
					for(int d=0; d<DEVS.length; d++) {
						if(sortedTimes[m][d][i]>0.0)
							out.addNumber("num5", sortedTimes[m][d][i]);
						else
							out.addEmpty();
					}
				out.endRow();
				c <<= 1;
			}
			
			out.beginRow();
			out.addString("hdry", "alpha");
			out.endRow();
			
			for(int i=0; i<=2; i++) {
				out.beginRow();
				out.addString("bgy", String.format("BCE%d", i));
				for(int m=0; m<M.length; m++)
					for(int d=0; d<DEVS.length; d++) {
						int col = m*4+i+2;
						out.addFormula("num5", String.format("=(R4C/R4C%d)*(R6C%d/R%dC)", col, col, (d==3) ? 10 : 6));
					}
				out.endRow();
			}
			
			out.skipRow();
			out.beginRow();
			out.addString("hdr", "average");
			for(int m=0; m<M.length; m++)
				for(int d=0; d<DEVS.length; d++)
					out.addFormula(m%2==0 ? "bgs0" : "bgs1", "=AVERAGE(R6C:R16C)");
			out.endRow();
			
			out.beginRow();
			out.addString("hdry", "alpha");
			out.endRow();
			
			for(int i=0; i<=2; i++) {
				out.beginRow();
				out.addString("bgy", String.format("BCE%d", i));
				for(int m=0; m<M.length; m++)
					for(int d=0; d<DEVS.length; d++) {
						int col = m*4+i+2;
						out.addFormula("num5", String.format("=(R4C/R4C%d)*(R[%d]C%d/R[%d]C)", col, -2-i, col, -2-i));
					}
				out.endRow();
			}
			
/*			out.addString("hdr", "dev_id");
			out.addString("hdr", "mode");
			out.addString("hdr", "alpha");
			out.endRow();
			for(int d=0; d<DEVS.length; d++) {
				out.beginRow();
				out.addFewEmpty(6);
				String cs = null;
				String csnum = "num4";
				if(d==BCE) {
					cs = "bgp";
					csnum = "num4bgp";
					out.addString(cs, "BCE");
				}
				else {
					out.addEmpty();
				}
				out.addString(cs, DEVS[d]);
				if(d<=2)
					out.addNumber(cs, d);
				else
					out.addString(cs, "1*");
				out.addString(cs, M[MODE]);
				out.addNumber(csnum, ALPHA[MODE][d]);
				out.endRow();
			}
			out.skipRow();
			
			out.beginRow();
			out.addString("hdr", "m");
			out.addString("hdr", "p");
			out.addString("hdr", "z");
			out.addString("hdr", "n0");
			out.addString("hdr", "n1");
			out.addString("hdr", "n2");
			out.addString("hdr", "n");
			out.addString("hdr", "aS");
			out.addString("hdr", "a_min");
			out.addString("hdr", "Na");
			out.addString("hdr", "SP_law");
			out.addString("hdr", "T1");
			out.addString("hdr", "TN");
			out.addString("hdr", "j");
			out.addString("hdr", "g");
			out.addString("hdr", "SP_meas");
			out.addString("hdr", "err");
			out.endRow();
			
			for(BenchmarkResult res : times.results) {
				out.beginRow();
				out.addString(M[res.m]);
				out.addNumber("bga", res.p);
				out.addNumber(res.z);
				out.addNumber(res.n[0]);
				out.addNumber(res.n[1]);
				out.addNumber(res.n[2]);
				
				out.addFormula("bga","=RC[-3]+RC[-2]+RC[-1]");
				out.addFormula("num4", String.format("=R%dC11", res.z+2));
				
				// a_min
				double amin = 0.0;
				int dmin = -1;
				for(int d=0; d<=2; d++) {
					if(res.n[d]>0) {
						int dev = getDev(d, res.n);
						if(dmin<0 || ALPHA[res.m][dev]<amin) {
							amin = ALPHA[res.m][dev];
							dmin = dev;
						}
					}
				}
				out.addFormula("num4", String.format("=R%dC11", dmin+2));
				
				// Na
				if(BALANCED)
					out.addFormula("num2", String.format("=RC4*R2C11+RC5*R%dC11+RC6*R4C11", getDev(1, res.n)+2)); 
				else
					out.addFormula("num2", "=RC[-3]*RC[-1]");
				
				// SP_law
				switch(WORKLOAD) {
					case amd:
						out.addFormula("num4bgb", "=1/((1-RC2)/RC[-3]+RC2/RC[-1])");
						break;
					case gusProp:
						out.addFormula("num4bgb", "=(1-RC2)*RC[-3]+RC2*RC[-1]");
						break;
					case gusPar:
						out.addFormula("num4bgb", "=(1-RC2)+RC[-1]*(1-(1-RC2)/RC[-3])");
						break;
				}
				
				out.addFormula("num2", String.format("=R%dC[+1]", 4+DEVS.length)); // T1 
				out.addNumber("num2bgy", res.total);
				out.addNumber(res.j);
				out.addFormula("=RC[-1]*RC4"); // g 
				out.addFormula("num4bgb", "=RC[-4]/RC[-3]*((1-RC2)+RC2*RC[-2]*RC7)"); // SP_meas 
				out.addFormula("pc", "=(RC[-6]-RC[-1])/RC[-1]"); // err 
				
				out.endRow();
			}*/
			
			out.endSheet();
			
			out.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

}
