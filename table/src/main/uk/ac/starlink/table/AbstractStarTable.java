package uk.ac.starlink.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Abstract base class providing an implementation of the generic and
 * straightforward parts of the <tt>StarTable</tt> interface.
 * Various abstract subclasses of this are provided, and it is these 
 * which are in general designed for subclassing by concrete StarTable
 * implementations.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class AbstractStarTable implements StarTable {

    private ColumnHeader[] headers = new ColumnHeader[ 0 ];
    private Map metadata = new HashMap();

    /**
     * Returns a Map which is initially empty.
     *
     * @return  a map
     */
    public Map getMetadata() {
        return metadata;
    }

    public void setMetadata( Map metadata ) {
        this.metadata = metadata;
    }

    /**
     * Goes through the column headers and picks out all the metadata keys
     * that exist, returning a union of them all in alphabetical order.
     * Subclasses should override this if they can do better, for instance
     * providing an order for the keys.
     *
     * @return  an alphabetically ordered set of all the metadata keys 
     *          which in fact crop up in column metadata
     */
    public List getColumnMetadataKeys() {
        Set keys = new TreeSet();  // order alphabetically
        for ( int i = 0; i < getColumnCount(); i++ ) {
           keys.addAll( getHeader( i ).getMetadata().keySet() );
        }
        return Collections.unmodifiableList( new ArrayList( keys ) );
    }

    abstract public ColumnHeader getHeader( int icol );
    abstract public int getColumnCount();
    abstract public long getRowCount();
    abstract public boolean isRandom();
    abstract public boolean hasNext();
    abstract public void setCurrent( long irow ) throws IOException;
    abstract public long getCurrent();
    abstract public void next() throws IOException;
    abstract public void advanceCurrent( long offset ) throws IOException;
    abstract public Object getCell( int icol ) throws IOException;
    abstract public Object getCell( long irow, int icol ) throws IOException;
    abstract public Object[] getRow() throws IOException;
    abstract public Object[] getRow( long irow ) throws IOException;

}
