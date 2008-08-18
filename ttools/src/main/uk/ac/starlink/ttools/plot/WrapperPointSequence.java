package uk.ac.starlink.ttools.plot;

/**
 * PointSequence implementation based on an existing PointSequence object.
 * All behaviour is delegated to the base.
 *
 * @author   Mark Taylor
 * @since    24 Apr 2008
 */
public class WrapperPointSequence implements PointSequence {

    private final PointSequence base_;
 
    /**
     * Constructor.
     *
     * @param   base   base object
     */
    public WrapperPointSequence( PointSequence base ) {
        base_ = base;
    }

    public boolean next() {
        return base_.next();
    }

    public double[] getPoint() {
        return base_.getPoint();
    }

    public double[][] getErrors() {
        return base_.getErrors();
    }

    public String getLabel() {
        return base_.getLabel();
    }

    public boolean isIncluded( int iset ) {
        return base_.isIncluded( iset );
    }

    public void close() {
        base_.close();
    }
}
