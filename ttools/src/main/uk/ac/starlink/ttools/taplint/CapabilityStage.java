package uk.ac.starlink.ttools.taplint;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.vo.TapCapability;
import uk.ac.starlink.vo.TapLanguage;
import uk.ac.starlink.vo.TapLanguageFeature;
import uk.ac.starlink.vo.TapQuery;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Stage for checking content of TAPRegExt capability metadata.
 *
 * @author   Mark Taylor
 * @since    3 Jun 2011
 * @see     <a href="http://www.ivoa.net/Documents/TAPRegExt/index.html"
 *             >IVOA TAPRegExt Standard</a>
 */
public class CapabilityStage implements Stage, CapabilityHolder {

    private TapCapability tcap_;

    private static final String ADQL2_ID = "ivo://ivoa.net/std/ADQL#v2.0";
    private static final Pattern UDF_FORM_REGEX =
        Pattern.compile( "([A-Za-z][A-Za-z0-9_]*)"
                       + "\\s*\\((.*)\\)\\s*->\\s*(.*)" );
    private static final String TOKEN_REGEX = "[^()<>@,;:\\\"/\\[\\]\\?=\\s]+";
    private static final Pattern OUT_MIME_REGEX =
        Pattern.compile( "(text|image|audio|video|application|x-" + TOKEN_REGEX
                       + ")/" + TOKEN_REGEX + "\\s*(;.*)?",
                         Pattern.CASE_INSENSITIVE );

    public String getDescription() {
        return "Check content of TAPRegExt capabilities record";
    }

    /**
     * Returns the TAP capability record obtained by the last run of this stage.
     *
     * @return   tap capability object
     */
    public TapCapability getCapability() {
        return tcap_;
    }

    public void run( Reporter reporter, URL serviceUrl ) {
        URL capUrl;
        try {
            capUrl = new URL( serviceUrl + "/capabilities" );
        }
        catch ( MalformedURLException e ) {
            capUrl = null;
            reporter.report( ReportType.FAILURE, "CAIO",
                             "Bad URL " + serviceUrl + "?", e );
        }
        tcap_ = checkCapabilities( reporter, capUrl );
    }

    /**
     * Performs validation checks on a TAPRegExt document at a given URL.
     *
     * @param   reporter  destination for validation messages
     * @param   capUrl  URL of a Capabilities document
     */
    public static TapCapability checkCapabilities( Reporter reporter,
                                                   URL capUrl ) {

        /* Attempt to read a TapCapability object from the URL.
         * If it can't be done, give up now. */
        final TapCapability tcap;
        reporter.report( ReportType.INFO, "CURL",
                         "Reading capability metadata from " + capUrl );
        try {
            tcap = TapCapability.readTapCapability( capUrl );
        }
        catch ( SAXException e ) {
            reporter.report( ReportType.ERROR, "FLSX",
                             "Error parsing capabilities metadata", e );
            return null;
        }
        catch ( IOException e ) {
            reporter.report( ReportType.ERROR, "FLIO",
                             "Error reading capabilities metadata", e );
            return null;
        }

        /* Read a DOM from the same place.  This shouldn't fail if the
         * previous step succeeded, but if it does, just report the failure
         * and carry on with a null DOM; some later checks won't get run. */
	Document capsDoc = null; 
        try {
            capsDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                     .parse( new BufferedInputStream( capUrl.openStream() ) );
        }
        catch ( IOException e ) {
            reporter.report( ReportType.ERROR, "CAIO",
                             "Error reading capabilities from " + capUrl, e );
        }
        catch ( SAXException e ) {
            reporter.report( ReportType.ERROR, "CAXM",
                             "Error parsing capabilities from " + capUrl, e );
        }
        catch ( ParserConfigurationException e ) {
            reporter.report( ReportType.FAILURE, "CAPC",
                             "Error parsing capabilities XML from " + capUrl,
                             e );
        }

        /* Do the work. */
        new CapabilityRunner( reporter, tcap, capsDoc ).run();
        return tcap;
    }

    /**
     * Does the work for the Capability stage.
     */
    private static class CapabilityRunner implements Runnable {
        private final Reporter reporter_;
        private final TapCapability tcap_;
        private final Document capsDoc_;

        /**
         * Constructor.
         *
         * @param  reporter  validation message destination
         * @param  tcap  TAP capability object
         * @param  capsDoc  DOM of a Capabilities document
         */
        CapabilityRunner( Reporter reporter, TapCapability tcap,
                          Document capsDoc ) {
            reporter_ = reporter;
            tcap_ = tcap;
            capsDoc_ = capsDoc;
        }

        public void run() {
            checkLanguages();
            checkUploadMethods();
            checkOutputFormats();
        }

        /**
         * Checks that language declarations are correct.
         */
        private void checkLanguages() {

            /* Check at least one language is present. */
            TapLanguage[] languages = tcap_.getLanguages();
            if ( languages.length == 0 ) {
                reporter_.report( ReportType.ERROR, "NOQL",
                                  "No query languages declared" );
                return;
            }

            /* Go through each language and locate the ADQL2 one if present. */
            boolean hasAdql2 = false;
            boolean hasAdql = false;
            for ( int il = 0; il < languages.length; il++ ) {
                TapLanguage lang = languages[ il ];
                String langName = lang.getName();
                boolean isAdql2 = false;
                if ( "ADQL".equals( langName ) ) {
                    hasAdql = true;
                    int nvers = lang.getVersions().length;
                    assert nvers == lang.getVersionIds().length;
                    for ( int iv = 0; iv < nvers; iv++ ) {
                        String vname = lang.getVersions()[ iv ];
                        if ( vname == null || vname.trim().length() == 0 ) {
                            String msg = new StringBuffer()
                               .append( "Language " )
                               .append( langName )
                               .append( " has empty version string" )
                               .toString();
                            reporter_.report( ReportType.WARNING, "LVAN", msg );
                        }
                        String vid = lang.getVersionIds()[ iv ];
                        boolean isNumber2 = "2.0".equals( vname );
                        boolean hasId2 = ADQL2_ID.equals( vid );
                        isAdql2 = isNumber2 || hasId2;
                        if ( isNumber2 && 
                             ( vid == null || vid.trim().length() == 0 ) ) {
                            String msg = new StringBuffer()
                               .append( "Language " )
                               .append( langName )
                               .append( " has version " )
                               .append( vname )
                               .append( " without ivo-id=\"" )
                               .append( ADQL2_ID )
                               .append( "\"" )
                               .toString();
                            reporter_.report( ReportType.WARNING, "A2MN", msg );
                        }
                        else if ( isNumber2 && ! hasId2 ) {
                            String msg = new StringBuffer()
                               .append( "Language " )
                               .append( langName )
                               .append( " has version " )
                               .append( vname )
                               .append( " with non-standard ivo-id=\"" )
                               .append( vid )
                               .append( "\"" )
                               .append( " != \"" )
                               .append( ADQL2_ID )
                               .append( "\"" )
                               .toString();
                            reporter_.report( ReportType.WARNING, "A2MX", msg );
                        }
                        else if ( ! isNumber2 && hasId2 ) {
                            String msg = new StringBuffer()
                               .append( "Language Version with ivo-id=\"" )
                               .append( vid )
                               .append( "\" is " )
                               .append( langName )
                               .append( "-" )
                               .append( vname )
                               .append( " not " )
                               .append( "ADQL-2.0" )
                               .toString();
                            reporter_.report( ReportType.ERROR, "A2XI", msg );
                        }
                    }
                }
                hasAdql2 = hasAdql2 || isAdql2;

                /* Check non-standard/optional features of this language. */
                checkFeatures( lang, isAdql2 );
            }

            /* Report on presence of (required) ADQL 2. */
            if ( ! hasAdql ) {
                reporter_.report( ReportType.ERROR, "ADQX",
                                  "ADQL not declared as a query language" );
            }
            else if ( ! hasAdql2 ) {
                reporter_.report( ReportType.WARNING, "AD2X",
                                  "ADQL-2.0 not declared as a query language" );
            }
        }

        /**
         * Checks that language feature declarations are correct.
         *
         * @param  language   language declaration object
         * @param  isAdql2   true iff <code>language</code> represents ADQL v2.0
         */
        private void checkFeatures( TapLanguage language, boolean isAdql2 ) {
            String langName = language.getName();
            String[] versions = language.getVersions();
            if ( versions.length == 1 ) {
                langName += "-" + versions[ 0 ];
            }
            Map<String,TapLanguageFeature[]> featuresMap =
                language.getFeaturesMap();
            for ( String ftype : featuresMap.keySet() ) {
                TapLanguageFeature[] features = featuresMap.get( ftype );
                if ( TapCapability.UDF_FEATURE_TYPE.equals( ftype ) ) {
                    checkUdfs( langName, features );
                }
                else if ( TapCapability.ADQLGEO_FEATURE_TYPE.equals( ftype ) ) {
                    checkAdqlGeoms( langName, features );
                }
                else if ( ftype.startsWith( TapCapability
                                           .TAPREGEXT_STD_URI ) ) {
                    String msg = new StringBuffer()
                       .append( "Unknown standard feature key \"" )
                       .append( ftype )
                       .append( "\" for language " )
                       .append( langName )
                       .toString();
                    reporter_.report( ReportType.ERROR, "KEYX", msg );
                }
                else {
                    String msg = new StringBuffer()
                       .append( "Custom feature type \"" )
                       .append( ftype )
                       .append( "\" for language " )
                       .append( langName )
                       .toString();
                    reporter_.report( ReportType.WARNING, "CULF", msg );
                }
            }
        }

        /**
         * Checks that language features declaring User Defined Fuctions
         * are correct.
         *
         * @param  langName  language name, suitable for human consumption
         * @parma  features  language features declaring UDFs
         */
        private void checkUdfs( String langName,
                                TapLanguageFeature[] features ) {
            for ( int ifeat = 0; ifeat < features.length; ifeat++ ) {
                String form = features[ ifeat ].getForm();
                if ( form == null ||
                     ! UDF_FORM_REGEX.matcher( form.trim() ).matches() ) {
                    String msg = new StringBuffer()
                       .append( "Declared " )
                       .append( langName )
                       .append( " UDF " )
                       .append( " has wrong form \"" )
                       .append( form )
                       .append( "\"" )
                       .append( " not \"" )
                       .append( "f(a T[, ...]) -> T)" )
                       .append( "\"" )
                       .toString();
                    reporter_.report( ReportType.ERROR, "UDFE", msg );
                }
            }
        }

        /**
         * Checks that language features declaring ADQL Geometry functions
         * are correct.
         *
         * @param  langName  language name, suitable for human consumption
         * @param  features  language features declaring ADQL v2 geometry
         *                   functions
         */
        private void checkAdqlGeoms( String langName,
                                     TapLanguageFeature[] features ) {
            Collection<String> geomSet =
                    new HashSet<String>( Arrays.asList( new String[] {
                "AREA", "BOX", "CENTROID", "CIRCLE", "CONTAINS", "COORD1",
                "COORD2", "COORDSYS", "DISTANCE", "INTERSECTS", "POINT",
                "POLYGON", "REGION",
            } ) );
            for ( int ifeat = 0; ifeat < features.length; ifeat++ ) {
                String form = features[ ifeat ].getForm();
                if ( ! geomSet.contains( form ) ) {
                    String msg = new StringBuffer()
                       .append( "Declared " )
                       .append( langName )
                       .append( " geometry function \"" )
                       .append( form )
                       .append( "\" unknown" )
                       .toString();
                    reporter_.report( ReportType.ERROR, "GEOX", msg );
                }
            }
        }

        /**
         * Checks that upload methods are declared correctly.
         */
        private void checkUploadMethods() {
            String[] upMethods = tcap_.getUploadMethods();
            String stdPrefix = TapCapability.TAPREGEXT_STD_URI + "#upload-";
            Collection<String> mandatorySuffixList = // TAP 2.5.{1,2}
                Arrays.asList( new String[] { "inline", "http" } );
            Collection<String> stdSuffixList =
                Arrays.asList( new String[] { "inline", "http",
                                              "https", "ftp" } );
            for ( int iu = 0; iu < upMethods.length; iu++ ) {
                String upMethod = upMethods[ iu ];
                if ( upMethod.startsWith( stdPrefix ) ) {
                    String frag = upMethod.substring( stdPrefix.length() );
                    if ( ! stdSuffixList.contains( frag ) ) {
                        String msg = new StringBuffer()
                           .append( "Unknown suffix \"" )
                           .append( frag )
                           .append( "\" for upload method" )
                           .toString();
                        reporter_.report( ReportType.ERROR, "UPBD", msg );
                    }
                }
                else {
                    String msg = new StringBuffer()
                       .append( "Custom upload method \"" )
                       .append( upMethod )
                       .append( "\"" )
                       .toString();
                    reporter_.report( ReportType.WARNING, "UPCS", msg );
                }
            }
            if ( upMethods.length > 0 ) {
                for ( String msuff : mandatorySuffixList ) {
                    String mmeth = stdPrefix + msuff;
                    if ( ! Arrays.asList( upMethods ).contains( mmeth ) ) {
                        String msg = new StringBuilder()
                           .append( "Mandatory upload method " )
                           .append( mmeth )
                           .append( " not declared" )
                           .append( ", though uploads are " )
                           .append( "apparently supported" )
                           .toString();
                        reporter_.report( ReportType.ERROR, "MUPM", msg );
                    }
                }
            }
        }

        /**
         * Locates and returns the capability element in the capabilities DOM
         * which has a given standard ID.
         *
         * @param   stdId  required standardID attribute value
         * @return  capability element or null
         */
        private Element getCapabilityElement( String stdId ) {
            NodeList capList = capsDoc_.getDocumentElement()
                              .getElementsByTagName( "capability" );
            for ( int ic = 0; ic < capList.getLength(); ic++ ) {
                Element capEl = (Element) capList.item( ic );
                if ( stdId.equals( capEl.getAttribute( "standardID" ) ) ) {
                   return capEl;
                }
            }
            return null;
        }

        /**
         * Checks that output formats are declared correctly.
         */
        private void checkOutputFormats() {
            Element treEl = getCapabilityElement( "ivo://ivoa.net/std/TAP" );
            if ( treEl == null ) {
                reporter_.report( ReportType.ERROR, "TCAP",
                                  "No TAPRegExt capability element" );
                return;
            }
            OutputFormat[] outFormats = readOutputFormats( treEl );
            if ( outFormats.length == 0 ) {
                reporter_.report( ReportType.ERROR, "NOOF",
                                  "No output formats defined" );
                return;
            }
            Collection<String> outKeyList = Arrays.asList( new String[] {
                "#output-votable-td",
                "#output-votable-binary",
                "#output-votable-binary2",
            } );
            String stdPrefix = TapCapability.TAPREGEXT_STD_URI;
            for ( int iof = 0; iof < outFormats.length; iof++ ) {
                OutputFormat of = outFormats[ iof ];
                String ofName = of.aliases_.length > 0
                              ? of.aliases_[ 0 ]
                              : of.mime_;
                String mime = of.mime_;
                if ( mime == null 
                     || ! OUT_MIME_REGEX.matcher( mime.trim() ).matches() ) {
                    String msg = new StringBuffer()
                       .append( "Illegal MIME type \"" )
                       .append( mime )
                       .append( "\" for output format " )
                       .append( ofName )
                       .toString();
                    reporter_.report( ReportType.ERROR, "BMIM", msg );
                }
                String ivoid = of.ivoid_;
                if ( ivoid != null && ivoid.startsWith( stdPrefix ) &&
                     ! outKeyList
                      .contains( ivoid.substring( stdPrefix.length() ) ) ) {
                    String msg = new StringBuffer()
                       .append( "Unknown standard output format key \"" )
                       .append( ivoid )
                       .append( " for output format " )
                       .append( ofName )
                       .toString();
                    reporter_.report( ReportType.ERROR, "XOFK", msg );
                }
            }
        }

        /**
         * Reads a TAPRegExt capability element to find the declared
         * output formats.
         *
         * @param   treEl  capability element with
         *                 standardID="ivo://ivoa.net/std/TAP"
         * @return   output formats declared
         */
        private OutputFormat[] readOutputFormats( Element treEl ) {
            List<OutputFormat> ofList = new ArrayList<OutputFormat>();
            for ( Node treChild = treEl.getFirstChild(); treChild != null;
                  treChild = treChild.getNextSibling() ) {
                if ( treChild instanceof Element &&
                     "outputFormat".equals( treChild.getNodeName() ) ) {
                    Element ofel = (Element) treChild;
                    String ivoid = ofel.getAttribute( "ivo-id" );
                    String mime = null;
                    List<String> aliasList = new ArrayList<String>();
                    for ( Node ofChild = ofel.getFirstChild(); ofChild != null;
                          ofChild = ofChild.getNextSibling() ) {
                        if ( ofChild instanceof Element ) {
                            Element el = (Element) ofChild;
                            String tname = el.getTagName();
                            if ( "mime".equals( tname ) ) {
                                mime = DOMUtils.getTextContent( el );
                            }
                            if ( "alias".equals( tname ) ) {
                                aliasList.add( DOMUtils.getTextContent( el ) );
                            }
                        }
                    }
                    String[] aliases = aliasList.toArray( new String[ 0 ] );
                    ofList.add( new OutputFormat( mime, aliases, ivoid ) );
                }
            }
            return ofList.toArray( new OutputFormat[ 0 ] );
        }
    }

    /**
     * Aggregates information about a declared output format.
     */
    private static class OutputFormat {
        final String mime_;
        final String[] aliases_;
        final String ivoid_;

        /**
         * Constructor.
         *
         * @param  mime  MIME type
         * @param  aliases   array of alias strings, may be empty
         * @parm  ivoid  identifier URI
         */
        OutputFormat( String mime, String[] aliases, String ivoid ) {
            mime_ = mime;
            aliases_ = aliases;
            ivoid_ = ivoid;
        }

        public String toString() {
            return "OutputFormat: "
                 + "mime: " + mime_ + ", "
                 + "ivoid: " + ivoid_ + ", "
                 + "aliases: " + Arrays.asList( aliases_ );
        }
    }

    /**
     * Can be used for standalone validation of a Capabilities document
     * at a given URL.
     */
    public static void main( String[] args ) throws MalformedURLException {
        if ( args.length != 1 ) {
            System.err.println( "Usage: "
                              + CapabilityStage.class.getName() + ": "
                              + "<cap-doc-url>" );
            System.exit( 1 );
        }
        Reporter reporter = new Reporter( System.out, ReportType.values(), 10,
                                          false, 1024 );
        URL capUrl = new URL( args[ 0 ] );
        checkCapabilities( reporter, capUrl );
    }
}
