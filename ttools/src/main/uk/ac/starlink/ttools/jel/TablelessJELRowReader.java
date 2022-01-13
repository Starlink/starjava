package uk.ac.starlink.ttools.jel;

/**
 * JELRowReader implementation which has no columns.
 * It does not inherit from StarTableJELRowReader.
 * All methods referring to columns throw an UnsupportedOperationException,
 * but since there are no columns, they should never be invoked.
 *
 * @author   Mark Taylor
 * @since    13 Oct 2014
 */
public class TablelessJELRowReader extends JELRowReader {

    /**
     * Constructor.
     */
    public TablelessJELRowReader() {
    }

    protected Class<?> getColumnClass( int icol ) {
        return null;
    }

    protected int getColumnIndexByName( String name ) {
        return -1;
    }

    protected Constant<?> getConstantByName( String name ) {
        return null;
    }

    protected boolean getBooleanColumnValue( int icol ) {
        throw new UnsupportedOperationException();
    }

    protected byte getByteColumnValue( int icol ) {
        throw new UnsupportedOperationException();
    }

    protected char getCharColumnValue( int icol ) {
        throw new UnsupportedOperationException();
    }

    protected short getShortColumnValue( int icol ) {
        throw new UnsupportedOperationException();
    }

    protected int getIntColumnValue( int icol ) {
        throw new UnsupportedOperationException();
    }

    protected long getLongColumnValue( int icol ) {
        throw new UnsupportedOperationException();
    }

    protected Object getObjectColumnValue( int icol ) {
        throw new UnsupportedOperationException();
    }

    protected float getFloatColumnValue( int icol ) {
        throw new UnsupportedOperationException();
    }

    protected double getDoubleColumnValue( int icol ) {
        throw new UnsupportedOperationException();
    }

    protected boolean isBlank( int icol ) {
        throw new UnsupportedOperationException();
    }
}
