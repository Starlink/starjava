package uk.ac.starlink.ttools.plot;

import java.util.Iterator;

/**
 * Wrapper implementation of BinnedData which normalises bins so that 
 * the total value of all bins in a given subset is unity.
 *
 * @author   Mark Taylor
 * @since    28 May 2008
 */
public class NormalisedBinnedData implements BinnedData {

    private final BinnedData base_;
    private final double[] weightSums_;

    /**
     * Constructor.
     *
     * @param  base  binned data object providing basic functionality
     */
    public NormalisedBinnedData( BinnedData base ) {
        base_ = base;
        weightSums_ = new double[ base.getSetCount() ];
    }

    public void submitDatum( double value, double weight, boolean[] setFlags ) {
        base_.submitDatum( value, weight, setFlags );
        for ( int is = 0; is < setFlags.length; is++ ) {
            if ( setFlags[ is ] ) {
                weightSums_[ is ] += weight;
            }
        }
    }

    public int getSetCount() {
        return base_.getSetCount();
    }

    /**
     * Returns false.
     */
    public boolean isInteger() {
        return false;
    }

    public Iterator getBinIterator( boolean includeEmpty ) {
        final Iterator baseIt = base_.getBinIterator( includeEmpty );
        return new Iterator() {
            public boolean hasNext() {
                return baseIt.hasNext();
            }
            public void remove() {
                baseIt.remove();
            }
            public Object next() {
                final Bin baseBin = (Bin) baseIt.next();
                return new Bin() {
                    public double getLowBound() {
                        return baseBin.getLowBound();
                    }
                    public double getHighBound() {
                        return baseBin.getHighBound();
                    }
                    public double getWeightedCount( int iset ) {
                        double wsum = weightSums_[ iset ];
                        return wsum == 0 
                             ? 0.0
                             : baseBin.getWeightedCount( iset ) / wsum;
                    }
                };
            }
        };
    }
}
