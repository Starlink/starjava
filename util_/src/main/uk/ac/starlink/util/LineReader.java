package uk.ac.starlink.util;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Class containing methods for reading strings from the user (standard input).
 * Both normal and 'masked' reads are provided - the latter is useful
 * for reading passwords in such a way that they don't appear on the screen.
 * 
 * @author   Mark Taylor
 * @since    27 Nov 2006
 */
public class LineReader {

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
                    Thread.sleep( 10 );
                }
                catch ( InterruptedException e ) {
                    interrupt();
                }
            }
        }
    }
}
