package uk.ac.starlink.topcat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * List reflecting the {@link javax.swing.table.TableColumn} objects 
 * in a {@link javax.swing.table.TableColumnModel}.  
 * Unlike a <code>TableColumnModel</code>, this never throws away 
 * any columns, it just maintains an array of flags to indicate which
 * columns are currently in the model and which are not.
 * On construction it registers itself as a listener on its ColumnModel
 * so that it automatically keeps up to date with its state.
 *
 * @author   Mark Taylor (Starlink)
 * @since    27 Feb 2004
 */
public class ColumnList implements TableColumnModelListener {

    private final TableColumnModel columnModel;
    private final List<TableColumn> columnList;
    private final List<Boolean> active;

    /**
     * Constructs a new ColumnList, which will track a given column model.
     *
     * @param  columnModel  table column model to track
     */
    public ColumnList( TableColumnModel columnModel ) {
        this.columnModel = columnModel;
        this.columnList = Collections.list( columnModel.getColumns() );
        this.active =
            new ArrayList<Boolean>( Collections.nCopies( columnList.size(), 
                                                         Boolean.TRUE ) );
        columnModel.addColumnModelListener( this );
    }

    /**
     * Returns one of the columns in this list.
     * 
     * @param  jcol  list index of the column
     * @return  column at list index <code>jcol</code>
     */
    public TableColumn getColumn( int jcol ) {
        return columnList.get( jcol );
    }

    /**
     * Indicates whether the column at a given index is currently active
     * (present in the table column model).
     *
     * @param   jcol  list index of the column
     * @return  true  iff column at <code>jcol</code> is active
     */
    public boolean isActive( int jcol ) {
        return active.get( jcol ).booleanValue();
    }

    /**
     * Marks a given column as active.  This will remove or re-introduce 
     * it into the table column model if necessary.
     *
     * @param  jcol  list index of the column
     * @param  actv  whether the column at <code>jcol</code> should be present
     *         in the table column model
     */
    public void setActive( int jcol, boolean actv ) {

        /* Don't attempt any action if no change is required. */
        if ( actv != isActive( jcol ) ) {

            /* If a change is required, make changes to the column model 
             * which will cause changes to this column list to take place
             * (since it is listening on the column model). */
            TableColumn tcol = getColumn( jcol );

            /* (Re-)add the column to the column model. */
            if ( actv ) {
                assert ! isActive( jcol );
                int last1 = columnModel.getColumnCount();
                columnModel.addColumn( tcol );
                columnModel.moveColumn( last1, getModelIndex( jcol ) );
            }

            /* Remove the column from the column model. */
            else {
                assert isActive( jcol );
                columnModel.removeColumn( tcol );
            }
        }
    }

    /**
     * Returns the number of columns in this list.
     *
     * @return  number of columns
     */
    public int size() {
        return columnList.size();
    }

    /**
     * Returns the position in this list at which the given table column
     * can be found.
     *
     * @param  tcol  sought column
     * @return  index of <code>tcol</code> in this list, or -1 if it's not there
     */
    public int indexOf( TableColumn tcol ) {
        return columnList.indexOf( tcol );
    }

    /**
     * Returns the index in the table column model of a column at a given
     * index in this list.
     *
     * @param  jcol  list index
     * @return  index into the table column model of column at <code>jcol</code>
     *          int this list, or one bigger than the size of the column model
     *          if it's not in it
     */
    public int getModelIndex( int jcol ) {
        int i = 0;
        for ( int j = 0; j < jcol; j++ ) {
            if ( isActive( j ) ) {
                i++;
            }
        }
        return i;
    }

    /**
     * Returns true if everything is OK.  Intended for use in assert statements.
     *
     * @return  true if invariants hold
     */
    private boolean invariants() {
        int ncol = size();
        boolean ok = true;
        ok = ok && ncol == columnList.size()
                && ncol == active.size();
        int i = 0;
        for ( int j = 0; j < ncol; j++ ) {
            if ( isActive( j ) ) {
                ok = ok && columnModel.getColumn( i++ ) == columnList.get( j );
            }
        }
        return ok;
    }
  
    /**
     * Returns the index in this ColumnList corresponding to an index
     * in the column model for a column which already exists in this list, 
     * or -1 if it's off the end of the list.
     *
     * @param   i  TableColumnModel index
     * @return   index in this list corresponding to <code>i</code>
     */
    private int getExistingActiveIndex( int i ) {
        int nActive = 0;
        int ncol = size();
        for ( int j = 0; j < ncol; j++ ) {
            if ( isActive( j ) ) {
                if ( nActive++ == i ) {
                    return j;
                }
            }
        }
        return -1;
    }

    /**
     * Returns the index in this ColumnList corresponding to an index
     * in the column model which doesn't yet exist in this list.
     *
     * @param  i  TableColumnModel index
     * @return  new index in this list corresponding to <code>i</code>
     */
    private int getNewActiveIndex( int i ) {
        int nActive = 0;
        int ncol = size();
        for ( int j = 0; j < ncol; j++ ) {
            if ( nActive == i ) {
                return j;
            }
            if ( isActive( j ) ) {
                nActive++;
            }
        }
        return ncol;
    }

    /*
     * Methods implementing TableColumnModelListener.
     * These methods keep this list up to date with its base column model.
     */

    public void columnAdded( TableColumnModelEvent evt ) {
        int iTo = evt.getToIndex();
        TableColumn tcol = columnModel.getColumn( iTo );
        if ( ! columnList.contains( tcol ) ) {
            int jTo = getNewActiveIndex( iTo );
            columnList.add( jTo, tcol );
            active.add( jTo, Boolean.TRUE );
        }
        else {
            int jcol = columnList.indexOf( tcol );
            assert ! isActive( jcol );
            active.set( jcol, Boolean.TRUE );
        }
    }

    public void columnMoved( TableColumnModelEvent evt ) {
        int iFrom = evt.getFromIndex();
        int jFrom = getExistingActiveIndex( iFrom );
        int iTo = evt.getToIndex();
        int jTo = getExistingActiveIndex( iTo );
        TableColumn tcol = columnModel.getColumn( iTo );
        if ( tcol == columnList.get( jFrom ) && iFrom != iTo ) {
            assert isActive( jFrom );
            columnList.remove( jFrom );
            active.remove( jFrom );
            columnList.add( jTo, tcol );
            active.add( jTo, Boolean.TRUE );
        }
    }

    public void columnRemoved( TableColumnModelEvent evt ) {
        int iFrom = evt.getFromIndex();
        int jFrom = getExistingActiveIndex( iFrom );
        assert isActive( jFrom );
        active.set( jFrom, Boolean.FALSE );
        assert invariants();
    }

    public void columnMarginChanged( ChangeEvent evt ) {
    }
    public void columnSelectionChanged( ListSelectionEvent evt ) {
    }

}
