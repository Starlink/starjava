package uk.ac.starlink.ndx;

import java.io.IOException;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.Requirements;

/**
 * A BulkDataImpl implementation built from the image, variance, quality
 * {@link NDArray}s.  The main work done by this class is to construct the 
 * {@link NdxAccess} object from those <tt>NDArray</tt>s.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ArraysBulkDataImpl implements BulkDataImpl {

    private final NDArray image;
    private final NDArray variance;
    private final NDArray quality;

    public ArraysBulkDataImpl( NDArray image, NDArray variance, 
                               NDArray quality ) {
        if ( image == null ) {
            throw new NullPointerException( "Image array must be present" );
        }
        this.image = image;
        this.variance = variance;
        this.quality = quality;
    }

    public boolean hasVariance() {
        return variance != null;
    }

    public boolean hasQuality() {
        return quality != null;
    }

    public NDArray getImage() {
        return image;
    }

    public NDArray getVariance() {
        return variance;
    }

    public NDArray getQuality() {
        return quality;
    }

    public NdxAccess getAccess( Requirements req, boolean wantImage,
                                boolean wantVariance, boolean wantQuality,
                                byte badbits ) throws IOException {
        return new ArraysNdxAccess( image, variance, quality, req, wantImage,
                                    wantVariance, wantQuality, badbits );
    }
}
