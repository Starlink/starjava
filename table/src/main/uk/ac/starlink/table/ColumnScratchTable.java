package uk.ac.starlink.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a <tt>StarTable</tt> by storing the data in a set of 
 * columns.  This will be more efficient than storing it in rows,
 * especially if some of the columns are of a primitive type.
 */
public class ColumnScratchTable extends RandomStarTable {

    private final int nrow;
    private ArrayColumn[] columns;
    private List auxDataInfos;

    /**
     * Constructs a scratch <tt>StarTable</tt> containing data copied from
     * a base table.
     *
     * @param  basetab  the base table from which to copy data and metadata
     *         for the new one
     */
    public ColumnScratchTable( StarTable basetab ) throws IOException {

        /* Validate and set the number of rows. */
        long rowCount = basetab.getRowCount();
        if ( rowCount < 0 ) {
            throw new IllegalArgumentException( "Illegal row count < 0" );
        }
        if ( rowCount > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException( 
                "Cannot create scratch array with more than 2^31 rows" );
        }
        nrow = (int) rowCount;
        assert (long) nrow == rowCount;

        /* Set per-table info from the base table. */
        auxDataInfos = new ArrayList( basetab.getColumnAuxDataInfos() );
        setParameters( new ArrayList( basetab.getParameters() ) );

        /* Set up columns for this table based on those from the base table.  
         * These columns will handle both header information and storage
         * of the table data. */
        int ncol = basetab.getColumnCount();
        columns = new ArrayColumn[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            ColumnInfo baseinfo = basetab.getColumnInfo( i );
            columns[ i ] = ArrayColumn.makeColumn( baseinfo, nrow );
        }

        /* Populate the columns with data from the base table. */
        for ( int irow = 0; basetab.hasNext(); basetab.next(), irow++ ) {
            long lrow = (long) irow;
            Object[] row = basetab.getRow();
            for ( int icol = 0; icol < ncol; icol++ ) {
                columns[ icol ].storeValue( lrow, row[ icol ] );
            }
        }
    }


    public long getRowCount() {
        return (long) nrow;
    }

    public int getColumnCount() {
        return columns.length;
    }

    public List getColumnAuxDataInfos() {
        return auxDataInfos;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return columns[ icol ];
    }

    protected Object[] doGetRow( long lrow ) {
        int irow = (int) lrow;
        int ncol = columns.length;
        Object[] row = new Object[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            row[ i ] = columns[ i ].readValue( lrow );
        }
        return row;
    }

    public Object getCell( long lrow, int icol ) {
        return columns[ icol ].readValue( lrow );
    }

    public Object getCell( int icol ) {
        return getCell( getCurrent(), icol );
    }

}
