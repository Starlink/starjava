// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: MemoryCatalog.java,v 1.13 2002/08/05 10:57:20 brighton Exp $


package jsky.catalog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import jsky.coords.CoordinateRadius;
import jsky.coords.Coordinates;
import jsky.coords.WorldCoordinates;
import jsky.util.Saveable;
import jsky.util.SaveableAsHTML;
import jsky.util.StringTokenizerUtil;

/**
 * Used to manage tabular catalog data in memory, such as the
 * result of a catalog query or the contents of a local catalog file.
 * This class can be easily used with a JTable widget, since it extends the
 * DefaultTableModel class. It can also be used as an in memory catalog for
 * further searches.
 * <p>
 * In ASCII form, the data is represented as a tab separated
 * table with an optional header and column headings.
 * <p>
 * The header may contain comments (starting with '#') or other
 * text. Lines of the form "keyword: value" define properties,
 * which are saved in a Properties object for access by client
 * classes.
 * <p>
 * A dashed line "---" divides the column headings from the data
 * rows. The column headings should be separated by tabs.
 * <p>
 * There is one row per line and each row should have the same
 * number of tab separated columns as the table headings.
 *
 * @version $Revision: 1.13 $
 * @author Allan Brighton
 */
public class MemoryCatalog extends DefaultTableModel
        implements TableQueryResult, Saveable, SaveableAsHTML {

    // Table property names
    public static final String EQUINOX = "equinox";
    public static final String SYMBOL = "symbol";
    public static final String ID_COL = "id_col";
    public static final String RA_COL = "ra_col";
    public static final String DEC_COL = "dec_col";
    public static final String X_COL = "x_col";
    public static final String Y_COL = "y_col";

    /** The catalog corresponding to this table. */
    private Catalog _catalog;

    /** The name of the catalog, or null if not known */
    private String _name;

    /** The id or short name of the catalog, or null if not known */
    private String _id;

    /** The short name or alias for the catalog, or null if not known */
    private String _title;

    /** Optional description text */
    private String _description;

    /** Optional URL pointing to more information about the catalog */
    private URL _docURL;

    /** An object used to access the coordinate values in a row, such as (ra, dec) or (x, y), if present */
    private RowCoordinates _rowCoordinates;

    /** Array of objects describing the table columns */
    private FieldDesc[] _fields;

    /** True if the query result was truncated and more data would have been available */
    private boolean _more = false;

    /** True if the table is read-only and should not be editable */
    private boolean _readOnly = true;

    /** contains keyword/value pairs associated with the table (scanned from the table header) */
    private Properties _properties = new Properties();

    /** Table columns are delimited by this char */
    private String _columnSeparator = "\t";

    /** Used to guess the data type for a column. */
    private Vector _columnClasses;

    /** The filename of the local catalog, if known */
    private String _filename;

    /** If set, represents the arguments to the query that resulted in this table. */
    private QueryArgs _queryArgs;


    /**
     * Create a MemoryCatalog with the given information.
     *
     * @param fields an array of objects describing the table columns
     * @param dataVector a vector of data rows, each of which is a vector of column values.
     * @param more true if the query result was truncated and more data would have been available.
     */
    public MemoryCatalog(FieldDesc[] fields, Vector dataVector) {
        _fields = fields;
        this.dataVector = dataVector;

        if (_fields == null)
            throw new RuntimeException("No columns defined for MemoryCatalog");

        // these are needed by the TableModel interface
        columnIdentifiers = makeColumnIdentifiers(_fields);
    }


    /**
     * Initialize the table from the given stream by reading up to
     * maxRows of the data (in tab separated table format).
     *
     * @param catalog the catalog where the data originated, if known
     * @param in the stream to read the table data from
     * @param maxRows the maximum number of data rows to read
     */
    public MemoryCatalog(Catalog catalog, InputStream in, int maxRows) throws IOException {
        _catalog = catalog;
        _init(in, maxRows);

        if (_catalog != null) {
            setId(catalog.getId());
            setTitle("Query Results from: " + _catalog.getTitle());
        }
    }

    /**
     * Initialize the table from the given stream.
     *
     * @param catalog the catalog where the data originated, if known
     * @param in the stream to read the catalog data from
     */
    public MemoryCatalog(Catalog catalog, InputStream in) throws IOException {
        this(catalog, in, -1);
    }

    /**
     * Initialize the table from the given stream by reading up to
     * maxRows of the data.
     *
     * @param catalog the catalog where the data originated, if known
     * @param in the stream to read the catalog data from
     * @param queryArgs represents the arguments to the query that resulted in this table
     */
    public MemoryCatalog(Catalog catalog, InputStream in, QueryArgs queryArgs) throws IOException {
        this(catalog, in, queryArgs.getMaxRows());
        _queryArgs = queryArgs;
    }

    /**
     * Initialize the table from the given file.
     *
     * @param catalog the catalog where the data originated, if known
     * @param filename the name of the catalog file
     */
    public MemoryCatalog(Catalog catalog, String filename) throws IOException {
        this(catalog, new FileInputStream(filename));
        _filename = filename;
	String name = new File(filename).getName();
        setName(name);
	setId(name);
	setTitle(name);
    }

    /**
     * Initialize the table from the given file
     *
     * @param filename the name of the catalog file
     */
    public MemoryCatalog(String filename) throws IOException {
        this((Catalog)null, filename);
    }


    /**
     * Construct a new MemoryCatalog with the given column fields and data rows
     * (For internal use only).
     *
     * @param table the source catalog table
     * @param fields an array of objects describing the table columns
     * @param dataVector a vector of data rows, each of which is a vector of column values.
     */
    public MemoryCatalog(MemoryCatalog table, FieldDesc[] fields, Vector dataRows) {
        this(fields, dataRows);
        setName(table.getName());
        setId(table.getId());
        setTitle("Query Results from: " + table.getTitle());
        _initColumnClasses();
    }


    /**
     * This constructor is only for use by derived classes.
     * The derived class must take care of initializing the "fields",
     * "dataVector", and "columnIdentifiers" member variables.
     */
    protected MemoryCatalog() {
    }


    /**
     * Implementation of the clone method (makes a shallow copy).
     */
    public Object clone() {
	try {
	    return super.clone();
	} 
	catch (CloneNotSupportedException ex) {
	    throw new InternalError(); // won't happen
	}
    }


    /** Set a reference to the catalog used to create this table. */
    public void setCatalog(Catalog cat) {
        _catalog = cat;
    }

    /**
     * Return the catalog used to create this table, or null if not known.
     */
    public Catalog getCatalog() {
        return _catalog;
    }


    /**
     * Initialize the table from the given stream, reading at most maxRows data rows.
     */
    protected void _init(InputStream in, int maxRows) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line = null, prevLine = null;

        // read the table header
        while (true) {
            line = reader.readLine();
            if (line == null)
                break;

            // check for error message starting with "***" - a convention of skycat servers
            if (line.startsWith("***"))
                throw new RuntimeException(line.substring(3));

            if (line.length() == 0 || line.charAt(0) == '#')
                continue;	// empty line or comment

            if (line.charAt(0) == '-') {
                // dashed line marks end of header
                if (prevLine == null)
                    throw new IOException("Can't parse tab table header");
                setColumnIdentifiers(_parseHeading(prevLine));
                _initFields();
                break;
            }
            prevLine = line;

            // Check for "keyword: value" pairs in the header and save them
            // as properties
            _parseProperty(line);
        }

        // initialize the column class vector to nulls and set to
        // String or Double later as needed
        Vector columnIdentifiers = getColumnIdentifiers();
        if (columnIdentifiers == null) {
            // no header, empty table
            setColumnIdentifiers(new Vector());
            dataVector = new Vector();
            return;
        }
        int n = columnIdentifiers.size();
        _columnClasses = new Vector(n, 1);
        for (int i = 0; i < n; i++)
            _columnClasses.add(null);

        // read the table data
        dataVector = new Vector(1024, 256);
        int nrows = 1;
        while (true) {
            line = reader.readLine();
            if (line == null || line.equals("[EOD]"))
                break;
            dataVector.add(_parseRow(line));
            if (maxRows > 0 && nrows++ >= maxRows)
                break;
        }
    }

    /**
     * Check for a "keyword: value" pair in the given string and, if found,
     * save it as a property.
     */
    protected void _parseProperty(String s) {
        int i = s.indexOf(':');
        if (i != -1) {
            String key = s.substring(0, i).trim();
            String value = s.substring(i + 1).trim();
            _properties.setProperty(key, value);
        }
    }


    /**
     * Parse the given line looking for tab separated column heading strings
     * and return a vector containing the strings found.
     *
     * @param s A line containing tab separated column headings.
     * @return A vector of column heading Strings.
     */
    protected Vector _parseHeading(String s) {
        StringTokenizerUtil st = new StringTokenizerUtil(s, _columnSeparator);
        Vector v = new Vector(st.countTokens(), 1);
        while (st.hasMoreTokens()) {
            v.add(st.nextToken().trim());
        }
        return v;
    }


    /** Initialize the fields array, which describes the table columns */
    protected void _initFields() {
        int n = getColumnIdentifiers().size();
        FieldDescAdapter[] fields = new FieldDescAdapter[n];
        for (int i = 0; i < n; i++) {
            fields[i] = new FieldDescAdapter((String) (getColumnIdentifiers().get(i)));
        }

	// determine which columns are RA and Dec, if any
	RowCoordinates rowCoordinates = getRowCoordinates();
	if (rowCoordinates != null && rowCoordinates.isWCS()) {
	    int raCol = rowCoordinates.getRaCol();
	    if (raCol >= 0 && raCol < n)
		fields[raCol].setIsRA(true);
	    int decCol = rowCoordinates.getDecCol();
	    if (decCol >= 0 && decCol < n)
		fields[decCol].setIsDec(true);
	}

	setFields(fields);
    }


    /**
     * Parse the given table row looking for tab separated items.
     * If fewer than numCols columns are found, set the rest to "".
     * Any extra columns are silently ignored.
     * <p>
     * Item strings that look like numbers are inserted into the
     * result as Doubles.
     *
     * @param lineStr A string containing a line from the table.
     * @return A Vector containing the items in the row.
     */
    protected Vector _parseRow(String lineStr) {
        StringTokenizerUtil st = new StringTokenizerUtil(lineStr, _columnSeparator);
        int numCols = getColumnIdentifiers().size();
        int i = 0;
        Vector row = new Vector(numCols, 1);

        while (st.hasMoreTokens()) {
            if (i++ >= numCols)
                break;
            String s = st.nextToken().trim();
            if (s.length() != 0) {
                Object o = _parseItem(s);
                _checkColumnClass(i - 1, o);
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
     * Parse the given string and return a Double or String
     * object, depending on the format of the string.
     *
     * @param s A String to be parsed.
     * @return A Double or String object.
     */
    protected Object _parseItem(String s) {
        try {
            return new Double(s);
        }
        catch (NumberFormatException e) {
        }
        return s;
    }

    /**
     * Check the class type for the given column to see if all of the
     * items are the same type and update the _columnClasses vector, setting
     * the correct lowest common class for each column.
     *
     * @param col The column index
     * @param o   The item in the column
     */
    protected void _checkColumnClass(int col, Object o) {
        Class t = (Class) _columnClasses.get(col);
        Class c = o.getClass();
        if (t == null) {
            _columnClasses.set(col, c);
        }
        else if (!c.equals(t)) {
            _columnClasses.set(col, Object.class);
        }
    }

    /** Allow access to this inherited member variable in a subclass */
    public Vector getColumnIdentifiers() {
        return columnIdentifiers;
    }

    /**
     * Create and return a vector of table column identifiers (column headings)
     * based on the given fields.
     *
     * @return a vector of Strings
     */
    protected Vector makeColumnIdentifiers(FieldDesc[] fields) {
        Vector v = new Vector(fields.length, 1);
        for (int i = 0; i < fields.length; i++) {
            String label = fields[i].getName();
            if (label == null)
                label = "";
            v.add(label);
        }
        return v;
    }

    /** Return the name of the catalog  */
    public String getName() {
        if (_name != null)
            return _name;
        if (_id != null)
            return _id;
	if (_filename != null)
	    return new File(_filename).getName();
        return "unknown";
    }

    /** Set the name for the catalog */
    public void setName(String name) {
        _name = name;
    }

    /** Return the filename of the local catalog, if known */
    public String getFilename() {
	return _filename;
    }

    /** Set the filename of the local catalog */
    public void setFilename(String filename) {
	_filename = filename;
    }

    /** Return the id or short name of the catalog  */
    public String getId() {
        return _id;
    }

    /** Set the id or short name for the catalog */
    public void setId(String id) {
        _id = id;
    }

    /** Return the title for the catalog  */
    public String getTitle() {
        return _title;
    }

    /** Set the title for the catalog */
    public void setTitle(String title) {
        _title = title;
    }

    /** Return a description of the catalog, or null if not available */
    public String getDescription() {
        return _description;
    }

    /** Set the description of the catalog */
    public void setDescription(String description) {
        _description = description;
    }

    /** Return the documentation URL for the catalog */
    public URL getDocURL() {
        return _docURL;
    }

    /** Set the doc URL for the catalog */
    public void setDocURL(URL docURL) {
        _docURL = docURL;
    }


    /**
     * Return the number of query parameters that this catalog accepts.
     * In this case, since we only have a table and no catalog server,
     * the query parameters are the same as the table columns, and the
     * parameter values may be ValueRange objects.
     */
    public int getNumParams() {
        return _fields.length;
    }

    /**
     * Return a description of the ith query parameter.
     * In this case, since we only have a table and no catalog server,
     * the query parameters are the same as the table columns, and the
     * parameter values may be ValueRange objects.
     */
    public FieldDesc getParamDesc(int i) {
        return _fields[i];
    }

    /**
     * Return a description of the named query parameter.
     * In this case, since we only have a table and no catalog server,
     * the query parameters are the same as the table columns, and the
     * parameter values may be ValueRange objects.
     */
    public FieldDesc getParamDesc(String name) {
	for(int i = 0; i < _fields.length; i++)
	    if (_fields[i] != null && _fields[i].getName().equals(name))
		return _fields[i];
	return null;
    }

    /**
     * Return the number of table columns..
     */
    public int getNumColumns() {
        return _fields.length;
    }

    /**
     * Return a description of the ith table column parameter.
     */
    public FieldDesc getColumnDesc(int i) {
        return _fields[i];
    }

    /**
     * This method is required to implement the Catalog interface, but currently does nothing here.
     */
    public void setRegionArgs(QueryArgs queryArgs, CoordinateRadius region) {
    }

    /**
     * Return true if this is a local catalog, and false if it requires
     * network access or if a query could hang. A local catalog query is
     * run in the event dispatching thread, while others are done in a
     * separate thread.
     */
    public boolean isLocal() {
        return true;
    }

    /**
     * Return true if this object represents an image server.
     */
    public boolean isImageServer() {
        return false;
    }

    /** Return the catalog type (normally one of the Catalog constants: CATALOG, ARCHIVE, DIRECTORY, LOCAL, IMAGE_SERVER) */
    public String getType() {
        return Catalog.LOCAL;
    }

    /** Set the parent catalog directory */
    public void setParent(CatalogDirectory catDir) {
    }

    /** Return a reference to the parent catalog directory, or null if not known. */
    public CatalogDirectory getParent() {
	return null;
    }


    /** 
     * Return an array of Catalog or CatalogDirectory objects representing the 
     * path from the root catalog directory to this catalog.
     */
    public Catalog[] getPath() {
	return null;
    }


    /**
     * Query the catalog using the given argument and return the result.
     *
     * @param queryArgs An object describing the query arguments.
     * @return An object describing the result of the query.
     */
    public QueryResult query(QueryArgs queryArgs) throws IOException {
        // max rows that were requested (add 1 to check if more rows were available)
        int maxRows = queryArgs.getMaxRows();

        // if not null, search for an object with this id
        String objectId = queryArgs.getId();

        // This vector holds the rows found
        Vector dataRows = new Vector(Math.min(maxRows, 1024), Math.min(maxRows, 256));

        // Get the region to search
        CoordinateRadius region = queryArgs.getRegion();

        // Specifies the ranges for column values we are search for.
        SearchCondition[] conditions = queryArgs.getConditions();

        // Get the numerical indexes for the search columns
        int[] searchCols = null;
        if (conditions != null) {
            // get the numerical indexes for the search columns
            searchCols = new int[conditions.length];
            for (int i = 0; i < conditions.length; i++)
                searchCols[i] = getColumnIndex(conditions[i].getName());
        }

        // search each row...
        int n = 0, numRows = dataVector.size();
        for (int i = 0; i < numRows; i++) {
            Vector row = (Vector) dataVector.get(i);
            if (compareRow(row, objectId, region, conditions, searchCols)) {
                dataRows.add(row);
                if (maxRows != 0 && ++n >= maxRows + 1)
                    break;
            }
        }

        MemoryCatalog result = makeQueryResult(_fields, dataRows);
        result.setName(_name);
        result.setTitle(_title);
        result.setDescription(_description);
        result.setDocURL(_docURL);
        result.setRowCoordinates(_rowCoordinates);

        // Truncate the result, if needed
        if (maxRows != 0 && n > maxRows) {
            result.setMore(true);
            result.setNumRows(maxRows);
        }
        else {
            result.setMore(false);
        }

        return result;
    }


    /**
     * Return a new MemoryCatalog with the given column fields and data rows.
     *
     * @param fields an array of objects describing the table columns
     * @param dataVector a vector of data rows, each of which is a vector of column values.
     */
    protected MemoryCatalog makeQueryResult(FieldDesc[] fields, Vector dataRows) {
        MemoryCatalog table = new MemoryCatalog(this, fields, dataRows);
        table.setProperties(getProperties());
        return table;
    }


    /** Format and return the contents of the table as a String */
    public String toString() {
        return getClass().getName() + ":" + getName();
    }


    /**
     * Return true if the given row satisfies the given query arguments.
     *
     * @param row        A vector containing the column values for a row.
     * @param objectId   If not null, search for an object with this id.
     * @param region     The region (center, radius) for a circular search, if not null.
     * @param conditions Specifies the ranges for column values we are search for.
     * @param searchCols An array of column indexes corresponding to the condition
     *                   argument (used for efficiency).
     *
     * @return True if the row satisfies the conditions, otherwise false.
     */
    protected boolean compareRow(Vector row, String objectId,
                                 CoordinateRadius region,
                                 SearchCondition[] conditions,
                                 int[] searchCols) {

        if (_rowCoordinates != null) {
            if (region != null) {
                // check if the row coordinates are within the given region
                Coordinates pos = _rowCoordinates.getCoordinates(row);
                if (pos == null || !region.contains(pos))
                    return false;	// position not found or not in region
            }

            int idCol = _rowCoordinates.getIdCol();
            if (objectId != null && idCol >= 0) {
                Comparable tableValue = (Comparable) row.get(idCol);
                if (tableValue == null || !tableValue.equals(objectId)) {
                    return false; // no match
                }
            }
        }

        // check any other conditions for column values
        if (conditions == null)
            return true;	// no condition, just center pos/radius

        int n = conditions.length;
        if (n > 0) {
            for (int i = 0; i < n; i++) {
                Comparable tableValue = (Comparable) row.get(searchCols[i]);
                if (tableValue == null || !conditions[i].isTrueFor(tableValue)) {
                    return false; // no match
                }
            }
        }
        return true;			// a match
    }


    /** Return true if the query result was truncated and more data would have been available */
    public boolean isMore() {
        return _more;
    }

    /** Called to indicate if the result was truncated and more rows would have been available */
    public void setMore(boolean more) {
        _more = more;
    }

    /** Set the table column headings with a Vector of Strings */
    public void setColumnIdentifiers(Vector columnIdentifiers) {
        this.columnIdentifiers = columnIdentifiers;
    }


    /** Add a row to the table */
    public void addRow(Vector row) {
        dataVector.add(row);
    }


    /** return true if the table contains the given column */
    public boolean hasCol(String name) {
        return (getColumnIndex(name) >= 0);
    }

    /** Return the value at the given row and columm name */
    public Object getValueAt(int row, String name) {
        return getValueAt(row, getColumnIndex(name));
    }

    /**
     * Return the table column index for the given column name
     */
    public int getColumnIndex(String name) {
        int numCols = columnIdentifiers.size();
        for (int i = 0; i < numCols; i++) {
            if (name.equalsIgnoreCase((String) columnIdentifiers.get(i)))
                return i;
        }
        return -1;
    }

    /**
     * Return the table column name for the given column index
     */
    public String getColumnName(int index) {
        return (String) (columnIdentifiers.get(index));
    }

    /**
     * Returns the lowest common denominator Class in the column.  This is used
     * by the table to set up a default renderer and editor for the column.
     *
     * @return the common ancestor class of the object values in the model.
     */
    public Class getColumnClass(int columnIndex) {
        Object o = _columnClasses.get(columnIndex);
        if (o != null && o instanceof Class)
            return (Class) o;
        return String.class;
    }

    /** Return the vector used to guess the data type for a column. */
    public Vector getColumnClasses() {return _columnClasses;}

    /** Set the vector used to guess the data type for a column. */
    public void setColumnClasses(Vector v) {_columnClasses = v;}


    /** Return an array of objects describing the table columns */
    public FieldDesc[] getFields() {
        return _fields;
    }

    /** Set the array of objects describing the table columns. */
    public void setFields(FieldDesc[] fields) {
	_fields = fields;
    }

    /**
     * Returns true if the cell at <I>rowIndex</I> and <I>columnIndex</I>
     * is editable.  Otherwise, setValueAt() on the cell will not change
     * the value of that cell.
     *
     * @param	rowIndex	the row whose value is to be looked up
     * @param	columnIndex	the column whose value is to be looked up
     * @return	true if the cell is editable.
     * @see #setValueAt
     */
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return !_readOnly;
    }

    /** Return true if the table is read-only and should not be editable */
    public boolean isReadOnly() {
        return _readOnly;
    }

    /** Set to true if the table is read-only and should not be editable */
    public void setReadOnly(boolean b) {
        _readOnly = b;
    }

    /**
     * Sort the contents of the table by the given sort columns and in the
     * given order.
     *
     * @param sortCols an array of column names to sort by
     * @param sortOrder if true, sort in ascending order, otherwise descending.
     */
    public void sort(String[] sortCols, String sortOrder) {
        // XXX not impl
    }


    /** Return true if the table has coordinate columns, such as (ra, dec) */
    public boolean hasCoordinates() {
        return (_rowCoordinates != null);
    }


    /**
     * Return a Coordinates object based on the appropriate columns in the given row,
     * or null if there are no coordinates available for the row.
     */
    public Coordinates getCoordinates(int rowIndex) {
        if (_rowCoordinates != null) {
            Vector row = (Vector) dataVector.get(rowIndex);
            return _rowCoordinates.getCoordinates(row);
        }
        return null;
    }

    public void print() {
        System.out.println("table: " + getTitle());
        int numCols = getColumnCount();
        int numRows = getRowCount();
        for (int i = 0; i < numCols; i++) {
            System.out.println("col(" + i + ") = " + getColumnName(i));
        }
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                System.out.println("table(" + i + "," + j + ") = " + getValueAt(i, j));
            }
        }
    }

    /** Save the table to the given filename format. */
    public void saveAs(String filename) throws IOException {
        saveAs(new FileOutputStream(filename));
    }


    /** Save the table to the given stream */
    public void saveAs(OutputStream os) {
        int numCols = getColumnIdentifiers().size(),
	    numRows = dataVector.size(),
	    n = numCols - 1;
        if (numCols == 0) {
            return;
	}

        PrintStream out;
	if (os instanceof PrintStream)
	    out = (PrintStream)os;
	else
	    out = new PrintStream(os);

	// System specific newline sequence
	String newline = System.getProperty("line.separator");

        // save the table header
        _saveHeader(out);

        // save the column headings
        for (int col = 0; col < numCols; col++) {
            out.print(getColumnIdentifiers().get(col));
            if (col < n)
                out.print(_columnSeparator);
        }
        out.print(newline);

        // dashed line
        for (int col = 0; col < numCols; col++) {
            int l = ((String) (getColumnIdentifiers().get(col))).length();
            for (int i = 0; i < l; i++)
                out.print("-");
            if (col < n)
                out.print(_columnSeparator);
        }
        out.print(newline);

        // save the data
        for (int row = 0; row < numRows; row++) {
            Vector rowVec = (Vector) (dataVector.get(row));
            for (int col = 0; col < numCols; col++) {
                out.print(rowVec.get(col));
                if (col < n)
                    out.print(_columnSeparator);
            }
            out.print(newline);
        }
    }

    /**
     * Save the table header (part before the column headings)
     * to the given PrintStream.
     */
    protected void _saveHeader(PrintStream out) {
        out.println("Table");
        out.println("");

	// Set some properties if needed to be included in the header.
	// Default first 3 table columns are id, ra, dec
	RowCoordinates rowCoordinates = getRowCoordinates();
	if (rowCoordinates != null) {
	    int idCol = rowCoordinates.getIdCol();
	    if (idCol != 0)
		setProperty(ID_COL, String.valueOf(idCol));
	    int raCol = rowCoordinates.getRaCol();
	    if (raCol != 1)
		setProperty(RA_COL, String.valueOf(raCol));
	    int decCol = rowCoordinates.getDecCol();
	    if (decCol != 2)
		setProperty(DEC_COL, String.valueOf(decCol));
	}
	Catalog cat = getCatalog();
	if (cat instanceof PlotableCatalog) {
	    TablePlotSymbol[] symbols = ((PlotableCatalog)cat).getSymbols();
	    String symbolInfo = TablePlotSymbol.getPlotSymbolInfo(symbols);
	    if (symbolInfo != null)
		setProperty(SYMBOL, symbolInfo);
	}

        if (_properties.size() > 0) {
	    _saveProperties(out);
        }
    }

    /**
     * Save the table header (part before the column headings)
     * to the given PrintStream.
     */
    protected void _saveProperties(PrintStream out) {
	out.println("# Begin properties");
	for (Enumeration e = _properties.propertyNames(); e.hasMoreElements();) {
	    String key = (String) e.nextElement();
	    out.println(key + ": " + _properties.getProperty(key));
	}
	out.println("# End properties");
	out.println("");
    }


    /** Save the table to the given filename in HTML format */
    public void saveAsHTML(String filename) throws IOException {
        FileOutputStream os = new FileOutputStream(filename);
        int numCols = getColumnIdentifiers().size(),
                numRows = dataVector.size(),
                n = numCols - 1;
        if (numCols == 0)
            return;

        PrintStream out = new PrintStream(os);

        // table title
        out.println("<html>");
        out.println("<body>");
        out.println("<table BORDER COLS=" + numCols + " WIDTH=\"100%\" NOSAVE>");
        out.println("<caption>" + getTitle() + "</caption>");

        // column headings
        out.println("<tr>");
        for (int col = 0; col < numCols; col++) {
            out.println("<th>" + getColumnIdentifiers().get(col) + "</th>");
        }
        out.println("</tr>");

        // data rows
        for (int row = 0; row < numRows; row++) {
            Vector rowVec = (Vector) (dataVector.get(row));
            out.println("<tr>");
            for (int col = 0; col < numCols; col++) {
                out.println("<td>" + rowVec.get(col) + "</td>");
            }
            out.println("</tr>");
        }
        out.println("</table>");
        out.println("</body>");
        out.println("</html>");
    }


    /**
     * Return the object representing the arguments to the query that resulted in this table,
     * if known, otherwise null.
     */
    public QueryArgs getQueryArgs() {
        return _queryArgs;
    }

    /**
     * Set the object representing the arguments to the query that resulted in this table.
     */
    public void setQueryArgs(QueryArgs queryArgs) {
        _queryArgs = queryArgs;
    }


    /** 
     * Return an object storing the column indexes where RA and Dec are found, 
     * using the first plot symbol definition found.
     */
    public RowCoordinates getRowCoordinates() {
	if (_rowCoordinates != null)
	    return _rowCoordinates;

	if (_catalog instanceof PlotableCatalog) {
	    PlotableCatalog cat = (PlotableCatalog)_catalog;
	    if (cat.getNumSymbols() != 0) {
		TablePlotSymbol plotSym = cat.getSymbolDesc(0);
		_rowCoordinates = plotSym.getRowCoordinates();
	    }
	}

	return _rowCoordinates;
    }

    /** Set the RowCoordinates object for this catalog */
    public void setRowCoordinates(RowCoordinates rowCoordinates) {
        _rowCoordinates = rowCoordinates;
    }


    /**
     * Return the center coordinates for this table from the query arguments,
     * if known, otherwise return the coordinates of the first row, or null
     * if there are no world coordinates available.
     */
    public WorldCoordinates getWCSCenter() {
        if (_queryArgs != null && _queryArgs.getRegion() != null) {
            Coordinates pos = _queryArgs.getRegion().getCenterPosition();
            if (pos instanceof WorldCoordinates)
                return (WorldCoordinates) pos;
        }
	
	int nrows = getRowCount();
	if (nrows != 0) {
	    RowCoordinates rowCoordinates = getRowCoordinates();
	    if (rowCoordinates != null && rowCoordinates.isWCS()) {
		Vector dataVec = getDataVector();
		Vector rowVec = (Vector) dataVec.get(0);
		Coordinates pos = rowCoordinates.getCoordinates(rowVec);
		if (pos instanceof WorldCoordinates)
		    return (WorldCoordinates) pos;
	    }
	}
	return null;
    }


    /** Determine the best class to use for each column by scanning the table items. */
    protected void _initColumnClasses() {
        int numCols = getColumnIdentifiers().size();
        int numRows = dataVector.size();
        _columnClasses = new Vector(numCols, 1);

        for (int col = 0; col < numCols; col++) {
            _columnClasses.add(null);
        }

        for (int row = 0; row < numRows; row++) {
            Vector rowVec = (Vector) dataVector.get(row);
            int n = rowVec.size();
            for (int col = 0; col < n; col++) {
                Object o = rowVec.get(col);
                if (o != null)
                    _checkColumnClass(col, o);
            }
        }
    }


    /** Return the properties defined in the table header */
    public Properties getProperties() {
        return _properties;
    }

    /** Replace the table properties */
    public void setProperties(Properties p) {
        _properties = p;
    }

    /** Return the value of the named property as a String */
    public String getProperty(String key) {
        return _properties.getProperty(key);
    }

    /** Set the value of the named property */
    public void setProperty(String key, String value) {
        _properties.setProperty(key, value);
    }


    /**
     * Test cases
     */
    public static void main(String[] args) {
        // generate a 2 X 3 table to test with
        int numRows = 6, numCols = 4;

        // set the column descriptions
        FieldDescAdapter[] columns = new FieldDescAdapter[numCols];
        for (int i = 0; i < numCols; i++) {
            columns[i] = new FieldDescAdapter("Col-" + i);
        }

        // set the table data
        Vector rows = new Vector(numRows, 1);
        for (int i = 0; i < numRows; i++) {
            Vector cols = new Vector(numCols, 1);
            for (int j = 0; j < numCols; j++) {
                cols.add("item-" + i + "," + j);
            }
            rows.add(cols);
        }

        // create a memory catalog
        MemoryCatalog cat = new MemoryCatalog(columns, rows);
        cat.setName("test cat");

        // test row,col access
        System.out.println("test row,col access:");
        for (int i = 0; i < numCols; i++) {
            System.out.println("col(" + i + ") = " + cat.getColumnName(i));
        }
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                System.out.println("table(" + i + "," + j + ") = " + cat.getValueAt(i, j));
            }
        }

        // test query
        try {
            System.out.println("test query:");
            QueryArgs queryArgs = new BasicQueryArgs(cat);
            queryArgs.setParamValue("Col-2", "item-2,2");
            TableQueryResult result = (TableQueryResult) cat.query(queryArgs);
            System.out.println("result rows: " + result.getRowCount());
            for (int i = 0; i < result.getColumnCount(); i++) {
                System.out.println("col(" + i + ") = " + result.getColumnName(i));
            }
            for (int i = 0; i < result.getRowCount(); i++) {
                for (int j = 0; j < result.getColumnCount(); j++) {
                    System.out.println("table(" + i + "," + j + ") = " + result.getValueAt(i, j));
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
