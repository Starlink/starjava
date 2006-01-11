package uk.ac.starlink.util.gui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Extends ButtonGroup by associating a value with each button.
 *
 * @author   Mark Taylor
 * @since    11 Jan 2005
 */
public class ValueButtonGroup extends ButtonGroup {

    private final Map valueMap_;
    private final List listeners_;
    private final ChangeListener buttonListener_;

    /**
     * Constructor.
     */
    public ValueButtonGroup() {
        valueMap_ = new HashMap();
        listeners_ = new ArrayList();
        buttonListener_ = new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                if ( evt.getSource() instanceof AbstractButton ) {
                    AbstractButton butt = (AbstractButton) evt.getSource();
                    if ( butt.isSelected() ) {
                        fireValueChanged();
                    }
                }
                else {
                    fireValueChanged();
                }
            }
        };
    }

    /**
     * Adds a button and associates a value with it.
     * When <code>button</code> is selected, {@link #getValue} will
     * return <code>value</code>.  You can use <code>null</code> for a
     * value, but don't have two buttons with associated values which
     * are equal.
     *
     * @param   button   button 
     * @param   value    associated value
     */
    public void add( AbstractButton button, Object value ) {
        super.add( button );
        valueMap_.put( button, value );
        button.addChangeListener( buttonListener_ );
    }

    public void remove( AbstractButton button ) {
        button.removeChangeListener( buttonListener_ );
        valueMap_.remove( button );
        super.remove( button );
    }

    /**
     * Returns the currently selected value, that is the value associated
     * with the currently selected button.
     *
     * @return   selected value
     */
    public Object getValue() {
        for ( Iterator it = buttons.iterator(); it.hasNext(); ) {
            AbstractButton button = (AbstractButton) it.next();
            if ( button.isSelected() ) {
                return valueMap_.get( button );
            }
        }
        return null;
    }

    /**
     * Sets the currently selected value.  The associated button will be
     * selected (and others deselected).  <code>value</code> must be one
     * of the values associated with a button in this group.
     *
     * @param  value  new value
     */
    public void setValue( Object value ) {
        for ( Iterator it = valueMap_.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            AbstractButton button = (AbstractButton) entry.getKey();
            if ( value == null ? ( value == entry.getValue() ) 
                               : ( value.equals( entry.getValue() ) ) ) {
                if ( ! button.isSelected() ) {
                    button.setSelected( true );
                    fireValueChanged();
                }
                return;
            }
        }
        throw new IllegalArgumentException( "No value " + value + " in group" );
    }

    /**
     * Adds a listener which will be notified whenever this group's selected
     * value changes.
     *
     * @param  listener  listener to add
     */
    public void addChangeListener( ChangeListener listener ) {
        listeners_.add( listener );
    }

    /**
     * Removes a listener previously added by <code>addChangeListener</code>.
     *
     * @param  listener   listener to remove
     */
    public void removeChangeListener( ChangeListener listener ) {
        listeners_.remove( listener );
    }

    /**
     * Notifies listeners that the value has changed.
     */
    private void fireValueChanged() {
        ChangeEvent evt = new ChangeEvent( this );
        for ( Iterator it = listeners_.iterator(); it.hasNext(); ) {
            ((ChangeListener) it.next()).stateChanged( evt );
        }
    }
}
