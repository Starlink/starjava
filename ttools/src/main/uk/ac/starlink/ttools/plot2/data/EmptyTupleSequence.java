package uk.ac.starlink.ttools.plot2.data;

/**
 * TupleSequence implementation with no data.
 * The <code>next</code> method always returns false.
 *
 * @author   Mark Taylor
 * @since    19 Dec 2018
 */
public class EmptyTupleSequence implements TupleSequence {

    public boolean next() {
        return false;
    }

    public boolean getBooleanValue( int ic ) {
        throw new UnsupportedOperationException();
    }

    public int getIntValue( int ic ) {
        throw new UnsupportedOperationException();
    }

    public long getLongValue( int ic ) {
        throw new UnsupportedOperationException();
    }

    public double getDoubleValue( int ic ) {
        throw new UnsupportedOperationException();
    }

    public Object getObjectValue( int ic ) {
        throw new UnsupportedOperationException();
    }

    public long getRowIndex() {
        return -1;
    }
}
