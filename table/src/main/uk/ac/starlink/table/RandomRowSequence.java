package uk.ac.starlink.table;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * An implementation of <tt>RowSequence</tt> which obtains its data
 * from a random-access <tt>StarTable</tt> object.
 */
public class RandomRowSequence implements RowSequence {

    private StarTable startab;
    private long irow = -1;

    /**
     * Constructs a RowSequence from a StarTable which must offer random
     * access and know its number of rows.
     *
     * @param  startab  the StarTable object providing the data
     * @throws IllegalArgumentException if <tt>startab.isRandom()==false</tt>
     *                                  or <tt>startab.getRowCount()&lt;0</tt>
     */
    public RandomRowSequence( StarTable startab ) {
        this.startab = startab;
        if ( ! startab.isRandom() ) {
            throw new IllegalArgumentException( 
                "Table " + startab + " is not random access" );
        }
        long nrow = startab.getRowCount();
        if ( nrow < 0L ) {
            throw new IllegalArgumentException( 
                "Table " + startab + " row count unknown " +
                "(getRowCount()=" + nrow + ")" );
        }
    }

    public boolean next() {
        if ( irow < getRowCount() - 1 ) {
            irow++;
            return true;
        }
        else {
            return false;
        }
    }

    public Object[] getRow() throws IOException {
        if ( irow >= 0 ) {
            return startab.getRow( irow );
        }
        else {
            throw new IllegalStateException( "No current row" );
        }
    }

    public Object getCell( int icol ) throws IOException {
        if ( irow >= 0 ) {
            return startab.getCell( irow, icol );
        }
        else {
            throw new IllegalStateException( "No current row" );
        }
    }

    private long getRowCount() {
        long nrow = startab.getRowCount();
        if ( nrow < 0 ) {
            throw new IllegalStateException( "Table " + startab + 
                                             " row count < 0" );
        }
        return nrow;
    }

    public void close() {
    }
}
