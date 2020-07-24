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
public abstract class WrapperRowAccess implements RowAccess {

    private final RowAccess baseAcc_;

    /**
     * Constructs a new RowAccess based on a given one.
     *
     * @param  baseAcc  the base row access
     */
    public WrapperRowAccess( RowAccess baseAcc ) {
        baseAcc_ = baseAcc;
    }

    public void setRowIndex( long irow ) throws IOException {
        baseAcc_.setRowIndex( irow );
    }

    public Object getCell( int icol ) throws IOException {
        return baseAcc_.getCell( icol );
    }

    public Object[] getRow() throws IOException {
        return baseAcc_.getRow();
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
