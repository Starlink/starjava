package uk.ac.starlink.table;

import java.io.IOException;

/**
 * RowSequence which wraps another RowSequence.  This class acts as a
 * wrapper around an existing 'base' RowSequence object; all its methods
 * are implemented by forwarding them to the corresponding methods of
 * that base sequence.
 * <p>
 * This class is provided so that it can be extended by subclasses
 * which modify the view of the base sequence in useful ways.
 *
 * @author   Mark Taylor (Starlink)
 * @see      WrapperStarTable
 */
public class WrapperRowSequence implements RowSequence {

    protected RowSequence baseSeq;

    /**
     * Constructs a new RowSequence based on a given one.
     *
     * @param  baseSeq  the base row sequence
     */
    public WrapperRowSequence( RowSequence baseSeq ) {
        this.baseSeq = baseSeq;
    }

    public boolean next() throws IOException {
        return baseSeq.next();
    }

    public Object getCell( int icol ) throws IOException {
        return baseSeq.getCell( icol );
    }

    public Object[] getRow() throws IOException {
        return baseSeq.getRow();
    }

    public void close() throws IOException {
        baseSeq.close();
    }

    /**
     * Returns an indication of the wrapper structure of this sequence.
     *
     * @return  string representation
     */
    public String toString() {
        StringBuffer sbuf = new StringBuffer( super.toString() );
        for ( RowSequence rseq = this; rseq instanceof WrapperRowSequence; ) {
            rseq = ((WrapperRowSequence) rseq).baseSeq;
            sbuf.append( " -> " );
            sbuf.append( rseq.getClass().getName() );
        }
        return sbuf.toString();
    }
}
