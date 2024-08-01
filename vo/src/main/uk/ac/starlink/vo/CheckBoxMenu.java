package uk.ac.starlink.vo;

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A menu which contains only checkbox-type entries and has an associated
 * ListSelectionModel.
 */
public class CheckBoxMenu extends JMenu implements ListSelectionListener {

    private ListSelectionModel selModel;

    /**
     * Constructs a new CheckBoxMenu.
     */
    @SuppressWarnings("this-escape")
    public CheckBoxMenu() {
        super();
        setSelectionModel( new DefaultListSelectionModel() );
    }

    /**
     * Constructs a new CheckBoxMenu with a given name.
     *
     * @param  name  the menu name
     */
    @SuppressWarnings("this-escape")
    public CheckBoxMenu( String name ) {
        this(); 
        setText( name );
    }

    /**
     * Returns the number of tickable entries in the menu.
     *
     * @return  number of entries
     */
    public int getEntryCount() {
        return getItemCount();
    }

    public void addListSelectionListener( ListSelectionListener listener ) {
        selModel.addListSelectionListener( listener );
    }

    public void removeListSelectionListener( ListSelectionListener listener ) {
        selModel.removeListSelectionListener( listener );
    }

    /**
     * Adds an item to the menu.  The item will be represented as a 
     * checkbox menu item; ticking/unticking it will cause this object's
     * selection model to be updated (and vice versa).
     *
     * @param  text  the label for the next item on the menu
     */
    public void addMenuItem( String text ) {
        final int pos = getEntryCount();
        final JCheckBoxMenuItem item = 
            new JCheckBoxMenuItem( text, selModel.isSelectedIndex( pos ) );
        item.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                if ( item.getState() ) {
                    selModel.addSelectionInterval( pos, pos );
                }
                else {
                    selModel.removeSelectionInterval( pos, pos );
                }
            }
        } );
        add( item );
    }

    /**
     * Returns the selection model used to keep track of the ticked/unticked
     * status of the checkboxes in this menu.
     *
     * @return   the selection model
     */
    public ListSelectionModel getSelectionModel() {
        return selModel;
    }

    /**
     * Sets the selection model used to keep track of the ticked/unticked
     * status of the checkboxes in this menu.  You can slot your own
     * model in here, any previous one is discarded by this object.
     * 
     * @param   selModel the new selection model
     */
    public void setSelectionModel( ListSelectionModel selModel ) {
        if ( this.selModel != null ) {
            this.selModel.removeListSelectionListener( this );
        }
        this.selModel = selModel;
        for ( int i = 0; i < getItemCount(); i++ ) {
            ((JCheckBoxMenuItem) getItem( i ))
           .setState( selModel.isSelectedIndex( i ) );
        }
        selModel.addListSelectionListener( this );
    }

    public void valueChanged( ListSelectionEvent evt ) {
        int first = evt.getFirstIndex();
        int last = evt.getLastIndex();
        for ( int i = first; i <= last; i++ ) {
            ((JCheckBoxMenuItem) getItem( i ))
           .setState( selModel.isSelectedIndex( i ) );
        }
    }
    
}
