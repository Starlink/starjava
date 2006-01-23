package uk.ac.starlink.topcat;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import uk.ac.starlink.util.gui.WeakListDataListener;

/**
 * ComboBoxModel which tracks the current contents of the ControlWindow's
 * list of tables.
 *
 * @author   Mark Taylor (Starlink)
 * @since    15 Mar 2004
 */
public class TablesListComboBoxModel extends AbstractListModel
                                     implements ComboBoxModel, 
                                                ListDataListener {
    private final ListModel tablesList;
    private Object selected;

    /**
     * Constructs a new model based on the global tables list.
     */
    public TablesListComboBoxModel() {
        tablesList = ControlWindow.getInstance().getTablesListModel();
        tablesList.addListDataListener( new WeakListDataListener( this ) );
    }

    /**
     * If there is only one entry in the tables list, select that.
     * Otherwise no action.
     */
    public void selectIfUnique() {
        if ( tablesList.getSize() == 1 ) {
            setSelectedItem( tablesList.getElementAt( 0 ) );
        }
    }

    public Object getSelectedItem() {
        return selected;
    }

    public void setSelectedItem( Object item ) {
        this.selected = item;
    }

    public int getSize() {
        return tablesList.getSize();
    }

    public Object getElementAt( int index ) {
        return tablesList.getElementAt( index );
    }

    public void contentsChanged( ListDataEvent evt ) {
        checkConsistent();
        fireContentsChanged( this, evt.getIndex0(), evt.getIndex1() );
    }

    public void intervalAdded( ListDataEvent evt ) {
        checkConsistent();
        fireIntervalAdded( this, evt.getIndex0(), evt.getIndex1() );
    }

    public void intervalRemoved( ListDataEvent evt ) {
        checkConsistent();
        fireIntervalRemoved( this, evt.getIndex0(), evt.getIndex1() );
    }

    /**
     * Ensure that invariants hold following a change to the tables list.
     */
    private void checkConsistent() {

        /* If the selected table is no longer part of the list, unset the
         * selection. */
        Object selected = getSelectedItem();
        if ( selected != null ) {
            int n = tablesList.getSize();
            boolean in = false;
            for ( int i = 0; i < n; i++ ) {
                in = in || selected.equals( tablesList.getElementAt( i ) );
            }
            if ( ! in ) {
                setSelectedItem( null );
            }
        }
    }
}
