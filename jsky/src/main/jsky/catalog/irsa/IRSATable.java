// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: IRSATable.java,v 1.2 2002/08/05 10:57:20 brighton Exp $

package jsky.catalog.irsa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;
import java.util.Vector;

import jsky.catalog.Catalog;
import jsky.catalog.FieldDesc;
import jsky.catalog.FieldDescAdapter;
import jsky.catalog.MemoryCatalog;
import jsky.catalog.QueryArgs;
import jsky.catalog.RowCoordinates;
import jsky.util.StringTokenizerUtil;


/**
 * Used to read and write IRSA style catalog tables and manage 
 * the rows and columns in memory.
 * This class extends the MemoryCatalog class, which supports
 * searching and working with a JTable widget.
 *
 * @version $Revision: 1.2 $
 * @author Allan Brighton
 */
public class IRSATable extends MemoryCatalog {

    /** Used to hold the data types for the table columns. */
    private Vector _columnTypes;

    /** Used to hold the units string for the table columns. */
    private Vector _columnUnits;

    /** Used to hold the null values for the table columns. */
    private Vector _columnNulls;


    /**
     * Initialize the table from the given stream by reading up to
     * maxRows of the data.
     *
     * @param catalog the catalog where the data originated, if known
     * @param in the stream to read the catalog data from
     * @param maxRows the maximum number of data rows to read
     */
    public IRSATable(IRSACatalog catalog, InputStream in, int maxRows) throws IOException {
	super(catalog, in, maxRows);
    }

    /**
     * Initialize the table from the given stream by reading up to
     * maxRows of the data.
     *
     * @param catalog the catalog where the data originated, if known
     * @param in the stream to read the catalog data from
     * @param queryArgs represents the arguments to the query that resulted in this table
     */
    public IRSATable(IRSACatalog catalog, InputStream in, QueryArgs queryArgs) throws IOException {
        super(catalog, in, queryArgs);
    }


    /**
     * Initialize the table from the given stream.
     *
     * @param catalog the catalog where the data originated, if known
     * @param in the stream to read the catalog data from
     */
    public IRSATable(IRSACatalog catalog, InputStream in) throws IOException {
        super(catalog, in);
    }


    /**
     * Initialize the table from the given file.
     *
     * @param catalog the catalog where the data originated, if known
     * @param filename the name of the catalog file
     */
    public IRSATable(IRSACatalog catalog, String filename) throws IOException {
        super(catalog, filename);
    }


    /**
     * Initialize the table from the given file
     *
     * @param filename the name of the catalog file
     */
    public IRSATable(String filename) throws IOException {
        super(filename);
    }


    /**
     * Construct a new IRSATable with the given column fields and data rows
     * (For internal use only).
     *
     * @param table the source catalog table
     * @param fields an array of objects describing the table columns
     * @param dataVector a vector of data rows, each of which is a vector of column values.
     */
    private IRSATable(IRSATable table, FieldDesc[] fields, Vector dataRows) {
        super(fields, dataRows);
        setName(table.getName());
        setId(table.getId());
        setTitle("Query Results from: " + table.getTitle());
        setColumnClasses(table.getColumnClasses());
    }

    /**
     * Construct a new IRSATable with no header or data
     * (For use only by derived classes).
     */
    protected IRSATable() {
    }

    /**
     * Return the catalog used to create this table,
     * or a dummy, generated catalog object, if not known.
     */
    public Catalog getCatalog() {
	Catalog catalog = super.getCatalog();
        if (catalog != null) 
	    return catalog;

	String filename = getFilename();
	if (filename == null)
	    filename = "unknown";
	File file = new File(filename);
	return new IRSACatalog(file, this);
    }


    /**
     * Initialize the table from the given stream, reading at most maxRows data rows
     * (Redefined from the parent class to accept the input in teh IRSA format).
     */
    protected void _init(InputStream in, int maxRows) throws IOException {
	// Here is part of an example table for reference:
	//
	// \fixlen = T
	// \primary    = 0
	// \RowsRetreived =                 25  
	// \QueryTime     =   00:00:00.04173              
	// |    pscname|       ra|      dec|   spt_ind|
	// |       char|   double|   double|       int|
	// |           |  degrees|  degrees|          |
	// |       null|     null|     null|      null|
	// 03156+4025     49.7276   40.5981  233312210 
	// 03176+4012     50.2293   40.3907  233312210 

        // read the table header
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

	// expect: \fixlen = T
	String line = reader.readLine(); 
	if (line == null)
	    throw new IOException("EOF reading table data");

	if (!(line.equals("\\fixlen = T"))) {
	    // errors are reported in 3 lines, first line is ERROR
	    if (line.equals("ERROR")) {
		String line2 = reader.readLine(); 
		String line3 = reader.readLine(); 
		if (line2 != null)
		    line += ", " + line2;
		if (line3 != null)
		    line += ", " + line3;
		throw new IOException(line);
	    }
	    throw new IOException("Unknown table format: expected first line to be: '\\fixlen = T', not: " + line);
	}

	// ignore: \primary
	line = reader.readLine();

	// expect: \RowsRetreived = 
	line = reader.readLine();
	int rowsRetreived = Integer.parseInt(line.substring(17).trim());
	if (maxRows > 0 && rowsRetreived > maxRows)
	    rowsRetreived = maxRows;

	// ignore: \QueryTime 
	line = reader.readLine();
	
	// column headings
	line = reader.readLine(); 
	setColumnIdentifiers(_parseHeading(line));

	// column types
	line = reader.readLine();   
        _columnTypes = _parseHeading(line);
        setColumnClasses(_getColumnClasses(_columnTypes));

	// column units
	line = reader.readLine();   
        _columnUnits = _parseHeading(line);

	// column null values
	line = reader.readLine();               
        _columnNulls = _parseHeading(line);

	_initFields();

        // read the table data
        dataVector = new Vector(rowsRetreived, 256);
        int nrows = 1;
        while (true) {
            line = reader.readLine();
            if (line == null)
                break;
            dataVector.add(_parseRow(line));
            if (maxRows > 0 && nrows++ >= maxRows)
                break;
        }
    }


    /**
     * Return a new MemoryCatalog with the given column fields and data rows.
     *
     * @param fields an array of objects describing the table columns
     * @param dataVector a vector of data rows, each of which is a vector of column values.
     */
    protected MemoryCatalog makeQueryResult(FieldDesc[] fields, Vector dataRows) {
        IRSATable table = new IRSATable(this, fields, dataRows);
        return table;
    }


    /**
     * Parse the given line and return a vector containing the strings.
     *
     * @param s A line containing strings separated by "|" and spaces
     * @return A vector of Strings.
     */
    protected Vector _parseHeading(String s) {
	if (s == null || s.length() == 0)
	    throw new RuntimeException("Missing header line in catalog table");

        StringTokenizerUtil st = new StringTokenizerUtil(s.substring(1), "|");
        Vector v = new Vector(st.countTokens(), 1);
        while (st.hasMoreTokens()) {
            v.add(st.nextToken().trim());
        }
	
        return v;
    }


    /** Initialize the fields array, which describes the table columns */
    protected void _initFields() {
	Vector colNames = getColumnIdentifiers();
	int n = colNames.size();
        FieldDescAdapter[] fields = new FieldDescAdapter[n];
	int raCol = -1, decCol = -1;
	double equinox = 2000.;
        for (int i = 0; i < n; i++) {
	    String name = (String)colNames.get(i);
            fields[i] = new FieldDescAdapter(name);
	    fields[i].setUnits((String)_columnUnits.get(i));
	    if (name.equalsIgnoreCase("ra")) {
		fields[i].setIsRA(true);
		raCol = i;
	    }
	    else if (name.equalsIgnoreCase("dec")) {
		fields[i].setIsDec(true);
		decCol = i;
	    }
	}

	if (raCol >= 0 && decCol >= 0) {
	    setRowCoordinates(new RowCoordinates(raCol, decCol, equinox));
	}

	setFields(fields);
    }


    /**
     * Parse the given table row and return a vector of objects for it.
     *
     * @param lineStr A string containing a line from the table.
     * @return A Vector containing the items in the row.
     */
    protected Vector _parseRow(String lineStr) {
        StringTokenizer st = new StringTokenizer(lineStr, " ");
        int numCols = getColumnIdentifiers().size();
        int i = 0;
        Vector row = new Vector(numCols, 1);

        while (st.hasMoreTokens()) {
            if (i++ >= numCols)
                break;
            String s = st.nextToken().trim();
            if (s.length() != 0) {
                Object o = _parseItem(i-1, s);
                row.add(o);
            }
            else {
                // treat empty cells as null, having no type
                row.add(null);
            }
        }
        // fill at end, if needed
        while (i++ < numCols)
            row.add(null);

        return row;
    }

    /**
     * Given a vector of type names from the table header, return a vector
     * of Class objects to use for them.
     */
    private Vector _getColumnClasses(Vector typeNames) {
	int n = typeNames.size();
	Vector columnClasses = new Vector(n);
	for(int i = 0; i < n; i++) {
	    String type = (String)typeNames.get(i);
	    if (type.equals("double"))
		columnClasses.add(Double.class);
	    else if (type.equals("int"))
		columnClasses.add(Integer.class);
	    else 
		columnClasses.add(String.class);
	}
	return columnClasses;
    }

    /**
     * Parse the given string and return an object of the correct type
     * for the given column index, or null if the value matches the null value
     * for the column.
     *
     * @param colIndex teh column index
     * @param s A String to be parsed.
     * @return A Double or String object.
     */
    private Object _parseItem(int colIndex, String s) {
	String nullStr = (String)_columnNulls.get(colIndex);
	if (s == null || s.equals(nullStr))
	    return null;
	Class c = getColumnClass(colIndex);
        try {
	    if (c == Double.class)
		return new Double(s);
	    if (c == Integer.class)
		return new Integer(s);
        }
        catch (NumberFormatException e) {
        }
        return s;
    }

    /** Save the table to the given filename */
    public void saveAsIRSA(String filename) throws IOException {
        saveAs(new FileOutputStream(filename));
    }


    /** Save the table to the given stream */
    public void saveAsIRSA(OutputStream os) {
        int numCols = getColumnIdentifiers().size(),
	    numRows = dataVector.size(),
	    n = numCols - 1;
        if (numCols == 0) {
            return;
	}

	// System specific newline sequence
	String newline = System.getProperty("line.separator");

        PrintStream out;
	if (os instanceof PrintStream)
	    out = (PrintStream)os;
	else
	    out = new PrintStream(os);

        // save the table header
        out.println("\\fixlen = T");
        out.println("\\primary    = 0");
        out.println("\\RowsRetreived =                 " + numRows);
        out.println("\\QueryTime     =   00:00:00.04173");

	// XXX TODO
	// XXX need to get the format string from the data dictionary to get the column widths correct! XXX
	// XXX call _catalog.getFieldDesc()[col].getFormat() for FORTRAN like format string
	
        // save the column headings
	out.print("|");
        for (int col = 0; col < numCols; col++) {
            out.print(getColumnIdentifiers().get(col));
	    out.print("|");
        }
        out.print(newline);

        // save the data types
	out.print("|");
        for (int col = 0; col < numCols; col++) {
            out.print(_columnTypes.get(col));
	    out.print("|");
        }
        out.print(newline);


        // save the units
	out.print("|");
        for (int col = 0; col < numCols; col++) {
            out.print(_columnUnits.get(col));
	    out.print("|");
        }
        out.print(newline);


        // save the null values
	out.print("|");
        for (int col = 0; col < numCols; col++) {
            out.print(_columnNulls.get(col));
	    out.print("|");
        }
        out.print(newline);


        // save the data
        for (int row = 0; row < numRows; row++) {
            Vector rowVec = (Vector) (dataVector.get(row));
            for (int col = 0; col < numCols; col++) {
                out.print(rowVec.get(col));
		out.print(" ");
            }
            out.print(newline);
        }
    }


    /**
     * Test cases
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("usage: java -classpath ... IRSATable filename");
            System.exit(1);
        }
        IRSATable cat = null;
        try {
            cat = new IRSATable(args[0]);
            cat.saveAs(System.out);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("");
        System.out.println("test row,col access:");
        System.out.println("table(0,0) = " + cat.getValueAt(0, 0));
        System.out.println("table(3, ra) = " + cat.getValueAt(3, "ra"));
        System.out.println("table(3, RA) = " + cat.getValueAt(3, "RA"));
        System.out.println("table(3, dec) = " + cat.getValueAt(3, "dec"));
        System.out.println("table(3, Dec) = " + cat.getValueAt(3, "Dec"));
    }
}
