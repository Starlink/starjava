package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;
import javax.swing.text.StringContent;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.plot2.task.PlotSpec;
import uk.ac.starlink.ttools.plot2.task.StiltsPlotFormatter;
import uk.ac.starlink.ttools.plot2.task.StiltsPlot;
import uk.ac.starlink.ttools.task.LineInvoker;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Manages text display components for displaying a STILTS command
 * that is supposed to replicate the plot visible in a PlotPanel.
 * The replication may not be identical; a best effort is made.
 *
 * @author   Mark Taylor
 * @since    12 Sep 2017
 */
public class StiltsMonitor {

    private final PlotPanel<?,?> plotPanel_;
    private final JTextComponent textPanel_;
    private final Action clipboardAct_;
    private final Action errorAct_;
    private final Action executeAct_;
    private final Action[] actions_;
    private StiltsPlotFormatter formatter_;
    private PlotSpec<?,?> plotSpec_;
    private StiltsState state_;

    /**
     * Constructor.
     *
     * @param   plotPanel  panel to mirror
     */
    public StiltsMonitor( PlotPanel<?,?> plotPanel ) {
        plotPanel_ = plotPanel;
        plotSpec_ = plotPanel.getPlotSpec();

        /* Set up a text component to display the exported stilts command.
         * Note that the somewhat expensive getStiltsState method
         * is only called when the text component is actually visible,
         * which is often not the case.  This saves unnecessary work. */
        textPanel_ = new JTextPane() {
            @Override
            protected void paintComponent( Graphics g ) {
                StiltsState state = getStiltsState();
                StyledDocument doc = state.doc_;
                if ( ! doc.equals( getDocument() ) ) {
                    setDocument( doc );
                }
                super.paintComponent( g );
            }
        };
        DefaultCaret caret = new DefaultCaret();
        caret.setUpdatePolicy( DefaultCaret.NEVER_UPDATE );
        textPanel_.setCaret( caret );
        textPanel_.setEditable( false );
        textPanel_.setFont( Font.decode( "Monospaced" ) );

        /* Update the text when the plot changes. */
        plotPanel_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                plotSpec_ = plotPanel_.getPlotSpec();
                resetState();
            }
        }, true );

        /* Set up actions. */
        clipboardAct_ = new BasicAction( "Copy", null,
                                         "Copy STILTS command text "
                                       + "to the system clipboard" ) {
            public void actionPerformed( ActionEvent evt ) {
                StiltsState state = getStiltsState();
                String txt = state.text_;
                if ( txt != null ) {
                    TopcatUtils.setClipboardText( txt );
                }
            }
        };
        errorAct_ = new BasicAction( "Error", ResourceIcon.WARNING,
                                     "Show STILTS command line error" ) {
            public void actionPerformed( ActionEvent evt ) {
                Throwable err = getStiltsState().error_;
                if ( err != null ) {
                    Object src = evt.getSource();
                    Component comp = src instanceof Component
                                   ? (Component) src
                                   : null;
                    createErrorDialog( comp, err ).setVisible( true );
                }
            }
        };
        executeAct_ = new BasicAction( "Test", null,
                                       "Run the STILTS command" ) {
            final boolean useCache = true;
            public void actionPerformed( ActionEvent evt ) {
                StiltsState state = getStiltsState();
                createExecutionDialog( textPanel_, state.plot_,
                                       state.formatter_, useCache )
                   .setVisible( true );
            }
        };
        actions_ = new Action[] { clipboardAct_, executeAct_, errorAct_ };
    }

    /**
     * Sets the object that controls the details of formatting stilts
     * commands.  This can be assigned by the user to adjust formatting
     * details.
     *
     * @param  formatter  new formatter
     */
    public void setFormatter( StiltsPlotFormatter formatter ) {
        formatter_ = formatter;
        resetState();
    }

    /**
     * Returns the text panel which displays the stilts command.
     *
     * @return   text panel
     */
    public JTextComponent getTextPanel() {
        return textPanel_;
    }

    /**
     * Returns an action that copies all the current stilts command text
     * into the system clipboard.
     *
     * @return  clipboard action
     */
    public Action getClipboardAction() {
        return clipboardAct_;
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
     * Returns an action that will pop up the error resulting from
     * attempting to execute the current stilts command in a dialog window,
     * if an error exists.
     *
     * @return   error display action
     */
    public Action getErrorAction() {
        return errorAct_;
    }

    /**
     * Returns a list of actions that the GUI can present to the user
     * relating to the displayed command.
     *
     * @return  action array
     */
    public Action[] getActions() {
        return actions_;
    }

    /**
     * Returns the width of the text display panel in characters.
     * This assumes the font has a fixed width per character, which it does.
     * The returned value gives the width of the scrollpane viewport,
     * if applicable, rather than the width of the JTextComponent itself.
     * That is the best width to use when deciding where to wrap lines.
     *
     * @return   current visible text region width in units of character width
     */
    public int getWidthCharacters() {
        Container viewport =
            SwingUtilities.getAncestorOfClass( JViewport.class, textPanel_ );
        JComponent container = viewport instanceof JViewport
                             ? (JViewport) viewport
                             : textPanel_;
        int panelWidth = container.getWidth();
        FontMetrics fm = textPanel_.getFontMetrics( textPanel_.getFont() );
        double cwidth40 =
            fm.stringWidth( "123456789 123456789 123456789 123456789 " );
        return (int) Math.ceil( 40.0 * panelWidth / cwidth40 );
    }

    /**
     * Utility method for packaging the text panel returned by this
     * object's {@link #getTextPanel} method.
     * This puts it in a scroll pane, and makes sure the lines don't wrap.
     *
     * @param   textPanel  text panel, supposed to be from this monitor
     * @return   scrolled component containing panel
     */
    public static JComponent wrapTextPanel( JTextComponent textPanel ) {
        StyleContext styleContext = StyleContext.getDefaultStyleContext();
        int fontHeight =
            styleContext
           .getFontMetrics( styleContext.getFont( styleContext.getEmptySet() ) )
           .getHeight();
        JComponent noWrapPanel = new JPanel( new BorderLayout() );
        noWrapPanel.add( textPanel, BorderLayout.CENTER );
        JScrollPane scroller = new JScrollPane( noWrapPanel );
        scroller.getVerticalScrollBar().setUnitIncrement( fontHeight );
        JComponent container = new JPanel( new BorderLayout() );
        container.add( scroller, BorderLayout.CENTER );
        container.setPreferredSize( new Dimension( 600, 180 ) );
        return container;
    }

    /**
     * Called if the state that defines what appears in the text pane
     * is changed in some way.
     */
    private void resetState() {
        state_ = null;
        textPanel_.repaint();
    }

    /**
     * Returns information about STILTS command
     * for the current state of the displayed plot.
     *
     * @return  command state
     */
    private StiltsState getStiltsState() {
        if ( state_ == null ) {
            state_ = createStiltsState( plotSpec_, formatter_ );
            errorAct_.setEnabled( state_.error_ != null );
            executeAct_.setEnabled( state_.plot_ != null );
        }
        return state_;
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
            createExecutionDialog( Component parent, final StiltsPlot plot,
                                   final StiltsPlotFormatter formatter,
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
                    display = formatter.createPlotComponent( plot, useCache );
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
     * Returns a dialog window that will show an error generated by an
     * attempted STILTS plot.
     *
     * @param  parent  parent component
     * @param  error   error to show
     * @return   dialogue ready to display
     */
    private static JDialog createErrorDialog( Component parent,
                                              Throwable error ) {
        Object owner =
            SwingUtilities.getAncestorOfClass( Window.class, parent );
        Window window = owner instanceof Window ? (Window) owner : null;
        final JDialog dialog =
            new JDialog( window, "STILTS Error",
                         JDialog.ModalityType.APPLICATION_MODAL );
        JComponent controlLine = Box.createHorizontalBox();
        controlLine.add( Box.createHorizontalGlue() );
        controlLine.add( new JButton( new AbstractAction( "OK" ) {
            public void actionPerformed( ActionEvent evt ) {
                dialog.setVisible( false );
            }
        } ) );
        controlLine.add( Box.createHorizontalGlue() );
        JComponent content = new JPanel( new BorderLayout() );
        content.add( createErrorDisplay( error ),
                     BorderLayout.CENTER );
        content.add( controlLine, BorderLayout.SOUTH );
        dialog.setContentPane( content );
        dialog.pack();
        return dialog;
    }

    /**
     * Returns a component that can display the error generated by a
     * failed STILTS plot attempt.
     *
     * @param  error  exception
     * @return   display component
     */
    private static JComponent createErrorDisplay( Throwable error ) {
        JComponent panel = new JPanel( new BorderLayout() );
        panel.add( new JLabel( "STILTS plot execution error:" ),
                   BorderLayout.NORTH );
        JTextArea textPanel = new JTextArea();
        DefaultCaret caret = new DefaultCaret();
        caret.setUpdatePolicy( DefaultCaret.NEVER_UPDATE );
        textPanel.setCaret( caret );
        textPanel.setEditable( false );
        textPanel.setText( LineInvoker.getStackSummary( error ) );
        JScrollPane scroller = new JScrollPane( textPanel );
        scroller.setPreferredSize( new Dimension( 300, 100 ) );
        panel.add( scroller, BorderLayout.CENTER );
        return panel;
    }

    /**
     * Creates a StiltsState object based on a given plot spec and formatter.
     *
     * @param  plotSpec  plot specification
     * @param  formatter   command formatter
     * @return   state
     */
    private static StiltsState
            createStiltsState( PlotSpec<?,?> plotSpec,
                               StiltsPlotFormatter formatter ) {
        StiltsPlot sp;
        try {
            sp = StiltsPlot.createPlot( plotSpec, formatter );
        }
        catch ( Exception err ) {
            String errtxt = "???";
            return new StiltsState( null, formatter, errtxt, err,
                                    StiltsPlotFormatter
                                   .createBasicDocument( errtxt ) );
        }
        StyledDocument doc = formatter.createShellDocument( sp );
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
            formatter.createExecutable( sp );
            err = null;
        }
        catch ( Throwable e ) {
            err = e;
        }
        return new StiltsState( sp, formatter, txt, err, doc );
    }

    /**
     * Defines the result of trying to serialize a PlotSpec to a STILTS command.
     */
    private static class StiltsState {
        final StiltsPlot plot_;
        final StiltsPlotFormatter formatter_;
        final String text_;
        final Throwable error_;
        final StyledDocument doc_;

        /**
         * Constructor.
         *
         * @param  plot  plot spedification
         * @param  formatter   formatting object
         * @param  text  text of the STILTS command, not null
         * @param  error   error produced when trying to regenerate a
         *                 STILTS executable from the serialized command;
         *                 null if it seemed to work OK
         * @param  doc   styled document to display showing stilts command,
         *               or possibly error message
         */
        StiltsState( StiltsPlot plot, StiltsPlotFormatter formatter,
                     String text, Throwable error, StyledDocument doc ) {
            plot_ = plot;
            formatter_ = formatter;
            text_ = text;
            error_ = error;
            doc_ = doc;
        }
    }
}
