package uk.ac.starlink.table.view;

import java.io.IOException;
import javax.swing.table.AbstractTableModel;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.StarTable;

/**
 * A <tt>TableModel</tt> which is based on a <tt>StarTable</tt> but 
 * also provides methods for ordering rows and selecting which rows will
 * be visible.  This class is used as the <tt>TableModel</tt> for the
 * <tt>TableViewer</tt> widget.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ViewerTableModel extends AbstractTableModel {

    private StarTable startable;
    private RowSubset rset;
    private int[] order;
    private int[] rowMap;

    /**
     * Constructs a <tt>ViewerTableModel</tt> from a <tt>StarTable</tt>.
     * The supplied <tt>StarTable</tt> must provide random access.
     *
     * @param   startable  the <tt>StarTable</tt> object
     * @throws  IllegalArgumentException  if <tt>startable.isRandom</tt>
     *          returns <tt>false</tt>
     * @see     uk.ac.starlink.table.Tables#randomTable
     */
    public ViewerTableModel( StarTable startable ) {
        this.startable = startable;

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
     *
     * @param  order  mapping of rows in the table view, or <tt>null</tt>
     *         to indicate natural ordering
     */
    public void setOrder( int[] order ) {
        if ( order != null && order.length != startable.getRowCount() ) {
            throw new IllegalArgumentException( "Wrong number of rows!" );
        }
        this.rowMap = getRowMap( order, rset, getTableRowCount() );
        this.order = order;
    }

    /**
     * Configures this view to view only a subset of the rows of the base
     * model.
     *
     * @param   rset  RowSubset object indicating inclusion in subset of
     *          rows to be viewed
     */
    public void setSubset( RowSubset rset ) {
        this.rowMap = getRowMap( order, rset, getTableRowCount() );
        this.rset = rset;
    }

    /**
     * Returns the mapping from base table rows to the columns visible
     * in this model.
     *
     * @return  row mapping; may be <tt>null</tt> to indicate a unit map
     */
    public int[] getRowMap() {
        return rowMap;
    }

    /**
     * Returns the row map formed by combining an ordering map and 
     * a subset.
     *
     * @param   order  ordering map (may be null to indicate natural order)
     * @param   rset   row subset (may be null to indicate full inclusion)
     * @param   nrow   the number of rows in the base table
     * @return  map from rows in this model to rows int the base table
     *          (may be null to indicate a unit mapping)
     */
    private static int[] getRowMap( int[] order, RowSubset rset, int nrow ) {

        /* In the case of a non-trivial subset we need to assemble a list
         * of all the row indices which are actually used, possibly modulated
         * by a sort order. */
        if ( rset != null && rset != RowSubset.ALL ) {
            int[] rmap = new int[ nrow ];
            int j = 0;
            if ( order != null ) {
                for ( int i = 0; i < nrow; i++ ) {
                    if ( rset.isIncluded( i ) ) {
                        rmap[ j++ ] = order[ i ];
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

        /* In the case of a trivial subset (all rows included) the row map
         * is just the same as the sort order (possibly null). */
        else {
            return order;
        }
    }

    public int getColumnCount() {
        return startable.getColumnCount();
    }

    public int getRowCount() {
        return rowMap == null ? getTableRowCount()
                              : rowMap.length;
    }

    public Object getValueAt( int irow, int icol ) {
        long lrow = ( rowMap == null ) ? (long) irow
                                       : (long) rowMap[ irow ];
        try {
            return startable.getCell( lrow, icol );
        }
        catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the number of rows in the underlying table as an <tt>int</tt>.
     */
    private int getTableRowCount() {
        return AbstractStarTable.checkedLongToInt( startable.getRowCount() );
    }
    
}
