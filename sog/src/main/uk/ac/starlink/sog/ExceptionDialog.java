package uk.ac.starlink.sog;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;
import javax.swing.JDialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;

/**
 * Creates an error dialog that also displays the full stack trace of
 * an exception.
 */
public class ExceptionDialog
{
    private JTextArea textArea = null;
    private JScrollPane scrollPane = null;

    public ExceptionDialog( Component parent, Exception e )
    {
        makeTextRegion();
        fillTextRegion( e );

        // Create the dialog, need an none static JOptionPane one so
        // we can resize it.
        JOptionPane pane =  new JOptionPane( scrollPane, 
                                             JOptionPane.ERROR_MESSAGE,
                                             JOptionPane.DEFAULT_OPTION, 
                                             null, null, null );
        JDialog dialog = pane.createDialog( parent, "Error" );
        dialog.setResizable( true );
        dialog.show();
        dialog.dispose();
    }

    /**
     * Make a component (JScrollPane plus TextArea) for displaying the
     * text.
     */
    protected void makeTextRegion()
    {
        textArea = new JTextArea( 4, 40 );
        textArea.setEditable( false );
        scrollPane = new JScrollPane( textArea );
    }
    
    /**
     * Write the full exception to the JTextArea.
     */
    protected void fillTextRegion( Exception e )
    {
        textArea.append( e.toString() + "\n" );
        StackTraceElement[] elements = e.getStackTrace();
        for ( int i = 0; i < elements.length; i++ ) {
            textArea.append( elements[i].toString() + "\n" );
        }
        textArea.setCaretPosition( 0 );
    }


    public static void main( String[] args ) 
    {
        Exception e = new Exception( "An exception" );
        ExceptionDialog exceptionDialog = new ExceptionDialog( null, e );
    }
}
