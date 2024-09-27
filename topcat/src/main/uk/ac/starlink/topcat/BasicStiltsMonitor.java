package uk.ac.starlink.topcat;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;
import uk.ac.starlink.ttools.task.CommandFormatter;
import uk.ac.starlink.ttools.task.StiltsCommand;

/**
 * StiltsMonitor concrete subclass for STILTS commands without special
 * requirements.
 *
 * @author   Mark Taylor
 * @since    10 Oct 2024
 */
public class BasicStiltsMonitor extends
        StiltsMonitor<BasicStiltsMonitor.BasicState> {

    private StiltsCommand command_;
    private CommandFormatter formatter_;

    /**
     * Constructor.
     */
    public BasicStiltsMonitor() {
    }

    protected BasicState createState() {
        return createState( command_, formatter_ );
    }

    /**
     * Configures this monitor with a given command and formatter.
     *
     * @param  command  stilts command
     * @param  formatter  formatting details
     */
    public void configure( StiltsCommand command, CommandFormatter formatter ) {
        command_ = command;
        formatter_ = formatter;
    }

    /**
     * Creates a state from a supplied command and formatter.
     *
     * @param  command  stilts command
     * @param  formatter  formatting details
     * @return  new state
     */
    private static BasicState createState( StiltsCommand command,
                                           CommandFormatter formatter ) {
        if ( command == null || formatter == null ) {
            return BasicState.EMPTY;
        }
        StyledDocument doc = formatter.createShellDocument( command );
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
            formatter.createExecutable( command );
            err = null;
        }
        catch ( Throwable e ) {
            err = e;
        }
        return new BasicState( txt, err, doc );
    }

    /**
     * Concrete state class for the BasicStiltsMonitor.
     */
    public static class BasicState implements StiltsMonitor.State {

        private String text_;
        private Throwable error_;
        private StyledDocument doc_;

        /** State representing no executable command. */
        public static BasicState EMPTY =
            new BasicState( null, null, new DefaultStyledDocument() );

        /**
         * Constructor.
         *
         * @param  text  text of the STILTS command, not null
         * @param  error   error produced when trying to regenerate a
         *                 STILTS executable from the serialized command;
         *                 null if it seemed to work OK
         * @param  doc   styled document to display showing stilts command,
         *               or possibly error message
         */
        BasicState( String text, Throwable error, StyledDocument doc ) {
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
