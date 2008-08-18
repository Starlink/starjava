package uk.ac.starlink.ttools.plot;

/**
 * PlotData implementation based on an existing PlotData object.
 * All behaviour is delegated to the base.
 *
 * @author    Mark Taylor
 * @since     24 Apr 2008
 */
public class WrapperPlotData implements PlotData {

    private final PlotData base_;

    /**
     * Constructor.
     *
     * @param  base  base plot data
     */
    public WrapperPlotData( PlotData base ) {
        base_ = base;
    }

    public int getNdim() {
        return base_.getNdim();
    }

    public int getNerror() {
        return base_.getNerror();
    }

    public int getSetCount() {
        return base_.getSetCount();
    }

    public String getSetName( int iset ) {
        return base_.getSetName( iset );
    }

    public Style getSetStyle( int iset ) {
        return base_.getSetStyle( iset );
    }

    public boolean hasLabels() {
        return base_.hasLabels();
    }

    public PointSequence getPointSequence() {
        return base_.getPointSequence();
    }
}
