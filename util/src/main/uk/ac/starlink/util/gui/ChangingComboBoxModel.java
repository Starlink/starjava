package uk.ac.starlink.util.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.DefaultComboBoxModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * ComboBoxModel which can notify {@link javax.swing.event.ChangeListener}s
 * and {@link javax.swing.event.ActionListener}s of changes in the selection.
 *
 * @author   Mark Taylor
 * @since    6 Jun 2007
 */
public class ChangingComboBoxModel extends DefaultComboBoxModel {

    private final Collection changeListeners_;
    private final Collection actionListeners_;

    /**
     * Constructs an empty model.
     */
    public ChangingComboBoxModel() {
        this( new Object[ 0 ] );
    }

    /**
     * Constructs a model with a given initial list of items.
     *
     * @param   items  initial list of items in the model
     */
    public ChangingComboBoxModel( Object[] items ) {
        super( items );
        changeListeners_ = new ArrayList();
        actionListeners_ = new ArrayList();
    }

    /**
     * Adds a listener which is notified whenever the selection changes.
     *
     * @param  listener  listener to add
     */
    public void addChangeListener( ChangeListener listener ) {
        changeListeners_.add( listener );
    }

    /**
     * Removes a listener previously added by {@link #addListener}.
     *
     * @param   listener  listener to remove
     */
    public void removeChangeListener( ChangeListener listener ) {
        changeListeners_.remove( listener );
    }

    /**
     * Adds a listener which is notified whenever the selection changes.
     *
     * @param  listener  listener to add
     */
    public void addActionListener( ActionListener listener ) {
        actionListeners_.add( listener );
    }

    /**
     * Removes a listener previously added by {@link #addListener}.
     *
     * @param   listener  listener to remove
     */
    public void removeActionListener( ActionListener listener ) {
        actionListeners_.remove( listener );
    }

    public void setSelectedItem( Object item ) {
        Object oldItem = getSelectedItem();
        super.setSelectedItem( item );
        if ( item == null ? oldItem != null
                          : ! item.equals( oldItem ) ) {
            fireSelectionChanged( this );
            fireActionPerformed( this );
        }
    }

    /**
     * Called to notify listeners of a change.
     *
     * @param  source  change source
     */
    protected void fireSelectionChanged( Object source ) {
        ChangeEvent evt = new ChangeEvent( source );
        for ( Iterator it = changeListeners_.iterator(); it.hasNext(); ) {
            ChangeListener listener = (ChangeListener) it.next();
            listener.stateChanged( evt );
        }
    }

    /**
     * Called to notify listeners of a change.
     *
     * @param  source  change source
     */
    protected void fireActionPerformed( Object source ) {
        ActionEvent evt = new ActionEvent( source, 0, "change" );
        for ( Iterator it = actionListeners_.iterator(); it.hasNext(); ) {
            ActionListener listener = (ActionListener) it.next();
            listener.actionPerformed( evt );
        }
    }
}
