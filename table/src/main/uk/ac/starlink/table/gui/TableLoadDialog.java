package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.io.IOException;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenu;
import uk.ac.starlink.table.StarTableFactory;

/**
 * Interface for an object which can handle the user interaction for
 * selecting a table or tables to load.
 *
 * @author   Mark Taylor
 * @since    13 Sept 2010
 */
public interface TableLoadDialog {

    /**
     * Returns the name of this dialogue.
     * This may be used as the text of a button
     * ({@link javax.swing.Action#NAME}).
     *
     * @return  dialogue name
     */
    String getName();

    /**
     * Returns a short description of this dialogue.
     * This may be used as the tooltip text of a button
     * ({@link javax.swing.Action#SHORT_DESCRIPTION}).
     *
     * @return  dialogue description
     */
    String getDescription();

    /**
     * Returns an icon associated with this dialogue.
     * A size of 24x24 pixels is preferred.
     * Null may be returned if no icon is available.
     *
     * @return  dialogue icon
     */
    Icon getIcon();

    /**
     * Returns the GUI component which allows the user to select how tables
     * are to be loaded.
     *
     * @return  component for user interaction
     */
    Component getQueryComponent();

    /**
     * Returns an array of menus which may be presented in the window 
     * alongside the query component.
     *
     * @return   menu array; may be empty
     */
    JMenu[] getMenus();

    /**
     * Returns an array of actions suitable for presentation as toolbar
     * buttons alongside the query component.
     *
     * @return  toolbar action array; may be empty
     */
    Action[] getToolbarActions();

    /**
     * Indicates whether this dialogue may be used.  Normally it will return
     * true, but in the case that classes or other resources required 
     * for its use are missing, it should return false.  In this case most
     * of the other methods will not be called.
     * 
     * @return  true iff this dialogue may be able to do something useful
     */
    boolean isAvailable();

    /**
     * Provides some configuration which must be performed before use.
     * This method should be called before {@link #getQueryComponent} is called.
     *
     * <p>The <code>tfact</code> argument provides a table factory which
     * resembles the one to be used for generating tables.
     * Although this factory should not in general be used or retained,
     * since the one presented later to the TableLoader should be used
     * instead, it can be interrogated for known table formats etc.
     *
     * <p>The <code>submitAct</code> argument sets the action which
     * when invoked will cause {@link #createTableLoader} to be called.
     * Its setEnabled method can be called to reflect readiness,
     * and it can be added as a listener to dialogue-specific events
     * which indicate that a selection has been made.
     *
     * @param  tfact  representative table factory
     * @param  submitAct   action for load submission
     */
    void configure( StarTableFactory tfact, Action submitAct );

    /**
     * Returns the action set by {@link #configure}.
     *
     * @return  action which initiates a table load attempt
     */
    Action getSubmitAction();

    /**
     * Returns a new object which specifies how table loading is to 
     * be performed.  The actions performed by the returned object will
     * presumably be determined by the state at call time of this 
     * dialogues GUI component.
     *
     * <p>If the dialogue is not in a suitable state, either return null,
     * or, if you want to provide more detailed information about what's
     * wrong, throw a RuntimeException with an informative message.
     *
     * @return   new table loader object
     */
    TableLoader createTableLoader();
}
