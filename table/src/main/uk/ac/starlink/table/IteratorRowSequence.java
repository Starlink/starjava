package uk.ac.starlink.table;

import java.io.IOException;
import java.util.Iterator;

/**
 * Implementation of <code>RowSequence</code> based on an Iterator which 
 * returns table rows in sequence.  The iterator's next method may 
 * throw an exception of the (unchecked) type 
 * {@link uk.ac.starlink.table.IteratorRowSequence.PackagedIOException}
 * if it needs to throw something (<code>Iterator.next</code> is not declared
 * to throw any checked exceptions).
 */
public class IteratorRowSequence implements RowSequence {

    private Iterator<Object[]> rowIt_;
    private Object[] currentRow_;

    /**
     * Constructs a new RowSequence from an Iterator.
     * Each object returned by the iterator's <code>next</code> method must
     * be an <code>Object[]</code> array representing the data in the next
     * table row (it must have one element for each column).
     * The <code>next</code> method may throw 
     * {@link uk.ac.starlink.table.IteratorRowSequence.PackagedIOException}s.
     *
     * @param   rowIt  iterator over the rows
     */
    public IteratorRowSequence( Iterator<Object[]> rowIt ) {
        rowIt_ = rowIt;
    }

    public boolean next() throws IOException {
        if ( rowIt_.hasNext() ) {
            currentRow_ = rowIt_.next();
            return true;
        }
        else {
            return false;
        }
    }

    public Object[] getRow() {
        if ( currentRow_ == null ) {
            throw new IllegalStateException( "No current row" );
        }
        else {
            return currentRow_;
        }
    }

    public Object getCell( int icol ) {
        return getRow()[ icol ];
    }

    public void close() {
    }


    /**
     * Unchecked exception class to be used for smuggling 
     * <code>IOException</code>s out of 
     * the <code>next</code> method of an Iterator for use by 
     * <code>IteratorRowSequence</code>
     */
    public static class PackagedIOException extends RuntimeException {

        /**
         * Construct an unchecked exception packaging a given IOException.
         *
         * @param   e  the packaged exception
         */
        public PackagedIOException( IOException e ) {
            super( e );
        }
    }
    
}
