package uk.ac.starlink.topcat.join;

import java.awt.Component;
import java.io.IOException;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.ProgressIndicator;
import uk.ac.starlink.util.ErrorDialog;

/**
 * Abstract class defining general characteristics of a component which
 * can perform some sort of matching action and present itself graphically.
 *
 * @author   Mark Taylor (Starlink)
 * @since    18 Mar 2004
 */
public abstract class MatchSpec extends JPanel {

    public final static ValueInfo MATCHTYPE_INFO =
        new DefaultValueInfo( "Match type", String.class,
                              "Type of match which created this table" );
    public final static ValueInfo ENGINE_INFO =
        new DefaultValueInfo( "Match algorithm", String.class,
                              "Matching algorithm which created this table" );

    /**
     * Performs the match calculation.  
     * If it's the kind of match operation which constructs a new table,
     * that table should be returned.  If not, a <tt>null</tt> return
     * is considered a success.
     * This method is called from a
     * thread other than the event dispatch thread, so it can take its
     * time, and must not call Swing things.
     *
     * @param   indicator  a progress indicator which the calculation
     *          should try to update
     * @return  a StarTable created by the match if appropriate
     * @throws  IOException  if there's some trouble
     * @throws  InterruptedException  if the user interrupts the calculation
     */
    public abstract void calculate( ProgressIndicator indicator ) 
            throws IOException, InterruptedException;

    /**
     * This method is called from the event dispatch thread if the
     * calculation terminates normally.
     *
     * @param  parent  window controlling the invocation
     */
    public abstract void matchSuccess( Component parent );

    /**
     * Invoked from the event dispatch thread before {@link #calculate}
     * is called.  A check should be made that it is sensible to call
     * calculate; if not an exception should be thrown.
     *
     * @param  parent  window controlling the invocation
     * @throws IllegalStateException (with a message suitable for presentation
     *         to the user) if <tt>calculate</tt> cannot be called in the
     *         current state
     */
    public abstract void checkArguments();

    /**
     * This method is called from the event dispatch thread if the 
     * calculation terminates with an exception.
     *
     * @param  th  exception characterising the failure
     * @param  parent  window controlling the invocation
     */
    public void matchFailure( Throwable th, Component parent ) {
        ErrorDialog.showError( th, "Match failed", parent );
    }

    /**
     * Returns a graphical component which can be presented to the user
     * representing the match to be carried out.  The user may interact
     * with this to modify the match characteristics.
     *
     * @return  graphical component
     */
    public JComponent getPanel() {
        return this;
    }
}
