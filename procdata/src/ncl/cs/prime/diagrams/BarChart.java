package ncl.cs.prime.diagrams;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class BarChart {

//	public static final String[] palette = {"#ff420e", "#004586", "#9999ff", "#ffd320", "#399d1c", "#7e0021", "#83caff"};
	public static final String[] palette = {"#ff6633", "#225588", "#bbbbff", "#ffdd22", "#44aa22", "#770022", "#77bbff"};
	
	public static final int defPivotX = 100;
	public static final int defPivotY = 300;
	public static final int chartPaddingX = 75;
	public static final int chartPaddingY = 20;
	
	public static final int areaHeight = 180;
	public static final int barWidth = 12;
	public static final int barMargin = 3;
	public static final int legendItemMargin = 20;
	
	public final Data data; 

	public String yAxisFormat = "%.1f";
	public String[] xAxisLabelData;
	public String[] xAxisNames;
	public double min, max, step;
	public double log = 0.0;

	public String[] bars; 
	public String[] legend;
	public String[] barColors = {palette[0], palette[1]};
	
	public int labelBar = -1;
	public String labelData;
	public String title;
	
	public BarChart(String title, Data data) {
		this.title = title;
		this.data = data;
	}
	
	public BarChart setRange(double min, double max, double step) {
		this.min = min;
		this.max = max;
		this.step = step;
		return this;
	}

	public BarChart setLogRange(double log, double min, double max, double step) {
		this.log = log;
		this.min = min;
		this.max = max;
		this.step = step;
		return this;
	}

	public BarChart setBars(String... bars) {
		this.bars = new String[bars.length];
		this.legend = new String[bars.length];
		for(int i=0; i<bars.length; i++) {
			String[] s = bars[i].split("\\:", 2); 
			this.legend[i] = s[0];
			this.bars[i] = s[1];
		}
		return this;
	}

	public BarChart setXAxisLabels(String... labels) {
		this.xAxisLabelData = new String[labels.length];
		this.xAxisNames = new String[labels.length];
		for(int i=0; i<labels.length; i++) {
			String[] s = labels[i].split("\\:", 2); 
			this.xAxisNames[i] = s[0];
			this.xAxisLabelData[i] = s[1];
		}
		return this;
	}

	public BarChart setBarColors(int... colorIds) {
		this.barColors = new String[bars.length];
		for(int i=0; i<bars.length; i++) {
			this.barColors[i] = palette[colorIds[i]];
		}
		return this;
	}
	
	public BarChart setLabelBar(int bar, String dataHdr) {
		this.labelBar = bar;
		this.labelData = dataHdr;
		return this;
	}
		
	private static double logb(double x, double b) {
		return Math.log(x) / Math.log(b);
	}
	
	private static FontMetrics fontMetrics(String family, int style, float size) {
		BufferedImage helper = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Font font = new Font(family, style, 10).deriveFont(size);
		Graphics2D g2 = (Graphics2D) helper.getGraphics();
		g2.setFont(font);
		return g2.getFontMetrics();
	}
	
	private double nexty(double y) {
		return (log>0.0) ? y*step : y+step;
	}
	
	private double val(Data.Row row, int b) {
		if(log>0.0)
			return logb(row.getDouble(bars[b])/min, log)*(double)areaHeight/logb(max/min, log);
		else
			return (row.getDouble(bars[b])-min)*(double)areaHeight/max;
	}
	
	public int getWidth() {
		int len = data.rows.size();
		int gridx = bars.length*barWidth + barMargin*2;
		return len*gridx;
	}

	public int getHeight() {
		return areaHeight+60;
	}

	public void write(String path) throws IOException {
		PrintWriter out = new PrintWriter(new File(path));
		write(defPivotX, defPivotY, out);
		out.close();
	}
	
	public void write(int pivotx, int pivoty, PrintWriter out) {
		if(log>0.0 && step<=1.0 || step<=0.0)
			throw new RuntimeException("Bad step");
		
		out.printf("<g transform=\"translate(%d, %d)\">\n", pivotx, pivoty);
		
		double tx, ty;
		
		int len = data.rows.size();
		int gridx = bars.length*barWidth + barMargin*2;
		double gridy = (log>0.0) ? areaHeight / logb(max/min, step) : areaHeight * step / (max-min);
		
		// Grid
		out.printf("<g><!-- Grid -->\n");
		
		// Grid
		// out.printf("<g stroke-dasharray=\"1, 2\" stroke=\"#000000\" stroke-width=\"0.5\">\n");
		out.printf("<g stroke=\"#000000\" stroke-width=\"0.25\">\n");
		out.printf("\t<line x1=\"%d\" y1=\"-%d\" x2=\"%d\" y2=\"0\"/>\n", len*gridx, areaHeight, len*gridx);
		ty = -gridy;
		for(double y=nexty(min); y<=max; y=nexty(y)) {
			out.printf("\t<line x1=\"0\" y1=\"%f\" x2=\"%d\" y2=\"%f\"/>\n", ty, len*gridx, ty);
			ty -= gridy;
		}
		out.printf("</g>\n");

		out.printf("</g>\n");
		
		// Bars
		out.printf("<g><!-- Bars -->\n");
		
		out.printf("<g stroke=\"#000000\" stroke-width=\"0.5\">\n");
		tx = barMargin;
		for(int r=0; r<len; r++) {
			Data.Row row = data.rows.get(r);
			for(int b=0; b<bars.length; b++) {
				double val = val(row, b);
				if(val>0.0)
					out.printf("\t<rect x=\"%f\" y=\"%f\" width=\"%d\" height=\"%f\" fill=\"%s\" />\n", tx+b*barWidth, -val, barWidth, val, barColors[b]);
				else
					out.printf("\t<rect x=\"%f\" y=\"0\" width=\"%d\" height=\"%f\" fill=\"%s\" />\n", tx+b*barWidth, barWidth, -val, barColors[b]);
			}
			tx += gridx;
		}
		out.printf("</g>\n");
		
		out.printf("</g>\n");
		
		// Axes
		out.printf("<g><!-- Axes -->\n");
		out.printf("<line x1=\"0\" y1=\"-%d\" x2=\"0\" y2=\"0\" stroke=\"#000000\" stroke-width=\"1\"/>\n", areaHeight);
		out.printf("<line x1=\"0\" y1=\"0\" x2=\"%d\" y2=\"0\" stroke=\"#000000\" stroke-width=\"1\"/>\n", len*gridx);
		
		// Axis marks
		out.printf("<g stroke=\"#000000\" stroke-width=\"0.5\">\n");
		ty = 0;
		for(double y=min; y<=max; y=nexty(y)) {
			out.printf("\t<line x1=\"-5\" y1=\"%f\" x2=\"0\" y2=\"%f\"/>\n", ty, ty);
			ty -= gridy;
		}
		tx = 0;
		for(int r=0; r<=len; r++) {
			out.printf("\t<line x1=\"%f\" y1=\"0\" x2=\"%f\" y2=\"5\"/>\n", tx, tx);
			tx += gridx;
		}
		out.printf("</g>\n");

		// Y-axis labels
		out.printf("<g text-anchor=\"end\" style=\"font-weight:normal;font-size:10px;font-family:Arial;fill:#000000;stroke:none\">\n");
		ty = 3;
		for(double y=min; y<=max; y=nexty(y)) {
			out.printf("\t<text transform=\"translate(-7.5, %f)\">"+yAxisFormat+"</text>\n", ty, y);
			ty -= gridy;
		}
		out.printf("</g>\n");
		
		// X-axis labels
		out.printf("<g text-anchor=\"middle\" style=\"font-weight:normal;font-size:12px;font-family:Arial;fill:#000000;stroke:none\">\n");
		tx = gridx/2.0;
		for(int r=0; r<len; r++) {
			Data.Row row = data.rows.get(r);
			out.printf("\t<text transform=\"translate(%f, 12.5)\">\n", tx);
			ty = 0;
			for(int i=0; i<xAxisNames.length; i++) {
				out.printf("\t\t<tspan x=\"0\" y=\"%f\">%s</tspan>\n", ty, row.get(xAxisLabelData[i]));
				ty += 11;
			}
			out.printf("\t</text>\n");
			tx += gridx;
		}
		tx = tx - gridx/2.0 +barMargin + 2.5;
		out.printf("\t<text text-anchor=\"start\" transform=\"translate(%f, 12.5)\">\n", tx);
		ty = 0;
		for(int i=0; i<xAxisNames.length; i++) {
			out.printf("\t\t<tspan x=\"0\" y=\"%f\">%s</tspan>\n", ty, xAxisNames[i]);
			ty += 11;
		}
		out.printf("\t</text>\n");
		out.printf("</g>\n");
		
		out.printf("</g>\n");

		// Bar labels
		if(labelBar>=0) {
			out.printf("<g><!-- Bar labels -->\n");
			
			out.printf("<g text-anchor=\"middle\" style=\"font-weight:normal;font-size:11px;font-family:Arial\">\n");
			for(int layer=0; layer<2; layer++) {
				if(layer==0)
					out.printf("<g style=\"fill:none;stroke:#ffffff;stroke-width:3\">\n");
				else
					out.printf("<g style=\"fill:#000000;fill-opacity:1;stroke:none\">\n");
				tx = barMargin + barWidth*labelBar+barWidth/2.0;
				for(int r=0; r<len; r++) {
					Data.Row row = data.rows.get(r);
					double val = val(row, labelBar);
					if(val<0.0)
						val = 0.0;
					out.printf("\t<text transform=\"translate(%f, %f)\">%s</text>\n", tx, -val-5.0, row.get(labelData));
					tx += gridx;
				}
				out.printf("</g>\n");
			}
			out.printf("</g>\n");
			
			out.printf("</g>\n");
		}

		
		// Title and legend
		double mid = len*gridx/2.0; 
		out.printf("<g><!-- Title and legend -->\n");
		out.printf("<text text-anchor=\"middle\" style=\"font-weight:bold;font-size:14px;font-family:Arial;fill:#000000;stroke:none\" x=\"%f\" y=\"%d\">%s</text>\n",
				mid, -areaHeight-26, title);
		FontMetrics fm = fontMetrics("Arial", Font.PLAIN, 12f);
		int[] widths = new int[legend.length];
		int legendWidth = 0;
		for(int b=0; b<legend.length; b++) {
			if(b>0)
				legendWidth += legendItemMargin;
			int w = fm.stringWidth(legend[b]);
			widths[b] = w;
			legendWidth += w;
		}
		tx = mid - legendWidth/2.0;
		for(int b=0; b<legend.length; b++) {
			out.printf("<g transform=\"translate(%f, %d)\">\n", tx, -areaHeight-10);
			out.printf("\t<rect x=\"0\" y=\"-6.5\" width=\"6\" height=\"6\" style=\"fill:%s;stroke:none\"/>\n", barColors[b]);
			out.printf("\t<text style=\"font-weight:normal;font-size:12px;font-family:Arial;fill:#000000;stroke:none\" x=\"9\" y=\"0\">%s</text>\n", legend[b]);
			out.printf("</g>\n");
			tx += widths[b]+legendItemMargin;
		}
		out.printf("</g>\n");

		out.printf("</g>\n");
	}
	
	public static PrintWriter startSvg(String path) throws IOException {
		PrintWriter out = new PrintWriter(new File(path));
		out.printf("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<svg\n"
			+ "\txmlns:svg=\"http://www.w3.org/2000/svg\"\n"
			+ "\txmlns=\"http://www.w3.org/2000/svg\"\n"
			+ "\twidth=\"793.70079\"\n"
			+ "\theight=\"1122.51969\"\n"
			+ "\tpagecolor=\"#ffffff\"\n"
			+ "\tbordercolor=\"#777777\"\n"
			+ "\tborderopacity=\"1\"\n"
			+ "><g transform=\"scale(0.5)\">\n");
		return out;
	}

	public static void layoutCharts(PrintWriter out, BarChart[] charts, int cols) {
		if(cols<=0) {
			int x = defPivotX;
			for(BarChart ch: charts) {
				ch.write(x, defPivotY, out);
				x += ch.getWidth()+chartPaddingX;
			}
		}
		else {
			int[] w = new int[cols];
			int col = 0;
			for(BarChart ch: charts) {
				int cw = ch.getWidth()+chartPaddingX;
				if(cw>w[col])
					w[col] = cw;
				col++;
				if(col>=cols)
					col = 0;
			}
			int x = defPivotX;
			int y = defPivotY;
			col = 0;
			for(BarChart ch: charts) {
				ch.write(x + (w[col]-chartPaddingX-ch.getWidth())/2, y, out);
				x += w[col];
				col++;
				if(col>=cols) {
					col = 0;
					x = defPivotX;
					y += ch.getHeight()+chartPaddingY;
				}
			}
		}
	}
	
	public static void finishSvg(PrintWriter out) {
		out.printf("</g></svg>\n");
		out.close();
	}
	
}
