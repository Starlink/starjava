package uk.ac.starlink.table;

/**
 * Implementation of both RowSequence and RowAccess which has no rows.
 * Singleton class.
 * 
 * @since    28 Oct 2004
 * @author   Mark Taylor (Starlink)
 */
public class EmptyRowSequence implements RowSequence, RowAccess {

    /** Instance. */
    private static final EmptyRowSequence INSTANCE = new EmptyRowSequence();

    /**
     * Private constructor prevents instantiation.
     */
    private EmptyRowSequence() {
    }

    /**
     * Always returns false.
     */
    public boolean next() {
        return false;
    }

    public void setRowIndex( long irow ) {
        throw new IllegalArgumentException( "Out of bounds (no data)" );
    }

    /**
     * Always throws IllegalStateException.
     */
    public Object getCell( int icol ) {
        throw new IllegalStateException();
    }

    /**
     * Always throws IllegalStateException.
     */
    public Object[] getRow() {
        throw new IllegalStateException();
    }

    /**
     * Does nothing.
     */
    public void close() {
    }

    /**
     * Returns singleton instance of this class.
     *
     * @return   instance
     */
    public static EmptyRowSequence getInstance() {
        return INSTANCE;
    }
}
