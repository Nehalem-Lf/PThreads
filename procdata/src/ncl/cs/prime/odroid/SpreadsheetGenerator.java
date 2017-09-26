package ncl.cs.prime.odroid;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class SpreadsheetGenerator {

	public static class CellStyle {
		public final String id;
		public String valign = null;
		public String halign = null;
		public String font = null;
		public int fontSize = 0;
		public boolean bold = false;
		public boolean italic = false;
		public String textColor = null; 
		public String bgColor = null;
		public boolean bottomBorder = false;
		public String numFormat = null;
		
		public CellStyle(String id) {
			this.id = id;
		}
		
		public CellStyle(String id, CellStyle src) {
			this.id = id;
			this.valign = src.valign;
			this.halign = src.halign;
			this.font = src.font;
			this.fontSize = src.fontSize;
			this.bold = src.bold;
			this.italic = src.italic;
			this.textColor = src.textColor; 
			this.bgColor = src.bgColor;
			this.bottomBorder = src.bottomBorder;
			this.numFormat = src.numFormat;
		}
		
		public CellStyle align(String valign, String halign) {
			this.valign = valign;
			this.halign = halign;
			return this;
		}
		
		public CellStyle font(String font, int size) {
			this.font = font;
			this.fontSize = size;
			return this;
		}
		
		public CellStyle bold() {
			this.bold = true;
			return this;
		}
		
		public CellStyle italic() {
			this.italic = true;
			return this;
		}
		
		public CellStyle textColor(String color) {
			this.textColor = color;
			return this;
		}

		public CellStyle bgColor(String color) {
			this.bgColor = color;
			return this;
		}
		
		public CellStyle bottomBorder() {
			this.bottomBorder = true;
			return this;
		}

		public CellStyle numFormat(String fmt) {
			this.numFormat = fmt;
			return this;
		}

		public void print(PrintWriter out, boolean def) {
			if(def)
				out.print("\t<Style ss:ID=\"Default\" ss:Name=\"Normal\">\n");
			else
				out.printf("\t<Style ss:ID=\"%s\">\n", id);
			if(valign!=null && halign!=null)
				out.printf("\t\t<Alignment ss:Horizontal=\"%s\" ss:Vertical=\"%s\"/>\n", halign, valign);
			if(font!=null) {
				out.printf("\t\t<Font ss:FontName=\"%s\" x:Family=\"Swiss\" ss:Size=\"%d\" %s%s%s/>\n", font, fontSize,
						bold ? "ss:Bold=\"1\" " : "",
						italic ? "ss:Italic=\"1\" " : "",
						textColor!=null ? "ss:Color=\""+textColor+"\" " : ""
					);
			}
			if(bgColor!=null)
				out.printf("\t\t<Interior ss:Color=\"%s\" ss:Pattern=\"Solid\"/>\n", bgColor);
			if(bottomBorder) {
				out.print("\t\t<Borders>\n" + 
						"\t\t\t<Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Color=\"#000000\" ss:Weight=\"1\" />\n" + 
						"\t\t</Borders>\n");
			}
			if(numFormat!=null)
				out.printf("\t\t<NumberFormat ss:Format=\"%s\"/>\n", numFormat);
			out.print("\t</Style>\n");
		}
	}
	
	private PrintWriter out;
	
	public SpreadsheetGenerator(String path, String author) throws IOException {
		out = new PrintWriter(new File(path));
		out.print("<?xml version=\"1.0\"?>\n" + 
				"<?mso-application progid=\"Excel.Sheet\"?>\n" + 
				"<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n" + 
				"\txmlns:o=\"urn:schemas-microsoft-com:office:office\"\n" + 
				"\txmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n" + 
				"\txmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n" + 
				"\txmlns:html=\"http://www.w3.org/TR/REC-html40\">\n" + 
				"<DocumentProperties xmlns=\"urn:schemas-microsoft-com:office:office\">\n" + 
				"\t<Author>"+author+"</Author>\n" + 
				"\t<Version>16.00</Version>\n" + 
				"</DocumentProperties>\r\n" + 
				"<OfficeDocumentSettings xmlns=\"urn:schemas-microsoft-com:office:office\">\n" + 
				"\t<AllowPNG/>\n" + 
				"</OfficeDocumentSettings>\n" + 
				"<ExcelWorkbook xmlns=\"urn:schemas-microsoft-com:office:excel\">\n" + 
				"\t<TabRatio>370</TabRatio>\n" + 
				"\t<ActiveSheet>0</ActiveSheet>\n" + 
				"\t<ProtectStructure>False</ProtectStructure>\n" + 
				"\t<ProtectWindows>False</ProtectWindows>\n" + 
				"</ExcelWorkbook>\n");
	}
	
	private int colIndex;
	private boolean addIndex;
	
	public void beginStyles(CellStyle def) {
		out.print("<Styles>\n");
		def.print(out, true);
	}
	
	public void addStyle(CellStyle style) {
		style.print(out, false);
	}
	
	public void endStyles() {
		out.print("</Styles>\n");
	}
	
	public void beginSheet(String name, int colWidth) {
		out.printf("<Worksheet ss:Name=\"%s\">\n\t<Table ss:DefaultColumnWidth=\"%d\">\n", name, colWidth);
	}

	public void beginRow() {
		out.print("\t<Row>\n");
		colIndex = 1;
		addIndex = false;
	}
	
	public void skipRow() {
		out.print("\t<Row></Row>\n");
	}

	public void skipCells(int cells) {
		colIndex += cells;
		addIndex = true;
	}
	
	private void addCell(String styleId, String formula, String data, String dataType) {
		out.print("\t<Cell");
		if(addIndex) {
			out.printf(" ss:Index=\"%d\"", colIndex);
			addIndex = false;
		}
		colIndex++;
		if(styleId!=null)
			out.printf(" ss:StyleID=\"%s\"", styleId);
		if(formula!=null)
			out.printf(" ss:Formula=\"%s\"", formula);
		if(data!=null)
			out.printf("><Data ss:Type=\"%s\">%s</Data></Cell>\n", dataType, data);
		else
			out.print(" />\n");
	}
	
	public void addEmpty() {
		addCell(null, null, null, null);
	}

	public void addFewEmpty(int num) {
		for(int i=0; i<num; i++)
			addCell(null, null, null, null);
	}

	public void addFormula(String formula) {
		addCell(null, formula, null, null);
	}
	
	public void addString(String data) {
		addCell(null, null, data, "String");
	}

	public void addNumber(int data) {
		addCell(null, null, Integer.toString(data), "Number");
	}

	public void addNumber(double data) {
		addCell(null, null, Double.toString(data), "Number");
	}

	public void addEmpty(String styleId) {
		addCell(styleId, null, null, null);
	}
	
	public void addFewEmpty(int num, String styleId) {
		for(int i=0; i<num; i++)
			addCell(styleId, null, null, null);
	}

	public void addFormula(String styleId, String formula) {
		addCell(styleId, formula, null, null);
	}
	
	public void addString(String styleId, String data) {
		addCell(styleId, null, data, "String");
	}

	public void addNumber(String styleId, int data) {
		addCell(styleId, null, Integer.toString(data), "Number");
	}

	public void addNumber(String styleId, double data) {
		addCell(styleId, null, Double.toString(data), "Number");
	}

	public void endRow() {
		out.print("\t</Row>\n");
	}

	public void endSheet() {
		out.print("\t</Table>\n</Worksheet>\n");
	}
	
	public void close() {
		out.print("</Workbook>");
		out.close();
	}
	
}
