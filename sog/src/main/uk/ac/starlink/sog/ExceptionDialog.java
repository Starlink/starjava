package uk.ac.starlink.sog;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
 
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Creates an error dialog that also displays the full stack trace of
 * an exception.
 */
public class ExceptionDialog
{
    // UI components.
    private JPanel messagesArea = null;
    private JLabel message = null;
    private JTextArea textArea = null;
    private JButton details = null;
    private JScrollPane scrollPane = null;
    private JPanel dummyPanel = null;

    /**
     * Create an error dialog that initially displays the message of
     * the given exception, but which will also display the full text
     * of the exception stack trace.
     * 
     * @param the a parent for this window, can be null.
     * @param e the exception to report and maybe display.
     */
    public ExceptionDialog( Component parent, Exception e )
    {
        //  Initialize the interface and extract information to make
        //  the various reports.
        initUI();
        makeReports( e );

        //  Create a suitable JOptionPane wth our messagesArea and two
        //  buttons, one for "OK" and the other (usually NO) for the
        //  "Show details" option.
        Object[] options = { "OK", "Show details" };
        final JOptionPane pane =  new JOptionPane( messagesArea,
                                                   JOptionPane.ERROR_MESSAGE,
                                                   JOptionPane.YES_NO_OPTION,
                                                   null, options, null );
        final JDialog dialog = pane.createDialog( parent, "Error" );

        //  Allow resizing so that more of the stacktrace can be
        //  revealed.
        dialog.setResizable( true );

        //  Normally the control buttons will both close the window,
        //  but for the show details option we want to keep it
        //  open. Hence all this effort...
        pane.addPropertyChangeListener( new PropertyChangeListener() 
        {
            public void propertyChange( PropertyChangeEvent e ) 
            {
                String prop = e.getPropertyName();
                if ( e.getSource() == pane &&
                     ( prop.equals( pane.VALUE_PROPERTY ) ||
                       prop.equals( pane.INPUT_VALUE_PROPERTY ) ) ) 
                {
                    Object selected = e.getNewValue();
                    if ( selected instanceof String ) {
                        if ( ((String)selected).equals( "OK" ) ) {
                            dialog.dispose();
                        }
                        else {
                            showDetails();
                            dialog.setVisible( true );
                        }
                    } 
                    else {
                        showDetails();
                        dialog.setVisible( true );
                    }
                }
            }
        });

        //  Display the dialog and wait for it to go away.
        dialog.show();
        dialog.dispose();
    }

    /**
     * Create our extra UI components.
     */
    protected void initUI()
    {
        // Container for all messages.
        messagesArea = new JPanel( new BorderLayout() );

        //  Main message. May be all that is needed.
        message = new JLabel();

        //  Component for text of whole of stack trace. Not shown
        //  until "Show details" is requested.
        textArea = new JTextArea( 4, 40 );
        textArea.setEditable( false );
        scrollPane = new JScrollPane( textArea );

        // Dummy component to reserve space for stack trace.
        dummyPanel = new JPanel();
        dummyPanel.setPreferredSize( new Dimension( 100, 50 ) );

        messagesArea.add( message, BorderLayout.NORTH );
        messagesArea.add( dummyPanel, BorderLayout.CENTER );
    }

    /**
     * Write the full exception and message to the report.
     */
    protected void makeReports( Exception e )
    {
        message.setText( e.getMessage() );
        textArea.setText( e.getMessage() + "\n" );
        StackTraceElement[] elements = e.getStackTrace();
        for ( int i = 0; i < elements.length; i++ ) {
            textArea.append( elements[i].toString() + "\n" );
        }
        textArea.setCaretPosition( 0 );
    }

    /**
     * Show the details of the exception.
     */
    protected void showDetails()
    {
        messagesArea.remove( dummyPanel );
        messagesArea.add( scrollPane, BorderLayout.CENTER );
    }
    
    public static void main( String[] args )
    {
        Exception e = new Exception( "An exception" );
        ExceptionDialog exceptionDialog = new ExceptionDialog( null, e );
    }
}
