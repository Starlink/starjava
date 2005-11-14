package uk.ac.starlink.topcat.plot;

import java.util.Iterator;

/**
 * Stores and dispenses binned data for a histogram.
 *
 * @author   Mark Taylor
 * @since    11 Nov 2005
 */
public interface BinnedData {

    /**
     * Submits a value for inclusion in this BinnedData object.
     * As well as the value itself, a mask of boolean flags is given
     * that indicates which subsets are considered to contain the
     * submitted value.
     * 
     * @param  value  value for inclusion
     * @param  array of flags, one for each subset; true for inclusion,
     *         false for exclusion
     */
    void submitDatum( double value, boolean[] setFlags );

    /**
     * Returns an iterator over the bins managed by this object.
     * It is inadvisable to call {@link #submitDatum} during the 
     * lifetime of this iterator.
     *
     * @return   iterator which dispenses {@link BinnedData.Bin} instances
     */
    Iterator getBinIterator();

    /**
     * Represents a single bin.
     * Instances of this class are dispensed by {@link #getBinIterator}.
     */
    interface Bin {

        /**
         * Returns the lowest value which will fall into this bin.
         *
         * @return  lower bound of bin
         */
        double getLowBound();

        /**
         * Returns the highest value which will fall into this bin.
         *
         * @return  upper bound of bin
         */
        double getHighBound();

        /**
         * Returns the number of data in this bin for a given subset.
         *
         * @param  iset  subset index
         * @return   occupancy count for <code>iset</code> in this bin
         */
        int getCount( int iset );
    }
}
