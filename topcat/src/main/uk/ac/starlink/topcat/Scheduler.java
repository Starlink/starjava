package uk.ac.starlink.topcat;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Schedules tasks to run conditionally on on the Event Dispatch Thread.
 * Such tasks are only run if this object's (abstract) <code>isActive</code>
 * method returns true at both scheduling time and run time.
 *
 * @author   Mark Taylor 
 * @since    5 Jun 2014
 */
public abstract class Scheduler {

    private final JComponent parent_;

    /**
     * Constructor.
     *
     * @param  parent  parent component used for parenting popup windows;
     *                 may be null
     */
    public Scheduler( JComponent parent ) {
        parent_ = parent;
    }

    /**
     * Indicates whether this object is considered active.
     * If not, no jobs will be scheduled, and any scheduled jobs
     * will be ignored when they are run on the EDT.
     *
     * <p>The expectation is that this method starts off by returning true,
     * but may eventually transition to returning false.  Once that has
     * happened, it will not return true again.
     *
     * @return   true iff this object is considered active
     */
    public abstract boolean isActive();

    /**
     * Schedules a runnable to be performed later on the Event Dispatch Thread,
     * as long as this object is considered active.
     * If <code>isActive</code> returns false either when this method is
     * called, or when the runnable comes to be executed on the EDT,
     * nothing is done.
     *
     * <p>This method may be called on any thread.
     *
     * @param  runnable  action to run on the EDT if still active
     */
    public void schedule( final Runnable runnable ) {
        if ( isActive() ) {
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    if ( isActive() ) {
                        runnable.run();
                    }
                }
            } );
        }
    }

    /**
     * Schedules display of a JOptionPane message.
     *
     * <p>This method may be called on any thread.
     *
     * @param   message   the Object to display
     * @param   title    the title string for the dialog
     * @param   messageType   the type of message to be displayed,
     *          one of the JOptionPane.*_MESSAGE constants
     * @see  javax.swing.JOptionPane
     */
    public void scheduleMessage( final String message, final String title,
                                 final int messageType ) {
        schedule( new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog( parent_, message, title,
                                               messageType );
            }
        } );
    }

    /**
     * Schedules display of an ErrorDialog error message.
     *
     * <p>This method may be called on any thread.
     * @param  title  window title
     * @param  error  throwable
     * @see  uk.ac.starlink.util.gui.ErrorDialog
     */
    public void scheduleError( final String title, final Throwable error ) {
        schedule( new Runnable() {
            public void run() {
                ErrorDialog.showError( parent_, title, error );
            }
        } );
    }

    /**
     * Schedules display of an OutOfMemoryError.
     *
     * @param  error  throwable
     * @see   TopcatUtils#memoryError
     */
    public void scheduleMemoryError( final OutOfMemoryError error ) {
        schedule( new Runnable() {
            public void run() {
                TopcatUtils.memoryError( error );
            }
        } );
    }

    /**
     * Returns the component specified for this scheduler at construction time.
     * 
     * @return  parent component, may be null
     */
    public JComponent getParent() {
        return parent_;
    }
}
