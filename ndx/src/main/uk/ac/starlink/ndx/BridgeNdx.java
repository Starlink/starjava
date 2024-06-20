package uk.ac.starlink.ndx;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.ast.AstPackage;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.WinMap;
import uk.ac.starlink.ast.xml.XAstReader;
import uk.ac.starlink.ast.xml.XAstWriter;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.hdx.AbstractHdxFacade;
import uk.ac.starlink.hdx.HdxFacade;
import uk.ac.starlink.hdx.HdxDocument;
import uk.ac.starlink.hdx.HdxException;
import uk.ac.starlink.hdx.HdxResourceType;
import uk.ac.starlink.hdx.HdxResourceFactory;
import uk.ac.starlink.hdx.PluginException;

/**
 * Default <code>Ndx</code> implementation.
 * This class builds an <code>Ndx</code> from an {@link NdxImpl}.
 *
 * <p>The static initialiser for this class is also responsible
 * for creating and registering the 
 * {@link uk.ac.starlink.hdx.HdxResourceType} which corresponds to
 * Ndx.  For this to happen, this <code>BridgeNdx</code> class must be named in a
 * <code>Hdx.properties</code> file, as described in class 
 * {@link uk.ac.starlink.hdx.HdxResourceType}.
 *
 * @author   Mark Taylor (Starlink)
 * @author   Peter Draper (Starlink)
 * @author   Norman Gray (Starlink)
 */
public class BridgeNdx implements Ndx {

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.ndx" );

    private final NdxImpl impl;
    private FrameSet ast;
    private String title;
    private String label;
    private String units;
    private Boolean hasEtc;
    private Boolean hasTitle;
    private Boolean hasLabel;
    private Boolean hasUnits;
    private Boolean hasVariance;
    private Boolean hasQuality;
    private Boolean hasWCS;
    private Integer badbits;
    private NDArray image;
    private NDArray variance;
    private NDArray quality;

    protected Document ndxDocumentCache;

    final private static String XMLNAME_IMAGE = "image";
    final private static String XMLNAME_VARIANCE = "variance";
    final private static String XMLNAME_QUALITY = "quality";
    final private static String XMLNAME_TITLE = "title";
    final private static String XMLNAME_LABEL = "label";
    final private static String XMLNAME_UNITS = "units";
    final private static String XMLNAME_BADBITS = "badbits";
    final private static String XMLNAME_WCS = "wcs";
    final private static String XMLNAME_ETC = "etc";
    

    /*
     * Static initialiser creates the HdxResourceType corresponding to Ndx.
     */
    static private HdxResourceType ndxType;
    static {
        ndxType = HdxResourceType.newHdxResourceType("ndx");
        if (ndxType == null)
            throw new PluginException("Ooops: type ndx was already defined");

        try {
            // Register fallback handler
            ndxType.registerHdxResourceFactory
                (new HdxResourceFactory() {
                    public Object getObject (Element el)
                        throws HdxException {
                        String att = el.getAttribute("url");
                        assert att != null;
                        if (att.length() == 0)
                            /*
                             *  That is, instantiate a new BridgeNdx only
                             *  if the element has _no_ url attribute, so
                             *  that the data and variance content are
                             *  present as elements
                             * 
                             *  XXX we should deal, either here or when the
                             *  element was constructed, with the case
                             *  <ndx url="xxx"/>
                             */
                            return new BridgeNdx(new DomNdxImpl(el));
                        else
                            return null;
                    }
                });
            ndxType.setElementValidator
                (new uk.ac.starlink.hdx.ElementValidator() {
                    public boolean validateElement(Element el) {
                        /*
                        *  Validate by requiring that each child is an
                        *  element, and that each _registered_ element
                        *  is valid.  If we find any elements that
                        *  aren't registered, assume they're OK.  Is
                        *  this sensible?
                        */
                        for (Node n = el.getFirstChild();
                             n != null;
                             n = n.getNextSibling()) {
                            if (n.getNodeType() != Node.ELEMENT_NODE)
                                return false;
                            HdxResourceType t
                                = HdxResourceType.match((Element)n);
                            if (t != HdxResourceType.NONE
                                && !t.isValid((Element)n))
                                return false;
                        }
                        // no objections, so...
                        return true;
                    }
                });
            ndxType.setHoistAttribute("uri");
            ndxType.setConstructedClass("uk.ac.starlink.ndx.Ndx");

            /*
            *  Register the array types corresponding to
            *  XMLNAME_{IMAGE,VARIANCE,QUALITY}, and the fallback
            *  handlers for them.  This might sit more naturally in
            *  NDArrayFactory, except that we would then have to
            *  duplicate the XMLNAME_... names.
            */
            HdxResourceFactory hrf = new HdxResourceFactory() {
                    public Object getObject(org.w3c.dom.Element el) 
                        throws uk.ac.starlink.hdx.HdxException {
                        /*
                         * We do not have to worry whether this
                         * element is in fact an HdxElement, and so
                         * whether or not the element is actually a
                         * facade for a pre-made object (and so on).
                         * We would not have been called unless we
                         * were to construct the object from scratch,
                         * from the URL.
                         *
                         * Also, we don't need to worry about checking
                         * that the Element is of an appropriate type.
                         * Firstly, we shouldn't be called otherwise,
                         * but secondly, if this is the wrong element,
                         * then makeNDArray will fail and return null.
                         */
                        try {
                            String url = el.getAttribute("url");
                            if (url == null)
                                return null;
                            uk.ac.starlink.array.NDArrayFactory ndaf
                                    = new uk.ac.starlink.array.NDArrayFactory();
                            return ndaf.makeNDArray
                                    (new URL(url),
                                    uk.ac.starlink.array.AccessMode.READ);
                        } catch (java.net.MalformedURLException ex) {
                            throw new uk.ac.starlink.hdx.HdxException
                                ("Can't create URL: "
                                 + el.getAttribute("url")
                                 + " (" + ex + ")");
                        } catch (IOException ex) {
                            throw new uk.ac.starlink.hdx.HdxException
                                ("Unexpectedly failed to read "
                                 + el.getAttribute("url")
                                 + " (" + ex + ")");
                        }
                    }
            };

            String[] subtype = 
                    { XMLNAME_IMAGE, XMLNAME_VARIANCE, XMLNAME_QUALITY };
            for (int i=0; i<subtype.length; i++) {
                HdxResourceType newtype
                    = HdxResourceType.newHdxResourceType(subtype[i]);
                if (newtype == null)
                    throw new uk.ac.starlink.hdx.PluginException
                        ("Ooops: type " + subtype[i] + " already defined");
                newtype.registerHdxResourceFactory(hrf);
                newtype.setHoistAttribute("uri");
                newtype.setElementValidator
                    (new uk.ac.starlink.hdx.ElementValidator() {
                        public boolean validateElement(Element el) {
                            // Require that the element has a uri attribute
                            String uriString = el.getAttribute("uri");
                            if (uriString.length() == 0)
                                return false;

                            try {
                                /*
                                *  Construct a URI object, to validate
                                *  the syntax of uriString.  Should we
                                *  try converting it to a URL?
                                *  Probably not, since the toURL()
                                *  method requires that the URI be
                                *  absolute, which we don't
                                *  necessarily want.
                                */
                                java.net.URI uri = new URI(uriString);
                                return true;
                            } catch (java.net.URISyntaxException e) {
                                // Ignore, but return false
                                return false;
                            }
                        }
                    });
                newtype.setConstructedClass("uk.ac.starlink.array.NDArray");
            }
        } catch (HdxException ex) {
            throw new PluginException("Failed to register types!: " + ex);
        }
    }
    

    /**
     * Constructs an {@link Ndx} implementation from an <code>NdxImpl</code> 
     * object.
     *
     * @param  impl  object which provides services to this BridgeNdx
     */
    public BridgeNdx( NdxImpl impl ) {
        this.impl = impl;
    }

    public NDArray getImage() {
        if ( image == null ) {
            image = impl.getImage();
        }
        return image;
    }

    public NDArray getVariance() {
        if ( ! hasVariance() ) {
            throw new UnsupportedOperationException( "No variance component" );
        }
        if ( variance == null ) {
            variance = impl.getVariance();
        }
        return variance;
    }

    public NDArray getQuality() {
        if ( ! hasQuality() ) {
            throw new UnsupportedOperationException( "No quality component" );
        }
        if ( quality == null ) {
            quality = impl.getQuality();
        }
        return quality;
    }

    public boolean hasVariance() {
        if ( hasVariance == null ) {
            hasVariance = Boolean.valueOf( impl.hasVariance() );
        }
        return hasVariance.booleanValue();
    }

    public boolean hasQuality() {
        if ( hasQuality == null ) {
            hasQuality = Boolean.valueOf( impl.hasQuality() );
        }
        return hasQuality.booleanValue();
    }

    public boolean hasTitle() {
        if ( hasTitle == null ) {
            hasTitle = Boolean.valueOf( impl.hasTitle() );
        }
        return hasTitle.booleanValue();
    }

    public boolean hasLabel() {
        if ( hasLabel == null ) {
            hasLabel = Boolean.valueOf( impl.hasLabel() );
        }
        return hasLabel.booleanValue();
    }

    public boolean hasUnits() {
        if ( hasUnits == null ) {
            hasUnits = Boolean.valueOf( impl.hasUnits() );
        }
        return hasUnits.booleanValue();
    }

    public boolean hasEtc() {
        if ( hasEtc == null ) {
            hasEtc = Boolean.valueOf( impl.hasEtc() );
        }
        return hasEtc.booleanValue();
    }

    public boolean hasWCS() {
        if ( hasWCS == null ) {
            hasWCS = Boolean.valueOf( AstPackage.isAvailable() &&
                                      impl.hasWCS() );
        }
        return hasWCS.booleanValue();
    }

    public String getTitle() {
        if ( ! hasTitle() ) {
            throw new UnsupportedOperationException( "No title component" );
        }
        if ( title == null ) {
            title = impl.getTitle();
        }
        return title;
    }

    public String getLabel() {
        if ( ! hasLabel() ) {
            throw new UnsupportedOperationException( "No label component" );
        }
        if ( label == null ) {
            label = impl.getLabel();
        }
        return label;
    }

    public String getUnits() {
        if ( ! hasUnits() ) {
            throw new UnsupportedOperationException( "No units component" );
        }
        if ( units == null ) {
            units = impl.getUnits();
        }
        return units;
    }

    public Source getEtc() {
        if ( ! hasEtc() ) {
            throw new UnsupportedOperationException( "No Etc component" );
        }
        return impl.getEtc();
    }

    public int getBadBits() {
        if ( badbits == null ) {
            badbits = Integer.valueOf( impl.getBadBits() );
        }
        return badbits.intValue();
    }

    public FrameSet getAst() {
        if ( ! hasWCS() ) {
            throw new UnsupportedOperationException( "No WCS component" );
        }
        if ( ast == null ) {
            try {

                /* Implementation may supply the WCS in a number of formats.
                 * Try to cope with all, or throw an exception. */
                Object fsobj = impl.getWCS();
                if ( fsobj instanceof FrameSet ) {
                    ast = (FrameSet) fsobj;
                }
                else if ( fsobj instanceof Element ) {
                    ast = makeAst( new DOMSource( (Element) fsobj ) );
                }
                else if ( fsobj instanceof Source ) {
                    ast = makeAst( (Source) fsobj );
                }
                else {
                    logger.warning( "Unknown WCS object type " + fsobj );
                }
            }
            catch ( IOException e ) {
                logger.warning( "Error retrieving WCS: " + e ); 
            }
            if ( ast == null ) {
                ast = Ndxs.getDefaultAst( this );
            }
        }
        return ast;
    }

    public boolean isPersistent() {
        return ( getImage().getURL() != null )
            && ( ! hasVariance() || getVariance().getURL() != null )
            && ( ! hasQuality() || getQuality().getURL() != null );
    }


    /**
     * Turns a Source into a FrameSet.  The source must represent a <wcs>
     * element with a supported 'encoding' attribute.  Currently only
     * AST-XML encoding is supported; in this case the <wcs> element
     * has to contain a <FrameSet> element as written by 
     * {@link uk.ac.starlink.ast.xml.XAstWriter}.
     *
     * @param  astsrc
     * @return a FrameSet object
     * @throws  IOException   if it can't be done
     */
    private static FrameSet makeAst( Source astsrc ) throws IOException {
        Node astnode;
        try { 
            astnode = new SourceReader().getElement( astsrc );
        }
        catch ( TransformerException e ) {
            throw (IOException) new IOException( e.toString() )
                               .initCause( e );
        }
        if ( astnode instanceof Element &&
             astnode.getNodeName().equals( "wcs" ) ) {
            Element astel = (Element) astnode;
            if ( astel.getAttribute( "encoding" ).equals( "AST-XML" ) ) {
                for ( Node child = astel.getFirstChild(); child != null;
                      child = child.getNextSibling() ) {
                    if ( child instanceof Element &&
                         ((Element) child).getTagName().equals( "FrameSet" ) ) {
                        return (FrameSet) new XAstReader()
                                         .makeAst( (Element) child );
                    }
                }
                throw new IOException( "No <FrameSet> element in <wcs>" );
            }
            else {
                throw new IOException( 
                    "Unsupported encoding on <wcs> element" );
            }
        }
        else {
            throw new IOException( "XML does not represent <wcs> element" );
        }
    }

    /**
     * Generates an XML view of this Ndx object as a <code>Source</code>.
     * The XML is built using only public methods of this Ndx rather than
     * any private values, so that this method can safely be inherited
     * by subclasses.
     *
     * <p>Does not currently throw <code>HdxException</code> if the
     * XML cannot be generated, but it should.
     *
     * @param  base  URL against which others are to be relativised
     * @return  an XML Source representation of this Ndx
     * @throws uk.ac.starlink.hdx.PluginException (unchecked) if the XML cannot be generated
     * @deprecated replaced by
     * <code>getHdxFacade().getSource(URLUtils.urlToUri(base))</code>
     */
    public Source toXML( URL base ) {
        /*
         *  this method comes from the Ndx interface, but has the same
         *  functionality as getSource
         */
        try {
            return getHdxFacade().getSource( URLUtils.urlToUri(base) );
        } catch (HdxException ex) {
            // this method `should' throw an HdxException, but the
            // interface doesn't allow that, so for now just convert
            // it to a PluginException
            throw new uk.ac.starlink.hdx.PluginException( ex );
        } catch (java.net.MalformedURLException ex) {
            // ditto
            throw new uk.ac.starlink.hdx.PluginException( ex );
        }
    }
    
    /**
     * Returns the Hdx type corresponding to Ndx objects.
     */
    public static HdxResourceType getHdxResourceType() {
        return ndxType;
    }

    /**
     * Generalises the Document.importNode method so it works for a wider
     * range of Node types.
     */
    private Node importNode( Document doc, Node inode ) {

        /* Importing a DocumentFragment should work (in fact I think that's
         * pretty much what DocumentFragments were designed for) but when
         * you try to do it using Crimson it throws:
         *
         *   org.apache.crimson.tree.DomEx: 
         *      HIERARCHY_REQUEST_ERR: This node isn't allowed there
         *
         * I'm pretty sure that's a bug.  Work round it by hand here. */
        if ( inode instanceof DocumentFragment ) {
            Node onode = doc.createDocumentFragment();
            for ( Node ichild = inode.getFirstChild(); ichild != null; 
                  ichild = ichild.getNextSibling() ) {
                Node ochild = doc.importNode( ichild, true );
                onode.appendChild( ochild );
            }
            return onode;
        }

        /* It isn't permitted to import a whole document.  Just get its
         * root element. */
        else if ( inode instanceof Document ) {
            Node rootnode = ((Document) inode).getDocumentElement();
            return doc.importNode( rootnode, true );
        }

        /* Otherwise, just let Document.importNode do the work. */
        else {
            return doc.importNode( inode, true );
        }
    }

    private Element adornDOM(Element ndxEl, HdxDocument doc, URI base)
            throws java.net.MalformedURLException {
        
        /* Get the base URI in a form suitable for using with URI.relativize. */
        URI baseUri;
        if ( base != null ) {
            try {
                String scheme = base.getScheme();
                String auth = base.getAuthority();
                String path = base.getPath();
                if ( path == null ) {
                    path = "";
                }
                path = path.replaceFirst("[^/]*$", "" ); // remove trailing path
                baseUri = new URI( scheme, auth, path, "", "" );
            }
            catch ( URISyntaxException e ) {
                baseUri = null;
            }
        }
        else {
            baseUri = null;
        }

        /* Write a title element. */
        if ( hasTitle() ) {
            Element titleEl = doc.createElement( XMLNAME_TITLE );
            titleEl.setAttribute( "value", getTitle() );
            ndxEl.appendChild( titleEl );
        }

        /* Write a label element. */
        if ( hasLabel() ) {
            Element labelEl = doc.createElement( XMLNAME_LABEL );
            labelEl.setAttribute( "value", getLabel() );
            ndxEl.appendChild( labelEl );
        }

        /* Write a units element. */
        if ( hasUnits() ) {
            Element unitsEl = doc.createElement( XMLNAME_UNITS );
            unitsEl.setAttribute( "value", getUnits() );
            ndxEl.appendChild( unitsEl );
        }

        /* Write an image element. */
        HdxResourceType type = HdxResourceType.match( XMLNAME_IMAGE );
        assert type != HdxResourceType.NONE;
        Element imEl = doc.createElement( getImage().getHdxFacade( type ));
        ndxEl.appendChild( imEl );
        if ( getImage().getURL() != null ) {
            URI iuri = URLUtils.urlToUri( getImage().getURL() );
            if ( baseUri != null ) {
                iuri = baseUri.relativize( iuri );
            }
            imEl.setAttribute( "uri", iuri.toString() );
        }
        else {
            Node imComm = doc.createComment( "Image array is virtual" );
            imEl.appendChild( imComm );
        }

        /* Write a variance element. */
        if ( hasVariance() ) {
            type = HdxResourceType.match( XMLNAME_VARIANCE );
            assert type != HdxResourceType.NONE;
            Element varEl = doc.createElement
                    ( getVariance().getHdxFacade( type ));
            ndxEl.appendChild( varEl );
            if ( getVariance().getURL() != null ) {
                URI vuri = URLUtils.urlToUri( getVariance().getURL() );
                if ( baseUri != null ) {
                    vuri = baseUri.relativize( vuri );
                }
                varEl.setAttribute( "uri", vuri.toString() );
            }
            else {
                Node varComm = doc.createComment
                        ( "Variance array is virtual" );
                varEl.appendChild( varComm );
            }
        }

        /* Write a quality element. */
        if ( hasQuality() ) {
            type = HdxResourceType.match( XMLNAME_QUALITY );
            assert type != HdxResourceType.NONE;
            Element qualEl = doc.createElement
                    ( getQuality().getHdxFacade( type ));
            ndxEl.appendChild( qualEl );
            if ( getQuality().getURL() != null ) {
                URI quri = URLUtils.urlToUri( getQuality().getURL() );
                if ( baseUri != null ) {
                    quri = baseUri.relativize( quri );
                }
                qualEl.setAttribute( "uri", quri.toString() );
            }
            else {
                Node qualComm = doc.createComment
                        ( "Quality array is virtual" );
                qualEl.appendChild( qualComm );
            }
        }

        /* Write a badbits element. */
        if ( getBadBits() != 0 ) {
            String bbrep = "0x" + Integer.toHexString( getBadBits() );
            Element bbEl = doc.createElement( XMLNAME_BADBITS );
            bbEl.setAttribute( "value", bbrep );
            ndxEl.appendChild( bbEl );
        }
        
        /* Write a WCS element. */
        if ( hasWCS() ) {
            FrameSet wfset = getAst();
            Source wcsSource = new XAstWriter().makeSource( wfset );
            try {
                Node wcsContent = new SourceReader().getDOM( wcsSource );
                wcsContent = importNode( doc, wcsContent ); 
                Element wcsEl = doc.createElement( XMLNAME_WCS );
                wcsEl.setAttribute( "encoding", "AST-XML" );
                wcsEl.appendChild( wcsContent );
                ndxEl.appendChild( wcsEl );
            }
            catch ( TransformerException e ) {
                logger.warning( "Trouble transforming WCS: "
                                + e.getMessage() );
                ndxEl.appendChild( doc.createComment( "Broken WCS" ) );
            }
        }

        /* Write an Etc element. */
        if ( hasEtc() ) {
            try {
                Source etcSrc = getEtc();
                Node etcEl = new SourceReader().getDOM( etcSrc );
                etcEl = importNode( doc, etcEl );

                /* Check that the returned object has the right form. */
                if ( etcEl instanceof Element && 
                     ((Element) etcEl).getTagName() == XMLNAME_ETC ) {
                    ndxEl.appendChild( etcEl );
                }
                else {
                    logger.warning( "Badly-formed Etc component from impl " 
                                  + impl +  "  - not added" );
                    ndxEl.appendChild( doc.createComment( "Broken ETC" ) );
                }
            }
            catch ( TransformerException e ) {
                logger.warning( 
                    "Error transforming Etc component - not added" );
                ndxEl.appendChild( doc.createComment( "Broken ETC" ) );
            }
        }

        return ndxEl;
    }

    public HdxFacade getHdxFacade() {
        return new BridgeNdxHdxFacade();
    }

    protected class BridgeNdxHdxFacade
            extends AbstractHdxFacade {

        public Object synchronizeElement( Element el, Object memento )
                throws HdxException {
            /*
            *  ignore memento -- this Ndx can't be changed (the Ndx
            *  interface has no mutator methods), so if the given
            *  element has children then it can only be because we've
            *  been here before
            */
            if ( el.hasChildNodes() )
                return null;
            
            if (! el.getTagName().equals( ndxType.xmlName() ) )
                // The world has gone mad -- this shouldn't happen
                throw new HdxException
                        ( "synchronizeElement given element <"
                          + el.getTagName()
                          + ">, not <"
                          + ndxType.xmlName()
                          + "> as expected");
            
            try {
                adornDOM( el, (HdxDocument)el.getOwnerDocument(), null );

                return null;

            } catch (java.net.MalformedURLException ex) {
                throw new HdxException("Failed to synchronise element ("
                                       + ex + ")");
            }
        }        

        public Object getObject( Element el )
                throws HdxException {
            HdxResourceType t = HdxResourceType.match( el );

            if ( t == HdxResourceType.NONE )
                throw new HdxException
                        ("getObject was asked to realise an unregistered Type:"
                         + el );
            String tagname = el.getTagName();
            Object ret = null;
            if ( t == ndxType )
                ret = BridgeNdx.this;
            else if ( tagname.equals( XMLNAME_IMAGE ) )
                ret = getImage();
            else if ( tagname.equals( XMLNAME_VARIANCE ) )
                ret = getVariance();
            else if ( tagname.equals( XMLNAME_QUALITY ) )
                ret = getQuality();

            /*
            *  These three are the only HdxResourceTypes which we
            *  register at the top.  If ret is still null, then it's
            *  because the code down here is out of date: that is,
            *  there's a type been registered which hasn't been added
            *  here, or else there's an element of a registered type
            *  within the Ndx which we don't think ought to be there
            *  (do we need to look at the ElementValidator?).  This is
            *  a coding error, so throw an assertion error rather than
            *  merely an HdxException
            */
            assert ret != null
                    : "Ooops: surprising registered type " + t + " in Ndx";
            
            assert t.getConstructedClass() != Object.class
                    && t.getConstructedClass().isInstance( ret );
            
            return ret;
        }

        public HdxResourceType getHdxResourceType() {
            return BridgeNdx.getHdxResourceType();
        }
    }
}
