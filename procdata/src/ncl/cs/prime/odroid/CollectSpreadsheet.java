package ncl.cs.prime.odroid;

import java.io.IOException;

import ncl.cs.prime.odroid.CollectTimes.BenchmarkResult;
import ncl.cs.prime.odroid.SpreadsheetGenerator.CellStyle;

public class CollectSpreadsheet {

	public static final String PATH = "../odroid";
	public static final String DATA_PATH = PATH+"/guspar";
	public static final String SRC = DATA_PATH+"/pthreads.log";
	public static final String OUTPUT = "guspar_results.xml";
	
	public static final String[] M = {"sqrt", "int", "log"};
	public static enum Workload { amd, gusProp, gusPar };
	public static final double[] FIXED_A15 = {0.9392, 1.2399, 1.7623};

	public static boolean BALANCED = false;
	public static Workload WORKLOAD = Workload.gusPar;

	private static class ModeCharacteristics {
		public int timeA7, timeA15;
		public double effA7, effA15, sdevA7, sdevA15;
		public boolean slowA15;
	}
	
	public static void main(String[] args) {
		try {
			CollectTimes times = new CollectTimes(SRC);
			MeanPower idlePower = new MeanPower(PATH+"/idle.csv", 1000, 0);
			ModeCharacteristics[] modes = new ModeCharacteristics[M.length];
			
			SpreadsheetGenerator out = new SpreadsheetGenerator(OUTPUT, "generated");
			
			CellStyle defStyle = new CellStyle(null).align("Bottom", "Right").font("Verdana", 8);
			out.beginStyles(defStyle);
			out.addStyle(new CellStyle("hdr", defStyle).bold());
			out.addStyle(new CellStyle("bgb").bgColor("#99CCFF"));
			out.addStyle(new CellStyle("bgp").bgColor("#DDDDFF"));
			out.addStyle(new CellStyle("bgs").bgColor("#DDDDDD"));
			out.addStyle(new CellStyle("bgy").bgColor("#FFFFCC"));
			out.addStyle(new CellStyle("num4").numFormat("0.0000"));
			out.addStyle(new CellStyle("num4fade", defStyle).textColor("#999999").numFormat("0.0000"));
			out.addStyle(new CellStyle("num4bgb").bgColor("#99CCFF").numFormat("0.0000"));
			out.addStyle(new CellStyle("num4bgo").bgColor("#FFCC99").numFormat("0.0000"));
			out.addStyle(new CellStyle("num4bgp").bgColor("#DDDDFF").numFormat("0.0000"));
			out.addStyle(new CellStyle("num4bgs").bgColor("#DDDDDD").numFormat("0.0000"));
			out.addStyle(new CellStyle("pc").numFormat("0.00%"));
			out.endStyles();
			
			out.beginSheet("experiments", 58);
			
			out.skipRow();
			out.beginRow();
			out.skipCells(7);
			out.addString("hdr", "a7idle");
			out.addString("hdr", "a15idle");
			out.addString("hdr", "totalidle");
			out.addString("hdr", "a7sdev");
			out.addString("hdr", "a15sdev");
			out.addString("hdr", "totasdev");
			out.endRow();
			out.beginRow();
			out.skipCells(7);
			out.addNumber("num4", idlePower.a7mean);
			out.addNumber("num4", idlePower.a15mean);
			out.addNumber("num4bgo", idlePower.totalMean);
			out.addNumber("pc", idlePower.a7sdev);
			out.addNumber("pc", idlePower.a15sdev);
			out.addNumber("pc", idlePower.totalSdev);
			out.endRow();
			
			out.skipRow();
			out.beginRow();
			out.addString("hdr", "m");
			out.addString("hdr", "p");
			out.addString("hdr", "n7");
			out.addString("hdr", "n15");
			out.addString("hdr", "n");
			out.addString("hdr", "j");
			out.addString("hdr", "totaltime");
			out.addString("hdr", "a7power");
			out.addString("hdr", "a15power");
			out.addString("hdr", "totalpower");
			out.addString("hdr", "a7sdev");
			out.addString("hdr", "a15sdev");
			out.addString("hdr", "totalsdev");
			out.addString("hdr", "a7eff");
			out.addString("hdr", "a15eff");
			out.addString("hdr", "totaleff");
			out.addString("hdr", "a7eff1");
			out.addString("hdr", "a15eff1");
			out.endRow();
			for(BenchmarkResult res : times.results) {
				res.calcPower(DATA_PATH);
				int n = res.n7+res.n15;
				String style = (n==1) ? "bgs" : null;
				out.beginRow();
				out.addString(style, M[res.m]);
				out.addNumber(style, res.p);
				out.addNumber(style, res.n7);
				out.addNumber(style, res.n15);
				out.addNumber(style, n);
				out.addNumber(res.j);
				out.addNumber("bgy", res.total);
				out.addNumber("num4", res.power.a7mean);
				out.addNumber("num4", res.power.a15mean);
				out.addNumber("num4bgp", res.power.totalMean);
				out.addNumber("pc", res.power.a7sdev);
				out.addNumber("pc", res.power.a15sdev);
				out.addNumber("pc", res.power.totalSdev);
				out.addFormula("num4", "=RC[-6]-R3C[-6]");
				out.addFormula("num4", "=RC[-6]-R3C[-6]");
				out.addFormula("num4bgp", "=RC[-6]-R3C[-6]");
				if(res.n7>0)
					out.addFormula(n==1 ? "num4bgs" : "num4fade", "=(RC[-9]-R3C[-9])/RC[-14]");
				else
					out.addEmpty(style);
				if(res.n15>0)
					out.addFormula(n==1 ? "num4bgs" : "num4fade", "=(RC[-9]-R3C[-9])/RC[-14]");
				else
					out.addEmpty(style);
				
				if(n==1) {
					if(modes[res.m]==null)
						modes[res.m] = new ModeCharacteristics();
					if(res.n7>0) {
						modes[res.m].timeA7 = res.total;
						modes[res.m].effA7 = res.power.a7mean - idlePower.a7mean;
						modes[res.m].sdevA7 = res.power.a7sdev;
					}
					else if(res.n15>0) {
						modes[res.m].timeA15 = res.total;
						modes[res.m].effA15 = res.power.a15mean - idlePower.a15mean;
						modes[res.m].sdevA15 = res.power.a15sdev;
					}
				}
				
				out.endRow();
			}
			
			out.endSheet();
			
			out.beginSheet("calc+compare", 50);
			
			out.skipRow();
			out.beginRow();
			out.addString("hdr", "m");
			out.addString("hdr", "timeA7");
			out.addString("hdr", "timeA15");
			out.addString("hdr", "a15");
			out.addString("hdr", "effA7");
			out.addString("hdr", "effA15");
			out.addString("hdr", "b15");
			out.addString("hdr", "sdevA7");
			out.addString("hdr", "sdevA15");
			out.addEmpty();
			out.addString("hdr", "a7idle");
			out.addString("hdr", "a15idle");
			out.addString("hdr", "totalidle");
			out.addString("hdr", "Ni");
			out.addString("hdr", "sdev");
			out.endRow();
			for(int m=0; m<M.length; m++) {
				out.beginRow();
				out.addString(M[m]);
				out.addNumber(modes[m].timeA7);
				if(WORKLOAD==Workload.gusPar) {
					out.addEmpty();
					out.addNumber("num4bgo", FIXED_A15[m]);
					modes[m].slowA15 = FIXED_A15[m] < 1.0;
				}
				else {
					out.addNumber(modes[m].timeA15);
					out.addFormula("num4bgo", "=RC[-2]/RC[-1]");
					modes[m].slowA15 = (modes[m].timeA7 / modes[m].timeA15) < 1.0;
				}
				out.addNumber("num4", modes[m].effA7);
				out.addNumber("num4", modes[m].effA15);
				out.addFormula("num4bgo", "=RC[-1]/RC[-2]");
				out.addNumber("pc", modes[m].sdevA7);
				out.addNumber("pc", modes[m].sdevA15);
				
				if(m==0) {
					out.addEmpty();
					out.addNumber("num4", idlePower.a7mean);
					out.addNumber("num4", idlePower.a15mean);
					out.addNumber("num4bgo", idlePower.totalMean);
					out.addFormula("num4", "=RC[-1]/RC[-3]");
					out.addNumber("pc", idlePower.totalSdev);
				}
				out.endRow();
			}
			
			out.skipRow();
			out.beginRow();
			out.skipCells(6);
			out.addFewEmpty(8, "bgb");
			out.addString("bgb", "SPEEDUP");
			out.addFewEmpty(7, "bgp");
			out.addString("bgp", "POWER");
			out.endRow();
			out.beginRow();
			out.addString("hdr", "m");
			out.addString("hdr", "p");
			out.addString("hdr", "aS");
			out.addString("hdr", "n");
			out.addString("hdr", "n7");
			out.addString("hdr", "n15");
			out.addString("hdr", "a_min");
			out.addString("hdr", "Na");
			out.addString("hdr", "SP_law");
			out.addString("hdr", "T1");
			out.addString("hdr", "TN");
			out.addString("hdr", "j");
			out.addString("hdr", "g");
			out.addString("hdr", "SP_meas");
			out.addString("hdr", "err");
			out.addString("hdr", "bS");
			out.addString("hdr", "Nb");
			out.addString("hdr", "PD_law");
			out.addString("hdr", "W1");
			out.addString("hdr", "WN");
			out.addString("hdr", "Wtotal");
			out.addString("hdr", "Wmeas");
			out.addString("hdr", "err");

			out.endRow();
			for(BenchmarkResult res : times.results) {
				out.beginRow();
				out.addString(M[res.m]);
				out.addNumber(res.p);
				
				// aS
				if(res.seqA15)
					out.addFormula("num4", String.format("=R%dC4", res.m+3));
				else
					out.addNumber("num4", 1.0);
				
				out.addNumber(res.n7+res.n15);
				out.addNumber(res.n7);
				out.addNumber(res.n15);
				
				// a_min
				if(modes[res.m].slowA15) {
					if(res.n15>0)
						out.addFormula("num4", String.format("=R%dC4", res.m+3)); 
					else
						out.addNumber("num4", 1.0);
				}
				else {
					if(res.n7==0)
						out.addFormula("num4", String.format("=R%dC4", res.m+3)); 
					else
						out.addNumber("num4", 1.0);
				}
				
				// Na
				if(BALANCED)
					out.addFormula("num4", String.format("=RC[-3]+RC[-2]*R%dC4", res.m+3)); 
				else
					out.addFormula("num4", "=RC4*RC7");
				
				// SP_law
				switch(WORKLOAD) {
					case amd:
						out.addFormula("num4bgb", "=1/((1-RC2)/RC3+RC2/RC[-1])");
						break;
					case gusProp:
						out.addFormula("num4bgb", "=(1-RC2)*RC3+RC2*RC[-1]");
						break;
					case gusPar:
						out.addFormula("num4bgb", "=(1-RC2)+RC[-1]*(1-(1-RC2)/RC3)");
						break;
				}
				
				out.addFormula(String.format("=R%dC2", res.m+3)); // T1 
				out.addNumber("bgy", res.total);
				out.addNumber(res.j);
				out.addFormula("=RC[-1]*RC4"); // g 
				out.addFormula("num4bgb", "=RC[-4]/RC[-3]*((1-RC2)+RC2*RC[-2]*RC4)"); // SP_meas 
				out.addFormula("pc", "=(RC[-6]-RC[-1])/RC[-1]"); // err 
				
				// bS
				if(res.seqA15)
					out.addFormula("num4", String.format("=R%dC7", res.m+3));
				else
					out.addNumber("num4", 1.0);
				
				// Nb
				if(BALANCED)
					out.addFormula("num4", String.format("=RC5+RC6*R%dC7", res.m+3));
				else
					out.addFormula("num4", String.format("=RC7*(RC5+RC6*R%dC7/R%dC4)", res.m+3, res.m+3));
				
				// PD_law
				switch(WORKLOAD) {
					case amd:
						out.addFormula("num4", "=((1-RC2)*RC[-2]/RC3+RC2*RC[-1]/RC8)");
						break;
					case gusProp:
						out.addFormula("num4", "=(RC[-2]*(1-RC2)+RC[-1]*RC2)/(RC3*(1-RC2)+RC8*RC2)");
						break;
					case gusPar:
						out.addFormula("num4", "=(RC[-2]*(1-RC2)+RC[-1]*(RC3-(1-RC2)))/(RC3*(1-RC2)+RC8*(RC3-(1-RC2)))");
						break;
				}

				out.addFormula("num4", String.format("=R%dC5", res.m+3)); // W1 
				out.addFormula("num4", "=RC[-1]*RC[-2]*RC9"); // WN 
				out.addFormula("num4bgp", "=RC[-1]+R3C13"); // Wtotal 
				out.addNumber("num4bgp", res.power.totalMean); 
				out.addFormula("pc", "=(RC[-2]-RC[-1])/RC[-1]"); // err 
				
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
