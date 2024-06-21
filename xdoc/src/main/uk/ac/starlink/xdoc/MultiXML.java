package uk.ac.starlink.xdoc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Utility class which takes a single XML file and writes it out to
 * a number of new XML/HTML files.
 * Each <code>&lt;filesection&gt;</code> element in the file is written
 * to a new file, with the name taken from the filesection's <code>file</code>
 * attribute.
 * Anything outside of a filesection is ignored.  Filesection elements should
 * not be nested.
 * <p>
 * Other attributes of the filesection element are used
 * to set output properties for the XML transformation:
 * <dl>
 * <dt>file
 * <dd>Name for the output file.
 * <dt>method
 * <dd>type of output ("xml", "html" or "text")
 * <dt>doctype-system
 * <dd>System ID for the HTML/XML declaration
 * <dt>doctype-public
 * <dd>Public ID for the HTML/XML declaration
 * <dt>indent
 * <dd>Whether the processor may prettyprint ("yes"/"no")
 * </dl>
 * These attribute names and values match those in XSLT's 
 * <code>xsl:output</code> element.
 * <p>
 * This class is intended for use when you are doing an XSLT-based 
 * transformation which you would like to produce multiple output files
 * from a single input file.
 *
 * @author   Mark Taylor (Starlink)
 * @since    9 Mar 2004
 */
public class MultiXML {

    private int verbose = 1;
    private String destdir;
    private String in;
    private String stylesheet;
    private Map<String,Object> params = new HashMap<String,Object>();

    /**
     * Runs the transformation.
     * Usage is
     * <pre>
     *   MultiXML [-verbose] [-style sheet] 
     *            [-param name=value [-param name=value] ...]
     *            infile outdir
     * </pre>
     * Where:
     * <dl>
     * <dt><code>-verbose</code>
     * <dd>prints the name of each file as it is written
     * <dt><code>-style sheet</code>
     * <dd>specifies the system id of an XSLT stylesheet with which to 
     *     process the <code>infile</code> prior to doing the split
     * <dt><code>-param name=value</code>
     * <dd>defines a parameter for use in the stylesheet transformation if
     *     there is one (may be used multiple times)
     * <dt><code>infile</code>
     * <dd>the system id of the input XML file
     * <dt><code>outdir</code>
     * <dd>directory into which output files will be written
     * </dl>
     *
     * @param  args  command-line arguments
     */
    public static void main( String[] args ) 
            throws IOException, TransformerException, SAXException,
                   ParserConfigurationException {

        /* Create unconfigured worker object. */
        MultiXML worker = new MultiXML();

        /* Process arguments. */
        String usage = "MultiXML " +
                       "[-verbose] [-style sheet] [-param name=value ...] " +
                       "infile outdir";
        try {
            List<String> argv = new ArrayList<String>( Arrays.asList( args ) );
            while ( argv.get( 0 ).startsWith( "-" ) ) {
                String arg = argv.get( 0 );
                if ( arg.equals( "-verbose" ) ) {
                    argv.remove( 0 );
                    worker.setVerbose( 2 );
                }
                else if ( arg.equals( "-style" ) ) {
                    argv.remove( 0 );
                    worker.setStylesheet( argv.remove( 0 ) );
                }
                else if ( arg.equals( "-param" ) ) {
                    argv.remove( 0 );
                    String setting = argv.remove( 0 );
                    int epos = setting.indexOf( "=" );
                    String name = setting.substring( 0, epos );
                    String value = setting.substring( epos + 1 );
                    worker.getParams().put( name, value );
                }
                else if ( arg.equals( "-h" ) ) {
                    System.out.println( usage );
                    System.exit( 0 );
                }
                else {
                    throw new IllegalArgumentException();
                }
            }
            worker.setIn( argv.remove( 0 ) );
            worker.setDestdir( argv.remove( 0 ) );
            if ( argv.size() != 0 ) {
                throw new IllegalArgumentException();
            }
        }

        /* Any argument trouble, print a usage message and exit. */
        catch ( RuntimeException e ) {
            System.err.println( usage );
            System.exit( 1 );
        }

        /* Do the work. */
        worker.run();

        /* Exit explicitly - this is here because of a bug in the JVM
         * (#4701990) which can fails to exit when the system property 
         * <code>java.awt.headless</code> is set - that can be a useful
         * property to set when running this. */
        System.exit( 0 );
    }

    /**
     * Default constructor.
     */
    public MultiXML() {
    }

    /**
     * Sets the verbosity level - 0, 1 or 2 for ascending amounts of verbosity.
     *
     * @param  verbose  verbosity level
     */
    public void setVerbose( int verbose ) {
        this.verbose = verbose;
    }

    /**
     * Sets the name of the directory to which files should be output.
     *
     * @param  destdir  output directory
     */
    public void setDestdir( String destdir ) {
        this.destdir = destdir;
    }

    /**
     * Sets the location of the input XML file.
     *
     * @param   in   input file system id - may be "-" or null 
     *               for standard input
     */
    public void setIn( String in ) {
        this.in = in;
    }

    /**
     * Sets the location of a stylesheet which is to be used to process
     * the input file before the splitting is done.  May be null if
     * the input file doesn't need further processing (already contains
     * <code>filesection</code> elements).
     *
     * @param  stylesheet  XSLT stylesheet system id
     */
    public void setStylesheet( String stylesheet ) {
        this.stylesheet = stylesheet;
    }

    /**
     * Returns the map of parameters which will be used to affect the
     * stylesheet transformation.  Only effective if the stylesheet has
     * been set at run time.
     *
     * @return  modifiable parameter map
     */
    public Map<String,Object> getParams() {
        return params;
    }

    /**
     * Performs the transformation in accordance with the current 
     * attributes of this object.
     */
    public void run() 
            throws IOException, TransformerException, SAXException,
                   ParserConfigurationException {

        /* Read the input document. */
        DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();

        /* See: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6181020 */
        try {
            fact.setFeature( "http://apache.org/xml/features/dom/"
                           + "defer-node-expansion", 
                             false );
        }
        catch ( ParserConfigurationException e ) {
            // no such feature - presumably no such bug
        }
        DocumentBuilder docBuilder = fact.newDocumentBuilder();
        log( 1, "Parsing document " + ( in == null ? " on standard input "
                                                   : in ) );
        Document doc = ( in == null || in.equals( "-" ) ) 
                     ? docBuilder.parse( System.in )
                     : docBuilder.parse( in );

        /* Do initial stylesheet transformation if necessary. */
        if ( stylesheet != null ) {
            Source ssrc = new StreamSource( stylesheet );
            Transformer styler = TransformerFactory.newInstance()
                                .newTransformer( ssrc );

            /* Customise the stylesheet with parameters as required. */
            for ( Map.Entry<String,Object> entry : params.entrySet() ) {
                styler.setParameter( entry.getKey(), entry.getValue() );
            }
            DOMSource xsrc = new DOMSource( doc );
            DOMResult xres = new DOMResult();
            log( 1, "Transforming with stylesheet " + stylesheet );
            styler.transform( xsrc, xres );
            doc = (Document) xres.getNode();
        }

        /* Ensure that the output directory exists. */
        File outDir = new File( destdir );
        outDir.mkdir();

        /* Work through the top-level elements of the input XML. */
        NodeList nl = doc.getDocumentElement()
                         .getElementsByTagName( "filesection" );
        log( 1, "Writing " + nl.getLength() + " files to " + outDir );
        for ( int i = 0; i < nl.getLength(); i++ ) {
            Element el = (Element) nl.item( i );
            assert el.getTagName().equals( "filesection" );

            /* Get attributes from the element. */
            File file = new File( outDir, el.getAttribute( "file" ) );
            String pubid = el.hasAttribute( "doctype-public" ) 
                         ? el.getAttribute( "doctype-public" ) : null;
            String sysid = el.hasAttribute( "doctype-system" )
                         ? el.getAttribute( "doctype-system" ) : null;
            String indent = el.hasAttribute( "indent" )
                          ? el.getAttribute( "indent" ) : null;
            String method = el.hasAttribute( "method" )
                          ? el.getAttribute( "method" ) : null;

            /* Configure the transformer as requested. */
            Transformer trans = TransformerFactory.newInstance()
                                                  .newTransformer();
            if ( pubid != null ) {
                trans.setOutputProperty( OutputKeys.DOCTYPE_PUBLIC, pubid );
            }
            if ( sysid != null ) {
                trans.setOutputProperty( OutputKeys.DOCTYPE_SYSTEM, sysid );
            }
            if ( indent != null ) {
                trans.setOutputProperty( OutputKeys.INDENT, indent );
            }
            if ( method != null ) { 
                trans.setOutputProperty( OutputKeys.METHOD, method );
            }

            /* Do the transformation into a stream named as directed. */
            log( 2, "Output file " + file );
            OutputStream out = new FileOutputStream( file );
            Result xres = new StreamResult( out );
            for ( Node child = el.getFirstChild(); child != null; 
                  child = child.getNextSibling() ) {
                if ( child instanceof Element ) {
                    trans.transform( new DOMSource( child ), xres );
                }
            }
            out.close();
        }
    }

    /**
     * Outputs some text according to the current verbosity level.
     *
     * @param   level  importance of message: 
     *          0 important, 1 quite important, 2 not very important
     * @param   line  message text
     */
    private void log( int level, String line ) {
        if ( verbose >= level ) {
            System.out.println( line );
        }
    }
}
