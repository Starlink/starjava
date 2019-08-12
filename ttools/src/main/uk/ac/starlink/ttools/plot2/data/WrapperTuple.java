package uk.ac.starlink.ttools.plot2.data;

/**
 * Tuple implementation that delegates all methods to a base instance.
 *
 * @author   Mark Taylor
 * @since    18 Nov 2013
 */
public class WrapperTuple implements Tuple {

    private final Tuple base_;

    /**
     * Constructor.
     *
     * @param   base   tuple to which all methods are delegated
     */
    public WrapperTuple( Tuple base ) {
        base_ = base;
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
