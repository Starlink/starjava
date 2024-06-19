package uk.ac.starlink.topcat.join;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.ProgressIndicator;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.util.IconUtils;
import uk.ac.starlink.util.gui.ErrorDialog;

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
     * This method is called from a
     * thread other than the event dispatch thread, so it can take its
     * time, and must not call Swing things.
     *
     * @param   indicator  a progress indicator which the calculation
     *          should try to update
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
     * @throws IllegalStateException (with a message suitable for presentation
     *         to the user) if <code>calculate</code> cannot be called in the
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
        if ( th instanceof OutOfMemoryError ) {
            TopcatUtils.memoryError( (OutOfMemoryError) th );
        }
        else {
            ErrorDialog.showError( parent, "Match Error", th );
        }
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

    /**
     * Returns a standard set of FixActions for a given number of tables.
     *
     * @param  nTable   number of tables
     * @return  <code>nTable</code>-element array of sensible FixActions
     */
    public JoinFixAction[] getDefaultFixActions( int nTable ) {
        JoinFixAction[] fixActs = new JoinFixAction[ nTable ];
        for ( int i = 0; i < nTable; i++ ) {
            fixActs[ i ] = JoinFixAction
                          .makeRenameDuplicatesAction( "_" + ( i + 1 ) );
        }
        return fixActs;
    }

    /**
     * Displays a modal dialogue containing a message, and possibly the
     * option to plot the match result.
     *
     * @param  parent   parent component
     * @param  lines    lines of text summarising the result
     * @param  plotAct  an action which can generate a plot of the match result
     */
    public static void showSuccessMessage( Component parent, String[] lines,
                                           final Action plotAct ) {

        /* Create a dialogue window to contain the components. */
        Frame fparent = parent == null
                      ? null
                      : (Frame) SwingUtilities
                               .getAncestorOfClass( Frame.class, parent );
        final JDialog dialog = new JDialog( fparent, "Successful Match", true );
        JComponent panel = (JComponent) dialog.getContentPane();
        panel.setLayout( new BorderLayout() );
        panel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );

        /* Add an info icon like JOptionPane does. */
        JLabel iconLabel =
            new JLabel( UIManager.getIcon( "OptionPane.informationIcon" ) );
        iconLabel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        panel.add( iconLabel, BorderLayout.WEST );

        /* Add the lines of text. */
        JComponent txtBox = Box.createVerticalBox();
        for ( int il = 0; il < lines.length; il++ ) {
            txtBox.add( new JLabel( lines[ il ] ) );
        } 
        txtBox.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        panel.add( txtBox, BorderLayout.CENTER );

        /* Prepare a button that just dismisses the dialogue. */
        Action dismissAct = new AbstractAction( "OK" ) {
            public void actionPerformed( ActionEvent evt ) {
                dialog.dispose();
            }
        };
        dismissAct.putValue( Action.SMALL_ICON, IconUtils.emptyIcon( 0, 24 ) );
        JButton dismissButt = new JButton( dismissAct );

        /* Prepare a button that performs a plot.  Decorate the supplied
         * action to dispose of the dialogue as well as doing the actual
         * plot. */
        JButton plotButt;
        if ( plotAct != null ) {
            Action pAct = new AbstractAction() {
                public void actionPerformed( ActionEvent evt ) {
                    plotAct.actionPerformed( evt );
                    dialog.dispose();
                }
            };
            String[] keys =
                { Action.NAME, Action.SMALL_ICON, Action.LONG_DESCRIPTION };
            for ( int ik = 0; ik < keys.length; ik++ ) {
                String key = keys[ ik ];
                pAct.putValue( key, plotAct.getValue( key ) );
            }
            pAct.setEnabled( plotAct.isEnabled() );
            plotButt = new JButton( pAct );
        }
        else {
            plotButt = null;
        }

        /* Place the button(s). */
        JComponent actBox = Box.createHorizontalBox();
        actBox.add( Box.createHorizontalGlue() );
        actBox.add( dismissButt );
        if ( plotButt != null ) {
            actBox.add( Box.createHorizontalStrut( 10 ) );
            actBox.add( plotButt );
        }
        actBox.add( Box.createHorizontalGlue() );
        actBox.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        panel.add( actBox, BorderLayout.SOUTH );

        /* Show the dialogue. */
        dialog.pack();
        if ( plotButt != null && plotButt.isEnabled() ) {
            plotButt.requestFocusInWindow();
        }
        dialog.setLocationRelativeTo( parent );
        dialog.setVisible( true );
    }
}
