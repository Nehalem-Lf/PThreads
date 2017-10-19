package ncl.cs.prime.diagrams;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Data {

	private static final String sepRegex = "(\\s+)|\\,";
	
	public static interface Filter {
		public boolean accept(Row row);
	}
	
	public class Row {
		public String[] cells;
		
		public String get(String hdr) {
			return cells[findCol(hdr)];
		}
		
		public int getInt(String hdr) {
			return Integer.parseInt(get(hdr));
		}
		
		public double getDouble(String hdr) {
			return Double.parseDouble(get(hdr));
		}
	}
	
	public String[] headers;
	private HashMap<String, Integer> headerMap = new HashMap<>();
	
	public ArrayList<Row> rows = new ArrayList<>();
	
	public Data(String path) throws IOException {
		Scanner in = new Scanner(new File(path));
		setHeaders(in.nextLine().split(sepRegex));
		while(in.hasNext()) {
			Row row = new Row();
			row.cells = in.nextLine().split(sepRegex);
			rows.add(row);
		}
		in.close();
	}

	public Data(Data src, Filter filter) {
		this.headers = src.headers;
		this.headerMap = src.headerMap;
		for(Row row: src.rows) {
			if(filter.accept(row))
				rows.add(row);
		}
	}

	public int findCol(String hdr) {
		return headerMap.get(hdr);
	}
	
	private void setHeaders(String[] headers) {
		this.headers = headers;
		for(int i=0; i<headers.length; i++) {
			headerMap.put(headers[i], i);
		}
	}
	
}
