package uk.ac.starlink.ttools.plot2.config;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Partial Specifier implementation.
 *
 * @author   Mark Taylor
 * @since    5 Mar 2013
 */
public abstract class SpecifierPanel<V> implements Specifier<V> {

    private final boolean isXFill_;
    private final List<ActionListener> listeners_;
    private final ActionListener actionForwarder_;
    private final ChangeListener changeForwarder_;
    private JComponent component_;

    /**
     * Constructor.
     *
     * @param  isXFill  true if the graphical component should expand to fill
     *                  the available horizontal space
     */
    protected SpecifierPanel( boolean isXFill ) {
        isXFill_ = isXFill;
        listeners_ = new ArrayList<ActionListener>();
        actionForwarder_ = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                forwardAction( evt );
            }
        };
        changeForwarder_ = new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                forwardAction( new ActionEvent( evt.getSource(), 0,
                                                "Change" ) );
            }
        };
    }

    public boolean isXFill() {
        return isXFill_;
    }

    /**
     * Abstract method called lazily during <code>getComponent</code>
     * to obtain the graphical component used by this specifier.
     * It will be called a maximum of once.  It is not necessary that
     * the component actually be created in this method, for instance
     * it may be created at construction time if that's more convenient.
     *
     * @return   graphical component
     */
    protected abstract JComponent createComponent();

    public JComponent getComponent() {
        if ( component_ == null ) {
            component_ = createComponent();
        }
        return component_;
    }

    public void addActionListener( ActionListener listener ) {
        listeners_.add( listener );
    }

    public void removeActionListener( ActionListener listener ) {
        listeners_.remove( listener );
    }

    /**
     * Returns a listener which will take ActionEvents and forward them
     * to any listeners registered with this panel.
     *
     * <p>In general any input component which forms part of this panel's
     * GUI should have as a listener the result of
     * <code>getActionForwarder</code> or <code>getChangeForwarder</code>,
     * so that changes in their state are propagated to listeners
     * of this specifier.
     *
     * @return   action forwarder
     */
    protected ActionListener getActionForwarder() {
        return actionForwarder_;
    }

    /**
     * Returns a listener which will take ChangeEvents and forward them
     * to any listeners registered with this panel.
     *
     * <p>In general any input component which forms part of this panel's
     * GUI should have as a listener the result of
     * <code>getActionForwarder</code> or <code>getChangeForwarder</code>,
     * so that changes in their state are propagated to listeners
     * of this specifier.
     *
     * @return  change forwarder
     */
    protected ChangeListener getChangeForwarder() {
        return changeForwarder_;
    }

    /**
     * Notifies all the registered action listeners of a non-specific event.
     */
    protected void fireAction() {
        forwardAction( new ActionEvent( this, 0, "Act" ) );
    }

    /**
     * Takes an action event and passes it on to all the registered listeners
     * of this panel.
     *
     * @param  evt  event to forward
     */
    private void forwardAction( ActionEvent evt ) {
        for ( ActionListener listener : listeners_ ) {
            listener.actionPerformed( evt );
        }
    }
}
