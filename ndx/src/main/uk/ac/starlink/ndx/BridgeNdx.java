package uk.ac.starlink.ndx;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
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
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.WinMap;
import uk.ac.starlink.ast.xml.XAstReader;
import uk.ac.starlink.ast.xml.XAstWriter;
import uk.ac.starlink.util.SourceReader;

/**
 * Default <tt>Ndx</tt> implementation.
 * This class builds an <tt>Ndx</tt> from an {@link NdxImpl}.
 *
 * @author   Mark Taylor (Starlink)
 * @author   Peter Draper (Starlink)
 * @author   Norman Gray (Starlink)
 */
public class BridgeNdx implements Ndx {

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.ndx" );

    private final NdxImpl impl;
    private NDArray image;
    private NDArray variance;
    private NDArray quality;
    private FrameSet wcs;
    private String title;
    private int badbits = -1;
    private BulkDataImpl bulkdata;

    /**
     * Constructs an {@link Ndx} implementation from an <tt>NdxImpl</tt> object.
     *
     * @param  impl  object which provides services to this BridgeNdx
     */
    public BridgeNdx( NdxImpl impl ) {
        this.impl = impl;
    }

    public NdxAccess getAccess( Requirements req, boolean wantImage,
                                boolean wantVariance, boolean wantQuality ) 
            throws IOException {
        return getBulkData().getAccess( req, wantImage, wantVariance, 
                                        wantQuality, getBadBits() );
    }

    public byte getBadBits() {
        if ( badbits == -1 ) {
            badbits = impl.getBadBits();
        }
        return (byte) badbits;
    }

    public String getTitle() {
        if ( ! impl.hasTitle() ) {
            throw new UnsupportedOperationException( "No title component" );
        }
        if ( title == null ) {
            title = impl.getTitle();
        }
        return title;
    }

    public NDArray getImage() {
        if ( image == null ) {
            image = getBulkData().getImage();
        }
        return image;
    }

    public NDArray getVariance() {
        BulkDataImpl bd = getBulkData();
        if ( ! bd.hasVariance() ) {
            throw new UnsupportedOperationException( "No variance component" );
        }
        if ( variance == null ) {
            variance = bd.getVariance();
        }
        return variance;
    }

    public NDArray getQuality() {
        BulkDataImpl bd = getBulkData();
        if ( ! bd.hasQuality() ) {
            throw new UnsupportedOperationException( "No quality component" );
        }
        if ( quality == null ) {
            quality = bd.getQuality();
        }
        return quality;
    }

    public boolean hasVariance() {
        return getBulkData().hasVariance();
    }

    public boolean hasQuality() {
        return getBulkData().hasQuality();
    }

    public boolean hasTitle() {
        return impl.hasTitle();
    }

    public boolean hasWCS() {
        return impl.hasWCS();
    }

    public FrameSet getWCS() {
        if ( wcs == null ) {

            try {
                /* Implementation may supply the WCS in a number of formats.
                 * Try to cope with all, or throw an exception. */
                Object fsobj = impl.getWCS();
                if ( fsobj instanceof FrameSet ) {
                    wcs = (FrameSet) fsobj;
                }
                else if ( fsobj instanceof Element ) {
                    wcs = makeWCS( (Element) fsobj );
                }
                else if ( fsobj instanceof Source ) {
                    Node el = new SourceReader().getDOM( (Source) fsobj );
                    wcs = makeWCS( (Element) el );
                }
                else {
                    logger.warning( "Unknown WCS object type " + fsobj );
                }
            }
            catch ( IOException e ) {
                logger.warning( "Error retrieving WCS: " + e );
            }
            catch ( TransformerException e ) {
                logger.warning( "Error transforming WCS: " + e );
            }
            if ( wcs == null ) {
                wcs = defaultWCS();
            }
        }
        return wcs;
    }

    public boolean isPersistent() {
        return ( getImage().getURL() != null )
            && ( ! hasVariance() || getVariance().getURL() != null )
            && ( ! hasQuality() || getQuality().getURL() != null );
    }


    private static FrameSet makeWCS( Element wcsel ) throws IOException {

        //  Note namespace prefix is null as HDX should have
        //  transformed it!
        XAstReader xr = new XAstReader();
        return (FrameSet) xr.makeAst(wcsel, null);
    }

    private FrameSet defaultWCS() {
        NDShape imshape = getImage().getShape();
        int ndim = imshape.getNumDims();
        Frame gridfrm = new Frame( ndim );
        gridfrm.setDomain( "GRID" );
        Frame imfrm = new Frame( ndim );
        imfrm.setDomain( "IMAGE-PIXEL" );
        FrameSet wcs = new FrameSet( gridfrm );
        Mapping gpmap = translateMap( getImage().getShape() );
        wcs.addFrame( FrameSet.AST__BASE, gpmap, imfrm );
        return wcs;
    }

    private static Mapping translateMap( NDShape shape ) {
        int ndim = shape.getNumDims();
        double[] ina = new double[ ndim ];
        double[] inb = new double[ ndim ];
        double[] outa = new double[ ndim ];
        double[] outb = new double[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            ina[ i ] = 0.5;
            inb[ i ] = 1.5;
            outa[ i ] = 0.0;
            outb[ i ] = 1.0;
        }
        return new WinMap( ndim, ina, inb, outa, outb );
    }

    public Source toXML() {

        /* Set up the document and root element. */
        DocumentBuilderFactory dfact = DocumentBuilderFactory.newInstance();
        DocumentBuilder dbuild;
        try {
            dbuild = dfact.newDocumentBuilder();
        }
        catch ( ParserConfigurationException e ) {
            throw new RuntimeException( "Trouble building vanilla parser", e );
        }
        Document doc = dbuild.newDocument();
        Element ndxEl = doc.createElement( "ndx" );
        doc.appendChild( ndxEl );

        /* Write a title element. */
        if ( getTitle() != null ) {
            Element titleEl = doc.createElement( "title" );
            ndxEl.appendChild( titleEl );
            Node titleNode = doc.createTextNode( getTitle() );
            titleEl.appendChild( titleNode );
        }

        /* Write an image element. */
        Element imEl = doc.createElement( "image" );
        ndxEl.appendChild( imEl );
        if ( getImage().getURL() != null ) {
            Node imUrl = doc.createTextNode( getImage().getURL()
                                            .toExternalForm() );
            imEl.appendChild( imUrl );
        }
        else {
            Node imComm = doc.createComment( "Data array is virtual" );
            imEl.appendChild( imComm );
        }

        /* Write a variance element. */
        if ( hasVariance() ) {
            Element varEl = doc.createElement( "variance" );
            ndxEl.appendChild( varEl );
            if ( getVariance().getURL() != null ) {
                Node varUrl = doc.createTextNode( getVariance().getURL()
                                                 .toExternalForm() );
                varEl.appendChild( varUrl );
            }
            else {
                Node varComm = doc.createComment( "Variance array is virtual" );
                varEl.appendChild( varComm );
            }
        }

        /* Write a quality element. */
        if ( hasQuality() ) {
            Element qualEl = doc.createElement( "quality" );
            ndxEl.appendChild( qualEl );
            if ( getQuality().getURL() != null ) {
                Node qualUrl = doc.createTextNode( getQuality().getURL()
                                                  .toExternalForm() );
                qualEl.appendChild( qualUrl );
            }
            else {
                Node qualComm = doc.createComment( "Quality array is virtual" );
                qualEl.appendChild( qualComm );
            }
        }

        /* Write a badbits element. */
        if ( getBadBits() != (byte) 0x00 ) {
            Node bbContent = 
                doc.createTextNode( Byte.toString( getBadBits() ) );
            Element bbEl = doc.createElement( "badbits" );
            bbEl.appendChild( bbContent );
            ndxEl.appendChild( bbEl );
        }
        
        /* Write a WCS element. */
        if ( impl.hasWCS() ) {
            Node wcsContent = new XAstWriter().makeElement( getWCS(), null );
            wcsContent = importNode( doc, wcsContent ); 
            Element wcsEl = doc.createElement( "wcs" );
            wcsEl.appendChild( wcsContent );
            ndxEl.appendChild( wcsEl );
        }

        /* Write an Etc element. */
        if ( impl.hasEtc() ) {
            try {
                Source etcSrc = impl.getEtc();
                Node etcContent = new SourceReader().getDOM( etcSrc );
                etcContent = importNode( doc, etcContent );
                Element etcEl = doc.createElement( "etc" );
                etcEl.appendChild( etcContent );
                ndxEl.appendChild( etcEl );
            }
            catch ( TransformerException e ) {
                logger.warning( "Error transforming Etc component" );
                ndxEl.appendChild( doc.createComment( "Broken ETC" ) );
            }
        }
        
        /* Return the new DOM as a source. */
        return new DOMSource( ndxEl );
    }

    private BulkDataImpl getBulkData() {
        if ( bulkdata == null ) {
            bulkdata = impl.getBulkData();
        }
        return bulkdata;
    }

    /**
     * Imports a node into a document, just as
     *
     *     Document.importNode( node, true )
     *
     * is supposed to do.  However, when the imported node is a 
     * DocumentFragment, Crimson throws:
     *
     *   org.apache.crimson.tree.DomEx: 
     *      HIERARCHY_REQUEST_ERR: This node isn't allowed there
     *
     * I'm almost certain this is a bug in crimson.  This method works 
     * around it.  If crimson (or whatever parser we're using) gets 
     * fixed it can be replaced with Document.importNode.
     */
    private Node importNode( Document doc, Node inode ) {
        if ( inode instanceof DocumentFragment ) {
            Node onode = doc.createDocumentFragment();
            for ( Node ichild = inode.getFirstChild(); ichild != null; 
                  ichild = ichild.getNextSibling() ) {
                Node ochild = doc.importNode( ichild, true );
                onode.appendChild( ochild );
            }
            return onode;
        }
        else {
            return doc.importNode( inode, true );
        }
    }

}
