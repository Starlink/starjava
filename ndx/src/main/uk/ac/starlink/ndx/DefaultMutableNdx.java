package uk.ac.starlink.ndx;

import java.io.IOException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.xml.XAstReader;

/**
 * Provides a simple implementation of the <code>MutableNdx</code> interface,
 * so provides mutator methods as well as the accessor methods of Ndx.
 * This class can be used to wrap a (presumably immutable) existing Ndx 
 * object, or to construct a mutable Ndx from an NdxImpl object.
 *
 * @author   Mark Taylor (Starlink)
 */
public class DefaultMutableNdx extends BridgeNdx implements MutableNdx {

    private int badbits = 0;
    private Element etc = null;
    private FrameSet ast = null;
    private String title = null;
    private String label = null;
    private String units = null;
    private NDArray image = null;
    private NDArray variance = null;
    private NDArray quality = null;
    private boolean astSet = false;
    private boolean imageSet = false;
    private boolean varianceSet = false;
    private boolean qualitySet = false;
    private boolean badbitsSet = false;
    private boolean etcSet = false;
    private boolean titleSet = false;
    private boolean labelSet = false;
    private boolean unitsSet = false;

    /**
     * Constructs a MutableNdx from a given NdxImpl. 
     * The resulting object will behave just as a <code>BridgeNdx</code>
     * constructed from <code>impl</code>
     * until such time as one of the <code>set</code> methods is used on it,
     * after which time the new value(s) will be returned by the 
     * corresponding <code>get</code> methods.
     *
     * @param  impl  the implementation object on which this Ndx will be based
     */
    public DefaultMutableNdx( NdxImpl impl ) {
        super( impl );
    }

    /**
     * Constructs a MutableNdx from a given Ndx.
     * The resulting object will behave just as the original Ndx
     * until such time as one of the <code>set</code> methods is used on it,
     * after which time the new value(s) will be returned by the 
     * corresponding <code>get</code> methods.
     *
     * @param  ndx  the underlying Ndx on which this one will be based
     */
    public DefaultMutableNdx( Ndx ndx ) {
        this( new WrapperNdxImpl( ndx ) );
    }

    /**
     * Constructs a MutableNdx from a given <code>NDArray</code> which
     * will be its Image component.
     * The resulting object has no optional components (title, etc and so on),
     * though these can be set using the various <code>set</code> methods.
     *
     * @param  image  the Image component of this Ndx
     * @throws  NullPointerException  if <code>image</code> is <code>null</code>
     */
    public DefaultMutableNdx( final NDArray image ) {
        this( new NdxImpl() {
            public NDArray getImage() { return image; }
            public boolean hasVariance() { return false; }
            public NDArray getVariance() { return null; }
            public boolean hasQuality() { return false; }
            public NDArray getQuality() { return null; }
            public int getBadBits() { return 0; }
            public boolean hasEtc() { return false; }
            public Source getEtc() { return null; }
            public boolean hasTitle() { return false; }
            public String getTitle() { return null; }
            public boolean hasLabel() { return false; }
            public String getLabel() { return null; }
            public boolean hasUnits() { return false; }
            public String getUnits() { return null; }
            public boolean hasWCS() { return false; }
            public Object getWCS() { return null; }
        } );
        if ( image == null ) {
            throw new NullPointerException( 
                "Null image component not permitted" );
        }
    }

    public void setImage( NDArray image ) {
        if ( image == null ) {
            throw new NullPointerException( "Null image array not permitted" );
        }
        imageSet = true;
        this.image = image;
    }
    public NDArray getImage() {
        if ( imageSet ) {
            return image;
        }
        else {
            return super.getImage();
        }
    }

    public void setVariance( NDArray variance ) {
        varianceSet = true;
        this.variance = variance;
    }
    public boolean hasVariance() {
        return varianceSet ? ( variance != null ) : super.hasVariance();
    }
    public NDArray getVariance() {
        if ( varianceSet ) {
            if ( hasVariance() ) {
                return variance;
            }
            else {
                throw new UnsupportedOperationException( 
                    "No variance component" );
            }
        }
        else {
            return super.getVariance();
        }
    }

    public void setQuality( NDArray quality ) {
        if ( quality != null && quality.getType().isFloating() ) {
            throw new IllegalArgumentException( 
                "Non-integer typed quality array (" + quality.getType() + 
                ") not permitted" );
        }
        qualitySet = true;
        this.quality = quality;
    }
    public boolean hasQuality() {
        return qualitySet ? ( quality != null ) : super.hasQuality();
    }
    public NDArray getQuality() {
        if ( qualitySet ) {
            if ( hasQuality() ) {
                return quality;
            }
            else {
                throw new UnsupportedOperationException( 
                    "No quality component" );
            }
        }
        else {
            return super.getQuality();
        }
    }
        
    public void setBadBits( int badbits ) {
        badbitsSet = true;
        this.badbits = badbits;
    }
    public int getBadBits() {
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

    public void setLabel( String label ) {
        labelSet = true;
        this.label = label;
    }
    public boolean hasLabel() {
        return labelSet ? ( label != null ) : super.hasLabel();
    }
    public String getLabel() {
        if ( labelSet ) {
            if ( hasLabel() ) {
                return label;
            }
            else {
                throw new UnsupportedOperationException( "No label component" );
            }
        }
        else {
            return super.getLabel();
        }
    }

    public void setUnits( String units ) {
        unitsSet = true;
        this.units = units;
    }
    public boolean hasUnits() {
        return unitsSet ? ( units != null ) : super.hasUnits();
    }
    public String getUnits() {
        if ( unitsSet ) {
            if ( hasUnits() ) {
                return units;
            }
            else {
                throw new UnsupportedOperationException( "No units component" );
            }
        }
        else {
            return super.getUnits();
        }
    }

    public void setWCS( Object wcsob ) {
        if ( wcsob == null ) {
            ast = null;
        }
        else if ( wcsob instanceof FrameSet ) {
            ast = (FrameSet) wcsob;
        }
        else if ( wcsob instanceof Source ) {
            Source wcssrc = (Source) wcsob;
            try {
                ast = (FrameSet) new XAstReader().makeAst( wcssrc );
            }
            catch ( IOException e ) {
                throw (RuntimeException)
                      new IllegalArgumentException( 
                          "Error transforming WCS Source" )
                     .initCause( e );
            }
        }
        else {
            throw new IllegalArgumentException( 
                "Unsupported object used to set WCS:" + wcsob );
        }
        astSet = true;
    }
    public boolean hasWCS() {
        return astSet ? ( ast != null ) : super.hasWCS();
    }
    public FrameSet getAst() {
        if ( astSet ) {
            if ( hasWCS() ) {
                return ast;
            }
            else {
                throw new UnsupportedOperationException( "No WCS component" );
            }
        }
        else {
            return super.getAst();
        }
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

}
