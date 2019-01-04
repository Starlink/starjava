package uk.ac.starlink.util.gui;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

/**
 * Table column model listener which delegates to another one as long as it is
 * reachable, but only retains a weak reference to it.
 * Adding a listener to an object in this way will not prevent the listener
 * (and any of its references) from being garbage collected.
 *
 * @author   Mark Taylor
 * @since    20 Jan 2005
 */
public class WeakTableColumnModelListener implements TableColumnModelListener {

    private final Reference<TableColumnModelListener> baseRef_;

    /**
     * Constructs a new listener based on an existing one.
     *
     * @param base listener
     */
    public WeakTableColumnModelListener( TableColumnModelListener base ) {
        baseRef_ = new WeakReference<TableColumnModelListener>( base );
    }

    public void columnAdded( TableColumnModelEvent evt ) {
        TableColumnModelListener base = baseRef_.get();
        if ( base != null ) {
            base.columnAdded( evt );
        }
    }

    public void columnRemoved( TableColumnModelEvent evt ) {
        TableColumnModelListener base = baseRef_.get();
        if ( base != null ) {
            base.columnRemoved( evt );
        }
    }

    public void columnMoved( TableColumnModelEvent evt ) {
        TableColumnModelListener base = baseRef_.get();
        if ( base != null ) {
            base.columnMoved( evt );
        }
    }

    public void columnMarginChanged( ChangeEvent evt ) {
        TableColumnModelListener base = baseRef_.get();
        if ( base != null ) {
            base.columnMarginChanged( evt );
        }
    }

    public void columnSelectionChanged( ListSelectionEvent evt ) {
        TableColumnModelListener base = baseRef_.get();
        if ( base != null ) {
            base.columnSelectionChanged( evt );
        }
    }
}
