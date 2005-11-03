package uk.ac.starlink.topcat.plot;

import uk.ac.starlink.table.ValueStore;
import uk.ac.starlink.table.storage.ArrayPrimitiveStore;

/**
 * Points implementation based on a ValueStore.
 * Is writable as well as readable.
 *
 * @author   Mark Taylor
 * @since    2 Nov 2005
 */
public class ValueStorePoints implements Points {

    private final int ndim_;
    private final int npoint_;
    private final ValueStore store_;

    /**
     * Constructs a new points object.
     *
     * @param  ndim  dimensionality
     * @param  number of points
     */
    public ValueStorePoints( int ndim, int npoint ) {
        ndim_ = ndim;
        npoint_ = npoint;

        /* Currently an ArrayPrimitiveStore implementation is hardwired in.
         * At some point this should be modified so that it uses the
         * default storage policy to get a suitable store object. */
        store_ = new ArrayPrimitiveStore( double.class, ndim * npoint );
    }

    public int getNdim() {
        return ndim_;
    }

    public int getCount() {
        return npoint_;
    }

    public void getCoords( int ipoint, double[] coords ) {
        store_.get( ipoint * ndim_, coords );
    }

    /**
     * Stores a point in the vector.
     *
     * @param   ipoint  index of point
     * @param   coords  coordinate array 
     */
    public void putCoords( int ipoint, double[] coords ) {
        store_.put( ipoint * ndim_, coords );
    }
}
