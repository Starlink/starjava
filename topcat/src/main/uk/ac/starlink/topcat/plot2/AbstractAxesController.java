package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.topcat.ActionForwarder;

/**
 * Partial AxesController implementation.
 *
 * @author   Mark Taylor
 * @since    18 Aug 2023
 */
public abstract class AbstractAxesController<P,A>
                implements AxesController<P,A> {

    private final ActionForwarder actionForwarder_;
    private final List<Control> controlList_;

    /**
     * Constructor.
     */
    protected AbstractAxesController() {
        actionForwarder_ = new ActionForwarder();
        controlList_ = new ArrayList<Control>();
    }

    /**
     * Adds a control to the list of controls managed by this object.
     *
     * @param  control   control to add
     */
    protected void addControl( Control control ) {
        controlList_.add( control );
        control.addActionListener( actionForwarder_ );
    }

    /**
     * Returns all the controls for user configuration of this controller.
     * This includes the main control and possibly others.
     *
     * @return  user controls
     */
    public Control[] getStackControls() {
        return controlList_.toArray( new Control[ 0 ] );
    }

    /**
     * Adds a listener notified when any of the controls changes.
     *
     * @param  listener  listener to add
     */
    public void addActionListener( ActionListener listener ) {
        actionForwarder_.addActionListener( listener );
    }

    /**
     * Removes a listener previously added by addActionListener.
     *
     * @param   listener   listener to remove
     */
    public void removeActionListener( ActionListener listener ) {
        actionForwarder_.removeActionListener( listener );
    }

    /**
     * Returns an object which will forward actions to listeners registered
     * with this panel.
     *
     * @return  action forwarder
     */
    public ActionListener getActionForwarder() {
        return actionForwarder_;
    }
}
