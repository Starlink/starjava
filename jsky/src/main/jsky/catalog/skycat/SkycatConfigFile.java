/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SkycatConfigFile.java,v 1.21 2002/08/04 21:48:50 brighton Exp $
 */


package jsky.catalog.skycat;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import jsky.catalog.AbstractCatalogDirectory;
import jsky.catalog.Catalog;
import jsky.catalog.CatalogDirectory;
import jsky.catalog.CatalogFactory;
import jsky.catalog.HTMLQueryResultHandler;
import jsky.util.FileUtil;
import jsky.util.Resources;
import jsky.util.StringUtil;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.ProgressPanel;


/**
 * Reads a Skycat style catalog config file and stores
 * information about the catalogs defined there. Since catalog
 * config files may point to other catalog config files to form a
 * hierarchy, this class also implements the CatalogDirectory interface.
 * <p>
 * See <A href="http://archive.eso.org/skycat/">the Skycat web page</A> for
 * more information about the format of Skycat catalog config files.
 *
 * @version $Revision: 1.21 $ $Date: 2002/08/04 21:48:50 $
 * @author Allan Brighton
 */
public class SkycatConfigFile extends AbstractCatalogDirectory {

    /** Top level config file */
    private static SkycatConfigFile _configFile;

    /** The URL for the default catalog config file, if set. */
    private static URL _defaultURL = null;

    /** Panel used to display download progress information */
    private ProgressPanel _progressPanel;


    /** Constants for some config file strings */
    public static final String SERV_TYPE = "serv_type";
    public static final String LONG_NAME = "long_name";
    public static final String SHORT_NAME = "short_name";
    public static final String URL = "url";
    public static final String BACKUP1 = "backup1";
    public static final String BACKUP2 = "backup2";
    public static final String EQUINOX = "equinox";
    public static final String SYMBOL = "symbol";
    public static final String ID_COL = "id_col";
    public static final String RA_COL = "ra_col";
    public static final String DEC_COL = "dec_col";
    public static final String X_COL = "x_col";
    public static final String Y_COL = "y_col";
    public static final String SEARCH_COLS = "search_cols";
    public static final String HELP = "help";

    /**
     * Parse the skycat style catalog config file pointed to by the given URL.
     *
     * @param name the display name for the config file
     * @param url the URL of the config file
     */
    public SkycatConfigFile(String name, URL url) {
	super(name);
        setURL(url);
        _load();
    }


    /**
     * Parse the given skycat style config file or URL.
     *
     * @param name the display name for the config file
     * @param configFileOrURL the file name or URL of the config file
     */
    public SkycatConfigFile(String name, String configFileOrURL) {
        this(name, FileUtil.makeURL(null, configFileOrURL));
    }


    /**
     * Parse the given skycat style config file or URL.
     *
     * @param configFileOrURL the file name or URL of the config file
     */
    public SkycatConfigFile(String configFileOrURL) {
        this(configFileOrURL, FileUtil.makeURL(null, configFileOrURL));
    }


    /**
     * Parse the skycat style config file from the already opened input stream.
     * The URL is passed as a reference.
     *
     * @param url the URL of the config file
     * @param handler used to report HTML errors from the HTTP server
     */
    public SkycatConfigFile(URL url, HTMLQueryResultHandler handler) {
	super(new File(url.toString()).getPath());
        setURL(url);
        setHTMLQueryResultHandler(handler);
        _load();
    }


    /**
     * Load the skycat style config file from the URL and store any catalogs
     * found there in the catalogs vector.
     */
    private void _load() {
	URL url = getURL();
	if (url == null)
	    return;

        try {
            String protocol = url.getProtocol();
            if (protocol.equals("file") || protocol.equals("jar")) {
                //  a local file, just read it
                _load(url.openStream());
            }
            else {
                // a remote URL
                if (_progressPanel == null)
                    _progressPanel = ProgressPanel.makeProgressPanel("Downloading the catalog config file...");
                _progressPanel.start();
                _progressPanel.setText("Connect: " + url.getHost() + ", waiting for reply.");

                URLConnection connection = _progressPanel.openConnection(url);
                String contentType = connection.getContentType();
                if (contentType.equals("text/html")) {
                    // must be an HTML formatted error message from the server
		    HTMLQueryResultHandler handler = getHTMLQueryResultHandler();
                    if (handler != null) {
                        handler.displayHTMLPage(url);
                        throw new RuntimeException("Error reading catalog config file URL: " + url.toString());
                    }
                }
                _load(connection.getInputStream());
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("I/O error reading catalog config file: " + e);
        }
        finally {
            if (_progressPanel != null)
                _progressPanel.stop();
        }
    }


    /**
     * Load the skycat style config file from the given stream and store any catalogs
     * found there in the catalogs vector.
     */
    private void _load(InputStream stream) {
        BufferedReader r = new BufferedReader(new InputStreamReader(stream));
        String line;
        int lineNumber = 0;
        Properties properties = null;
	List catalogs = new Vector();

        if (_progressPanel != null)
            _progressPanel.setText("Reading catalog config file...");

        while ((line = _getLine(r)) != null) {
            lineNumber++;

            // skip comments and empty lines
            if (line.length() == 0 || line.startsWith("#"))
                continue;

            // split keyword : value
            int i = line.indexOf(':');
            if (i == -1)
                throw new RuntimeException(getURL().toString() + ": line " + lineNumber
                        + ": missing separator (':') in config file");

            String key = line.substring(0, i).trim();
            String value = line.substring(i + 1).trim();

            if (key.equals(SERV_TYPE)) { // start a new entry
                if (properties != null) {
                    // add catalog for previous entry
                    SkycatConfigEntry entry = new SkycatConfigEntry(this, properties);
                    SkycatCatalog cat = new SkycatCatalog(entry, getHTMLQueryResultHandler());
                    catalogs.add(cat);
		    CatalogFactory.registerCatalog(cat, isLocal());
                }
                properties = new Properties();
            }
            properties.setProperty(key, value);
        }
        if (properties != null) {
            // register last catalog entry
            SkycatConfigEntry entry = new SkycatConfigEntry(this, properties);
            SkycatCatalog cat = new SkycatCatalog(entry, getHTMLQueryResultHandler());
            catalogs.add(cat);
        }
	setCatalogs(catalogs);

        if (_progressPanel != null)
            _progressPanel.setText("Done.");
    }


    /**
     * Read a line from the given BufferedReader and return the string.
     * Lines ending with backslash are continued on the next line
     *
     * @param r The BufferedReader to read from
     * @return the contents of the line, after backslash processing.
     */
    private String _getLine(BufferedReader r) {
        String line = null;
        try {
            if ((line = r.readLine()) != null) {
                if (line.endsWith("\\")) {
                    StringBuffer result = new StringBuffer(line);
                    do {
                        result.setLength(result.length() - 1);
                        if ((line = r.readLine()) != null)
                            result.append(line);
                    } while (line.endsWith("\\"));
                    return result.toString();
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return line;
    }


    /** This method is called once at startup to load the top level catalog directory */
    public static CatalogDirectory getDirectory() {
	return getConfigFile();
    }


    /**
     * If the catalog config file has already been loaded, return an
     * object describing the contents, otherwise search for a skycat
     * style catalog config file, load the contents if found, and return
     * the object for it.
     * <p>
     * First the <em>jsky.catalog.skycat.config</em> system property is checked.
     * If set, it should be the URL string or file name of the config file.
     * <p>
     * Next, the file ~/.jsky/skycat.cfg is checked. This file is created when
     * the user makes changes in the plot symbol settings in the table
     * display/configure window.
     * <p>
     * Finally, a default URL s used. It may be set by calling "setConfigFile"
     * and defaults to a config file included in this package (as a resource
     * file: jsky/catalog/skycat/skycat.cfg).
     *
     * @return a SkycatConfigFile object constructed from the file.
     */
    public static SkycatConfigFile getConfigFile() {
        if (_configFile != null)
            return _configFile;

        String[] urls = new String[4];
        int index = 0;

        // check the system property
        String urlStr = System.getProperty("jsky.catalog.skycat.config");
        if (urlStr != null && urlStr.length() != 0)
            urls[index++] = urlStr;

        // check for ~/.jsky/skycat.cfg
        String home = System.getProperty("user.home");
        String sep = System.getProperty("file.separator");
        String filename = home + sep + ".jsky" + sep + "skycat.cfg";
        if (new File(filename).exists()) {
            urlStr = "file:" + filename;
            if (!sep.equals("/"))
                urlStr = StringUtil.replace(urlStr, sep, "/");
            urls[index++] = urlStr;
        }

        // use the default resource
        if (_defaultURL == null)
            _defaultURL = Resources.getResource("skycat.cfg");
        if (_defaultURL == null)
            throw new RuntimeException("Can't find the default catalog config file resource (skycat.cfg).");
        urls[index++] = _defaultURL.toString();

        // add ~/.skycat/skycat.cfg to the catalog directory list
        filename = home + sep + ".skycat" + sep + "skycat.cfg";
        if (new File(filename).exists()) {
            urlStr = "file:" + filename;
            if (!sep.equals("/"))
                urlStr = StringUtil.replace(urlStr, sep, "/");
        }
        urls[index++] = urlStr;

        // use the first URL found for the main catalog list
        _configFile = new SkycatConfigFile(urls[0]);

        // include the other URLs as catalog directories
        for (int i = 1; i < index; i++) {
            if (urls[i] != null)
                _configFile.addCatalogDirectory(urls[i]);
        }

        return _configFile;
    }


    /**
     * Reload the catalog config file and return the new object for it.
     */
    public CatalogDirectory reload() {
        _configFile = null;
        getConfigFile();
	return _configFile;
    }


    /** 
     * Attempt to read a catalog subdirectory from the given URL and return
     * a CatalogDirectory object for it.
     *
     * @return the new CatalogDirectory
     * @throws RuntimeException if the catalog directory could not be created
     */
    public CatalogDirectory loadSubDir(URL url) {
	String filename = url.getFile();
	if (filename.endsWith(".cfg")) {
	    SkycatConfigFile cf = new SkycatConfigFile(url, getHTMLQueryResultHandler());
	    return cf;
	}
	throw new RuntimeException("Expected a Skycat style .cfg file, or an AstroCat XML file");
    }


    /**
     * Set the URL to use for the default catalog config file.
     *
     * @param url points to the skycat style catalog config file
     */
    public static void setConfigFile(URL url) {
        _defaultURL = url;
    }


    /**
     * Add a catalog directory to the catalog list.
     *
     * @param urlStr the URL of a catalog config file.
     */
    public void addCatalogDirectory(String urlStr) {
        if (getCatalog(urlStr) != null)
            return;
        Properties properties = new Properties();
        properties.setProperty(SERV_TYPE, "directory");
        properties.setProperty(LONG_NAME, urlStr);
        properties.setProperty(URL, urlStr);
        SkycatConfigEntry entry = new SkycatConfigEntry(this, properties);
        SkycatCatalog cat = new SkycatCatalog(entry, getHTMLQueryResultHandler());
        addCatalog(cat);
    }

    /**
     * Return a list of name servers (Catalogs with type equal to "namesvr") 
     * to use to resolve astronomical object names.
     */
    public List getNameServers() {
        List l = CatalogFactory.getCatalogsByType(Catalog.NAME_SERVER);
	if (l.size() != 0) {
	    return l;
	}
	_addDefaultNameServers();
        return CatalogFactory.getCatalogsByType(Catalog.NAME_SERVER);
    }

    /**
     * Return a vector containing a list of name servers (SkycatCatalogs with serv_type
     * equal to "namesvr") to use to resolve astronomical object names.
     */
    private void _addDefaultNameServers() {
	Properties p1 = new Properties();
	p1.setProperty(SERV_TYPE, "namesvr");
	p1.setProperty(LONG_NAME, "SIMBAD Names");
	p1.setProperty(SHORT_NAME, "simbad_ns@eso");
	p1.setProperty(URL, "http://archive.eso.org/skycat/servers/sim-server?&o=%id");
	SkycatConfigEntry entry = new SkycatConfigEntry(this, p1);
	SkycatCatalog cat = new SkycatCatalog(entry);
	CatalogFactory.registerCatalog(cat, false);

	Properties p2 = new Properties();
	p2.setProperty(SERV_TYPE, "namesvr");
	p2.setProperty(LONG_NAME, "NED Names");
	p2.setProperty(SHORT_NAME, "ned@eso");
	p2.setProperty(URL, "http://archive.eso.org/skycat/servers/ned-server?&o=%id");
	entry = new SkycatConfigEntry(this, p2);
	cat = new SkycatCatalog(entry);
	CatalogFactory.registerCatalog(cat, false);
    }

    /**
     * Save the config file to a file under the user's home directory
     * (~/.jsky/skycat.cfg) to make it permanent. The information is saved in the
     * same format used by the skycat application.
     */
    public void save() {
        String home = System.getProperty("user.home");
        String sep = System.getProperty("file.separator");
        String filename = home + sep + ".jsky" + sep + "skycat.cfg";
        try {
            save(filename);
        }
        catch (Exception e) {
            DialogUtil.error(e);
        }
    }


    /**
     * Save the contents of this object as a new skycat style catalog
     * config file using the given file name.
     */
    public void save(String filename) throws IOException {
        File file = new File(filename + ".tmp");
        File dir = file.getParentFile();
        if (!dir.isDirectory())
            dir.mkdirs();
        OutputStream stream = new BufferedOutputStream(new FileOutputStream(file));
        try {
            save(stream);
        }
        catch (Exception e) {
            stream.close();
            file.delete();
            throw new RuntimeException(e);
        }
        stream.close();

        File newFile = new File(filename);
        newFile.delete(); // needed under Windows!
        if (!file.renameTo(newFile))
            DialogUtil.error("Rename " + file + " to " + filename + " failed");
    }

    /**
     * Save the contents of this object in skycat catalog
     * config file format to the given stream.
     */
    public void save(OutputStream stream) {
        PrintWriter out = new PrintWriter(stream);

        out.println("# Catalog config file");
        out.println("# This file was automatically generated by JSky.");
        out.println();

	List catalogs = getCatalogs();
        int numCatalogs = getNumCatalogs();
        for (int i = 0; i < numCatalogs; i++) {
            out.println();
            SkycatCatalog catalog = (SkycatCatalog) catalogs.get(i);
            SkycatConfigEntry entry = catalog.getConfigEntry();
            Properties properties = entry.getProperties();
            out.println(SERV_TYPE + ": " + properties.getProperty(SERV_TYPE));
            for (Enumeration e = properties.keys(); e.hasMoreElements();) {
                String s = (String) e.nextElement();
                if (!s.equals(SERV_TYPE)) {
                    out.println(s + ": " + properties.getProperty(s));
		}
		
            }
        }
        out.close();
    }

    public void setParent(CatalogDirectory dir) {
	CatalogDirectory old = getParent();
	super.setParent(dir);
	String oldStr = "null", newStr = null;
	if (old != null)
	    oldStr = old.getName();
	if (dir != null)
	    newStr = dir.getName();
    }

    /**
     * Test cases
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("usage: java -classpath ... SkycatConfigFile catalogName");
            System.exit(1);
        }
        String catalogName = args[0];
        SkycatConfigFile configFile = SkycatConfigFile.getConfigFile();

        Catalog cat = configFile.getCatalog(catalogName);
        if (cat == null) {
            System.out.println("Can't find entry for catalog: " + catalogName);
            System.exit(1);
        }
        else {
            System.out.println("Test passed");
        }
    }
}
