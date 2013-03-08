package uk.ac.starlink.ttools.plot2.config;

import java.awt.event.ActionListener;
import javax.swing.JComponent;

/**
 * Can acquire a typed value from the GUI.
 *
 * @author   Mark Taylor
 * @since    5 Mar 2013
 */
public interface Specifier<V> {

    /**
     * Returns the graphical component that the user can interact with
     * to supply a value.  It should be line-like (not tall).
     *
     * <p>The returned component should preferably honour the JComponent
     * <code>setEnabled</code>/<code>isEnabled</code> methods.
     *
     * @return   specifier component
     */
    JComponent getComponent();

    /**
     * Returns the typed value currently specified by the graphical component.
     *
     * @return   specified value
     */
    V getSpecifiedValue();

    /**
     * Sets the typed value represented by the graphical component.
     * Calling this method ought to make it clear to the user what value
     * it is set at; in any case a subsequent call of
     * <code>getSpecifiedValue</code> should yield the same result.
     *
     * <p>However if a value is set which is of the correct type but
     * cannot be represented by this specifier, results are unpredictable.
     *
     * @param  value  new value
     */
    void setSpecifiedValue( V value );

    /**
     * Adds a listener which will be informed when the user interacts with
     * the graphical component to cause a (potential) change in the value.
     *
     * @param  listener  listener to add
     */
    void addActionListener( ActionListener listener );

    /**
     * Removes a listener previously added by <code>addActionListener</code>.
     *
     * @param  listener  listener to remove
     */
    void removeActionListener( ActionListener listener );

    /**
     * Whether the GUI component should fill the available width of a panel.
     * This rendering hint should on the whole this should be true
     * for expandable components, and false for fixed size components.
     * Components should have a fixed vertical size in any case.
     *
     * @return   true for horizontally expandable components
     */
    boolean isXFill();
}
