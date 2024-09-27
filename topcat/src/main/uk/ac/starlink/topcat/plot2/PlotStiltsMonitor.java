package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
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
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.StiltsMonitor;
import uk.ac.starlink.ttools.task.CommandFormatter;
import uk.ac.starlink.ttools.task.MapEnvironment;
import uk.ac.starlink.ttools.plot2.task.PlotDisplay;
import uk.ac.starlink.ttools.plot2.task.PlotSpec;
import uk.ac.starlink.ttools.plot2.task.PlotStiltsCommand;
import uk.ac.starlink.ttools.plot2.task.Suffixer;
import uk.ac.starlink.ttools.task.TableNamer;

/**
 * Manages text display components for displaying a STILTS command
 * that is supposed to replicate the plot visible in a PlotPanel.
 * The replication may not be identical; a best effort is made.
 *
 * @author   Mark Taylor
 * @since    12 Sep 2017
 */
public class PlotStiltsMonitor
        extends StiltsMonitor<PlotStiltsMonitor.PlotState> {

    private final PlotPanel<?,?> plotPanel_;
    private final Action executeAct_;
    private CommandFormatter formatter_;
    private TableNamer tableNamer_;
    private Suffixer layerSuffixer_;
    private Suffixer zoneSuffixer_;
    private PlotSpec<?,?> plotSpec_;

    /**
     * Constructor.
     *
     * @param   plotPanel  panel to mirror
     */
    public PlotStiltsMonitor( PlotPanel<?,?> plotPanel ) {
        plotPanel_ = plotPanel;
        plotSpec_ = plotPanel.getPlotSpec();


        /* Update the text when the plot changes. */
        plotPanel_.addChangeListener( evt -> {
                                          plotSpec_ = plotPanel_.getPlotSpec();
                                          resetState();
                                     }, true );

        executeAct_ = new BasicAction( "Test", null,
                                       "Run the STILTS command" ) {
            final boolean useCache = true;
            public void actionPerformed( ActionEvent evt ) {
                PlotState state = getState();
                createExecutionDialog( getTextPanel(), state.plot_,
                                       state.formatter_, useCache )
                   .setVisible( true );
            }
        };
    }

    /**
     * Sets the state that controls the details of formatting stilts commands.
     * These can be assigned by the user to adjust formatting details.
     *
     * @param  formatter   formatter
     * @param  tableNamer   table naming
     * @param  layerSuffixer  controls suffixes for layer identification
     * @param  zoneSuffixer   controls suffixes for zone identification
     */
    public void configure( CommandFormatter formatter, TableNamer tableNamer,
                           Suffixer layerSuffixer, Suffixer zoneSuffixer ) {
        formatter_ = formatter;
        tableNamer_ = tableNamer;
        layerSuffixer_ = layerSuffixer;
        zoneSuffixer_ = zoneSuffixer;
        resetState();
    }

    /**
     * Returns an action that will attempt to execute the current
     * stilts command and display the result in a dialog window.
     *
     * @return  execution action
     */
    public Action getExecuteAction() {
        return executeAct_;
    }

    /**
     * Returns a list of actions that the GUI can present to the user
     * relating to the displayed command.
     *
     * @return  action array
     */
    public Action[] getActions() {
        return new Action[] {
            getClipboardAction(), executeAct_, getErrorAction(),
        };
    }

    protected PlotState createState() {
        PlotState state = createState( plotSpec_, tableNamer_, layerSuffixer_,
                                       zoneSuffixer_, formatter_ );
        executeAct_.setEnabled( state.plot_ != null );
        return state;
    }

    /**
     * Returns a dialog window that will show the result of a stilts plot.
     *
     * @param  parent  parent component
     * @param  plot    plot specification
     * @param  formatter   formatting object
     * @param  useCache   whether output plot should use pixel caching
     * @return   dialogue ready to display
     */
    private static JDialog
            createExecutionDialog( Component parent,
                                   final PlotStiltsCommand plot,
                                   final CommandFormatter formatter,
                                   final boolean useCache ) {

        /* Set up a dialog window with a dismissal button. */
        Window owner =
            (Window) SwingUtilities.getAncestorOfClass( Window.class, parent );
        final JDialog dialog = new JDialog( owner, "STILTS Test" );
        final JComponent content = new JPanel( new BorderLayout() );
        final JComponent main = new JPanel( new BorderLayout() );
        main.setBorder( BorderFactory.createBevelBorder( BevelBorder.RAISED ) );
        content.add( main, BorderLayout.CENTER );
        dialog.setContentPane( content );
        JComponent controlLine = Box.createHorizontalBox();
        controlLine.add( Box.createHorizontalGlue() );
        controlLine.add( new JButton( new AbstractAction( "Dismiss" ) {
            public void actionPerformed( ActionEvent evt ) {
                dialog.setVisible( false );  
            }
        } ) );
        controlLine.add( Box.createHorizontalGlue() );
        controlLine.setBorder( BorderFactory
                              .createEmptyBorder( 10, 10, 10, 10 ) );
        content.add( controlLine, BorderLayout.SOUTH );

        /* Post a message indicating that an asynchronous plot
         * is taking place. */
        final JComponent waitingPanel = new JPanel( new BorderLayout() );
        JLabel msgLabel = new JLabel( "Executing..." );
        msgLabel.setHorizontalAlignment( JLabel.CENTER );
        msgLabel.setVerticalAlignment( JLabel.CENTER );
        waitingPanel.add( msgLabel, BorderLayout.CENTER );
        waitingPanel.setPreferredSize( new Dimension( 250, 150 ) );
        main.add( waitingPanel, BorderLayout.CENTER );

        /* Run the plot in a separate thread, which updates the dialogue
         * content panel when it's done. */
        Thread plotThread = new Thread( "StiltsTest" ) {
            public void run() {
                JComponent display = null;
                boolean repack;
                try {
                    display = createPlotComponent( plot, formatter, useCache );
                    repack = true;
                }
                catch ( Exception err ) {
                    display = createErrorDisplay( err );
                    repack = false;
                }
                final JComponent display0 = display;
                final boolean repack0 = repack;
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        main.removeAll();
                        main.add( display0, BorderLayout.CENTER );
                        if ( repack0 ) {
                            dialog.pack();
                        }
                        else {
                            content.revalidate();
                        }
                    }
                } );
            }
        };
        plotThread.setDaemon( true );
        plotThread.start();

        /* Prepare and return the dialog window. */
        dialog.pack();
        return dialog;
    }

    /**
     * Attempts to create a PlotDisplay that re-creates the plot
     * specified by this object.
     *
     * @param  plot   plot command
     * @param  formatter  command formatter
     * @param  caching  whether the plotted image is to be cached
     * @return  plot display component
     * @see   AbstractPlot2Task#createPlotComponent
     */
    private static PlotDisplay<?,?>
            createPlotComponent( PlotStiltsCommand plot,
                                 CommandFormatter formatter, boolean caching )
            throws TaskException, IOException, InterruptedException {
        MapEnvironment env = new MapEnvironment();
        formatter.populateEnvironment( plot, env );
        return plot.getTask().createPlotComponent( env, caching );
    }

    /**
     * Creates a PlotState object based on a given plot spec and formatter.
     *
     * @param  plotSpec  plot specification
     * @param  tableNamer  controls table naming
     * @param  layerSuffixer  controls suffixes for layers
     * @param  zoneSuffixer   controls suffixes for zones
     * @param  formatter   command formatter
     * @return   state
     */
    private static PlotState
            createState( PlotSpec<?,?> plotSpec, TableNamer tableNamer,
                         Suffixer layerSuffixer, Suffixer zoneSuffixer,
                         CommandFormatter formatter ) {
        PlotStiltsCommand plot;
        try {
            plot = PlotStiltsCommand
                  .createPlotCommand( plotSpec, tableNamer,
                                      layerSuffixer, zoneSuffixer );
        }
        catch ( Exception err ) {
            String errtxt = "???";
            return new PlotState( null, formatter, errtxt, err,
                                  CommandFormatter
                                 .createBasicDocument( errtxt ) );
        }
        StyledDocument doc = formatter.createShellDocument( plot );
        String txt;
        try {
            txt = doc.getText( 0, doc.getLength() );
        }
        catch ( BadLocationException e ) {
            assert false : e;
            txt = "???";
        }
        Throwable err;
        try {
            formatter.createExecutable( plot );
            err = null;
        }
        catch ( Throwable e ) {
            err = e;
        }
        return new PlotState( plot, formatter, txt, err, doc );
    }

    /**
     * Defines the result of trying to serialize a PlotSpec to a STILTS command.
     */
    public static class PlotState implements StiltsMonitor.State {

        private final PlotStiltsCommand plot_;
        private final CommandFormatter formatter_;
        private final String text_;
        private final Throwable error_;
        private final StyledDocument doc_;

        /**
         * Constructor.
         *
         * @param  plot  plot specification
         * @param  formatter   formatting object
         * @param  text  text of the STILTS command, not null
         * @param  error   error produced when trying to regenerate a
         *                 STILTS executable from the serialized command;
         *                 null if it seemed to work OK
         * @param  doc   styled document to display showing stilts command,
         *               or possibly error message
         */
        PlotState( PlotStiltsCommand plot, CommandFormatter formatter,
                   String text, Throwable error, StyledDocument doc ) {
            plot_ = plot;
            formatter_ = formatter;
            text_ = text;
            error_ = error;
            doc_ = doc;
        }

        public String getText() {
            return text_;
        }

        public StyledDocument getDocument() {
            return doc_;
        }

        public Throwable getError() {
            return error_;
        }
    }
}
