package ncl.cs.prime.diagrams;

import java.io.IOException;
import java.io.PrintWriter;

import ncl.cs.prime.diagrams.Data.Formula;
import ncl.cs.prime.diagrams.Data.Row;

public class DiagramsCL {

	public static final String PATH = "../opencl/results/csv";
	public static final String[] LAWS = {"amd", "bal", "gus", "guspar"};
	public static final String[] MODES = {"sqrt", "int", "log"};
	public static final double[][][] RANGES = {
		{{0.5, 512}, {0.5, 512}, {0.125, 256}},
		{{0.5, 1024}, {0.5, 1024}, {0.125, 256}},
		{{0.5, 16384}, {0.5, 8192}, {0.125, 256}},
		{{0.5, 16384}, {0.5, 8192}, {0.125, 256}},
	};

	public static final double[][] GUS_CMP_RANGES = {
		{0.5, 256}, {0.5, 256}, {0.125, 256},
	};

	public static boolean speedup = true;
	public static boolean balCompare = true;
	public static boolean gusCompare = true;
	
	private static class Diff implements Formula {
		public final String cola, colb;
		public Diff(String cola, String colb) {
			this.cola = cola;
			this.colb = colb;
		}
		@Override
		public String calc(Row row) {
			return String.format("%.0f%%", (row.getDouble(colb)-row.getDouble(cola))*100.0/row.getDouble(cola));
		}
	}

	private static PrintWriter out;
	
	private static BarChart createSpeedupChart(Data data, String mode, final double p, final int z, String zName, double min, double max) {
		Data d = new Data(data, new Data.Filter() {
				@Override
				public boolean accept(Data.Row row) {
					return row.getDouble("p")==p && row.getInt("z")==z && !(row.getInt("n1")==0 && row.getInt("n2")==0);
				}
			});
		String title = String.format("%s, p=%.1f, s=%s", mode, p, zName);
		BarChart ch = new BarChart(title, d)
			.setXAxisLabels("IntGPU:n1", "Nvidia:n2")
			.setBars("theory:SP_law", "measured:SP_meas")
			.setLogRange(2.0, min, max, 2.0)
			.setLabelBar(0, "err");
		if(min<0.5)
			ch.yAxisFormat = "%.3f";
		return ch;
	}
	
	private static BarChart createSPCompareChart(Data data, String mode, final double p, final int z, String zName, double min, double max) {
		Data d = new Data(data, new Data.Filter() {
				@Override
				public boolean accept(Data.Row row) {
					return row.getDouble("a.p")==p && row.getInt("a.z")==z && !(row.getInt("a.n1")==0 && row.getInt("a.n2")==0);
				}
			});
		String title = String.format("Speedup: %s, p=%.1f, s=%s", mode, p, zName);
		BarChart ch = new BarChart(title, d)
			.setXAxisLabels("IntGPU:a.n1", "Nvidia:a.n2")
			.setBars("equal-share:a.SP_meas", "balanced:b.SP_meas")
			.setLogRange(2.0, min, max, 2.0)
			.setLabelBar(1, "diff");
		if(min<0.5)
			ch.yAxisFormat = "%.3f";
		return ch;
	}
	
	public static void main(String[] args) {
		try {
			// Speedup diagrams
			if(speedup) {
				for(int law=0; law<LAWS.length; law++) {
					BarChart[] charts = new BarChart[MODES.length*4];
					
					for(int m=0; m<MODES.length; m++) {
						String mode = MODES[m];
						Data data = new Data(String.format("%s/%s_%s.csv", PATH, LAWS[law], mode));
						charts[m*4+0] = createSpeedupChart(data, mode, 0.9, 1, "IntGPU", RANGES[law][m][0], RANGES[law][m][1]);
						charts[m*4+1] = createSpeedupChart(data, mode, 0.9, 2, "Nvidia", RANGES[law][m][0], RANGES[law][m][1]);
						charts[m*4+2] = createSpeedupChart(data, mode, 0.9, 0, "CPU", RANGES[law][m][0], RANGES[law][m][1]);
						charts[m*4+3] = createSpeedupChart(data, mode, 0.3, 0, "CPU", RANGES[law][m][0], RANGES[law][m][1]);
					}
					
					out = BarChart.startSvg(String.format("%s/%s.svg", PATH, LAWS[law]));
					BarChart.layoutCharts(out, charts, 2);
					BarChart.finishSvg(out);
				}
			}
			
			// Balanced speedup comparison
			if(balCompare) {
				BarChart[] charts = new BarChart[MODES.length];
				
				for(int m=0; m<MODES.length; m++) {
					String mode = MODES[m];
					Data dataAmd = new Data(String.format("%s/%s_%s.csv", PATH, "amd", mode));
					Data dataBal = new Data(String.format("%s/%s_%s.csv", PATH, "bal", mode));
					Data data = new Data(dataAmd, dataBal, new String[] {"m", "p", "z", "n0", "n1", "n2"}, "a.", "b.");
					data.addCol("diff", new Diff("a.SP_meas", "b.SP_meas"));
					charts[m] = createSPCompareChart(data, mode, 0.9, 0, "CPU", RANGES[1][m][0], RANGES[1][m][1]);
				}

				out = BarChart.startSvg(String.format("%s/bal_compare.svg", PATH));
				BarChart.layoutCharts(out, charts, 1);
				BarChart.finishSvg(out);
			}
			
			// Gustafson speedup comparison
			if(gusCompare) {
				BarChart[] charts = new BarChart[MODES.length];
				
				for(int m=0; m<MODES.length; m++) {
					String mode = MODES[m];
					Data dataAmd = new Data(String.format("%s/%s_%s.csv", PATH, "amd", mode));
					Data dataBal = new Data(String.format("%s/%s_%s.csv", PATH, "bal", mode));
					Data data = new Data(dataAmd, dataBal, new String[] {"m", "p", "z", "n0", "n1", "n2"}, "a.", "b.");
					data.addCol("diff", new Diff("a.SP_meas", "b.SP_meas"));
					charts[m] = createSPCompareChart(data, mode, 0.3, 0, "CPU", GUS_CMP_RANGES[m][0], GUS_CMP_RANGES[m][1])
						.setBars("classical scaling:a.SP_meas", "purely parallel scaling:b.SP_meas");
				}

				out = BarChart.startSvg(String.format("%s/gus_compare.svg", PATH));
				BarChart.layoutCharts(out, charts, 1);
				BarChart.finishSvg(out);
			}
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

}
