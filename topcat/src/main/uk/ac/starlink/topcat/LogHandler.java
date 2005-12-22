package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.MemoryHandler;
import java.util.logging.SimpleFormatter;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * Log handler which can provide a window displaying recent log events.
 * Once the window has been displayed, maintaining it may become 
 * expensive as the list of events grows, but if it's never displayed
 * no great expense should be incurred (just maintenance of a 
 * {@link java.util.logging.MemoryHandler}).
 *
 * <p>Since logging is necessarily a system-wide matter, this is a 
 * singleton class.
 *
 * @author   Mark Taylor (Starlink)
 * @since    17 Jun 2005
 */
public class LogHandler extends MemoryHandler {

    /*
     * The implementation of this class is something of a mess for two reasons:
     *   1.  The logging API leaves something to be desired (for instance
     *       there's no way to get the target handler from a MemoryHandler)
     *   2.  This implementation is very basic - many more features ought
     *       to be present like better (coloured) formatting, user 
     *       selection of log message display level etc.  To do it properly
     *       you should probably rewrite.
     *   3.  Done in a hurry just prior to the demise of Starlink
     * The (small) public interface is OK I think though.
     */

    private final DocHandler docHandler_;
    private Document doc_;
    private LogWindow logWindow_;

    private static Logger logger_ = Logger.getLogger( "uk.ac.starlink.topcat" );
    private static LogHandler instance_;
    private static boolean configureFailed_;

    /**
     * Private constructor.
     *
     * @param   docHandler  target handler
     */
    private LogHandler( DocHandler docHandler ) {
        super( docHandler, 1000, Level.OFF );
        setLevel( Level.ALL );
        docHandler_ = docHandler;
    }

    /**
     * Displays a logging window which displays recent (the last 1000) and
     * any future log messages.
     *
     * @param  parent  parent component, may be used for positioning
     */
    public void showWindow( Component parent ) {
        if ( logWindow_ == null ) {
            logWindow_ = new LogWindow( parent );
        }
        logWindow_.makeVisible();
    }

    /**
     * Returns the sole instance of this class.
     * A null result may be returned if the Security Manager will not 
     * permit logging configuration to be performed.
     *
     * @return   singleton handler
     */
    public static LogHandler getInstance() {
        if ( instance_ == null && ! configureFailed_ ) {
            try {
                DocHandler target = new DocHandler();
                instance_ = new LogHandler( target );
            }
            catch ( SecurityException e ) {
                logger_.info( "Logging configuration failed" +
                              " - security exception" );
                configureFailed_ = true;
            }
        }
        return instance_;
    }

    /**
     * Handler implementation which publishes records to a 
     * {@link javax.swing.text.Document}.  Until a document has been 
     * set the records aren't published.
     */
    private static class DocHandler extends Handler {
        Document doc_;
        public DocHandler() {
            setFormatter( new SimpleFormatter() );
        }
        public void publish( LogRecord record ) {
            if ( doc_ != null ) {
                final String msg = getFormatter().format( record );
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        try {
                            doc_.insertString( doc_.getLength(), msg, null );
                        }
                        catch ( BadLocationException e ) {
                            assert false;
                        }
                    }
                } );
            }
        }
        public void flush() {
        }
        public void close() {
            doc_ = null;
        }
    }

    /**
     * Window which displays log messages.
     * The aesthetics leave something to be desired.
     */
    private class LogWindow extends AuxWindow {

         public LogWindow( Component parent ) {
             super( "Message Log", parent );

             /* Place a text window. */
             JEditorPane text = new JEditorPane();
             text.setEditable( false );
             JScrollPane scroller = new JScrollPane( text );
             scroller.setPreferredSize( new Dimension( 500, 300 ) );
             JComponent main = getMainArea();
             main.setLayout( new BorderLayout() );
             main.add( scroller );

             /* Notify the doc handler that it should be publishing to this
              * window. */
             doc_ = text.getDocument();
             docHandler_.doc_ = doc_;

             /* Ensure that all future records are published to the window
              * as soon as they happen. */
             setPushLevel( Level.ALL );

             /* Publish all the stored records to the window. */
             push();

             /* Define an action to clear the document. */
             Action clearAct = 
                 new BasicAction( "Clear", ResourceIcon.CLEAR,
                                  "Clear log of existing messages" ) {
                     public void actionPerformed( ActionEvent evt ) {
                         try {
                             doc_.remove( 0, doc_.getLength() );
                         }
                         catch ( BadLocationException e ) {
                             assert false;
                         }
                     }
                 };
             getToolBar().add( clearAct );
             getToolBar().addSeparator();

             addHelp( "LogWindow" );
         }
    }
}
