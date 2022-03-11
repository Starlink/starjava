package uk.ac.starlink.ttools.taplint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.w3c.dom.Element;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.vo.AdqlVersion;
import uk.ac.starlink.vo.OutputFormat;
import uk.ac.starlink.vo.StdCapabilityInterface;
import uk.ac.starlink.vo.TapCapability;
import uk.ac.starlink.vo.TapLanguage;
import uk.ac.starlink.vo.TapLanguageFeature;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.vo.TapService;
import uk.ac.starlink.vo.TapVersion;
import uk.ac.starlink.vo.UserAgentUtil;

/**
 * Stage for checking content of TAPRegExt capability metadata.
 *
 * @author   Mark Taylor
 * @since    3 Jun 2011
 * @see     <a href="http://www.ivoa.net/Documents/TAPRegExt/index.html"
 *             >IVOA TAPRegExt Standard</a>
 */
public class CapabilityStage implements Stage {

    private final CapabilityHolder capHolder_;

    private static final Pattern UDF_FORM_REGEX =
        Pattern.compile( "([A-Za-z][A-Za-z0-9_]*)"
                       + "\\s*\\((.*)\\)\\s*->\\s*(.*)" );
    private static final String TOKEN_REGEX = "[^()<>@,;:\\\"/\\[\\]\\?=\\s]+";
    private static final Pattern OUT_MIME_REGEX =
        Pattern.compile( "(text|image|audio|video|application|x-" + TOKEN_REGEX
                       + ")/" + TOKEN_REGEX + "\\s*(;.*)?",
                         Pattern.CASE_INSENSITIVE );
    private static final Set<String> NON_VO_SERVERSOFT =
           new HashSet<String>( Arrays.asList( new String[] {
        "apache", "nginx", "twistedweb", "php", "openssl", "mod_jk",
    } ) );

    /**
     * Constructor.
     *
     * @param  capHolder source for capabilities document
     */
    public CapabilityStage( CapabilityHolder capHolder ) {
        capHolder_ = capHolder;
    }

    public String getDescription() {
        return "Check TAP and TAPRegExt content of capabilities document";
    }

    public void run( Reporter reporter, TapService tapService ) {
        checkServerHeader( reporter, capHolder_.getServerHeader() );
        TapCapability tcap = capHolder_.getCapability();
        if ( tcap == null ) {
            reporter.report( FixedCode.F_CAP0, "No TAPRegExt capability" );
        }
        else {
            new TapRegExtRunner( reporter, tcap ).run();
        }
        Cap[] caps = readCaps( reporter, capHolder_.getElement() );
        if ( caps == null ) {
            reporter.report( FixedCode.F_CAP0, "No capabilities" );
        }
        else {
            new CapDocRunner( reporter, tapService, caps ).run();
        }
    }

    /**
     * Examines the content of an HTTP Server header associated with
     * service responses, and reports appropriately.
     *
     * @param  reporter  reporter
     * @param  serverHdr   content of HTTP Server header
     */
    private static void checkServerHeader( Reporter reporter,
                                           String serverHdr ) {
        if ( serverHdr == null || serverHdr.trim().length() == 0 ) {
            reporter.report( FixedCode.W_SVR0, "No HTTP Server header" );
        }
        String qHdr = "\"Server: " + serverHdr + "\"";
        final String[] tokens;
        try {
            tokens = UserAgentUtil.parseProducts( serverHdr );
        }
        catch ( RuntimeException e ) {

            /* RFC 7231 section 5.5.3. */
            reporter.report( FixedCode.E_SVRB,
                             "Bad product list syntax " + qHdr );
            return;
        }
        reporter.report( FixedCode.I_SVRI, "HTTP server header " + qHdr );

        /* Note-SoftID-1.0. */
        List<String> possVoProducts =
            Arrays.stream( tokens )
                  .filter( CapabilityStage::isPossibleVoProduct )
                  .collect( Collectors.toList() );
        if ( possVoProducts.size() == 0 ) {
            String msg = new StringBuffer()
               .append( "No apparent VO service software identification " )
               .append( "in HTTP header " )
               .append( qHdr )
               .append( " - see SoftID Note" )
               .toString();
            reporter.report( FixedCode.W_SVRV, msg );
        }
    }

    /**
     * Examines a word from an HTTP Server header and checks whether it
     * looks like it might be a SoftID-style VO service software component
     * identifier.
     *
     * @param  word  product token or comment from Server header
     * @return  true if it is plausibly a VO software component identifier
     */
    private static boolean isPossibleVoProduct( String word ) {

        /* If empty, it's not a VO component identifier. */
        if ( word == null || word.length() == 0 ) {
            return false;
        }

        /* If it's a comment, it's not a VO component identifier. */
        if ( word.charAt( 0 ) == '(' ) {  // RFC 7230 sec 3.2.6
            // it's a comment
            return false;
        }

        /* If it's one of several common non-VO server component names,
         * it's not a VO component identifier; see RFC 7231 sec 5.5.3. */
        String componentName = word.replaceAll( "/.*", "" ).toLowerCase();
        for ( String nonvoName : NON_VO_SERVERSOFT ) {
            if ( componentName.indexOf( nonvoName.toLowerCase() ) >= 0 ) {
                return false;
            }
        }

        /* Otherwise, it might be. */
        return true;
    }

    /**
     * Read parsed capability information from a Capabilities document element.
     * This yields something similar to the StdCapabilityInterface[] array
     * available from the CapabilityHolder object, but it preserves
     * more of the hierarchical information.
     *
     * @param  reporter   destination for validation messages
     * @param  capsEl   top-level element containing capabilities document
     *                  (presumably a &lt;capabilities&gt; element)
     * @return   hierarchy of capabilities represented by document
     */
    private static Cap[] readCaps( Reporter reporter, Element capsEl ) {
        if ( capsEl == null ) {
            return null;
        }
        List<Cap> caps = new ArrayList<Cap>();
        for ( Element capEl :
              DOMUtils.getChildElementsByName( capsEl, "capability" ) ) {
            Cap cap = new Cap( getAtt( capEl, "standardID" ) );
            caps.add( cap );
            for ( Element intfEl :
                  DOMUtils.getChildElementsByName( capEl, "interface" ) ) {
                Intf intf = new Intf( getAtt( intfEl, "xsi:type" ),
                                      getAtt( intfEl, "role" ),
                                      getAtt( intfEl, "version" ) );
                cap.intfs_.add( intf );
                for ( Element el :
                      DOMUtils.getChildElementsByName( intfEl, null ) ) {
                    String tagName = el.getTagName();
                    if ( "securityMethod".equals( tagName ) ) {
                        SecMeth sm = new SecMeth( getAtt( el, "standardID" ) );
                        intf.secMeths_.add( sm );
                    }
                    else if ( "accessURL".equals( tagName ) ) {
                        intf.accessUrl_ = DOMUtils.getTextContent( el ).trim();
                    }
                }
            }
        }
        return caps.toArray( new Cap[ 0 ] );
    }

    /**
     * Extracts the value of an attribute from an element.
     * Unlike the Element.getAttribute method, it returns null instead of
     * an empty string in case of no attribute present.
     *
     * @param   el  element
     * @param  attName  attribute name
     * @return   attribute value, or null if not present
     */
    private static String getAtt( Element el, String attName ) {
        return el.hasAttribute( attName ) ? el.getAttribute( attName ) : null;
    }

    /**
     * Tests the TapCapability object (content of TAPRegExt part of the
     * capabilities document).
     */
    private static class TapRegExtRunner implements Runnable {
        private final Reporter reporter_;
        private final TapCapability tcap_;

        /**
         * Constructor.
         *
         * @param  reporter  validation message destination
         * @param  tcap  TAP capability object
         */
        TapRegExtRunner( Reporter reporter, TapCapability tcap ) {
            reporter_ = reporter;
            tcap_ = tcap;
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
                reporter_.report( FixedCode.E_NOQL,
                                  "No query languages declared" );
                return;
            }

            /* Go through each language and locate ADQL2 ones if present. */
            boolean hasAdql = false;
            boolean hasAdql2 = false;
            SortedSet<AdqlVersion> adqlVersions = new TreeSet<AdqlVersion>();
            for ( int il = 0; il < languages.length; il++ ) {
                TapLanguage lang = languages[ il ];
                String langName = lang.getName();
                if ( "ADQL".equals( langName ) ) {
                    hasAdql = true;
                    int nvers = lang.getVersions().length;
                    assert nvers == lang.getVersionIds().length;
                    for ( int iv = 0; iv < nvers; iv++ ) {
                        String vname = lang.getVersions()[ iv ];
                        String vid = lang.getVersionIds()[ iv ];
                        if ( vname == null || vname.trim().length() == 0 ) {
                            String msg = new StringBuffer()
                               .append( "Language " )
                               .append( langName )
                               .append( " has empty version string" )
                               .toString();
                            reporter_.report( FixedCode.W_LVAN, msg );
                        }
                        AdqlVersion nameVers = AdqlVersion.byNumber( vname );
                        AdqlVersion idVers = AdqlVersion.byIvoid( vid );
                        if ( nameVers != null ) {
                            adqlVersions.add( nameVers );
                        }
                        if ( idVers != null ) {
                            adqlVersions.add( idVers );
                        }
                        if ( nameVers != null && 
                             ( vid == null || vid.trim().length() == 0 ) ) {
                            String msg = new StringBuffer()
                               .append( "Language " )
                               .append( langName )
                               .append( " has version " )
                               .append( vname )
                               .append( " without ivo-id=\"" )
                               .append( nameVers.getIvoid() )
                               .append( "\"" )
                               .toString();
                            reporter_.report( FixedCode.W_A2MN, msg );
                        }
                        else if ( nameVers != null && idVers == null ) {
                            String msg = new StringBuffer()
                               .append( "Language " )
                               .append( langName )
                               .append( " has version " )
                               .append( vname )
                               .append( " with non-standard ivo-id=\"" )
                               .append( vid )
                               .append( "\"" )
                               .append( " != \"" )
                               .append( nameVers.getIvoid() )
                               .append( "\"" )
                               .toString();
                            reporter_.report( FixedCode.W_A2MX, msg );
                        }
                        else if ( nameVers != null && idVers != null &&
                                  ! nameVers.equals( idVers ) ) {
                            String msg = new StringBuffer()
                               .append( "Language Version with ivo-id=\"" )
                               .append( vid )
                               .append( "\" is " )
                               .append( langName )
                               .append( "-" )
                               .append( vname )
                               .append( " not " )
                               .append( "ADQL " + idVers )
                               .toString();
                            reporter_.report( FixedCode.E_A2XI, msg );
                        }
                    }
                }

                /* Check non-standard/optional features of this language. */
                if ( adqlVersions.size() > 0 ) {
                    hasAdql2 = true;
                    AdqlVersion highestVersion = adqlVersions.last();
                    checkAdqlFeatures( lang, highestVersion );
                }

            }

            /* Report on presence of (required) ADQL 2. */
            if ( ! hasAdql ) {
                reporter_.report( FixedCode.E_ADQX,
                                  "ADQL not declared as a query language" );
            }
            else if ( ! hasAdql2 ) {
                reporter_.report( FixedCode.W_AD2X,
                                  "ADQL-2.0 not declared as a query language" );
            }
        }

        /**
         * Checks that ADQL language feature declarations are correct.
         *
         * @param  language   language declaration object
         * @param  highestVersion highest declared ADQL2.x version, not null
         */
        private void checkAdqlFeatures( TapLanguage language,
                                        AdqlVersion version ) {
            String langName = language.getName();
            String[] versions = language.getVersions();
            if ( versions.length == 1 ) {
                langName += "-" + versions[ 0 ];
            }
            Set<String> versionFeatures =
                new HashSet<String>( Arrays
                                    .asList( version.getFeatureUris() ) );
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
                    if ( ! versionFeatures.contains( ftype ) ) {
                        String msg = new StringBuffer()
                           .append( "Unknown standard feature key \"" )
                           .append( ftype )
                           .append( "\" for language " )
                           .append( langName )
                           .toString();
                        reporter_.report( FixedCode.E_KEYX, msg );
                    }
                }
                else {
                    String msg = new StringBuffer()
                       .append( "Custom feature type \"" )
                       .append( ftype )
                       .append( "\" for language " )
                       .append( langName )
                       .toString();
                    reporter_.report( FixedCode.W_CULF, msg );
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
                    reporter_.report( FixedCode.E_UDFE, msg );
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
                    reporter_.report( FixedCode.E_GEOX, msg );
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
                        reporter_.report( FixedCode.E_UPBD, msg );
                    }
                }
                else {
                    String msg = new StringBuffer()
                       .append( "Custom upload method \"" )
                       .append( upMethod )
                       .append( "\"" )
                       .toString();
                    reporter_.report( FixedCode.W_UPCS, msg );
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
                        reporter_.report( FixedCode.E_MUPM, msg );
                    }
                }
            }
        }

        /**
         * Checks that output formats are declared correctly.
         */
        private void checkOutputFormats() {
            OutputFormat[] outFormats = tcap_.getOutputFormats();
            if ( outFormats.length == 0 ) {
                reporter_.report( FixedCode.E_NOOF,
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
                String[] aliases = of.getAliases();
                String mime = of.getMime();
                String ofName = aliases.length > 0 ? aliases[ 0 ] : mime;
                if ( mime == null 
                     || ! OUT_MIME_REGEX.matcher( mime.trim() ).matches() ) {
                    String msg = new StringBuffer()
                       .append( "Illegal MIME type \"" )
                       .append( mime )
                       .append( "\" for output format " )
                       .append( ofName )
                       .toString();
                    reporter_.report( FixedCode.E_BMIM, msg );
                }
                String ivoid = of.getIvoid();
                if ( ivoid != null && ivoid.startsWith( stdPrefix ) &&
                     ! outKeyList
                      .contains( ivoid.substring( stdPrefix.length() ) ) ) {
                    String msg = new StringBuffer()
                       .append( "Unknown standard output format key \"" )
                       .append( ivoid )
                       .append( " for output format " )
                       .append( ofName )
                       .toString();
                    reporter_.report( FixedCode.E_XOFK, msg );
                }
            }
        }
    }

    /**
     * Tests the capability/interface structure of the capabilities document.
     */
    private static class CapDocRunner implements Runnable {
        private final Reporter reporter_;
        private final TapService tapService_;
        private final Cap[] caps_;

        private static final String TAPCAP_STDID = "ivo://ivoa.net/std/TAP";
        private static final String VOSI_URI = "ivo://ivoa.net/std/VOSI";
        private static final String DALI_URI = "ivo://ivoa.net/std/DALI";
        private static final Collection<String> SSO_SMIDS =
                new HashSet<String>( Arrays.asList( new String[] {
            "ivo://ivoa.net/sso#BasicAA",
            "ivo://ivoa.net/sso#tls-with-password",
            "ivo://ivoa.net/sso#tls-with-certificate",
            "ivo://ivoa.net/sso#cookie",
            "ivo://ivoa.net/sso#OAuth",
            "ivo://ivoa.net/sso#saml2.0",
            "ivo://ivoa.net/sso#OpenID",
        } ) );

        /**
         * Constructor.
         *
         * @param  reporter  validation message destination
         * @param  tapService   TAP service object
         * @param  caps   capability objects read from document
         */
        CapDocRunner( Reporter reporter, TapService tapService, Cap[] caps ) {
            reporter_ = reporter;
            tapService_ = tapService;
            caps_ = caps;
        }

        public void run() {
            Cap tapCap = getTapCap();
            checkSecurityMethods();
            if ( tapCap != null ) {

                /* TAP 1.1 section 2.4, section 2; also required by TAP 1.0.
                 * Examples is not listed here since will not in general
                 * have a role="std" interface
                 * (it's normally accessed via a web page).
                 * Availability is not listed since it can be at
                 * a different URL. */
                checkAccessUrl( tapCap, VOSI_URI + "#capabilities",
                                "/capabilities" );
                checkAccessUrl( tapCap, VOSI_URI + "#tables(-.*)?", "/tables" );
            }
        }

        /**
         * Returns the capability element representating the TAP service
         * itself.  This ought to be unique in the document.
         * Any anomalies are reported through the reporting system.
         *
         * @return  TAP capability, or null if none found
         */
        private Cap getTapCap() {

            /* Assemble all TAP capabilities. */
            List<Cap> tapcaps = new ArrayList<Cap>();
            for ( Cap cap : caps_ ) {
                if ( TAPCAP_STDID.equals( cap.standardId_ ) ) {
                    tapcaps.add( cap );
                }
            }

            /* TAP 1.1 section 2.4 requires exactly one such capability;
             * report if some other number is found, and bail out if
             * there are none. */
            if ( tapcaps.size() == 0 ) {
                String msg = new StringBuffer()
                   .append( "No capability element with " )
                   .append( "standardID='" + TAPCAP_STDID + "'" )
                   .toString();
                reporter_.report( FixedCode.E_CPT1, msg );
                return null;
            }
            else if ( tapcaps.size() > 1 ) {
                String msg = new StringBuffer()
                   .append( "Multiple capability elements with " )
                   .append( "standardID='" + TAPCAP_STDID + "'" )
                   .toString();
                reporter_.report( FixedCode.E_CPT1, msg );
            }

            /* Identify the one we will use in any case and its
             * std interface. */
            Cap tapcap = tapcaps.get( 0 );
            List<Intf> stdIntfs = new ArrayList<Intf>();
            for ( Intf intf : tapcap.intfs_ ) {
                if ( "std".equals( intf.role_ ) ) {
                    stdIntfs.add( intf );
                }
            }
            if ( stdIntfs.size() == 0 ) {
                String msg = new StringBuffer()
                   .append( "No TAP interface element with " )
                   .append( "role=\"std\"" )
                   .toString();
                reporter_.report( FixedCode.E_CPIF, msg );
            }
            else if ( stdIntfs.size() > 1 ) {
                String msg = new StringBuffer()
                   .append( "Multiple TAP interface elements with " )
                   .append( "role=\"std\"" )
                   .toString();
                reporter_.report( FixedCode.W_CPI2, msg );
            }

            /* Look at the standard interface. */
            Intf tapintf = stdIntfs.size() > 0 ? stdIntfs.get( 0 ) : null;
            if ( tapintf != null ) {

                /* Check the version is what we thought it was.
                 * In some cases this is a pointless step, since the version
                 * will have been determined by reading this value in any case,
                 * but not always. */
                String intfVers = tapintf.version_;
                TapVersion tapVers = tapService_.getTapVersion();
                if ( tapVers.is11() && ! "1.1".equals( intfVers ) ) {
                    String msg = new StringBuffer()
                       .append( "TAP interface does not declare " )
                       .append( "version 1.1" )
                       .append( " (<interface role=\"std\"" )
                       .append( " version=\"" + intfVers + "\">" )
                       .append( " for " + tapVers + ")" )
                       .toString();
                    reporter_.report( FixedCode.E_CPTV, msg );
                }
                else if ( ! tapVers.is11() && 
                          ! ( intfVers == null ||
                              intfVers.equals( "1.0" ) ) ) {
                    String msg = new StringBuffer()
                       .append( "TAP interface version declaration" )
                       .append( " mismatch" )
                       .append( " (<interface role=\"std\"" )
                       .append( " version=\"" + intfVers + "\">" )
                       .append( " for " + tapVers + ")" )
                       .toString();
                    reporter_.report( FixedCode.E_CPTV, msg );
                }

                /* Check that the URL is what we thought it was.
                 * That's not essential, but it can lead to surprises if not.
                 * Note however that depending on how taplint was invoked,
                 * the originally requested base URL may not be accessible
                 * by now anyway. */
                String tapUrl = tapService_.getIdentity();
                String intfUrl = tapintf.accessUrl_;
                if ( tapUrl != null && ! tapUrl.equals( intfUrl ) ) {
                    String msg = new StringBuffer()
                       .append( "TAP role='std' interface accessURL " )
                       .append( "differs from TAP service URL " )
                       .append( "(" + intfUrl + " != " + tapUrl + ")" )
                       .toString();
                    reporter_.report( FixedCode.W_CPUR, msg );
                }
            }

            /* Return the TAP capability. */
            return tapcap;
        }

        /**
         * Tests presence and content of securityMethod elements.
         */
        private void checkSecurityMethods() {
            for ( Cap cap : caps_ ) {
                for ( Intf intf : cap.intfs_ ) {
                    SecMeth[] sms = intf.secMeths_.toArray( new SecMeth[ 0 ] );
                    if ( sms.length == 1 &&
                         ( sms[ 0 ].standardId_ == null ||
                           sms[ 0 ].standardId_.trim().length() == 0 ) ) {

                        /* TAP 1.1 section 2.4. */
                        String msg = new StringBuffer()
                           .append( "Interface has single anonymous " )
                           .append( "security method - " )
                           .append( "zero security methods is preferred" )
                           .toString();
                        reporter_.report( FixedCode.W_CPAN, msg );
                    }
                    List<String> idlist = new ArrayList<String>();
                    for ( SecMeth sm : sms ) {
                        String stdid = sm.standardId_;
                        idlist.add( stdid );
                        if ( stdid != null && ! SSO_SMIDS.contains( stdid ) ) {
                            String msg = new StringBuffer()
                               .append( "Unknown SecurityMethod standardID " )
                               .append( "\"" + stdid + "\" " )
                               .append( "(not defined in SSO 2.0)" )
                               .toString();
                            reporter_.report( FixedCode.W_CPSM, msg );
                        }
                    }
                    if ( idlist.size() >
                         new HashSet<String>( idlist ).size() ) {
                        String msg = new StringBuffer()
                           .append( "Duplicate security methods present " )
                           .append( "in capabilities interface: " )
                           .append( idlist )
                           .toString();
                        reporter_.report( FixedCode.W_CPS2, msg );
                    }
                }
            }
        }

        /**
         * Tests that the accessURL for a TAP-related resource is in
         * its proper place.
         *
         * @param  tapCap  standard TAP capability
         * @param  stdIdRegex  pattern for standardID of capability to check
         * @param  subpath   subpath relative to TAP accessURL at which
         *                   the given capability is expected
         */
        private void checkAccessUrl( Cap tapCap, String stdIdRegex,
                                     String subpath ) {
            String tapUrl = getStdAccessUrl( tapCap );
            for ( Cap cap : caps_ ) {
                if ( cap.standardId_.matches( stdIdRegex ) ) {
                    String vosiUrl = getStdAccessUrl( cap );
                    if ( vosiUrl != null &&
                         ! vosiUrl.equals( tapUrl + subpath ) ) {

                        /* Issue a warning rather than an error here,
                         * since just because (e.g.) the capabilities
                         * accessURL is listed at a non-standard place
                         * doesn't mean that it's not present in the
                         * standard place (TAP does not in general require
                         * these capabilities to be present at all). */
                        String msg = new StringBuffer()
                           .append( "AccessURL for " )
                           .append( cap.standardId_ )
                           .append( " is not at fixed location" )
                           .append( " (" )
                           .append( "\"" + vosiUrl + "\"" )
                           .append( " != " )
                           .append( "\"" + tapUrl + subpath )
                           .toString();
                        reporter_.report( FixedCode.W_CPUL, msg );
                    }
                }
            }
        }

        /**
         * Returns the standard access URL for a given capability
         * (the one from the interface with role="std").
         * 
         * @param  cap  capability
         * @return   access URL
         */
        private static String getStdAccessUrl( Cap cap ) {
            for ( Intf intf : cap.intfs_ ) {
                if ( "std".equals( intf.role_ ) ) {
                    return intf.accessUrl_;
                }
            }
            return null;
        }
    }

    /**
     * Represents a capability element.
     */
    private static class Cap {
        final String standardId_;
        final List<Intf> intfs_;
        Cap( String standardId ) {
            standardId_ = standardId;
            intfs_ = new ArrayList<Intf>();
        }
    }

    /**
     * Represents the interface element child of a capability.
     */
    private static class Intf {
        final String xsiType_;
        final String role_;
        final String version_;
        String accessUrl_;
        final List<SecMeth> secMeths_;
        Intf( String xsiType, String role, String version ) {
            xsiType_ = xsiType;
            role_ = role;
            version_ = version;
            secMeths_ = new ArrayList<SecMeth>();
        }
    }

    /**
     * Represents the securityMethod element child of an interface.
     */
    private static class SecMeth {
        final String standardId_;
        SecMeth( String standardId ) {
            standardId_ = standardId;
        }
    }
}
