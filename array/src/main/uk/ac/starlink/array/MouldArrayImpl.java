package uk.ac.starlink.array;

/**
 * Wraps an NDArray to provide one with identical pixel data, but viewed
 * as a different shape, that is with its origin or dimensions different.
 *
 * @author   Mark Taylor (Starlink)
 */
public class MouldArrayImpl extends WrapperArrayImpl {

    private final OrderedNDShape oshape;

    /**
     * Creates a new ArrayImpl which which uses the pixels from an underlying
     * NDArray moulded into a different shape (origin/dimensions).
     * The number of pixels must be the same in the base NDArray and
     * the requested new shape.
     *
     * @param    nda    the base NDArray which will supply the pixels
     * @param    shape  the shape which this NDArray is to have
     * @throws   IllegalArgumentException   if shape is does not have the
     *           same number of pixels as the shape of nda
     */
    public MouldArrayImpl( NDArray nda, NDShape shape ) {
        super( nda );

        if ( shape.getNumPixels() != nda.getShape().getNumPixels() ) {
            throw new IllegalArgumentException(
                "Moulded array has different number of pixels (" +
                shape.getNumPixels() + ") " +
                "from base array (" + nda.getShape().getNumPixels() + ")" );
        }

        this.oshape =
            new OrderedNDShape( shape, nda.getShape().getOrder() );
    }

    public OrderedNDShape getShape() {
        return oshape;
    }

    /* XXX Should inherit mapping behaviour from base NDArray too, but
     * there are problems with when the canMap and getMapped methods
     * may get called. */
}

