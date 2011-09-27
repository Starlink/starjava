package uk.ac.starlink.topcat;

import java.awt.Component;
import javax.swing.Action;

/**
 * Defines an action which will appear on the TOPCAT tool bar.
 * As well as implementing this interface, such actions must have a
 * public no-arg constructor.
 * See the {@link DemoToolAction} class for an example implementation.
 *
 * @author   Mark Taylor
 * @since    27 Sep 2011
 */
public interface TopcatToolAction extends Action {

    /**
     * Sets the parent component.
     * This may be used when placing any windows associated with this action.
     * This method will normally be called once, after construction and before
     * the action is invoked.
     *
     * @param  parent  parent component
     */
    void setParent( Component parent );
}
