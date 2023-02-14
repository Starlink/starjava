package uk.ac.starlink.ttools.plot2;

import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * Splittable iterator over the data positions in a list of PositionClouds.
 *
 * @author   Mark Taylor
 * @since    13 Sep 2019
 */
public class DataPosSequence implements CoordSequence {

    private final int ndim_;
    private final PositionCloud[] clouds_;
    private final DataStore dataStore_;
    private final double[] dpos_;
    private int ic_;
    private int icHi_;
    private DataGeom geom_;
    private int iPosCoord_;
    private TupleSequence tseq_;

    /**
     * Constructor.
     *
     * @param   ndim  coordinate dimensionality
     * @param   clouds  list of point clouds providing positions over
     *                  which to iterate
     * @param   dataStore  data storage object
     */
    public DataPosSequence( int ndim, PositionCloud[] clouds,
                            DataStore dataStore ) {
        this( ndim, clouds, dataStore, 0, clouds.length, null );
    }

    /**
     * Internal constructor for recursion.
     *
     * @param   ndim  coordinate dimensionality
     * @param   clouds  list of point clouds providing positions over
     *                  which to iterate
     * @param   dataStore  data storage object
     * @param   ic   index of current cloud in array
     * @param   icHi   index of first cloud in array not to iterate over
     * @param   tseq   iterator over current cloud,
     *                 or null to start at the beginning
     */
    private DataPosSequence( int ndim, PositionCloud[] clouds,
                             DataStore dataStore,
                             int ic, int icHi, TupleSequence tseq ) {
        ndim_ = ndim;
        clouds_ = clouds;
        dataStore_ = dataStore;
        dpos_ = new double[ ndim ];
        ic_ = ic;
        icHi_ = icHi;
        if ( ic < icHi ) {
            initCloud( clouds[ ic ], tseq );
        }
        else {
            tseq_ = PlotUtil.EMPTY_TUPLE_SEQUENCE;
        }
        assert tseq_ != null;
    }

    public double[] getCoords() {
        return dpos_;
    }

    public boolean next() {
        while ( tseq_.next() ) {
            if ( geom_.readDataPos( tseq_, iPosCoord_, dpos_ ) ) {
                return true;
            }
        }
        if ( ic_ + 1 < icHi_ ) {
            initCloud( clouds_[ ++ic_ ], null );
            return next();
        }
        else {
            return false;
        }
    }

    public DataPosSequence split() {
        int ncloud = icHi_ - ic_;
        if ( ncloud >= 2 ) {
            int lo = ic_;
            int mid = ( ic_ + icHi_ ) / 2;
            ic_ = mid;
            return new DataPosSequence( ndim_, clouds_, dataStore_,
                                        lo, mid, null );
        }
        else if ( ncloud == 1 ) {
            TupleSequence tseq1 = tseq_.split();
            return tseq1 == null
                 ? null
                 : new DataPosSequence( ndim_, clouds_, dataStore_,
                                        ic_, icHi_, tseq1 );
        }
        else {
            return null;
        }
    }

    public long splittableSize() {
        long count = tseq_.splittableSize();
        for ( int i = ic_ + 1; i < icHi_; i++ ) {
            long nrow = clouds_[ i ].getTupleCount();
            if ( nrow < 0 ) {
                return -1;
            }
            count += nrow;
        }
        return count;
    }

    /**
     * Prepare to start iterating over a new cloud.
     *
     * @param  cloud  cloud to initialise for
     * @param  tseq   tuple sequence to use for iteration,
     *                or null to start at the beginning
     */
    private void initCloud( PositionCloud cloud, TupleSequence tseq ) {
        geom_ = cloud.getDataGeom();
        iPosCoord_ = cloud.getPosCoordIndex();
        tseq_ = tseq == null
              ? cloud.createTupleSequence( dataStore_ )
              : tseq;
    }

    /**
     * Adaptor interface defining a cloud of positions for use with
     * DataPosSequence.
     */
    public interface PositionCloud {

        /**
         * Returns the index of the data spec coordinate at which the
         * position information starts for this cloud.
         *
         * @return  position coordinate index
         */
        int getPosCoordIndex();

        /**
         * Returns the DataGeom for this cloud.
         *
         * @return  geom
         */
        DataGeom getDataGeom();

        /**
         * Returns a new tuple sequence that iterates over the points
         * in this cloud.
         *
         * @param  dataStore  data storage object
         * @return   new tuple sequence
         */
        TupleSequence createTupleSequence( DataStore dataStore );

        /**
         * Returns the approximate number of tuples that will be iterated
         * over by the tuple sequence this object creates.
         *
         * @return  approximate tuple count
         */
        long getTupleCount();
    }
}
