package uk.ac.starlink.topcat;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

/**
 * Convenience adapter class which implements the 
 * {@link javax.swing.event.TableColumnModelListener} interface.
 * All the methods in this implementation do nothing.
 *
 * @author   Mark Taylor (Starlink)
 */
public class TableColumnModelAdapter implements TableColumnModelListener {
    public void columnAdded( TableColumnModelEvent evt ) {}
    public void columnMoved( TableColumnModelEvent evt ) {}
    public void columnRemoved( TableColumnModelEvent evt ) {}
    public void columnMarginChanged( ChangeEvent evt ) {}
    public void columnSelectionChanged( ListSelectionEvent evt ) {}
}
