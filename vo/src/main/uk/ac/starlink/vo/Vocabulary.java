package uk.ac.starlink.vo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Represents information from an IVOA Vocabulary.
 *
 * <p>This class is written with reference to Version 2.0 of the document
 * <em>Vocabularies in the VO</em>, and particularly the Desise
 * serialization described there.  Note that document is in
 * Working Draft status at time of writing.
 *
 * @see  <a href="http://www.ivoa.net/documents/Vocabularies/"
 *          >Vocabularies in the VO</a>
 */
public class Vocabulary {

    private final String uri_;
    private final String flavour_;
    private final Map<String,VocabTerm> termMap_;

    /**
     * Constructor.
     *
     * @param  uri   namespace URI
     * @param  terms   entries in the vocabulary
     * @param  flavour  vocabulary flavour string
     */
    public Vocabulary( String uri, VocabTerm[] terms, String flavour ) {
        uri_ = uri;
        flavour_ = flavour;
        Map<String,VocabTerm> tmap = new LinkedHashMap<>();
        for ( VocabTerm term : terms ) {
            tmap.put( term.getTerm(), term );
        }
        termMap_ = Collections.unmodifiableMap( tmap );
    }

    /**
     * Returns the URI defining the namespace of this vocabulary.
     *
     * @return  vocabulary namespace URI
     */
    public String getUri() {
        return uri_;
    }

    /**
     * Returns the vocabulary flavour string.
     *
     * @return  flavour
     */
    public String getFlavour() {
        return flavour_;
    }

    /**
     * Returns a map of the terms contained in this vocabulary.
     * The map keys are unqualified term names (no # character).
     *
     * @return   term-&gt;properties map
     */
    public Map<String,VocabTerm> getTerms() {
        return termMap_;
    }

    /**
     * Returns a vocabulary read from a given URL.
     * The URL will usually be equal to the namespace in which the terms are
     * required (<code>http://www.ivoa.net/rdf/&lt;vocab-name&gt;</code>).
     *
     * @param  vocabUrl  resource URL
     * @return  vocabulary object
     */
    public static Vocabulary readVocabulary( URL vocabUrl ) throws IOException {
        return readVocabularyDesise( vocabUrl );
    }

    /**
     * Reads a vocabulary using desise encoding.
     * The vocabulary must be available on (Accept-header) request
     * from the URL in question in <code>application/x-desise+json</code>
     * format.  In accordance with the Vocabularies in VO 2.0 standard,
     * all IVOA vocabularies can be so retrieved using URLs equivalent
     * to their namespace URIs, of the form
     * <code>http://www.ivoa.net/rdf/&lt;vocab-name&gt;</code>.
     *
     * @param  vocabUrl  vocabulary URL, typically equal to the namespace URI
     * @return  fully populated vocabulary
     */
    public static Vocabulary readVocabularyDesise( URL vocabUrl )
            throws IOException {

        /* Open a connection that will read the vocabulary in Desise format. */
        URLConnection conn = vocabUrl.openConnection();
        if ( conn instanceof HttpURLConnection ) {
            ((HttpURLConnection) conn).setInstanceFollowRedirects( true );
        }
        conn.setRequestProperty( "Accept", "application/x-desise+json" );
        InputStream in = new BufferedInputStream( conn.getInputStream() );
        try {

            /* Parse the JSON in accordance with the desise format
             * defined in Vocabularies 2.0. */
            JSONTokener jt = new JSONTokener( in );
            Object top = jt.nextValue();
            if ( top instanceof JSONObject ) {
                JSONObject topObj = (JSONObject) top;
                Object uriObj = topObj.opt( "uri" );
                String uri = uriObj instanceof String ? (String) uriObj : null;
                Object flavourObj = topObj.opt( "flavour" );
                String flavour = flavourObj instanceof String
                               ? (String) flavourObj
                               : null;
                Object termsObj = topObj.get( "terms" );
                if ( termsObj instanceof JSONObject ) {
                    JSONObject termsJson = (JSONObject) termsObj;
                    List<VocabTerm> list = new ArrayList<>();
                    for ( Iterator<?> it = termsJson.keys(); it.hasNext(); ) {
                        Object key = it.next();
                        if ( key instanceof String ) {
                            String term = (String) key;
                            Object termObj = termsJson.get( term );
                            if ( termObj instanceof JSONObject ) {
                                JSONObject termJson = (JSONObject) termObj;
                                list.add( new JsonVocabTerm( term, termJson ) );
                            }
                        }
                    }
                    VocabTerm[] terms = list.toArray( new VocabTerm[ 0 ] );

                    /* Construct and return the Vocabulary object. */
                    return new Vocabulary( uri, terms, flavour );
                }
            }
        }
        finally {
            in.close();
        }
        throw new IOException( "JSON doesn't seem to follow desise syntax" );
    }

    /**
     * Reads a vocabulary using RDF XML encoding.
     * In accordance with the IVOA Vocabularies standard,
     * the vocabulary must be available from the URL in question
     * in <code>application/rdf+xml</code> format.
     *
     * <p><strong>Note:</strong> The RDF parsing performed by this method
     * is very sketchy and pulls out only a minimum of information from
     * the retrieved XML (the term values themselves).
     * This implementation is present mainly for historical reasons.
     * For practical purposes, the much more thorough
     * {@link #readVocabularyDesise} method should be used instead.
     * It would be possible to ehance this implementation to get
     * more or all the available information from the XML,
     * but since the desise-format variant ought to be available
     * for all IVOA vocabularies, why go to the effort?
     *
     * @param  vocabUrl  vocabulary namespace and resource URL
     * @return  vocabulary object
     * @deprecated  does very basic vocabulary parsing;
     *              use <code>readVocabularyDesise</code> instead
     */
    @Deprecated
    public static Vocabulary readVocabularyRdfXml( URL vocabUrl )
            throws IOException {

        /* Prepare a parser that can read an RDF SAX stream and
         * extract vocabulary information from it. */
        SAXParserFactory spfact = SAXParserFactory.newInstance();
        SAXParser parser;
        try {
            spfact.setNamespaceAware( false );
            spfact.setValidating( false );
            parser = spfact.newSAXParser();
        }
        catch ( ParserConfigurationException | SAXException e ) {
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
        catch ( SAXException e ) {
            throw new IOException( "RDF XML parse failed"
                                 + " (" + e.getMessage() + ")", e );
        }
        finally {
            in.close();
        }

        /* Come up with a URI; the supplied URL isn't guaranteed to be
         * the vocabulary namespace URI but it might be, so use it as
         * better than nothing. */
        String uri = vocabUrl.toString();

        /* Retrieve the terms and return a vocabulary instance. */
        VocabTerm[] terms = vHandler.terms_.toArray( new VocabTerm[ 0 ] );
        String flavour = null;
        return new Vocabulary( uri, terms, flavour );
    }

    /**
     * SAX handler that pulls vocabulary terms out of an XML stream in
     * application/rdf+xml format.  It does it using a shocking hack
     * that could easily get the wrong answer, but probably won't.
     */
    private static class VocabSaxHandler extends DefaultHandler {

        private final String termPrefix_;
        List<VocabTerm> terms_;
 
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
            terms_ = new ArrayList<VocabTerm>();
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
                        String term = value.substring( termPrefix_.length() );
                        terms_.add( new BasicVocabTerm( term ) );
                    }
                }
            }
        }
    }

    /**
     * Partial VocabTerm implementation.
     */
    private static abstract class AbstractVocabTerm implements VocabTerm {

        private final String term_;

        /**
         * Constructor.
         *
         * @param  term string
         */
        AbstractVocabTerm( String term ) {
            term_ = term;
        }

        public String getTerm() {
            return term_;
        }
    }

    /**
     * Minimal VocabTerm implementation that just provides the term itself.
     */
    private static class BasicVocabTerm extends AbstractVocabTerm {

        /**
         * Constructor.
         *
         * @param  term string
         */
        BasicVocabTerm( String term ) {
            super( term );
        }

        public String getLabel() {
            return null;
        }
        public String getDescription() {
            return null;
        }
        public boolean isPreliminary() {
            return false;
        }
        public boolean isDeprecated() {
            return false;
        }
        public String[] getWider() {
            return new String[ 0 ];
        }
        public String[] getNarrower() {
            return new String[ 0 ];
        }
        public String getUseInstead() {
            return null;
        }
        @Override
        public String toString() {
            return getTerm();
        }
    }

    /**
     * VocabTerm implementation based on a JSONObject consistent with
     * the desise serialization described in VocInVO2.
     */
    private static class JsonVocabTerm extends AbstractVocabTerm {

        private final String label_;
        private final String description_;
        private final boolean isPreliminary_;
        private final boolean isDeprecated_;
        private final String[] wider_;
        private final String[] narrower_;
        private final String useInstead_;

        /**
         * Constructor.
         *
         * @param  term  term string
         * @param  json  desise JSONObject containing properties
         */
        JsonVocabTerm( String term, JSONObject json ) {
            super( term );
            label_ = getString( json, "label" );
            description_ = getString( json, "description" );
            isPreliminary_ = json.has( "preliminary" );
            isDeprecated_ = json.has( "deprecated" );
            wider_ = getStrings( json, "wider" );
            narrower_ = getStrings( json, "narrower" );

            /* Note that the "useInstead" key is not documented in
             * VocInVO 2.1 section 3.2.1.  * This is an oversight in
             * that document scheduled for correction, see
             * https://wiki.ivoa.net/twiki/bin/view/IVOA/Vocabularies-2_1-Next
             * Such keys are actually produced by the desise serialiser at
             * http://www.ivoa.net/rdf/. */
            useInstead_ = getString( json, "useInstead" );
        }

        public String getLabel() {
            return label_;
        }

        public String getDescription() {
            return description_;
        }

        public boolean isPreliminary() {
            return isPreliminary_;
        }

        public boolean isDeprecated() {
            return isDeprecated_;
        }

        public String[] getWider() {
            return wider_;
        }

        public String[] getNarrower() {
            return narrower_;
        }

        public String getUseInstead() {
            return useInstead_;
        }

        /**
         * Extracts a string from a JSON object.
         *
         * @param  json  JSON object
         * @param  key   key within object
         * @return  best efforts string value corresponding to key,
         *          may be null
         */
        private static String getString( JSONObject json, String key ) {
            Object value = json.opt( key );
            return value instanceof String ? (String) value : null;
        }

        /**
         * Extracts a string array from a JSON object.
         *
         * @param  json  JSON object
         * @param  key   key within object
         * @return  best efforts string array corresponding to key,
         *          may be empty but not null
         */
        private static String[] getStrings( JSONObject json, String key ) {
            Object value = json.opt( key );
            List<String> list = new ArrayList<>();
            if ( value instanceof JSONArray ) {
                JSONArray jarray = (JSONArray) value;
                for ( int i = 0; i < jarray.length(); i++ ) {
                    Object el = jarray.get( i );
                    if ( el instanceof String ) {
                        list.add( (String) el );
                    }
                }
            }
            return list.toArray( new String[ 0 ] );
        }

        @Override
        public String toString() {
            return getTerm() + "(" + getLabel() + ")";
        }
    }

    /**
     * Test.
     */
    public static void main( String[] args ) throws IOException {
        String url = args.length > 0 ? args[ 0 ]
                                     : "http://www.ivoa.net/rdf/timescale";
        Vocabulary vocab = readVocabulary( new URL( url ) );
        System.out.println( vocab.getUri() );
        for ( VocabTerm term : vocab.getTerms().values() ) {
            System.out.println( "   " + term );
        }
    }
}
