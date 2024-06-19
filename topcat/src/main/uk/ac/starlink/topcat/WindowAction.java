package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Icon;
import javax.swing.JToggleButton;

/**
 * An {@link javax.swing.Action} which controls display of a window.
 * {@link java.awt.event.ActionEvent}s passed to this action may
 * have the action command set to one of the strings 
 * {@link #HIDE} or {@link #SHOW} to define what the command means
 * (in fact anything other than <code>HIDE</code> counts as <code>SHOW</code>).
 * It has a bound property with key {@link #VISIBLE} which indicates
 * whether the associated window is currently showing or not.
 * Setting the <code>VISIBLE</code> property true/false has the same effect as 
 * invoking the action with the <code>SHOW</code>/<code>HIDE</code>
 * command string.
 * <p>
 * This class is currently a bit messy and overspecified for what it does.
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Mar 2004
 */
public abstract class WindowAction extends BasicAction {

    /**
     * Action command text for the action which hides the window associated
     * with this action.
     */
    public static final String HIDE = "HIDE";

    /**
     * Action command text for the action which reveals the window associated
     * with this action.
     */
    public static final String SHOW = "SHOW";

    /**
     * Name of the bound property which indicates whether the window associated
     * with this action is currently visible in the GUI.
     */
    public static final String VISIBLE = "VISIBLE";

    private ActionEvent currentEvent;

    /**
     * Constructs a new WindowAction.
     *
     * @param   name  action name
     * @param   icon  action icon
     * @param   shortdesc   action short description property
     */
    protected WindowAction( String name, Icon icon, String shortdesc ) {
        super( name, icon, shortdesc );
    }

    public void actionPerformed( ActionEvent evt ) {
        String cmd = evt.getActionCommand();
        boolean show = ! HIDE.equals( cmd );
        currentEvent = evt;
        putValue( VISIBLE, Boolean.valueOf( show ) );
        currentEvent = null;
    }

    public Object getValue( String key ) {
        if ( VISIBLE.equals( key ) ) {
            return Boolean.valueOf( hasWindow() && 
                                    getWindow( null ).isShowing() );
        }
        else {
            return super.getValue( key );
        }
    }

    public void putValue( String key, Object newValue ) {
        if ( VISIBLE.equals( key ) ) {
            boolean show = ((Boolean) newValue).booleanValue();
            if ( show ) {
                boolean windowCreated = ! hasWindow();
                Window win = getWindow( getEventWindow( currentEvent ) );
                boolean wasVisible = ( ! windowCreated ) && win.isShowing();
                win.setVisible( true );
                if ( ! wasVisible ) {
                    firePropertyChange( VISIBLE, Boolean.FALSE, Boolean.TRUE );
                }
                if ( windowCreated ) {
                    win.addWindowListener( new WindowAdapter() {
                        public void windowClosed( WindowEvent evt ) {
                            firePropertyChange( VISIBLE, Boolean.TRUE,
                                                Boolean.FALSE );
                        }
                    } );
                }
            }
            else {
                if ( hasWindow() ) {
                    Window win = getWindow( null );
                    boolean wasVisible = win.isShowing();
                    if ( wasVisible ) {
                        getWindow( null ).dispose();
                        firePropertyChange( VISIBLE, Boolean.TRUE,
                                            Boolean.FALSE );
                    }
                }
            }
        }
        else {
            super.putValue( key, newValue );
        }
    }

    /**
     * Returns the window associated with this action, creating it if
     * necessary.
     *
     * @param  parent  component which may be used for placing the new window
     */
    public abstract Window getWindow( Component parent );

    /**
     * Indicates whether the window associated with this window is 
     * currently in existence.
     *
     * @return  true iff the window exists
     */
    public abstract boolean hasWindow();

    /**
     * Returns a toggle button which toggles visibility of the window
     * associated with this action.
     * <p>
     * ** Mostly working but not used at the moment **
     *
     * @return  new button
     */
    public JToggleButton getButton() {
        final JToggleButton button = new JToggleButton();
        button.setIcon( (Icon) getValue( SMALL_ICON ) );
        button.setToolTipText( (String) getValue( NAME ) );
        button.setModel( new JToggleButton.ToggleButtonModel() {
            public boolean isSelected() {
                return ((Boolean) getValue( VISIBLE )).booleanValue();
            }
            public void setSelected( boolean vis ) {
                putValue( VISIBLE, Boolean.valueOf( vis ) );
            }
        } );
        addPropertyChangeListener( new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent evt ) {
                if ( VISIBLE.equals( evt.getPropertyName() ) ) {
                    button.setSelected( button.isSelected() );
                }
            }
        } );
        return button;
    }
}
