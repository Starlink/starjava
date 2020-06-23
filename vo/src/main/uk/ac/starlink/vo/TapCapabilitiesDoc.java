package uk.ac.starlink.vo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.util.DOMUtils;

/**
 * Encapsulates useful information found in the capabilities document
 * describing a TAP service.
 * The static method {@link #readCapabilities} can construct an instance
 * of this class by parsing a suitable XML document.
 *
 * @author   Mark Taylor
 * @since    26 Sep 2018
 */
public class TapCapabilitiesDoc {

    private final TapCapability tapCapability_;
    private final StdCapabilityInterface[] intfs_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param   tapCapability   describes TAPRegExt information
     * @param   intfs      enumerates all known capability+interface pairs
     */
    public TapCapabilitiesDoc( TapCapability tapCapability,
                               StdCapabilityInterface[] intfs ) {
        tapCapability_ = tapCapability;
        intfs_ = intfs;
    }

    /**
     * Returns the object that describes the TAP services capabilities
     * as defined by TAPRegExt.
     *
     * @return  TAP-specific capability description, or null if none found
     */
    public TapCapability getTapCapability() {
        return tapCapability_;
    }

    /**
     * Returns a list of capability,interface pairs found associated with
     * this capabilities document.  Note this may include an entry
     * associated with the TAPRegExt capability.
     *
     * @return  array of zero or more capability/interface specifications
     */
    public StdCapabilityInterface[] getInterfaces() {
        return intfs_;
    }

    /**
     * Provides the URL to use for the capabilities endpoint,
     * given the TAP service base URL.
     * This method basically just adds "/capabilities" on the end of
     * the URL string, but it takes care of inserting a slash
     * when required etc.
     *
     * @param   baseUrl  service URL
     * @return  capabilities endpoint URL
     */
    public static URL getCapabilitiesUrl( URL baseUrl ) {
        if ( baseUrl == null ) {
            throw new NullPointerException();
        }
        else {
            try {
                return new URL( baseUrl 
                              + ( baseUrl.toString().endsWith( "/" ) ? ""
                                                                     : "/" )
                              + "capabilities" );
            }
            catch ( MalformedURLException e ) {
                throw new RuntimeException( "Surely not" );
            }
        }
    }

    /**
     * Reads a TAP capabilities document from a given URL and returns
     * a TapCapabilitiesDoc object based on it.  If it looks like there's
     * no suitable document there, an exception is thrown.
     *
     * @param   capsUrl   location of capabilities document
     * @return  capabilities object, not null
     */
    public static TapCapabilitiesDoc readCapabilities( URL capsUrl )
            throws IOException, SAXException {

        /* Read from the URL. */
        InputStream in = new BufferedInputStream( AuthManager.getInstance()
                                                 .openStream( capsUrl ) );

        /* Parse the XML document to a DOM. */
        Element capsEl;
        try {
            capsEl = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse( in )
                    .getDocumentElement();
        }
        catch ( ParserConfigurationException e ) {
            throw (IOException) new IOException( "Parser setup failed" )
                               .initCause( e );
        }
        finally {
            in.close();
        }

        /* Examine the top-level element to see if it appears to be a
         * [vosi:]capabilities element.  Carry on even if it isn't,
         * but this lets us finesse the warning/error messages in case
         * of trouble. */
        String elName = capsEl.getTagName().replaceFirst( ".*:", "" );
        boolean isCapabilities = elName.equals( "capabilities" );

        /* Read the standard interfaces. */
        StdCapabilityInterface[] intfs = getInterfaces( capsEl );

        /* Read the TAPRegExt capability. */
        TapCapability tapCap;
        try {
            tapCap = getTapCapability( capsEl );
        } 
        catch ( XPathExpressionException e ) {
            throw (IOException) new IOException( "XPath programming error?" )
                               .initCause( e );
        }

        /* Return as a TapCapabilitiesDoc as appropriate. */
        if ( tapCap != null || intfs.length > 0 ) {
            return new TapCapabilitiesDoc( tapCap, intfs );
        }
        else {
            if ( isCapabilities ) {
                logger_.warning( "No TAP capabilities content at " + capsUrl );
                return new TapCapabilitiesDoc( tapCap, intfs );
            }
            else {
                throw new IOException( "Doesn't appear to be a TAP "
                                     + "capabilities document at " + capsUrl );
            }
        }
    }

    /**
     * Extracts a TapCapability from the top-level capabilities element,
     * in accordance with the TAPRegExt standard.
     *
     * @param  capsEl  capabilities element
     */
    public static TapCapability getTapCapability( Element capsEl )
            throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        String capXpath = "capability[@standardID='ivo://ivoa.net/std/TAP']";
        Node capNode =
            (Node) xpath.evaluate( capXpath, capsEl, XPathConstants.NODE );
        if ( capNode == null ) {
            return null;
        }

        /* Get upload methods. */
        NodeList upNodeList =
            (NodeList) xpath.evaluate( "uploadMethod/@ivo-id",
                                       capNode, XPathConstants.NODESET );
        List<Ivoid> upList = new ArrayList<>();
        for ( int i = 0; i < upNodeList.getLength(); i++ ) {
            upList.add( new Ivoid( upNodeList.item( i ).getNodeValue() ) );
        }
        final Ivoid[] uploadMethods = upList.toArray( new Ivoid[ 0 ] );

        /* Get data models. */
        NodeList dmNodeList =
            (NodeList) xpath.evaluate( "dataModel/@ivo-id",
                                       capNode, XPathConstants.NODESET );
        List<Ivoid> dmList = new ArrayList<>();
        for ( int i = 0; i < dmNodeList.getLength(); i++ ) {
            dmList.add( new Ivoid( dmNodeList.item( i ).getNodeValue() ) );
        }
        final Ivoid[] dataModels = dmList.toArray( new Ivoid[ 0 ] );

        /* Get languages. */
        NodeList langNodeList =
            (NodeList) xpath.evaluate( "language",
                                       capNode, XPathConstants.NODESET );
        List<TapLanguage> langList = new ArrayList<TapLanguage>();
        for ( int il = 0; il < langNodeList.getLength(); il++ ) {
            Node lang = langNodeList.item( il );
            if ( lang instanceof Element ) {
                langList.add( getLanguage( (Element) lang ) );
            }
        }
        final TapLanguage[] languages =
            langList.toArray( new TapLanguage[ 0 ] );

        /* Get output formats. */
        NodeList ofmtNodeList =
            (NodeList) xpath.evaluate( "outputFormat",
                                       capNode, XPathConstants.NODESET );
        List<OutputFormat> ofmtList = new ArrayList<OutputFormat>();
        for ( int i = 0; i < ofmtNodeList.getLength(); i++ ) {
            Node ofmt = ofmtNodeList.item( i );
            if ( ofmt instanceof Element ) {
                ofmtList.add( getOutputFormat( (Element) ofmt ) );
            }
        }
        final OutputFormat[] outputFormats =
            ofmtList.toArray( new OutputFormat[ 0 ] );

        /* Get various limits. */
        final TapLimit[] outputLimits =
            getLimits( (NodeList) xpath.evaluate( "outputLimit/*", capNode,
                                                  XPathConstants.NODESET ),
                       null );
        final TapLimit[] uploadLimits =
            getLimits( (NodeList) xpath.evaluate( "uploadLimit/*", capNode,
                                                  XPathConstants.NODESET ),
                       null );
        final TapLimit[] retentionLimits =
            getLimits( (NodeList) xpath.evaluate( "retentionPeriod/*", capNode,
                                                  XPathConstants.NODESET ),
                       TapLimit.SECONDS );
        final TapLimit[] executionLimits =
            getLimits( (NodeList) xpath.evaluate( "executionDuration/*",
                                                  capNode,
                                                  XPathConstants.NODESET ),
                       TapLimit.SECONDS );

        /* Construct and return a new TapCapability. */
        return new TapCapability() {
            public Ivoid[] getUploadMethods() {
                return uploadMethods;
            }
            public TapLanguage[] getLanguages() {
                return languages;
            }
            public OutputFormat[] getOutputFormats() {
                return outputFormats;
            }
            public Ivoid[] getDataModels() {
                return dataModels;
            }
            public TapLimit[] getOutputLimits() {
                return outputLimits;
            }
            public TapLimit[] getUploadLimits() {
                return uploadLimits;
            }
            public TapLimit[] getExecutionLimits() {
                return executionLimits;
            }
            public TapLimit[] getRetentionLimits() {
                return retentionLimits;
            }
            public String toString() {
                return "uploadMethods: " + Arrays.asList( uploadMethods ) + "; "
                     + "languages: " + Arrays.asList( languages ) + "; "
                     + "outputFormats: " + Arrays.asList( outputFormats ) + "; "
                     + "dataModels: " + Arrays.asList( dataModels ) + "; "
                     + "outputLimits: " + Arrays.asList( outputLimits ) + "; "
                     + "uploadLimits: " + Arrays.asList( uploadLimits ) + "; "
                     + "execLimits: " + Arrays.asList( executionLimits ) + "; "
                     + "retentLimits: " + Arrays.asList( retentionLimits );
            }
        };
    }

    /**
     * Extracts a list of zero or more standard interfaces from the
     * capabilities element.
     *
     * @param  capsEl  capabilities element
     * @return   capability/interface objects
     */
    public static StdCapabilityInterface[] getInterfaces( Element capsEl ) {
        List<StdCapabilityInterface> intfList =
            new ArrayList<StdCapabilityInterface>();
        NodeList capEls = capsEl.getElementsByTagName( "capability" );
        for ( int ic = 0; ic < capEls.getLength(); ic++ ) {
            Element capEl = (Element) capEls.item( ic );
            final String stdId = capEl.getAttribute( "standardID" );
            final String capType = capEl.getAttribute( "xsi:type" );
            Element descEl =
                DOMUtils.getChildElementByName( capEl, "description" );
            final String description = descEl == null
                                     ? null
                                     : DOMUtils.getTextContent( descEl );
            NodeList intfEls = capEl.getElementsByTagName( "interface" );
            for ( int ii = 0; ii < intfEls.getLength(); ii++ ) {
                Element intfEl = (Element) intfEls.item( ii );
                final String intfType = intfEl.getAttribute( "xsi:type" );
                final String version = intfEl.getAttribute( "version" );
                final String role = intfEl.getAttribute( "role" );
                Element accessEl =
                    DOMUtils.getChildElementByName( intfEl, "accessURL" );
                final String accessUrl = accessEl == null
                                       ? null
                                       : DOMUtils.getTextContent( accessEl );
                NodeList secEls =
                    intfEl.getElementsByTagName( "securityMethod" );
                List<String> smids = new ArrayList<String>();
                for ( int is = 0; is < secEls.getLength(); is++ ) {
                    Element secmethEl = (Element) secEls.item( is );
                    smids.add( secmethEl.getAttribute( "standardID" ) );
                }
                final String[] secmethIds = smids.toArray( new String[ 0 ] );
                intfList.add( new StdCapabilityInterface() {
                    public String getStandardId() {
                        return stdId;
                    }
                    public String getXsiType() {
                        return capType;
                    }
                    public String getDescription() {
                        return description;
                    }
                    public String getInterfaceType() {
                        return intfType;
                    }
                    public String getAccessUrl() {
                        return accessUrl;
                    }
                    public String[] getSecurityMethodIds() {
                        return secmethIds;
                    }
                    public String getVersion() {
                        return version;
                    }
                    public String getRole() {
                        return role;
                    }
                    public String toString() {
                        return new StringBuffer()
                              .append( capType )
                              .append( "; " )
                              .append( intfType )
                              .append( "; " )
                              .append( Arrays.toString( secmethIds ) )
                              .append( "; " )
                              .append( accessUrl )
                              .toString();
                    }
                } );
            }
        }
        return intfList.toArray( new StdCapabilityInterface[ 0 ] );
    }

    /**
     * Reads a list of nodes representing limits in TAPRegExt format
     * and returns an array of corresponding objects.
     *
     * @param  nodeList  list of <code>hard</code> or <code>default</code>
     *                   elements
     * @param  fixedUnit  unit string if one is implicit, or null if it is
     *                    represented by a <code>unit</code> attribute
     * @return   array of limit objects
     */
    private static TapLimit[] getLimits( NodeList nodeList, String fixedUnit ) {
        List<TapLimit> limitList = new ArrayList<TapLimit>();
        for ( int i = 0; i < nodeList.getLength(); i++ ) {
            Node node = nodeList.item( i );
            if ( node instanceof Element ) {
                Element el = (Element) node;
                String tagName = el.getTagName();
                Boolean isHard = null;
                if ( "hard".equals( tagName ) ) {
                    isHard = Boolean.TRUE;
                }
                else if ( "default".equals( tagName ) ) {
                    isHard = Boolean.FALSE;
                }
                if ( isHard != null ) {
                    String unit = fixedUnit == null ? el.getAttribute( "unit" ) 
                                                    : fixedUnit;
                    String text = DOMUtils.getTextContent( el );
                    try {
                        long value = Long.parseLong( text.trim() );
                        limitList.add( new TapLimit( value,
                                                     isHard.booleanValue(),
                                                     unit ) );
                    }
                    catch ( NumberFormatException e ) {
                        // too bad
                    }
                }
            }
        }
        return limitList.toArray( new TapLimit[ 0 ] );
    }

    /**
     * Reads a TapLanguage object from a TAPRegExt language element.
     *
     * @param   langEl  element of type tr:Language (see TAPRegExt)
     * @return   corresponding TapLanguage object
     */
    private static TapLanguage getLanguage( Element langEl ) {

        /* Acquire relevant attribute and element values from DOM. */
        String langName = null;
        String langDesc = null;
        List<String> versionList = new ArrayList<>();
        List<Ivoid> versionIdList = new ArrayList<>();
        final Map<Ivoid,TapLanguageFeature[]> featureMap =
            new LinkedHashMap<>();
        for ( Node langChild = langEl.getFirstChild(); langChild != null;
              langChild = langChild.getNextSibling() ) {
            if ( langChild instanceof Element ) {
                Element childEl = (Element) langChild;
                String childName = childEl.getTagName();
                if ( "name".equals( childName ) ) {
                    langName = DOMUtils.getTextContent( childEl );
                }
                else if ( "description".equals( childName ) ) {
                    langDesc = DOMUtils.getTextContent( childEl );
                }
                else if ( "version".equals( childName ) ) {
                    String idTxt = childEl.getAttribute( "ivo-id" );
                    versionIdList.add( idTxt == null ? null
                                                     : new Ivoid( idTxt ) );
                    versionList.add( DOMUtils.getTextContent( childEl ) );
                }
                else if ( "languageFeatures".equals( childName ) ) {
                    Ivoid featType =
                        new Ivoid( childEl.getAttribute( "type" ) );
                    List<TapLanguageFeature> featList =
                        new ArrayList<TapLanguageFeature>();
                    NodeList featNodeList =
                        childEl.getElementsByTagName( "feature" );
                    for ( int ifeat = 0; ifeat < featNodeList.getLength();
                          ifeat++ ) {
                        Element featEl = (Element) featNodeList.item( ifeat );
                        Element formEl =
                            DOMUtils.getChildElementByName( featEl, "form" );
                        Element descEl =
                            DOMUtils.getChildElementByName( featEl,
                                                            "description" );
                        final String form = formEl == null
                                          ? null
                                          : DOMUtils.getTextContent( formEl );
                        final String desc = descEl == null
                                          ? null
                                          : DOMUtils.getTextContent( descEl );
                        featList.add( new TapLanguageFeature() {
                            public String getForm() {
                                return form;
                            }
                            public String getDescription() {
                                return desc;
                            }
                            public String toString() {
                                return form;
                            }
                        } );
                    }
                    TapLanguageFeature[] features =
                        featList.toArray( new TapLanguageFeature[ 0 ] );
                    featureMap.put( featType, features );
                }
            }
        }

        /* Bundle the results into a new TapLanguage implementation object. */
        final String name = langName;
        final String description = langDesc;
        final String[] versions = versionList.toArray( new String[ 0 ] );
        final Ivoid[] versionIds = versionIdList.toArray( new Ivoid[ 0 ] );
        return new TapLanguage() {
            public String getName() {
                return name;
            }
            public String[] getVersions() {
                return versions;
            }
            public Ivoid[] getVersionIds() {
                return versionIds;
            }
            public String getDescription() {
                return description;
            }
            public Map<Ivoid,TapLanguageFeature[]> getFeaturesMap() {
                return featureMap;
            }
            public String toString() {
                return name + "-" + Arrays.asList( versions );
            }
        };
    }

    /**
     * Reads an OutputFormat object from a TAPRegExt outputFormat element.
     *
     * @param   ofmtEl  element of type tr:OutputFormat (see TAPRegExt)
     * @return  corresponding OutputFormat object
     */
    private static OutputFormat getOutputFormat( Element ofmtEl ) {

        /* Acquire relevant attribute and element values from DOM. */
        String idTxt = ofmtEl.getAttribute( "ivo-id" );
        final Ivoid ivoid = idTxt == null ? null : new Ivoid( idTxt );
        String ofmtMime = null;
        List<String> aliasList = new ArrayList<String>();
        for ( Node ofmtChild = ofmtEl.getFirstChild(); ofmtChild != null;
              ofmtChild = ofmtChild.getNextSibling() ) {
            if ( ofmtChild instanceof Element ) {
                Element childEl = (Element) ofmtChild;
                String childName = childEl.getTagName();
                if ( "mime".equals( childName ) ) {
                    ofmtMime = DOMUtils.getTextContent( childEl );
                }
                else if ( "alias".equals( childName ) ) {
                    aliasList.add( DOMUtils.getTextContent( childEl ) );
                }
            }
        }

        /* Return the results as a new OutputFormat implementation object. */
        final String mime = ofmtMime;
        final String[] aliases = aliasList.toArray( new String[ 0 ] );
        return new OutputFormat() {
            public String getMime() {
                return mime;
            }
            public String[] getAliases() {
                return aliases;
            }
            public Ivoid getIvoid() {
                return ivoid;
            }
            public String toString() {
                if ( aliases.length > 0 ) {
                    return aliases[ 0 ];
                }
                else if ( mime != null ) {
                    return mime;
                }
                else if ( ivoid != null ) {
                    return ivoid.toString();
                }
                else {
                    return "?output-format?";
                }
            }
        };
    }
}
