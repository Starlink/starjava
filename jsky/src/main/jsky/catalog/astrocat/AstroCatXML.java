// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: AstroCatXML.java,v 1.1 2002/08/04 21:48:50 brighton Exp $

package jsky.catalog.astrocat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import jsky.catalog.Catalog;
import jsky.catalog.CatalogDirectory;
import jsky.catalog.CatalogFactory;
import jsky.catalog.FieldDescAdapter;
import jsky.catalog.PlotableCatalog;
import jsky.catalog.TablePlotSymbol;
import jsky.util.NameValue;
import jsky.util.Resources;
import jsky.util.SaxParserUtil;
import jsky.util.TclUtil;

import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


/**
 * Parses an XML stream conforming to the document type
 * definition "AstroCat.dtd" and saves the definitions found there.
 *
 * @version $Revision: 1.1 $
 * @author Allan Brighton
 */
public final class AstroCatXML extends SaxParserUtil {

    // used for XML output
    private static String VERSION = "0.1";
    private static final String NAMESPACE = "";
    private static final String LOCAL_NAME = "";
    private static final String CDATA = "CDATA";
    private static final String PUBLIC_ID = "-//JSky//DTD for Catalog Access//EN";
    private static final String SYSTEM_ID = "AstroCat.dtd";


    // list of AstroCatalog objects
    private Vector _catalogs;

    // the current catalog object
    private AstroCatalog _catalog;

    // list of FieldDescAdapter objects
    private List _params;

    // the current param object
    private FieldDescAdapter _param;

    // list of NameValue objects representing the parameter options
    private List _options;

    // the current parameter option object
    private NameValue _option;

    // list of TablePlotSymbol objects
    private List _symbols;

    // the current plot symbol object
    private TablePlotSymbol _symbol;

    /**
     * Default constructor. Call parse(urlStr) to do the actual parsing.
     */
    public AstroCatXML() {
    }

    /** Return the list of AstroCatalog definitions found in the XML file after parsing */
    public List getCatalogs() {return _catalogs;}



    // -- these methods are called by reflection from the base class --
    //    (which is why they must be declared public) 



    // called for the <catalogs> start tag
    public void _catalogsStart(Attributes attrs) {
	_catalogs = new Vector();
    }

    // called for the </catalogs> end tag
    public void _catalogsEnd() {
	// Resolve any references to existing catalogs in other directories.
	// A reference in this case is a catalog entry with a name, but no path.
	// It is then replaced by a catalog with that name registered with the 
	// CatalogFactory class.
	// If the abbreviated catalog description contains plot symbol descriptions,
	// they are copied to the original catalog object as well.
	int n = _catalogs.size();
	for(int i = n-1; i >= 0; i--) {
	    Catalog c = (Catalog)_catalogs.get(i);
	    if (c instanceof AstroCatalog) {
		AstroCatalog catalog = (AstroCatalog)c;
		String path = catalog.getURLPath();
		if (path == null || path.length() == 0) {
		    Catalog cat = CatalogFactory.getCatalogByName(catalog.getName());
		    if (cat == null) {
			_catalogs.remove(i); // remove dead links
			System.out.println("Warning: catalog not found in any config files: '" + catalog.getName() 
					   + "', referenced in " + getURL());
		    }
		    else if (_catalogs.contains(cat)) {
			_catalogs.remove(i); // remove duplicate links
			System.out.println("Warning: duplicate catalog: '" + catalog.getName() + "' in " + getURL());
		    }
		    else {
			_catalogs.setElementAt(cat, i);
			
			// copy plot symbol info, if defined
			if (c instanceof PlotableCatalog && cat instanceof PlotableCatalog) {
			    PlotableCatalog pc = (PlotableCatalog)c;
			    if (pc.getNumSymbols() != 0) {
				PlotableCatalog pcat = (PlotableCatalog)cat;
				pcat.setSymbols(pc.getSymbols());
				pcat.setSymbolsEdited(true);
			    }
			}
		    }
		}
	    }
	}
    }


    // called for the <catalog> start tag
    public void _catalogStart(Attributes attrs) {
	if (_catalogs == null)
	    _catalogs = new Vector();

        if (attrs != null) {
            _catalog = new AstroCatalog();
	    _catalog.setURL(getURL());
            int n = attrs.getLength();
            for (int i = 0; i < n; i++) {
                String name = attrs.getQName(i);
                String value = attrs.getValue(i);
                if (name.equals("id"))
                    _catalog.setId(value);
                else if (name.equals("name"))
                    _catalog.setName(value);
                else if (name.equals("type"))
                    _catalog.setType(value);
                else if (name.equals("protocol"))
                    _catalog.setProtocol(value);
                else if (name.equals("host"))
                    _catalog.setHost(value);
                else if (name.equals("port"))
                    _catalog.setPort(Integer.parseInt(value));
                else if (name.equals("path"))
                    _catalog.setURLPath(value);
                else if (name.equals("class"))
                    _catalog.setHandlerClass(value);
            }
        }
    }

    // called for the </catalog> end tag
    public void _catalogEnd() {
        if (_catalog != null) {
	    if (_catalog.getName() == null) {
		System.out.println("Warning: no name defined for catalog in " + getURL());
	    }
	    else {
		if (Catalog.DIRECTORY.equals(_catalog.getType())) {
		    // Handle catalog directory entry
		    _catalogs.add(_getCatalogDirectory(_catalog));
		}
		else {
		    // with path: add a new catalog
		    _catalogs.add(_catalog);
		}
	    }
            _catalog = null;
        }
    }


    // called for the <params> start tag
    public void _paramsStart(Attributes attrs) {
        _params = new ArrayList();
    }

    // called for the </params> end tag
    public void _paramsEnd() {
        if (_catalog != null && _params.size() != 0) {
            FieldDescAdapter[] params = new FieldDescAdapter[_params.size()];
            _params.toArray(params);
            _catalog.setParams(params);
        }
        _params = null;
    }


    // called for the <param> start tag
    public void _paramStart(Attributes attrs) {
        if (attrs != null) {
            _param = new FieldDescAdapter();
            int n = attrs.getLength();
            for (int i = 0; i < n; i++) {
                String name = attrs.getQName(i);
                String value = attrs.getValue(i);
                if (name.equals("id"))
                    _param.setId(value);
                else if (name.equals("name"))
                    _param.setName(value);
                else if (name.equals("description"))
                    _param.setDescription(value);
                else if (name.equals("value")) 
                    _param.setDefaultValue(value);
                else if (name.equals("type"))
                    _param.setType(value);
                else if (name.equals("units"))
                    _param.setUnits(value);
                else if (name.equals("format"))
                    _param.setFormat(value);
            }
        }
    }

    // called for the </param> end tag
    public void _paramEnd() {
        _params.add(_param);
    }


    // called for the <options> start tag
    public void _optionsStart(Attributes attrs) {
        _options = new ArrayList();
    }

    // called for the </options> end tag
    public void _optionsEnd() {
        if (_param != null && _options.size() != 0) {
            NameValue[] options = new NameValue[_options.size()];
            _options.toArray(options);
            _param.setOptions(options);
        }
        _options = null;
    }


    // called for the <option> start tag
    public void _optionStart(Attributes attrs) {
        if (attrs != null) {
            _option = new NameValue();
            int n = attrs.getLength();
            for (int i = 0; i < n; i++) {
                String name = attrs.getQName(i);
                String value = attrs.getValue(i);
                if (name.equals("name"))
                    _option.setName(value);
                else if (name.equals("value"))
                    _option.setValue(value);
            }
        }
    }

    // called for the </option> end tag
    public void _optionEnd() {
        _options.add(_option);
    }




    // called for the <symbols> start tag
    public void _symbolsStart(Attributes attrs) {
        _symbols = new ArrayList();
    }

    // called for the </symbols> end tag
    public void _symbolsEnd() {
        if (_catalog != null && _symbols.size() != 0) {
            TablePlotSymbol[] symbols = new TablePlotSymbol[_symbols.size()];
            _symbols.toArray(symbols);
            _catalog.setSymbols(symbols);
        }
        _symbols = null;
    }


    // called for the <symbol> start tag
    public void _symbolStart(Attributes attrs) {
        if (attrs != null) {
            _symbol = new TablePlotSymbol();
            int n = attrs.getLength();
            for (int i = 0; i < n; i++) {
                String name = attrs.getQName(i);
                String value = attrs.getValue(i);
                if (name.equals("name"))
                    _symbol.setName(value);
                else if (name.equals("description"))
                    _symbol.setDescription(value);
                else if (name.equals("raCol"))
                    _symbol.setRaCol(Integer.parseInt(value));
                else if (name.equals("decCol"))
                    _symbol.setDecCol(Integer.parseInt(value));
                else if (name.equals("equinox"))
                    _symbol.setEquinox(_getEquinox(value));
                else if (name.equals("columnsUsed"))
                    _symbol.setColNames(TclUtil.splitList(value));
                else if (name.equals("shape"))
                    _symbol.setShapeName(value);
                else if (name.equals("color"))
                    _symbol.setFg(value);
                else if (name.equals("condition"))
                    _symbol.setCond(value);
                else if (name.equals("ratio"))
                    _symbol.setRatio(value);
                else if (name.equals("angle"))
                    _symbol.setAngle(value);
                else if (name.equals("label"))
                    _symbol.setLabel(value);
                else if (name.equals("size"))
                    _symbol.setSize(value);
                else if (name.equals("units"))
                    _symbol.setUnits(value);
            }
        }
    }

    // called for the </symbol> end tag
    public void _symbolEnd() {
        _symbols.add(_symbol);
    }


    // Return the value of the given equinox string
    private double _getEquinox(String s) {
	if (s == null || s.length() == 0 || s.equals("J2000"))
	    return 2000.;
	if (s.equals("B1950"))
	    return 1950.;
	return Double.parseDouble(s);
    }


    // Return a CatalogDirectory object based on the settings in the given catalog object
    private CatalogDirectory _getCatalogDirectory(AstroCatalog cat) {
	String className = cat.getHandlerClass();
	Class c = AstroCatConfig.class;
	try {
	    if (className != null) 
		c = this.getClass().forName(className);
	    String path = cat.getURLPath();
	    if (path == null || path.length() == 0) {
		Object o = c.getMethod("getDirectory", null).invoke(null, null);
		CatalogDirectory catDir = (CatalogDirectory)o;
		catDir.setName(cat.getName());
		return catDir;
	    }
	    
	    // handle directory with path
	    // XXX
	    
	}
	catch (InvocationTargetException e1) {
	    Throwable t = e1.getTargetException();
	    RuntimeException ex = new RuntimeException("Error calling " + className + ".getDirectory(): " + t);
	    ex.setStackTrace(t.getStackTrace());
	    throw ex;
	}
	catch (Exception e2) {
	    throw new RuntimeException("Error calling " + className + ".getDirectory(): " + e2);
	}

	throw new RuntimeException("Could not load catalog directory entry: " + cat.getName());
    }




    // -- output methods --


    /** 
     * Save the given list of catalog descriptions in the given file.
     * Since the user can only edit the catalog list and the symbol definitions, only
     * that information is actually saved. The rest of the details are still read
     * from the original, default XML file.
     */
    public static void save(File file, List catalogs) throws SAXException, IOException {
        String sep = System.getProperty("file.separator");
        String filename = file.getPath();
	FileOutputStream out = null;
	XMLWriter xml = null;
	try {
	    out = new FileOutputStream(filename);
	    
	    OutputFormat format = OutputFormat.createPrettyPrint();
	    format.setExpandEmptyElements(false);
	    xml = new XMLWriter(out, format);
	    
	    xml.startDocument();

	    xml.startDTD("catalogs", PUBLIC_ID, SYSTEM_ID);
	    xml.endDTD();

	    _saveCatalogs(xml, catalogs);

	    xml.endDocument();
	}
	finally {
	    try {
		xml.flush();
		out.close();
	    }
	    catch(IOException ex) {
	    }
	}
    }

    // output the <catalogs> element
    private static void _saveCatalogs(XMLWriter xml, List catalogs) throws SAXException {
	AttributesImpl attrs = new AttributesImpl();
	_addAttr(attrs, "version", VERSION);
	xml.startElement(NAMESPACE, LOCAL_NAME, "catalogs", attrs);

	Iterator it = catalogs.listIterator();
	while(it.hasNext()) {
	    Catalog catalog = (Catalog)it.next();
	    _saveCatalog(xml, catalog);
	}

 	xml.endElement(NAMESPACE, LOCAL_NAME, "catalogs");
    }


    // Output a <catalog> element.
    // This version only writes an abbreviated entry, referring to the detailed XML file 
    // description with the path attribute (for AstroCatalogs) or just the name, for
    // other catalog types. Catalog directory entries refer to a class name for the
    // class that searches for the config file.
    private static void _saveCatalog(XMLWriter xml, Catalog catalog) throws SAXException {
	if (catalog instanceof AstroCatConfig) {
	    // ignore automatically added link to default config file
	    // (it would cause an endless loop anyway)
	    return;  
	}

	AttributesImpl attrs = new AttributesImpl();
	_addAttr(attrs, "name", catalog.getName());
	_addAttr(attrs, "type", catalog.getType());

	AstroCatalog cat = null;
	if (catalog instanceof AstroCatalog)  {
	    // For AstroCatalogs, include the "path" attribute, which points to the XML
	    // file with the detailed catalog description, which is searched for in the
	    // same directory as the file being parsed, or in the default resource location.
	    cat = (AstroCatalog)catalog;
	    URL url = cat.getURL();
	    if (url != null) {
		String fileName = url.getFile();
		if (fileName != null) {
		    File file = new File(fileName);
		    String relPath = file.getName();
		    _addAttr(attrs, "path", relPath);
		}
	    }
	}
	else if (catalog instanceof CatalogDirectory) {
	    // For catalog directories, include the "class" attribute, which indicates
	    // which class is responsible for finding and parsing the catalog list
	    _addAttr(attrs, "class", catalog.getClass().getName());
	}

	xml.startElement(NAMESPACE, LOCAL_NAME, "catalog", attrs);

	if (catalog instanceof PlotableCatalog)  {
	    // Include plot symbol information for catalogs that have it
	    PlotableCatalog pcat = (PlotableCatalog)catalog;
	    if (pcat.isSymbolsEdited())
		_saveSymbols(xml, pcat.getSymbols());
	}
	
 	xml.endElement(NAMESPACE, LOCAL_NAME, "catalog");
    }


    // Output a <symbols> element.
    private static void _saveSymbols(XMLWriter xml, TablePlotSymbol[] symbols) throws SAXException {
	AttributesImpl attrs = new AttributesImpl();
	xml.startElement(NAMESPACE, LOCAL_NAME, "symbols", attrs);

	for(int i = 0; i < symbols.length; i++) {
	    _saveSymbol(xml, symbols[i]);
	}

 	xml.endElement(NAMESPACE, LOCAL_NAME, "symbols");
    }


    // Output a <symbol> element.
    private static void _saveSymbol(XMLWriter xml, TablePlotSymbol symbol) throws SAXException {
	AttributesImpl attrs = new AttributesImpl();
	_addAttr(attrs, "name", symbol.getName());
	_addAttr(attrs, "description", symbol.getDescription());
	_addAttr(attrs, "columnsUsed", TclUtil.makeList(symbol.getColNames()));
	_addAttr(attrs, "shape", symbol.getShapeName());
	_addAttr(attrs, "color", symbol.getColorName(symbol.getFg()));
	_addAttr(attrs, "condition", symbol.getCond());
	_addAttr(attrs, "ratio", symbol.getRatio());
	_addAttr(attrs, "angle", symbol.getAngle());
	_addAttr(attrs, "label", symbol.getLabel());
	_addAttr(attrs, "size", symbol.getSize());
	_addAttr(attrs, "units", symbol.getUnits());

	int raCol = symbol.getRaCol(), decCol = symbol.getDecCol();
	double equinox = symbol.getEquinox();

	if (raCol != symbol.DEFAULT_RA_COL)
	    _addAttr(attrs, "raCol", String.valueOf(raCol));

	if (decCol != symbol.DEFAULT_DEC_COL)
	    _addAttr(attrs, "decCol", String.valueOf(decCol));

	if (equinox != symbol.DEFAULT_EQUINOX)
	    _addAttr(attrs, "equinox", String.valueOf(equinox));

	xml.startElement(NAMESPACE, LOCAL_NAME, "symbol", attrs);
 	xml.endElement(NAMESPACE, LOCAL_NAME, "symbol");
    }


    // Add an attribute name/value pair to the given AttributesImpl object, if the value
    // is not null
    private static void _addAttr(AttributesImpl attrs, String name, String value) {
	if (value != null)
	    attrs.addAttribute(NAMESPACE, LOCAL_NAME, name, CDATA, value);
    }


    /**
     * Test cases
     */
    public static void main(String[] args) {
        try {
            URL url = Resources.getResource("conf/AstroCat.xml");
            AstroCatXML astroCatXML = new AstroCatXML();
            astroCatXML.parse(url);
	    System.out.println("Parsed conf/AstroCat.xml and found " + astroCatXML.getCatalogs().size() + " catalogs");
	    
	    // test writing the catalog info in ./test/astrocat/
	    String sep = System.getProperty("file.separator");
	    File file = new File("test" + sep + "test.xml");
	    File dir = file.getParentFile();
	    if (!dir.isDirectory())
		dir.mkdirs();
	    astroCatXML.save(file, astroCatXML.getCatalogs());
	    System.out.println("Saved results to " + file);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
	System.out.println("Test passed");
    }
}
