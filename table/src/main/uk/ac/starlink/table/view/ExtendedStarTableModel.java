package uk.ac.starlink.table.view;

import java.awt.Toolkit;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RandomRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.table.gui.StarTableModel;

/**
 * A <tt>TableModel</tt> which can be modified in ways required by the
 * <tt>TableViewer</tt> widget.  The row order may be permuted and 
 * columns may be added.  The underlying <tt>StarTable</tt> itself is
 * not modified by any of these changes.
 * <p>
 * The column data is stored as a set of ColumnData objects, one for each
 * column managed by the table.  The column data/metadata may be 
 * retrieved directly using the {@link #getColumnData}/{@link getColumnInfo}
 * methods.
 * 
 * @author   Mark Taylor (Starlink)
 */
public class ExtendedStarTableModel extends StarTableModel {

    private List columnData = new ArrayList();
    private int[] rowMap;
 
    /**
     * Constructs a new table model from a <tt>StarTable</tt>.
     *
     * @param  startab  the star table
     */
    public ExtendedStarTableModel( final StarTable startab ) {
        super( startab );

        /* Set up ColumnData objects for each of the columns in the
         * given StarTable. */
        for ( int icol = 0; icol < startab.getColumnCount(); icol++ ) {
            ColumnInfo colinfo = startab.getColumnInfo( icol );
            final int ficol = icol;
            ColumnData coldat = new ColumnData( colinfo ) {
                public Object readValue( long lrow ) throws IOException {
                    return startab.getCell( lrow, ficol );
                }
            };
            columnData.add( coldat );
        }
    }

    /**
     * Permutes the row order relative to the basic order of the underlying
     * <tt>StarTable</tt>.  The supplied <tt>rowmap</tt> array defines
     * a mapping from the rows of this TableModel to the rows of the
     * StarTable.  Note that permutations are not cumulative; the effect of
     * calling this method is not affected by any previous calls to it.
     * A <tt>null</tt> value of <tt>rowmap</tt> may be used to indicate
     * the identity mapping.  <tt>rowmap</tt> does not have to represent
     * a 1:1 mapping.
     * <p>
     * Calling this method causes registered listeners to be notified that
     * changes have been made.
     * 
     * @param   rowmap  array giving the mapping of rows in this model to 
     *          rows in the underlying table; may be <tt>null</tt> to 
     *          indicate a unit mapping
     */
    public void permuteRows( int[] rowMap ) {
        this.rowMap = rowMap;
        fireTableDataChanged();
    }

    /**
     * Returns the permutation currently being used to map model rows to
     * view rows.
     *
     * @return  array giving the mapping of rows in this model to 
     *          rows in the underlying table; may be <tt>null</tt> to 
     *          indicate a unit map
     * @see  #permuteRows
     */
    public int[] getRowPermutation() {
        return rowMap;
    }

    /**
     * Adds a new column to the table.  A <tt>ColumnData</tt> object is
     * supplied which provides not only the column metadata but also
     * the column data itself, and perhaps facilities for writing to
     * the column.
     *
     * @param  newcol  the new column object
     */
    public void addColumn( ColumnData newcol ) {
        columnData.add( newcol );
    }

    /**
     * Returns a StarTable which represents the state of this TableModel.
     * This may differ from the base StarTable (as returned by 
     * {@link #getStarTable} in that it may have additional columns
     * and a permuted row order (and other changes?).  Subsequent changes
     * to the row order and column set of this model will not affect
     * the returned apparent StarTable, though changes to the actual
     * cell values in the model may do.
     *
     * @return  a StarTable object representing the current state of this model
     */
    public StarTable getApparentStarTable() {
        return new ApparentStarTable( this );
    }

    public int getColumnCount() {
        return columnData.size();
    }

    public int getRowCount() {
        return rowMap == null ? super.getRowCount() 
                              : rowMap.length;
    }

    public Object getValueAt( int irow, int icol ) {
        long lrow = ( rowMap == null ) ? (long) irow
                                       : (long) rowMap[ irow ];
        try {
            return getColumnData( icol ).readValue( lrow );
        }
        catch ( IOException e ) {
            e.printStackTrace();
            return e.toString();
        }
    }

    public String getColumnName( int icol ) {
        return getColumnData( icol ).getColumnInfo().getName();
    }

    public Class getColumnClass( int icol ) {
        return getColumnData( icol ).getColumnInfo().getContentClass();
    }

    public boolean isCellEditable( int irow, int icol ) {
        return getColumnData( icol ).isWritable();
    }

    public void setValueAt( Object value, int irow, int icol ) {
        if ( isCellEditable( irow, icol ) ) {
            try {
                long lrow = ( rowMap == null ) ? (long) irow
                                               : (long) rowMap[ irow ];
                getColumnData( icol ).storeValue( lrow, value );
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
        }
        else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /**
     * Returns the <tt>ColumnData</tt> object for a given column.
     *
     * @param  icol  the index of the column for which the result is required
     * @return the ColumnData for column <tt>icol</tt>
     */
    public ColumnData getColumnData( int icol ) {
        return (ColumnData) columnData.get( icol );
    }

    /**
     * Returns the <tt>ColumnInfo</tt> object for a given column.
     *
     * @param  icol  the index of the column for which the result is required
     * @return  the ColumnInfo for column <tt>icol</tt>
     */
    public ColumnInfo getColumnInfo( int icol ) {
        return getColumnData( icol ).getColumnInfo();
    }

    /**
     * Helper class for representing a snapshot of this table model
     * as a separate StarTable.
     */
    private static class ApparentStarTable extends WrapperStarTable {

        private final int[] rowMap;
        private final ExtendedStarTableModel stmodel;
        private final List columnData;

        /**
         * Constructs a new StarTable from a given ExtendedStarTableModel.
         * The row permutation and column list are taken as a snapshot
         * of the given model, so that subseqent changes in it will not
         * affect the new StarTable (though changes in the cell data may do).
         *
         * @param  stmodel  the model to construct the new table from
         */
        ApparentStarTable( ExtendedStarTableModel stmodel ) {

            /* Initialise from the base StarTable to copy parameter values
             * and so on without further effort. */
            super( stmodel.getStarTable() );

            /* If we've got here it must be random access. */
            assert stmodel.getStarTable().isRandom();

            /* Store the model that we're based on, and take snapshots of
             * the mutable parts of its configuration. */
            this.stmodel = stmodel;
            this.columnData = new ArrayList( stmodel.columnData );
            this.rowMap = ( stmodel.rowMap == null ) 
                        ? null
                        : (int[]) stmodel.rowMap.clone();
        }

        public int getColumnCount() {
            return columnData.size();
        }

        public long getRowCount() {
            return ( rowMap == null ) ? stmodel.getRowCount()
                                      : rowMap.length;
        }

        public ColumnInfo getColumnInfo( int icol ) {
            return getColumnData( icol ).getColumnInfo();
        }

        public Object getCell( long irow, int icol ) throws IOException {
            long lrow = ( rowMap == null ) 
                      ? irow
                      : (long) rowMap[ checkedLongToInt( irow ) ];
            return getColumnData( icol ).readValue( lrow );
        }

        public Object[] getRow( long irow ) throws IOException {
            long lrow = ( rowMap == null ) 
                      ? irow
                      : (long) rowMap[ checkedLongToInt( irow ) ];
            int ncol = getColumnCount();
            Object[] row = new Object[ ncol ];
            for ( int icol = 0; icol < ncol; icol++ ) {
                row[ icol ] = getColumnData( icol ).readValue( lrow );
            }
            return row;
        }

        public RowSequence getRowSequence() throws IOException {
            return new RandomRowSequence( this );
        }

        private ColumnData getColumnData( int icol ) {
            return (ColumnData) columnData.get( icol );
        }
    }


}
