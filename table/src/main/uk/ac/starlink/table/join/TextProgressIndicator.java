package uk.ac.starlink.table.join;

import java.io.PrintStream;

/**
 * ProgressIndicator which logs progress to an output stream.
 *
 * @author   Mark Taylor (Starlink)
 * @since    24 Mar 2004
 */
public class TextProgressIndicator implements ProgressIndicator {
    int dotCount;
    final int fullWidth = 78;
    int blankWidth;
    PrintStream out = System.out;

    /**
     * Constructs a new indicator which will output to a given stream.
     *
     * @param  out  output stream
     */
    public TextProgressIndicator( PrintStream out ) {
        this.out = out;
    }

    public void startStage( String stage ) {
        out.print( stage );
        blankWidth = fullWidth - stage.length();
        dotCount = 0;
    }

    public void setLevel( double level ) {
        int moreDots = (int) ( level * blankWidth ) - dotCount;
        for ( int i = 0; i < moreDots; i++ ) {
            out.print( "." );
            dotCount++;
        }
    }

    public void endStage() {
        out.println();
    }

    public void logMessage( String msg ) {
        out.println( msg );
    }
}
