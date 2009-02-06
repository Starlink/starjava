/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2005
 */
package net.ivoa.adql.convert;

import net.ivoa.util.Configuration;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.Properties;
import java.util.Enumeration;
import java.io.StringWriter;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class will transform ADQL/x (XML) into ADQL/s using XSL.  In general,
 * it can transform it into any string format by configuring in the desired
 * stylesheet. <p>
 * 
 * This class uses the Configuration class to specify the stylesheet to use.  
 * It looks for the following parameter tag:
 * <dl>
 *   <dt>stylesheet</dt>
 *   <dd>the file name of the stylesheet load.  If this stylesheet imports 
 *       other stylesheets, these will be loaded relative to the directory
 *       containing the importing sheet.  </dd>
 * </dd>
 * If the stylesheet is specified with a relative path, it will be first 
 * looked for relative to the current directory.  If it is not found there, 
 * it will be searched for as a resource relative to the current classpath.  
 */
public class XSLx2s implements X2STransformer {

    protected String vers = DEFAULT_VERSION;

    /**
     * the XSL Transformer object (with the stylesheet loaded) 
     */
    protected Transformer xslTransformer = null;

    /**
     * the configuration of the transform
     */
    protected Configuration config = null;

    /**
     * the version of ADQL that this transformer is configured to handle
     */
    protected String version = DEFAULT_VERSION;

    /**
     * a configuration parameter name for the top stylesheet to load and 
     * apply for this transform.  This stylesheet may import other stylesheets.
     */
    public static final String STYLESHEET_PARAM = "stylesheet";

    /**
     * a configuration parameter name for the ADQL version that the 
     * configuration applies to
     */
    public static final String VERSION_PARAM = "version";

    /**
     * the version to assume if one was not specified
     */
    public static final String DEFAULT_VERSION = "v1.05";

    /**
     * initialize the transform
     */
    public XSLx2s() { }

    /**
     * configure this transformer.  This needs to be called once before a 
     * call to transform().  
     */
    public void init(Configuration conf) throws TransformerException {
        config = conf;

        version = config.getParameter(VERSION_PARAM);
        if (version == null) version = DEFAULT_VERSION;

        loadStylesheet();
    }

    /**
     * return the version string for the ADQL standard that this transformer
     * understands and can convert.  This is only guaranteed to be correct
     * after init() is called; null is returned before-hand.  
     */
    public String getADQLVersion() { return version; }

    protected void loadStylesheet() throws TransformerException {

        String xslfname = config.getParameter(STYLESHEET_PARAM);

        // We pass the stylesheet file name to the transformer as a URL
        // so that paths of imported stylesheets are interpreted as 
        // relative to the top stylesheet's location rather than relative
        // to the current directory.  
        //
        // We'll look for the stylesheets relative to the current working 
        // directory; if not there, try to load it as a resource from the
        // classpath.
        //
        String xslurl = null;
        File xslfile = new File(xslfname);
        try {
            URL url = (xslfile.exists()) 
                ? xslfile.toURI().toURL()
                : getClass().getClassLoader().getResource(xslfname);
            if (url != null) xslurl = url.toString();
        }
        catch (MalformedURLException ex) {
            // not sure if it's really possible to get here, but anyway...
            xslurl = "file:" + xslfile.toString();
        }
        if (xslurl == null) 
            throw new TransformerException("stylesheet not found: " + 
                                           xslfile);

        // Now load the XSL into memory
        TransformerFactory tFactory = TransformerFactory.newInstance();
        xslTransformer = tFactory.newTransformer(new StreamSource(xslurl));
        
        return;
    }

    /**
     * transform the input ADQL/x
     */
    public String transform(Source adqlx) throws TransformerException {
        if (xslTransformer == null) 
            throw new IllegalStateException("Unitialized X2STransformer");
        
        StringWriter result = new StringWriter();
        xslTransformer.transform(adqlx, new StreamResult(result));
        result.flush();

        return result.toString();
    }

}
