/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2005, 2006
 */
package net.ivoa.adql.convert;

import net.ivoa.util.Configuration;

import org.w3c.dom.Element;

import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.namespace.QName;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * a general interface for converting ADQL/s to ADQL/x.  
 */
public class DOMParser2XML implements S2XTransformer {

    protected Transformer identTransformer = null;
    protected short nsmode = ADQLs2DOMParser.MODE_ALWAYS_QUALIFIED;
    protected int indent = 1;
    protected Configuration config = null;
    private Constructor parserConstructor = null;
    protected String version = null;

    /**
     * a configuration parameter name for the amount of indentation to 
     * use in the output XML.  If the value is not an integer, it is ignored.  
     * If the value is less than zero, then no indentation--including 
     * carriage returns--will be used.
     */
    public static final String PRETTY_INDENT_PARAM = "prettyIndent";

    /**
     * a configuration parameter name for the namespace qualification mode
     * to use.  The value must be one of:
     * <pre>
     *   default_ns          use xmlns= to minimize the use of element prefixes
     *   always_qualified    qualify every element with a prefix
     * </pre>
     */
    public static final String NS_MODE_PARAM = "nsMode";

    /**
     * a configuration parameter name for the ADQLs2DOMParser class to 
     * use.  If this is not provided, a default is determined based on 
     * the configured version.
     **/
    public static final String PARSER_CLASS_PARAM = "parser";

    /**
     * a configuration parameter name for the version of ADQL that this 
     * transformer is being configured for.
     */
    public static final String VERSION_PARAM = "version";

    /**
     * the version to assume if one was not specified
     */
    public static final String DEFAULT_VERSION = "v1.0";

    private static Hashtable modes = new Hashtable();
    static {
        modes.put("default_ns", new Short(ADQLs2DOMParser.MODE_DEFAULT_NS));
        modes.put("always_qualified", 
                  new Short(ADQLs2DOMParser.MODE_ALWAYS_QUALIFIED));
    }

    // default parser classes; these can be overridden in the config file
    private static Properties parsers = new Properties();
    static {
        parsers.setProperty("v0.7.4", 
                            "net.ivoa.adql.convert.parser.v0_7_4.ADQLParser");
        parsers.setProperty("v1.0", 
                            "net.ivoa.adql.convert.parser.v1_0.ADQLParser");
    }

    /**
     * initialize the transform
     */
    public DOMParser2XML() { }

    /**
     * configure this transformer.  
     */
    public void init(Configuration conf) throws TransformerException { 
        config = conf;

        version = config.getParameter(VERSION_PARAM);
        if (version == null) version = DEFAULT_VERSION;

        String parsercl = config.getParameter(PARSER_CLASS_PARAM);
        if (parsercl == null) parsercl = parsers.getProperty(version);
        if (parsercl == null) 
            throw new TransformerException("Unable to determine proper " + 
                                           "parser class for version " + 
                                           version);
        try {
            Class cl = Class.forName(parsercl);
            if (! ADQLs2DOMParser.class.isAssignableFrom(cl)) {
               throw new TransformerException(parsercl + 
                                              ": not an S2XTransformer class");
            }
            parserConstructor = findConstructor(cl);
        }
        catch (ClassNotFoundException e) {
            throw new TransformerException(parsercl + ": class not found");
        }

        String indentVal = config.getParameter(PRETTY_INDENT_PARAM);
        if (indentVal != null) {
            try {
                indent = Integer.parseInt(indentVal);
            }
            catch (NumberFormatException e) { }
        }
        String mode = config.getParameter(NS_MODE_PARAM);
        if (mode != null) {
            Short val = (Short) modes.get(mode.toLowerCase());
            if (val != null) nsmode = val.shortValue();
        }

        TransformerFactory tFactory = TransformerFactory.newInstance();
        identTransformer = tFactory.newTransformer();
    }

    /**
     * return the version string for the ADQL standard that this transformer
     * understands and can convert.  This is only guaranteed to be correct
     * after init() is called; null is returned before-hand.  
     */
    public String getADQLVersion() { return version; }

    private Constructor findConstructor(Class cl) throws TransformerException {
        try {
            Constructor out = cl.getConstructor(new Class[] { Reader.class });
            return out;
        }
        catch (NoSuchMethodException ex) {
            throw new TransformerException(ex);
        }
    }

    /**
     * transform the input ADQL/x
     * @param adqls   an ADQL/s string
     * @param out     the XML result to write into (either a SAXResult, 
     *                  a DOMResult, or a StreamResult).
     * @throws IllegalStateException if the the transformer has not been 
     *   properly configured
     */
    public void transform(String adqls, Result out) 
         throws TransformerException, IllegalStateException 
    {
        if (identTransformer == null) 
            throw new IllegalStateException("DOMParser2XML tranformer not " + 
                                            "initialized (call init())");

        ADQLs2DOMParser parser = createParser(new StringReader(adqls));
        parser.setIndent(indent);
        parser.setNSMode(nsmode);

        Element domroot = parser.parseSelect();
        parser.indent();
        
        DOMSource source = new DOMSource(domroot.getOwnerDocument());
        identTransformer.transform(source, out);
    }

    protected ADQLs2DOMParser createParser(Reader input) 
        throws TransformerException
    {
        if (parserConstructor == null) 
            throw new IllegalStateException("DOMParser2XML's parser not " + 
                                            "configured (call init())");

        try {
            return ((ADQLs2DOMParser) 
                    parserConstructor.newInstance(new Object[] { input }));
        }
        catch (InstantiationException ex) {
            throw new TransformerException(ex);
        }
        catch (InvocationTargetException ex) {
            throw new TransformerException(ex.getCause());
        }
        catch (IllegalArgumentException ex) {
            throw new InternalError("programmer error: bad argument to " +
                                    "ADQLs2DOMParser constructor");
        }
        catch (IllegalAccessException ex) {
            throw new InternalError("programmer error: non-public " +
                                    "ADQLs2DOMParser constructor");
        }
    }
}
