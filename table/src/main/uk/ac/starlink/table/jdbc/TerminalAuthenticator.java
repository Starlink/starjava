package uk.ac.starlink.table.jdbc;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Provides JDBC authentication using the terminal; assumes that someone
 * is sitting at <tt>System.in</tt>.
 */
public class TerminalAuthenticator implements JDBCAuthenticator {

    private final PrintStream err_;

    /**
     * Constructs a new authenticator with a given stream to use for
     * writing prompts.
     *
     * @param  promptStrm  output stream for prompting
     */
    public TerminalAuthenticator( PrintStream promptStrm ) {
        err_ = promptStrm;
    }

    /**
     * Constructs a new authenticator which uses System.err for prompting.
     */
    public TerminalAuthenticator() {
        this( System.err );
    }

    public String[] authenticate() throws IOException {
        return new String[] { readUser(), readPassword() };
    }

    /**
     * Prompts to the prompt stream and reads the user name from standard
     * input.
     *
     * @return  user name obtained from user
     */
    public String readUser() throws IOException {
        return readString( "JDBC User: ", err_ );
    }

    /**
     * Prompts to the prompt stream and reads the password from standard
     * input.
     *
     * @return  password obtained from user
     */
    public String readPassword() throws IOException {
        return readMaskedString( "JDBC Password: ", err_ );
    }

    /**
     * Reads a line of text from the user.
     *
     * @param   prompt  short line of text to act as a prompt
     * @param   outStrm  print stream to use for prompt output - typically
     *          System.err
     */
    public static String readString( String prompt, PrintStream outStrm )
            throws IOException {
        outStrm.print( prompt );
        outStrm.flush();

        StringBuffer ibuf = new StringBuffer();
        for ( boolean done = false; ! done; ) {
            int c = System.in.read();
            switch ( c ) {
                case -1:
                case '\n':
                case '\r':
                    done = true;
                    break;
                default:
                    ibuf.append( (char) c );
            }
        }
        return ibuf.toString();
    }

    /**
     * Reads a line of text from the user without it being visible to 
     * onlookers.  Suitable utility method for soliciting passwords.
     *
     * @param  prompt  short line of text to act as a prompt
     * @param  outStrm  print stream to use for output - typically System.err
     * @return  string entered by user
     */
    public static String readMaskedString( String prompt, PrintStream outStrm )
            throws IOException {
        StringBuffer sbuf = new StringBuffer();
        int linesep0 = (int) System.getProperty( "line.separator" ).charAt( 0 );
        MaskingThread masker = new MaskingThread( prompt, outStrm );
        masker.start();
        try {
            while ( true ) {
                int c = System.in.read();
                if ( c == linesep0 || c < 0 ) {
                    masker.interrupt();
                    if ( c < 0 ) {
                        outStrm.println();
                    }
                    break;
                }
                sbuf.append( (char) c );
            }
        }
        catch ( IOException e ) {
            masker.interrupt();
            outStrm.println();
            throw e;
        }
        return sbuf.toString();
    }

    /**
     * Helper class used to mask input from the user when a password is
     * being entered.
     */
    private static class MaskingThread extends Thread {
        final String prompt;
        final PrintStream ostrm;
        public MaskingThread( String prompt, PrintStream ostrm ) {
            this.ostrm = ostrm;
            this.prompt = prompt;
        }
        public void run() {
            while ( ! interrupted() ) {
                ostrm.print( '\r' + prompt + " \r" + prompt );
                ostrm.flush();
                try {
                    this.sleep( 10 );
                }
                catch ( InterruptedException e ) {
                    interrupt();
                }
            }
        }
    }
}
