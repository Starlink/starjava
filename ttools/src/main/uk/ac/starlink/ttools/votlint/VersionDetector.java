package uk.ac.starlink.ttools.votlint;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import uk.ac.starlink.util.IOUtils;

/**
 * Determines the version of a VOTable document.
 *
 * @author   Mark Taylor
 * @since    20 Nov 2012
 */
public class VersionDetector {

    /* Deliberately sloppy pattern - if it matches this it will identify
     * a version unambiguously, even if it doesn't get the URL correct. */
    private static final Pattern nsRegex =
        Pattern.compile( ".*VOTable/?v?([0-9a-z_.]+)",
                         Pattern.CASE_INSENSITIVE );

    /**
     * Determines the reported version of a VOTable document contained
     * in an input stream.  The attributes on the first encountered
     * VOTABLE start tag are used.  A "version" attribute is used if
     * available, otherwise the namespacing attributes are trawled.
     * Mark/reset is used; whether a version string is identified or not,
     * the stream is reset to the starting position on exit.
     *
     * @param  in  input stream
     * @return  declared version string, or null if none can be found
     */
    public static String getVersionString( BufferedInputStream in )
            throws IOException {
        assert in.markSupported();

        /* Try looking at the head of the stream for a selection of header
         * lengths.  It may fail for one length since the end of the header
         * may fall in the middle of something the parser is trying to parse. */
        int[] tryLengths = new int[] { 2 * 1024, 16 * 1024, 256 * 1024 };
        for ( int i = 0; i < tryLengths.length; i++ ) {
            int leng = tryLengths[ i ];

            /* Read the first part of the stream, and then reset it to the
             * starting position. */
            in.mark( leng );
            final byte[] buf;
            try {
                buf = IOUtils.readBytes( in, leng );
            }
            finally {
                in.reset();
            }

            /* See if this is enough to acquire the attributes on the
             * root VOTABLE element. */
            Attributes atts = getVotableAttributes( buf );

            /* If it is, interrogate them to find a version. */
            if ( atts != null ) {
                return getVersionString( atts );
            }
        }

        /* No luck. */
        return null;
    }

    /**
     * Try to get the declared version string from a set of attributes
     * expected to be found on the VOTABLE element start tag.
     *
     * @param   atts  VOTABLE attributes
     * @return  VOTable version, or null
     */
    private static String getVersionString( Attributes atts ) {
        String version = getAttributeValue( atts, "version" );
        if ( version != null ) {
            return version;
        }
        String xmlns = getAttributeValue( atts, "xmlns" );
        if ( xmlns != null ) {
            Matcher matcher = nsRegex.matcher( xmlns );
            if ( matcher.matches() ) {
                return matcher.group( 1 );
            }
        }
        int iSchemaLoc =
            atts.getIndex( "http://www.w3.org/2001/XMLSchema-instance",
                           "schemaLocation" );
        if ( iSchemaLoc >= 0 ) {
            String schemaLoc = atts.getValue( iSchemaLoc );
            String[] words = schemaLoc.split( "\\s+" );
            for ( int i = 0; i < words.length; i++ ) {
                Matcher matcher = nsRegex.matcher( words[ i ] );
                if ( matcher.matches() ) {
                    return matcher.group( 1 );
                }
            }
        }
        return null;
    }

    /**
     * Returns the value of a named attribute.
     * Namespaces are treated in a cavalier fashion.
     *
     * @param  atts  attributes object
     * @param  name  attribute name
     * @return  value for attribute named <code>name</code>, or null
     */
    private static String getAttributeValue( Attributes atts, String name ) {
        for ( int i = 0; i < atts.getLength(); i++ ) {
            if ( name.equals( atts.getQName( i ) ) ||
                 name.equals( atts.getLocalName( i ) ) ) {
                return atts.getValue( i );
            }
        }
        return null;
    }

    /**
     * Returns the attributes declared on the start tag of the first VOTABLE
     * element found in the XML document which starts with the bytes
     * contained in a given byte buffer.
     * A partial SAX parse is performed, and aborted once the VOTABLE element
     * has been encountered.
     *
     * @param  buf  start of an XML file
     * @return  attributes of VOTABLE element, or null
     */
    private static Attributes getVotableAttributes( byte[] buf ) {
        StartTagReader vtReader = new StartTagReader( "VOTABLE" );
        XMLReader parser;
        try {
            SAXParserFactory pfact = SAXParserFactory.newInstance();
            pfact.setNamespaceAware( true );
            pfact.setValidating( false );
            parser = pfact.newSAXParser().getXMLReader();
            parser.setContentHandler( vtReader );
            parser.setEntityResolver( vtReader );
            parser.setErrorHandler( vtReader );
        }
        catch ( ParserConfigurationException e ) {
            return null;
        }
        catch ( SAXException e ) {
            return null;
        }
        try {
            parser.parse( new InputSource( new ByteArrayInputStream( buf ) ) );
        }
        catch ( DoneException e ) {
            return vtReader.getTagAttributes();
        }
        catch ( SAXException e ) {
            return null;
        }
        catch ( IOException e ) {
            return null;
        }
        return null;
    }

    /**
     * SAX content handler which does only one job: acquires the attributes
     * on the first element with a given name.
     */
    private static class StartTagReader extends DefaultHandler
            implements ContentHandler, EntityResolver, ErrorHandler {

        private final String tagName_;
        private Attributes tagAtts_;

        /**
         * Constructor.
         *
         * @param   tagName  name of element whose attributes are to be
         *                   retrieved; do not include namespace
         */
        StartTagReader( String tagName ) {
            tagName_ = tagName;
        }

        /**
         * Following a parse using this content handler, this method
         * returns the attributes on the named tag, if the tag was encountered.
         *
         * @return  found attributes, or null
         */
        public Attributes getTagAttributes() {
            return tagAtts_;
        }

        // ContentHandler
        @Override
        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts )
                throws SAXException {
            if ( tagName_.equals( localName ) ||
                 tagName_.equals( qName ) ) {
                tagAtts_ = atts;
                throw new DoneException();
            }
        }

        // ErrorHandler.
        @Override
        public void fatalError( SAXParseException e ) throws SAXException {
            throw (DoneException) new DoneException().initCause( e );
        }

        // EntityResolver.
        /* Returns an empty source - we're not interested in the data. */
        @Override
        public InputSource resolveEntity( String publicId, String systemId ) {
            return new InputSource( new ByteArrayInputStream( new byte[ 0 ] ) );
        }
    }

    /**
     * Exception thrown on successful completion of the job for which
     * this handler was created.
     */
    private static class DoneException extends SAXException {
        DoneException() {
            super( "Completed parse" );
        }
    }

    /**
     * Prints out the version number of a VOTable file presented
     * on standard input.
     */
    public static void main( String[] args ) throws IOException {
        String version =
            new VersionDetector()
           .getVersionString( new BufferedInputStream( System.in ) );
        System.out.println( "version: " + version );
    }
}
