package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.JComponent;

/**
 * User interaction component that can appear in a ControlStack.
 * A control has a compact representation given by its label and icon,
 * and also a panel which has its actual GUI content.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public interface Control {

    /**
     * Returns the GUI component that is the business end of this control.
     * It will typically contain components for user interaction.
     *
     * @return   this controls graphical component
     */
    JComponent getPanel();

    /**
     * Returns a short string to label this control, visible in the stack.
     *
     * @return  label
     */
    String getControlLabel();

    /**
     * Returns an icon to represent this control, visible in the stack.
     *
     * @return   icon
     */
    Icon getControlIcon();

    /**
     * Adds a listener to be notified whenever the state of this control
     * changes, presumably as a result of user interaction.
     *
     * @param  listener  listener to add
     */
    void addActionListener( ActionListener listener );

    /**
     * Removes a listener which was previously added.
     *
     * @param   listener to remove
     */
    void removeActionListener( ActionListener listener );
}
