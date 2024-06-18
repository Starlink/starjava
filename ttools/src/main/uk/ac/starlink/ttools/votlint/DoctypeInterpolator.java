package uk.ac.starlink.ttools.votlint;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;
import uk.ac.starlink.util.IOUtils;

/**
 * Attempts to check that a DOCTYPE declaration is present in an
 * input stream representing XML.  If it is not, one is inserted.
 * The algorithm used here isn't bulletproof, but should work in most
 * sensible cases, and fail noisily if it can't do it.
 * It copes with a number of not-too-weird XML encodings.
 *
 * @author   Mark Taylor (Starlink)
 */
public class DoctypeInterpolator {

    private static final Pattern XMLDECL_PATTERN = 
        Pattern.compile( "<\\?[Xx][Mm][Ll][^\\?]*\\?>" );
    private static String VOTABLE_DECL =
        "<!DOCTYPE VOTABLE PUBLIC '-//votlint//VOTable.dtd' " +
                                 "'file:///dummylocation/VOTable.dtd'>";

    private boolean used_;
    private String votableVersion_;

    /**
     * Returns an input stream which is a copy of a given one, except
     * that if the given one doesn't include a DOCTYPE declaration,
     * one is inserted.
     * This method can only be called once for each instance of this class.
     *
     * @param  in  original input stream
     * @return  input stream like <code>in</code> but with a DOCTYPE
     * @throws  IllegalStateException  if this method has already been called
     *          on this object
     */
    public InputStream getStreamWithDoctype( BufferedInputStream in )
            throws IOException {
        if ( used_ ) {
            throw new IllegalStateException( "This interpolator already used" );
        }
        used_ = true;
        assert in.markSupported();

        /* Try looking at the head of the stream for a selection of header
         * lengths.  It may fail for one length since the end of the header
         * may fall in the middle of something the parser is trying to parse. */
        int[] tryLengths = new int[] { 2 * 1024, 16 * 1024, 256 * 1024 };
        for ( int i = 0; i < tryLengths.length; i++ ) {
            int leng = tryLengths[ i ];

            /* Mark and read the first part of the stream. */
            in.mark( leng );
            byte[] buf = IOUtils.readBytes( in, leng );

            /* See if this is enough to tell whether there is a Document
             * Type Declaration in the stream. */
            Boolean hasDoctype = hasDoctype( buf );

            /* If it is, act accordingly. */
            if ( hasDoctype != null ) {
                if ( ! hasDoctype.booleanValue() ) {
                    buf = interpolateDoctype( buf );
                }
                InputStream s1 = new ByteArrayInputStream( buf );
                InputStream s2 = in;
                return new SequenceInputStream( s1, s2 );
            }

            /* Otherwise, reset the read so we can do the same again
             * with a bigger chunk. */
            in.reset();

            /* If we've read the whole stream and not come up with any
             * conclusions, probably the XML is badly-formed or something.
             * Return the whole stream to the caller to deal with it. */
            boolean fullyRead = buf.length < leng;
            if ( fullyRead ) {
                break;
            }
        }

        /* If we've got this far without returning a (possibly modified)
         * stream, we just return the original one (reset to the start
         * if necessary) unchanged. */
        message( "Can't detect whether a Document Type Declaration " +
                 "is present" );
        return in;
    }

    /**
     * Returns the value of the <code>version</code> attribute of the top-level
     * VOTABLE element, if there was one and it was encountered.
     *
     * @return   declared VOTable version string, or null
     */
    public String getVotableVersion() {
        return votableVersion_;
    }

    /**
     * Presents a processing message.
     *
     * @param   msg  message text
     */
    public void message( String msg ) {
        System.err.println( msg );
    }

    /**
     * Tries to determines whether the start of an XML document 
     * contains a DOCTYPE declaration prior to the first element.
     * 
     * @param   buffer containing the head of an XML document
     * @return  TRUE if a DOCTYPE declaration is present; false if it is
     *          absent; null if <code>buf</code> doesn't go as far as
     *          the first element
     */
    private Boolean hasDoctype( byte[] buf ) {
        DeclarationChecker declCheck = new DeclarationChecker();
        XMLReader parser;
        try {
            parser = SAXParserFactory.newInstance().newSAXParser()
                                     .getXMLReader();
            parser.setContentHandler( declCheck );
            parser.setEntityResolver( declCheck );
            parser.setErrorHandler( declCheck );
            parser.setProperty( "http://xml.org/sax/properties/lexical-handler",
                                declCheck );
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
            return declCheck.hasDoctype();
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
     * Modifies a byte buffer representing the start of an XML document so
     * it contains a DOCTYPE declaration.
     *
     * @param  buf  buffer containing XML without a DOCTYPE
     * @return  buffer containing XML with a DOCTYPE
     */
    private byte[] interpolateDoctype( byte[] buf ) {

        /* Search for an XML declaration in all the likely encodings. */
        String[] encodings = new String[] { "UTF-8", "UTF-16BE", "UTF-16LE", };
        for ( int i = 0; i < encodings.length; i++ ) {
            String encoding = encodings[ i ];
            try {
                String content = new String( buf, encoding );
                Matcher matcher = XMLDECL_PATTERN.matcher( content );
                if ( matcher.find() ) {

                    /* If we've found one, interpolate the VOTable
                     * declaration immediately after it. */
                    return matcher
                          .replaceFirst( matcher.group( 0 ) + VOTABLE_DECL )
                          .getBytes( encoding );
                }
            }
            catch ( UnsupportedEncodingException e ) {
                assert false : "Unsupported encoding " + encoding + "?";
            }
        }

        /* Otherwise, we need to interpolate the VOTable declaration right
         * at the head of the buffer.  First determine the character 
         * encoding. */
        String encoding = "UTF-8";
        int bomleng = 0;
        if ( buf.length > 2 ) {
           if ( buf[ 0 ] == (byte) 0xfe && buf[ 1 ] == (byte) 0xff ) {
               encoding = "UTF-16BE";
               bomleng = 2;
           }
           else if ( buf[ 0 ] == (byte) 0xff && buf[ 1 ] == (byte) 0xfe ) {
               encoding = "UTF-16LE";
               bomleng = 2;
           }
        }

        /* Then prepare a buffer containing, in sequence, an optional 
         * byte-order mark, followed by the VOTable document type declaration
         * in the correct encoding, followed by the contents of the
         * original input buffer. */
        try {
            byte[] prebuf = VOTABLE_DECL.getBytes( encoding );
            byte[] fullbuf = new byte[ prebuf.length + buf.length ];
            System.arraycopy( buf, 0, fullbuf, 0, bomleng );
            System.arraycopy( prebuf, 0, fullbuf, bomleng, prebuf.length );
            System.arraycopy( buf, bomleng, fullbuf, bomleng + prebuf.length,
                              buf.length - bomleng );

            /* Return the doctored buffer. */
            return fullbuf;
        }
        catch ( UnsupportedEncodingException e ) {
            throw new AssertionError( "Unsupported encoding " + encoding +
                                      "?" );
        }
    }

    /**
     * ContentHandler and LexicalHandler which uses SAX callbacks for a 
     * single purpose: to work out whether a Document Type Declaration is
     * present.  One of the callbacks will throw a DoneException when 
     * it's decided one way or the other.
     */
    private class DeclarationChecker extends DefaultHandler
              implements ContentHandler, EntityResolver, ErrorHandler,
                         LexicalHandler {
        private Boolean hasDoctype_;

        /**
         * Indicates whether the parse worked out if a DOCTYPE declaration
         * was present or not.
         *
         * @return  TRUE if DOCTYPE is present at the head of the document,
         *          FALSE if it's not, null if not enough of the document
         *          is present to be able to tell
         */
        public Boolean hasDoctype() {
            return hasDoctype_;
        }

        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts )
            throws DoneException {
            if ( hasDoctype_ == null ) {
                hasDoctype_ = Boolean.FALSE;
            }
            if ( "VOTABLE".equals( localName ) ||
                 qName != null && ( qName.equals( "VOTABLE" ) ||
                                    qName.endsWith( ":VOTABLE" ) ) ) {
                votableVersion_ = atts.getValue( "version" );
            }
            throw new DoneException();
        }

        public void startDTD( String name, String publicId, String systemId ) {
            hasDoctype_ = Boolean.TRUE;
        }

        // ErrorHandler implementation.
        public void warning( SAXParseException e ) {
        }
        public void error( SAXParseException e ) {
        }
        public void fatalError( SAXParseException e ) throws SAXException {
            throw (DoneException) new DoneException()
                 .initCause( e );
        }

        // Dummy LexicalHandler methods.
        public void endDTD() {}
        public void startCDATA() {}
        public void endCDATA() {}
        public void startEntity( String name ) {}
        public void endEntity( String name ) {}
        public void comment( char[] ch, int start, int length ) {}

        // EntityResolver.
        public InputSource resolveEntity( String publicId, String systemId ) {

            /* Returns an empty source - we're not interested in the data. */
            return new InputSource( new ByteArrayInputStream( new byte[ 0 ] ) );
        }
    }

    /**
     * Exception thrown when enough of the exploratory SAX parse has been
     * completed to find out the requisite information.
     */
    private static class DoneException extends SAXException {
        DoneException() {
            super( "Successful completion" );
        }
    }

    /**
     * Filter program which takes a stream on standard output and writes
     * to standard output including the DOCTYPE.
     */
    public static void main( String[] args ) throws IOException {
        InputStream in = 
            new DoctypeInterpolator()
           .getStreamWithDoctype( new BufferedInputStream( System.in ) );
        for ( int c; ( c = in.read() ) >= 0; System.out.write( c ) ) {}
    }
}
