package uk.ac.starlink.table;

import java.io.IOException;
import java.util.Iterator;

/**
 * Implementation of <tt>RowSequence</tt> based on an Iterator which 
 * returns table rows in sequence.  The iterator's next method may 
 * throw an exception of the (unchecked) type 
 * {@link uk.ac.starlink.table.IteratorRowSequence.PackagedIOException}
 * if it needs to throw something (<tt>Iterator.next</tt> is not declared
 * to throw any checked exceptions).
 */
public class IteratorRowSequence implements RowSequence {

    private Iterator rowIt;
    private Object[] currentRow;

    /**
     * Constructs a new RowSequence from an Iterator.
     * Each object returned by the iterator's <tt>next</tt> method must
     * be an <tt>Object[]</tt> array representing the data in the next
     * table row (it must have one element for each column).
     * The <tt>next</tt> method may throw 
     * {@link uk.ac.starlink.table.IteratorRowSequence.PackagedIOException}s.
     *
     * @param   rowIt  iterator over the rows
     */
    public IteratorRowSequence( Iterator rowIt ) {
        this.rowIt = rowIt;
    }

    public boolean next() throws IOException {
        if ( rowIt.hasNext() ) {
            currentRow = (Object[]) rowIt.next();
            return true;
        }
        else {
            return false;
        }
    }

    public Object[] getRow() {
        if ( currentRow == null ) {
            throw new IllegalStateException( "No current row" );
        }
        else {
            return currentRow;
        }
    }

    public Object getCell( int icol ) {
        return getRow()[ icol ];
    }

    public void close() {
    }


    /**
     * Unchecked exception class to be used for smuggling 
     * <tt>IOException</tt>s out of 
     * the <tt>next</tt> method of an Iterator for use by 
     * <tt>IteratorRowSequence</tt>
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
