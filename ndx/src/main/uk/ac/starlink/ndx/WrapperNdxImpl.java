package uk.ac.starlink.ndx;

import javax.xml.transform.Source;
import uk.ac.starlink.array.NDArray;

/**
 * Provides an <tt>NdxImpl</tt> based on an <tt>Ndx</tt>.
 *
 * @author   Mark Taylor (Starlink)
 */
public class WrapperNdxImpl implements NdxImpl {

    private Ndx ndx;

    public WrapperNdxImpl( Ndx ndx ) {
        this.ndx = ndx;
        ndx.getClass();  // check not null
    }

    public byte getBadBits() {
        return ndx.getBadBits();
    }

    public boolean hasTitle() {
        return ndx.hasTitle();
    }

    public String getTitle() {
        return ndx.getTitle();
    }

    public boolean hasEtc() {
        return ndx.hasEtc();
    }

    public Source getEtc() {
        return ndx.getEtc();
    }

    public boolean hasWCS() {
        return true;
    }

    public Object getWCS() {
        return ndx.getWCS();
    }

    public BulkDataImpl getBulkData() {
        if ( ndx instanceof BridgeNdx ) {
            return ((BridgeNdx) ndx).getBulkData();
        }
        else {
            NDArray image = ndx.getImage();
            NDArray variance = ndx.hasVariance() ? ndx.getVariance() : null;
            NDArray quality = ndx.hasQuality() ? ndx.getQuality() : null;
            return new ArraysBulkDataImpl( image, variance, quality );
        }
    }

}
