package uk.ac.starlink.ttools.plot2.layer;

import java.util.Arrays;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.util.DoubleList;

/**
 * Combiner implementation that accumulates all input points per pixel
 * for custom combination by a user-supplied object.
 * This kind of accumulation is likely to be very expensive on memory
 * and probably CPU as well, but it's the only way in general to
 * calculate quantities like the per-pixel median.
 *
 * @author   Mark Taylor
 * @since    6 Nov 2015
 */
@Equality
public abstract class QuantileCombiner extends Combiner {

    private final Quantiler quantiler_;

    /**
     * Constructor.
     *
     * @param   name  combiner name
     * @param   description   combiner description
     * @param   quantiler   object to combine the actual submitted data values
     */
    public QuantileCombiner( String name, String description,
                             Quantiler quantiler ) {
        super( name, description, Type.INTENSIVE, true );
        quantiler_ = quantiler;
    }

    public ArrayBinList createArrayBinList( int size ) {
        final DoubleList[] dlists = new DoubleList[ size ];
        return new ArrayBinList( size, this ) {
            public void submitToBinInt( int index, double value ) {
                DoubleList dlist = dlists[ index ];
                if ( dlist == null ) {
                    dlists[ index ] = new DoubleList( new double[] { value } );
                }
                else {
                    dlist.add( value );
                }
            }
            public double getBinResultInt( int index ) {
                DoubleList dlist = dlists[ index ];
                return dlist == null ? Double.NaN
                                     : calculateQuantile( dlist );
            }
            public void copyBin( int index, Combiner.Container bin ) {
                QuantileContainer container = (QuantileContainer) bin;
                dlists[ index ] = container.dlist_;
            }
        };
    }

    public Container createContainer() {
        return new QuantileContainer();
    }

    @Override
    public int hashCode() {
        int code = 23234232;
        code = 23 * code + quantiler_.hashCode();
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof QuantileCombiner ) {
            QuantileCombiner other = (QuantileCombiner) o;
            return this.quantiler_.equals( other.quantiler_ );
        }
        else {
            return false;
        }
    }

    /**
     * Calculates a result value from a list of submitted data values.
     *
     * @param  dlist  list of submitted data values
     * @return   result of combining submitted values
     */
    private double calculateQuantile( DoubleList dlist ) {
        double[] values = dlist.toDoubleArray();
        Arrays.sort( values );
        return quantiler_.calculateValue( values );
    }

    private class QuantileContainer implements Container {
        final DoubleList dlist_;
        QuantileContainer() {
            dlist_ = new DoubleList();
        }
        public void submit( double datum ) {
            dlist_.add( datum );
        }
        public double getCombinedValue() {
            return calculateQuantile( dlist_ );
        }
    }

    /**
     * Defines the calculation of the combined result from submitted
     * data values.
     */
    @Equality
    public interface Quantiler {

        /**
         * Calculates the output (typically, but not necessarily, quantile)
         * value from a sorted list of submitted data values.
         *
         * @param  sortedValues  sorted array of finite numeric values
         * @return   combined result value
         */
        double calculateValue( double[] sortedValues );
    }
}
