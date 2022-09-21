package uk.ac.starlink.table.join;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ProgressIndicator which logs progress to an output stream.
 *
 * @author   Mark Taylor (Starlink)
 * @since    24 Mar 2004
 */
public class TextProgressIndicator implements ProgressIndicator {

    private final PrintStream out_;
    private final Profiler profiler_;
    private final int fullWidth_;
    private final AtomicInteger dotCount_;
    private int blankWidth_;
    private long lastUsedMem_;

    /**
     * Constructs a new indicator which will output to a given stream.
     *
     * @param  out  output stream
     * @param  profile  true iff profiling reports are to be made along with
     *         the normal progress log
     */
    public TextProgressIndicator( PrintStream out, boolean profile ) {
        out_ = out;
        profiler_ = profile ? new Profiler() : null;
        fullWidth_ = 78;
        dotCount_ = new AtomicInteger();
    }

    public void startStage( String stage ) {
        out_.print( stage );
        blankWidth_ = fullWidth_ - stage.length();
        dotCount_.set( 0 );
        if ( profiler_ != null ) {
            profiler_.reset();
        }
    }

    public void setLevel( double level ) {
        level = Math.max( Math.min( level, 1.0 ), 0.0 );
        int newDots = (int) Math.round( level * blankWidth_ );
        int oldDots = dotCount_.getAndSet( newDots );
        for ( int i = oldDots; i < newDots; i++ ) {
            out_.print( "." );
        }
    }

    public void endStage() {
        out_.println();
        if ( profiler_ != null ) {
            logMessage( profiler_.report() );
        }
    }

    public void logMessage( String msg ) {
        out_.println( msg );
    }
}
