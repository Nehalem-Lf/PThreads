package ncl.cs.prime.diagrams;

import java.io.IOException;
import java.io.PrintWriter;

import ncl.cs.prime.diagrams.Data.Formula;
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
			{{2000, 200}, {2000 /*1400*/, 200}},
		};
	
	public static boolean speedup = false;
	public static boolean power = false;
	public static boolean balCompare = true;
	public static boolean gusCompare = false;
	
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
	
	private static BarChart createSpeedupChart(String name, Data data, final String mode, final double p, double max, double step) {
		Data d = new Data(data, new Data.Filter() {
				@Override
				public boolean accept(Data.Row row) {
					return row.getDouble("p")==p && row.get("m").equals(mode);
				}
			});
		String title = String.format("%s: %s, p=%.1f", name, mode, p);
		BarChart ch = new BarChart(title, d)
			.setXAxisLabels("A7:n7", "A15:n15")
			.setBars("theory:SP_law", "measured:SP_meas")
			.setRange(0.0, max, step)
			.setLabelBar(0, "err");
		return ch;
	}
	
	private static BarChart createPowerChart(String name, Data data, final String mode, final double p, double max, double step) {
		BarChart ch = createSpeedupChart(name, data, mode, p, max, step)
			.setBars("theory:Wtotal", "measured:Wmeas")
			.setBarColors(3, 4)
			.setLabelBar(0, "W_err");
		return ch;
	}
	
	private static BarChart createSPCompareChart(String name, Data data, final String mode, final double p, double max, double step) {
		Data d = new Data(data, new Data.Filter() {
				@Override
				public boolean accept(Data.Row row) {
					return row.getDouble("a.p")==p && row.get("a.m").equals(mode);
				}
			});
		String title = String.format("%s: %s, p=%.1f", name, mode, p);
		BarChart ch = new BarChart(title, d)
			.setXAxisLabels("A7:a.n7", "A15:a.n15")
			.setBars("equal-share:a.SP_meas", "balanced:b.SP_meas")
			.setRange(0.0, max, step)
			.setLabelBar(1, "diff");
		return ch;
	}
	
	private static BarChart createPwrCompareChart(String name, Data data, final String mode, final double p, double max, double step) {
		BarChart ch = createSPCompareChart(name, data, mode, p, max, step)
			.setBars("equal-share:a.Wmeas", "balanced:b.Wmeas")
			.setBarColors(3, 4)
			.setLabelBar(1, "W_diff");
		return ch;
	}
	
	private static BarChart createEDPCompareChart(String name, Data data, final String mode, final double p, double max, double step) {
		BarChart ch = createSPCompareChart(name, data, mode, p, max, step)
			.setBars("equal-share:a.EDP", "balanced:b.EDP")
			.setBarColors(5, 6)
			.setLabelBar(1, "EDP_diff");
		ch.yAxisFormat = "%.0f";
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
						charts[m*2] = createSpeedupChart("Speedup", data, mode, 0.3, SP_RANGES[law][0][0], SP_RANGES[law][0][1]);
						charts[m*2+1] = createSpeedupChart("Speedup", data, mode, 0.9, SP_RANGES[law][1][0], SP_RANGES[law][1][1]);
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
						charts[m*2] = createPowerChart("Power, W", data, mode, 0.3, PWR_RANGES[law][0][0], PWR_RANGES[law][0][1]);
						charts[m*2+1] = createPowerChart("Power, W", data, mode, 0.9, PWR_RANGES[law][1][0], PWR_RANGES[law][1][1]);
					}
					
					out = BarChart.startSvg(String.format("%s/%s_pwr.svg", PATH, LAWS[law]));
					BarChart.layoutCharts(out, charts, 2);
					BarChart.finishSvg(out);
				}
			}
			
			// Balanced comparison
			if(balCompare) {
				BarChart[] charts = new BarChart[6];
				
				Data dataAmd = new Data(String.format("%s/%s.csv", PATH, "amd"));
				Data dataBal = new Data(String.format("%s/%s.csv", PATH, "bal"));
				Data data = new Data(dataAmd, dataBal, new String[] {"m", "p", "n7", "n15"}, "a.", "b.");
				data.addCol("diff", new Diff("a.SP_meas", "b.SP_meas"));
				data.addCol("W_diff", new Diff("a.Wmeas", "b.Wmeas"));
				data.addCol("EDP_diff", new Diff("a.EDP", "b.EDP"));
				for(int m=1; m<MODES.length; m++) {
					String mode = MODES[m];
					charts[m-1] = createSPCompareChart("Speedup", data, mode, 0.9, SP_RANGES[1][1][0], SP_RANGES[1][1][1]);
					charts[2+m-1] = createPwrCompareChart("Power, W", data, mode, 0.9, PWR_RANGES[1][1][0], PWR_RANGES[1][1][1]);
					charts[4+m-1] = createEDPCompareChart("Energy-delay product, Js", data, mode, 0.9, EDP_RANGES[1][1][0], EDP_RANGES[1][1][1]);
				}

				out = BarChart.startSvg(String.format("%s/bal_compare.svg", PATH));
				BarChart.layoutCharts(out, charts, 2);
				BarChart.finishSvg(out);
			}
			
			// Gustafson speedup comparison
			if(gusCompare) {
				BarChart[] charts = new BarChart[MODES.length];
				
				Data dataAmd = new Data(String.format("%s/%s.csv", PATH, "gus"));
				Data dataBal = new Data(String.format("%s/%s.csv", PATH, "guspar"));
				Data data = new Data(dataAmd, dataBal, new String[] {"m", "p", "n7", "n15"}, "a.", "b.");
				data.addCol("diff", new Diff("a.SP_meas", "b.SP_meas"));
				for(int m=0; m<MODES.length; m++) {
					String mode = MODES[m];
					charts[m] = createSPCompareChart("Speedup", data, mode, 0.3, SP_RANGES[3][0][0], SP_RANGES[3][0][1])
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
