package uk.ac.starlink.topcat;

import java.awt.event.ActionEvent;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
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

/**
 * Provides storage for a list of options with listeners.
 * This class implements both {@link java.util.List} and  
 * {@link javax.swing.ListModel} (which <tt>ListModel</tt> really
 * ought to do itself), and also provides methods to create 
 * some useful models like <tt>JComboBoxModel</tt> based on the same data.
 * <p>
 * The <tt>List</tt> implementation is not fully mutable, though you can 
 * add an item to the end.
 * <p>
 * The implementation of this class is a bit hairy at the moment, and
 * there may be bugs.  I'm not sure whether I'm going to use it long
 * term or not yet; if so it will need shoring up somewhat.
 *
 * @author   Mark Taylor (Starlink)
 */
public class OptionsListModel extends AbstractList implements ListModel {

    private final List options;
    private final BasicListModel bmodel;

    
    public OptionsListModel() {
        options = new ArrayList();
        bmodel = new BasicListModel();
    }

    public Object get( int index ) {
        return options.get( index );
    }

    public Object getElementAt( int index ) {
        return get( index );
    }

    public int size() {
        return options.size();
    }

    public int getSize() {
        return size();
    }

    public boolean add( Object obj ) {
        int index = options.size();
        boolean ret = options.add( obj );
        fireIntervalAdded( index, index );
        return ret;
    }

    /**
     * Constructs a new ComboBoxModel backed by this list.
     *
     * @see  #makeComboBox
     */
    public ComboBoxModel makeComboBoxModel() {
        class ListComboBoxModel extends BasicListModel 
                                implements ComboBoxModel, ListDataListener {
            private Object selected;
            public void setSelectedItem( Object item ) {
                if ( ( selected != null && ! selected.equals( item ) ) ||
                     ( selected == null && item != null ) ) {
                    selected = item;
                    // ?? this is what the Sun DefaultComboBoxModel does
                    fireContentsChanged( this, -1, -1 );
                }
            }
            public Object getSelectedItem() {
                return selected;
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
     * @return  a combo box from which items in this model can be selected
     */
    public JComboBox makeComboBox() {
        final JComboBox box = new JComboBox( makeComboBoxModel() );
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
     * method) and will activate the supplied <tt>menuAction</tt> action
     * if selected.  In this case the action's <tt>actionPerformed</tt>
     * method will be called with an <tt>ActionEvent</tt> that has
     * an <tt>id</tt> corresponding to its position in this list and
     * a <tt>command</tt> string which is the same as its toString method.
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
                for ( int i = 0; i < options.size(); i++ ) {
                    add( makeJMenuItem( menuAction, i ) );
                }
            }
        }
        ListMenu menu = new ListMenu();
        menu.setText( menuName );
        menu.contentsChanged( new ListDataEvent( this, 
                                                 ListDataEvent.CONTENTS_CHANGED,
                                                 0, options.size() - 1 ) );
        addListDataListener( menu );
        return menu;
    }

    private JMenuItem makeJMenuItem( final Action menuAction, 
                                     final int index ) {
        final String text = options.get( index ).toString();
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
                        addMenuItem( options.get( i ).toString() );
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
                int nel = options.size();
                for ( int i = 0; i < nel; i++ ) {
                    addMenuItem( options.get( i ).toString() );
                }
            }
        }
        ListMenu menu = new ListMenu();
        menu.setText( menuName );
        menu.contentsChanged( new ListDataEvent( this, 
                                                 ListDataEvent.CONTENTS_CHANGED,
                                                 0, options.size() - 1 ) );
        addListDataListener( menu );
        return menu;
    }

    /*
     * Methods and classes below here are just concerned with providing
     * the functionality in AbstractListModel by delegation.
     */

    public void addListDataListener( ListDataListener l ) {
        bmodel.addListDataListener( l );
    }
    public void removeListDataListener( ListDataListener l ) {
        bmodel.removeListDataListener( l );
    }

    public void fireContentsChanged( int i0, int i1 ) {
        bmodel.fireContentsChanged( bmodel, i0, i1 );
    }
    public void fireIntervalAdded( int i0, int i1 ) {
        bmodel.fireIntervalAdded( bmodel, i0, i1 );
    }
    public void fireIntervalRemoved( int i0, int i1 ) {
        bmodel.fireIntervalRemoved( bmodel, i0, i1 );
    }

    private class BasicListModel extends AbstractListModel {
        public Object getElementAt( int index ) {
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
       
}
