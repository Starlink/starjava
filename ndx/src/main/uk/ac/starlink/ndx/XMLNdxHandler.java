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
import org.w3c.dom.Text;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrayFactory;
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
                if ( tagname.equals( "image" ) || tagname.equals( "data" ) ) {
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
        try {
            url = new URL( getTextContent( el ) );
        }
        catch ( MalformedURLException e ) {
            throw (IOException) 
                  new IOException( "Bad location for " + el.getTagName() )
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
     * Provides an XML Source representation of an existing Ndx object.
     * <p>
     * This method, which just invokes {@link Ndx#toXML}, is only really
     * provided for symmetry with {@link
     * #makeNdx(javax.xml.transform.Source)}.
     *
     * @param  ndx  the Ndx object to turn into XML
     */
    public Source outputNdx( Ndx ndx ) {
        return ndx.toXML();
    }

    /**
     * Writes an XML representation of an existing Ndx object to the given 
     * (writable) URL.
     *
     * @param  url  the URL to which the Ndx is to be serialised
     * @param  ndx  the Ndx object to serialise
     * @throws java.net.UnknownServiceException if the URL does not support
     *         output (most protocols, apart from <tt>file</tt>, don't)
     * @throws IOException  if some other I/O error occurs, 
     */
    public boolean outputNdx( URL url, Ndx ndx ) throws IOException {
        OutputStream ostrm;
        if ( url.toString().equals( "file:-" ) ) {
            ostrm = System.out;
        }
        else if ( isXmlUrl( url ) ) {
            if ( url.getProtocol().equals( "file" ) ) {
                String filename = url.getPath();
                ostrm = new FileOutputStream( filename );
            }
            else {
                URLConnection conn = url.openConnection();
                conn.setDoInput( false );
                conn.setDoOutput( true );
    
                /* The following may throw a java.net.UnknownServiceException
                 * (which is-a IOException) - in fact it almost certiainly will,
                 * since I don't know of any URL protocols (including file)
                 * which support output streams. */
                conn.connect();
                ostrm = conn.getOutputStream();
            }
            ostrm = new BufferedOutputStream( ostrm );
        }
        else {
            return false;
        }
        SourceReader sr = new SourceReader();

        // this bit isn't safe - can't guarantee the transformer will be
        // invoked, so we may get no declaration after all.
        sr.setIncludeDeclaration( true );
        sr.setIndent( 2 );
        try { 
            sr.writeSource( outputNdx( ndx ), ostrm );
        }
        catch ( TransformerException e ) {
            throw (IOException) new IOException( "Trouble writing XML" )
                               .initCause( e );
        }
        ostrm.close();
        return true;
    }

    private boolean isXmlUrl( URL url ) {
        return url.getPath().endsWith( ".xml" );
    }
    
}
