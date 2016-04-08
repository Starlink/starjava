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
        super( name, description );
        quantiler_ = quantiler;
    }

    public BinList createArrayBinList( int size ) {
        final DoubleList[] dlists = new DoubleList[ size ];
        return new ArrayBinList( size, this, true ) {
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
        };
    }

    public BinList createHashBinList( long size ) {
        return new HashBinList( size, this, true );
    }

    public Container createContainer() {
        final DoubleList dlist_ = new DoubleList();
        return new Container() {
            public void submit( double datum ) {
                dlist_.add( datum );
            }
            public double getResult() {
                return calculateQuantile( dlist_ );
            }
        };
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
