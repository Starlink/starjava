package uk.ac.starlink.ndx;

import java.io.IOException;
import uk.ac.starlink.array.ArrayImpl;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.ConvertArrayImpl;
import uk.ac.starlink.array.Converter;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.array.TypeConverter;

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
    private final NDArray returnedQuality;

    public ArraysBulkDataImpl( NDArray image, NDArray variance, 
                               NDArray quality ) {
        if ( image == null ) {
            throw new NullPointerException( "Image array must be present" );
        }
        if ( quality != null && quality.getType() != Type.BYTE ) {
            throw new IllegalArgumentException( 
                "Quality array is not of type Byte" );
        }
        this.image = image;
        this.variance = variance;
        BadHandler qbh1 = quality.getBadHandler();
        this.quality = quality;
        if ( qbh1.getBadValue() == null ) {
            this.returnedQuality = quality;
        }
        else {
            /* Bad values make no sense in quality.  If we have a quality
             * array with bad values, because of the way the array handler
             * works, wrap it so it doesn't have them any more. */
            Type qtype = quality.getType();
            BadHandler qbh2 = BadHandler.getHandler( qtype, null );
            Converter qconv = new TypeConverter( qtype, qbh1, qtype, qbh2 );
            ArrayImpl qimpl = new ConvertArrayImpl( quality, qconv );
            this.returnedQuality = new BridgeNDArray( qimpl );
        }
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
        return returnedQuality;
    }

    public NdxAccess getAccess( Requirements req, boolean wantImage,
                                boolean wantVariance, boolean wantQuality,
                                byte badbits ) throws IOException {
        return new ArraysNdxAccess( image, variance, quality, req, wantImage,
                                    wantVariance, wantQuality, badbits );
    }
}
