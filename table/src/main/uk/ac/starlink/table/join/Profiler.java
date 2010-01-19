package uk.ac.starlink.table.join;

/**
 * Utility class for simple reporting on CPU and memory resource usage.
 *
 * @author   Mark Taylor
 * @since    19 Jan 2010
 */
class Profiler {

    private final Runtime runtime_;
    private long resetUsed_;
    private long resetTime_;
    private long gcTime_;

    /**
     * Constructor.
     * Does not perform a reset.
     */
    public Profiler() {
        runtime_ = Runtime.getRuntime();
    }

    /**
     * Resets this profiler's state, so that subsequent reports will be
     * relative to the state when this was called.
     */
    public void reset() {
        gcTime_ = 0;
        resetTime_ = System.currentTimeMillis();
        resetUsed_ = getCurrentUsedMemory();
    }

    /**
     * Returns a brief (one-line) report on resource usage since the last
     * {@link #reset} call.
     *
     * @return  resource usage report
     */
    public String report() {
        long usedMem = getCurrentUsedMemory();
        long remainingMem = runtime_.maxMemory() - usedMem;
        long elapsedTime = System.currentTimeMillis() - resetTime_;
        return new StringBuffer()
              .append( "Mem: " )
              .append( formatMemory( resetUsed_ ) )
              .append( " -> " )
              .append( formatMemory( usedMem ) )
              .append( "; Time: " )
              .append( formatTime( elapsedTime ) )
              .append( " (" )
              .append( "gc " )
              .append( formatTime( gcTime_ ) )
              .append( ", remaining " )
              .append( formatMemory( remainingMem ) )
              .append( ")" )
              .toString();
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
        int tenths = (int) Math.round( millis / 100 );
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
