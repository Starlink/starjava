// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: AstroCatalog.java,v 1.2 2002/08/04 22:10:08 brighton Exp $

package jsky.catalog.astrocat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Vector;

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
import jsky.coords.CoordinateRadius;
import jsky.coords.Coordinates;
import jsky.coords.WorldCoords;
import jsky.util.Logger;
import jsky.util.NameValue;
import jsky.util.Resources;
import jsky.util.StringUtil;
import jsky.util.gui.ProgressBarFilterInputStream;
import jsky.util.gui.ProgressPanel;
import jsky.coords.HMS;
import jsky.coords.DMS;


/**
 * Represents a catalog server, as described in an AstroCat XML catalog description file.
 * This class is responsible for generating the catalog query.
 *
 * @version $Revision: 1.2 $
 * @author Allan Brighton
 */
public class AstroCatalog implements PlotableCatalog {

    // constants for search parameter names
    static final String OBJECT = "Object";
    static final String NAME_SERVER = "Name Server";
    static final String RA = "RA";
    static final String DEC = "Dec";
    static final String EQUINOX = "Equinox";
    static final String RADIUS = "Radius";
    static final String MIN_RADIUS = "Min Radius";
    static final String MAX_RADIUS = "Max Radius";
    static final String SIZE = "Size";
    static final String WIDTH = "Width";
    static final String HEIGHT = "Height";
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

    /* Optional link to the parent config file object, or null for the root config file */
    private CatalogDirectory _parent;

    // These correspond to the attributes for the <catalog> element in the AstroCat DTD
    private String _id;
    private String _name;
    private String _description;
    private String _docURL;
    private String _type = Catalog.CATALOG;
    private String _protocol;
    private String _host;
    private int    _port = 80;
    private String _path;
    private String _handlerClass;

    // Array of query parameters for this catalog
    private FieldDesc[] _paramDesc;

    // Fake parameter descriptions, designed to make the user interface uniform for
    // all catalogs that support search by position and radius
    private FieldDesc[] _dummyParamDesc;

    // Optional array of catalog table plot symbol definitions for use with this catalog
    private TablePlotSymbol[] _symbols;

    // Set to true if the user edited the plot symbol definitions
    private boolean _symbolsEdited = false;

    // Optional handler, used to report HTML format errors from servers
    private HTMLQueryResultHandler _htmlQueryResultHandler;

    // Panel used to display download progress information
    private ProgressPanel _progressPanel;

    // true if this object represents a catalog (query returns a table)
    private boolean _isCatalog = false;

    // true if this object represents an image server (query returns an image)
    private boolean _isImageServer = false;

    /** Default constructor */
    public AstroCatalog() {
    }


    /** Implementation of the clone method (makes a shallow copy). */
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
        _isCatalog = _type.equals(CATALOG) || _type.equals(ARCHIVE) || _type.equals(LOCAL);
        _isImageServer = _type.equals(IMAGE_SERVER);
    }

    /** Return the catalog type (one of the constants: CATALOG, ARCHIVE, DIRECTORY, LOCAL, IMAGE_SERVER) */
    public String getType() {
        return _type;
    }


    /** Set the protocol to use to access the catalog. For example: "http", "file". */
    public void setProtocol(String protocol) {
        _protocol = protocol;
    }

    /** Return the protocol to use to access the catalog. For example: "http", "file". */
    public String getProtocol() {
        return _protocol;
    }

    /** Set the host name where the catalog server lives */
    public void setHost(String host) {
        _host = host;
    }

    /** Return the host name where the catalog server lives */
    public String getHost() {
        return _host;
    }

    /** Set the host name where the catalog server lives */
    public void setPort(int port) {
        _port = port;
    }

    /** Return the host name where the catalog server lives */
    public int getPort() {
        return _port;
    }


    /** Set the path name to the catalog server */
    public void setURLPath(String path) {
        _path = path;
    }

    /** Return the path name to the catalog server */
    public String getURLPath() {
        return _path;
    }


    /** Set the array of query parameters for this catalog */
    public void setParams(FieldDesc[] params) {
        _paramDesc = params;
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
	AstroCatConfig.getConfigFile().save();
    }

    /**
     * May be set to the name of a class implementing the QueryResultHandler interface.
     * An instance of this class is then created to handle the result from the catalog server.
     */
    public void setHandlerClass(String handlerClass) {
        _handlerClass = handlerClass;
    }


    /**
     * Return the name of a class implementing the QueryResultHandler interface.
     * An instance of this class is then created to handle the result from the catalog server.
     */
    public String getHandlerClass() {
	return _handlerClass;
    }



    // -- The methods below implement the Catalog interface --

    /** Return a string to display as a title for the catalog in a user interface */
    public String getTitle() {
        return _name;
    }

    /** If this catalog can be querried, return the number of query parameters that it accepts */
    public int getNumParams() {
        if (_dummyParamDesc == null) 
	    _initSearchParameters();
        return _dummyParamDesc.length;
    }

    /** Return a description of the ith query parameter */
    public FieldDesc getParamDesc(int i) {
        if (_dummyParamDesc == null) 
	    _initSearchParameters();
        return _dummyParamDesc[i];
    }

    /** Return a description of the named query parameter, if found, otherwise null. */
    public FieldDesc getParamDesc(String name) {
        if (_dummyParamDesc == null) 
	    _initSearchParameters();
	for(int i = 0; i < _dummyParamDesc.length; i++)
	    if (_dummyParamDesc[i] != null && _dummyParamDesc[i].getName().equals(name))
		return _dummyParamDesc[i];
	return null;
    }


    /** Return the number of plot symbol definitions associated with this catalog. */
    public int getNumSymbols() {
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
	WorldCoords pos = (WorldCoords) coords;
	String[] radec = pos.format(equinox);
	queryArgs.setParamValue(RA, radec[0]);
	queryArgs.setParamValue(DEC, radec[1]);
	queryArgs.setParamValue(EQUINOX, equinoxStr);
	queryArgs.setParamValue(RADIUS, region.getMaxRadius());
	queryArgs.setParamValue(MIN_RADIUS, region.getMinRadius());
	queryArgs.setParamValue(MAX_RADIUS, region.getMaxRadius());
	queryArgs.setParamValue(SIZE, region.getWidth());
	queryArgs.setParamValue(WIDTH, region.getWidth());
	queryArgs.setParamValue(HEIGHT, region.getHeight());
    }

    /**
     * Return true if this is a local catalog, and false if it requires
     * network access or if a query could hang. A local catalog query is
     * run in the event dispatching thread, while others are done in a
     * separate thread.
     */
    public boolean isLocal() {
        return ((_type != null && _type.equalsIgnoreCase(LOCAL))
		|| _protocol != null && _protocol.equals("file"));
		
    }

    /**
     * Return true if this object represents an image server.
     */
    public boolean isImageServer() {
        return (_type != null && _type.equalsIgnoreCase(IMAGE_SERVER));
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
        if (_dummyParamDesc == null) 
	    _initSearchParameters();

        if (_protocol == null || _protocol.equalsIgnoreCase("http")) {
            return _httpQuery(queryArgs);
        }

        // XXX other catalog types...
        throw new RuntimeException("Query not supported for this catalog type: " + _protocol + ":" + _type);
    }



    // -- private methods --

    // Determine the query region based on the given query arguments
    private void _setQueryRegion(QueryArgs queryArgs, SearchCondition[] sc) throws IOException {
        if (queryArgs.getRegion() != null || sc == null || sc.length == 0)
            return;

	// get the center position
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

        // look for a min and max radius, or width and height parameters
	// See corresponding code in _initSearchParameters().
	if (_isCatalog) {
	    if (_findParamByType(_paramDesc, "radius") != null) {
		Double radius = (Double)queryArgs.getParamValue(RADIUS);
		double r;
		if (radius == null) 
		    r = 15.; 
		else
		    r = radius.doubleValue();
		queryArgs.setRegion(new CoordinateRadius(wcs, r));
	    }
	    else {
		double r1, r2;
		Double minRadius = (Double)queryArgs.getParamValue(MIN_RADIUS);
		if (minRadius == null) 
		    r1 = 0.;
		else
		    r1 = minRadius.doubleValue();
		Double maxRadius = (Double)queryArgs.getParamValue(MAX_RADIUS);
		if (maxRadius == null) 
		    r2 = 15.;
		else
		    r2 = maxRadius.doubleValue();
		queryArgs.setRegion(new CoordinateRadius(wcs, r1, r2));
	    }
	}
	else if (_isImageServer) {
	    if (_findParamByType(_paramDesc, "size") != null) {
		Double size = (Double)queryArgs.getParamValue(SIZE);
		double sz;
		if (size == null) 
		    sz = 15.; 
		else
		    sz = size.doubleValue();
		double radius = Math.sqrt(2. * sz * sz) / 2.;
		queryArgs.setRegion(new CoordinateRadius(wcs, radius, sz, sz));
	    }
	    else {
		double w, h;
		Double width = (Double)queryArgs.getParamValue(WIDTH);
		if (width == null) 
		    w = 0.;
		else
		    w = width.doubleValue();
		Double height = (Double)queryArgs.getParamValue(HEIGHT);
		if (height == null) 
		    h = 15.;
		else
		    h = height.doubleValue();
		double radius = Math.sqrt(w*w + h*h) / 2.;
		queryArgs.setRegion(new CoordinateRadius(wcs, radius, w, h));
	    }
	}
    }


    // Return the equinox setting from the given query arguments object, or default to 2000.
    private double _getEquinox(QueryArgs queryArgs) {
	String equinoxStr = (String)queryArgs.getParamValue(EQUINOX);
	if (equinoxStr.equals(B1950)) 
	    return 1950.;
	return 2000.;
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


    // Query an HTTP based catalog server and return the result
    private QueryResult _httpQuery(QueryArgs queryArgs) throws IOException {
        URL queryUrl = _getQueryUrl(queryArgs);

        if (Logger.isDebugEnabled(this)) {
            Logger.debug(this, "URL = " + queryUrl);
        }

	if (_type.equals(IMAGE_SERVER))
	    return new URLQueryResult(queryUrl);

        if (_progressPanel == null) {
            _progressPanel = ProgressPanel.makeProgressPanel("Downloading query results ...");
        }
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


    // Return the base URL string for this catalog
    private String _getBaseUrl() {
	if (_host == null || _protocol == null)
	    return _path;

	String urlStr = _protocol + "://" + _host;
	if (_port != 80)
	    urlStr += ":" + _port;
	if (_path != null)
	    urlStr += _path;
	return urlStr;
    }


    // Return the URL to use to query the catalog with the current parameter values
    private URL _getQueryUrl(QueryArgs queryArgs) throws MalformedURLException, IOException {
        // determine the query region and max rows settings
	String urlStr = _getBaseUrl();
	SearchCondition[] sc = queryArgs.getConditions();
	
	if (! _centerPosRadiusSearchSupported()) {
	    // no search by pos/radius: just use the parameters defined in the XML file
	    if (sc != null && sc.length != 0) {
		for (int i = 0; i < sc.length; i++) {
		    String sep = (i == 0) ? "?" : "&";
		    urlStr += sep + sc[i].getId() + "=" + sc[i].getVal();
		}
	    }
	}
	else {
	    // fill in values from the dummy parameters
	    _setQueryRegion(queryArgs, sc);
	    _setMaxRows(queryArgs, sc);
	    if (_paramDesc != null && _paramDesc.length != 0) {
		for (int i = 0; i < _paramDesc.length; i++) {
		    String value = _getParamValue(_paramDesc[i], queryArgs);
		    if (value != null && value.length() != 0) {
			String sep = (i == 0) ? "?" : "&";
			urlStr += sep + _paramDesc[i].getId() + "=" + value;
		    }
		}
	    }
	}

	urlStr = StringUtil.replace(urlStr, " ", "%20");
	return new URL(urlStr);
    }

    
    // Return the value for the given parameter based on the dummy parameter values
    private String _getParamValue(FieldDesc param, QueryArgs queryArgs) {
	// check if it is not one of the position/radius related parameters
	for(int i = 0; i < _dummyParamDesc.length; i++) {
	    if (_dummyParamDesc[i] == param) {
		Object o = queryArgs.getParamValue(i);
		if (o != null)
		    return o.toString();
		return null;
	    }
	}
	
	// Must be one of the position/radius related parameters
	CoordinateRadius region = queryArgs.getRegion();
	if (region == null) 
	    return null;
	WorldCoords pos = (WorldCoords)region.getCenterPosition();
		
	String type = param.getType();
	String units = param.getUnits();
	String format = param.getFormat();

	if (type.equalsIgnoreCase("radec")) {
	    return _getRADec(pos.getRA(), pos.getDec(), format);
	}
	if (type.equalsIgnoreCase("ra")) {
	    return _getRA(pos.getRA(), format);
	}
	if (type.equalsIgnoreCase("dec")) {
	    return _getDec(pos.getDec(), format);
	}
	if (type.equalsIgnoreCase("radius")) {
	    return String.valueOf(_getValueInUnits(region.getMaxRadius(), "arcmin", units));
	}
	if (type.equalsIgnoreCase("minradius")) {
	    return String.valueOf(_getValueInUnits(region.getMinRadius(), "arcmin", units));
	}
	if (type.equalsIgnoreCase("maxradius")) {
	    return String.valueOf(_getValueInUnits(region.getMaxRadius(), "arcmin", units));
	}
	if (type.equalsIgnoreCase("width") || type.equalsIgnoreCase("size")) {
	    return String.valueOf(_getValueInUnits(region.getWidth(), "arcmin", units));
	}
	if (type.equalsIgnoreCase("height")) {
	    return String.valueOf(_getValueInUnits(region.getHeight(), "arcmin", units));
	}

	return null;
    }


    // Return RA in the given format
    private String _getRA(HMS ra, String format) {
	String raStr = ra.toString();
	if (format == null || format.length() == 0)
	    return raStr;

	if (format.startsWith("h+m+s")) 
	    return StringUtil.replace(raStr, ":", "+");
	if (format.startsWith("h m s")) 
	    return StringUtil.replace(raStr, ":", " ");

	return raStr;
    }
	    

    // Return Dec in the given format
    private String _getDec(DMS dec, String format) {
	String decStr = dec.toString();
	if (format == null || format.length() == 0)
	    return decStr;

	if (format.endsWith("d+m+s")) 
	    return StringUtil.replace(decStr, ":", "+");
	if (format.endsWith("d m s")) 
	    return StringUtil.replace(decStr, ":", " ");

	return decStr;
    }
	    
    // Return an RA Dec string in the given format
    private String _getRADec(HMS ra, DMS dec, String format) {
	String raDecStr = _getRA(ra, format) + " " + _getDec(dec, format);
	if (format == null || format.length() == 0)
	    return raDecStr;

	if (format.equals("h+m+s d+m+s")) 
	    return StringUtil.replace(raDecStr, " ", "%2b");

	return raDecStr;
    }
	    
    
    // Convert the given value from inUnits to outUnits and return the result
    private double _getValueInUnits(double value, String inUnits, String outUnits) {
	if (inUnits == null || inUnits.length() == 0 || outUnits == null || outUnits.length() == 0)
	    return value;
	
	if (inUnits.startsWith("arcmin")) {
	    if (outUnits.startsWith("arcsec")) 
		return value*60.;
	    if (outUnits.startsWith("arcmin")) 
		return value;
	    if (outUnits.startsWith("deg")) 
		return value/4;
	    if (outUnits.startsWith("hour")) 
		return value/60.;
	}
	else if (inUnits.startsWith("arcsec")) {
	    if (outUnits.startsWith("arcsec")) 
		return value;
	    if (outUnits.startsWith("arcmin")) 
		return value/60.;
	    if (outUnits.startsWith("deg")) 
		return value/240.;
	    if (outUnits.startsWith("hour")) 
		return value/3600.;
	}
	else if (inUnits.startsWith("deg")) {
	    if (outUnits.startsWith("arcsec")) 
		return value*240.;
	    if (outUnits.startsWith("arcmin")) 
		return value*4;
	    if (outUnits.startsWith("deg")) 
		return value;
	    if (outUnits.startsWith("hour")) 
		return value/15.;
	}
	else if (inUnits.startsWith("hours")) {
	    if (outUnits.startsWith("arcsec")) 
		return value*3600.;
	    if (outUnits.startsWith("arcmin")) 
		return value*60.;
	    if (outUnits.startsWith("deg")) 
		return value*15.;
	    if (outUnits.startsWith("hour")) 
		return value;
	}
	else {
	    System.out.println("Warning: unrecognized units: '" + inUnits + "'");
	}
	System.out.println("Warning: unrecognized units: '" + outUnits + "'");

	return value;
    }

    
    // Return true if this catalog supports search by center position and radius
    private boolean _centerPosRadiusSearchSupported() {
	if (_isCatalog || _isImageServer) {
	    boolean hasPos = false, hasSizeOrRadius = false;
	    for(int i = 0; i < _paramDesc.length; i++) {
		String type = _paramDesc[i].getType();
		if (type != null && type.length() != 0) {
		    if (type.equalsIgnoreCase("radec") || type.equalsIgnoreCase("ra"))
			hasPos = true;
		    else if (type.equalsIgnoreCase("radius") 
			     || type.equalsIgnoreCase("minradius") 
			     || type.equalsIgnoreCase("maxradius") 
			     || type.equalsIgnoreCase("size")
			     || type.equalsIgnoreCase("width")
			     || type.equalsIgnoreCase("height"))
			hasSizeOrRadius = true;
		    if (hasPos && hasSizeOrRadius)
			return true;
		}
	    }
	}
	return false;
    }


    // Read the given input stream and return a query result for it, using the
    // handler class, if provided, otherwise the default is to look for a catalog
    // table in skycat (tab separated table) format.
    private QueryResult _makeQueryResult(InputStream ins, QueryArgs queryArgs) throws IOException {
        //XXX if (_handlerClass == null || _handlerClass.equals(AstroCatTable.class.getName())) {
	AstroCatTable result = new AstroCatTable(this, ins, queryArgs);
	return result;
	//}

        // XXX call handler class (with input stream!)
	//return null;
    }

    
    // Initialize the catalog parameters. These may not be the same as the
    // parameters sent to the server. For catalogs than can be searched by center 
    // position and radius, dummy parameters are used to keep the basic interface 
    // similar to the existing skycat catalog GUI. The code that makes the URL to 
    // send to the server then translates the user's input into the correct parameters.
    private void _initSearchParameters() {
	_checkForDetailedCatalogDesc();

	if (! _centerPosRadiusSearchSupported()) {
	    // no search by pos/radius: just use the parameters defined in the XML file
	    _dummyParamDesc = _paramDesc;
	    return;
	}

	// initialize dummy parameters for search by pos/radius (for a more uniform user interface)
	Vector params = new Vector(10, 10);

        if (_isCatalog || _isImageServer) {
            // Define the "standard" parameters
	    FieldDescAdapter p = new FieldDescAdapter(OBJECT);
	    p.setDescription("Enter the name of the object");
	    params.add(p);

	    p = new FieldDescAdapter(NAME_SERVER);
	    p.setDescription("Select the name server to use to resolve the object name");
	    List l = AstroCatConfig.getConfigFile().getNameServers();
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

	    if (_isCatalog) {
		if (_findParamByType(_paramDesc, "radius") != null) {
		    p = new FieldDescAdapter(MAX_RADIUS);
		    p.setDescription("The radius from the center coordinates in arcmin");
		    p.setFieldClass(Double.class);
		    p.setUnits("arcmin");
		    params.add(p);
		}
		else {
		    if (_findParamByType(_paramDesc, "minradius") != null) {
			p = new FieldDescAdapter(MIN_RADIUS);
			p.setDescription("The minimum radius from the center coordinates in arcmin");
			p.setFieldClass(Double.class);
			p.setUnits("arcmin");
			params.add(p);
		    }
		    if (_findParamByType(_paramDesc, "maxradius") != null) {
			p = new FieldDescAdapter(MAX_RADIUS);
			p.setDescription("The maximum radius from the center coordinates in arcmin");
			p.setFieldClass(Double.class);
			p.setUnits("arcmin");
			params.add(p);
		    }
		}
	    }
	    else if (_isImageServer) {
		if (_findParamByType(_paramDesc, "size") != null) {
		    p = new FieldDescAdapter(SIZE);
		    p.setDescription("The requested size (width or height) about the center coordinates in arcmin");
		    p.setFieldClass(Double.class);
		    p.setUnits("arcmin");
		    params.add(p);
		}
		else {
		    if (_findParamByType(_paramDesc, "width") != null) {
			p = new FieldDescAdapter(WIDTH);
			p.setDescription("The width about the center coordinates in arcmin");
			p.setFieldClass(Double.class);
			p.setUnits("arcmin");
			params.add(p);
		    }
		    if (_findParamByType(_paramDesc, "height") != null) {
			p = new FieldDescAdapter(HEIGHT);
			p.setDescription("The height about the center coordinates in arcmin");
			p.setFieldClass(Double.class);
			p.setUnits("arcmin");
			params.add(p);
		    }
		}
	    }

	    if (_isCatalog && 
		_findParamByType(_paramDesc, "maxobjects") == null
		&& _findParamByType(_paramDesc, "maxrows") == null) {
		p = new FieldDescAdapter(MAX_OBJECTS);
		p.setDescription("The maximum number of objects to return");
		p.setFieldClass(Integer.class);
		p.setDefaultValue(new Integer(1000));
		params.add(p);
	    }
	}
	
	// Add the rest of the parameters not dealing with position and radius
	for(int i = 0; i < _paramDesc.length; i++) {
	    if (! _isPosRadiusParam(_paramDesc[i]))
		params.add(_paramDesc[i]);
	}

	_dummyParamDesc = new FieldDescAdapter[params.size()];
	params.toArray(_dummyParamDesc);
    }


    // Return true if the given parameter is part of a center position or radius specification
    private boolean _isPosRadiusParam(FieldDesc param) {
	String type = param.getType();
	if (type != null && type.length() != 0) {
	    return type.equals("radec")
		|| type.equalsIgnoreCase("ra")
		|| type.equalsIgnoreCase("dec")
		|| type.equalsIgnoreCase("equinox")
		|| type.equalsIgnoreCase("epoch")
		|| type.equalsIgnoreCase("nameserver")
		|| type.equalsIgnoreCase("radius")
		|| type.equalsIgnoreCase("minradius")
		|| type.equalsIgnoreCase("maxradius")
		|| type.equalsIgnoreCase("size")
		|| type.equalsIgnoreCase("width")
		|| type.equalsIgnoreCase("height");
	}
	return false;
    }


    // Search for a parameter with the given type and return it if found, or null otherwise
    private FieldDesc _findParamByType(FieldDesc[] params, String type) {
	for(int i = 0; i < params.length; i++) {
	    if (type.equalsIgnoreCase(params[i].getType()))
		return params[i];
	}
	return null;
    }



    // The top level AstroCat.xml file contains short entries only describing the catalog
    // names and types. The path field may point to a XML file with a detailed description 
    // of each catalog. This method follows the link and updates the fields in this class
    // from it, if needed.
    private void _checkForDetailedCatalogDesc() {
	if (_paramDesc == null && _path != null && _path.endsWith(".xml") && ! _type.equals(DIRECTORY)) {
	    // should be an AstroCat XML file with the catalog details
	    try {
		String urlStr = _getBaseUrl();
		URL url = new URL(_url, urlStr);
		String filename = url.getFile();
		if (filename != null) {
		    File file = new File(filename);
		    if (! file.exists()) {
			// also search in the default directory
			URL defaultURL = Resources.getResource("conf/" + _path);
			if (defaultURL != null)
			    url = defaultURL;
		    }
		}

		AstroCatXML astroCatXML = new AstroCatXML();
		astroCatXML.parse(url);

		List catalogs = astroCatXML.getCatalogs();
		if (catalogs.size() != 1) 
		    throw new RuntimeException("Expected a single catalog description in: " + url);
		AstroCatalog cat = (AstroCatalog)catalogs.get(0);
		_url = url;
		//_parent = cat._parent; (don't set this, since it would then be null)
		_id = cat._id;
		_name = cat._name;
		_description = cat._description;
		_docURL = cat._docURL;
		_type = cat._type;
		_protocol = cat._protocol;
		_host = cat._host;
		_port = cat._port;
		_path = cat._path;
		_handlerClass = cat._handlerClass;
		_paramDesc = cat._paramDesc;
		if (_symbols == null)
		    _symbols = cat._symbols;
	    }
	    catch(Exception e) {
		throw new RuntimeException(e);
	    }
	}
    }


	/**
     * Test cases
     */
    public static void main(String[] args) {
        String catalogName = "Guide Star Catalog at ESO";
        AstroCatConfig configFile = AstroCatConfig.getConfigFile();
        Catalog cat = configFile.getCatalog(catalogName);
        if (cat == null) {
            System.out.println("Can't find entry for catalog: " + catalogName);
            System.exit(1);
        }

        try {
            System.out.println("test query: at center position/radius: ");
            QueryArgs queryArgs = new BasicQueryArgs(cat);
            queryArgs.setParamValue("center", "03:19:44.44+41:30:58.21");
            queryArgs.setParamValue("radius", "2");
            QueryResult queryResult = cat.query(queryArgs);
            System.out.println("result: " + queryResult);
	    if (queryResult instanceof AstroCatTable) 
		((AstroCatTable)queryResult).saveAs(System.out);
	    else 
		System.out.println("Can't print table");

	    /*
            queryArgs = new BasicQueryArgs(cat);
            queryArgs.setRegion(new CoordinateRadius(new WorldCoords("03:19:44.44", "+41:30:58.21"), 2.));
            queryResult = cat.query(queryargs);
            System.out.println("result: " + queryResult);
	    */

        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

	System.out.println("Test passed");
	System.exit(0);
    }
}

