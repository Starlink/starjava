package uk.ac.starlink.util;

import java.awt.Component;
import java.awt.Dimension;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Utility class for displaying an error dialog to the user.
 * <p>
 * <em><strong>Note</strong> 
 * I put this class the UTIL package since a component of this sort has 
 * obvious general use.  However, it is not especially beautifully 
 * laid out - by all means feel free to improve it -- Mark
 * </em>
 *
 * @author   Mark Taylor (Starlink)
 */
public class ErrorDialog {

    /**
     * Pops up a modal dialog to the user displaying a <tt>Throwable</tt> 
     * object.  As well as a short message, the throwable's stack trace
     * is displayed in a scrollable window.  The user has to click on 
     * a button or something to dismiss it.
     *
     * @param  th  the Throwable object to display
     * @param  message  a short text message to display in the dialog
     *         (may be <tt>null</tt>)
     * @param  parent  a parent window, used for positioning the dialog
     *         (may be null)
     */
    public static void showError( Throwable th, String message,
                                  Component parent ) {
        if ( message == null ) {
            message = th.getMessage();
        }
        String title = th.getClass().getName();

        /* Get the stack trace as a string. */
        StringWriter traceWriter = new StringWriter();
        th.printStackTrace( new PrintWriter( traceWriter ) );
        String trace = traceWriter.toString();
 
        /* Add the full stack trace in a scrollable window. */
        JTextArea ta = new JTextArea();
        ta.setLineWrap( false );
        ta.setEditable( false );
        ta.append( trace );
        ta.setCaretPosition( 0 );
        JScrollPane sp = new JScrollPane();
        sp.setMaximumSize( new Dimension( 500, 300 ) );
        sp.setPreferredSize( new Dimension( 500, 200 ) );
        sp.setViewportView( ta );

        /* Construct the stack of objects which will form the body of 
         * the window. */
        Object[] content = new Object[] { message, sp };

        /* Present the whole lot to the user in a JOptionPane. */
        JOptionPane.showMessageDialog( parent, content, title, 
                                       JOptionPane.ERROR_MESSAGE );
    }
}
