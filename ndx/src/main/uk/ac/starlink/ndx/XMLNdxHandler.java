package uk.ac.starlink.ndx;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.logging.Logger;
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
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.array.NDArrayFactory;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.xml.XAstReader;
import uk.ac.starlink.util.SourceReader;

/**
 * Turns URLs which reference XML files into Ndxs.
 * This is a quick and dirty implementation, and may not cope with 
 * weirdy XML files.
 * <p>
 * A URL is normally only considered suitable if it ends in '.xml'.  
 * However, the special URL "<tt>file:-</tt>" may be used to 
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
     * @throws  IOException  if some error occurs in the I/O
     */
    public Ndx makeNdx( URL url ) throws IOException {
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
        return makeNdx( xsrc );
    }

    /**
     * Constructs a readable Ndx object from an XML source.
     *
     * @param  xsrc  an XML source containing the XML representation of
     *               the NDX
     * @throws  IOException  if some error occurs in the I/O
     */
    public Ndx makeNdx( Source xsrc ) throws IOException {

        /* Get a DOM. */
        Node ndxdom;
        try {
            ndxdom = new SourceReader().getDOM( xsrc );
        }
        catch ( TransformerException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }

        /* Check we have an <ndx> element. */
        Element ndxel;
        if ( ndxdom instanceof Document ) {
            ndxel = ((Document) ndxdom).getDocumentElement();
        }
        else if ( ndxdom instanceof Element ) {
            ndxel = (Element) ndxdom;
        }
        else {
            throw new RuntimeException( "Do what?" );
        }
        if ( ! ndxel.getTagName().equals( "ndx" ) ) {
            throw new IOException( 
                "XML element of type <" + ndxel.getTagName() + "> not <ndx>" );
        }
        Document doc = ndxel.getOwnerDocument();

        /* Parse the DOM looking for known elements.  A proper implementation
         * probably wants to use XPaths and so on. */
        NDArray image = null;
        NDArray variance = null;
        NDArray quality = null;
        String title = null;
        FrameSet wcs = null;
        byte badbits = 0;
        Node etc = null;

        for ( Node child = ndxel.getFirstChild(); child != null;
              child = child.getNextSibling() ) {
            if ( child instanceof Element ) {
                Element cel = (Element) child;
                String tagname = cel.getTagName();
                if ( tagname.equals( "image" ) ) {
                    image = makeNDArray( cel, AccessMode.READ );
                }
                else if ( tagname.equals( "variance" ) ) {
                    variance = makeNDArray( cel, AccessMode.READ );
                }
                else if ( tagname.equals( "quality" ) ) {
                    quality = makeNDArray( cel, AccessMode.READ );
                }
                else if ( tagname.equals( "title" ) ) {
                    title = getTextContent( cel );
                }
                else if ( tagname.equals( "badbits" ) ) {
                    badbits = Byte.parseByte( getTextContent( cel ) );
                }
                else if ( tagname.equals( "wcs" ) ) {
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
                                        .makeAst( wcssrc, null );
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

        return makeNdx( image, variance, quality, title, wcs, badbits, etc );
    }

    private NDArray makeNDArray( Element el, AccessMode mode )
            throws IOException {
        URL url;
        String loc = getTextContent( el );
        try {
            url = new URL( loc );
        }
        catch ( MalformedURLException e ) {
            throw (IOException) 
                  new IOException( "Bad location for " + el.getTagName() 
                                 + ": " + loc )
                 .initCause( e );
        }
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
                         final FrameSet wcs, final byte badbits,
                         final Node etc )
            throws IOException {
        if ( image == null ) {
            throw new IOException( "No <image> component found" );
        }

        NdxImpl impl = new NdxImpl() {
            public BulkDataImpl getBulkData() {
                return new ArraysBulkDataImpl( image, variance, quality );
            }
            public String getTitle() {
                return title;
            }
            public Object getWCS() {
                return wcs;
            }
            public byte getBadBits() {
                return badbits;
            }
            public Source getEtc() {
                return new DOMSource( etc );
            }
            public boolean hasTitle() {
                return title != null;
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
     * or <tt>url</tt> does not end in '.xml' an IOException will be thrown.
     * This behaviour may be subject to change in future releases.
     *
     * @param  xurl the URL to which the Ndx is to be serialised in XML
     * @param  ndx  the Ndx object to serialise
     * @throws java.net.UnknownServiceException if the URL does not support
     *         output (most protocols, apart from <tt>file</tt>, don't)
     * @throws IOException  if some other I/O error occurs, 
     */
    public boolean outputNdx( URL xurl, Ndx ndx ) throws IOException {

        /* If we need one, get a URL for writing array data. */
        URL aurl = null;
        ArrayBuilder fab = null;
        if ( ! ndx.isPersistent() ) {
            if ( xurl.toString().endsWith( ".xml" ) ) {
                String aloc = xurl.toString()
                             .replaceFirst( ".xml$", "-data.fits" );
                try {
                    aurl = new URL( aloc );
                }
                catch ( MalformedURLException e ) {
                    throw new AssertionError();
                }
            }
            else {
                throw new IOException( "Cannot write non-persistent NDX to " +
                                       "URL not ending in '.xml': " + xurl );
            }

            /* Get hold of a FitsArrayBuilder, or throw an exception if
             * we don't have one. */
            for ( Iterator it = arrayfact.getBuilders().iterator();
                  it.hasNext(); ) {
                ArrayBuilder builder = (ArrayBuilder) it.next();
                if ( builder.getClass().getName()
                    .equals( "uk.ac.starlink.fits.FitsArrayBuilder" ) ) {
                    fab = builder;
                    break;
                }
            }
            if ( fab == null ) {
                throw new IOException( "Cannot write non-persistent NDX; " +
                                       "FITS package not installed" );
            }
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
                 * (which is-a IOException) - in fact it almost certiainly will,
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
        if ( ! writeArrays ) {
            xsrc = ndx.toXML();
        }
        else {
            int hdu = 0;
            URL iurl;
            URL vurl;
            URL qurl;

            Node ndxel;
            try {
                ndxel = new SourceReader().getDOM( ndx.toXML() );
            }
            catch ( TransformerException e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
 
            iurl = new URL( aurl, "#" + ++hdu );
            NDArray inda1= ndx.getImage();
            NDArray inda2 = fab.makeNewNDArray( iurl, inda1.getShape(),
                                                inda1.getType() );
            NDArrays.copy( inda1, inda2 );

            if ( ndx.hasVariance() ) {
                vurl = new URL( aurl, "#" + ++hdu );
                NDArray vnda1 = ndx.getVariance();
                NDArray vnda2 = fab.makeNewNDArray( vurl, vnda1.getShape(),
                                                    vnda1.getType() );
                NDArrays.copy( vnda1, vnda2 );
            }
            else {
                vurl = null;
            }

            if ( ndx.hasQuality() ) {
                qurl = new URL( aurl, "#" + ++hdu );
                NDArray qnda1 = ndx.getQuality();
                NDArray qnda2 = fab.makeNewNDArray( qurl, qnda1.getShape(),
                                                    qnda1.getType() );
                NDArrays.copy( qnda1, qnda2 );
            }
            else {
                qurl = null;
            }

            Document doc = ndxel.getOwnerDocument();
            NodeList children = ndxel.getChildNodes();
            for ( int i = 0; i < children.getLength(); i++ ) {
                Node node = children.item( i );
                if ( node instanceof Element &&
                     node.getNodeName().equals( "image" ) ) {
                    Node inode = doc.createElement( node.getNodeName() );
                    inode.appendChild( doc.createTextNode( iurl.toString() ) );
                    ndxel.replaceChild( inode, node );
                }
                if ( node instanceof Element &&
                     node.getNodeName().equals( "variance" ) ) {
                    Node vnode = doc.createElement( node.getNodeName() );
                    vnode.appendChild( doc.createTextNode( vurl.toString() ) );
                    ndxel.replaceChild( vnode, node );
                }
                if ( node instanceof Element &&
                     node.getNodeName().equals( "quality" ) ) {
                    Node qnode = doc.createElement( node.getNodeName() );
                    qnode.appendChild( doc.createTextNode( qurl.toString() ) );
                    ndxel.replaceChild( qnode, node );
                }
            }
            xsrc = new DOMSource( ndxel );
        }

        /* Write the XML to the XML stream. */
        SourceReader sr = new SourceReader();
        // this bit isn't safe - can't guarantee the transformer will be
        // invoked, so we may get no declaration after all.
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

    private boolean isXmlUrl( URL url ) {
        return url.getPath().endsWith( ".xml" );
    }
    
}
