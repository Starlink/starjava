/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: AstroCatConfig.java,v 1.2 2002/08/11 13:37:28 brighton Exp $
 */


package jsky.catalog.astrocat;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import jsky.catalog.AbstractCatalogDirectory;
import jsky.catalog.Catalog;
import jsky.catalog.CatalogDirectory;
import jsky.catalog.CatalogFactory;
import jsky.catalog.FieldDescAdapter;
import jsky.catalog.HTMLQueryResultHandler;
import jsky.catalog.skycat.SkycatConfigFile;
import jsky.util.FileUtil;
import jsky.util.Resources;
import jsky.util.StringUtil;
import jsky.util.gui.DialogUtil;


/**
 * Reads an AstroCat XML catalog description file and stores
 * information about the catalogs defined there. Since catalog
 * config files may point to other catalog config files to form a
 * hierarchy, this class also implements the CatalogDirectory interface.
 *
 * @version $Revision: 1.2 $
 * @author Allan Brighton
 */
public class AstroCatConfig extends AbstractCatalogDirectory {

    /** Top level config file */
    private static AstroCatConfig _configFile;

    /** The URL for the default catalog config file, if set. */
    private static URL _defaultURL = null;


    /**
     * Parse the AstroCat XML catalog config file pointed to by the given URL.
     *
     * @param name the display name for the config file
     * @param url the URL of the config file
     */
    public AstroCatConfig(String name, URL url) {
	super(name);
        setURL(url);
        _load();
    }


    /**
     * Parse the given AstroCat XML file or URL.
     *
     * @param name the display name for the config file
     * @param configFileOrURL the file name or URL of the config file
     */
    public AstroCatConfig(String name, String configFileOrURL) {
        this(name, FileUtil.makeURL(null, configFileOrURL));
    }


    /**
     * Parse the given AstroCat XML file or URL.
     *
     * @param configFileOrURL the file name or URL of the config file
     */
    public AstroCatConfig(String configFileOrURL) {
        this(configFileOrURL, FileUtil.makeURL(null, configFileOrURL));
    }


    /**
     * Parse the AstroCat XML file from the already opened input stream.
     * The URL is passed as a reference.
     *
     * @param url the URL of the config file
     * @param handler used to report HTML errors from the HTTP server
     */
    public AstroCatConfig(URL url, HTMLQueryResultHandler handler) {
	super(new File(url.toString()).getPath());
        setURL(url);
        setHTMLQueryResultHandler(handler);
        _load();
    }



    /**
     * Load an AstroCat XML file from the given URL and store any catalogs
     * found there in the catalogs list.
     */
    private void _load() {
	URL url = getURL();
	if (url == null)
	    return;

	AstroCatXML astroCatXML = new AstroCatXML();
	astroCatXML.parse(url);
	setCatalogs(astroCatXML.getCatalogs());
    }


    /** This method is called once at startup to load the top level catalog directory */
    public static CatalogDirectory getDirectory() {
	return getConfigFile();
    }


    /**
     * If the catalog config file has already been loaded, return an
     * object describing the contents, otherwise search for an AstroCat
     * XML catalog config file, load the contents if found, and return
     * the object for it.
     * <p>
     * First the <em>jsky.catalog.astrocat.config</em> system property is checked.
     * If set, it should be the URL string or file name of the config file.
     * <p>
     * Next, the file ~/.jsky/AstroCat.xml is checked. This file is created 
     * automatically when the user makes any changes in the catalog configuration or
     * plot symbol settings in the table display/configure window.
     * <p>
     * Finally, a default URL s used. It may be set by calling "setConfigFile"
     * and defaults to a config file included in this package (as a resource
     * file: jsky/catalog/astrocat/conf/AstroCat.xml).
     *
     * @return a AstroCatConfig object constructed from the file.
     */
    public static AstroCatConfig getConfigFile() {
        if (_configFile != null)
            return _configFile;

        String[] urls = new String[4];
        int index = 0;

        // check the system property
        String urlStr = System.getProperty("jsky.catalog.astrocat.config");
        if (urlStr != null && urlStr.length() != 0)
            urls[index++] = urlStr;

        // check for ~/.jsky/astrocat/AstroCat.xml
        String home = System.getProperty("user.home");
        String sep = System.getProperty("file.separator");
        String filename = home + sep + ".jsky" + sep + "AstroCat.xml";
        if (new File(filename).exists()) {
            urlStr = "file:" + filename;
            if (!sep.equals("/"))
                urlStr = StringUtil.replace(urlStr, sep, "/");
            urls[index++] = urlStr;
        }

        // use the default resource
        if (_defaultURL == null)
            _defaultURL = Resources.getResource("conf/AstroCat.xml");
        if (_defaultURL == null)
            throw new RuntimeException("Can't find the default catalog config file resource (AstroCat.xml).");
        urls[index++] = _defaultURL.toString();

        // use the first URL found for the main catalog list
        _configFile = new AstroCatConfig(urls[0]);

        // include the other URLs as catalog directories
        for (int i = 1; i < index; i++) {
            if (urls[i] != null) {
                _configFile.addCatalogDirectory(urls[i]);
	    }
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
	if (filename.endsWith(".xml")) {
	    AstroCatConfig cf = new AstroCatConfig(url, getHTMLQueryResultHandler());
	    return cf;
	}

	// for compatibility, also allow Skycat config files here
	if (filename.endsWith(".cfg")) {
	    SkycatConfigFile cf = new SkycatConfigFile(url, getHTMLQueryResultHandler());
	    return cf;
	}
	throw new RuntimeException("Expected an AstroCat XML file, or a Skycat style .cfg file");
    }


    /**
     * Set the URL to use for the default catalog config file.
     *
     * @param url points to the AstroCat XML catalog config file
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

	try {
	    URL url = new URL(urlStr);
	    AstroCatConfig cat = new AstroCatConfig(urlStr, url);
	    addCatalog(cat);
	}
	catch(MalformedURLException e) {
	    e.printStackTrace();
	}
    }


    /**
     * Return a list of name servers (Catalogs with serv_type
     * equal to "namesvr") to use to resolve astronomical object names.
     */
    public List getNameServers() {
        List l = super.getNameServers();
	if (l.size() != 0) {
	    return l;
	}
	_addDefaultNameServers();
        return CatalogFactory.getCatalogsByType(Catalog.NAME_SERVER);
    }

    /**
     * Register the default name servers (SIMBAD and NED) to use to resolve astronomical object names.
     */
    private void _addDefaultNameServers() {
	AstroCatalog cat = new AstroCatalog();
	cat.setId("simbad_ns@eso");
	cat.setName("SIMBAD Names");
	cat.setType(Catalog.NAME_SERVER);
	cat.setHost("archive.eso.org");
	cat.setURLPath("/skycat/servers/sim-server");
	FieldDescAdapter[] params = new FieldDescAdapter[1];
	params[0] = new FieldDescAdapter();
	FieldDescAdapter param = params[0];
	param.setName("Object Name");
	param.setId("o");
	param.setDescription("Enter the name of the object (star, galaxy)");
	cat.setParams(params);
	CatalogFactory.registerCatalog(cat, false);

	cat = new AstroCatalog();
	cat.setId("ned@eso");
	cat.setName("NED Names");
	cat.setType(Catalog.NAME_SERVER);
	cat.setHost("archive.eso.org");
	cat.setURLPath("/skycat/servers/ned-server");
	params = new FieldDescAdapter[1];
	params[0] = new FieldDescAdapter();
	param = params[0];
	param.setName("Object Name");
	param.setId("o");
	param.setDescription("Enter the name of the object (star, galaxy)");
	cat.setParams(params);
	CatalogFactory.registerCatalog(cat, false);
    }


    /**
     * Save the catalog list in the default location (~/.jsky/AstroCat.xml) 
     */
    public void save() {
        String home = System.getProperty("user.home");
        String sep = System.getProperty("file.separator");
        String fileName = home + sep + ".jsky" + sep + "AstroCat.xml";
        try {
            save(fileName);
        }
        catch (Exception e) {
            DialogUtil.error(e);
        }
    }


    /**
     * Save the catalog list to the given file (in the AstroCat XML format).
     * Since the user can only edit the catalog list and the symbol definitions, only
     * that information is actually saved. The rest of the details are still read
     * from the original, default XML file.
     * 
     * @param filename the file name in which to store the catalog information
     */
    public void save(String filename) {
        File file = new File(filename + ".tmp");
        File dir = file.getParentFile();
        if (!dir.isDirectory())
            dir.mkdirs();

	try {
	    AstroCatXML.save(file, getCatalogs());
	}
	catch(Exception e) {
            file.delete();
            throw new RuntimeException(e);
	}

        File newFile = new File(filename);
        newFile.delete(); // needed under Windows!
        if (!file.renameTo(newFile))
            DialogUtil.error("Rename " + file + " to " + filename + " failed");
    }



    /**
     * Test cases
     */
    public static void main(String[] args) {
        AstroCatConfig configFile = AstroCatConfig.getConfigFile();
        String catalogName = "Guide Star Catalog at ESO";
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
