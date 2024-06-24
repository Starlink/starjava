package uk.ac.starlink.ndx;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.CDATASection;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayBuilder;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.array.NDArrayFactory;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.ast.AstPackage;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.xml.XAstReader;
import uk.ac.starlink.hdx.HdxException;
import uk.ac.starlink.hdx.HdxResourceType;
import uk.ac.starlink.hdx.HdxFactory;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.util.URLUtils;

/**
 * Turns URLs which reference XML files into Ndxs.
 * This does not do such sophisticated processing as the HDX system,
 * and is mainly intended as a stopgap until that is complete.
 * No namespace is used in XML files, and array URL references may 
 * be either as the value of a 'url' attribute or as the text
 * content of an element, e.g.:
 * <pre>
 *    &lt;variance url="http://archive.org/data/stars-vars.fits"/&gt;
 * </pre>
 * or
 * <pre>
 *    &lt;variance&gt;http://archive.org/data/stars-vars.fits&lt;/variance&gt;
 * </pre>
 * URLs relative to the position of the XML file in question are allowed.
 * <p>
 * A URL is normally only considered suitable if it ends in '.xml'.  
 * However, the special URL "<code>file:-</code>" may be used to 
 * indicate standard input/output.
 */
public class XMLNdxHandler implements NdxHandler {

    /** Sole instance of the class. */
    private static XMLNdxHandler instance = new XMLNdxHandler();

    private static NDArrayFactory arrayfact = new NDArrayFactory();
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.ndx" );

    /**
     * Private sole constructor.
     */
    private XMLNdxHandler() {}

    /**
     * Returns an XMLNdxHandler.
     *
     * @return  the sole instance of this class
     */
    public static XMLNdxHandler getInstance() {
        return instance;
    }

    /**
     * Constructs an Ndx object from a URL pointing to an appropriate 
     * XML resource.
     *
     * @param  url  a URL pointing to some XML representing an NDX
     * @param   mode read/write/update access mode for component arrays
     * @throws  IOException  if some error occurs in the I/O
     */
    public Ndx makeNdx( URL url, AccessMode mode ) throws IOException {
        Source xsrc;
        if ( url.toString().equals( "file:-" ) ) {
            xsrc = new StreamSource( System.in );
        }
        else if ( isXmlUrl( url ) ) {
            xsrc = new StreamSource( url.toString() );
        }
        else {
            return null;
        }
        return makeNdx( xsrc, mode );
    }

    /**
     * Constructs a readable Ndx object from an XML source.
     *
     * @param  xsrc  an XML source containing the XML representation of
     *               the NDX.  Must represent a document or element.
     *               Note that the SystemId attribute, if present,
     *               will be used to resolve relative URLs
     * @param   mode read/write/update access mode for component arrays
     * @throws  IOException  if some error occurs in the I/O
     * @throws  IllegalArgumentException  if <code>xsrc</code> does not 
     *          correspond to a document or element XML source
     */
    public Ndx makeNdx( Source xsrc, AccessMode mode ) throws IOException {

        /* Try to get the System ID for resolving relative URLs. */
        String systemId = xsrc.getSystemId();

        /* Get a DOM. */
        Node ndxdom;
        try {
            ndxdom = new SourceReader().getDOM( xsrc );
        }
        catch ( TransformerException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }

        // Examine the DOM
        Element hdxel;
        if ( ndxdom instanceof Document ) {
            hdxel = ((Document) ndxdom).getDocumentElement();
        }
        else if ( ndxdom instanceof Element ) {
            hdxel = (Element) ndxdom;
        }
        else {
            throw new IllegalArgumentException( "Not a Document or Element" );
        }
        assert hdxel != null;

        Element ndxel;
        /*
         * Check to see if it's an Hdx in fact.  If so, extract the
         * single Ndx child from it, carefully verifying that this is
         * in fact the case.  Throw an IOException otherwise.
         *
         * It is essential that we call BridgeNdx.getHdxResourceType()
         * _before_ embarking on this chain of tests, on the
         * off-chance that this is the first time that the BridgeNdx
         * class has been used.  That class's initialiser is what
         * creates and registers the Ndx type, and thus an expression like
         *     HdxResourceType.match( <ndx> ) == BridgeNdx.getHdxResourceType()
         * can return _false_ if the .match is invoked before
         * BridgeNdx has the chance to register the Ndx type.
         */
        HdxResourceType ndxType = BridgeNdx.getHdxResourceType();
        if ( HdxResourceType.match( hdxel ) == HdxResourceType.HDX ) {
            Element kid = null;
            for ( Node n = hdxel.getFirstChild();
                  n != null;
                  n = n.getNextSibling() ) {
                if ( n.getNodeType() != Node.ELEMENT_NODE ) {
                    // What is this?  Hdx DOMs should have only elements in them
                    if ( logger.isLoggable( Level.INFO ) ) {
                        String nv = n.getNodeValue();
                        logger.info( "Non-element node " + n.getNodeName()
                                     + "=" + (nv==null ? "<null>" : nv)
                                     + " ignored");
                    }
                    continue;
                }
                if ( HdxResourceType.match( (Element)n ) == ndxType ) {
                    if ( kid != null )
                        // Ooops, we've already found an Ndx child
                        throw new IOException(
                     "Can't make Ndx from HDX with multiple Ndx children" );
                    kid = (Element)n;
                }
            }
            if ( kid == null )
                // didn't find an Ndx
                throw new IOException
                        ( "Can't make Ndx from HDX with no Ndx children" );
            ndxel = kid;
        } else if ( HdxResourceType.match( hdxel ) == ndxType ) {
            /* 
             * That's fine -- this should have been an <hdx>, but be
             * nice and recover
             */
            ndxel = hdxel;
        } else {
            throw new IOException
                    ( "XML element of type <" + hdxel.getTagName()
                      + "> not <" + ndxType.xmlName() + ">" );
        }
        assert ndxel != null;
        
        Document doc = ndxel.getOwnerDocument();

        /* Parse the DOM looking for known elements.  A proper implementation
         * probably wants to use XPaths and so on. */
        NDArray image = null;
        NDArray variance = null;
        NDArray quality = null;
        String title = null;
        String label = null;
        String units = null;
        FrameSet wcs = null;
        int badbits = 0;
        Node etc = null;

        for ( Node child = ndxel.getFirstChild(); child != null;
              child = child.getNextSibling() ) {
            if ( child instanceof Element ) {
                Element cel = (Element) child;
                String tagname = cel.getTagName();
                if ( tagname.equals( "image" ) ) {
                    image = makeNDArray( cel, mode, systemId );
                }
                else if ( tagname.equals( "variance" ) ) {
                    variance = makeNDArray( cel, mode, systemId );
                }
                else if ( tagname.equals( "quality" ) ) {
                    quality = makeNDArray( cel, mode, systemId );
                }
                else if ( tagname.equals( "title" ) ) {
                    title = cel.getAttribute( "value" );
                    if (title.length() == 0) // no such attribute
                        title = getTextContent( cel );
                }
                else if ( tagname.equals( "label" ) ) {
                    label = cel.getAttribute( "value" );
                    if (label.length() == 0) // no such attribute
                        label = getTextContent( cel );
                }
                else if ( tagname.equals( "units" ) ) {
                    units = cel.getAttribute( "value" );
                    if (units.length() == 0) // no such attribute
                        units = getTextContent( cel );
                }
                else if ( tagname.equals( "badbits" ) ) {
                    String badbitsString = cel.getAttribute( "value" );
                    try {
                        if (badbitsString.length() == 0) // no such attribute
                            badbitsString = getTextContent( cel );
                        badbits = Long.decode(badbitsString).intValue();
                    } catch (NumberFormatException e) {
                        if (logger.isLoggable( Level.WARNING ) )
                            logger.warning
                                    ( "badbits element had bad content \""
                                      + badbitsString
                                      + "\": ignored");
                        badbits = 0;
                    }
                }
                else if ( tagname.equals( "wcs" ) && 
                          AstPackage.isAvailable() ) {
                    Source wcssrc = null;
                    for ( Node ch = cel.getFirstChild(); ch != null; 
                          ch = ch.getNextSibling() ) {
                        if ( ch instanceof Element ) {
                            String chname = ((Element) ch).getTagName();
                            if ( chname.equals( "FrameSet" ) ) {
                                wcssrc = new DOMSource( ch );
                                break;
                            }
                        }
                    }
                    try {
                        wcs = (FrameSet) new XAstReader()
                                        .makeAst( wcssrc );
                    }
                    catch ( IOException e ) {
                        wcs = null;
                    }
                    if ( wcs == null ) {
                        logger.warning( "Broken WCS element" );
                    }
                }
                else if ( tagname.equals( "etc" ) ) {
                    etc = doc.createElement( "etc" );
                    for ( Node ext = cel.getFirstChild(); ext != null;
                          ext = ext.getNextSibling() ) {
                        etc.appendChild( doc.importNode( ext, true ) );
                    }
                }
            }
        }

        return makeNdx( image, variance, quality, title, label, units, 
                        wcs, badbits, etc );
    }

    private NDArray makeNDArray( Element el, AccessMode mode, String systemId )
            throws IOException {
        String loc;
        if ( el.hasAttribute( "uri" ) ) {
            loc = el.getAttribute( "uri" );
        }
        else if ( el.hasAttribute( "url" ) ) {
            loc = el.getAttribute( "url" );
        }
        else {
            loc = getTextContent( el );
        }
 
        if ( loc == null || loc.trim().length() == 0 ) {
            throw new IOException( "No location supplied for <" 
                                 + el.getTagName() + "> array" );
        }

        URL url = URLUtils.makeURL( systemId, loc );
        return arrayfact.makeNDArray( url, mode );
    }

    private String getTextContent( Element el ) {
        String content = "";
        for ( Node sub = el.getFirstChild(); sub != null; 
              sub = sub.getNextSibling() ) {
            if ( sub instanceof Text || sub instanceof CDATASection ) {
                content += ((CharacterData) sub).getData();
            }
        }
        return content;
    }

    private Ndx makeNdx( final NDArray image, final NDArray variance, 
                         final NDArray quality, final String title,
                         final String label, final String units,
                         final FrameSet wcs, final int badbits,
                         final Node etc )
            throws IOException {
        if ( image == null ) {
            throw new IOException( "No <image> component found" );
        }

        NdxImpl impl = new NdxImpl() {
            public NDArray getImage() {
                return image;
            }
            public NDArray getVariance() {
                return variance;
            }
            public NDArray getQuality() {
                return quality;
            }
            public String getTitle() {
                return title;
            }
            public String getLabel() {
                return label;
            }
            public String getUnits() {
                return units;
            }
            public Object getWCS() {
                return wcs;
            }
            public int getBadBits() {
                return badbits;
            }
            public Source getEtc() {
                return new DOMSource( etc );
            }
            public boolean hasTitle() {
                return title != null;
            }
            public boolean hasLabel() {
                return label != null;
            }
            public boolean hasUnits() {
                return units != null;
            }
            public boolean hasVariance() {
                return variance != null;
            }
            public boolean hasQuality() {
                return quality != null;
            }
            public boolean hasWCS() {
                return wcs != null;
            }
            public boolean hasEtc() {
                return etc != null;
            }
        };

        return new BridgeNdx( impl );
    }
    
    /**
     * Writes an XML representation of an existing Ndx object to the given 
     * (writable) URL.  
     * <p>
     * If any of the array components is virtual, then the array data
     * needs to be output too.  Currently, for a URL 'blah.xml'
     * these are written into successive components of a new FITS file
     * 'blah-data.fits'.  If the FITS handlers are not installed,
     * or <code>url</code> does not end in '.xml' an IOException will be thrown.
     * This behaviour may be subject to change in future releases.
     *
     * @param  xurl the URL to which the Ndx is to be serialised in XML
     * @param  ndx  the Ndx object to serialise
     * @throws java.net.UnknownServiceException if the URL does not support
     *         output (most protocols, apart from <code>file</code>, don't)
     * @throws IOException  if some other I/O error occurs
     */
    public boolean outputNdx( URL xurl, Ndx ndx ) throws IOException {

        /* If we need one, get a URL for writing array data. */
        URL aurl = null;
        ArrayBuilder fab = null;
        if ( ! ndx.isPersistent() ) {
            aurl = getDataUrl( xurl );
            fab = getFitsArrayBuilder();
        }
        boolean writeArrays = ( aurl != null );

        /* Get XML output stream. */
        OutputStream xstrm;
        if ( xurl.toString().equals( "file:-" ) ) {
            if ( writeArrays ) {
                throw new IOException( "Cannot serialise non-persistent NDX " 
                                     + "to a stream" );
            }
            xstrm = System.out;
        }
        else if ( isXmlUrl( xurl ) ) {
            if ( xurl.getProtocol().equals( "file" ) ) {
                xstrm = new FileOutputStream( xurl.getPath() );
            }
            else {
                URLConnection xconn = xurl.openConnection();
                xconn.setDoInput( false );
                xconn.setDoOutput( true );
    
                /* The following may throw a java.net.UnknownServiceException
                 * (which is-a IOException) - in fact it almost certainly will,
                 * since I don't know of any URL protocols (including file)
                 * which support output streams. */
                xconn.connect();
                xstrm = xconn.getOutputStream();
            }
            xstrm = new BufferedOutputStream( xstrm );
        }
        else {
            return false;
        }

        /* Get an XML source representing the XML to be written. */
        Source xsrc;
        try {
            if ( ! writeArrays ) {
                xsrc = HdxFactory.getInstance()
                        .newHdxContainer(ndx.getHdxFacade())
                        .getSource( null );
            }
            else {
                int hdu = 0;

                NDArray inda2;
                URL iurl = new URL( aurl, "#" + hdu++ );
                NDArray inda1 = ndx.getImage();
                inda2 = fab.makeNewNDArray( iurl, inda1.getShape(),
                                            inda1.getType(),
                                            inda1.getBadHandler() );
                NDArrays.copy( inda1, inda2 );

                NDArray vnda2;
                if ( ndx.hasVariance() ) {
                    URL vurl = new URL( aurl, "#" + hdu++ );
                    NDArray vnda1 = ndx.getVariance();
                    vnda2 = fab.makeNewNDArray( vurl, vnda1.getShape(),
                                                vnda1.getType(),
                                                vnda1.getBadHandler() );
                    NDArrays.copy( vnda1, vnda2 );
                }
                else {
                    vnda2 = null;
                }

                NDArray qnda2;
                if ( ndx.hasQuality() ) {
                    URL qurl = new URL( aurl, "#" + hdu++ );
                    NDArray qnda1 = ndx.getQuality();
                    BadHandler qbh = BadHandler.getHandler( qnda1.getType(), null );
                    qnda2 = fab.makeNewNDArray( qurl, qnda1.getShape(),
                                                qnda1.getType(), qbh );
                    NDArrays.copy( qnda1, qnda2 );
                }
                else {
                    qnda2 = null;
                }

                MutableNdx ndx2 = new DefaultMutableNdx( ndx );
                ndx2.setImage( inda2 );
                ndx2.setVariance( vnda2 );
                ndx2.setQuality( qnda2 );
                xsrc = HdxFactory.getInstance()
                        .newHdxContainer( ndx2.getHdxFacade() )
                        .getSource( URLUtils.urlToUri(xurl) );
            }
        } catch (uk.ac.starlink.hdx.HdxException ex) {
            throw new IOException( "Trouble constructing HdxContainer" );
        }
        
        /* Write the XML to the XML stream. */
        SourceReader sr = new SourceReader();
        /*
         * This bit isn't safe - we can't guarantee the transformer will be
         * invoked, so we may get no declaration after all.
         */
        sr.setIncludeDeclaration( true );
        sr.setIndent( 2 );
        try { 
            sr.writeSource( xsrc, xstrm );
        }
        catch ( TransformerException e ) {
            throw (IOException) new IOException( "Trouble writing XML" )
                    .initCause( e );
        }
        
        xstrm.close();
        return true;
    }

    /**
     * Writes an XML file representing a new NDX with writable array 
     * components.
     * <p>
     * The array components themselves reside in a new fits file; 
     * currently for a URL 'blah.xml' a new fits file called
     * 'blah-data.fits' is written.  If the FITS handlers are not installed,
     * or <code>url</code> does not end in '.xml' an IOException will be thrown.
     * This behaviour may be subject to change in future releases.
     *
     * @param  url  a URL at which the new NDX should be written
     * @param  template   a template Ndx object from which non-array data
     *                    should be initialised - any title, bad bits mask,
     *                    WCS component etc will be copied from here
     * @return  true if the new Ndx was written successfully
     * @throws  IOException  if the URL is understood but an NDArray cannot
     *                       be made
     */
    public boolean makeBlankNdx( URL url, Ndx template ) throws IOException {
        if ( ! isXmlUrl( url ) ) {
            return false;
        }
        URL xurl = url;
        URL aurl = getDataUrl( xurl );
        ArrayBuilder fab = getFitsArrayBuilder();
        OutputStream xstrm = new FileOutputStream( xurl.getPath() );
        xstrm = new BufferedOutputStream( xstrm );

        /* Make NDArrays containing the data. */
        int hdu = 0;
        URL iurl = new URL( aurl, "#" + hdu++ );
        NDArray inda1 = template.getImage();
        NDArray inda2 = fab.makeNewNDArray( iurl, inda1.getShape(),
                                            inda1.getType(), 
                                            inda1.getBadHandler() );

        NDArray vnda2;
        if ( template.hasVariance() ) { 
            URL vurl = new URL( aurl, "#" + hdu++ );
            NDArray vnda1 = template.getVariance();
            vnda2 = fab.makeNewNDArray( vurl, vnda1.getShape(),
                                        vnda1.getType(),
                                        vnda1.getBadHandler() );
        }
        else {
            vnda2 = null;
        }

        NDArray qnda2;
        if ( template.hasQuality() ) {
            URL qurl = new URL( aurl, "#" + hdu++ );
            NDArray qnda1 = template.getQuality();
            BadHandler qbh = BadHandler.getHandler( qnda1.getType(), null );
            qnda2 = fab.makeNewNDArray( qurl, qnda1.getShape(),
                                        qnda1.getType(), qbh );
        }
        else {
            qnda2 = null;
        }

        MutableNdx ndx = new DefaultMutableNdx( template );
            
        /* Write the XML representation of the NDX to the XML stream. */
        SourceReader sr = new SourceReader();
        sr.setIncludeDeclaration( true );
        sr.setIndent( 2 );
        try {
            sr.writeSource( ndx.getHdxFacade()
                            .getSource( URLUtils.urlToUri( xurl ) ),
                            xstrm );
        }
        catch ( TransformerException e ) {
            throw (IOException) new IOException( "Trouble writing XML" )
                               .initCause( e );
        }
        catch ( HdxException e ) {
            throw (IOException) new IOException( "Trouble with HDX" )
                               .initCause( e );
        }
        xstrm.close();
        return true;
    }

    private boolean isXmlUrl( URL url ) {
        return url.getPath().endsWith( ".xml" );
    }

    /**
     * Returns a URL for a FITS file related to a given URL suitable
     * for writing array data into.
     *
     * @param  baseUrl  the URL of an XML file
     * @return  a URL suitable for writing fits data to if the XML is in
     *          baseUrl
     * @throws IOException  if something breaks
     */
    private static URL getDataUrl( URL baseUrl ) throws IOException {
        URL durl;
        if ( baseUrl.toString().endsWith( ".xml" ) ) {
            String dloc = baseUrl.toString()
                                 .replaceFirst( ".xml$", "-data.fits" );
            try {
                return new URL( dloc );
            }
            catch ( MalformedURLException e ) {
                throw new AssertionError();
            }
        }
        else {
            throw new IOException( "Cannot write data for base URL <"
                                 + baseUrl + "> not ending in '.xml'" );
        }
    }
    

    /**
     * Get an ArrayBuilder that can build Fits files.
     *
     * @return   a FitsArrayBuilder
     * @throws IOException  if the FITS handlers aren't installed
     */
    private static ArrayBuilder getFitsArrayBuilder() throws IOException {
        for ( Iterator it = arrayfact.getBuilders().iterator();
              it.hasNext(); ) {
            ArrayBuilder builder = (ArrayBuilder) it.next();
            if ( builder.getClass().getName()
                .equals( "uk.ac.starlink.oldfits.FitsArrayBuilder" ) ) {
                return builder;
            }
        }
        throw new IOException( "Can't get FitsArrayBuilder - " +
                               "FITS package not installed" );
    }
}
