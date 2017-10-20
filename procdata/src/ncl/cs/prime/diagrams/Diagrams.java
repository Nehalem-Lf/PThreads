package ncl.cs.prime.diagrams;

import java.io.IOException;
import java.io.PrintWriter;

import ncl.cs.prime.diagrams.Data.Row;

public class Diagrams {

	public static final String PATH = "../odroid/results";
	public static final String[] LAWS = {"amd", "bal", "gus", "guspar"};
	public static final String[] MODES = {"sqrt", "int", "log"};
	public static final double[][][] SP_RANGES = {
		{{3.0, 0.5}, {6.0, 1.0}},
		{{3.0, 0.5}, {8.0, 1.0}},
		{{4.0, 0.5}, {8.0, 1.0}},
		{{6.0, 0.5}, {8.0, 1.0}},
	};
	public static final double[][][] PWR_RANGES = {
			{{1.4, 0.2}, {2.5, 0.5}},
			{{1.4, 0.2}, {3.0, 0.5}},
			{{2.0, 0.5}, {3.0, 0.5}},
			{{2.5, 0.5}, {3.0, 0.5}},
		};
	public static final double[][][] EDP_RANGES = {
			{{2000, 200}, {2000, 200}},
			{{2000, 200}, {2000, 200}},
		};
	
	public static boolean speedup = false;
	public static boolean power = true;
	public static boolean balCompare = false;
	
	private static PrintWriter out;
	
	private static BarChart createSpeedupChart(Data data, final String mode, final double p, double max, double step) {
		Data d = new Data(data, new Data.Filter() {
				@Override
				public boolean accept(Data.Row row) {
					return row.getDouble("p")==p && row.get("m").equals(mode);
				}
			});
		String title = String.format("%s, p=%.1f", mode, p);
		BarChart ch = new BarChart(title, d)
			.setXAxisLabels("A7:n7", "A15:n15")
			.setBars("theory:SP_law", "measured:SP_meas")
			.setRange(0.0, max, step)
			.setLabelBar(0, "err");
		return ch;
	}
	
	private static BarChart createPowerChart(Data data, final String mode, final double p, double max, double step) {
		BarChart ch = createSpeedupChart(data, mode, p, max, step)
			.setBars("theory:Wtotal", "measured:Wmeas")
			.setBarColors(3, 4)
			.setLabelBar(0, "W_err");
		return ch;
	}
	
	private static BarChart createBalSPCompareChart(Data data, final String mode, final double p, double max, double step) {
		Data d = new Data(data, new Data.Filter() {
				@Override
				public boolean accept(Data.Row row) {
					return row.getDouble("amd.p")==p && row.get("amd.m").equals(mode);
				}
			});
		String title = String.format("%s, p=%.1f", mode, p);
		BarChart ch = new BarChart(title, d)
			.setXAxisLabels("A7:amd.n7", "A15:amd.n15")
			.setBars("equal-share:amd.SP_meas", "balanced:bal.SP_meas")
			.setRange(0.0, max, step)
			.setLabelBar(1, "diff");
		return ch;
	}
	
	private static BarChart createBalPwrCompareChart(Data data, final String mode, final double p, double max, double step) {
		BarChart ch = createBalSPCompareChart(data, mode, p, max, step)
			.setBars("equal-share:amd.Wmeas", "balanced:bal.Wmeas")
			.setBarColors(3, 4)
			.setLabelBar(1, "W_diff");
		return ch;
	}
	
	private static BarChart createBalEDPCompareChart(Data data, final String mode, final double p, double max, double step) {
		BarChart ch = createBalSPCompareChart(data, mode, p, max, step)
			.setBars("equal-share:amd.EDP", "balanced:bal.EDP")
			.setBarColors(5, 6)
			.setLabelBar(1, "EDP_diff");
		return ch;
	}
	
	public static void main(String[] args) {
		try {
			// Speedup diagrams
			if(speedup) {
				for(int law=0; law<LAWS.length; law++) {
					BarChart[] charts = new BarChart[MODES.length*2];
					
					Data data = new Data(String.format("%s/%s.csv", PATH, LAWS[law]));
					for(int m=0; m<MODES.length; m++) {
						String mode = MODES[m];
						charts[m*2] = createSpeedupChart(data, mode, 0.3, SP_RANGES[law][0][0], SP_RANGES[law][0][1]);
						charts[m*2+1] = createSpeedupChart(data, mode, 0.9, SP_RANGES[law][1][0], SP_RANGES[law][1][1]);
					}
					
					out = BarChart.startSvg(String.format("%s/%s.svg", PATH, LAWS[law]));
					BarChart.layoutCharts(out, charts, 2);
					BarChart.finishSvg(out);
				}
			}
			
			// Power diagrams
			if(power) {
				for(int law=0; law<LAWS.length; law++) {
					BarChart[] charts = new BarChart[MODES.length*2];
					
					Data data = new Data(String.format("%s/%s.csv", PATH, LAWS[law]));
					for(int m=0; m<MODES.length; m++) {
						String mode = MODES[m];
						charts[m*2] = createPowerChart(data, mode, 0.3, PWR_RANGES[law][0][0], PWR_RANGES[law][0][1]);
						charts[m*2+1] = createPowerChart(data, mode, 0.9, PWR_RANGES[law][1][0], PWR_RANGES[law][1][1]);
					}
					
					out = BarChart.startSvg(String.format("%s/%s_pwr.svg", PATH, LAWS[law]));
					BarChart.layoutCharts(out, charts, 2);
					BarChart.finishSvg(out);
				}
			}
			
			// Balanced speedup comparison
			if(balCompare) {
				BarChart[] charts = new BarChart[6];
				
				Data dataAmd = new Data(String.format("%s/%s.csv", PATH, "amd"));
				Data dataBal = new Data(String.format("%s/%s.csv", PATH, "bal"));
				Data data = new Data(dataAmd, dataBal, new String[] {"m", "p", "n7", "n15"}, "amd.", "bal.");
				data.addCol("diff", new Data.Formula() {
					@Override
					public String calc(Row row) {
						return String.format("%.0f%%", (row.getDouble("bal.SP_meas")-row.getDouble("amd.SP_meas"))*100.0/row.getDouble("amd.SP_meas"));
					}
				});
				data.addCol("W_diff", new Data.Formula() {
					@Override
					public String calc(Row row) {
						return String.format("%.0f%%", (row.getDouble("bal.Wmeas")-row.getDouble("amd.Wmeas"))*100.0/row.getDouble("amd.Wmeas"));
					}
				});
				data.addCol("EDP_diff", new Data.Formula() {
					@Override
					public String calc(Row row) {
						return String.format("%.0f%%", (row.getDouble("bal.EDP")-row.getDouble("amd.EDP"))*100.0/row.getDouble("amd.EDP"));
					}
				});
				for(int m=1; m<MODES.length; m++) {
					String mode = MODES[m];
					charts[m-1] = createBalSPCompareChart(data, mode, 0.9, SP_RANGES[1][1][0], SP_RANGES[1][1][1]);
					charts[2+m-1] = createBalPwrCompareChart(data, mode, 0.9, PWR_RANGES[1][1][0], PWR_RANGES[1][1][1]);
					charts[4+m-1] = createBalEDPCompareChart(data, mode, 0.9, EDP_RANGES[1][1][0], EDP_RANGES[1][1][1]);
				}

				out = BarChart.startSvg(String.format("%s/bal_compare.svg", PATH));
				BarChart.layoutCharts(out, charts, 2);
				BarChart.finishSvg(out);
			}
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

}
