/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: IRSAConfig.java,v 1.3 2002/08/20 09:57:58 brighton Exp $
 */


package jsky.catalog.irsa;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import jsky.catalog.AbstractCatalogDirectory;
import jsky.catalog.Catalog;
import jsky.catalog.CatalogDirectory;
import jsky.catalog.CatalogFactory;
import jsky.catalog.HTMLQueryResultHandler;
import jsky.util.FileUtil;
import jsky.util.Resources;
import jsky.util.StringUtil;
import jsky.util.gui.DialogUtil;


/**
 * Reads an 
 * <a href="http://irsa.ipac.caltech.edu/cgi-bin/Oasis/CatList/nph-catlist">IRSA XML</a>
 * catalog description (Holdings) file and stores information about the catalogs defined there. 
 *
 * @version $Revision: 1.3 $
 * @author Allan Brighton
 */
public class IRSAConfig extends AbstractCatalogDirectory {

    /** Top level config file */
    private static IRSAConfig _configFile;

    /** The URL for the default catalog config file, if set. */
    private static URL _defaultURL = null;


    /**
     * Parse the IRSA XML catalog config file pointed to by the given URL.
     *
     * @param name the display name for the config file
     * @param url the URL of the config file
     */
    public IRSAConfig(String name, URL url) {
	super(name);
        setURL(url);
        _load();
    }


    /**
     * Parse the given IRSA XML file or URL.
     *
     * @param name the display name for the config file
     * @param configFileOrURL the file name or URL of the config file
     */
    public IRSAConfig(String name, String configFileOrURL) {
        this(name, FileUtil.makeURL(null, configFileOrURL));
    }


    /**
     * Parse the given IRSA XML file or URL.
     *
     * @param configFileOrURL the file name or URL of the config file
     */
    public IRSAConfig(String configFileOrURL) {
        this(configFileOrURL, FileUtil.makeURL(null, configFileOrURL));
    }


    /**
     * Parse the IRSA XML file from the already opened input stream.
     * The URL is passed as a reference.
     *
     * @param url the URL of the config file
     * @param handler used to report HTML errors from the HTTP server
     */
    public IRSAConfig(URL url, HTMLQueryResultHandler handler) {
	super(new File(url.toString()).getPath());
        setURL(url);
        setHTMLQueryResultHandler(handler);
        _load();
    }


    /**
     * Load an IRSA XML file from the given URL and store any catalogs
     * found there in the catalogs list.
     */
    private void _load() {
	URL url = getURL();
	if (url == null)
	    return;

	IRSAXML irsaXML = new IRSAXML();
	irsaXML.parse(url);
	setCatalogs(irsaXML.getCatalogs());
    }


    /** This method is called once at startup to load the top level catalog directory */
    public static CatalogDirectory getDirectory() {
	return getConfigFile();
    }


    /**
     * If the catalog config file has already been loaded, return an
     * object describing the contents, otherwise search for an IRSA
     * XML catalog config file, load the contents if found, and return
     * the object for it.
     * <p>
     * First the <em>jsky.catalog.irsa.config</em> system property is checked.
     * If set, it should be the URL string or file name of the config file.
     * <p>
     * Next, the file ~/.jsky/nph-catlist.xml is checked. This file is created 
     * automatically when the user makes any changes in the catalog configuration or
     * plot symbol settings in the table display/configure window.
     * <p>
     * Finally, a default URL s used. It may be set by calling "setConfigFile"
     * and defaults to a config file included in this package (as a resource
     * file: jsky/catalog/irsa/conf/nph-catlist.xml).
     *
     * @return a IRSAConfig object constructed from the file.
     */
    public static IRSAConfig getConfigFile() {
        if (_configFile != null)
            return _configFile;

        String[] urls = new String[4];
        int index = 0;

        // check the system property
        String urlStr = System.getProperty("jsky.catalog.irsa.config");
        if (urlStr != null && urlStr.length() != 0)
            urls[index++] = urlStr;

        // check for ~/.jsky/irsa/nph-catlist.xml
        String home = System.getProperty("user.home");
        String sep = System.getProperty("file.separator");
        String filename = home + sep + ".jsky" + sep + "irsa" + sep + "nph-catlist.xml";
        if (new File(filename).exists()) {
            urlStr = "file:" + filename;
            if (!sep.equals("/"))
                urlStr = StringUtil.replace(urlStr, sep, "/");
            urls[index++] = urlStr;
        }

        // use the default resource
        if (_defaultURL == null)
            _defaultURL = Resources.getResource("conf/nph-catlist.xml");
        if (_defaultURL == null)
            throw new RuntimeException("Can't find the default catalog config file resource (nph-catlist.xml).");
        urls[index++] = _defaultURL.toString();

        // use the first URL found for the main catalog list
        _configFile = new IRSAConfig(urls[0]);

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
	IRSAConfig cf = new IRSAConfig(url, getHTMLQueryResultHandler());
	return cf;
    }


    /**
     * Set the URL to use for the default catalog config file.
     *
     * @param url points to the IRSA XML catalog config file
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

	IRSAConfig catDir = new IRSAConfig(urlStr);
	addCatalog(catDir);
    }


    /**
     * Save the catalog descriptions to a set of IRSA XML files under ~/.jsky/irsa/. 
     */
    public void save() {
        String home = System.getProperty("user.home");
        String sep = System.getProperty("file.separator");
        String filename = home + sep + ".jsky" + sep + "irsa" + sep;
        try {
            save(filename);
        }
        catch (Exception e) {
            DialogUtil.error(e);
        }
    }


    /**
     * Save the catalog descriptions to a set of IRSA XML files under the given directory.
     * 
     * @param dirName the directory name in which to store the XML files (must end with a file separator)
     */
    public void save(String dirName) throws IOException {
	File dir = new File(dirName);
	if (!dir.isDirectory())
            dir.mkdirs();

	IRSAXML irsaXML = new IRSAXML();
	irsaXML.save(dir, getCatalogs());
    }


    /**
     * Test cases
     */
    public static void main(String[] args) {
        IRSAConfig configFile = IRSAConfig.getConfigFile();
        String catalogName = "2MASS Second Incremental Release Point Source Catalog (PSC)";
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
