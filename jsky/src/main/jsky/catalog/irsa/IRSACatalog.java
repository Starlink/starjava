// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: IRSACatalog.java,v 1.3 2002/08/20 09:57:58 brighton Exp $

package jsky.catalog.irsa;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Vector;

import javax.swing.JComponent;

import jsky.catalog.BasicQueryArgs;
import jsky.catalog.Catalog;
import jsky.catalog.CatalogDirectory;
import jsky.catalog.FieldDesc;
import jsky.catalog.FieldDescAdapter;
import jsky.catalog.HTMLQueryResultHandler;
import jsky.catalog.PlotableCatalog;
import jsky.catalog.QueryArgs;
import jsky.catalog.QueryResult;
import jsky.catalog.SearchCondition;
import jsky.catalog.TablePlotSymbol;
import jsky.catalog.TableQueryResult;
import jsky.catalog.URLQueryResult;
import jsky.catalog.gui.CatalogUIHandler;
import jsky.catalog.gui.QueryResultDisplay;
import jsky.coords.CoordinateRadius;
import jsky.coords.Coordinates;
import jsky.coords.WorldCoords;
import jsky.util.NameValue;
import jsky.util.StringUtil;
import jsky.util.gui.ProgressBarFilterInputStream;
import jsky.util.gui.ProgressPanel;
import jsky.util.Logger;


/**
 * Represents a catalog server, as described in an 
 * <a href="http://irsa.ipac.caltech.edu/">IRSA</a> 
 * XML catalog description file. 
 * This class is responsible for generating the catalog query.
 *
 * @version $Revision: 1.3 $
 * @author Allan Brighton
 */
public class IRSACatalog implements PlotableCatalog, CatalogUIHandler {

    // constants for SQL select menu
    static final Integer MINI_COLUMN_LIST = new Integer(0);
    static final Integer SHORT_COLUMN_LIST = new Integer(1);
    static final Integer STANDARD_COLUMN_LIST = new Integer(2);
    static final Integer ALL_COLUMNS = new Integer(3);
    static final Integer CUSTOM_SQL = new Integer(4);

    // constants for search parameter names
    static final String OBJECT = "Object";
    static final String NAME_SERVER = "Name Server";
    static final String RA = "RA";
    static final String DEC = "Dec";
    static final String EQUINOX = "Equinox";
    static final String RADIUS = "Radius";
    static final String SELECT = "Select";
    static final String MAX_OBJECTS = "Max Objects";

    // Options for equinox parameter
    private static final String J2000 = "2000";
    private static final String B1950 = "1950";
    private static NameValue[] _equinoxOptions = new NameValue[] {
	new NameValue("J2000", J2000),
	new NameValue("B1950", B1950)
    };


    // The URL of the XML file describing this catalog, if known
    private URL _url;

    // Optional link to the parent config file object, or null for the root config file
    private CatalogDirectory _parent;

    private String _id;          // catalog id (IRSA name)
    private String _name;        // catlog name (IRSA description)
    private String _description; // same as _name here by default

    // URL for the web page
    private String _docURL = "http://irsa.ipac.caltech.edu/";
    private String _type = Catalog.CATALOG;

    // catalog server URL info
    private String _protocol = "http";
    private String _host = "irsa.ipac.caltech.edu";
    private int    _port = 80;
    private String _path = "/cgi-bin/Oasis/CatSearch/nph-catsearch";

    // path to the data dictionary cgi-bin server, which returns an XML file 
    // describing the table columns and is needed to construct the SQL select 
    // statement parameter
    private String _ddPath = "/cgi-bin/Oasis/DD/nph-dd";

    // IRSA parameters
    private String _server;     // IRSA server id to pass to cgi-bin script (not hostname)
    private String _database;   // IRSA database id
    private String _ddname;     // IRSA Data dictionary id

    // Array of query parameters for this catalog
    private FieldDescAdapter[] _paramDesc;

    // Optional array of catalog table plot symbol definitions for use with this catalog
    private TablePlotSymbol[] _symbols;

    // Set to true if the user edited the plot symbol definitions
    private boolean _symbolsEdited = false;

    // Optional handler, used to report HTML format errors from servers
    private HTMLQueryResultHandler _htmlQueryResultHandler;

    // Panel used to display download progress information
    private ProgressPanel _progressPanel;

    /** If this is a local catalog, this may optionally point to the data */
    private IRSATable _table;

    // Custom GUI component that includes SQL support
    private JComponent _uiComponent;
    
    // Array of column descriptions downloaded from the IRSA data dictionary
    private IRSAFieldDesc[] _fieldDesc;


    /** 
     * Create an IRSACatalog with the given settings (normally from the nph-catlist.xml file)
     *
     * @param desc IRSA catalog title
     * @param server IRSA server id to pass to cgi-bin script (not hostname)
     * @param database IRSA database id
     * @param catname IRSA catalog id
     * @param ddname IRSA Data dictionary id
     * @param url the URL of the XML file containing the description of this catalog
     */
    public IRSACatalog(String desc, String server, String database, String catname, String ddname, URL url) {
	_name = desc;
	_id = catname;
	_description = "IRSA Catalog Search Service";
	_server = server;
	_database = database;
	_ddname = ddname;
	_url = url;
    }

    
    /** 
     * Create an IRSACatalog object for searching a local catalog file in the IRSA format.
     *
     * @param file the local file
     * @param table the table data for the catalog (optional)
     */
    public IRSACatalog(File file, IRSATable table) {
	_type = LOCAL;
	_name = _id = _description = file.getName();
	_description = "Accessed via the NASA/IPAC Infrared Science Archive";
	_protocol = "file";
	_host = "localhost";
	_path = file.getPath();
	_table = table;
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


    /** Return the URL of the XML file describing this catalog, if known, otherwise null. */
    public URL getURL() {
	return _url;
    }

    /** Set the URL of the XML file describing this catalog. */
    public void setURL(URL url) {
	_url = url;
    }


    /* Return the link to the parent catalog directory, or null for the root or if not known.  */
    public CatalogDirectory getParent() {
        return _parent;
    }

    /* Set the link to the parent catalog directory (null for the root config file) */
    public void setParent(CatalogDirectory dir) {
            _parent = dir;
    }


    /** 
     * Return an array of Catalog or CatalogDirectory objects representing the 
     * path from the root catalog directory to this catalog.
     */
    public Catalog[] getPath() {
	if (_parent == null)
	    return null;

	return _parent.getPath(this);
    }


    /** Return the name of the catalog */
    public String toString() {
        return getName();
    }

    // -- Set catalog server properties --

    /** Set the catalog ID (short name) */
    public void setId(String id) {
        _id = id;
    }

    /** Return the Id or short name of the catalog */
    public String getId() {
        return _id;
    }

    /** Set the display name of the catalog */
    public void setName(String name) {
        _name = name;
    }

    /** Return the name of the catalog */
    public String getName() {
        return _name;
    }

    /** Set the catalog description or copyright info */
    public void setDescription(String description) {
        _description = description;
    }

    /** Return a description of the catalog, or null if not available */
    public String getDescription() {
        return _description;
    }

    /** Set a URL pointing to more information about the catalog */
    public void setDocURL(String docURL) {
        _docURL = docURL;
    }

    /** Return a URL pointing to documentation for the catalog, or null if not available */
    public URL getDocURL() {
	try {
	    return new URL(_docURL);
	}
	catch(Exception e) {
	    return null;
	}
    }

    /** Set the catalog type (one of the constants: CATALOG, ARCHIVE, DIRECTORY, LOCAL, IMAGE_SERVER) */
    public void setType(String type) {
        _type = type;
    }

    /** Return the catalog type (one of the constants: CATALOG, ARCHIVE, DIRECTORY, LOCAL, IMAGE_SERVER) */
    public String getType() {
        return _type;
    }

    /** Set the protocol to use to access the catalog. For example: "http", "file". */
    public void setProtocol(String protocol) {
        _protocol = protocol;
    }

    /** Set the host name where the catalog server lives */
    public void setHost(String host) {
        _host = host;
    }

    /** Set the host name where the catalog server lives */
    public void setPort(int port) {
        _port = port;
    }


    /** Set the path name to the catalog server */
    public void setURLPath(String path) {
        _path = path;
    }

    /** Return the path name to the catalog server */
    public String getURLPath() {
        return _path;
    }


    /** Set the array of catalog table plot symbol definitions for use with this catalog */
    public void setSymbols(TablePlotSymbol[] symbols) {
        _symbols = symbols;
    }

    /** Set to true if the user edited the plot symbol definitions (default: false) */
    public void setSymbolsEdited(boolean edited) {
	_symbolsEdited = edited;
    }

    /** Return true if the user edited the plot symbol definitions otherwise false */
    public boolean isSymbolsEdited() {
	return _symbolsEdited;
    }

    /** Save the catalog symbol information to disk with the user's changes */
    public void saveSymbolConfig() {
	IRSAConfig.getConfigFile().save();
    }


    // -- The methods below implement the Catalog interface --

    /** Return a string to display as a title for the catalog in a user interface */
    public String getTitle() {
        return _name;
    }

    /** If this catalog can be querried, return the number of query parameters that it accepts */
    public int getNumParams() {
        if (_paramDesc == null) 
	    _initSearchParameters();
        return _paramDesc.length;
    }

    /** Return a description of the ith query parameter */
    public FieldDesc getParamDesc(int i) {
        if (_paramDesc == null) 
	    _initSearchParameters();
        return _paramDesc[i];
    }

    /** Return a description of the named query parameter, if found, otherwise null. */
    public FieldDesc getParamDesc(String name) {
        if (_paramDesc == null) 
	    _initSearchParameters();
	for(int i = 0; i < _paramDesc.length; i++)
	    if (_paramDesc[i] != null && _paramDesc[i].getName().equals(name))
		return _paramDesc[i];
	return null;
    }


    /** Return the number of plot symbol definitions associated with this catalog. */
    public int getNumSymbols() {
        if (_symbols == null)
            _initPlotSymbols();
        if (_symbols == null)
	    return 0;
        return _symbols.length;
    }

    /** Return the ith plot symbol description */
    public TablePlotSymbol getSymbolDesc(int i) {
        return _symbols[i];
    }

    /** Return the array of symbol descriptions */
    public TablePlotSymbol[] getSymbols() {
        if (_symbols == null)
            _initPlotSymbols();
        return _symbols;
    }


    /**
     * Given a description of a region of the sky (center point and radius range),
     * and the current query argument settings, set the values of the corresponding
     * query parameters.
     *
     * @param queryArgs (in/out) describes the query arguments
     * @param region (in) describes the query region (center and radius range)
     */
    public void setRegionArgs(QueryArgs queryArgs, CoordinateRadius region) {
        Coordinates coords = region.getCenterPosition();
        String equinoxStr = (String) queryArgs.getParamValue(EQUINOX);
        double equinox = _getEquinox(queryArgs);
	WorldCoords pos = (WorldCoords)coords;
	String[] radec = pos.format(equinox);
	queryArgs.setParamValue(RA, radec[0]);
	queryArgs.setParamValue(DEC, radec[1]);
	queryArgs.setParamValue(EQUINOX, equinoxStr);
	queryArgs.setParamValue(RADIUS, region.getMaxRadius());
    }

    /**
     * Return true if this is a local catalog, and false if it requires
     * network access or if a query could hang. A local catalog query is
     * run in the event dispatching thread, while others are done in a
     * separate thread.
     */
    public boolean isLocal() {
        return _type.equals(LOCAL);
    }

    /**
     * Return true if this object represents an image server.
     */
    public boolean isImageServer() {
        return false;
    }


    /**
     * Query the catalog using the given arguments and return the result.
     * The result of a query may be any class that implements the QueryResult
     * interface. It is up to the calling class to interpret and display the
     * result. In the general case where the result is downloaded via HTTP,
     * The URLQueryResult class may be used.
     *
     * @param queryArgs An object describing the query arguments.
     * @return An object describing the result of the query.
     */
    public QueryResult query(QueryArgs queryArgs) throws IOException {
        if (_type.equals(CATALOG)) 
            return _queryCatalog(queryArgs);

        if (_type.equals(LOCAL)) 
            return _queryLocalCatalog(queryArgs);
        

        // XXX other catalog types...
        throw new RuntimeException("Query not supported for this catalog type: " + _protocol + ":" + _type);
    }


    // Query an HTTP based catalog server and return the result
    private QueryResult _queryCatalog(QueryArgs queryArgs) throws IOException {
        URL queryUrl = _getQueryUrl(queryArgs);

        if (Logger.isDebugEnabled(this)) {
            Logger.debug(this, "URL = " + queryUrl);
        }

	_updateProgressPanel("Downloading query results ...");
        ProgressBarFilterInputStream in = null;
        try {
            URLConnection connection = _progressPanel.openConnection(queryUrl);
            String contentType = connection.getContentType();
            if (contentType != null && contentType.equals("text/html")) {
                // might be an HTML error from the catalog server
                return new URLQueryResult(queryUrl);
            }
            InputStream ins = connection.getInputStream();
            in = _progressPanel.getLoggedInputStream(ins, connection.getContentLength());
            return _makeQueryResult(in, queryArgs);
        }
        finally {
            if (in != null)
                _progressPanel.stopLoggingInputStream(in);
            _progressPanel.stop();
        }
    }


    /**
     * Query the local catalog file using the given argument and return the result.
     *
     * @param queryArgs An object describing the query arguments.
     * @return An object describing the result of the query.
     */
    private QueryResult _queryLocalCatalog(QueryArgs queryArgs) throws IOException {
        // determine the query region and max rows settings
        SearchCondition[] sc = queryArgs.getConditions();
        _setQueryRegion(queryArgs, sc);
        _setMaxRows(queryArgs, sc);

        // The conditions were handled above, so remove them in query args
        queryArgs.setParamValues(null);

        // If this is a local catalog file and the table has been loaded already, use it
        // (The URL is really just a file path name in this case)
        IRSATable cat = _table;
        if (cat == null) {
            if (_path != null) {
                cat = new IRSATable(this, _path);
            }
            else {
                return null;
            }
        }

        // do the query
        QueryResult result = cat.query(queryArgs);

        // set a reference to this catalog in the resulting table
        if (result instanceof IRSATable) {
            ((IRSATable)result).setCatalog(this);
        }

        return result;
    }


    // Return the equinox setting from the given query arguments object, or default to 2000.
    private double _getEquinox(QueryArgs queryArgs) {
	String equinoxStr = (String)queryArgs.getParamValue(EQUINOX);
	if (equinoxStr.equals(B1950)) 
	    return 1950.;
	return 2000.;
    }

    // Determine the query region based on the given query arguments
    private void _setQueryRegion(QueryArgs queryArgs, SearchCondition[] sc) throws IOException {
        if (queryArgs.getRegion() != null || sc == null || sc.length == 0)
            return;

        // look for a min and max radius parameters
        Double radius = (Double)queryArgs.getParamValue(RADIUS);
        if (radius == null) 
	    radius = new Double(15.);  // XXX default to 15 arcmin ?

	WorldCoords wcs;
	String objectName = (String)queryArgs.getParamValue(OBJECT);
	if (objectName == null || objectName.length() == 0) {
	    // no object name specified, check RA and Dec
	    String raStr = (String) queryArgs.getParamValue(RA);
	    String decStr = (String) queryArgs.getParamValue(DEC);
	    if (raStr == null || decStr == null)
		return;
	    double equinox = _getEquinox(queryArgs);
	    wcs = new WorldCoords(raStr, decStr, equinox, true);
	}
	else {
	    // an object name was specified, which needs to be resolved with a nameserver
	    Object o = queryArgs.getParamValue(NAME_SERVER);
	    if (!(o instanceof Catalog))
		throw new RuntimeException("No name server was specified");
	    wcs = _resolveObjectName(objectName, (Catalog)o);
	}
	queryArgs.setRegion(new CoordinateRadius(wcs, 0., radius.doubleValue()));
    }


    /**
     * Resolve the given astronomical object name using the given name server
     * and return the world coordinates corresponding the name.
     */
    private WorldCoords _resolveObjectName(String objectName, Catalog cat) throws IOException {
        QueryArgs queryArgs = new BasicQueryArgs(cat);
        queryArgs.setId(objectName);
        QueryResult r = cat.query(queryArgs);
        if (r instanceof TableQueryResult) {
            Coordinates coords = ((TableQueryResult) r).getCoordinates(0);
            if (coords instanceof WorldCoords)
                return (WorldCoords) coords;
        }
        throw new RuntimeException("Unexpected result from " + cat.toString());
    }

    /** Check for a "Max Objects" argument and if found, set queryArgs.maxRows with the value. */
    protected void _setMaxRows(QueryArgs queryArgs, SearchCondition[] sc) {
        if (queryArgs.getMaxRows() != 0 || sc == null || sc.length == 0)
            return;

        // look for a min and max radius parameters
        Integer maxObjects = (Integer)queryArgs.getParamValue(MAX_OBJECTS);
        if (maxObjects != null)
            queryArgs.setMaxRows(maxObjects.intValue());
    }


    // Return the URL to use to query the catalog with the current parameter values
    private URL _getQueryUrl(QueryArgs queryArgs) throws MalformedURLException, IOException {
        // determine the query region and max rows settings
        SearchCondition[] sc = queryArgs.getConditions();
        _setQueryRegion(queryArgs, sc);
        _setMaxRows(queryArgs, sc);
	CoordinateRadius region = queryArgs.getRegion();
	String centerStr = "";
	String radiusStr = "";
	if (region != null) {
	    WorldCoords pos = (WorldCoords)region.getCenterPosition();
	    centerStr = pos.getRA().toString() + " " + pos.getDec().toString() + " eq J2000";
	    radiusStr = "" + region.getMaxRadius() + "%20arcmin";
	}

	String urlStr = _protocol + "://" + _host;
	if (_port != 80)
	    urlStr += ":" + _port;
	urlStr += _path;
	urlStr += "?server=" + _server;
	urlStr += "&database=" + _database;
	urlStr += "&catalog=" + _id;
	urlStr += "&sql=" + ((IRSAQueryArgs)queryArgs).getSQLString();
	urlStr += "&within=" + radiusStr;
	urlStr += "&objstr=" + centerStr;

	urlStr = StringUtil.replace(urlStr, " ", "%20");

	return new URL(urlStr);
    }


    // Read the given input stream and return a query result for it
    private QueryResult _makeQueryResult(InputStream ins, QueryArgs queryArgs) throws IOException {
	IRSATable result = new IRSATable(this, ins, queryArgs);
	return result;
    }

    

    // Initialize the catalog parameters. These are not the same as the
    // parameters sent to the server. These are dummy parameters used to
    // keep the basic interface similar to the existing skycat catalog GUI.
    // The code that makes the URL to send to the server then translates
    // the user's input into the correct parameters.
    private void _initSearchParameters() {
	Vector params = new Vector(10);
	FieldDescAdapter p;

	p = new FieldDescAdapter(OBJECT);
	p.setDescription("Enter the name of the object");
	params.add(p);

	p = new FieldDescAdapter(NAME_SERVER);
	p.setDescription("Select the name server to use to resolve the object name");
	List l = IRSAConfig.getConfigFile().getNameServers();
	NameValue[] ar = new NameValue[l.size()];
	for(int i = 0; i < ar.length; i++) {
	    Catalog cat = (Catalog)l.get(i);
	    ar[i] = new NameValue(cat.getName(), cat);
	}
	p.setOptions(ar);
	params.add(p);

	p = new FieldDescAdapter(RA);
	p.setIsRA(true);
	p.setDescription("Right Ascension in the selected equinox, format: hh:mm:ss.sss");
	params.add(p);

	p = new FieldDescAdapter(DEC);
	p.setDescription("Declination in the selected equinox, format: dd:mm:ss.sss");
	p.setIsDec(true);
	params.add(p);

	p = new FieldDescAdapter(EQUINOX);
	p.setDescription("Equinox of RA and Dec");
	p.setOptions(_equinoxOptions);
	params.add(p);

	p = new FieldDescAdapter(RADIUS);
	p.setDescription("The maximum radius from the center coordinates in arcmin");
	p.setFieldClass(Double.class);
	p.setUnits("arcmin");
	params.add(p);

	p = new FieldDescAdapter(SELECT);
	p.setDescription("SQL SELECT statement to be run on the server");
	ar = new NameValue[] {
	    new NameValue("Mini Column List", MINI_COLUMN_LIST),
	    new NameValue("Short Column List", SHORT_COLUMN_LIST),
	    new NameValue("Standard Column List", STANDARD_COLUMN_LIST),
	    new NameValue("All Columns", ALL_COLUMNS)
	    //new NameValue("Custom SQL SELECT statement...", CUSTOM_SQL)
	};
	p.setOptions(ar);
	params.add(p);

	p = new FieldDescAdapter(MAX_OBJECTS);
	p.setDescription("The maximum number of objects to return");
	p.setFieldClass(Integer.class);
	p.setDefaultValue(new Integer(1000));
	params.add(p);

	_paramDesc = new FieldDescAdapter[params.size()];
	params.toArray(_paramDesc);
    }

    
    // Set the string displayed in the progress panel
    private void _updateProgressPanel(String msg) {
        if (_progressPanel == null) 
            _progressPanel = ProgressPanel.makeProgressPanel(msg);
	else {
            _progressPanel.setTitle(msg);
	}
    }


    /** 
     * Download the XML file describing the catalog's table columns and return an
     * array of field descriptions corresponding to the columns.
     * This method should be called in a background thread to avoid hanging.
     */
    public IRSAFieldDesc[] getFieldDesc() throws MalformedURLException, IOException {
	if (_fieldDesc == null) {
	    URL url = _getDataDictionaryURL();
	    _updateProgressPanel("Downloading the data dictionary ...");
	    
	    ProgressBarFilterInputStream in = null;
	    try {
		URLConnection connection = _progressPanel.openConnection(url);
		String contentType = connection.getContentType();
		if (!contentType.equals("text/xml")) 
		    throw new RuntimeException("Error reading the IRSA data dictionary from: " + url);

		InputStream ins = connection.getInputStream();
		in = _progressPanel.getLoggedInputStream(ins, connection.getContentLength());
		IRSADataDictXML dd = new IRSADataDictXML();
		dd.parse(url, in);
		_fieldDesc = dd.getColumns();
	    }
	    finally {
		if (in != null)
		    _progressPanel.stopLoggingInputStream(in);
		_progressPanel.stop();
	    }
	}
	
	
	return _fieldDesc;
    }


    // Return the URL for the catalog's data dictionary on the IRSA web site
    private URL _getDataDictionaryURL() throws MalformedURLException {
	String urlStr = _protocol + "://" + _host;
	if (_port != 80)
	    urlStr += ":" + _port;
	urlStr += _ddPath;
	urlStr += "?server=" + _server;
	urlStr += "&database=" + _database;
	urlStr += "&datadict=" + _ddname;

	return new URL(urlStr);
    }




    /** Implement the {@link CatalogUIHandler} interface to get a custom GUI */
    public JComponent makeComponent(QueryResultDisplay display) {
	if (_uiComponent == null) {
	    try {
		_uiComponent = new IRSACatalogQueryTool(this, display);
	    }
	    catch(Exception e) {
		throw new RuntimeException(e);
	    }
	}
	return _uiComponent;
    }


    // Initialize default plot symbol definitions for this catalog
    private void _initPlotSymbols() {
	if (_fieldDesc == null) {
	    // should be defined at this point. If not, wait until later
	    return;
	}

	// determine the ra and dec columns
	int raCol = -1, decCol = -1;
	for(int i = 0; i < _fieldDesc.length; i++) {
	    if (_fieldDesc[i].isRA())
		raCol = i;
	    else if (_fieldDesc[i].isDec())
		decCol = i;
	}
	if (raCol == -1 || decCol == -1) {
	    return;
	}

	// define a single symbol that just marks the ra,dec position
	_symbols = new TablePlotSymbol[1];

	TablePlotSymbol symbol = new TablePlotSymbol();
	symbol.setRaCol(raCol);
	symbol.setDecCol(decCol);

	_symbols[0] = symbol;
    }



    /**
     * Test cases
     */
    public static void main(String[] args) {
        String catalogName = "2MASS Second Incremental Release Point Source Catalog (PSC)";
        IRSAConfig configFile = IRSAConfig.getConfigFile();
        IRSACatalog cat = (IRSACatalog)configFile.getCatalog(catalogName);
        if (cat == null) {
            System.out.println("Can't find entry for catalog: " + catalogName);
            System.exit(1);
        }

        try {
            System.out.println("test query: at center position/radius: ");
	    IRSAQueryArgs queryArgs = new IRSAQueryArgs(cat);
	    queryArgs.setRegion(new CoordinateRadius(new WorldCoords("03:19:44.44", "+41:30:58.21"), 2.));
	    queryArgs.setSQLString("select ra, dec from " + cat.getId() + ";");
	    IRSATable queryResult = (IRSATable)cat.query(queryArgs);
	    System.out.println("result: " + queryResult);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

	System.out.println("Test passed");
	System.exit(0);
    }
}

