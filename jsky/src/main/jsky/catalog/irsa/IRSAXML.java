// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: IRSAXML.java,v 1.2 2002/08/20 09:57:58 brighton Exp $

package jsky.catalog.irsa;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Vector;

import jsky.util.Resources;
import jsky.util.SaxParserUtil;

import org.xml.sax.Attributes;


/**
 * Parses an IRSA Holdings XML file and saves the catalog definitions found there.
 * See http://irsa.ipac.caltech.edu/.
 *
 * @version $Revision: 1.2 $
 * @author Allan Brighton
 */
public class IRSAXML extends SaxParserUtil {

    // list of IRSACatalog objects, one for each catalog entry in the XML file
    private List _catalogs;

    // These correspond to the fields in the XML file
    private IRSACatalog _catalog;  // the current catalog object
    private String _desc;          // catalog title
    private String _server;        // server id to pass to cgi-bin script (not hostname)
    private String _database;      // database id
    private String _catname;       // catalog id
    private String _ddname;        // Data dictionary id


    /**
     * Default constructor. Call parse(urlStr) to do the actual parsing.
     */
    public IRSAXML() {
    }

    /** Return the list of IRSACatalog definitions found in the XML file after parsing */
    public List getCatalogs() {return _catalogs;}


    // -- these methods are called by reflection from the base class --
    //    (which is why they must be declared public) 


    // called for the <Holdings> start tag
    public void _HoldingsStart(Attributes attrs) {
        _catalogs = new Vector();
    }

    // called for the </Holdings> end tag
    public void _HoldingsEnd() {
    }


    // called for the <catalog> start tag
    public void _catalogStart(Attributes attrs) {
    }

    // called for the </catalog> end tag
    public void _catalogEnd() {
	if (_desc == null || _server == null || _database == null || _catname == null || _ddname == null) {
	    System.out.println("Warning: Missing IRSA fields in XML file: " +  getURL());
	}
	else {
	    _catalog = new IRSACatalog(_desc, _server, _database, _catname, _ddname, getURL());
	    _catalogs.add(_catalog);
	}
	_desc = _server = _database = _catname = _ddname = null;
    }



    // called for the <desc> start tag
    public void _descStart(Attributes attrs) {
    }
    // called for the </desc> end tag
    public void _descEnd() {
	_desc = getCData();
    }


    // called for the <server> start tag
    public void _serverStart(Attributes attrs) {
    }
    // called for the </server> end tag
    public void _serverEnd() {
	_server = getCData();
    }


    // called for the <database> start tag
    public void _databaseStart(Attributes attrs) {
    }
    // called for the </database> end tag
    public void _databaseEnd() {
	_database = getCData();
    }


    // called for the <catname> start tag
    public void _catnameStart(Attributes attrs) {
    }
    // called for the </catname> end tag
    public void _catnameEnd() {
	_catname = getCData();
    }


    // called for the <ddname> start tag
    public void _ddnameStart(Attributes attrs) {
    }
    // called for the </ddname> end tag
    public void _ddnameEnd() {
	_ddname = getCData();
    }


    /** 
     * Save the given list of catalog descriptions as a set of IRSA XML files in the
     * given directory.
     */
    public void save(File dir, List catalogs) {
        String sep = System.getProperty("file.separator");
        File file = new File(dir.getPath() + sep + "nph-catlist.xml");

	System.out.println("XXX save to XML not implemented yet XXX");
    }



    // called for the <count> start tag
    public void _countStart(Attributes attrs) {
    }
    // called for the </count> end tag
    public void _countEnd() {
    }



    // called for the <ERROR> start tag
    public void _ERRORStart(Attributes attrs) {
    }
    // called for the </ERROR> end tag
    public void _ERROREnd() {
	throw new RuntimeException(getCData() + ": while reading : " + getURL());
    }



    /**
     * Test cases
     */
    public static void main(String[] args) {
        try {
            URL url = Resources.getResource("conf/nph-catlist.xml");
            IRSAXML irsaXML = new IRSAXML();
            irsaXML.parse(url);
	    System.out.println("Parsed conf/nph-catlist.xml and found " + irsaXML.getCatalogs().size() + " catalogs");
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
	System.out.println("Test passed");
    }
}
