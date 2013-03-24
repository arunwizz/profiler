package org.arrah.framework.dataquality;

/***********************************************
 *     Copyright to Vivek Kumar Singh          * 
 *                                             *
 * Any part of code or file can be changed,    *
 * redistributed, modified with the copyright  *
 * information intact                          * 
 *                                             *
 * Author$ : Vivek Singh                       *
 *                                             *
 ***********************************************/

/* 
 * This file is used to Similarity Check
 * for Lucene Index directory or RAM
 *
 */

import java.util.Hashtable;
import java.util.Vector;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.RAMDirectory;
import org.arrah.framework.ndtable.ReportTableModel;
import org.arrah.framework.rdbms.JDBCRowset;

public class SimilarityCheckLucene {
	private ReportTableModel _rt, outputRT;
	private String[] colName;
	private String[] colType;
	private int rowC;
	private IndexWriter writer;
	private IndexSearcher searcher;
	private RAMDirectory idx;
	private Vector<Integer> skipVC;

	private Hashtable<Integer, Integer> parentMap;
	boolean isRowSet = false;
	private JDBCRowset _rows;

	// For ReportTable Input
	public SimilarityCheckLucene(ReportTableModel rt) {
		_rt = rt;
		colName = getColName();
		rowC = _rt.getModel().getRowCount();
	}

	// For Rowset Table Input
	public SimilarityCheckLucene(JDBCRowset rowSet) {
		isRowSet = true;
		_rows = rowSet;
		colName = _rows.getColName();
		colType = _rows.getColType();
		rowC = _rows.getRowCount();
	}

	private String[] getColName() {
		int colC = _rt.getModel().getColumnCount();
		colName = new String[colC];
		colType = new String[colC];

		for (int i = 0; i < colC; i++) {
			colName[i] = _rt.getModel().getColumnName(i);
			colType[i] = _rt.getModel().getColumnClass(i).getName();
		}
		return colName;
	}

	public Document createDocument(int rowId, Object[] row) {
		Document doc = new Document();
		if (row == null)
			return doc;
		try {
			doc.add(new Field("at__rowid__", Integer.toString(rowId),
					Field.Store.YES, Field.Index.NO));
			for (int i = 0; i < row.length; i++) {
				if (row[i] != null && colName[i] != null)
					doc.add(new Field(colName[i], row[i].toString(),
							Field.Store.NO, Field.Index.TOKENIZED));
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("\n " + e.getMessage());
			System.out.println("\n Error: Document Creation Error");
		}
		return doc;
	}

	public void makeIndex() {
		if (createIndex() == false)
			return;
		addDocument();
		closeIndex();
	}

	public boolean createIndex() {
		// Construct a RAMDirectory to hold the in-memory representation
		// of the index.
		idx = new RAMDirectory();
		try {
			writer = new IndexWriter(idx, new StandardAnalyzer(), true);
		} catch (Exception e) {
			System.out.println("\n " + e.getMessage());
			System.out.println("\n ERROR: Index Open Exception");
			return false;
		}
		return true;

	}

	public void closeIndex() {
		try {
			writer.optimize();
			writer.close();
		} catch (Exception e) {
			System.out.println("\n " + e.getMessage());
			System.out.println("\n ERROR: Index Close Exception");
		}
	}

	public void addDocument() {
		// Create multiple threads(10) if rowId > 100
		try {
			if (rowC <= 100 || isRowSet == true) { // Rowset has synch problem
				for (int i = 0; i < rowC; i++)
					if (isRowSet == false)
						writer.addDocument(createDocument(i, _rt.getRow(i)));
					else
						writer.addDocument(createDocument(i + 1,
								_rows.getRow(i + 1)));
			} else {
				final int THREADCOUNT = 10;
				Thread[] tid = new Thread[THREADCOUNT];
				final int rowthread = rowC / THREADCOUNT;
				for (int i = 0; i < THREADCOUNT; i++) {
					final int tindex = i;
					tid[tindex] = new Thread(new Runnable() {
						public void run() {
							if (tindex < THREADCOUNT - 1)
								for (int j = tindex * rowthread; j < tindex
										* rowthread + rowthread; j++)
									try {
										writer.addDocument(createDocument(j,
												_rt.getRow(j)));
									} catch (Exception e) {
										System.out.println(" Lucene Exeception:"+e.getMessage());
									}
							else
								for (int j = tindex * rowthread; j < rowC; j++)
									try {
										writer.addDocument(createDocument(j,
												_rt.getRow(j)));
									} catch (Exception e) {
										System.out.println("Lucene Exeception:"+e.getMessage());
									}
						}
					});
					tid[i].start();
				}
				for (int i = 0; i < THREADCOUNT; i++)
					try {
						tid[i].join();
					} catch (Exception e) {
						System.out.println(" Thread Exeception:"+e.getMessage());
					}
			}
		} catch (Exception e) {
			System.out.println(" Add Document Exeception:"+e.getMessage());
		}
	}

	public void searchTableIndex(String query) {
		if (openIndex() == false)
			return;
		// Add a boolean column for delete
		String[] newColN = new String[colName.length + 1];
		newColN[0] = "Delete Editable"; // This column you can change
		for (int i = 0; i < colName.length; i++)
			newColN[i + 1] = colName[i];

		outputRT = new ReportTableModel(newColN, false, true);
		skipVC = new Vector<Integer>();
		parentMap = new Hashtable<Integer, Integer>();

		// Search in complete Index

		String queryString = query;
		if (queryString == null || queryString.equals("") == true)
			return;
		Query qry = parseQuery(queryString);
		Hits hit = searchIndex(qry);
		if (hit == null || hit.length() <= 1)
			return; // It will match self
		// Iterate over the Documents in the Hits object

		for (int j = 0; j < hit.length(); j++) {
			try {
				Document doc = hit.doc(j);
				String rowid = doc.get("at__rowid__");
				parentMap.put(outputRT.getModel().getRowCount(),
						Integer.parseInt(rowid));
				Object[] row = null;
				if (isRowSet == false)
					row = _rt.getRow(Integer.parseInt(rowid));
				else
					row = _rows.getRow(Integer.parseInt(rowid));
				Object[] newRow = new Object[row.length + 1];
				boolean del = false;
				newRow[0] = del;
				for (int k = 0; k < row.length; k++)
					newRow[k + 1] = row[k];
				outputRT.addFillRow(newRow);
				skipVC.add(Integer.parseInt(rowid));
			} catch (Exception e) {
				System.out.println("\n " + e.getMessage());
				System.out.println("\n Error: Can not open Document");
			}
		}
		outputRT.addNullRow();

		closeSeachIndex();

	}

	public boolean openIndex() {
		try {
			searcher = new IndexSearcher(idx);
		} catch (Exception e) {
			System.out.println("\n " + e.getMessage());
			System.out.println("\n Error: Can not open Index Searcher");
			return false;
		}
		return true;
	}

	public Query parseQuery(String query) {
		try {
			QueryParser qp = new QueryParser(colName[0], new StandardAnalyzer());
			qp.setAllowLeadingWildcard(true);
			return qp.parse(query);
		} catch (Exception e) {
			System.out.println("\n " + e.getMessage());
			System.out.println("\n Error: Can not Parse Query");
			return null;
		}
	}

	public Hits searchIndex(Query query) {
		if (query == null)
			return null;
		Hits hit = null;
		try {
			hit = searcher.search(query);
		} catch (Exception e) {
			System.out.println("\n " + e.getMessage());
			System.out.println("\n Error: Can not Search  Index");
			return null;
		}
		return hit;
	}

	public void closeSeachIndex() {
		try {
			searcher.close();
		} catch (Exception e) {
			System.out.println("\n " + e.getMessage());
			System.out.println("\n Error: Can not Close Search  Index");
		}
	}

} // End of Similarity Check
