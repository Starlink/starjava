package uk.ac.starlink.ttools;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;

/**
 * Can identify columns of a table using string identifiers.
 * Permitted identifiers are (currently) column name (if in JEL-friendly
 * format), column $ID (ditto) or column index (first column is "1").
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Mar 2005
 */
public class ColumnIdentifier {

    private final JELRowReader jelly_;
    private final StarTable table_;

    /**
     * Constructor.
     *
     * @param  table  table whose columns this identifier can identify
     */
    public ColumnIdentifier( StarTable table ) {
        table_ = table;
        jelly_ = new JELRowReader( table ) {
            protected Object getCell( int icol ) {
                throw new UnsupportedOperationException();
            }
            protected Object[] getRow() {
                throw new UnsupportedOperationException();
            }
            public long getCurrentRow() { 
                return -1L;
            }
        };
    }

    /**
     * Returns the index of a column given an identifying string.
     * If the string can't be identified as a column of this object's
     * table, an <tt>IOException</tt> is thrown.
     *
     * @param   colid   identifying string
     * @return  column index
     * @throws  IOException  if <tt>colid</tt> does not name a column
     */
    public int getColumnIndex( String colid ) throws IOException {
        int ix = jelly_.getColumnIndex( colid );
        if ( ix < 0 ) {
            try {
                int ix1 = Integer.parseInt( colid );
                ix = ix1 - 1;
                if ( ix < 0 || ix >= table_.getColumnCount() ) {
                    throw new IOException( "Column index out of range: "
                                         + colid );
                }
            }
            catch ( NumberFormatException e ) {
                throw new IOException( "No such column " + colid );
            }
        }
        assert ix >= 0 && ix <= table_.getColumnCount();
        return ix;
    }
}
