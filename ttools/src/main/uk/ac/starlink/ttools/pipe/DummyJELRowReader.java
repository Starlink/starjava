package uk.ac.starlink.ttools.pipe;

import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.JELRowReader;

/**
 * JELRowReader which can't actually read any rows.  It's only good for
 * identifying columns, checking JEL expressions, etc.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Mar 2005
 */
public class DummyJELRowReader extends JELRowReader {

    /**
     * Constructor.
     *
     * @param  baseTable  table whose columns this reader will be based on
     */
    DummyJELRowReader( StarTable baseTable ) {
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
