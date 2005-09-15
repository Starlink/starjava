package uk.ac.starlink.splat.util;

import java.lang.Exception;
import java.awt.Component;
import javax.swing.JOptionPane;

/**
 *  General class for all exceptions thrown in SPLAT.
 */
public class SplatException 
    extends Exception 
{
    public SplatException() 
    {
         super();
    }
    
    public SplatException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public SplatException( Throwable cause )
    {
        super( cause );
    }

    public SplatException ( String message ) 
    {
        super( message );
    }

    /**
     *  Throw an exception and display a message in a MessageDialog of
     *  a JOptionPane.
     *
     *  @param parentComponent Determines the Frame in which the
     *                         dialog is displayed. If null, or if the
     *                         parentComponent has no Frame, a default
     *                         Frame is used.
     *  @param message The error message to be displayed and also
     *                 forms the message part of the exception.
     *  @param title Title for the dialog window.
     *  @param messageType JOptionPage message type, should be one of
     *                     ERROR_MESSAGE, INFORMATION_MESSAGE,
     *                     WARNING_MESSAGE, QUESTION_MESSAGE, or PLAIN_MESSAGE.
     *
     */
    public SplatException( Component parentComponent, String message,
                           String title, int messageType ) 
    {
        super( message );
        JOptionPane.showMessageDialog( parentComponent, message,
                                       title, messageType );
    }
}
