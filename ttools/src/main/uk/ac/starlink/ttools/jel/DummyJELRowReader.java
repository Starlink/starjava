package uk.ac.starlink.ttools.jel;

import uk.ac.starlink.table.StarTable;

/**
 * JELRowReader which can't actually read any rows.  It's only good for
 * identifying columns, checking JEL expressions, etc.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Mar 2005
 */
public class DummyJELRowReader extends StarTableJELRowReader {

    /**
     * Constructor.
     *
     * @param  baseTable  table whose columns this reader will be based on
     */
    public DummyJELRowReader( StarTable baseTable ) {
        super( baseTable );
    }

    /**
     * Throws UnsupportedOperationException.
     */
    protected Object getCell( int icol ) {
        throw new UnsupportedOperationException();
    }

    public long getCurrentRow() {
        return -1L;
    }
}
