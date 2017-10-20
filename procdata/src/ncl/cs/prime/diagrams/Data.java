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
	
	public static interface Formula {
		public String calc(Row row);
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
	
	public Data(Data left, Data right, String[] key, String leftPrefix, String rightPrefix) {
		this.headers = new String[left.headers.length+right.headers.length];
		int j=0;
		for(int i=0; i<left.headers.length; i++, j++) {
			this.headers[j] = leftPrefix+left.headers[i];
			headerMap.put(headers[j], j);
		}
		for(int i=0; i<right.headers.length; i++, j++) {
			this.headers[j] = rightPrefix+right.headers[i];
			headerMap.put(headers[j], j);
		}
		int[] lkeyCols = new int[key.length];
		int[] rkeyCols = new int[key.length];
		for(int i=0; i<key.length; i++) {
			lkeyCols[i] = left.findCol(key[i]);
			rkeyCols[i] = right.findCol(key[i]);
		}
		for(Row lrow: left.rows) {
			for(Row rrow: right.rows) {
				boolean match = true;
				for(int i=0; i<key.length; i++)
					if(!lrow.cells[lkeyCols[i]].equals(rrow.cells[rkeyCols[i]])) {
						match = false;
						break;
					}
				if(match) {
					Row row = new Row();
					row.cells = new String[this.headers.length];
					j=0;
					for(int i=0; i<left.headers.length; i++, j++) {
						row.cells[j] = lrow.cells[i];
					}
					for(int i=0; i<right.headers.length; i++, j++) {
						row.cells[j] = rrow.cells[i];
					}
					rows.add(row);
				}
			}
		}
	}

	public int findCol(String hdr) {
		Integer col = headerMap.get(hdr);
		if(col==null)
			throw new RuntimeException("No column "+hdr);
		return col;
	}
	
	public void addCol(String hdr, Formula calc) {
		int col = this.headers.length;
		String[] headers = new String[col+1];
		for(int i=0; i<this.headers.length; i++)
			headers[i] = this.headers[i];
		headers[col] = hdr;
		headerMap.put(hdr, col);
		this.headers = headers;
		
		for(Row row: rows) {
			String[] cells = new String[col+1];
			for(int i=0; i<col; i++)
				cells[i] = row.cells[i];
			cells[col] = calc.calc(row);
			row.cells = cells;
		}
	}
	
	private void setHeaders(String[] headers) {
		this.headers = headers;
		for(int i=0; i<headers.length; i++) {
			headerMap.put(headers[i], i);
		}
	}
	
}
