package uk.ac.starlink.table.join;

/**
 * Defines how two errors are combined to define a separation threshold.
 *
 * @author   Mark Taylor
 * @since    15 May 2024
 */
public enum ErrorSummation {

    /** Combines errors by simple addition. */
    SIMPLE( "simple addition", "" ) {
        public double combine( double e1, double e2 ) {
            return e1 + e2;
        }
        public double combineSquared( double e1, double e2 ) {
            double c = e1 + e2;
            return c * c;
        }
    },

    /** Combines errors by addition in quadrature. */
    QUADRATURE( "addition in quadrature", " in quadrature" ) {
        public double combine( double e1, double e2 ) {
            return Math.hypot( e1, e2 );
        }
        public double combineSquared( double e1, double e2 ) {
            return e1 * e1 + e2 * e2;
        }
    };

    private final String description_;
    private final String tail_;

    /**
     * Constructor.
     *
     * @param  description  short human-readable description
     * @param  tail   string suitable for appending to matcher description
     */
    private ErrorSummation( String description, String tail ) {
        description_ = description;
        tail_ = tail;
    }

    /**
     * Combines two error values to produce a threshold value for separation.
     *
     * @param   e1  first error
     * @param   e2  second error
     * @return   threshold value for largest separation that counts as a match
     */
    public abstract double combine( double e1, double e2 );

    /**
     * Returns the square of the result of the
     * {@link #combine combine} method.
     *
     * <p>This method is provided in case there is a more efficient
     * implementation than the obvious one.
     *
     * @param   e1  first error
     * @param   e2  second error
     * @return numerically equal to <code>combine(e1,e2)*combine(e1,e2)</code>
     */
    public abstract double combineSquared( double e1, double e2 );

    /**
     * Returns a short human-readable description of the combination method.
     *
     * @return  description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Returns (possibly empty) text suitable for appending to a matcher
     * description.
     *
     * @return  tail string; may be empty but not null
     */
    public String getTail() {
        return tail_;
    }
}
