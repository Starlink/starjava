package uk.ac.starlink.table.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Handles asynchronous table saving.
 * To save a table, create an instance of this class with a suitable 
 * implementation of the {@link #attemptSave} method and call 
 * {@link #invoke} on it from the event dispatch thread.
 * A progress bar will advise the user on how
 * the save is going, and if the save fails a popup will advise of the
 * error.  When the save has completed one way or another, the
 * {@link #done} method will be called.
 *
 * @author   Mark Taylor (Starlink)
 * @since    24 Feb 2005
 */
public abstract class SaveWorker {

    private final String location_;
    private final ProgressBarStarTable progTable_;
    private final JProgressBar progBar_;
    private final JDialog progPopup_;
    private Thread worker_;

    /**
     * Constructs a save worker which will use its own popup widow for
     * progress display.
     *
     * @param  parent   parent component used for progress bar popup
     * @param  table  table to be saved
     * @param  location  string identifying the save destination - used for
     *         display purposes only
     */
    protected SaveWorker( Component parent, StarTable table, String location ) {
        progBar_ = new JProgressBar();
        progTable_ = new ProgressBarStarTable( table, progBar_ );
        location_ = location;
        progPopup_ = new JDialog( getFrame( parent ), "Saving...", true );
        progPopup_.setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
        progPopup_.setContentPane( createProgressPanel( progBar_, 
                                                        table.getName(),
                                                        location ) );
        progPopup_.pack();
        progPopup_.setLocationRelativeTo( parent );
    }

    /**
     * Constructs a save worker which will use a given progress bar for
     * progress display.
     *
     * @param  progBar  progress bar which will be updated to display progress
     * @param  table    table to be saved
     * @param  location  string identifying the save destination - used for
     *                   display purposes only
     */
    protected SaveWorker( JProgressBar progBar, StarTable table,
                          String location ) {
        progBar_ = progBar;
        progTable_ = new ProgressBarStarTable( table, progBar_ );
        location_ = location;
        progPopup_ = null;
    }

    /**
     * This method should do the work of saving the given table.
     * It will not be called on the event dispatch thread, so may 
     * take some time to execute.
     *
     * @param   table  table to save
     * @throws  IOException   if the table cannot be saved
     */
    protected abstract void attemptSave( StarTable table ) throws IOException;

    /**
     * Called from the event dispatch thread when the save has completed.
     * The <tt>success</tt> argument indicates whether the table was
     * saved successfully or not.  If it was not, the user will already
     * have been informed of this.
     *
     * <p>The default implementation does nothing, but subclasses may 
     * override it to react in some way to the save's completion.
     *
     * @return   success  save status
     */
    protected abstract void done( boolean success );

    /**
     * Interrupts any save which is in progress.  Call from the event
     * dispatch thread.
     */
    public void cancel() {
        if ( worker_ != null ) {
            worker_.interrupt();
            worker_ = null;
        }
    }

    /**
     * Begins the save.  Should be invoked from the event dispatch thread.
     */
    public void invoke() {
        worker_ = new Thread( "Saver thread" ) {
            Throwable error;
            public void run() {
                try {
                    attemptSave( progTable_ );
                    error = null;
                }
                catch ( IOException e ) {
                    error = e;
                }
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        if ( worker_ != null ) {
                            worker_ = null;
                            if ( error == null ) {
                                saveSucceeded();
                                done( true );
                            }
                            else {
                                saveFailed( error );
                                done( false );
                            }
                        }
                    }
                } );
            }
        };
        worker_.start();
        saveStarted();
    }

    /**
     * Called from the event dispatch thread when the table save starts.
     */
    private void saveStarted() {
        if ( progPopup_ != null ) {
            progPopup_.show();
        }
    }

    /**
     * Called from the event dispatch thread if the table save completes
     * successfully.
     */
    private void saveSucceeded() {
        if ( progPopup_ != null ) {
            progPopup_.dispose();
        }
    }

    /**
     * Called from the event dispatch thread if the table save fails.
     *
     * @parm   error   error
     */
    private void saveFailed( Throwable error ) {
        String[] msg = new String[] {
            "Error saving " + progTable_.getName(),
            "to " + location_,
        };
        ErrorDialog.showError( progPopup_, "Save Error", error, msg );
        progPopup_.dispose();
    }

    /**
     * Constructs a panel which contains a progress bar and some
     * suitable text.
     *
     * @param  progBar  progress bar to use
     * @param  name     name of table being saved
     * @param  dest     destination string for table to be saved
     * @return   panel containing progress bar
     */
    private JComponent createProgressPanel( JProgressBar progBar,
                                            String name, String dest ) {
        JComponent main = new JPanel( new BorderLayout() );
        Border gapBorder = BorderFactory.createEmptyBorder( 5, 5, 5, 5 );

        Action cancelAction = new AbstractAction( "Cancel" ) {
            public void actionPerformed( ActionEvent evt ) {
                cancel();
            }
        };

        JComponent iconBox = new JPanel();
        iconBox.setBorder( gapBorder );

        iconBox.add( new JLabel( UIManager
                                .getIcon( "OptionPane.informationIcon" ) ) );
        main.add( iconBox, BorderLayout.WEST );

        JComponent midBox = Box.createVerticalBox();
        midBox.setBorder( gapBorder );
        midBox.add( new JLabel( "Saving table " + name ) );
        midBox.add( new JLabel( "to " + dest ) );
        midBox.add( Box.createVerticalStrut( 5 ) );
        midBox.add( progBar );
        main.add( midBox, BorderLayout.CENTER );

        JComponent controlBox = Box.createHorizontalBox();
        controlBox.setBorder( gapBorder );
        controlBox.add( Box.createHorizontalGlue() );
        controlBox.add( new JButton( cancelAction ) );
        controlBox.add( Box.createHorizontalGlue() );
        main.add( controlBox, BorderLayout.SOUTH );

        return main;
    }

    /**
     * Works out the Frame owner of a component.
     * 
     * @param  parent  component
     * @return  ancestor which is a Frame, if any
     */
    private static Frame getFrame( Component parent ) {
        if ( parent == null ) {
            return null;
        }
        else if ( parent instanceof Frame ) {
            return (Frame) parent;
        }
        else {
            return (Frame) SwingUtilities
                          .getAncestorOfClass( Frame.class, parent );
        }
    }

}
