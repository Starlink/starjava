package uk.ac.starlink.table.gui;

import java.awt.Component;
import javax.swing.ComboBoxModel;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

/**
 * Interface describing the action of a dialogue with which the user
 * can interact to specify a new table to load.
 *
 * @author    Mark Taylor (Starlink)
 * @since     25 Nov 2004
 * @see    LoadWorker
 */
public interface TableLoadDialog {

    /**
     * Presents the user with a dialogue which may be used to specify 
     * a table to load.  This method should return true if an attempt
     * will be made to load a table, and false if it will not
     * (for instance if the user hit a Cancel button).
     *
     * <p>In the event that true is returned, the implementation of this
     * method should ensure that notification of the table load attempt
     * should be made to the <tt>consumer</tt> argument 
     * using the defined {@link TableConsumer} methods.
     * The purpose of doing it like this
     * (rather than just returning a <tt>StarTable</tt> from this method)
     * is so that the table loading, which may be time-consuming,
     * can be done in a thread other than the event dispatch thread
     * on which this method will have been called.
     * The {@link LoadWorker} class is provided to assist with this;
     * the usual idiom for performing the load from the event dispatch
     * thread within the implementation of this method looks like this:
     * <pre>
     *     new LoadWorker( tableConsumer, tableId ) {
     *         protected StarTable attemptLoad() throws IOException {
     *             return tableFactory.makeStarTable( ... );
     *         }
     *     }.invoke();
     * </pre>
     *
     * <p>Error conditions during the course of the user interaction by 
     * this method should in general be dealt with by informing the user
     * (e.g. with a popup) and permitting another attempt.
     * Some sort of cancel button should be provided which should trigger
     * return with a false result.
     *
     * <p>The <tt>formatModel</tt> argument may be used to determine or 
     * set the format to be used for interpreting tables.
     * Its entries are <tt>String</tt>s, and in general the selected one
     * should be passed as the <tt>handler</tt> argument to one of
     * <tt>factory</tt>'s <tt>makeStarTable</tt> methods.
     * The dialogue may wish to allow the user to modify the selection
     * by presenting a {@link javax.swing.JComboBox} based on this model.
     * A suitable model can be obtained using 
     * {@link TableLoadChooser#makeFormatBoxModel}.
     * 
     * @param  parent   parent window
     * @param  factory  factory which may be used for table creation
     * @param  formatModel  comboBoxModel 
     * @param  consumer  object which can do something with the loaded table
     * @return   true if an attempt will be made to load a table
     */
    boolean showLoadDialog( Component parent, StarTableFactory factory,
                            ComboBoxModel formatModel, TableConsumer consumer );

    /**
     * Name of this dialogue.  This will typically be used as the text of
     * a button ({@link javax.swing.Action#NAME}) 
     * which invokes <tt>showLoadDialog</tt>.
     *
     * @return  name
     */
    String getName();

    /**
     * Description of this dialogue.  This will typically be used as the
     * tooltip text of a button ({@link javax.swing.Action#SHORT_DESCRIPTION})
     * which invokes <tt>showLoadDialog</tt>
     *
     * @return   short description
     */
    String getDescription();

    /**
     * Indicates whether this dialog can be invoked.  This allows the
     * implementation to check that it has enough resources (e.g. required
     * classes) for it to be worth trying it.  This method should be 
     * invoked before the first invocation of {@link #showLoadDialog},
     * but is not guaranteed to be invoked again.
     *
     * @return  true iff this dialog can be used
     */
    boolean isAvailable();
}
