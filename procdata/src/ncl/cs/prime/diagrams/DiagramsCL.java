package ncl.cs.prime.diagrams;

import java.io.IOException;
import java.io.PrintWriter;

import ncl.cs.prime.diagrams.Data.Row;

public class DiagramsCL {

	public static final String PATH = "../opencl/results/csv";
	public static final String[] LAWS = {"amd", "bal", "gus", "guspar"};
	public static final String[] MODES = {"sqrt", "int", "log"};
	public static final double[][][] RANGES = {
		{{0.5, 512}, {0.5, 512}, {0.125, 256}},
		{{0.5, 512}, {0.5, 512}, {0.125, 256}},
		{{0.5, 16384}, {0.5, 8192}, {0.125, 256}},
		{{0.5, 16384}, {0.5, 8192}, {0.125, 256}},
	};
	
	public static boolean speedup = true;
	public static boolean balCompare = false;
	
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
			.setXAxisLabels("IntGPU:n1", "NVidia:n2")
			.setBars("theory:SP_law", "measured:SP_meas")
			.setLogRange(2.0, min, max, 2.0)
			.setLabelBar(0, "err");
		if(min<0.5)
			ch.yAxisFormat = "%.3f";
		return ch;
	}
	
	private static BarChart createBalCompareChart(Data data, String mode, final double p, final int z, String zName, double min, double max) {
		Data d = new Data(data, new Data.Filter() {
				@Override
				public boolean accept(Data.Row row) {
					return row.getDouble("amd.p")==p && row.getInt("amd.z")==z && !(row.getInt("amd.n1")==0 && row.getInt("amd.n2")==0);
				}
			});
		String title = String.format("%s, p=%.1f, s=%s", mode, p, zName);
		BarChart ch = new BarChart(title, d)
			.setXAxisLabels("IntGPU:amd.n1", "NVidia:amd.n2")
			.setBars("equal-share:amd.SP_meas", "balanced:bal.SP_meas")
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
					Data data = new Data(dataAmd, dataBal, new String[] {"m", "p", "z", "n0", "n1", "n2"}, "amd.", "bal.");
					data.addCol("diff", new Data.Formula() {
						@Override
						public String calc(Row row) {
							return String.format("%.0f%%", (row.getDouble("bal.SP_meas")-row.getDouble("amd.SP_meas"))*100.0/row.getDouble("amd.SP_meas"));
						}
					});
					charts[m] = createBalCompareChart(data, mode, 0.9, 0, "CPU", RANGES[1][m][0], RANGES[1][m][1]);
				}

				out = BarChart.startSvg(String.format("%s/bal_compare.svg", PATH));
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
