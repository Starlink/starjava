package uk.ac.starlink.table;

import java.io.IOException;

/**
 * RowAccess which wraps another RowAccess.  This class acts as a
 * wrapper around an existing 'base' RowSequence object; all its methods
 * are implemented by forwarding them to the corresponding methods of
 * that base sequence.
 * <p>
 * This class is provided so that it can be extended by subclasses
 * which modify the view of the base sequence in useful ways.
 *
 * @author   Mark Taylor (Starlink)
 * @since    24 Jul 2020
 * @see      WrapperStarTable
 */
public class WrapperRowAccess implements RowAccess {

    private final RowAccess baseAcc_;
    private final RowData data_;

    /**
     * Constructs a new RowAccess based on a given one.
     *
     * @param  baseAcc  the base row access
     */
    public WrapperRowAccess( RowAccess baseAcc ) {
        this( baseAcc, baseAcc );
    }

    /**
     * Constructs a new RowAccess based on a given one but with
     * a supplied data access implementation.
     *
     * @param  baseAcc  the base row access
     * @param  data   RowData object whose methods will be used
     *                to implement the getCell and getRow methods
     */
    public WrapperRowAccess( RowAccess baseAcc, RowData data ) {
        baseAcc_ = baseAcc;
        data_ = data;
    }

    public void setRowIndex( long irow ) throws IOException {
        baseAcc_.setRowIndex( irow );
    }

    public Object getCell( int icol ) throws IOException {
        return data_.getCell( icol );
    }

    public Object[] getRow() throws IOException {
        return data_.getRow();
    }

    public void close() throws IOException {
        baseAcc_.close();
    }

    /**
     * Returns an indication of the wrapper structure of this object.
     *
     * @return  string representation
     */
    @Override
    public String toString() {
        StringBuffer sbuf = new StringBuffer( super.toString() );
        for ( RowAccess racc = this; racc instanceof WrapperRowAccess; ) {
            racc = ((WrapperRowAccess) racc).baseAcc_;
            sbuf.append( " -> " );
            sbuf.append( racc.getClass().getName() );
        }
        return sbuf.toString();
    }
}
