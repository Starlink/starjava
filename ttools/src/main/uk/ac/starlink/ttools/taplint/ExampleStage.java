package uk.ac.starlink.ttools.taplint;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.ContentType;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.vo.AdqlValidator;
import uk.ac.starlink.vo.StdCapabilityInterface;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.vo.TapService;

/**
 * Validation stage for testing TAP /examples document.
 * At time of writing the relevant specifications are split between
 * DALI 1.1 and TAP 1.1 (both WDs).
 * An earlier specification was in a TAP Implementation Note.
 *
 * <p>The details may undergo some changes.
 *
 * @author   Mark Taylor
 * @since    20 May 2016
 */
public class ExampleStage implements Stage {

    private final TapRunner tapRunner_;
    private final CapabilityHolder capHolder_;
    private final MetadataHolder metaHolder_;

    private static final String EXAMPLES_STDID =
        "ivo://ivoa.net/std/DALI#examples";
    private static final int QUERY_MAXREC = 10;
    private static final Pattern XMLNAME_REGEX = createXmlNameRegex();
    private static final Set<String> BODY_PLAINTEXT_PROPS = createStringSet(
        "name", "capability", "continuation",  // DALI 1.1
        "query", "table"                       // TAP 1.1
    );
    private static final Set<String> GP_PLAINTEXT_PROPS = createStringSet(
        "key", "value"
    );

    // Permitted MIME type for examples document mandated by TAP 1.1 WD.
    private static final ContentTypeOptions CTYPE_XHTML =
        new ContentTypeOptions( new ContentType[] {
            new ContentType( "text", "html" ),
            new ContentType( "application", "xhtml+xml" ),
        } );

    // The 'correct' value for the RDFa @vocab attribute is a real mess.
    // The values listed below have some claim to legitimacy.
    // The worst problem is that TOPCAT versions 4.4 and earlier
    // (from 4.3, when TAP examples support was introduced)
    // required one of the forms TAPNOTE_VOCAB or PRAGMATIC_VOCAB,
    // and failed to find the example elements otherwise
    // (including in absence of any @vocab),
    // so until TOPCAT versions 4.3-4.4 inclusive fall out of use,
    // services probably need to use one of those forms,
    // despite the fact that they are not permitted by any REC.
    // If that's not a concern, the DALI 1.1 value is probably preferred.
    // See dal list thread "DALI examples vocab" starting 19 May 2016,
    // also private thread "examples @vocab" between MBT,
    // Markus Demleitner and Pat Dowler in July 2017.
    private static final String DALI10_VOCAB;    // DALI 1.0
    private static final String TAPNOTE_VOCAB;   // TAP Implementation Note
    private static final String PRAGMATIC_VOCAB; // Common practice mid-2017
    private static final String DALI11_VOCAB;    // DALI 1.1
    private static final String[] EXAMPLES_VOCABS = new String[] {
        DALI10_VOCAB =    "ivo://ivoa.net/std/DALI#examples",
        TAPNOTE_VOCAB =   "ivo://ivoa.net/std/DALI-examples",
        PRAGMATIC_VOCAB = "ivo://ivoa.net/std/DALI-examples#",
        DALI11_VOCAB =    "http://www.ivoa.net/rdf/examples#",
    };

    /**
     * Constructor.
     *
     * @param   tapRunner  runs TAP queries; if null, no queries attempted
     * @param   capHolder  provides capability metadata at runtime
     * @param   metaHolder provides table metadata at runtime
     */
    public ExampleStage( TapRunner tapRunner, CapabilityHolder capHolder,
                         MetadataHolder metaHolder ) {
        tapRunner_ = tapRunner;
        capHolder_ = capHolder;
        metaHolder_ = metaHolder;
    }

    public String getDescription() {
        return "Check content of examples document";
    }

    public void run( Reporter reporter, TapService tapService ) {
        URL exUrl = tapService.getExamplesEndpoint();
        ExampleRunner runner =
            new ExampleRunner( reporter, tapService, tapRunner_,
                               capHolder_, metaHolder_ );
        boolean hasExamples;
        try {
            runner.checkExamplesDocument( exUrl );
            hasExamples = true;
        }
        catch ( FileNotFoundException e ) {
            reporter.report( FixedCode.F_EXNO,
                             "No examples document at " + exUrl );
            hasExamples = false;
        }

        /* Check the capabilities declaration matches the reality. */
        StdCapabilityInterface[] intfs = capHolder_.getInterfaces();
        if ( intfs != null ) {
            boolean declaresExamples = false;
            for ( StdCapabilityInterface intf : intfs ) {
                if ( EXAMPLES_STDID.equals( intf.getStandardId() ) ) {
                    declaresExamples = true;
                }
            }
            if ( hasExamples && ! declaresExamples ) {
                reporter.report( FixedCode.E_EXDH,
                                 "Examples endpoint present but undeclared" );
            }
            else if ( declaresExamples && ! hasExamples ) {
                reporter.report( FixedCode.E_EXDH,
                                 "Examples endpoint declared but absent" );
            }
            else {
                assert hasExamples == declaresExamples;
            }
        }

        /* Summarise. */
        if ( hasExamples ) {
            runner.reportSummary();
        }
    }

    /**
     * Returns the value of a named attribute on a given element,
     * or null if no such attribute is present.
     * This differs from the (suprising?) behaviour of the DOM
     * Element.getAttribute method, which returns an empty string in
     * case of attribute absence.
     *
     * @param  el  element
     * @param  attName  attribute name
     * @return   attribute value, or null
     */
    private static String getAttribute( Element el, String attName ) {
        return el.hasAttribute( attName ) ? el.getAttribute( attName ) : null;
    }

    /**
     * Returns (something like) the text form of a DOM element.
     * The element and any descendents should be represented using tags
     * with angle brackets.
     * Intended for report messages.
     *
     * @param  el  element
     * @return   representation of el; may fall back to text content if
     *           there is a problem
     */
    private static String getMarkupContent( Element el ) {
        try {
            Transformer trans =
                TransformerFactory.newInstance().newTransformer();
            StringWriter sw = new StringWriter();
            trans.transform( new DOMSource( el ), new StreamResult( sw ) );
            String content = sw.toString();
            return content.replaceFirst( "^<\\?[Xx][Mm][Ll].*?\\?>", "" );
        }
        catch ( TransformerException e ) {
            return el.getTextContent();
        }
    }

    /**
     * Convenience method to turn a vararg list of strings into an
     * unmodifiable set.
     *
     * @param  items  strings
     * @return  set with specified content
     */
    private static Set<String> createStringSet( String... items ) {
        return Collections
              .unmodifiableSet( new HashSet<String>( Arrays.asList( items ) ) );
    }

    /**
     * Returns a regular expression corresponding to an XML Name.
     *
     * @return  XML Name regex pattern
     */
    private static Pattern createXmlNameRegex() {
        String startCharPat = "_\\p{L}";
        String laterCharPat = startCharPat + "0-9\\-\\.";
        return Pattern.compile( "[" + startCharPat + "]"
                              + "[" + laterCharPat + "]*" );
    }

    /**
     * Reads an XML document from a given URL.
     * If there was some problem, a message is reported and null is returned.
     *
     * @param   reporter  reporter
     * @param   url  document URL
     * @param   reqType   required MIME type information,
     *                    or null if no constraints
     * @return  DOM document or null
     */
    private static Document readXml( Reporter reporter, URL url,
                                     ContentTypeOptions reqType )
            throws SAXException, IOException {
        URLConnection conn = url.openConnection();
        conn.connect();
        InputStream in = conn.getInputStream();
        if ( reqType != null ) {
            reqType.checkType( reporter, conn.getContentType(), url );
        }
        DocumentBuilder db;
        try {
            db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch ( ParserConfigurationException e ) {
            reporter.report( FixedCode.F_UNEX,
                             "Trouble setting up XML parse", e );
            return null;
        }
        db.setErrorHandler( new ReporterErrorHandler( reporter ) );

        /* Treat standard external entities as blank.
         * This is mainly to avoid trying to download the DTD
         * declared in the Document Type Declaration,
         * which is typically from w3.org and has a long timeout. */
        db.setEntityResolver( new EntityResolver() {
            public InputSource resolveEntity( String publicId,
                                              String systemId ) {
                if ( publicId != null && publicId.indexOf( "DTD" ) > 0 ||
                     systemId != null && systemId.indexOf( "w3.org" ) > 0 ) {
                    return new InputSource( new StringReader( "" ) );
                }
                else {
                    return new InputSource( systemId );
                }
            }
        } );
        return db.parse( in );
    }

    /**
     * Locates elements corresponding to an XPath expression.
     *
     * @param   reporter  reporter
     * @param   contextEl  context element
     * @param  findPath   XPath expression referring to elements
     * @return   array of zero or more elements matching the XPath expression
     */
    private static Element[] xpathElements( Reporter reporter,
                                            Element contextEl,
                                            String findPath ) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nl;
        try {
            nl = (NodeList) xpath.evaluate( findPath, contextEl,
                                            XPathConstants.NODESET );
        }
        catch ( XPathExpressionException e ) {
            reporter.report( FixedCode.F_UNEX,
                             "Bad XPath expression: " + findPath, e );
            return new Element[ 0 ];
        }
        int nn = nl.getLength();
        List<Element> elList = new ArrayList<Element>( nn );
        for ( int i = 0; i < nn; i++ ) {
            Node node = nl.item( i );
            if ( node instanceof Element ) {
                elList.add( (Element) node );
            }
        }
        return elList.toArray( new Element[ 0 ] );
    }

    /**
     * Extracts name/value pairs encoded in an example element
     * as per the @name='generic-parameter' mechanism specified in DALI.
     * Generic-parameters within generic-parameters are ignored.
     *
     * @param  reporter   reporter
     * @param  el   container element
     * @return   list of key/value pairs for comprehensibly-coded
     *           generic-parameters
     */
    private static List<MetaPair> readGenericParams( Reporter reporter,
                                                     Element el ) {
        List<MetaPair> gparamList = new ArrayList<MetaPair>();
        String gparamPath = ".//*[@property='generic-parameter']";
        for ( Element gparamEl : xpathElements( reporter, el, gparamPath ) ) {
            String typeof = getAttribute( gparamEl, "typeof" );
            if ( ! "keyval".equals( typeof ) ) {
                reporter.report( FixedCode.E_GPER,
                                 "@property='generic-parameter' without "
                               + "@typeof='keyval'" );
            }
            List<MetaPair> gpMetas = new ArrayList<MetaPair>();
            for ( Node child = gparamEl.getFirstChild(); child != null;
                  child = child.getNextSibling() ) {
                if ( child instanceof Element ) {
                    gpMetas.addAll( readTextProperties( reporter,
                                                        GP_PLAINTEXT_PROPS,
                                                        (Element) child ) );
                }
            }
            String gpKey = null;
            String gpValue = null;
            for ( MetaPair mp : gpMetas ) {
                if ( "key".equals( mp.key_ ) ) {
                    if ( gpKey != null ) {
                        reporter.report( FixedCode.E_GPER,
                                         "Multiple @property='key' elements "
                                       + "for generic parameter: "
                                       + gpKey + ", " + mp.value_ );
                    }
                    else {
                        gpKey = mp.value_;
                    }
                }
                if ( "value".equals( mp.key_ ) ) {
                    if ( gpValue != null ) {
                        reporter.report( FixedCode.E_GPER,
                                         "Multiple @property='value' elements "
                                       + "for generic parameter: "
                                       + gpValue + ", " + mp.value_ );
                    }
                    else {
                        gpValue = mp.value_;
                    }
                }
            }
            if ( gpKey != null && gpValue != null ) {
                gparamList.add( new MetaPair( gpKey, gpValue ) );
            }
            else if ( gpKey == null && gpValue == null ) {
                String msg = new StringBuffer()
                   .append( "@property='generic-parameter' element without " )
                   .append( "descendents @property='key' " )
                   .append( "and @property='value': " )
                   .append( getMarkupContent( gparamEl ) )
                   .toString();
                reporter.report( FixedCode.E_GPER, msg );
            }
            else {
                String msg = new StringBuffer()
                   .append( "@property='generic-parameter' element without " )
                   .append( "descendents @property='key' " )
                   .append( "or @property='value': " )
                   .append( gpMetas )
                   .toString();
                reporter.report( FixedCode.E_GPER, msg );
            }
        }
        return gparamList;
    }

    /**
     * Extracts name/value pairs encoded within an example element
     * using @property values for name and element content for value.
     * @property='generic-parameter' elements and their descendents
     * are ignored.
     *
     * @param   el  container element
     * @param   propNames   names of plain text properties to be gathered;
     *                      other properties are ignored
     * @return   list of key/value pairs for comprehensibly-coded
     *           RDFa properties
     */
    private static List<MetaPair> readTextProperties( Reporter reporter,
                                                      Set<String> propNames,
                                                      Element el ) {
        List<MetaPair> list = new ArrayList<MetaPair>();
        String propName = getAttribute( el, "property" );
        if ( ! "generic-parameter".equals( propName ) ) {
            if ( propNames.contains( propName ) ) {
                String content = getRdfaPropertyTextValue( reporter, el );
                list.add( new MetaPair( propName, content ) );
            }

            /* Recurse. */
            for ( Node child = el.getFirstChild(); child != null;
                  child = child.getNextSibling() ) {
                if ( child instanceof Element ) {
                    list.addAll( readTextProperties( reporter, propNames,
                                                     (Element) child ) );
                }
            }
        }
        return list;
    }

    /**
     * Returns the text value associated with an element marked
     * as an RDFa Lite property.  If the element content is not plain text,
     * an error is reported.
     *
     * @param  reporter  reporter
     * @param  el  element
     * @return   plain text content or best stab at it; not null
     */
    private static String getRdfaPropertyTextValue( Reporter reporter,
                                                    Element el ) {
        final String value;
        final String property = el.getAttribute( "property" );

        /* As far as I can tell from RDFa, the @href and @src attributes
         * take precedence over element content.  @content does as well,
         * but it is forbidden by RDFa Lite.  However, I'm not 100% certain
         * of this, the RDFa and RDFa Lite standards don't seem very clear
         * to me about how this is resolved. */
        String hrefAtt = getAttribute( el, "href" );
        String srcAtt = getAttribute( el, "src" );
        String contentAtt = getAttribute( el, "content" );
        if ( contentAtt != null ) {
            String msg = new StringBuffer()
               .append( "@content with @property attribute " )
               .append( "forbidden in RDFa Lite: " )
               .append( "<" )
               .append( el.getTagName() )
               .append( " property=\"" )
               .append( property )
               .append( "\"" )
               .append( " content=\"" )
               .append( contentAtt )
               .append( "\"" )
               .append( ">" )
               .toString();
            reporter.report( FixedCode.E_RALC, msg );
        }
        if ( hrefAtt != null ) {
            return hrefAtt;
        }
        else if ( srcAtt != null ) {
            return srcAtt;
        }
        else {
            boolean plainText = true;
            StringBuffer sbuf = new StringBuffer();
            for ( Node child = el.getFirstChild(); child != null;
                  child = child.getNextSibling() ) {
                if ( child instanceof Text ) {
                    sbuf.append( ((Text) child).getData() );
                }
                else {
                    plainText = false;
                }
            }
            if ( ! plainText ) {
                reporter.report( FixedCode.E_PTXT,
                                 "Non-plain text content of element with "
                               + "property=\"" + property + "\": "
                               + getMarkupContent( el ) );
            }
            return sbuf.toString();
        }
    }

    /**
     * Object that performs example tests, and can accumulate and
     * report statistics on them.
     */
    private static class ExampleRunner {

        private final Reporter reporter_;
        private final TapService tapService_;
        private final TapRunner tapRunner_;
        private final CapabilityHolder capHolder_;
        private final MetadataHolder metaHolder_;
        private final TestCount syntaxValidCount_;
        private final TestCount symbolValidCount_;
        private final TestCount executedCount_;
        private final Set<String> exampleDocUrls_;
        private int docCount_;
        private int exampleCount_;
        private AdqlValidatorKit vkit_;

        /**
         * Constructor.
         * @param   reporter   reporter
         * @param   tapService    TAP service description
         * @param   tapRunner  runs TAP queries; if null, no queries attempted
         * @param   capHolder  provides capability metadata at runtime
         * @param   metaHolder provides table metadata at runtime
         */
        ExampleRunner( Reporter reporter, TapService tapService,
                       TapRunner tapRunner, CapabilityHolder capHolder,
                       MetadataHolder metaHolder ) {
            reporter_ = reporter;
            tapService_ = tapService;
            tapRunner_ = tapRunner;
            capHolder_ = capHolder;
            metaHolder_ = metaHolder;
            exampleDocUrls_ = new HashSet<String>();
            syntaxValidCount_ = new TestCount( "Syntax validity" );
            symbolValidCount_ = new TestCount( "Symbol validity" );
            executedCount_ = new TestCount( "Execution" );
        }

        /**
         * Does the work of checking an examples document read from a 
         * given location.
         * Most conditions are handled and reported, but if the document
         * does not exist (e.g. 404) a FileNotFoundException is thrown.
         *
         * @param  exUrl   URL of DALI/TAP examples document
         * @throws  FileNotFoundException  if document at exUrl does not exist
         */
        public void checkExamplesDocument( URL exUrl )
                throws FileNotFoundException {
            Document doc = null;
            reporter_.report( FixedCode.I_EURL,
                              "Reading examples document from " + exUrl );
            try {
                doc = readXml( reporter_, exUrl, CTYPE_XHTML );
            }
            catch ( SAXException e ) {
                reporter_.report( FixedCode.E_EXPA,
                                  "Examples document not well-formed X(HT)ML"
                                + " at " + exUrl, e );
            }
            catch ( FileNotFoundException e ) {
                throw e;
            }
            catch ( IOException e ) {
                reporter_.report( FixedCode.E_EXIO,
                                  "Error reading examples document at " + exUrl,
                                  e );
            }
            if ( doc == null ) {
                return;
            }

            /* Extract the element within which we will look for examples. */
            Element examplesEl = getExamplesElement( doc );
            if ( examplesEl == null ) {
                return;
            }
            docCount_++;

            /* Use available metadata to perform some of the tests. */
            getAdqlValidatorKit();

            /* Get continuation URLs. */
            URL[] continuationUrls = getContinuationUrls( examplesEl, exUrl );

            /* Get example elements. */
            Element[] exEls = xpathElements( reporter_, examplesEl,
                                             ".//*[@typeof='example']" );
            if ( exEls.length == 0 && continuationUrls.length == 0 ) {
                reporter_.report( FixedCode.W_EX00,
                                  "No examples found in examples document "
                                + exUrl );
            }

            /* Check each in turn. */
            int iex = 0;
            for ( Element exEl : exEls ) {

                /* Acquire example object from DOM element. */
                Example ex = createExample( exEl, iex++ );
                if ( ex != null ) {

                    /* Perform validation. */
                    ExampleStatus status = validateExample( ex );

                    /* Accumulate statistics for later summary. */
                    exampleCount_++;
                    syntaxValidCount_.addResult( status.getSyntaxValid() );
                    symbolValidCount_.addResult( status.getSymbolValid() );
                    executedCount_.addResult( status.getExecuted() );
                }
            }

            /* Recurse into any continuation documents. */
            for ( URL contUrl : continuationUrls ) {
                try {
                    checkExamplesDocument( contUrl );
                }
                catch ( FileNotFoundException e ) {
                    reporter_.report( FixedCode.E_CTNO,
                                      "Continuation document \"" + contUrl
                                    + "\" not found", e );
                }
            }
        }

        /**
         * Issues summary reports on activity of this runner to date.
         */
        public void reportSummary() {
            String countSummary = new StringBuffer()
               .append( "Found " )
               .append( exampleCount_ )
               .append( exampleCount_ == 1 ? " example" : " examples" )
               .append( " in " )
               .append( docCount_ )
               .append( docCount_ == 1 ? " document" : " documents" )
               .toString();
            reporter_.report( FixedCode.S_XNUM, countSummary );
            String testSummary = new StringBuffer()
               .append( syntaxValidCount_ )
               .append( ", " )
               .append( symbolValidCount_ )
               .append( ", " )
               .append( executedCount_ )
               .toString();
            reporter_.report( FixedCode.S_XVAL, testSummary );
        }

        /**
         * Retrieves the element within which examples should be looked for
         * in a DALI examples document.
         * If there's an unrecoverable problem, it is reported
         * and null is returned.
         *
         * @param  doc      containing document
         * @return  examples element or null
         */
        private Element getExamplesElement( Document doc ) {

            /* Locate RDFa vocab attributes. */
            Element docEl = doc.getDocumentElement();
            Element[] vocabEls =
                xpathElements( reporter_, docEl, "//@vocab/.." );
            int nvocab = vocabEls.length;

            /* Look for the one mandated by DALI examples. */
            if ( nvocab == 1 ) {
                Element examplesEl = vocabEls[ 0 ];
                String vatt = getAttribute( examplesEl, "vocab" );
                String msgIntro =
                    "Examples document vocab attribute \"" + vatt + "\"";
                String workWithTopcat =
                    "work with some TOPCAT versions " +
                    "(v4.3-4.4 require \"" + PRAGMATIC_VOCAB + "\", " +
                    "later versions don't care)";
                if ( DALI10_VOCAB.equals( vatt ) ||
                     DALI11_VOCAB.equals( vatt ) ) {
                    String daliVersion = DALI11_VOCAB.equals( vatt )
                                       ? "1.1" : "1.0";
                    String msg = new StringBuffer()
                       .append( msgIntro )
                       .append( " is correct according to DALI " )
                       .append( daliVersion )
                       .append( ", but won't " )
                       .append( workWithTopcat )
                       .append( " :-(." )
                       .toString();
                    reporter_.report( FixedCode.I_EXVT, msg );
                }
                else if ( TAPNOTE_VOCAB.equals( vatt ) ||
                          PRAGMATIC_VOCAB.equals( vatt ) ) {
                    String msg = new StringBuffer()
                       .append( msgIntro )
                       .append( " is contrary to DALI, but is required to " )
                       .append( workWithTopcat )
                       .append( " :-(." )
                       .toString();
                    reporter_.report( FixedCode.W_EXVC, msg );
                }
                else {
                    assert ! Arrays.asList( EXAMPLES_VOCABS ).contains( vatt );
                    String msg = new StringBuffer()
                       .append( msgIntro )
                       .append( " wrong, should probably be \"" )
                       .append( DALI11_VOCAB )
                       .append( "\" (DALI 1.1)" )
                       .append( " or maybe \"" )
                       .append( PRAGMATIC_VOCAB )
                       .append( "\" (to work with some TOPCAT versions)" )
                       .toString();
                    reporter_.report( FixedCode.E_EXVC, msg );
                }
                return examplesEl;
            }

            /* Missing vocab is probably a mistake, try looking for examples
             * anyway. */
            if ( nvocab == 0 ) {
                reporter_.report( FixedCode.E_NOVO,
                                  "No vocab attributes in examples document"
                                + " - will examine whole doc" );
                return docEl;
            }

            /* DALI forbids multiple vocabs, but if they are there, try to
             * identify a likely looking container. */
            else {
                assert nvocab > 1;
                List<String> vocabs = new ArrayList<String>();
                for ( Element vel : vocabEls ) {
                    vocabs.add( getAttribute( vel, "vocab" ) );
                }
                String errMsg = "Multiple vocab attributes in examples document"
                              + vocabs;
                int exIx = -1;
                for ( int iv = 0; iv < vocabEls.length; iv++ ) {
                    if ( Arrays.asList( EXAMPLES_VOCABS )
                        .contains( getAttribute( vocabEls[ iv ], "vocab" ) ) ) {
                        exIx = iv;
                    }
                }
                final String infoMsg;
                final Element examplesEl;
                if ( exIx >= 0 ) {
                    examplesEl = vocabEls[ exIx ];
                    infoMsg = "will use " + getAttribute( examplesEl, "vocab" );
                }
                else {
                    infoMsg = "will examine whole doc";
                    examplesEl = docEl;
                }
                reporter_.report( FixedCode.E_TOVO, errMsg + " - " + infoMsg );
                return examplesEl;
            }
        }

        /**
         * Returns a list of URLs for examples sub-documents referenced using
         * property="continuation" RDFa assertions in the supplied element.
         * 
         * @param  el   element which may contain continuation references
         * @param  contextUrl  URL of the containing document,
         *                     used to resolve relative URLs in href values
         * @return   list of zero or more URLs of continuation
         *           examples documents
         */
        private URL[] getContinuationUrls( Element el, URL contextUrl ) {
            List<URL> urls = new ArrayList<URL>();
            Element[] contEls =
                xpathElements( reporter_, el,
                               ".//*[@property='continuation']" );
            for ( Element contEl : contEls ) {
                String resourceAtt = getAttribute( contEl, "resource" );
                if ( resourceAtt != null ) {
                    String msg = new StringBuffer()
                       .append( "Attribute resource=\"" )
                       .append( resourceAtt )
                       .append( "\" for property=\"continuation\" element" )
                       .append( " should be absent" )
                       .append( " (see DALI1.1 Erratum #1," )
                       .append( " not empty as DALI1.1 sec 2.3.4" )
                       .append( " original text)" )
                       .toString();
                    reporter_.report( FixedCode.E_EXCR, msg );
                }
                String hrefAtt = getAttribute( contEl, "href" );
                URL url;
                try {
                    url = hrefAtt == null ? new URL( "" )
                                          : new URL( contextUrl, hrefAtt );
                }
                catch ( MalformedURLException e ) {
                    url = null;
                    String msg = new StringBuffer()
                       .append( hrefAtt == null || hrefAtt.length() == 0
                              ? "Missing @href attribute"
                              : "@href attribute \"" + hrefAtt
                                 + "\" not a URL" )
                       .append( " for property=\"continuation\" element" )
                       .toString();
                    reporter_.report( FixedCode.E_EXCH, msg );
                }
                if ( url != null ) {
                    if ( exampleDocUrls_.add( url.toString() ) ) {
                        urls.add( url );
                    }
                    else {
                        reporter_.report( FixedCode.W_MLTR,
                                          "Multiple references (loop?) "
                                        + "to continuation document : " + url );
                    }
                }
            }
            return urls.toArray( new URL[ 0 ] );
        }

        /**
         * Takes an element presumed to include example markup ant tries to
         * parse it as an Example object.  Any problems are reported.
         * If no usable example can be generated, null is returned.
         *
         * @param   exEl   element containing RDFa example markup
         * @param   iseq   running index of invocations
         *                 (first invocation is zero)
         * @return  usable example object (contains a non-empty query), or null
         */
        private Example createExample( Element exEl, int iseq ) {
            AdqlValidatorKit vkit = getAdqlValidatorKit();

            /* Stash some report messages until we have the example label
             * to associate them with. */
            HoldReporter stashReporter = new HoldReporter();

            /* Read RDFa id and resource attributes. */
            String idAtt = getAttribute( exEl, "id" );
            String resourceAtt = getAttribute( exEl, "resource" );

            /* Read key/value pairs encoded in @property='generic-parameter'
             * elements, as specified by DALI.  LANG and QUERY are of interest
             * here, though others may be present. */
            List<MetaPair> gparamList =
                readGenericParams( stashReporter, exEl );
            String gpLang = null;
            String gpQuery = null;
            int ngpLang = 0;
            int ngpQuery = 0;
            for ( MetaPair gparam : gparamList ) {
                String gpkey = gparam.key_;
                String gpvalue = gparam.value_;
                if ( "LANG".equalsIgnoreCase( gpkey ) ) {
                    ngpLang++;
                    gpLang = gpvalue;
                }
                if ( "QUERY".equalsIgnoreCase( gpkey ) ) {
                    ngpQuery++;
                    gpQuery = gpvalue;
                }
            }

            /* Read key/value pairs encoded by custom @property attributes;
             * those of interest here are "name" specified by DALI,
             * and "query" and "table" specified by TAP 1.1.
             * We should maybe also look at "capability". */
            List<MetaPair> tpropList =
                readTextProperties( stashReporter, BODY_PLAINTEXT_PROPS, exEl );
            int ntpName = 0;
            int ntpQuery = 0;
            String tpQuery = null;
            String tpName = null;
            List<String> tpTables = new ArrayList<String>();
            for ( MetaPair tprop : tpropList ) {
                String tpkey = tprop.key_;
                String tpvalue = tprop.value_;
                if ( "name".equals( tpkey ) ) {
                    ntpName++;
                    tpName = tpvalue;
                }
                if ( "query".equals( tpkey ) ) {
                    ntpQuery++;
                    tpQuery = tpvalue;
                }
                if ( "table".equals( tpkey ) ) {
                    tpTables.add( tpvalue );
                }
            }

            /* Identify the values we need: query string, query language,
             * and example label. */
            final String exQuery = tpQuery != null ? tpQuery : gpQuery;
            final String exLang = gpLang;
            final String exLabel;
            if ( idAtt != null ) {
                exLabel = idAtt;
            }
            else if ( tpName != null ) {
                exLabel = "\"" + tpName + "\"";
            }
            else {
                exLabel = "#" + ( iseq + 1 );
            }

            /* Announce example under consideration, and emit relevant reports
             * accumulated about it so far.  We have to do it like this,
             * since we didn't have the example label before. */
            reporter_.report( FixedCode.I_EXMP,
                              "Examining example #" + ( iseq + 1 )
                            + ": " + exLabel );
            stashReporter.dumpReports( reporter_ );
            stashReporter = null;

            /* Report on required attributes. */
            if ( ntpName != 1 ) {
                reporter_.report( FixedCode.E_XPRM,
                                  "Example " + exLabel
                                + " has no unique @property='name' "
                                + "(" + ntpName + " present)" );
            }
            if ( ntpQuery > 1 ) {
                reporter_.report( FixedCode.E_XPRM,
                                  "Example " + exLabel
                                + " has no unique @property='query' "
                                + "(" + ntpQuery + " present)" );
            }

            /* Report on RDFa id and resource attributes. */
            if ( idAtt == null ) {
                reporter_.report( FixedCode.E_XID0,
                                  "Missing id attribute on example "
                                + exLabel );
            }
            else if ( ! XMLNAME_REGEX.matcher( idAtt ).matches() ) {
                reporter_.report( FixedCode.W_NMID,
                                  "Example @id=\"" + idAtt + "\" is not "
                                + "an XML Name - probably illegal for element "
                                + exEl.getTagName() );
            }

            /* Report on RDFa @resource attribute. */
            if ( resourceAtt == null ) {
                reporter_.report( FixedCode.E_XRS0,
                                  "Missing resource attribute on example " 
                                + exLabel );
            }
            else if ( idAtt != null &&
                      ! resourceAtt.equals( "#" + idAtt ) ) {
                reporter_.report( FixedCode.E_XRS1,
                                  "Resource/id attribute mismatch on example: "
                                + '"' + resourceAtt + "\" != \"#"
                                + idAtt + '"' );
            }

            /* Report on multiplicity of known generic-parameter values. */
            if ( ngpLang > 1 ) {
                reporter_.report( FixedCode.E_XPRM,
                                  "Multiple LANG generic-parameters "
                                + "in example " + exLabel );
            }
            if ( ngpQuery > 1 ) {
                reporter_.report( FixedCode.E_XPRM,
                                  "Multiple QUERY generic-parameters "
                                + "in example " + exLabel );
            }

            /* Report on tables referenced by name. */
            for ( String tname : tpTables ) {
                if ( ! vkit.hasTable( tname ) ) {
                    reporter_.report( FixedCode.E_UKTB,
                                      "Unknown table \"" + tname + "\" "
                                    + "referenced with property=\"table\" "
                                    + "in example " + exLabel );
                }
            }

            /* Return an example object if possible. */
            if ( exQuery != null ) {
                return new Example() {
                    public String getQuery() {
                        return exQuery;
                    }
                    public String getLang() {
                        return exLang;
                    }
                    @Override
                    public String toString() {
                        return exLabel;
                    }
                };
            }
            else {
                reporter_.report( FixedCode.E_XPRM,
                                  "Example " + exLabel + " has no query text, "
                                + "cannot test" );
                return null;
            }
        }

        /**
         * Performs validation checks on an example.
         *
         * @param  ex   example object
         * @return   summary of validation outcomes
         */
        private ExampleStatus validateExample( Example ex ) {
            AdqlValidatorKit vkit = getAdqlValidatorKit();
            String query = ex.getQuery();

            /* Check basic ADQL syntax. */
            AdqlValidator syntaxValidator = vkit.getSyntaxValidator();
            boolean syntaxOk;
            try {
                syntaxValidator.validate( query );
                syntaxOk = true;
            }
            catch ( Throwable e ) {
                String errmsg = e.getMessage().replaceAll( "\n", " " );
                reporter_.report( FixedCode.W_EXVL,
                                  "Validation syntax error for example " + ex
                                + ": " + errmsg );
                syntaxOk = false;
            }
            final Boolean syntaxValid = Boolean.valueOf( syntaxOk );

            /* Maybe check with known symbols as well. */
            final Boolean symbolValid;
            boolean hasUploads =
                query.toUpperCase().indexOf( "TAP_UPLOAD" ) >= 0;
            if ( syntaxOk && ! hasUploads ) {
                AdqlValidator symbolValidator =
                    vkit.getValidator( ex.getLang() );
                boolean symbolOk;
                try {
                    symbolValidator.validate( query );
                    symbolOk = true;
                }
                catch ( Throwable e ) {
                    String errmsg = e.getMessage().replaceAll( "\n", " " );
                    reporter_.report( FixedCode.W_EXVL,
                                      "Validation symbol error for example "
                                    + ex + ": " + errmsg );
                    symbolOk = false;
                }
                symbolValid = Boolean.valueOf( symbolOk );
            }
            else {
                symbolValid = null;
            }

            /* Maybe attempt to execute the query. */
            final Boolean executed;
            if ( ! hasUploads && tapRunner_ != null && tapService_ != null ) {
                Map<String,String> extraParams =
                    new LinkedHashMap<String,String>();
                if ( QUERY_MAXREC >= 0 ) {
                    extraParams.put( "MAXREC",
                                     Integer.toString( QUERY_MAXREC ) );
                }
                TapQuery tq = new TapQuery( tapService_, query, extraParams );
                StarTable table;
                try {
                    table = tapRunner_.attemptGetResultTable( reporter_, tq );
                }
                catch ( IOException e ) {
                    reporter_.report( FixedCode.W_QERR,
                                      "Example query execution failed", e );
                    table = null;
                }
                catch ( SAXException e ) {
                    reporter_.report( FixedCode.E_QERX,
                                      "TAP query result parse failed", e );
                    table = null;
                }
                executed = Boolean.valueOf( table != null );
            }
            else {
                executed = null;
            }

            /* Return result. */
            return new ExampleStatus() {
                public Boolean getSyntaxValid() {
                    return syntaxValid;
                }
                public Boolean getSymbolValid() {
                    return symbolValid;
                }
                public Boolean getExecuted() {
                    return executed;
                }
            };
        }

        /**
         * Returns a lazily created validator kit for this runner.
         * This is so we can control when it's created (it may not
         * actually be needed, and in that case creating one could
         * cause confusing report messages).
         *
         * @return   validator kit
         */
        private AdqlValidatorKit getAdqlValidatorKit() {
            if ( vkit_ == null ) {
                vkit_ = AdqlValidatorKit
                       .createInstance( reporter_,
                                        metaHolder_.getTableMetadata(),
                                        capHolder_.getCapability() );
            }
            return vkit_;
        }
    }

    /**
     * Characterises that information about an example in an examples document
     * required to perform tests on it.
     */
    private interface Example {

        /**
         * Returns the query for this example.
         *
         * @return   non-null query
         */
        String getQuery();

        /**
         * Returns an explicitly supplied specifier for the language
         * in which the query is written.
         *
         * @return  explicitly supplied language identifier, or null
         */
        String getLang();
    }

    /**
     * Summarises validation outcomes for an example.
     */
    private interface ExampleStatus {

        /**
         * Outcome of query syntax validity test.
         *
         * @return  TRUE for success, FALSE for failure, null for no attempt
         */
        Boolean getSyntaxValid();

        /**
         * Outcome of query symbol validity test.
         *
         * @return  TRUE for success, FALSE for failure, null for no attempt
         */
        Boolean getSymbolValid();

        /**
         * Outcome of query execution.
         *
         * @return  TRUE for success, FALSE for failure, null for no attempt
         */
        Boolean getExecuted();
    }

    /**
     * Accumulates and summarises test outcomes.
     */
    private static class TestCount {
        final String name_;
        private int nTry_;
        private int nSucceed_;

        /**
         * Constructor.
         *
         * @param  name  human-readable name for counted quantity
         */
        TestCount( String name ) {
            name_ = name;
        }

        /**
         * Accumulates results of a test.
         *
         * @param  result: TRUE for success, FALSE for failure,
         *                 null for not attempted
         */
        void addResult( Boolean result ) {
            if ( result != null ) {
                nTry_++;
                if ( result.booleanValue() ) {
                    nSucceed_++;
                }
            }
        }

        /**
         * Summarises count.
         */
        @Override
        public String toString() {
            return new StringBuffer()
                  .append( name_ )
                  .append( " success/attempt: " )
                  .append( nSucceed_ )
                  .append( "/" )
                  .append( nTry_ )
                  .toString();
        }
    }

    /**
     * Key/value pair.
     */
    private static class MetaPair {
        final String key_;
        final String value_;

        /**
         * Constructor.
         *
         * @param   key  key
         * @param  value  value
         */
        MetaPair( String key, String value ) {
            key_ = key;
            value_ = value;
        }

        @Override
        public String toString() {
            return key_ + "=" + value_;
        }
    }
}
