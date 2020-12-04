package uk.ac.starlink.ttools.filter;

import java.util.function.Supplier;

/**
 * Calculates quantile values given a sequence of data samples.
 *
 * <p>Data must be submitted before quantiles are calculated.
 * The sequence is as follows:
 * <ol>
 * <li>Make zero or more calls to {@link #acceptDatum}
 *     and {@link #addQuantiler}</li>
 * <li>Make one or more calls to {@link #ready}</li>
 * <li>Make zero or more calls to {@link #getValueAtQuantile}</li>
 * </ol>
 * Doing it out of sequence leads to undefined behaviour.
 *
 * @author   Mark Taylor
 * @since    3 Dec 2020
 */
public interface Quantiler {

    /**
     * Accepts a value to accumulate for quantile calculations.
     * NaN values are ignored.
     *
     * @param  value  value to accumulate
     */
    void acceptDatum( double value );

    /**
     * Merges the state of another compatible accumulator into this one;
     * the effect is as if all the {@link #acceptDatum} calls that were
     * made on <code>other</code> had been made on this one.
     *
     * @param  other  compatible quantiler to merge with this
     */
    void addQuantiler( Quantiler other );

    /**
     * Call after all data has been accumulated and before quantiles are
     * to be calculated.
     */
    void ready();

    /**
     * Returns the value at a given quantile.
     *
     * @param   quantile  value in the range 0..1
     * @return   value at quantile, or NaN if no data
     */
    double getValueAtQuantile( double quantile );
}
