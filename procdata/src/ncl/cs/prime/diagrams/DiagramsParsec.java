package ncl.cs.prime.diagrams;

import java.io.IOException;
import java.io.PrintWriter;

public class DiagramsParsec {

	public static final String PATH = "../odroid/data_parsec3/results";
	public static final String[] MODES = {"bodytrack", "ferret", "fluidanimate"};
	public static final double MAX = 10.0;
	public static final double STEP = 2.0;
	
	private static PrintWriter out;
	
	private static BarChart createParsecChart(Data data, final String mode, double max, double step) {
		Data d = new Data(data, new Data.Filter() {
				@Override
				public boolean accept(Data.Row row) {
					return row.getInt("n7")>0 && row.getInt("n15")>0 && row.get("app").equals(mode);
				}
			});
		BarChart ch = new BarChart(mode, d)
			.setXAxisLabels("A7:n7", "A15:n15")
			.setBars("low:SP_min", "measured:SP_meas", "high:SP_max")
			.setBarColors(0, 2, 1)
			.setRange(0.0, max, step)
			.setLabelBar(1, "q_minmax");
		return ch;
	}
	
	public static void main(String[] args) {
		try {
			// Speedup - load balancing diagrams
			BarChart[] charts = new BarChart[MODES.length];
			
			Data data = new Data(String.format("%s/parsec_short.csv", PATH));
			for(int m=0; m<MODES.length; m++) {
				String mode = MODES[m];
				charts[m] = createParsecChart(data, mode, MAX, STEP);
			}
			
			out = BarChart.startSvg(String.format("%s/parsec_short.svg", PATH));
			BarChart.layoutCharts(out, charts, 0);
			BarChart.finishSvg(out);
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

}
