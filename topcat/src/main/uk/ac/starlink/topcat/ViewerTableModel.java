package uk.ac.starlink.topcat;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.swing.table.AbstractTableModel;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.EditableColumn;
import uk.ac.starlink.table.RowPermutedStarTable;
import uk.ac.starlink.table.StarTable;

/**
 * A <code>TableModel</code> which is based on a <code>StarTable</code> but 
 * also provides methods for ordering rows and selecting which rows will
 * be visible.  This class is used as the <code>TableModel</code> for the
 * <code>TableViewer</code> widget.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ViewerTableModel extends AbstractTableModel {

    private PlasticStarTable startable_;
    private RowSubset rset_ = RowSubset.ALL;
    private int[] order_;
    private int[] rowMap_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );
    private static boolean columnBugWarned_;

    /**
     * Constructs a <code>ViewerTableModel</code> from a <code>StarTable</code>.
     * The supplied <code>StarTable</code> must provide random access.
     *
     * @param   startable  the <code>StarTable</code> object
     * @throws  IllegalArgumentException  if <code>startable.isRandom</code>
     *          returns <code>false</code>
     */
    public ViewerTableModel( PlasticStarTable startable ) {
        startable_ = startable;

        /* Ensure that we have a random access table to use, and that it
         * is not unfeasibly large. */
        if ( ! startable.isRandom() ) {
            throw new IllegalArgumentException(
                "Table " + startable + " does not have random access" );
        }
        if ( startable.getRowCount() > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException(
                "Table has too many rows (" + startable.getRowCount() +
                " > Integer.MAX_VALUE)" );
        }
    }

    /**
     * Configures this view to view the rows of the base model in a
     * given order.  The supplied order array should be a 1:1 mapping
     * of rows in the base table to the order in which they will be viewed.
     * This method triggers a suitable <code>TableModelEvent</code>
     * to listeners.
     *
     * @param  order  mapping of rows in the table view, or <code>null</code>
     *         to indicate natural ordering
     */
    public void setOrder( int[] order ) {
        if ( order != null && order.length != startable_.getRowCount() ) {
            throw new IllegalArgumentException( "Wrong number of rows!"
                + " (" + order.length + " != " + startable_.getRowCount() );
        }
        order_ = order;
        setRowMap( getRowMap( order, rset_, getTableRowCount() ) );
        fireTableDataChanged();
    }

    /**
     * Configures this view to view only a subset of the rows of the base
     * model.
     * This method triggers a suitable <code>TableModelEvent</code>
     * to listeners.
     *
     * @param   rset  RowSubset object indicating inclusion in subset of
     *          rows to be viewed
     */
    public void setSubset( RowSubset rset ) {
        rowMap_ = getRowMap( order_, rset, getTableRowCount() );
        rset_ = rset;
        fireTableDataChanged();
    }

    /**
     * Returns the RowSubset currently used by the viewer model.
     *
     * @return the row subset
     */
    public RowSubset getSubset() {
        return rset_;
    }

    /**
     * Returns the mapping from row index visible in this model to 
     * row index in the base table.
     *
     * @return  row mapping; may be <code>null</code> to indicate a unit map
     */
    public int[] getRowMap() {
        return rowMap_;
    }

    /**
     * Returns an iterator over the base table row indices represented
     * by this view.  This is an iteration over the values in the
     * {@link #getRowMap rowMap} if it is non-null, or an iterator
     * over all the rows if it is null.
     * The number of elements iterated over is given by
     * {@link #getRowCount}.
     *
     * @return  row index iterator
     */
    public Iterator<Long> getRowIndexIterator() {
        final int[] rowMap = rowMap_;
        if ( rowMap == null ) {
            final long n = startable_.getRowCount();
            return new Iterator<Long>() {
                private long lx_ = -1;
                public boolean hasNext() {
                    return lx_ < n - 1;
                }
                public Long next() {
                    return new Long( ++lx_ );
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
        else {
            final long n = rowMap.length;
            return new Iterator<Long>() {
                private int kx_ = -1;
                public boolean hasNext() {
                    return kx_ < n - 1;
                }
                public Long next() {
                    return new Long( rowMap[ ++kx_ ] );
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    /**
     * Sets the mapping from row index visible in this model to 
     * row index in the base table.
     *
     * @param  rowMap  row mapping;
     *                 may be <code>null</code> to indicate a unit map
     */
    public void setRowMap( int[] rowMap ) {
        rowMap_ = rowMap;
        fireTableDataChanged();
    }

    /**
     * Returns the row map formed by combining an ordering map and 
     * a subset.
     *
     * @param   order  ordering map (may be null to indicate natural order)
     * @param   rset   row subset
     * @param   nrow   the number of rows in the base table
     * @return  map from rows in this model to rows int the base table
     *          (may be null to indicate a unit mapping)
     */
    private static int[] getRowMap( int[] order, RowSubset rset, int nrow ) {

        /* In the case of a trivial subset (all rows included) the row map
         * is just the same as the sort order (possibly null). */
        if ( rset == RowSubset.ALL ) {
            return order;
        }

        /* In the case of a non-trivial subset we need to assemble a list
         * of all the row indices which are actually used, possibly modulated
         * by a sort order. */
        else {
            int[] rmap = new int[ nrow ];
            int j = 0;
            if ( order != null ) {
                for ( int i = 0; i < nrow; i++ ) {
                    int k = order[ i ];
                    if ( rset.isIncluded( k ) ) {
                        rmap[ j++ ] = k;
                    }
                }
            }
            else {
                for ( int i = 0; i < nrow; i++ ) {
                    if ( rset.isIncluded( i ) ) {
                        rmap[ j++ ] = i;
                    }
                }
            }
            int[] rmap2 = new int[ j ];
            System.arraycopy( rmap, 0, rmap2, 0, j );
            return rmap2;
        }
    }

    public int getColumnCount() {
        return startable_.getColumnCount();
    }

    public int getRowCount() {
        return rowMap_ == null ? getTableRowCount()
                              : rowMap_.length;
    }

    /**
     * Returns the index of the data model row corresponding 
     * to a given row in this view model.
     *
     * @param   irow  index of the row in this view model
     * @return  index of the row in the base table
     */
    public long getBaseRow( int irow ) {
        return ( rowMap_ == null ) ? (long) irow
                                   : (long) rowMap_[ irow ];
    }

    /**
     * Returns the index at which a given table row appears in this view model.
     * If the given table row doesn't appear in the view model 
     * (it's not included in the current subset) then -1 is returned.
     *
     * @param  lrow  index of the row in the base table
     * @return  index of the row in this view model, or -1
     */
    public int getViewRow( long lrow ) {
        if ( rowMap_ == null ) {
            return (int) lrow;
        }
        else {
            int nr = rowMap_.length;
            int irow = AbstractStarTable.checkedLongToInt( lrow );
            for ( int i = 0; i < nr; i++ ) {
                if ( rowMap_[ i ] == irow ) {
                    return i;
                }
            }
            return -1;
        }
    }

    public Object getValueAt( int irow, int icol ) {
        if ( icol >= 0 ) {
            try {
                return startable_.getCell( getBaseRow( irow ), icol );
            }
            catch ( IOException e ) {
                e.printStackTrace();
                return null;
            }
        }

        /* This shouldn't happen - calling this method with a negative
         * column index is an error.  However, a workaround for a Mac OSX
         * JTable bug in TopcatModel results in such calls.  The first 
         * time it happens log a warning; otherwise, just make sure that
         * we avoid throwing an exception. */
        else {
            if ( ! columnBugWarned_ ) {
                columnBugWarned_ = true;
                logger_.info( "Bad column " + icol 
                            + " - Mac OS X JTable bug?" );
            }
            return null;
        }
    }

    public boolean isCellEditable( int irow, int icol ) {
        // return startable_.getColumnData( icol ).isWritable();
        return true;
    }

    public void setValueAt( Object val, int irow, int icol ) {

        /* Check if this column is writable or not.  If it is not, then
         * we will have to replace it with a column which is writable,
         * ensure that it contains the same data as the original, and
         * slot it into the same place as the original column. */
        ColumnData coldat = startable_.getColumnData( icol );
        if ( ! startable_.getColumnData( icol ).isWritable() ) {
            ColumnData oldcol = startable_.getColumnData( icol );
            ColumnData newcol = new EditableColumn( oldcol );
            ColumnInfo info = newcol.getColumnInfo();
            info.setNullable( true );
            info.setElementSize( -1 );
            startable_.setColumn( icol, newcol );
        }

        /* We have a writable column.  Write the value to the appropriate
         * cell. */
        assert startable_.getColumnData( icol ).isWritable();
        try {
            startable_.getColumnData( icol )
                     .storeValue( getBaseRow( irow ), val );
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }

        /* Since there may be synthetic columns, message the table view
         * that any cell in the current row may have changed. */
        fireTableRowsUpdated( irow, irow );
    }

    /**
     * Returns the number of rows in the underlying table
     * as an <code>int</code>.
     */
    private int getTableRowCount() {
        return AbstractStarTable.checkedLongToInt( startable_.getRowCount() );
    }

    /**
     * Returns a new StarTable whose data corresponds to the current state of 
     * this ViewerTableModel.  It has the same row ordering and subset,
     * but subsequent changes to this model will not affect the data
     * viewed from the resulting object.
     *
     * @return   StarTable view of a snapshot of the data available from this 
     *           model
     */
    public StarTable getSnapshot() {
        return getRowPermutedView( startable_ );
    }

    /**
     * Returns a view of a given table whose rows are permuted in the same way
     * as the current view is permuted with respect to the basic table.
     * The submitted table may be returned if no permutation is in force.
     *
     * @param  table  table to be permuted
     * @return   a table which is a possibly permuted view of <code>table</code>
     */
    public StarTable getRowPermutedView( StarTable table ) {
        if ( rowMap_ == null ) {
            return table;
        }
        else {
            int nrow = rowMap_.length;
            long[] rowMapCopy = new long[ nrow ];
            for ( int i = 0; i < nrow; i++ ) {
                rowMapCopy[ i ] = (long) rowMap_[ i ];
            }
            return new RowPermutedStarTable( table, rowMapCopy );
        }
    }
}
