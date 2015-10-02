package uk.ac.starlink.ttools.plot2.layer;

/**
 * Represents a bounded list of bins.
 * Each bin is given a fixed integer label, from zero to a specified maximum.
 * In practice, not all the bins may be used, and implementations may
 * take advantage of this.
 * Each bin accumulates a floating point value, which starts at zero.
 *
 * <p>Instances of this class are in general not thread-safe.
 *
 * @author   Mark Taylor
 * @since    20 Sep 2015
 */
public interface BinList {

    /**
     * Returns the maximum number of bins.  All bins have an index in the
     * range 0..size-1.
     *
     * @return  bin count
     */
    long getSize();

    /**
     * Returns the combination method used for bins.
     *
     * @return  combiner
     */
    Combiner getCombiner();

    /**
     * Adds a given value to the bin at the given index.
     *
     * @param   index  bin index
     * @param   value  increment for the current bin value
     */
    void addToBin( long index, double value );

    /**
     * Returns the value that has been accumulated into the given bin index.
     *
     * @param  index  bin index
     * @return   bin value
     */
    double getValue( long index );

    /**
     * Returns the range of bin values currently present in all the bins.
     *
     * @return   2-element array giving (min,max) of all bin values
     */
    double[] getBounds();
}
