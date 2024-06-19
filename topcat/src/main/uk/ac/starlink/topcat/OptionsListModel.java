package uk.ac.starlink.topcat;

import java.awt.event.ActionEvent;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.ComboBoxModel;
import javax.swing.ListModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import uk.ac.starlink.util.gui.CustomComboBoxRenderer;

/**
 * Provides storage for a list of options with listeners.
 * This class implements both {@link java.util.List} and  
 * {@link javax.swing.ListModel} (which <code>ListModel</code> really
 * ought to do itself), and also provides methods to create 
 * some useful models like <code>JComboBoxModel</code> based on the same data.
 *
 * @author   Mark Taylor (Starlink)
 */
public class OptionsListModel<T> extends AbstractList<T>
                                 implements ListModel<T> {

    private final List<Entry<T>> entryList_;
    private final BasicListModel bmodel_;
    private int nextId_;
    
    public OptionsListModel() {
        entryList_ = new ArrayList<Entry<T>>();
        bmodel_ = new BasicListModel();
    }

    public T get( int index ) {
        return index >= 0 && index < entryList_.size()
             ? entryList_.get( index ).obj_
             : null;
    }

    public T getElementAt( int index ) {
        return get( index );
    }

    public int size() {
        return entryList_.size();
    }

    public int getSize() {
        return size();
    }

    public boolean add( T obj ) {
        int index = entryList_.size();
        boolean ret = entryList_.add( new Entry<T>( nextId_++, obj ) );
        fireIntervalAdded( index, index );
        return ret;
    }

    public T set( int irow, T obj ) {
        Entry<T> entry = entryList_.get( irow );
        T oldval = entry.obj_;
        entry.obj_ = obj;
        bmodel_.fireContentsChanged( bmodel_, irow, irow );
        return oldval;
    }

    public T remove( int irow ) {
        Entry<T> entry = entryList_.remove( irow );
        bmodel_.fireIntervalRemoved( bmodel_, irow, irow );
        return entry.obj_;
    }

    /**
     * Returns the unique ID value for the option currently at a given index
     * in this list.  The ID value for a given option is a small integer
     * which does not change.  ID values are not re-used within a given
     * instance of this class.
     *
     * @param  index  current index for option
     * @return  identifier for option
     */
    public int indexToId( int index ) {
        return entryList_.get( index ).id_;
    }

    /**
     * Returns the index at which an option with the given ID currently resides.
     * If no option with the given ID is present, -1 is returned.
     *
     * @param   id  unique identifier
     * @return  index of option with <code>id</code>, or -1
     * @see   #indexToId
     */
    public int idToIndex( int id ) {
        int index = 0;
        for ( Iterator<Entry<T>> it = entryList_.iterator(); it.hasNext();
              index++ ) {
            if ( it.next().id_ == id ) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Constructs a new ComboBoxModel backed by this list.
     *
     * @see  #makeComboBox
     */
    public ComboBoxModel<T> makeComboBoxModel() {
        class ListComboBoxModel extends BasicListModel
                                implements ComboBoxModel<T>, ListDataListener {
            private Object selected_;
            public void setSelectedItem( Object item ) {
                if ( ( selected_ != null && ! selected_.equals( item ) ) ||
                     ( selected_ == null && item != null ) ) {
                    selected_ = item;
                    // ?? this is what the Sun DefaultComboBoxModel does
                    fireContentsChanged( this, -1, -1 );
                }
            }
            public Object getSelectedItem() {
                return selected_;
            }
            public void contentsChanged( ListDataEvent evt ) {
                fireContentsChanged( this, evt.getIndex0(), evt.getIndex1() );
            }
            public void intervalAdded( ListDataEvent evt ) {
                fireIntervalAdded( this, evt.getIndex0(), evt.getIndex1() );
            }
            public void intervalRemoved( ListDataEvent evt ) {
                fireIntervalRemoved( this, evt.getIndex0(), evt.getIndex1() );
            }
        }
        ListComboBoxModel cbm = new ListComboBoxModel();
        addListDataListener( cbm );
        return cbm;
    }

    /**
     * Makes a new JComboBox from this model.  This adds to the functionality
     * of {@link #makeComboBoxModel} by ensuring that the box is 
     * revalidated when new items are added to the model; otherwise the
     * box can end up too small.
     *
     * <p>Note however that no renderer is installed, so custom
     * rendering must be as required handled by client code.
     *
     * @return  a combo box from which items in this model can be selected
     */
    public JComboBox<T> makeComboBox() {
        final JComboBox<T> box = new JComboBox<T>( makeComboBoxModel() );
        addListDataListener( new ListDataListener() {
            public void intervalAdded( ListDataEvent evt ) {
                box.revalidate();
            }
            public void intervalRemoved( ListDataEvent evt ) {}
            public void contentsChanged( ListDataEvent evt ) {}
        } );
        return box;
    }

    /**
     * Constructs a new JMenu backed by this list.
     * One entry is added to the menu for each option in this list;
     * the menu item will be labelled by the list item (using its toString
     * method) and will activate the supplied <code>menuAction</code> action
     * if selected.  In this case the action's <code>actionPerformed</code>
     * method will be called with an <code>ActionEvent</code> that has
     * an <code>id</code> corresponding to its position in this list and
     * a <code>command</code> string which is the same as its toString method.
     *
     * @param   menuName  the name of the menu
     * @param   menuAction the action to activate
     */
    public JMenu makeJMenu( String menuName, final Action menuAction ) {
        class ListMenu extends JMenu implements ListDataListener {
            public void intervalAdded( ListDataEvent evt ) {
                int start = evt.getIndex0();
                int nel = evt.getIndex1() - start + 1;
                if ( start == getItemCount() ) {
                    for ( int i = start; i < start + nel; i++ ) {
                        add( makeJMenuItem( menuAction, i ) );
                    }
                }
                else {
                    throw new UnsupportedOperationException( 
                        "Can't handle event: " + evt );
                }
            }
            public void intervalRemoved( ListDataEvent evt ) {
                contentsChanged( evt );
            }
            public void contentsChanged( ListDataEvent evt ) {
                removeAll();
                for ( int i = 0; i < entryList_.size(); i++ ) {
                    add( makeJMenuItem( menuAction, i ) );
                }
            }
        }
        ListMenu menu = new ListMenu();
        menu.setText( menuName );
        menu.contentsChanged( new ListDataEvent( this, 
                                                 ListDataEvent.CONTENTS_CHANGED,
                                                 0, entryList_.size() - 1 ) );
        addListDataListener( menu );
        return menu;
    }

    private JMenuItem makeJMenuItem( final Action menuAction, 
                                     final int index ) {
        final String text = entryList_.get( index ).obj_.toString();
        Action act = new AbstractAction( text ) {
            public void actionPerformed( ActionEvent evt ) {
                ActionEvent evt1 = new ActionEvent( this, index, text );
                menuAction.actionPerformed( evt1 );
            }
        };
        return new JMenuItem( act );
    }

    public CheckBoxMenu makeCheckBoxMenu( String menuName ) {
        class ListMenu extends CheckBoxMenu implements ListDataListener {
            public void intervalAdded( ListDataEvent evt ) {
                int start = evt.getIndex0();
                int nel = evt.getIndex1() - start + 1;
                if ( start == getItemCount() ) {
                    for ( int i = start; i < start + nel; i++ ) {
                        addMenuItem( entryList_.get( i ).obj_.toString() );
                    }
                }
                else {
                    throw new UnsupportedOperationException(
                        "Can't handle event: " + evt );
                }
            }
            public void intervalRemoved( ListDataEvent evt ) {
                contentsChanged( evt );
            }
            public void contentsChanged( ListDataEvent evt ) {
                removeAll();
                for ( Entry<T> entry : entryList_ ) {
                    addMenuItem( entry.toString() );
                }
            }
        }
        ListMenu menu = new ListMenu();
        menu.setText( menuName );
        menu.contentsChanged( new ListDataEvent( this, 
                                                 ListDataEvent.CONTENTS_CHANGED,
                                                 0, entryList_.size() - 1 ) );
        addListDataListener( menu );
        return menu;
    }

    /*
     * Provide the functionality in AbstractListModel by delegation.
     */
    public void addListDataListener( ListDataListener l ) {
        bmodel_.addListDataListener( l );
    }
    public void removeListDataListener( ListDataListener l ) {
        bmodel_.removeListDataListener( l );
    }
    public void fireContentsChanged( int i0, int i1 ) {
        bmodel_.fireContentsChanged( bmodel_, i0, i1 );
    }
    public void fireIntervalAdded( int i0, int i1 ) {
        bmodel_.fireIntervalAdded( bmodel_, i0, i1 );
    }
    public void fireIntervalRemoved( int i0, int i1 ) {
        bmodel_.fireIntervalRemoved( bmodel_, i0, i1 );
    }

    /**
     * ListModel adapter.
     */
    private class BasicListModel extends AbstractListModel<T> {
        public T getElementAt( int index ) {
            return get( index );
        }
        public int getSize() {
            return size();
        }
        protected void fireContentsChanged( Object source, int i0, int i1 ) {
            super.fireContentsChanged( source, i0, i1 );
        }
        protected void fireIntervalAdded( Object source, int i0, int i1 ) {
            super.fireIntervalAdded( source, i0, i1 );
        }
        protected void fireIntervalRemoved( Object source, int i0, int i1 ) {
            super.fireIntervalRemoved( source, i0, i1 );
        }
    }

    /**
     * Struct type class which associates an object and a unique identifier.
     */
    private static class Entry<T> {
        final int id_;
        T obj_;

        /**
         * Constructor.
         *
         * @param  unique identifier
         * @param  option object
         */
        Entry( int id, T obj ) {
            id_ = id;
            obj_ = obj;
        }
    }
}
