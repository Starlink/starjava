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
    private FrameSet ast;
    private String title;
    private Boolean hasEtc;
    private Boolean hasTitle;
    private Boolean hasVariance;
    private Boolean hasQuality;
    private Boolean hasWCS;
    private Integer badbits;
    private NDArray image;
    private NDArray variance;
    private NDArray quality;

    /**
     * Constructs an {@link Ndx} implementation from an <tt>NdxImpl</tt> object.
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

    public boolean hasEtc() {
        if ( hasEtc == null ) {
            hasEtc = Boolean.valueOf( impl.hasEtc() );
        }
        return hasEtc.booleanValue();
    }

    public boolean hasWCS() {
        if ( hasWCS == null ) {
            hasWCS = Boolean.valueOf( impl.hasWCS() );
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

    public Source getEtc() {
        if ( ! hasEtc() ) {
            throw new UnsupportedOperationException( "No Etc component" );
        }
        return impl.getEtc();
    }

    public int getBadBits() {
        if ( badbits == null ) {
            badbits = new Integer( impl.getBadBits() );
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


    private static FrameSet makeAst( Source astsrc ) throws IOException {

        //  Note namespace prefix is null as HDX should have
        //  transformed it!
        return (FrameSet) new XAstReader().makeAst( astsrc, null );
    }

    /**
     * Generates an XML view of this Ndx object as a <tt>Source</tt>.
     * The XML is built using only public methods of this Ndx rather than
     * any private values, so that this method can safely be inherited
     * by subclasses.
     *
     * @param  base  URL against which others are to be relativised
     * @return  an XML Source representation of this Ndx
     */
    public Source toXML( URL base ) {

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

        /* Get the base URI in a form suitable for using with URI.relativize. */
        URI baseUri;
        if ( base != null ) {
            try {
                baseUri = new URI( base.toExternalForm() );
                String scheme = baseUri.getScheme();
                String auth = baseUri.getAuthority();
                String path = baseUri.getPath();
                path = path.replaceFirst( "[^/]*$", "" );
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
            Element titleEl = doc.createElement( "title" );
            ndxEl.appendChild( titleEl );
            Node titleNode = doc.createTextNode( getTitle() );
            titleEl.appendChild( titleNode );
        }

        /* Write an image element. */
        Element imEl = doc.createElement( "image" );
        ndxEl.appendChild( imEl );
        if ( getImage().getURL() != null ) {
            URI iuri = urlToUri( getImage().getURL() );
            if ( baseUri != null ) {
                iuri = baseUri.relativize( iuri );
            }
            Node imUrl = doc.createTextNode( iuri.toString() );
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
                URI vuri = urlToUri( getVariance().getURL() );
                if ( baseUri != null ) {
                    vuri = baseUri.relativize( vuri );
                }
                Node varUrl = doc.createTextNode( vuri.toString() );
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
                URI quri = urlToUri( getQuality().getURL() );
                if ( baseUri != null ) {
                    quri = baseUri.relativize( quri );
                }
                Node qualUrl = doc.createTextNode( quri.toString() );
                qualEl.appendChild( qualUrl );
            }
            else {
                Node qualComm = doc.createComment( "Quality array is virtual" );
                qualEl.appendChild( qualComm );
            }
        }

        /* Write a badbits element. */
        if ( getBadBits() != 0 ) {
            String bbrep = "0x" + Integer.toHexString( getBadBits() );
            Node bbContent = doc.createTextNode( bbrep );
            Element bbEl = doc.createElement( "badbits" );
            bbEl.appendChild( bbContent );
            ndxEl.appendChild( bbEl );
        }
        
        /* Write a WCS element. */
        if ( hasWCS() ) {
            FrameSet wfset = getAst();
            Source wcsSource = new XAstWriter().makeSource( wfset, null );
            try {
                Node wcsContent = new SourceReader().getDOM( wcsSource );
                wcsContent = importNode( doc, wcsContent ); 
                Element wcsEl = doc.createElement( "wcs" );
                wcsEl.setAttribute( "encoding", "AST-XML" );
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
        return ( base != null ) ? new DOMSource( ndxEl, base.toExternalForm() )
                                : new DOMSource( ndxEl );
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

    /**
     * Turns a URL into a URI catching the exceptions.  I don't think that
     * an exception can actuallly result here, since a URIs are surely a
     * superset of URLs?  So why doesn't this method (or an equivalent 
     * constructor) exist in the URI class??.
     */
    private static URI urlToUri( URL url ) {
        try {
            return new URI( url.toExternalForm() );
        }
        catch ( URISyntaxException e ) {
            throw new AssertionError( "Failed to convert URL <" + url + "> "
                                    + "to URI" );
        }
    }

}
