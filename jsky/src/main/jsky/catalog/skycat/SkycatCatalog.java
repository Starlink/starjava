/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SkycatCatalog.java,v 1.30 2002/08/05 10:57:21 brighton Exp $
 */

package jsky.catalog.skycat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Runtime;
import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;

import jsky.catalog.BasicQueryArgs;
import jsky.catalog.Catalog;
import jsky.catalog.CatalogDirectory;
import jsky.catalog.FieldDesc;
import jsky.catalog.HTMLQueryResultHandler;
import jsky.catalog.MemoryCatalog;
import jsky.catalog.PlotableCatalog;
import jsky.catalog.QueryArgs;
import jsky.catalog.QueryResult;
import jsky.catalog.RowCoordinates;
import jsky.catalog.SearchCondition;
import jsky.catalog.TablePlotSymbol;
import jsky.catalog.TableQueryResult;
import jsky.catalog.URLQueryResult;
import jsky.coords.CoordinateRadius;
import jsky.coords.Coordinates;
import jsky.coords.ImageCoords;
import jsky.coords.WorldCoords;
import jsky.util.Logger;
import jsky.util.gui.ProgressBarFilterInputStream;
import jsky.util.gui.ProgressPanel;


/**
 * Represents a catalog as described in a Skycat style catalog
 * config file. The (keyword: value) pairs in the config file are stored
 * here in a Properties object.
 *
 * @version $Revision: 1.30 $
 * @author Allan Brighton
 */
public class SkycatCatalog implements PlotableCatalog {

    /** The catalog configuration entry for this catalog. */
    private SkycatConfigEntry _entry;

    /** Optional handler, used to report HTML format errors from servers */
    private HTMLQueryResultHandler _htmlQueryResultHandler;

    /** Panel used to display download progress information */
    private ProgressPanel _progressPanel;

    /** If this is a local catalog, this may optionally point to the data */
    private SkycatTable _table;

    /** Used to assign a unique name to query results */
    private int _queryCount = 0;

 
    /**
     * Initialize the catalog from the given catalog configuration entry.
     *
     * @param entry the catalog configuration file entry describing the catalog
     */
    public SkycatCatalog(SkycatConfigEntry entry) {
        _entry = entry;
    }

    /**
     * Initialize the catalog from the given catalog configuration entry.
     *
     * @param entry the catalog configuration file entry describing the catalog
     * @param table the data for the catalog (optional, only for local catalgs)
     */
    public SkycatCatalog(SkycatConfigEntry entry, SkycatTable table) {
        this(entry);
        _table = table;
    }

    /**
     * Initialize the catalog from the given table.
     *
     * @param table the data for the catalog (optional, only for local catalgs)
     */
    public SkycatCatalog(SkycatTable table) {
        this(table.getConfigEntry());
        _table = table;
        _table.setCatalog(this);
    }


    /**
     * Initialize the catalog from the given catalog configuration entry.
     *
     * @param entry the catalog configuration file entry describing the catalog
     * @param handler used to report HTML errors from the HTTP server
     */
    public SkycatCatalog(SkycatConfigEntry entry, HTMLQueryResultHandler handler) {
        this(entry);
        setHTMLQueryResultHandler(handler);
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


    /** Set the name of the catalog  */
    public void setName(String name) {
        _entry.setName(name);
    }

    /** Return the name of the catalog  */
    public String getName() {
        return _entry.getName();
    }

    /** Return the object used to manage the configuration info for this catalog  */
    public SkycatConfigEntry getConfigEntry() {
        return _entry;
    }

    /** Returns the number of querries made so far */
    public int getQueryCount() {
        return _queryCount;
    }

    /** Set the object used to manage the configuration info for this catalog  */
    public void setConfigEntry(SkycatConfigEntry entry) {
        _entry = entry;
    }

    /** Return the table data (for local catalogs) or null, if not known. */
    public SkycatTable getTable() {
        return _table;
    }

    /** Return the name of the catalog */
    public String toString() {
        return getName();
    }

    /** Return the id of the catalog (same as name here) */
    public String getId() {
        return _entry.getShortName();
    }

    /** Return the title of the catalog (same as name here) */
    public String getTitle() {
        return _entry.getName();
    }

    /** Return a description of the catalog (same as name here) */
    public String getDescription() {
        return _entry.getProperty("copyright");
    }

    /** Return a URL pointing to documentation for the catalog, or null if not available */
    public URL getDocURL() {
        return _entry.getDocURL();
    }

    /** If this catalog can be querried, return the number of query parameters that it accepts */
    public int getNumParams() {
        return _entry.getNumParams();
    }

    /** Return a description of the ith query parameter */
    public FieldDesc getParamDesc(int i) {
        return _entry.getParamDesc(i);
    }

    /** Return a description of the named query parameter */
    public FieldDesc getParamDesc(String name) {
        return _entry.getParamDesc(name);
    }


    /** Return the number of plot symbol definitions associated with this catalog. */
    public int getNumSymbols() {
        return _entry.getNumSymbols();
    }

    /** Return the ith plot symbol description */
    public TablePlotSymbol getSymbolDesc(int i) {
        return _entry.getSymbolDesc(i);
    }

    /** Return the array of symbol descriptions */
    public TablePlotSymbol[] getSymbols() {
        return _entry.getSymbols();
    }

    /** Set the array of catalog table plot symbol definitions for use with this catalog */
    public void setSymbols(TablePlotSymbol[] symbols) {
        _entry.setSymbols(symbols);
    }

    /** Set to true if the user edited the plot symbol definitions (default: false) */
    public void setSymbolsEdited(boolean edited) {
        _entry.setSymbolsEdited(edited);
    }

    /** Return true if the user edited the plot symbol definitions otherwise false */
    public boolean isSymbolsEdited() {
        return _entry.isSymbolsEdited();
    }

    /** Save the catalog symbol information to disk with the user's changes */
    public void saveSymbolConfig() {
	SkycatConfigFile.getConfigFile().save();
    }

    /** Return a short name or alias for the catalog  */
    public String getShortName() {
        return _entry.getShortName();
    }

    /** Return true if the catalog has RA and DEC coordinate columns */
    public boolean isWCS() {
        return _entry.getRowCoordinates().isWCS();
    }

    /** Return the value of the "equinox" property, if defined, otherwise 2000. */
    public double getEquinox() {
        return _entry.getRowCoordinates().getEquinox();
    }

    /** Return true if the catalog has X and Y columns (assumed to be image pixel coordinates) */
    public boolean isPix() {
        return _entry.getRowCoordinates().isPix();
    }

    /**
     * Return true if this is a local catalog, and false if it requires
     * network access or if a query could hang. A local catalog query is
     * run in the event dispatching thread, while others are done in a
     * separate thread.
     */
    public boolean isLocal() {
        return _entry.getServType().equals(LOCAL);
    }

    /**
     * Return true if this object represents an image server.
     */
    public boolean isImageServer() {
        return _entry.getServType().equals(IMAGE_SERVER);
    }


    /** Return the catalog type (normally one of the Catalog constants: CATALOG, ARCHIVE, DIRECTORY, LOCAL, IMAGE_SERVER) */
    public String getType() {
        return _entry.getServType();
    }

    /** Set the parent catalog directory */
    public void setParent(CatalogDirectory catDir) {
	_entry.setConfigFile(catDir);
    }

    /** Return a reference to the parent catalog directory, or null if not known. */
    public CatalogDirectory getParent() {
	return _entry.getConfigFile();
    }


    /** 
     * Return an array of Catalog or CatalogDirectory objects representing the 
     * path from the root catalog directory to this catalog.
     */
    public Catalog[] getPath() {
	CatalogDirectory parent = getParent();
	if (parent == null)
	    return null;

	return parent.getPath(this);
    }


    /**
     * Query the catalog using the given argument and return the result.
     *
     * @param queryArgs An object describing the query arguments.
     * @return An object describing the result of the query.
     */
    public QueryResult query(QueryArgs queryArgs) throws IOException {
        _queryCount++;
        String servType = _entry.getServType();
        if (servType.equals(LOCAL))
            return _queryLocalCatalog(queryArgs);

        if (servType.equals(CATALOG) || servType.equals(ARCHIVE) || servType.equals(NAME_SERVER))
            return _queryCatalog(queryArgs);

        if (servType.equals(IMAGE_SERVER))
            return _queryImageServer(queryArgs);

        if (servType.equals(DIRECTORY))
            return _queryCatalogDirectory(queryArgs);

        // XXX other catalog types...
        throw new RuntimeException("Query not supported for this catalog type: " + servType);
    }


    /**
     * Query the local catalog using the given argument and return the result.
     *
     * @param queryArgs An object describing the query arguments.
     * @return An object describing the result of the query.
     */
    private QueryResult _queryLocalCatalog(QueryArgs queryArgs) throws IOException {
        String urlStr = _entry.getURL(0);
        if (urlStr != null && urlStr.startsWith("java://"))
            return _queryJavaCatalog(queryArgs);

        // determine the query region and max rows settings
        SearchCondition[] sc = queryArgs.getConditions();
        _setQueryRegion(queryArgs, sc);
        _setMaxRows(queryArgs, sc);

        // The conditions were handled above, so remove them in query args
        queryArgs.setParamValues(null);

        // If this is a local catalog file and the table has been loaded already, use it
        // (The URL is really just a file path name in this case)
        SkycatTable cat = _table;
        if (cat == null) {
            if (urlStr != null) {
                cat = new SkycatTable(this, urlStr);
            }
            else {
                return null;
            }
        }

        // do the query
        QueryResult result = cat.query(queryArgs);

        // set a reference to this catalog in the resulting table
        if (result instanceof SkycatTable) {
            ((SkycatTable) result).setCatalog(this);
        }

        return result;
    }

    /**
     * Query the catalog using the given argument and return the result.
     *
     * @param queryArgs An object describing the query arguments.
     * @return An object describing the result of the query.
     */
    private QueryResult _queryCatalog(QueryArgs queryArgs) throws IOException {
        int n = _entry.getNumURLs();
        for (int i = 0; i < n; i++) {
            String urlStr = _entry.getURL(i);
            if (urlStr != null) {
                urlStr = _getQueryUrl(urlStr, queryArgs);
                if (urlStr.startsWith(File.separator)
                        || (urlStr.length() > 2 && urlStr.charAt(1) == ':')) { // C:\dir\command ...
                    // may be a local command path name (for security, must be from a local config file)
                    CatalogDirectory catDir = _entry.getConfigFile();
                    if (catDir != null && !catDir.isLocal())
                        throw new RuntimeException("Invalid catalog URL: " + urlStr + ", in remote config file");
                    Process process = Runtime.getRuntime().exec(urlStr);
                    //InputStream stderr = process.getErrorStream();
                    InputStream stdout = process.getInputStream();
                    SkycatTable cat = new SkycatTable(this, stdout, queryArgs);
                    cat.setConfigEntry(_entry);
                    return cat;
                }
                else if (urlStr.startsWith("java://")) {
                    return _queryJavaCatalog(queryArgs);
                }
                else {
                    // normal URL
                    URL queryUrl = new URL(urlStr);
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

                        SkycatTable cat = new SkycatTable(this, in, queryArgs);
                        cat.setConfigEntry(_entry);
                        return cat;
                    }
                    finally {
                        if (in != null)
                            _progressPanel.stopLoggingInputStream(in);
                        _progressPanel.stop();
                    }
                }
            }
        }
        throw new RuntimeException("No query URL was specified in the config file.");
    }

    /**
     * Query the catalog using the given argument and return the result.
     *
     * @param queryArgs An object describing the query arguments.
     * @return An object describing the result of the query.
     */
    private QueryResult _queryImageServer(QueryArgs queryArgs) throws IOException {
        int n = _entry.getNumURLs();
        for (int i = 0; i < n; i++) {
            String urlStr = _entry.getURL(i);
            if (urlStr != null) {
                urlStr = _getQueryUrl(urlStr, queryArgs);
                if (urlStr.startsWith(File.separator)) {
                    // may be a local command path name
                    throw new RuntimeException("Local commands not supported for image server (yet)");
                }
                else {
                    // normal URL
                    return new URLQueryResult(new URL(urlStr));
                }
            }
        }
        throw new RuntimeException("No query URL was specified in the config file.");
    }

    /**
     * Return a query result listing the contents of the catalog directory.
     *
     * @param queryArgs An object describing the query arguments (not used here)
     * @return An object describing the result of the query.
     */
    private QueryResult _queryCatalogDirectory(QueryArgs queryArgs) {
        int numURLs = _entry.getNumURLs();
        for (int i = 0; i < numURLs; i++) {
            try {
                return new URLQueryResult(new URL(_entry.getURL(0)));
            }
            catch (Exception e) {
                if (i == (numURLs - 1))
                    throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("No URL was specified in the config file.");
    }

    /**
     * Return the result of a query to a Java class based catalog.
     * The catalog class name should be specified in the catalog config
     * URL in the format: java://<classname>?<arg1>&<arg2>&...&<argn>.
     *
     * @param queryArgs An object describing the query arguments (not used here)
     * @return An object describing the result of the query.
     */
    private QueryResult _queryJavaCatalog(QueryArgs queryArgs) throws IOException {
        QueryResult result = null;

        // determine the query region and max rows settings
        SearchCondition[] sc = queryArgs.getConditions();
        _setQueryRegion(queryArgs, sc);
        _setMaxRows(queryArgs, sc);

        String urlStr = _entry.getURL(0);
        if (urlStr != null) {
            StringTokenizer token = new StringTokenizer(urlStr.substring(7), "?\t");
            urlStr = _getQueryUrl(urlStr, queryArgs);
            String className = token.nextToken();
            try {
                Class catalogClass = this.getClass().forName(className);
                Catalog catalog = (Catalog) catalogClass.newInstance();
                result = catalog.query(queryArgs);
                if (result instanceof MemoryCatalog && !(result instanceof SkycatTable)) {
                    MemoryCatalog mcat = (MemoryCatalog) result;
                    result = new SkycatTable(_entry, mcat.getDataVector(), mcat.getFields());
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                if (e instanceof IOException)
                    throw (IOException) e;
                throw new RuntimeException(e);
            }
        }

        // set a reference to this catalog in the resulting table
        if (result instanceof SkycatTable) {
            ((SkycatTable) result).setCatalog(this);
        }

        return result;
    }


    /**
     *
     * Given a URL string with variables (%ra, %dec, etc.), substitute the
     * variables and return the new URL string.
     * <p>
     * The following substitutions are then performed on the given URL:
     * <p>
     *  %ra, %dec   - world coordinates of center point (for catalogs based in wcs)
     * <p>
     *  %x, %y      - image coordinates of center point (for pixel based catalogs)
     * <p>
     *  %r1, %r2    - min and max radius (for circular query)
     * <p>
     *  %m1, %m2    - min and max magnitude
     * <p>
     *  %n          - max number of rows to return
     * <p>
     *  %cols       - comma sep. list of columns to return: col1,col2,...coln (not implemented)
     * <p>
     *  %id         - ID field of item to return (if supported)
     * <p>
     *  %mime-type  - value for http mime-type field
     * <p>
     *  %sort       - insert list of sort columns: col1,col2,... (not implemented)
     * <p>
     *  %sortorder  - insert string: increasing or decreasing (not implemented)
     * <p>
     *  %cond       - insert search condition, if any, in the format
     *                col1=minVal,maxVal&col2=minVal,maxVal,...
     *
     * @param url A string containing the raw URL, before substitution.
     * @param queryArgs An object describing the query arguments.
     * @return The substituted, expanded URL to use to make the query.
     */
    private String _getQueryUrl(String urlStr, QueryArgs queryArgs) throws IOException {

        if (_entry.getNumParams() == 0)
            return urlStr;

        int n = urlStr.length();
        StringBuffer buf = new StringBuffer(n * 2);
        boolean urlHasId = false, urlHasRaDec = false, urlHasXy = false;
        SearchCondition[] sc = queryArgs.getConditions();

        // determine the query region and max rows settings
        _setQueryRegion(queryArgs, sc);
        _setMaxRows(queryArgs, sc);

        // expand the variables in the catalog server URL
        for (int c = 0; c < n;) {
            if (urlStr.charAt(c) == '%') {
                c++;
                if (urlStr.charAt(c) == '%') {
                    // make "%%" expand to "%"
                    buf.append('%');
                    c++;
                }
                else if (urlStr.startsWith("id", c)) {
                    // %id
                    String id = queryArgs.getId();
                    if (id == null)
                        id = queryArgs.getParamValueAsString("id", null);
                    if (id != null)
                        buf.append(id);
                    c += 2;
                    urlHasId = true;
                }
                else if (urlStr.startsWith("ra", c)) {
                    CoordinateRadius region = queryArgs.getRegion();
                    if (region != null) {
                        WorldCoords pos = (WorldCoords) region.getCenterPosition();
                        buf.append(pos.getRA().toString());
                    }
                    c += 2;
                    urlHasRaDec = true;
                }
                else if (urlStr.startsWith("dec", c)) {
                    CoordinateRadius region = queryArgs.getRegion();
                    if (region != null) {
                        WorldCoords pos = (WorldCoords) region.getCenterPosition();
                        buf.append(pos.getDec().toString());
                    }
                    c += 3;
                    urlHasRaDec = true;
                }
                else if (urlStr.charAt(c) == 'x') {
                    CoordinateRadius region = queryArgs.getRegion();
                    if (region != null) {
                        ImageCoords pos = (ImageCoords) region.getCenterPosition();
                        buf.append(pos.getX());
                    }
                    c++;
                    urlHasXy = true;
                }
                else if (urlStr.charAt(c) == 'y') {
                    CoordinateRadius region = queryArgs.getRegion();
                    if (region != null) {
                        ImageCoords pos = (ImageCoords) region.getCenterPosition();
                        buf.append(pos.getY());
                    }
                    c++;
                    urlHasXy = true;
                }
                else if (urlStr.startsWith("r1", c)) {
                    CoordinateRadius region = queryArgs.getRegion();
                    if (region != null)
                        if (region.getMinRadius() != 0.0 || region.getMaxRadius() != 0.0)
                            buf.append(region.getMinRadius());
                    c += 2;
                }
                else if (urlStr.startsWith("r2", c)) {
                    CoordinateRadius region = queryArgs.getRegion();
                    if (region != null)
                        if (region.getMinRadius() != 0.0 || region.getMaxRadius() != 0.0)
                            buf.append(region.getMaxRadius());
                    c += 2;
                }
                else if (urlStr.charAt(c) == 'w') {
                    if (sc != null && sc.length > 0) {
                        for (int i = 0; i < sc.length; i++) {
                            if (sc[i].getName().equals(SkycatConfigEntry.WIDTH)) {
                                buf.append(sc[i].getVal());
                                break;
                            }
                        }
                    }
                    c++;
                }
                else if (urlStr.charAt(c) == 'h') {
                    if (sc != null && sc.length > 0) {
                        for (int i = 0; i < sc.length; i++) {
                            if (sc[i].getName().equals(SkycatConfigEntry.HEIGHT)) {
                                buf.append(sc[i].getVal());
                                break;
                            }
                        }
                    }
                    c++;
                }
                else if (urlStr.startsWith("m1", c)) { // min magnitude
                    if (sc != null && sc.length > 0) {
                        String m1 = "0"; // default value
                        for (int i = 0; i < sc.length; i++) {
                            FieldDesc p = sc[i].getFieldDesc();
                            if (p.isMin()) {
                                String id = p.getId();
                                // field name is "mag" by convention
                                if (id != null && id.equalsIgnoreCase("mag")) {
                                    m1 = sc[i].getVal().toString();
                                    break;
                                }
                            }
                        }
                        buf.append(m1);
                    }
                    c += 2;
                }
                else if (urlStr.startsWith("m2", c)) { // max magnitude
                    if (sc != null && sc.length > 0) {
                        String m2 = "0"; // default value
                        for (int i = 0; i < sc.length; i++) {
                            FieldDesc p = sc[i].getFieldDesc();
                            if (p.isMax()) {
                                String id = p.getId();
                                // field name is "mag" by convention
                                if (id != null && id.equalsIgnoreCase("mag")) {
                                    m2 = sc[i].getVal().toString();
                                    break;
                                }
                            }
                        }
                        buf.append(m2);
                    }
                    c += 2;
                }
                else if (urlStr.charAt(c) == 'n') {
                    if (queryArgs.getMaxRows() > 0)
                        buf.append(queryArgs.getMaxRows());
                    c++;
                }
                else if (urlStr.startsWith("cond", c)) {
                    // insert a list of conditions (param names and min/max values)
                    if (sc != null && sc.length > 0) {
                        for (int i = 0; i < sc.length; i++) {
                            // Note: ignore the "standard" parameters: defined in
                            // SkycatConfigEntry.determineSearchParameters()
                            String name = sc[i].getName();
                            if ((isWCS() && (name.equalsIgnoreCase(SkycatConfigEntry.OBJECT)
                                    || name.equalsIgnoreCase(SkycatConfigEntry.RA)
                                    || name.equalsIgnoreCase(SkycatConfigEntry.DEC)
                                    || name.equalsIgnoreCase(SkycatConfigEntry.EQUINOX)))
                                    || (isPix() && (name.equalsIgnoreCase(SkycatConfigEntry.X)
                                    || name.equalsIgnoreCase(SkycatConfigEntry.Y)))
                                    || name.equalsIgnoreCase(SkycatConfigEntry.MIN_RADIUS)
                                    || name.equalsIgnoreCase(SkycatConfigEntry.MAX_RADIUS)
                                    || name.equalsIgnoreCase(SkycatConfigEntry.WIDTH)
                                    || name.equalsIgnoreCase(SkycatConfigEntry.HEIGHT)
                                    || name.equalsIgnoreCase(SkycatConfigEntry.MAX_OBJECTS)) {
                                continue;
                            }

                            String id = sc[i].getFieldDesc().getId();
                            if (id == null)
                                id = name;
                            if (sc[i].isRange())
                                buf.append(id + "=" + sc[i].getMinVal() + "," + sc[i].getMaxVal());
                            else
                                buf.append(id + "=" + sc[i].getMinVal());
                            if (sc.length - i > 1)
                                buf.append('&');
                        }
                    }
                    c += 4;
                }
                else if (urlStr.startsWith("mime-type", c)) {
                    buf.append("application/x-fits"); // XXX should be hard coded in the config file?
                    c += 9;
                }
                /* XXX these were supported in skycat, but not by any servers I know of
		else if (urlStr.startsWith("cols", c)) {
		    // insert a list of column names
		    String[] colNames = queryArgs.getNames();
		    for (int i = 0; i < colNames.length; i++) {
			buf.append(colNames[i]);
			if (colNames.length - i > 1)
			    buf.append(',');
		    }
		    c += 4;
		}
		else if (urlStr.startsWith("sortorder", c)) {
		    buf.append(queryArgs.getSortOrder() ? "increasing" : "decreasing");
		    c += 9;
		}
		else if (urlStr.startsWith("sort", c)) {
		    // insert a list of sort column names
		    // XXX note: not all servers may accept the list...
		    String[] sortCols = queryArgs.getSortColumns();
		    if (sortCols.length > 0) {
			for (int i = 0; i < sortCols.length; i++) {
			    buf.append(sortCols[i]);
			    if (sortCols.length - i > 1)
				buf.append(',');
			}
		    }
		    c += 4;
		}
		*/
            }
            else {
                buf.append(urlStr.charAt(c++));
            }
        }

        // report an error if the caller specified an id, but there is none in the URL
        if (!urlHasId && queryArgs.getId() != null && queryArgs.getId().length() != 0)
            throw new RuntimeException(_entry.getName() + " does not support search by id");

        // report an error if the caller supplied a position, but there is none in the URL
        if (queryArgs.getRegion() != null) {
            if (queryArgs.getRegion().getCenterPosition() instanceof WorldCoords && !urlHasRaDec)
                throw new RuntimeException(_entry.getName() + " does not support search by World Coordinates");

            if (queryArgs.getRegion().getCenterPosition() instanceof ImageCoords && !urlHasXy)
                throw new RuntimeException(_entry.getName() + " does not support search by image coordinates");
        }

        if (Logger.isDebugEnabled(this)) {
            Logger.debug(this, "URL = " + buf.toString());
        }

        return buf.toString();
    }


    /** Return the equinox setting from the given query arguments object. */
    private double _getEquinox(QueryArgs queryArgs) {
        String equinoxStr = (String) queryArgs.getParamValue(SkycatConfigEntry.EQUINOX);
        double equinox = 2000.;
        if (equinoxStr != null && equinoxStr.endsWith("1950"))
            equinox = 1950.;
        return equinox;
    }

    /** Determine the query region based on the given query arguments */
    protected void _setQueryRegion(QueryArgs queryArgs, SearchCondition[] sc) throws IOException {
        if (queryArgs.getRegion() != null || sc == null || sc.length == 0)
            return;

        // look for a min and max radius parameters
        Double r1 = (Double) queryArgs.getParamValue(SkycatConfigEntry.MIN_RADIUS);
        Double r2 = (Double) queryArgs.getParamValue(SkycatConfigEntry.MAX_RADIUS);
        if (r1 != null || r2 != null) {
            if (r1 != null) {
                if (r2 == null) {
                    r2 = r1;
                    r1 = new Double(0.);
                }
            }
            else if (r2 != null) {
                if (r1 == null)
                    r1 = new Double(0.);
            }
        }
        else {
            // look for a radius search condition
            for (int i = 0; i < sc.length; i++) {
                String name = sc[i].getName();
                if (name.equalsIgnoreCase("radius")) {
                    r1 = (Double) sc[i].getMinVal();
                    r2 = (Double) sc[i].getMaxVal();
                    break;
                }
            }
        }
        if (r1 == null && r2 == null) {
            // use default values
            r1 = new Double(0.);
            r2 = new Double(10.);
        }

        // look for the center position parameters
        if (isWCS()) {
            WorldCoords wcs;
            String objectName = (String) queryArgs.getParamValue(SkycatConfigEntry.OBJECT);
            if (objectName == null || objectName.length() == 0) {
                // no object name specified, check RA and Dec
                String raStr = (String) queryArgs.getParamValue(SkycatConfigEntry.RA);
                String decStr = (String) queryArgs.getParamValue(SkycatConfigEntry.DEC);
                if (raStr == null || decStr == null)
                    return;
                double equinox = _getEquinox(queryArgs);
                wcs = new WorldCoords(raStr, decStr, equinox, true);
            }
            else {
                // an object name was specified, which needs to be resolved with a nameserver
                Object o = queryArgs.getParamValue(SkycatConfigEntry.NAME_SERVER);
                if (!(o instanceof Catalog))
                    throw new RuntimeException("No name server was specified");
                wcs = _resolveObjectName(objectName, (Catalog) o);
            }
            queryArgs.setRegion(new CoordinateRadius(wcs, r1.doubleValue(), r2.doubleValue()));
        }
        else if (isPix()) {
            Double x = (Double) queryArgs.getParamValue(SkycatConfigEntry.X);
            Double y = (Double) queryArgs.getParamValue(SkycatConfigEntry.Y);
            if (x == null || y == null)
                return;
            ImageCoords ic = new ImageCoords(x.intValue(), y.intValue());
            queryArgs.setRegion(new CoordinateRadius(ic, r1.doubleValue(), r2.doubleValue()));
        }
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
        Integer maxObjects = (Integer) queryArgs.getParamValue(SkycatConfigEntry.MAX_OBJECTS);
        if (maxObjects != null)
            queryArgs.setMaxRows(maxObjects.intValue());
    }

    /** Optional handler, used to report HTML format errors from HTTP servers */
    public void setHTMLQueryResultHandler(HTMLQueryResultHandler handler) {
        _htmlQueryResultHandler = handler;
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
        RowCoordinates rowCoordinates = _entry.getRowCoordinates();
        String equinoxStr = (String) queryArgs.getParamValue(SkycatConfigEntry.EQUINOX);
        double equinox = _getEquinox(queryArgs);
        if (rowCoordinates.isWCS()) {
            WorldCoords pos = (WorldCoords) coords;
            String[] radec = pos.format(equinox);
            queryArgs.setParamValue(SkycatConfigEntry.RA, radec[0]);
            queryArgs.setParamValue(SkycatConfigEntry.DEC, radec[1]);
            queryArgs.setParamValue(SkycatConfigEntry.EQUINOX, equinoxStr);
            queryArgs.setParamValue(SkycatConfigEntry.MIN_RADIUS, region.getMinRadius());
            queryArgs.setParamValue(SkycatConfigEntry.MAX_RADIUS, region.getMaxRadius());
            queryArgs.setParamValue(SkycatConfigEntry.WIDTH, region.getWidth());
            queryArgs.setParamValue(SkycatConfigEntry.HEIGHT, region.getHeight());
        }
        else if (rowCoordinates.isPix()) {
            ImageCoords pos = (ImageCoords) coords;
            queryArgs.setParamValue(SkycatConfigEntry.X, pos.getX());
            queryArgs.setParamValue(SkycatConfigEntry.Y, pos.getY());
            queryArgs.setParamValue(SkycatConfigEntry.MIN_RADIUS, region.getMinRadius());
            queryArgs.setParamValue(SkycatConfigEntry.MAX_RADIUS, region.getMaxRadius());
            queryArgs.setParamValue(SkycatConfigEntry.WIDTH, region.getWidth());
            queryArgs.setParamValue(SkycatConfigEntry.HEIGHT, region.getHeight());
        }
    }


    /**
     * Test cases
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("usage: java -classpath ... SkycatCatalog catalogName");
            System.exit(1);
        }
        String catalogName = args[0];
        SkycatConfigFile configFile = SkycatConfigFile.getConfigFile();
        Catalog cat = configFile.getCatalog(catalogName);
        if (cat == null) {
            System.out.println("Can't find entry for catalog: " + catalogName);
            System.exit(1);
        }

        try {
            QueryArgs q1 = new BasicQueryArgs(cat);
            q1.setId("GSC0285601186");
            QueryResult r1 = cat.query(q1);
            System.out.println("result: " + r1);

            System.out.println("");
            System.out.println("test query: at center position/radius: ");
            QueryArgs q2 = new BasicQueryArgs(cat);
            q2.setRegion(new CoordinateRadius(new WorldCoords("03:19:44.44", "+41:30:58.21"), 2.));
            QueryResult r2 = cat.query(q2);
            System.out.println("result: " + r2);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
