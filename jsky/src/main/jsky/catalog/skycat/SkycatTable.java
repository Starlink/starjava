/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SkycatTable.java,v 1.25 2002/08/16 22:21:13 brighton Exp $
 */

package jsky.catalog.skycat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import jsky.catalog.BasicQueryArgs;
import jsky.catalog.Catalog;
import jsky.catalog.FieldDesc;
import jsky.catalog.FieldDescAdapter;
import jsky.catalog.MemoryCatalog;
import jsky.catalog.QueryArgs;
import jsky.catalog.QueryResult;
import jsky.catalog.RowCoordinates;
import jsky.coords.CoordinateRadius;
import jsky.coords.WorldCoords;


/**
 * Used to read and write Skycat style tab separated
 * catalog data and manage the rows and columns in memory.
 * This class extends the  MemoryCatalog class, which supports
 * searching and working with
 * a JTable widget.
 *
 * @version $Revision: 1.25 $ $Date: 2002/08/16 22:21:13 $
 * @author Allan Brighton
 */
public class SkycatTable extends MemoryCatalog {

    /** Object used to manage the configuration info for this catalog  */
    private SkycatConfigEntry _entry;


    /**
     * Initialize the table from the given stream by reading up to
     * maxRows of the data.
     *
     * @param catalog the catalog where the data originated, if known
     * @param in the stream to read the catalog data from
     * @param maxRows the maximum number of data rows to read
     */
    public SkycatTable(SkycatCatalog catalog, InputStream in, int maxRows) throws IOException {
        super(catalog, in, maxRows);

        if (catalog != null) {
            setConfigEntry(catalog.getConfigEntry());
            setId(_entry.getShortName());
        }

        checkProperties();
    }

    /**
     * Initialize the table from the given stream by reading up to
     * maxRows of the data.
     *
     * @param catalog the catalog where the data originated, if known
     * @param in the stream to read the catalog data from
     * @param queryArgs represents the arguments to the query that resulted in this table
     */
    public SkycatTable(SkycatCatalog catalog, InputStream in, QueryArgs queryArgs) throws IOException {
        this(catalog, in, queryArgs.getMaxRows());
        setQueryArgs(queryArgs);
    }


    /**
     * Initialize the table from the given stream.
     *
     * @param catalog the catalog where the data originated, if known
     * @param in the stream to read the catalog data from
     */
    public SkycatTable(SkycatCatalog catalog, InputStream in) throws IOException {
        this(catalog, in, -1);
    }


    /**
     * Initialize the table from the given file.
     *
     * @param catalog the catalog where the data originated, if known
     * @param filename the name of the catalog file
     */
    public SkycatTable(SkycatCatalog catalog, String filename) throws IOException {
        super(catalog, filename);

        if (catalog != null) {
            setConfigEntry(catalog.getConfigEntry());
            setId(_entry.getShortName());
        }

        setFilename(filename);
	String name = new File(filename).getName();
        setName(name);
	setId(name);
	setTitle(name);

        checkProperties();
    }


    /**
     * Initialize the table from the given file
     *
     * @param cf a reference to a skycat config file object (may be used later to search for a name server)
     * @param filename the name of the catalog file
     */
    public SkycatTable(SkycatConfigFile cf, String filename) throws IOException {
        this((SkycatCatalog) null, filename);
        setConfigEntry(new SkycatConfigEntry(cf, getProperties()));
    }

    /** Initialize the table from the given file */
    public SkycatTable(String filename) throws IOException {
        this(SkycatConfigFile.getConfigFile(), filename);
    }

    /**
     * Construct a new SkycatTable with the given data.
     *
     * @param configEntry a config entry describing the table
     * @param dataVector a vector of data rows, each of which is a vector of column values.
     * @param fields an array of objects describing the table columns
     */
    public SkycatTable(SkycatConfigEntry configEntry, Vector dataRows, FieldDesc[] fields) {
        super(fields, dataRows);
	String name = configEntry.getShortName();
        setName(name);
        setId(name);
        setTitle("Query Results from: " + configEntry.getLongName());
        setConfigEntry(configEntry);
        _initColumnClasses();
    }


    /**
     * Construct a new SkycatTable with the given column fields and data rows
     * (For internal use only).
     *
     * @param table the source catalog table
     * @param fields an array of objects describing the table columns
     * @param dataVector a vector of data rows, each of which is a vector of column values.
     */
    protected SkycatTable(SkycatTable table, FieldDesc[] fields, Vector dataRows) {
        super(fields, dataRows);
        setName(table.getName());
        setId(table.getId());
        setTitle("Query Results from: " + table.getTitle());
        setConfigEntry(table.getConfigEntry());
        _initColumnClasses();
    }

    /**
     * Construct a new SkycatTable with no header or data
     * (For use only by derived classes).
     */
    protected SkycatTable() {
    }

    /**
     * Return the catalog used to create this table,
     * or a dummy, generated catalog object, if not known.
     */
    public Catalog getCatalog() {
	Catalog catalog = super.getCatalog();
        if (catalog != null) 
	    return catalog;

	// If there is no filename, pick a dummy name
	String filename = getFilename();
	if (filename == null)
	    filename = "unknown";

            // if there is no config entry, generate a default one
	if (_entry == null) {
	    Properties properties = new Properties();
	    String name = new File(filename).getName();
	    properties.setProperty(SkycatConfigFile.SERV_TYPE, "local");
	    properties.setProperty(SkycatConfigFile.LONG_NAME, name);
	    properties.setProperty(SkycatConfigFile.SHORT_NAME, name);
	    properties.setProperty(SkycatConfigFile.URL, filename);
	    properties.setProperty(SkycatConfigFile.RA_COL, "-1");
	    properties.setProperty(SkycatConfigFile.DEC_COL, "-1");
	    _entry = new SkycatConfigEntry(properties);
	}
	else {
	    // make sure the entry has the correct type and URL
	    _entry.setServType(Catalog.LOCAL);
	    _entry.setURLs(new String[]{filename});
	}
	return new SkycatCatalog(_entry, this);
    }


    /** Return the object used to manage the configuration info for this catalog  */
    public SkycatConfigEntry getConfigEntry() {
        return _entry;
    }

    /**
     * Set the object used to manage the configuration info for this catalog.
     * The argument may also be the configuration entry for the catalog where the table
     * data originated, in which case a new local catalog entry is created.
     */
    public void setConfigEntry(SkycatConfigEntry entry) {
        Properties p = (Properties) entry.getProperties().clone();
        String servType = p.getProperty(SkycatConfigFile.SERV_TYPE);
        if (servType == null || !servType.equals("local")) {
            p.setProperty(SkycatConfigFile.SERV_TYPE, "local");

            // search_cols doesn't work for local catalogs
            p.remove(SkycatConfigFile.SEARCH_COLS);

	    p.setProperty(SkycatConfigFile.SHORT_NAME, entry.getShortName());
	    p.setProperty(SkycatConfigFile.LONG_NAME, entry.getLongName());

            _entry = new SkycatConfigEntry(p);
        }
        else {
            _entry = entry;
        }

	RowCoordinates rowCoordinates = entry.getRowCoordinates();
        setRowCoordinates(rowCoordinates);
        if (rowCoordinates.isWCS()) {
	    FieldDesc[] fields = getFields();
	    if (fields != null) {
		int idCol = rowCoordinates.getIdCol();
		if (idCol >= 0 && idCol < fields.length)
		    ((FieldDescAdapter)fields[idCol]).setIsId(true);
		    
		int raCol = rowCoordinates.getRaCol();
		if (raCol >= 0 && raCol < fields.length)
		    ((FieldDescAdapter) fields[raCol]).setIsRA(true);
	    
		int decCol = rowCoordinates.getDecCol();
		if (decCol >= 0 && decCol < fields.length)
		    ((FieldDescAdapter) fields[decCol]).setIsDec(true);
	    }
        }
    }

    /**
     * If no properties were defined in the header, add the default
     * settings
     */
    protected void checkProperties() {
	String name = getName();
	String id = getId();
	String title = getTitle();
	Properties properties = getProperties();
        if (properties.getProperty("serv_type") == null) {
            if (name == null) {
                if (id != null)
                    setName(id);
                else
                    setName("unknown");
            }
            if (id == null)
                setId(name);
            if (title == null)
                setTitle(name);
            properties.setProperty("serv_type", "local");
            properties.setProperty("long_name", getName());
            properties.setProperty("short_name", getId());
            properties.setProperty("url", "none");
        }
    }

    /**
     * Return a new MemoryCatalog with the given column fields and data rows.
     *
     * @param fields an array of objects describing the table columns
     * @param dataVector a vector of data rows, each of which is a vector of column values.
     */
    protected MemoryCatalog makeQueryResult(FieldDesc[] fields, Vector dataRows) {
        SkycatTable table = new SkycatTable(this, fields, dataRows);
        table.setProperties(getProperties());
        return table;
    }

    /**
     * Save the table header (part before the column headings)
     * to the given PrintStream.
     * (redefined from the parent class to leave out some properties)
     */
    protected void _saveProperties(PrintStream out) {
	Properties properties = getProperties();
	out.println("# Begin properties");
	for (Enumeration e = properties.propertyNames(); e.hasMoreElements();) {
	    String key = (String) e.nextElement();
	    if (! (key.equals(SkycatConfigFile.LONG_NAME) 
		   || key.equals(SkycatConfigFile.SHORT_NAME)
		   || key.equals(SkycatConfigFile.URL)
		   || key.equals(SkycatConfigFile.SERV_TYPE))) {
		out.println(key + ": " + properties.getProperty(key));
	    }
	}
	out.println("# End properties");
	out.println("");
    }


    /**
     * Test cases
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("usage: java -classpath ... SkycatTable filename");
            System.exit(1);
        }
        SkycatTable cat = null;
        try {
            cat = new SkycatTable(args[0]);
            cat.saveAs(System.out);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("");
        System.out.println("test row,col access:");
        System.out.println("table(0,0) = " + cat.getValueAt(0, 0));
        System.out.println("table(3,4) = " + cat.getValueAt(3, 4));
        System.out.println("table(3, ra) = " + cat.getValueAt(3, "ra"));
        System.out.println("table(3, RA) = " + cat.getValueAt(3, "RA"));
        System.out.println("table(3, dec) = " + cat.getValueAt(3, "dec"));
        System.out.println("table(3, Dec) = " + cat.getValueAt(3, "Dec"));

        try {
            System.out.println("");
            System.out.println("test query: of GSC0285601186");
            QueryArgs q = new BasicQueryArgs(cat);
            q.setId("GSC0285601186");
            QueryResult r = cat.query(q);
            if (r instanceof SkycatTable) {
                SkycatTable table = (SkycatTable) r;
                System.out.println("Number of result rows: " + table.getRowCount());
                if (table.getRowCount() != 0)
                    System.out.println("result: " + ((SkycatTable) r).toString());
            }
            else {
                System.out.println("Failed search by ID");
            }

            System.out.println("");
            System.out.println("test query: at center position/radius: ");
            q = new BasicQueryArgs(cat);
            q.setRegion(new CoordinateRadius(new WorldCoords("03:19:44.44", "+41:30:58.21"), 1.));
            r = cat.query(q);

            if (r instanceof SkycatTable) {
                SkycatTable table = (SkycatTable) r;
                System.out.println("Number of result rows: " + table.getRowCount());
                if (table.getRowCount() != 0)
                    System.out.println("result: " + ((SkycatTable) r).toString());
            }
            else {
                System.out.println("Failed search by position");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}


