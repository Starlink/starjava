package uk.ac.starlink.vo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.starlink.util.DOMUtils;

/**
 * Describes the capabilities of a TAP service as serialized by the
 * TAPRegExt schema.
 *
 * @author   Mark Taylor
 * @since    7 Mar 2011
 */
public abstract class TapCapability {

    /**
     * IVO ID for the TAPRegExt standard registry record {@value}.
     * This forms the base URI onto which fragment parts are appended to
     * generate StandardsRegExt StandardKey-style keys to describe some
     * concepts used by TAPRegExt standard.
     */
    public static final String TAPREGEXT_STD_URI =
        "ivo://ivoa.net/std/TAPRegExt";

    /**
     * Feature type key for ADQL(-like) User-Defined Functions. {@value}
     */
    public static final String UDF_FEATURE_TYPE =
        TAPREGEXT_STD_URI + "#features-udf";

    /**
     * Feature type key for ADQL geometrical functions. {@value}
     */
    public static final String ADQLGEO_FEATURE_TYPE =
        TAPREGEXT_STD_URI + "#features-adqlgeo";

    /**
     * Returns an array of upload methods known by this capability.
     *
     * @return  uploadMethod element ivo-id attribute values
     */
    public abstract String[] getUploadMethods();

    /**
     * Returns an array of query language specifiers known by this capability.
     *
     * @return  array of language objects
     */
    public abstract TapLanguage[] getLanguages();

    /**
     * Returns an array of data models known by this capability.
     *
     * @return   dataModel element ivo-id attribute values
     */
    public abstract String[] getDataModels();

    /**
     * Returns an array of limit values representing the data limits for
     * result tables.
     * Legal values for limit units are "row" or "byte".
     *
     * @return   output table limits
     */
    public abstract TapLimit[] getOutputLimits();

    /**
     * Returns an array of limit values representing the data limits for
     * uploaded tables.
     * Legal values for limit units are "row" or "byte".
     *
     * @return   upload table limits
     */
    public abstract TapLimit[] getUploadLimits();

    /**
     * Returns an array of limit values representing the time limits for
     * query execution.
     * The limit units will be "seconds".
     *
     * @return   execution time limits
     */
    public abstract TapLimit[] getExecutionLimits();

    /**
     * Returns an array of limit values representing the time limits for
     * query retention.
     * The limit units will be "seconds".
     *
     * @return   retention time limits
     */
    public abstract TapLimit[] getRetentionLimits();

    /**
     * Reads a TAPRegExt document from a given URL and returns a TapCapability
     * object based on it.
     *
     * @param   url   location of document
     * @return  capability object
     */
    public static TapCapability readTapCapability( URL url )
            throws IOException, SAXException {
        try {
            return attemptReadTapCapability( url );
        }
        catch ( ParserConfigurationException e ) {
            throw (IOException) new IOException( "Parser setup failed" )
                               .initCause( e );
        }
        catch ( XPathExpressionException e ) {
            throw (IOException) new IOException( "XPath programming error?" )
                               .initCause( e );
        }
    }

    /**
     * Attempts to read a TAPRegExt document from a given URL.
     *
     * @param   url   location of document
     * @return  capability object
     */
    private static TapCapability attemptReadTapCapability( URL url )
            throws ParserConfigurationException, XPathExpressionException,
                   IOException, SAXException {

        /* Parse and prepare for document interrogation. */
        Document capsDoc = DocumentBuilderFactory.newInstance()
                          .newDocumentBuilder()
                          .parse( new BufferedInputStream( url.openStream() ) );
        XPath xpath = XPathFactory.newInstance().newXPath();
        String capXpath = "capability[@standardID='ivo://ivoa.net/std/TAP']";
        Node capNode =
            (Node) xpath.evaluate( capXpath, capsDoc.getDocumentElement(),
                                   XPathConstants.NODE );
        if ( capNode == null ) {
            throw new IOException( "No element \"" + capXpath + "\""
                                 + " at " + url );
        }

        /* Get upload methods. */
        NodeList upNodeList =
            (NodeList) xpath.evaluate( "uploadMethod/@ivo-id",
                                       capNode, XPathConstants.NODESET );
        List<String> upList = new ArrayList<String>();
        for ( int i = 0; i < upNodeList.getLength(); i++ ) {
            upList.add( upNodeList.item( i ).getNodeValue() );
        }
        final String[] uploadMethods = upList.toArray( new String[ 0 ] );

        /* Get data models. */
        NodeList dmNodeList =
            (NodeList) xpath.evaluate( "dataModel/@ivo-id",
                                       capNode, XPathConstants.NODESET );
        List<String> dmList = new ArrayList<String>();
        for ( int i = 0; i < dmNodeList.getLength(); i++ ) {
            dmList.add( dmNodeList.item( i ).getNodeValue() );
        }
        final String[] dataModels = dmList.toArray( new String[ 0 ] );

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
            public String[] getUploadMethods() {
                return uploadMethods;
            }
            public TapLanguage[] getLanguages() {
                return languages;
            }
            public String[] getDataModels() {
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
                     + "dataModels: " + Arrays.asList( dataModels ) + "; "
                     + "outputLimits: " + Arrays.asList( outputLimits ) + "; "
                     + "uploadLimits: " + Arrays.asList( uploadLimits ) + "; "
                     + "execLimits: " + Arrays.asList( executionLimits ) + "; "
                     + "retentLimits: " + Arrays.asList( retentionLimits );
            }
        };
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
                    String unit = fixedUnit == null ? el.getAttribute( "unit" )                                                     : fixedUnit;
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
        List<String> versionList = new ArrayList<String>();
        List<String> versionIdList = new ArrayList<String>();
        final Map<String,TapLanguageFeature[]> featureMap =
            new LinkedHashMap<String,TapLanguageFeature[]>();
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
                    versionIdList.add( childEl.getAttribute( "ivo-id" ) );
                    versionList.add( DOMUtils.getTextContent( childEl ) );
                }
                else if ( "languageFeatures".equals( childName ) ) {
                    String featType = childEl.getAttribute( "type" );
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
        final String[] versionIds = versionIdList.toArray( new String[ 0 ] );
        return new TapLanguage() {
            public String getName() {
                return name;
            }
            public String[] getVersions() {
                return versions;
            }
            public String[] getVersionIds() {
                return versionIds;
            }
            public String getDescription() {
                return description;
            }
            public Map<String,TapLanguageFeature[]> getFeaturesMap() {
                return featureMap;
            }
            public String toString() {
                return name + "-" + Arrays.asList( versions );
            }
        };
    }

    public static void main( String[] args ) throws IOException, SAXException {
        System.out.println( readTapCapability( new URL( args[ 0 ] ) ) );
    }
}
