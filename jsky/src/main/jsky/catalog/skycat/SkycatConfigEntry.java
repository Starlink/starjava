/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SkycatConfigEntry.java,v 1.19 2002/08/05 10:57:21 brighton Exp $
 */

package jsky.catalog.skycat;

import java.io.File;
import java.net.URL;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import jsky.catalog.Catalog;
import jsky.catalog.CatalogDirectory;
import jsky.catalog.FieldDesc;
import jsky.catalog.FieldDescAdapter;
import jsky.catalog.RowCoordinates;
import jsky.catalog.TablePlotSymbol;
import jsky.util.NameValue;
import jsky.util.StringUtil;
import jsky.util.TclUtil;
import java.util.List;


/**
 * Manages the configuration information for a Skycat style catalog.
 *
 * @version $Revision: 1.19 $ $Date: 2002/08/05 10:57:21 $
 * @author Allan Brighton
 */
public class SkycatConfigEntry {

    /** parameter: object (star, galaxy,...) name (to be resolved by a name server) */
    public static final String OBJECT = "Object";

    /** parameter: name server to use to resolve an object (star, galaxy, ...) name. */
    public static final String NAME_SERVER = "Name Server";

    /** parameter: RA coordinate */
    public static final String RA = "RA";

    /** parameter: DEC coordinate */
    public static final String DEC = "Dec";

    /** parameter: equinox of RA,Dec */
    public static final String EQUINOX = "Equinox";

    /** parameter: X coordinate (image coords) */
    public static final String X = "X";

    /** parameter: Y coordinate (image coords) */
    public static final String Y = "Y";

    /** parameter: minimum radius for center coordinates in arcmin (from RA,Dec), or pixels (from X,Y). */
    public static final String MIN_RADIUS = "Min Radius";

    /** parameter: maximum radius for center coordinates in arcmin (from RA,Dec), or pixels (from X,Y). */
    public static final String MAX_RADIUS = "Max Radius";

    /** parameter: max number of objects to return from a query. */
    public static final String MAX_OBJECTS = "Max Objects";

    /** parameter: width of query area (in arcmin or pixels) */
    public static final String WIDTH = "Width";

    /** parameter: height of query area (in arcmin or pixels) */
    public static final String HEIGHT = "Height";

    /** Default equinox. */
    public static final String J2000 = "2000";

    /** Alternative equinox. */
    public static final String B1950 = "1950";


    /** A reference to the SkycatConfigFile object containing this catalog, if known. */
    private CatalogDirectory _configFile;

    /** The name of the catalog  */
    private String _name;

    /** The same as name, except for local catalogs  */
    private String _longName;

    /** A short name or alias for the catalog  */
    private String _shortName;

    /** contains keyword, value pairs from the catalog config file */
    private Properties _properties;

    /** The type of the catalog server ("catalog", "local", "imagesvr", ... */
    private String _servType;

    /** Object storing the column indexes where RA,Dec or X,Y are found */
    private RowCoordinates _rowCoordinates;

    /**
     * An array of URLs (or command names) to use to access the catalog.
     * The first URL is the main one, the others are backups.
     */
    private String[] _urls;

    /** An array of objects describing the columns that can be used to search this catalog. */
    private FieldDescAdapter[] _paramDesc;

    /** Options for equinox parameter */
    private static NameValue[] _equinoxOptions = new NameValue[] {
	new NameValue("J2000", J2000),
	new NameValue("B1950", B1950)
    };

    // An array of plot symbol descriptions, constructed from the symbol property string
    private TablePlotSymbol[] _symbols;

    // Set to true if the user edited the plot symbol definitions
    private boolean _symbolsEdited = false;


    /**
     * Initialize the config entry from the given catalog configuration properties.
     * See <A href="http://archive.eso.org/skycat/">the Skycat web page</A> for
     * links to a description of the standard keywords.
     *
     * @param configFile a reference to the SkycatConfigFile object containing
     *                   this entry, if known (used to search for a name server entry)
     *
     * @param properties contains (keyword : value) pairs, such as those
     *                   found in a (skycat style) catalog configuration file,
     *                   describing the catalog features.
     */
    public SkycatConfigEntry(SkycatConfigFile configFile, Properties properties) {
        _configFile = configFile;
        _properties = properties;
        parseProperties();
    }


    /**
     * Initialize the config entry from the given catalog configuration properties.
     * See <A href="http://archive.eso.org/skycat/">the Skycat web page</A> for
     * links to a description of the standard keywords.
     *
     * @param properties contains (keyword : value) pairs, such as those
     *                   found in a (skycat style) catalog configuration file,
     *                   describing the catalog features.
     */
    public SkycatConfigEntry(Properties properties) {
        this(SkycatConfigFile.getConfigFile(), properties);
    }

    /** Return the fields of this catalog entry as a property table. */
    public Properties getProperties() {
        return _properties;
    }

    /**
     * Return a reference to the SkycatConfigFile object containing this catalog, or
     * null if not known.
     */
    public CatalogDirectory getConfigFile() {
        return _configFile;
    }

    /**
     * Return a reference to the SkycatConfigFile object containing this catalog, or
     * null if not known.
     */
    public void setConfigFile(CatalogDirectory configFile) {
        _configFile = configFile;
    }

    /**
     * Parse the catalog properties and set the values for the member variables as needed.
     */
    protected void parseProperties() {
        _servType = _properties.getProperty(SkycatConfigFile.SERV_TYPE);
        if (_servType == null)
            throw new RuntimeException("Missing 'serv_type' entry in catalog config file");

        _longName = _properties.getProperty(SkycatConfigFile.LONG_NAME);
        _name = _longName;
        if (_servType.equals("local") && _name != null) {
            _name = new File(_name).getName(); // just use tail of name
        }
        _shortName = _properties.getProperty(SkycatConfigFile.SHORT_NAME);
        if (_shortName == null)
            _shortName = _name;
        if (_name == null)
            throw new RuntimeException("Missing 'long_name' entry in catalog config file");

        // get the URL (or command name) and backups
        _urls = new String[3];

        // Start DM change (Daniella Malin <daniella@lmtsun.astro.umass.edu>):
        // Allow a variable in the URL in the form ${name}, where "name" is the
        // name of a system property.
        String lmtmc;
        String cat_dir = _properties.getProperty(SkycatConfigFile.URL);
        if (cat_dir != null) {
            int mark = cat_dir.indexOf("{") + 1;
            if (mark > 0) {
                if (cat_dir.indexOf("}") > -1) {
                    lmtmc = cat_dir.substring(mark, cat_dir.indexOf("}"));
                    lmtmc = System.getProperty(lmtmc);
                    mark = cat_dir.indexOf("/");
                    cat_dir = cat_dir.substring(mark, cat_dir.length());
                    cat_dir = lmtmc + cat_dir;
                }
            }
        }
        _urls[0] = cat_dir;
        //_urls[0] = _properties.getProperty(SkycatConfigFile.URL);
        // end DM change

        _urls[1] = _properties.getProperty(SkycatConfigFile.BACKUP1);
        _urls[2] = _properties.getProperty(SkycatConfigFile.BACKUP2);

        // just ignore any missing or malformatted column index values for now
        int idCol = -1, raCol = -1, decCol = -1, xCol = -1, yCol = -1;
        double equinox = 2000.;
        boolean idColSet = false;
        boolean raColSet = false;
        boolean decColSet = false;
        boolean xColSet = false;
        boolean yColSet = false;
        try {
            idCol = Integer.parseInt(getProperty(SkycatConfigFile.ID_COL));
            idColSet = true;
        }
        catch (Exception e) {
        }
        try {
            raCol = Integer.parseInt(getProperty(SkycatConfigFile.RA_COL));
            raColSet = true;
        }
        catch (Exception e) {
        }
        try {
            decCol = Integer.parseInt(getProperty(SkycatConfigFile.DEC_COL));
            decColSet = true;
        }
        catch (Exception e) {
        }
        try {
            equinox = Double.parseDouble(getProperty(SkycatConfigFile.EQUINOX));
        }
        catch (Exception e) {
        }
        try {
            xCol = Integer.parseInt(getProperty(SkycatConfigFile.X_COL));
            xColSet = true;
        }
        catch (Exception e) {
        }
        try {
            yCol = Integer.parseInt(getProperty(SkycatConfigFile.Y_COL));
            yColSet = true;
        }
        catch (Exception e) {
        }

        if (!idColSet)
            idCol = 0;
        if (!raColSet && !decColSet && !xColSet && !yColSet) {
            raCol = 1;
            decCol = 2;
        }
        if (raCol >= 0 && decCol >= 0)
            _rowCoordinates = new RowCoordinates(raCol, decCol, equinox);
        else if (xCol >= 0 && yCol >= 0)
            _rowCoordinates = new RowCoordinates(xCol, yCol);
        else
            _rowCoordinates = new RowCoordinates();

        _rowCoordinates.setIdCol(idCol);
    }


    /**
     * Skycat catalogs normally default to the following parameters:
     * objectName, nameServer, ra, dec, minRadius, maxRadius, maxObjects.
     * In addition, the search_cols config field may add search parameters.
     * <p>
     * Parse the search column information from the catalog config entry, if found,
     * and set the paramDesc member variable.
     * The format of the search_cols value is: name minLabel MaxLabel : ...,
     * (in Tcl list format).
     */
    protected void determineSearchParameters() {
        boolean isCatalog = _servType.equals("catalog") || _servType.equals("archive") || _servType.equals("local");

        if (_servType.equals("namesvr")) {
            _paramDesc = new FieldDescAdapter[1];
            _paramDesc[0] = new FieldDescAdapter(OBJECT);
            _paramDesc[0].setDescription("Enter the name of the object");
        }
        else if (isCatalog || _servType.equals("imagesvr")) {
            Vector params = new Vector(10, 10);
            FieldDescAdapter p;
            boolean hasCoords = false;

            // Define the "standard" parameters
            if (_rowCoordinates.isWCS()) {
                hasCoords = true;

                p = new FieldDescAdapter(OBJECT);
                p.setDescription("Enter the name of the object");
                params.add(p);

                p = new FieldDescAdapter(NAME_SERVER);
                p.setDescription("Select the name server to use to resolve the object name");
		List l = _configFile.getNameServers();
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
            }
            else if (_rowCoordinates.isPix()) {
                hasCoords = true;

                p = new FieldDescAdapter(X);
                p.setDescription("The X pixel coordinate");
                p.setFieldClass(Double.class);
                params.add(p);

                p = new FieldDescAdapter(Y);
                p.setDescription("The Y pixel coordinate");
                p.setFieldClass(Double.class);
                params.add(p);
	    }

            if (hasCoords) {
                if (isCatalog) {
                    p = new FieldDescAdapter(MIN_RADIUS);
                    p.setDescription("The minimum radius from the center coordinates in arcmin");
                    p.setFieldClass(Double.class);
		    p.setUnits("arcmin");
                    params.add(p);

                    p = new FieldDescAdapter(MAX_RADIUS);
                    p.setDescription("The maximum radius from the center coordinates in arcmin");
                    p.setFieldClass(Double.class);
		    p.setUnits("arcmin");
                    params.add(p);
                }
                else {
                    p = new FieldDescAdapter(WIDTH);
                    p.setDescription("The width about the center coordinates in arcmin");
                    p.setFieldClass(Double.class);
		    p.setUnits("arcmin");
                    params.add(p);

                    p = new FieldDescAdapter(HEIGHT);
                    p.setDescription("The height about the center coordinates in arcmin");
                    p.setFieldClass(Double.class);
		    p.setUnits("arcmin");
                    params.add(p);
                }
            }

            // Additional search parameters may be defined in the search_cols string
            if (!_servType.equals("local"))
                checkSearchCols(params);

            if (isCatalog) {
                p = new FieldDescAdapter(MAX_OBJECTS);
                p.setDescription("The maximum number of objects to return");
                p.setFieldClass(Integer.class);
                p.setDefaultValue(new Integer(1000));
                params.add(p);
            }
            _paramDesc = new FieldDescAdapter[params.size()];
            params.toArray(_paramDesc);
        }
	else {
	    _paramDesc = new FieldDescAdapter[0];
	}
    }


    /**
     * Check for additional search parameters defined in the "search_cols" property
     * and add them to the given parameter description vector.
     */
    protected void checkSearchCols(Vector params) {
        String searchCols = getProperty(SkycatConfigFile.SEARCH_COLS);
        if (searchCols != null && searchCols.length() != 0) {
            // parse the tcl lists
            StringTokenizer st = new StringTokenizer(searchCols, ":");
            int n = st.countTokens();
            for (int i = 0; i < n; i++) {
                String s = st.nextToken();
                String[] ar = TclUtil.splitList(s);
                FieldDescAdapter p;
                if (ar.length == 3) {
                    p = new FieldDescAdapter(ar[1]);
                    p.setId(ar[0]);
                    p.setIsMin(true);
                    params.add(p);
                    p = new FieldDescAdapter(ar[2]);
                    p.setId(ar[0]);
                    p.setIsMax(true);
                    params.add(p);
                }
                else if (ar.length == 2) {
                    p = new FieldDescAdapter(ar[1]);
                    params.add(p);
                }
                else {
                    throw new RuntimeException("Invalid format for search columns specification: " + s);
                }
            }
        }
    }

    public static NameValue[] getEquinoxOptions() {
        return _equinoxOptions;
    }

    /** Return the value of the named property as a String */
    public String getProperty(String key) {
        return _properties.getProperty(key);
    }

    /** Set the name of the catalog  */
    public void setName(String name) {
        _name = name;
    }

    /** Return the name of the catalog  */
    public String getName() {
        return _name;
    }

    /** Return a URL pointing to documentation for the catalog, or null if not available */
    public URL getDocURL() {
        String s = getProperty(SkycatConfigFile.HELP);
        URL url = null;
        if (s != null) {
            try {
                url = new URL(s);
            }
            catch (Exception e) {
                return null;
            }
        }
        return url;
    }

    /** Return the value from the serv_type field. */
    public String getServType() {
        return _servType;
    }

    /** Return the value from the serv_type field. */
    public void setServType(String servType) {
        _servType = servType;
    }

    /** If this catalog can be querried, return the number of query parameters that it accepts */
    public int getNumParams() {
        if (_paramDesc == null) 
	    determineSearchParameters();
        return _paramDesc.length;
    }

    /** Return a description of the ith query parameter */
    public FieldDesc getParamDesc(int i) {
        if (_paramDesc == null) 
	    determineSearchParameters();

        return _paramDesc[i];
    }

    /** Return a description of the named query parameter, if found, otherwise null. */
    public FieldDesc getParamDesc(String name) {
        if (_paramDesc == null) 
	    determineSearchParameters();
	for(int i = 0; i < _paramDesc.length; i++)
	    if (_paramDesc[i] != null && _paramDesc[i].getName().equals(name))
		return _paramDesc[i];
	return null;
    }

    /** Set the array describing the query parameters. */
    public void setParamDesc(FieldDescAdapter[] paramDesc) {
        _paramDesc = paramDesc;
    }

    /** Return a short name or alias for the catalog  */
    public String getShortName() {
        return _shortName;
    }

    /** Return the long name of the catalog  */
    public String getLongName() {
        return _longName;
    }

    /** Return the number of query URLs defined */
    public int getNumURLs() {
        return _urls.length;
    }

    /** Return the nth query URL */
    public String getURL(int n) {
        return _urls[n];
    }

    /** Return the nth query URL */
    public void setURLs(String[] urls) {
        _urls = urls;
    }

    /** Return the object storing the column indexes where RA,Dec or X,Y are found */
    public RowCoordinates getRowCoordinates() {
        return _rowCoordinates;
    }

    /** Return the number of plot symbol definitions associated with this catalog. */
    public int getNumSymbols() {
        return getSymbols().length;
    }

    /** Return the ith plot symbol description */
    public TablePlotSymbol getSymbolDesc(int i) {
        return getSymbols()[i];
    }

    /** Return the array of symbol descriptions */
    public TablePlotSymbol[] getSymbols() {
	if (_symbols == null)
	    _symbols = _parsePlotSymbolInfo();
        return _symbols;
    }

    /** Set the array of catalog table plot symbol definitions */
    public void setSymbols(TablePlotSymbol[] symbols) {
        _symbols = symbols;
        String symbolInfo = TablePlotSymbol.getPlotSymbolInfo(symbols);
	if (symbolInfo != null)
	    _properties.setProperty(SkycatConfigFile.SYMBOL, symbolInfo);
    }

    /** Set to true if the user edited the plot symbol definitions (default: false) */
    public void setSymbolsEdited(boolean edited) {
	_symbolsEdited = edited;
    }

    /** Return true if the user edited the plot symbol definitions otherwise false */
    public boolean isSymbolsEdited() {
	return _symbolsEdited;
    }

    /**
     * Parsed the plot symbol information for the given table and return
     * an array of objects describing it.
     *
     * @param table object representing the catalog table
     * @return an array of TablePlotSymbol objects, one for each plot symbol defined.
     */
    private TablePlotSymbol[] _parsePlotSymbolInfo() {
        String symbolInfo = getProperty(SkycatConfigFile.SYMBOL);
        String[] ar = null;
        if (symbolInfo == null) {
            // default symbol settings
            ar = new String[]{"", "square yellow", "4"};
        }
        else {
            // Some config entries may not be separated with spaces ("{...}:{...}").
            // Insert spaces to avoid Tcl list errors
            symbolInfo = StringUtil.replace(symbolInfo, ":", " : ");

            // The format of symbolInfo is: list : list : ...,
            // where each list has 3 items: colInfo plotInfo sizeInfo,
            // where each item may have some more details specified (see the
            // skycat docs for details).
            // In the array below, every 4th element (if there is one) should
            // be a ":", since we are treating the entire string as one Tcl list.
            ar = TclUtil.splitList(symbolInfo);
            if (ar.length < 3)
                throw new RuntimeException("Bad plot symbol entry: " + symbolInfo);
        }

        // number of plot symbols (each entry has three elements, plus ":" between)
        TablePlotSymbol[] symbols = new TablePlotSymbol[(ar.length + 1) / 4];

        int n = 0;
        for (int i = 0; i < ar.length; i += 4) {
            if ((ar.length > i + 3 && !ar[i + 3].equals(":")) || ar.length < i + 3)
                throw new RuntimeException("Bad plot symbol entry: " + symbolInfo);
            symbols[n++] = new SkycatPlotSymbol(null, ar[i], ar[i + 1], ar[i + 2]);
        }
        return symbols;
    }
}

