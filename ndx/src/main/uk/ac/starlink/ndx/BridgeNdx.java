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
    private FrameSet wcs;
    private String title;
    private Boolean hasEtc;
    private Boolean hasTitle;
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


    public NDArray getImage() {
        return getBulkData().getImage();
    }

    public NDArray getVariance() {
        BulkDataImpl bd = getBulkData();
        if ( ! bd.hasVariance() ) {
            throw new UnsupportedOperationException( "No variance component" );
        }
        return bd.getVariance();
    }

    public NDArray getQuality() {
        BulkDataImpl bd = getBulkData();
        if ( ! bd.hasQuality() ) {
            throw new UnsupportedOperationException( "No quality component" );
        }
        return bd.getQuality();
    }

    public boolean hasVariance() {
        return getBulkData().hasVariance();
    }

    public boolean hasQuality() {
        return getBulkData().hasQuality();
    }

    public boolean hasTitle() {
        if ( hasTitle == null ) {
            hasTitle = Boolean.valueOf( impl.hasTitle() );
        }
        return hasTitle.booleanValue();
    }

    public boolean hasEtc() {
        if ( hasEtc == null ) {
            hasEtc = Boolean.valueOf( impl.hasEtc() );
        }
        return hasEtc.booleanValue();
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

    public Source getEtc() {
        if ( ! hasEtc() ) {
            throw new UnsupportedOperationException( "No Etc component" );
        }
        return impl.getEtc();
    }

    public byte getBadBits() {
        if ( badbits == -1 ) {
            badbits = impl.getBadBits();
        }
        return (byte) badbits;
    }

    public FrameSet getWCS() {
        if ( wcs == null ) {

            /* See about getting the WCS from the implementation. */
            if ( impl.hasWCS() ) {
                try {

                    /* Implementation may supply the WCS in a number of formats.
                     * Try to cope with all, or throw an exception. */
                    Object fsobj = impl.getWCS();
                    if ( fsobj instanceof FrameSet ) {
                        wcs = (FrameSet) fsobj;
                    }
                    else if ( fsobj instanceof Element ) {
                        wcs = makeWCS( new DOMSource( (Element) fsobj ) );
                    }
                    else if ( fsobj instanceof Source ) {
                        wcs = makeWCS( (Source) fsobj );
                    }
                    else {
                        logger.warning( "Unknown WCS object type " + fsobj );
                    }
                }
                catch ( IOException e ) {
                    logger.warning( "Error retrieving WCS: " + e );
                }
            }

            /* If we didn't get a WCS from the implementation for one reason
             * or another, use a default one. */
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


    private static FrameSet makeWCS( Source wcsrc ) throws IOException {

        //  Note namespace prefix is null as HDX should have
        //  transformed it!
        return (FrameSet) new XAstReader().makeAst( wcsrc, null );
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

    /**
     * Generates an XML view of this Ndx object as a <tt>Source</tt>.
     * The XML is built using only public methods of this Ndx rather than
     * any private values, so that this method can safely be inherited
     * by subclasses.
     *
     * @return  an XML Source representation of this Ndx
     */
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
        if ( hasTitle() ) {
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
            Node imComm = doc.createComment( "Image array is virtual" );
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
        FrameSet wfset = getWCS();
        if ( ! wfset.equals( defaultWCS() ) ) {
            Source wcsSource = new XAstWriter().makeSource( wfset, null );
            try {
                Node wcsContent = new SourceReader().getDOM( wcsSource );
                wcsContent = importNode( doc, wcsContent ); 
                Element wcsEl = doc.createElement( "wcs" );
                wcsEl.appendChild( wcsContent );
                ndxEl.appendChild( wcsEl );
            }
            catch ( TransformerException e ) {
                logger.warning( "Trouble transforming WCS: " + e.getMessage() );
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
                     ((Element) etcEl).getTagName() == "etc" ) {
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
        
        /* Return the new DOM as a source. */
        return new DOMSource( ndxEl );
    }

    /*
     * This package-private method is overridden by DefaultMutableNdx 
     * so that it can inherit the bulk data related functionality of 
     * this class.  Thus it is important that this class always uses
     * this method rather than the bulkdata private instance variable.
     */
    BulkDataImpl getBulkData() {
        if ( bulkdata == null ) {
            bulkdata = impl.getBulkData();
        }
        return bulkdata;
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

}
