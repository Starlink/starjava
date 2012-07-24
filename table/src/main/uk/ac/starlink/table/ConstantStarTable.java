package uk.ac.starlink.table;

/**
 * Table implementation representing a table in which every row is
 * the same as every other.
 *
 * @author   Mark Taylor
 * @since    3 Jul 2006
 */
public class ConstantStarTable extends RandomStarTable {

    private final ColumnInfo[] infos_;
    private final Object[] row_;
    private final long nrow_;

    /**
     * Constructs a new constant star table.
     *
     * @param  infos  array of column metadata objects (one for each column)
     * @param  cells  row data - the same for every row
     * @param  nrow   number of rows in this table
     */
    public ConstantStarTable( ColumnInfo[] infos, Object[] cells, long nrow ) {
        if ( infos.length != cells.length ) {
            throw new IllegalArgumentException(
                 "Multiplicity of cells and infos do not match" );
        }
        infos_ = infos;
        row_ = cells;
        nrow_ = nrow;
    }

    public int getColumnCount() {
        return infos_.length;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return infos_[ icol ];
    }

    public long getRowCount() {
        return nrow_;
    }

    public Object getCell( long lrow, int icol ) {
        return row_[ icol ];
    }
}
