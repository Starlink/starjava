package uk.ac.starlink.ndx;

import javax.xml.transform.Source;
import uk.ac.starlink.array.NDArray;

/**
 * Provides an <code>NdxImpl</code> based on an <code>Ndx</code>.
 *
 * @author   Mark Taylor (Starlink)
 */
public class WrapperNdxImpl implements NdxImpl {

    private Ndx ndx;

    public WrapperNdxImpl( Ndx ndx ) {
        this.ndx = ndx;
        if ( ndx == null ) {
            throw new NullPointerException( "Null Ndx not permitted" );
        }
    }

    public int getBadBits() {
        return ndx.getBadBits();
    }

    public boolean hasTitle() {
        return ndx.hasTitle();
    }

    public String getTitle() {
        return ndx.getTitle();
    }

    public boolean hasLabel() {
        return ndx.hasLabel();
    }

    public String getLabel() {
        return ndx.getLabel();
    }

    public boolean hasUnits() {
        return ndx.hasUnits();
    }

    public String getUnits() {
        return ndx.getUnits();
    }

    public boolean hasEtc() {
        return ndx.hasEtc();
    }

    public Source getEtc() {
        return ndx.getEtc();
    }

    public boolean hasWCS() {
        return ndx.hasWCS();
    }

    public Object getWCS() {
        return ndx.getAst();
    }

    public NDArray getImage() {
        return ndx.getImage();
    }

    public boolean hasVariance() {
        return ndx.hasVariance();
    }

    public NDArray getVariance() {
        return ndx.getVariance();
    }

    public boolean hasQuality() {
        return ndx.hasQuality();
    }

    public NDArray getQuality() {
        return ndx.getQuality();
    }

}
