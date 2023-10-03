package uk.ac.starlink.topcat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * Basic implementation of CheckBoxList.
 * This provides a simple internal implementation of the checkbox model
 * and uses a DefaultListModel for the data.
 * Various parts can be overridden as required.
 *
 * @author   Mark Taylor
 * @since    21 Dec 2017
 */
public class BasicCheckBoxList<T> extends CheckBoxList<T> {

    private final Set<T> activeSet_;

    /**
     * Constructs a list with default rendering.
     *
     * @param   canSelect   true if list item selection is permitted
     */
    public BasicCheckBoxList( boolean canSelect ) {
        this( canSelect,
              createLabelRendering( s -> s == null ? null : s.toString() ) );
    }

    /**
     * Constructs a list with custom rendering.
     *
     * @param   canSelect   true if list item selection is permitted
     * @param   rendering   how to render list entries
     */
    public BasicCheckBoxList( boolean canSelect, Rendering<T,?> rendering ) {
        super( new DefaultListModel<T>(), canSelect, rendering );
        activeSet_ = new HashSet<T>();
    }

    @Override
    public DefaultListModel<T> getModel() {
        @SuppressWarnings("unchecked")
        DefaultListModel<T> model = (DefaultListModel<T>) super.getModel();
        return model;
    }

    @Override
    public void setModel( ListModel<T> model ) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a list of all the items currently in this list.
     *
     * @return  list of all items
     */
    public List<T> getItems() {
        List<T> list = new ArrayList<T>();
        ListModel<T> listModel = getModel();
        for ( int i = 0; i < listModel.getSize(); i++ ) {
            list.add( listModel.getElementAt( i ) );
        }
        return list;
    }

    /**
     * Returns a list of the items currently in this list whose
     * check box is selected.
     *
     * @return   list of active items
     */
    public List<T> getCheckedItems() {
        List<T> list = new ArrayList<T>();
        for ( T item : getItems() ) {
            if ( isChecked( item ) ) {
                list.add( item );
            }
        }
        return list;
    }

    public boolean isChecked( T item ) {
        return activeSet_.contains( item );
    }

    public void setChecked( T item, boolean isChecked ) {
        if ( isChecked ) {
            activeSet_.add( item );
        }
        else {
            activeSet_.remove( item );
        }
        int index = getIndex( item );
        if ( index >= 0 ) {
            ListDataEvent evt =
                new ListDataEvent( this, ListDataEvent.CONTENTS_CHANGED,
                                   index, index );
            for ( ListDataListener l : getModel().getListDataListeners() ) {
                l.contentsChanged( evt );
            }
        }
    }

    public void moveItem( int ifrom, int ito ) {
        DefaultListModel<T> listModel = getModel();
        listModel.add( ito, listModel.remove( ifrom ) );
    }

    /**
     * Returns the index of a given item in this list.
     *
     * @param  item  item to locate
     * @return  index in list, or -1 if not found
     */
    private int getIndex( T item ) {
        ListModel<T> model = getModel();
        int n = model.getSize();
        for ( int i = 0; i < n; i++ ) {
            if ( Objects.equals( model.getElementAt( i ), item ) ) {
                return i;
            }
        }
        return -1;
    }
}
