package uk.ac.starlink.table;

import java.util.NoSuchElementException;

/**
 * Row sequence implementation which has no rows.
 * 
 * @since    28 Oct 2004
 * @author   Mark Taylor (Starlink)
 */
public class EmptyRowSequence implements RowSequence {

    public boolean next() {
        return false;
    }

    public Object getCell( int icol ) {
        throw new IllegalStateException();
    }

    public Object[] getRow() {
        throw new IllegalStateException();
    }

    public void close() {
    }
}
