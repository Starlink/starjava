package uk.ac.starlink.ndx;

import java.io.IOException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.xml.XAstReader;

/**
 * Provides a simple implementation of the <tt>MutableNdx</tt> interface,
 * so provides mutator methods as well as the accessor methods of Ndx.
 * This class can be used to wrap a (presumably immutable) existing Ndx 
 * object, or to construct a mutable Ndx from an NdxImpl or a BulkDataImpl
 * object.
 *
 * @author   Mark Taylor (Starlink)
 */
public class DefaultMutableNdx extends BridgeNdx implements MutableNdx {

    private byte badbits = -1;
    private Element etc = null;
    private FrameSet wcs = null;
    private String title = null;
    private BulkDataImpl bulkdata = null;
    private boolean badbitsSet = false;
    private boolean etcSet = false;
    private boolean titleSet = false;

    /**
     * Constructs a MutableNdx from a given NdxImpl. 
     * The resulting object will behave just as a <tt>BridgeNdx</tt>
     * constructed from <tt>impl</tt>
     * until such time as one of the <tt>set</tt> methods is used on it,
     * after which time the new value(s) will be returned by the 
     * corresponding <tt>get</tt> methods.
     *
     * @param  impl  the implementation object on which this Ndx will be based
     */
    public DefaultMutableNdx( NdxImpl impl ) {
        super( impl );
    }

    /**
     * Constructs a MutableNdx from a given Ndx.
     * The resulting object will behave just as the original Ndx
     * until such time as one of the <tt>set</tt> methods is used on it,
     * after which time the new value(s) will be returned by the 
     * corresponding <tt>get</tt> methods.
     *
     * @param  ndx  the underlying Ndx on which this one will be based
     */
    public DefaultMutableNdx( Ndx ndx ) {
        this( new WrapperNdxImpl( ndx ) );
    }

    /**
     * Constructs a MutableNdx from a given bulk data implementation.
     * The resulting object has no optional components (title, etc and so on),
     * though these can be set using the various <tt>set</tt> methods.
     *
     * @param  datimpl  the bulk data implementation to be used by this Ndx
     */
    public DefaultMutableNdx( final BulkDataImpl datimpl ) {
        this( new NdxImpl() {
            public BulkDataImpl getBulkData() { return datimpl; }
            public byte getBadBits() { return (byte) 0; }
            public boolean hasEtc() { return false; }
            public Source getEtc() { return null; }
            public boolean hasTitle() { return false; }
            public String getTitle() { return null; }
            public boolean hasWCS() { return false; }
            public Object getWCS() { return null; }
        } );
    }

    public void setBadBits( byte badbits ) {
        badbitsSet = true;
        this.badbits = badbits;
    }
    public byte getBadBits() {
        return badbitsSet ? badbits : super.getBadBits();
    }

    public void setTitle( String title ) {
        titleSet = true;
        this.title = title;
    }
    public boolean hasTitle() {
        return titleSet ? ( title != null ) : super.hasTitle();
    }
    public String getTitle() {
        if ( titleSet ) {
            if ( hasTitle() ) {
                return title;
            }
            else {
                throw new UnsupportedOperationException( "No title component" );
            }
        }
        else {
            return super.getTitle();
        }
    }

    public void setWCS( Object wcsob ) throws IOException {
        if ( wcsob instanceof FrameSet ) {
            wcs = (FrameSet) wcsob;
        }
        else if ( wcsob instanceof Source ) {
            Source wcssrc = (Source) wcsob;
            wcs = (FrameSet) new XAstReader().makeAst( wcssrc, null );
        }
    }
    public FrameSet getWCS() {
        return ( wcs != null ) ? wcs : super.getWCS();
    }
 
    public void setEtc( Node etc ) {
        Element etcel = null;
        if ( etc instanceof Element ) {
            etcel = (Element) etc;
        }
        else if ( etc instanceof Document ) {
            etcel = ((Document) etc).getDocumentElement();
        }
        if ( etcel != null && etcel.getTagName().equals( "etc" ) ) {
            etcSet = true;
            this.etc = etcel;
        }
        else {
            throw new IllegalArgumentException( 
                "Supplied node is not of type <etc>" );
        }
    }
    public boolean hasEtc() {
        return etcSet ? ( etc != null ) : super.hasEtc();
    }
    public Source getEtc() {
        if ( etcSet ) {
            if ( hasEtc() ) {
                return new DOMSource( etc );
            }
            else {
                throw new UnsupportedOperationException( "No Etc component" );
            }
        }
        else {
            return super.getEtc();
        }
    }

    /**
     * Sets the bulk data implementation for this Ndx.
     * This does the work for all the array access methods 
     * <tt>getAccess</tt>, <tt>hasVariance</tt>, <tt>hasQuality</tt>,
     * <tt>getImage</tt>, <tt>getVariance</tt>, <tt>getQuality</tt>.
     */
    public void setBulkData( BulkDataImpl bulkdata ) {
        bulkdata.getClass();  // check not null
        this.bulkdata = bulkdata;
    }

    /*
     * Overrides the package-private method in BridgeNDArray which is 
     * used for all bulk data access, so we can inherit its behaviour 
     * for all bulk data related methods.
     */
    BulkDataImpl getBulkData() {
        return ( bulkdata != null ) ? bulkdata : super.getBulkData();
    }

   
}
