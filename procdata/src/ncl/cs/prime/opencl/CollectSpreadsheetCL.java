package ncl.cs.prime.opencl;

import java.io.IOException;

import ncl.cs.prime.opencl.CollectTimesCL.BenchmarkResult;
import ncl.cs.prime.oxml.SpreadsheetGenerator;
import ncl.cs.prime.oxml.SpreadsheetGenerator.CellStyle;

public class CollectSpreadsheetCL {

	public static final String PATH = "../opencl/results/171017_2";
	public static final String SRC = PATH+"/multicl_guspar_logs3.log";
	public static final String OUTPUT = PATH+"/guspar_logs3.xml";
	
	public static final String[] M = {"sqrt", "int", "log"};
	public static enum Workload { amd, gusProp, gusPar };

	public static boolean BALANCED = false;
	public static Workload WORKLOAD = Workload.gusPar;

	public static String[] DEVS = {"CPU", "IntGPU", "Nvidia", "IntGPU 16+"};
	public static int BCE = 1;
	public static double[][] ALPHA = {
		{24.3514350982078, 1.0, 14.9801660268563, 0.768866737195047},
		{40.667109080911, 1.0, 7.88676615499091, 0.769127042087032},
		{42.6939047915327, 1.0, 0.246829649846585, 0.856323537072923},
	};
	
	private static int getDev(int d, int[] n) {
		return (d==1 && n[1]>=16) ? 3 : d; 
	}
	
	public static void main(String[] args) {
		try {
			CollectTimesCL times = new CollectTimesCL(SRC);
			int mode = times.results.get(0).m;
			
			SpreadsheetGenerator out = new SpreadsheetGenerator(OUTPUT, "generated");
			
			CellStyle defStyle = new CellStyle(null).align("Bottom", "Right").font("Verdana", 8);
			out.beginStyles(defStyle);
			out.addStyle(new CellStyle("hdr", defStyle).bold());
			out.addStyle(new CellStyle("bgb").bgColor("#99CCFF"));
			out.addStyle(new CellStyle("bgp").bgColor("#DDDDFF"));
			out.addStyle(new CellStyle("bga").bgColor("#CCFFFF"));
			out.addStyle(new CellStyle("bgy").bgColor("#FFFFCC"));
			out.addStyle(new CellStyle("num2").numFormat("0.00"));
			out.addStyle(new CellStyle("num4").numFormat("0.0000"));
			out.addStyle(new CellStyle("num2bgy").bgColor("#FFFFCC").numFormat("0.00"));
			out.addStyle(new CellStyle("num4fade", defStyle).textColor("#999999").numFormat("0.0000"));
			out.addStyle(new CellStyle("num4bgb").bgColor("#99CCFF").numFormat("0.0000"));
			out.addStyle(new CellStyle("num4bgo").bgColor("#FFCC99").numFormat("0.0000"));
			out.addStyle(new CellStyle("num4bgp").bgColor("#DDDDFF").numFormat("0.0000"));
			out.addStyle(new CellStyle("num4bga").bgColor("#CCFFFF").numFormat("0.0000"));
			out.addStyle(new CellStyle("pc").numFormat("0.00%"));
			out.endStyles();
			
			out.beginSheet("calc+compare", 50);
			
			out.beginRow();
			out.addFewEmpty(7);
			out.addString("hdr", "dev");
			out.addString("hdr", "dev_id");
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
				out.addString(cs, M[mode]);
				out.addNumber(csnum, ALPHA[mode][d]);
				out.endRow();
			}
			out.skipRow();
			
			out.beginRow();
			out.addString("hdr", "m");
			out.addString("hdr", "w");
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
				if(res.m==2 && res.n[2]>64)
					continue;
				out.beginRow();
				out.addString(M[res.m]);
				out.addNumber(res.w);
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
					out.addFormula("num2", String.format("=RC5*R2C11+RC6*R%dC11+RC7*R4C11", getDev(1, res.n)+2)); 
				else
					out.addFormula("num2", "=RC[-3]*RC[-1]");
				
				// SP_law
				switch(WORKLOAD) {
					case amd:
						out.addFormula("num4bgb", "=1/((1-RC3)/RC[-3]+RC3/RC[-1])");
						break;
					case gusProp:
						out.addFormula("num4bgb", "=(1-RC3)*RC[-3]+RC3*RC[-1]");
						break;
					case gusPar:
						out.addFormula("num4bgb", "=(1-RC3)+RC[-1]*(1-(1-RC3)/RC[-3])");
						break;
				}
				
				out.addFormula("num2", String.format("=R%dC[+1]", 4+DEVS.length)); // T1 
				out.addNumber("num2bgy", res.total);
				out.addNumber(res.j);
				out.addFormula("num2", "=RC[-1]*RC8"); // g 
				out.addFormula("num4bgb", "=(RC2/R8C2)*(RC[-4]/RC[-3])*((1-RC3)+RC3*RC[-2]*RC8)"); // SP_meas 
				out.addFormula("pc", "=(RC[-6]-RC[-1])/RC[-1]"); // err 
				
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
