package uk.ac.starlink.ttools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.StarEntityResolver;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOElementFactory;

/**
 * Copies a VOTable XML document intact but with control over the
 * DATA encoding type.
 *
 * @author   Mark Taylor (Starlink)
 * @since    19 Apr 2005
 */
public class VotCopy {

    private static Logger logger_ = Logger.getLogger( "uk.ac.starlink.ttools" );
    private static final String SAX_PROPERTY = "http://xml.org/sax/properties/";

    /**
     * Main method.  See usage message (-h) for details.
     */
    public static void main( String[] args ) {
        String cmdname = "votcopy";
        String usage = cmdname
                     + " [-h[elp]]"
                     + " [-disk]"
                     + " [-base name]"
                     + " [-debug]"
                     + " [-strict]"
                     + " [-f[ormat] tabledata|binary|fits|none]"
                     + " [-encode encoding]"
                     + " [<in> [<out>]]";

        /* Process flags. */
        List argList = new ArrayList( Arrays.asList( args ) );
        DataFormat format = DataFormat.TABLEDATA;
        Charset encoding = null;
        String base = null;
        boolean inline = true;
        boolean debug = false;
        Boolean isStrict = null;
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.equals( "-f" ) || arg.equals( "-format" ) ) {
                it.remove();
                if ( it.hasNext() ) {
                    String fname = (String) it.next();
                    it.remove();
                    if ( fname.equalsIgnoreCase( "tabledata" ) ) {
                        format = DataFormat.TABLEDATA;
                    }
                    else if ( fname.equalsIgnoreCase( "fits" ) ) {
                        format = DataFormat.FITS;
                    }
                    else if ( fname.equalsIgnoreCase( "binary" ) ) {
                        format = DataFormat.BINARY;
                    }
                    else if ( fname.equalsIgnoreCase( "none" ) ) {
                        format = null;
                    }
                    else {
                        System.err.println( usage );
                        System.exit( 1 );
                    }
                }
            }
            else if ( arg.equals( "-encode" ) || arg.equals( "-encoding" ) ) {
                it.remove();
                if ( it.hasNext() ) {
                    encoding = getEncoding( (String) it.next() );
                    it.remove();
                }
            }
            else if ( arg.equals( "-base" ) ) {
                it.remove();
                if ( it.hasNext() && base == null ) {
                    base = (String) it.next();
                    it.remove();
                }
                else {
                    System.err.println( usage );
                    System.exit( 1 );
                }
            }
            else if ( arg.equals( "-href" ) ) {
                it.remove();
                inline = false;
            }
            else if ( arg.equals( "-debug" ) ) {
                it.remove();
                debug = true;
            }
            else if ( arg.equals( "-strict" ) ) {
                it.remove();
                isStrict = Boolean.TRUE;
            }
            else if ( arg.equals( "-h" ) || arg.equals( "-help" ) ) {
                System.out.println( usage );
                System.exit( 0 );
            }
            else if ( arg.startsWith( "-" ) && arg.length() > 1 ) {
                System.err.println( usage );
                System.exit( 1 );
            }
        }

        /* Determine if we want strict parsing. */
        boolean strict = isStrict == null
                       ? VOElementFactory.isStrictByDefault()
                       : isStrict.booleanValue();

        /* Get non-flag arguments. */
        String inName = argList.isEmpty() ? "-" : (String) argList.remove( 0 );
        String outName = argList.isEmpty() ? "-" : (String) argList.remove( 0 );
        if ( ! argList.isEmpty() ) {
            System.err.println( usage );
            System.exit( 1 );
        }

        /* Work out base filename if required. */
        if ( ! inline && base == null ) {
            if ( outName == null || "-".equals( outName ) ) {
                System.err.println( "Must supply -base argument with -href " +
                                    "output to a stream" );
                System.exit( 1 );
            }
            else {
                base = outName.replaceFirst( "\\.[a-zA-Z]*$", "" );
            }
        }

        /* Get input and output streams. */
        String systemId = inName.equals( "-" ) ? "." : inName;
        InputStream in = null;
        Writer out = null;
        try {
            in = DataSource.getInputStream( inName );
            OutputStream ostrm = outName.equals( "-" )
                               ? (OutputStream) System.out
                               : (OutputStream) new FileOutputStream( outName );
            out = encoding == null ? new OutputStreamWriter( ostrm )
                                   : new OutputStreamWriter( ostrm, encoding );
            in = new BufferedInputStream( in );

            /* Do the copy. */
            saxCopy( systemId, in, out, format, inline, base, strict );
            out.flush();
        }
        catch ( IOException e ) {
            if ( debug ) {
                e.printStackTrace( System.err );
            }
            else {
                System.err.println( e.toString() );
            }
            System.exit( 1 );
        }
        catch ( SAXException e ) {
            if ( debug ) {
                e.printStackTrace( System.err );
            }
            else {
                if ( e.getCause() instanceof StreamRereadException ) {
                    System.err.println( e.getMessage() );
                    System.err.println( "Try -dom option" );
                }
                else {
                    System.err.println( e.toString() );
                }
            }
            System.exit( 1 );
        }
        finally {
            try {
                if ( in != null ) {
                    in.close();
                }
                if ( out != null ) {
                    out.close();
                }
            }
            catch ( IOException e ) {
                // no action
            }
        }
    }

    /**
     * Copies the SAX stream to the output, writing TABLE DATA elements
     * in a given encoding.
     *
     * @param  systemId  system ID of input stream
     * @param  in        input stream
     * @param  out       destination stream
     * @param  format    VOTable encoding format (may be null for DATA-less)
     * @param  base      base URI for out-of-line output tables
     * @param  strict    whether to enforce VOTable standard strictly
     */
    public static void saxCopy( String systemId, InputStream in, Writer out,
                                DataFormat format, boolean inline, String base,
                                boolean strict )
            throws SAXException, IOException {

        /* Get a SAX parser. */
        XMLReader parser;
        try {
            SAXParserFactory spfact = SAXParserFactory.newInstance();
            spfact.setValidating( false );
            spfact.setNamespaceAware( true );
            trySetFeature( spfact, "namespace-prefixes", true );
            trySetFeature( spfact, "external-general-entities", false );
            parser = spfact.newSAXParser().getXMLReader();
        }
        catch ( ParserConfigurationException e ) {
            throw (SAXException) new SAXException( e.getMessage() )
                                .initCause( e );
        }

        /* Install a custom entity resolver. */
        parser.setEntityResolver( StarEntityResolver.getInstance() );

        /* Install an error handler. */
        parser.setErrorHandler( new ErrorHandler() {
            public void error( SAXParseException e ) {
                logger_.warning( e.toString() );
            }
            public void warning( SAXParseException e ) {
                logger_.warning( e.toString() );
            }
            public void fatalError( SAXParseException e ) throws SAXException {
                throw e;
            }
        } );

        /* Install a content handler which can pull out data from one table
         * as required. */
        VotCopyHandler copier = new VotCopyHandler( strict, format, inline,
                                                    base );
        copier.setOutput( out );
        parser.setContentHandler( copier );

        /* Try to set the lexical handler.  If this fails you just lose some
         * lexical details such as comments and CDATA marked sections. */
        try {
            parser.setProperty( SAX_PROPERTY + "lexical-handler",
                                (LexicalHandler) copier );
        }
        catch ( SAXException e ) {
            logger_.info( "Lexical handler not set: " + e );
        }

        /* Create a SAX input source. */
        InputSource saxsrc = new InputSource( in );
        saxsrc.setSystemId( systemId );

        /* Do the parse. */
        parser.parse( saxsrc );
    }

    /**
     * Gets an encoding from a string.  If the encoding is not available,
     * a useful error message is written to standard error and exit
     * is called.
     *
     * @param  encName  encoding name
     * @return  encoding
     */
    private static Charset getEncoding( String encName ) {
        try {
            return Charset.forName( encName );
        }
        catch ( UnsupportedCharsetException e ) {
        }
        catch ( IllegalCharsetNameException e ) {
        }
        StringBuffer err = new StringBuffer();
        err.append( "Unsupported encoding: " + encName + "\n" );
        err.append( "Supported encodings: " );
        for ( Iterator it = Charset.availableCharsets().keySet().iterator();
              it.hasNext(); ) {
            err.append( ' ' );
            err.append( it.next().toString() );
        }
        System.err.println( err.toString() );
        System.exit( 1 );
        throw new AssertionError();
    }

    /**
     * Attempts to set a feature on a SAXParserFactory, but doesn't panic
     * if it can't.
     *
     * @param  spfact   factory
     * @param  feature  feature name <em>excluding</em> the 
     *                  "http://xml.org/sax/features/" part
     */
    private static boolean trySetFeature( SAXParserFactory spfact,
                                          String feature, boolean value ) {
        try {
            spfact.setFeature( "http://xml.org/sax/features/" + feature, 
                               value );
            return true;
        }
        catch ( ParserConfigurationException e ) {
            return false;
        }
        catch ( SAXException e ) {
            return false;
        }
    }

}
