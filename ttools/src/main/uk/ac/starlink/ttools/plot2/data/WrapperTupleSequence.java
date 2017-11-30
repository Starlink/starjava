package uk.ac.starlink.ttools.plot2.data;

/**
 * TupleSequence implementation that delegates all methods to a base instance.
 *
 * @author   Mark Taylor
 * @since    18 Nov 2013
 */
public class WrapperTupleSequence implements TupleSequence {

    private final TupleSequence base_;

    /**
     * Constructor.
     *
     * @param   base   sequence to which all methods are delegated
     */
    public WrapperTupleSequence( TupleSequence base ) {
        base_ = base;
    }

    public boolean next() {
        return base_.next();
    }

    public long getRowIndex() {
        return base_.getRowIndex();
    }

    public boolean getBooleanValue( int icol ) {
        return base_.getBooleanValue( icol );
    }

    public int getIntValue( int icol ) {
        return base_.getIntValue( icol );
    }

    public double getDoubleValue( int icol ) {
        return base_.getDoubleValue( icol );
    }

    public long getLongValue( int icol ) {
        return base_.getLongValue( icol );
    }

    public Object getObjectValue( int icol ) {
        return base_.getObjectValue( icol );
    }
}
