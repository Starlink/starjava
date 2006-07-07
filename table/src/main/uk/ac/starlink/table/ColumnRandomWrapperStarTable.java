package uk.ac.starlink.table;

import java.io.IOException;

/**
 * RandomWrapperStarTable which works by storing the data in a set of
 * fixed length arrays.  This will be more efficient on memory used
 * than storing it in rows if some of the columns have a primitive type,
 * but it may make reading slower.
 *
 * @author   Mark Taylor (Starlink)
 * @see  RowRandomWrapperStarTable
 */
public class ColumnRandomWrapperStarTable extends RandomWrapperStarTable {

    private int ncol;
    private int nrow;
    private ArrayColumn[] columns;
    private long currentRow = 0L;

    /**
     * Constructs a new table to store a given number of rows.
     * The <tt>nrow</tt> argument supplied is the number that this table
     * will have, which may or may not match that of the base table.
     *
     * @param  baseTable  base table
     * @param  nrow  number of rows in this table
     */
    public ColumnRandomWrapperStarTable( StarTable baseTable, int nrow )
            throws IOException {
        super( baseTable );
        this.nrow = nrow;

        /* Set up columns for this table based on those from the base table. */
        ncol = getColumnCount();
        columns = new ArrayColumn[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo colinfo = getColumnInfo( icol );
            columns[ icol ] = ArrayColumn.makeColumn( colinfo, nrow );
        }
    }

    public long getRowCount() {
        return (long) nrow;
    }

    protected synchronized void storeNextRow( Object[] row ) {
        if ( currentRow < nrow ) {
            for ( int icol = 0; icol < ncol; icol++ ) {
                columns[ icol ].storeValue( currentRow, row[ icol ] );
            }
            currentRow++;
        }
    }

    protected Object[] retrieveStoredRow( long lrow ) {
        Object[] row = new Object[ ncol ];
        if ( lrow < nrow ) {
            for ( int icol = 0; icol < ncol; icol++ ) {
                row[ icol ] = columns[ icol ].readValue( lrow );
            }
        }
        return row;
    }
}
