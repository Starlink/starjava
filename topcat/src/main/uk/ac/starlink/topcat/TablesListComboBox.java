package uk.ac.starlink.topcat;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import uk.ac.starlink.util.gui.WeakListDataListener;

/**
 * ComboBox which tracks the current contents of the ControlWindow's
 * list of tables.
 *
 * @author   Mark Taylor
 * @since    31 Aug 2010
 */
public class TablesListComboBox extends JComboBox {

    private final ListModel tablesList_;
    private final BasicComboBoxModel comboBoxModel_;

    /**
     * Constructs a combo box based on the ControlWindow's list of tables.
     */
    public TablesListComboBox() {
        this( ControlWindow.getInstance().getTablesListModel() );
    }

    /**
     * Constructs a combo box based on a given ListModel containing tables.
     *
     * @param  tablesList  list of TopcatModels
     */
    public TablesListComboBox( ListModel tablesList ) {
        tablesList_ = tablesList;
        comboBoxModel_ = new BasicComboBoxModel( tablesList );
        final Object src = TablesListComboBox.this;
        tablesList_.addListDataListener( new ListDataListener() {
            public void contentsChanged( ListDataEvent evt ) {
                checkConsistent();
                comboBoxModel_.fireContentsChanged( src, evt.getIndex0(),
                                                         evt.getIndex1() );
            }
            public void intervalRemoved( ListDataEvent evt ) {
                checkConsistent();
                comboBoxModel_.fireIntervalRemoved( src, evt.getIndex0(),
                                                         evt.getIndex1() );
            }
            public void intervalAdded( ListDataEvent evt ) {
                checkConsistent();
                comboBoxModel_.fireIntervalAdded( src, evt.getIndex0(),
                                                       evt.getIndex1() );
            }
        } );
        setModel( comboBoxModel_ );
    }

    /**
     * If there is only one entry in the tables list, select that.
     * Otherwise no action.
     */
    public void selectIfUnique() {
        if ( tablesList_.getSize() == 1 ) {
            comboBoxModel_.setSelectedItem( tablesList_.getElementAt( 0 ) );
        }
    }

    /**
     * Ensure that invariants hold following a change to the tables list.
     * Specifically, if the currently selected table is no longer in the
     * tables list, remove it from the combo box.
     */
    private void checkConsistent() {
        Object selected = comboBoxModel_.getSelectedItem();
        if ( selected == null ) {
            return;
        }
        for ( int i = 0; i < tablesList_.getSize(); i++ ) {
            if ( selected.equals( tablesList_.getElementAt( i ) ) ) {
                return;
            }
        }

        /* Selected item is no longer in the table list (it has been removed);
         * clear the selection. */
        setSelectedItem( null );
    }

    /**
     * ComboBoxModel implementation based on a given ListModel.
     */
    private static class BasicComboBoxModel extends AbstractListModel
                                            implements ComboBoxModel {
        private final ListModel listModel_;
        private Object selected_;

        /**
         * Constructor.
         *
         * @param   listModel   list containing combo box contents
         */
        BasicComboBoxModel( ListModel listModel ) {
            listModel_ = listModel;
        }

        public Object getSelectedItem() {
            return selected_;
        }
        public void setSelectedItem( Object item ) {
            selected_ = item;
        }
        public int getSize() {
            return listModel_.getSize();
        }
        public Object getElementAt( int ix ) {
            return listModel_.getElementAt( ix );
        }
        public void fireContentsChanged( Object src, int index0, int index1 ) {
            super.fireContentsChanged( src, index0, index1 );
        }
        public void fireIntervalAdded( Object src, int index0, int index1 ) {
            super.fireIntervalAdded( src, index0, index1 );
        }
        public void fireIntervalRemoved( Object src, int index0, int index1 ) {
            super.fireIntervalRemoved( src, index0, index1 );
        }
    }
}
