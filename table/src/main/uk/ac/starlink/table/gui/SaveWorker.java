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
    private final ProgressBarStarTable[] progTables_;
    private final JProgressBar progBar_;
    private final JDialog progPopup_;
    private Thread worker_;

    /**
     * Constructs a save worker which will use its own popup widow for
     * progress display.
     *
     * @param  parent   parent component used for progress bar popup
     * @param  tables  tables to be saved
     * @param  location  string identifying the save destination - used for
     *         display purposes only
     */
    protected SaveWorker( Component parent, StarTable[] tables,
                          String location ) {
        progBar_ = new JProgressBar();
        progTables_ = toProgTables( tables, progBar_ );
        location_ = location;
        progPopup_ = new JDialog( getFrame( parent ), "Saving...", true );
        progPopup_.setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
        String txt = "Saving "
                 + ( tables.length == 1 ? "table" : "tables" )
                 + " to " + location;
        progPopup_.setContentPane( createProgressPanel( progBar_, txt ) );
        progPopup_.pack();
        progPopup_.setLocationRelativeTo( parent );
    }

    /**
     * Constructs a save worker which will use a given progress bar for
     * progress display.
     *
     * @param  progBar  progress bar which will be updated to display progress
     * @param  tables    tables to be saved
     * @param  location  string identifying the save destination - used for
     *                   display purposes only
     */
    protected SaveWorker( JProgressBar progBar, StarTable[] tables,
                          String location ) {
        progBar_ = progBar;
        progTables_ = toProgTables( tables, progBar_ );
        location_ = location;
        progPopup_ = null;
    }

    /**
     * This method should do the work of saving the given tables.
     * It will not be called on the event dispatch thread, so may 
     * take some time to execute.
     *
     * @param   tables  tables to save
     * @throws  IOException   if the table cannot be saved
     */
    protected abstract void attemptSave( StarTable[] tables )
            throws IOException;

    /**
     * Called from the event dispatch thread when the save has completed.
     * The <code>success</code> argument indicates whether the table was
     * saved successfully or not.  If it was not, the user will already
     * have been informed of this.
     *
     * <p>The default implementation does nothing, but subclasses may 
     * override it to react in some way to the save's completion.
     *
     * @param   success  save status
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
        if ( progPopup_ != null ) {
            progPopup_.dispose();
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
                    attemptSave( progTables_ );
                    error = null;
                }
                catch ( Throwable e ) {
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
            progPopup_.setVisible( true );
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
            "Error saving table" + ( progTables_.length == 1 ? "" : "s" ),
            "to " + location_,
        };
        ErrorDialog.showError( progPopup_, "Save Error", error, msg );
        if ( progPopup_ != null ) {
            progPopup_.dispose();
        }
    }

    /**
     * Constructs a panel which contains a progress bar and some
     * suitable text.
     *
     * @param  progBar  progress bar to use
     * @param  txt   text for presentation to the user
     * @return   panel containing progress bar
     */
    private JComponent createProgressPanel( JProgressBar progBar, String txt ) {
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
        midBox.add( new JLabel( txt ) );
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

    /**
     * Converts an array of undecorated tables to ProgressBarStarTables.
     *
     * @param  tables  input tables
     * @param  progBar  progress bar whose progress table reading will affect
     * @return  array of tables, one for each element of input array
     */
    private static ProgressBarStarTable[] toProgTables( StarTable[] tables,
                                                        JProgressBar progBar ) {
        int nTable = tables.length;
        ProgressBarStarTable[] progTables = new ProgressBarStarTable[ nTable ];
        for ( int i = 0; i < nTable; i++ ) {
            progTables[ i ] = new ProgressBarStarTable( tables[ i ], progBar );
            if ( nTable > 1 ) {
                progTables[ i ].setActiveLabel( ( i + 1 ) + "/" + nTable );
            }
        }
        return progTables;
    }
}
