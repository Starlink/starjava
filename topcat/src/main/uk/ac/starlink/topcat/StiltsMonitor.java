package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import uk.ac.starlink.ttools.task.LineInvoker;
import uk.ac.starlink.ttools.task.TableNamer;

/**
 * Manages a text display component for showing a STILTS comand.
 *
 * @author   Mark Taylor
 * @since    27 Sep 2024
 */
public abstract class StiltsMonitor<S extends StiltsMonitor.State> {

    private final JTextComponent textPanel_;
    private final Action clipboardAct_;
    private final Action errorAct_;
    private S state_;

    /**
     * Constructor.
     */
    protected StiltsMonitor() {

        /* Set up a text component to display the stilts command.
         * Note that the potentially expensive getStiltsState method
         * is only called when the text component is actually visible,
         * which is often not the case.  This saves unnecessary work. */
        textPanel_ = new JTextPane() {
            @Override
            protected void paintComponent( Graphics g ) {
                S state = getState();
                StyledDocument doc = state.getDocument();
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

        /* Set up actions. */
        clipboardAct_ =
            BasicAction
           .create( "Copy", ResourceIcon.CLIPBOARD,
                    "Copy STILTS command text to the system clipboard",
                    evt -> {
                        String txt = getState().getText();
                        if ( txt != null ) {
                            TopcatUtils.setClipboardText( txt );
                        }
                    } );
        errorAct_ =
            BasicAction
           .create( "Error", ResourceIcon.WARNING,
                    "Show STILTS command line error",
                    evt -> {
                        Throwable err = getState().getError();
                        if ( err != null ) {
                            Object src = evt.getSource();
                            Component comp = src instanceof Component
                                           ? (Component) src
                                           : null;
                            createErrorDialog( comp, err ).setVisible( true );
                        }
                    } );
    }

    /**
     * Returns a state object describing the content that should be
     * displayed in this monitor.
     *
     * @return  new state
     */
    protected abstract S createState();

    /**
     * Returns the current state of this monitor.
     * A new state is lazily created if no current state is present.
     *
     * @return  current state
     */
    public S getState() {
        if ( state_ == null ) {
            state_ = createState();
            errorAct_.setEnabled( state_.getError() != null );
        }
        return state_;
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
     * Called if the state that defines what appears in the text pane
     * is changed in some way.
     */
    protected void resetState() {
        state_ = null;
        textPanel_.repaint();
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
     * Returns a component that can display an error.
     *
     * @param  error  exception
     * @return   display component
     */
    public static JComponent createErrorDisplay( Throwable error ) {
        JComponent panel = new JPanel( new BorderLayout() );
        panel.add( new JLabel( "STILTS command error: " ), BorderLayout.NORTH );
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
        content.add( createErrorDisplay( error ), BorderLayout.CENTER );
        content.add( controlLine, BorderLayout.SOUTH );
        dialog.setContentPane( content );
        dialog.pack();
        return dialog;
    }

    /**
     * Represents a STILTS command.
     */
    public interface State {

        /**
         * Returns text of the command suitable for execution in a shell.
         *
         * @return  command text, not null
         */
        String getText();

        /**
         * Returns a styled document displaying the stilts command,
         * or possibly an error message.
         *
         * @return  styled document, not null
         */
        StyledDocument getDocument();

        /**
         * Returns the error produced when trying to generate a stilts
         * executable from the serialized commmand, or null if it
         * seemed to work OK.
         *
         * @return  throwable, or null
         */
        Throwable getError();
    }
}
