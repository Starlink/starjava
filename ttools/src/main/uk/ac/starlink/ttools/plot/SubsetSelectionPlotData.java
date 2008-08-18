package uk.ac.starlink.ttools.plot;

/**
 * PlotData wrapper implementation which rearranges subset indexes.
 *
 * @author   Mark Taylor
 * @since    9 Apr 2008
 */
public class SubsetSelectionPlotData extends WrapperPlotData {

    private final PlotData base_;
    private final int[] isets_;

    /**
     * Constructor.
     * The <code>i</code><sup>th</code> subset in this object will be
     * the same as the <code>isets[i]</code><sup>th</code>
     * one from <code>base</code>.
     *
     * @param   base  plot data on which this is based
     * @param   isets  list of the indices of subsets from <code>base</code>
     *                 which are to appear in this object
     */
    public SubsetSelectionPlotData( PlotData base, int[] isets ) {
        super( base );
        base_ = base;
        isets_ = (int[]) isets.clone();
    }

    public int getSetCount() {
        return isets_.length;
    }

    public String getSetName( int iset ) {
        return base_.getSetName( isets_[ iset ] );
    }

    public Style getSetStyle( int iset ) {
        return base_.getSetStyle( isets_[ iset ] );
    }

    public PointSequence getPointSequence() {
        return new SubsetSelectionPointSequence( base_.getPointSequence() );
    }

    /**
     * PointSequence implementation used by this object.
     */
    private class SubsetSelectionPointSequence extends WrapperPointSequence {
        private final PointSequence baseSeq_;

        /**
         * Constructor.
         *
         * @param  baseSeq   point sequence from base PlotData
         */
        SubsetSelectionPointSequence( PointSequence baseSeq ) {
            super( baseSeq );
            baseSeq_ = baseSeq;
        }

        public boolean isIncluded( int iset ) {
            return baseSeq_.isIncluded( isets_[ iset ] );
        }
    }
}
