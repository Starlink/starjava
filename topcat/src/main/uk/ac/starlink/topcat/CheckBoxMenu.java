package uk.ac.starlink.topcat;

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

    public CheckBoxMenu() {
        super();
        setSelectionModel( new DefaultListSelectionModel() );
    }

    public CheckBoxMenu( String name ) {
        this(); 
        setText( name );
    }

    public int getEntryCount() {
        return getItemCount();
    }

    public void addListSelectionListener( ListSelectionListener listener ) {
        selModel.addListSelectionListener( listener );
    }

    public void removeListSelectionListener( ListSelectionListener listener ) {
        selModel.removeListSelectionListener( listener );
    }

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

    public ListSelectionModel getSelectionModel() {
        return selModel;
    }

    public void setSelectionModel( ListSelectionModel selModel ) {
        if ( this.selModel != null ) {
            this.selModel.removeListSelectionListener( this );
        }
        this.selModel = selModel;
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
