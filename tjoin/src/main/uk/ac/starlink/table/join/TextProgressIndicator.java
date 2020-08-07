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
    final Profiler profiler;
    long lastUsedMem;

    /**
     * Constructs a new indicator which will output to a given stream.
     *
     * @param  out  output stream
     * @param  profile  true iff profiling reports are to be made along with
     *         the normal progress log
     */
    public TextProgressIndicator( PrintStream out, boolean profile ) {
        this.out = out;
        this.profiler = profile ? new Profiler() : null;
    }

    public void startStage( String stage ) {
        out.print( stage );
        blankWidth = fullWidth - stage.length();
        dotCount = 0;
        if ( profiler != null ) {
            profiler.reset();
        }
    }

    public void setLevel( double level ) {
        assert level >= 0 && level <= 1;
        level = Math.max( Math.min( level, 1.0 ), 0.0 );
        int moreDots = (int) ( level * blankWidth ) - dotCount;
        for ( int i = 0; i < moreDots; i++ ) {
            out.print( "." );
            dotCount++;
        }
    }

    public void endStage() {
        out.println();
        if ( profiler != null ) {
            logMessage( profiler.report() );
        }
    }

    public void logMessage( String msg ) {
        out.println( msg );
    }
}
