package uk.ac.starlink.table;

import java.io.IOException;

/**
 * ColumnData which wraps another ColumnData.  The behaviour of this 
 * ColumnData is identical to that of the base one.  It is intended for
 * subclasses which may modify the behaviour in some way.
 * 
 * @author   Mark Taylor (Starlink)
 */
public class WrapperColumn extends ColumnData {

    private final ColumnData base;

    /**
     * Initialises a new WrapperColumn based on a base column.
     *
     * @param  base  the base column
     */
    public WrapperColumn( ColumnData base ) {
        super( base.getColumnInfo() );
        this.base = base;
    }

    public Object readValue( long irow ) throws IOException {
        return base.readValue( irow );
    }

    public void storeValue( long irow, Object val ) throws IOException {
        base.storeValue( irow, val );
    }

    public boolean isWritable() {
        return base.isWritable();
    }

    /**
     * Returns the column on which this one is based.
     *
     * @return  wrapped column
     */
    public ColumnData getBaseColumn() {
        return base;
    }
}
