package uk.ac.starlink.vo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Represents information from an IVOA Vocabulary.
 * Functionality right now is very limited; it just returns the names
 * of the terms defined.
 * The vocabulary parsing implementation is also extremely sloppy,
 * it ignores the RDF document structure altogether and just looks
 * for some likely looking attribute names.  But it's probably good
 * enough for current purposes.
 *
 * <p>If more is required in the future, the API and implementation
 * may change significantly.
 *
 * <p>In principle, this follows the prescription of the IVOA Vocabularies 
 * standard.  However, at time of writing, the most recent version of
 * that document, v1.19, is out of date and in practice not followed by
 * actual vocabularies in the VO; in particular none of the SKOS
 * machinery is used any more.  The current implementation of this
 * class is very minimally dependent on the details of vocabulary
 * implementation, so it doesn't matter much for now.
 *
 * @see  <a href="http://www.ivoa.net/documents/latest/Vocabularies.html"
 *          >IVOA Vocabularies Recommendation</a>
 */
public class Vocabulary {

    private final String[] termNames_;

    /**
     * Constructor.
     *
     * @param  termNames  list of unqualified names;
     *                    these are fragment identifiers only (no # character)
     *                    within the namespace of the vocabulary in question
     */
    public Vocabulary( String[] termNames ) {
        termNames_ = termNames;
    }

    /**
     * Returns the list of names referenced in this vocabulary.
     *
     * @return  list of unqualified names;
     *          these are fragment identifiers only (no # character)
     *          within the namespace of the vocabulary in question
     */
    public String[] getTermNames() {
        return termNames_;
    }

    /**
     * Returns a vocabulary read from a given URL.
     * The URL is both the namespace in which terms are required,
     * and the location of the vocabulary document.
     * In accordance with the IVOA Vocabularies standard,
     * the vocabulary must be available from the URL in question
     * in <code>application/rdf+xml</code> format.
     *
     * @param  vocabUrl  vocabularly namespace and resource URL
     */
    public static Vocabulary readVocabulary( URL vocabUrl )
            throws IOException, SAXException {

        /* Prepare a parser that can read an RDF SAX stream and
         * extract vocabulary information from it. */
        SAXParserFactory spfact = SAXParserFactory.newInstance();
        SAXParser parser;
        try {
            spfact.setNamespaceAware( false );
            spfact.setValidating( false );
            parser = spfact.newSAXParser();
        }
        catch ( ParserConfigurationException e ) {
            throw (IOException) new IOException( "SAX trouble" ).initCause( e );
        }
        VocabSaxHandler vHandler = new VocabSaxHandler( vocabUrl + "#" );

        /* Open a connection that will read the vocabulary in RDF/XML format. */
        URLConnection conn = vocabUrl.openConnection();
        if ( conn instanceof HttpURLConnection ) {
            ((HttpURLConnection) conn).setInstanceFollowRedirects( true );
        }
        conn.setRequestProperty( "Accept", "application/rdf+xml" );
        InputStream in = new BufferedInputStream( conn.getInputStream() );

        /* Perform the parse. */
        try {
            parser.parse( in, vHandler );
        }
        finally {
            in.close();
        }

        /* Retrieve the terms and return a vocabulary instance. */
        return new Vocabulary( vHandler.terms_.toArray( new String[ 0 ] ) );
    }

    /**
     * SAX handler that pulls vocabulary terms out of an XML stream in
     * application/rdf+xml format.  It does it using a shocking hack
     * that could easily get the wrong answer, but probably won't.
     */
    private static class VocabSaxHandler extends DefaultHandler {

        private final String termPrefix_;
        Set<String> terms_;
 
        /**
         * Constructor.
         *
         * @param  termPrefix  any term referenced in an RDF "about" attribute
         *                     which starts with this string will be identified
         *                     as a named term
         */
        VocabSaxHandler( String termPrefix ) {
            termPrefix_ = termPrefix;
        }

        @Override
        public void startDocument() {
            terms_ = new LinkedHashSet<String>();
        }

        @Override
        public void startElement( String uri, String localName, String qName,
                                  Attributes atts ) {

            /* Look through all the attributes of all the elements, and
             * find any one that has a name like "about".
             * If the attribute value starts with our required prefix,
             * assume we've found a name. */
            for ( int ia = 0; ia < atts.getLength(); ia++ ) {
                String lname = atts.getLocalName( ia );
                String qname = atts.getQName( ia );
                if ( "about".equals( lname ) ||
                     "about".equals( qname ) ||
                     qname != null && qname.endsWith( ":about" ) ) {
                    String value = atts.getValue( ia );
                    if ( value.startsWith( termPrefix_ ) ) {
                        terms_.add( value.substring( termPrefix_.length() ) );
                    }
                }
            }
        }
    }

    /**
     * Test.
     */
    public static void main( String[] args ) throws IOException, SAXException {
        String url = args.length > 0 ? args[ 0 ]
                                     : "http://www.ivoa.net/rdf/timescale";
        String[] names = readVocabulary( new URL( url ) ).getTermNames();
        System.out.println( Arrays.toString( names ) );
    }
}
