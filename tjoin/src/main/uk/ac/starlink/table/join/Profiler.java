package uk.ac.starlink.table.join;

/**
 * Utility class for simple reporting on CPU and memory resource usage.
 *
 * @author   Mark Taylor
 * @since    19 Jan 2010
 */
class Profiler {

    private final Runtime runtime_;
    private final boolean isTime_;
    private final boolean isMem_;
    private long resetUsed_;
    private long resetTime_;
    private long gcTime_;

    /**
     * Constructs a profiler with default configuration.
     * Does not perform a reset.
     */
    public Profiler() {
        this( true, true );
    }

    /**
     * Constructs a profiler with explicit configuration.
     * Does not perform a reset.
     *
     * @param  isTime  true to report timings
     * @param  isMem   true to report memory usage - this calls System.gc
     *                 so may slow things down
     */
    public Profiler( boolean isTime, boolean isMem ) {
        runtime_ = Runtime.getRuntime();
        isTime_ = isTime;
        isMem_ = isMem;
    }

    /**
     * Resets this profiler's state, so that subsequent reports will be
     * relative to the state when this was called.
     */
    public void reset() {
        gcTime_ = 0;
        resetTime_ = System.currentTimeMillis();
        if ( isMem_ ) {
            resetUsed_ = getCurrentUsedMemory();
        }
    }

    /**
     * Returns a brief (one-line) report on resource usage since the last
     * {@link #reset} call.
     *
     * @return  resource usage report
     */
    public String report() {
        StringBuffer sbuf = new StringBuffer();
        final long usedMem;
        final long remainingMem;
        if ( isMem_ ) {
            usedMem = getCurrentUsedMemory();
            remainingMem = runtime_.maxMemory() - usedMem;
        }
        else {
            usedMem = 0;
            remainingMem = 0;
        }
        long elapsedTime = isTime_ ? System.currentTimeMillis() - resetTime_
                                   : 0;
        if ( isTime_ ) {
            if ( sbuf.length() > 0 ) {
                sbuf.append( "; " );
            }
            sbuf.append( "Time: " )
                .append( formatTime( elapsedTime ) );
            if ( isMem_ ) {
                sbuf.append( " (gc: " )
                    .append( formatTime( gcTime_ ) )
                    .append( ")" );
            }
        }
        if ( isMem_ ) {
            if ( sbuf.length() > 0 ) {
                sbuf.append( "; " );
            }
            sbuf.append( "Mem: " )
                .append( formatMemory( resetUsed_ ) )
                .append( " -> " )
                .append( formatMemory( usedMem ) )
                .append( ", remaining " )
                .append( formatMemory( remainingMem ) );
        }
        return sbuf.toString();
    }

    /**
     * Formats a byte count in a compact way.
     *
     * @param   nbyte  byte count
     * @return   memory size string
     */
    public String formatMemory( long nbyte ) {
        int megas = (int) Math.round( (double) nbyte / 1024 / 1024 );
        return megas + "M";
    }

    /**
     * Formats a time in a compact way.
     *
     * @param  millis  millisecond count
     * @return  elapsed time string
     */
    public String formatTime( long millis ) {
        long tenths = millis / 100;
        return ( tenths / 10 ) + "." + ( tenths % 10 ) + "s";
    }

    /**
     * Performs a garbage collection, and then obtains and returns the
     * amount of memory currently used.
     * Since a gc is called, this method may not be cheap to call.
     *
     * @return   bytes used
     */
    public long getCurrentUsedMemory() {
        long startGc = System.currentTimeMillis();
        runtime_.gc();
        gcTime_ += System.currentTimeMillis() - startGc;
        return runtime_.totalMemory() - runtime_.freeMemory();
    }
}
