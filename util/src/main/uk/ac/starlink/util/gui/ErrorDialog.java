package uk.ac.starlink.util.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 * Utility class for displaying an error dialogue to the user.
 * Calling the static <code>showError</code> method pops up a modal dialogue
 * which informs the user what went wrong.  As initially displayed this
 * just contains the message or name of the exception plus optionally
 * some additional lines of text, but a button is provided which
 * can display the full stack trace.
 *
 * @author   Mark Taylor (Starlink)
 * @since    23 Feb 2005
 */
public class ErrorDialog extends JDialog {

    private final JComponent holder_;
    private final String[] message_;
    private final Throwable error_;
    private final String title_;
    private JComponent summaryPanel_;
    private JComponent detailPanel_;

    /**
     * Constructor.
     *
     * @param   frame  frame which will own the dialogue
     * @param   error  throwable to be displayed
     * @param   title  dialogue window title
     * @param   message  additional text, one line per element
     */
    public ErrorDialog( Frame frame, Throwable error, String title,
                        String[] message ) {
        super( frame, title == null ? "Error" : title, true );
        error_ = error;
        title_ = title;
        message_ = message == null ? new String[ 0 ] : message;
        setDefaultCloseOperation( DISPOSE_ON_CLOSE );
        Border gapBorder = BorderFactory.createEmptyBorder( 5, 5, 5, 5 );

        /* Action for disposing of the dialogue. */
        Action disposeAction = new AbstractAction( "OK" ) {
            public void actionPerformed( ActionEvent evt ) {
                dispose();
            }
        };

        /* Place an error icon. */
        Container main = getContentPane();
        holder_ = new JPanel( new CardLayout() );
        JLabel iconLabel = 
            new JLabel( UIManager.getIcon( "OptionPane.errorIcon" ) );
        JComponent iconPanel = new JPanel( new BorderLayout() );
        iconPanel.add( iconLabel );
        iconPanel.setBorder( gapBorder );
        main.add( iconPanel, BorderLayout.WEST );

        /* Create and place the component which will hold the main display. */
        JComponent holderBox = new JPanel( new BorderLayout() );
        holderBox.setBorder( gapBorder );
        holderBox.add( holder_, BorderLayout.CENTER );
        holderBox.add( Box.createHorizontalStrut( 256 ), BorderLayout.SOUTH );
        holderBox.add( Box.createVerticalStrut( 64 ), BorderLayout.WEST );
        main.add( holderBox, BorderLayout.CENTER );

        /* Create and place the buttons. */
        JComponent buttonBox = Box.createHorizontalBox();
        JButton detailButton = new JButton( new DetailAction() );
        buttonBox.add( Box.createHorizontalGlue() );
        buttonBox.add( new JButton( disposeAction ) );
        buttonBox.add( Box.createHorizontalStrut( 10 ) );
        buttonBox.add( new JButton( new DetailAction() ) );
        buttonBox.add( Box.createHorizontalGlue() );
        buttonBox.setBorder( gapBorder );
        main.add( buttonBox, BorderLayout.SOUTH );
    }

    /**
     * Returns a component which contains a summary of the error.
     */
    private JComponent getSummaryPanel() {
        if ( summaryPanel_ == null ) {

            /* Assemble the text to display. */
            List<String> lines =
                new ArrayList<String>( Arrays.asList( message_ ) );
            String errmsg = error_.getMessage();
            if ( errmsg != null ) {
                String[] msgLines = errmsg.split( "\n" );
                for ( int i = 0; i < Math.min( msgLines.length, 6 ); i++ ) {
                    String line = msgLines[ i ];
                    if ( line != null && line.length() > 160 ) {
                        line = line.substring( 0, 160 ) + "...";
                    }
                    lines.add( line );
                }
                if ( msgLines.length > 6 ) {
                    lines.add( "..." );
                }
            }
            if ( lines.size() == 0 ) {
                lines.add( title_ == null ? error_.getClass().getName()
                                          : title_ );
            }

            /* Put it into a box. */
            JComponent lineBox = Box.createVerticalBox();
            lineBox.add( Box.createVerticalGlue() );
            for ( String line : lines ) {
                lineBox.add( new JLabel( line, SwingConstants.LEFT ) );
            }
            lineBox.add( Box.createVerticalGlue() );
            summaryPanel_ = new JPanel( new BorderLayout() ); 
            summaryPanel_.add( lineBox );
        }
        return summaryPanel_;
    }

    /**
     * Returns a component which contains the full stack trace.
     */
    private JComponent getDetailPanel() {
        if ( detailPanel_ == null ) {

            /* Get the stack trace as a string. */
            StringWriter traceWriter = new StringWriter();
            error_.printStackTrace( new PrintWriter( traceWriter ) );
            String trace = traceWriter.toString();

            /* Add the trace to a scrollable text window. */
            JTextArea ta = new JTextArea();
            ta.setLineWrap( false );
            ta.setEditable( false );
            ta.append( trace );
            ta.setCaretPosition( 0 );
            JScrollPane scroller = new JScrollPane( ta );
            detailPanel_ = scroller;
        }
        return detailPanel_;
    }

    /**
     * Action which toggles display of the detailed error information.
     */
    private class DetailAction extends AbstractAction {

        private boolean detail_;

        DetailAction() {
            setShowDetail( false );
            pack();
        }

        public void actionPerformed( ActionEvent evt ) {
            setShowDetail( ! detail_ );
            Rectangle oldBounds = getBounds();
            int cx = oldBounds.x + oldBounds.width / 2;
            int cy = oldBounds.y + oldBounds.height / 2;
            Rectangle newBounds = getBounds();
            setLocation( new Point( cx - newBounds.width / 2,
                                    cy - newBounds.height / 2 ) );
            
        }

        void setShowDetail( boolean detail ) {
            if ( detail ) {
                if ( detailPanel_ == null ) {
                    holder_.add( getDetailPanel(), "DETAIL" );
                    Dimension size = holder_.getPreferredSize();
                    size.height = Math.min( size.height, 300 );
                    size.width = Math.min( size.width, 500 );
                    holder_.setPreferredSize( size );
                    pack();
                }
                ((CardLayout) holder_.getLayout()).show( holder_, "DETAIL" );
                putValue( NAME, "Hide Details" );
            }
            else {
                if ( summaryPanel_ == null ) {
                    holder_.add( getSummaryPanel(), "SUMMARY" );
                }
                ((CardLayout) holder_.getLayout()).show( holder_, "SUMMARY" );
                putValue( NAME, "Show Details" );
            }
            detail_ = detail;
        }
    }

    /**
     * Pops up a modal dialogue displaying information about an error
     * with an additional multi-line message.
     *
     * @param  parent  parent component
     * @param  title   title of the dialogue window
     * @param  error   the throwable to be displayed
     * @param  message an array of one or more strings providing additional
     *                 information about the error.  Each string is displayed
     *                 as a line.
     */
    public static void showError( Component parent, String title, 
                                  Throwable error, String[] message ) {

        /* Get the Frame ancestor of the parent component. */
        Frame fparent = parent == null
                      ? null
                      : (Frame) SwingUtilities
                               .getAncestorOfClass( Frame.class, parent );

        /* Construct and prepare the error dialogue. */
        JDialog dialog = new ErrorDialog( fparent, error, title, message );
        dialog.setLocationRelativeTo( parent );
        dialog.pack();

        /* Display the modal dialogue. */
        dialog.setVisible( true );
    }

    /**
     * Pops up a modal dialogue displaying information about an error
     * with an additional single-line message.
     *
     * @param  parent  parent component
     * @param  title   title of the dialogue window
     * @param  error   the throwable to be displayed
     * @param  message one-line message text
     */
    public static void showError( Component parent, String title, 
                                  Throwable error, String message ) {
        showError( parent, title, error, new String[] { message } );
    }

    /**
     * Pops up a modal dialogue displaying information about an error.
     *
     * @param  parent  parent component
     * @param  title   title of the dialogue window
     * @param  error   the throwable to be displayed
     */
    public static void showError( Component parent, String title,
                                  Throwable error ) {
        showError( parent, title, error, new String[ 0 ] );
    }

    /**
     * Pops up a modal dialogue displaying information about an error.
     *
     * @param  parent  parent component
     * @param  error   the throwable to be displayed
     */
    public static void showError( Component parent, Throwable error ) {
        showError( parent, "Error", error, new String[ 0 ] );
    }

    public static void main( String[] args ) {
        showError( null, "Error", new Error( "Some trouble" ), args );
        System.exit( 0 );
    }

}
