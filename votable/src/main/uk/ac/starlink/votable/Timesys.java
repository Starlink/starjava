package uk.ac.starlink.votable;

/**
 * Utility class for working with VOTable TIMESYS elements.
 *
 * @author   Mark Taylor
 * @since    26 Apr 2019
 */
class Timesys {

    private final double timeorigin_;
    private final String timescale_;
    private final String refposition_;

    /**
     * Constructor.
     *
     * @param   timeorigin   numeric value of time origin (JD offset);
     *                       if the timeys doesn't have one, use NaN
     * @param   timescale    timescale value
     * @param   refposition  refposition value
     */
    protected Timesys( double timeorigin, String timescale,
                       String refposition ) {
        timeorigin_ = timeorigin;
        timescale_ = timescale;
        refposition_ = refposition;
    }

    /**
     * Returns the numeric value of time origin.
     *
     * @return   JD offset in days, or NaN if no time origin
     */
    public double getTimeorigin() {
        return timeorigin_;
    }

    /**
     * Returns the timescale identifier.
     *
     * @return  timescale
     */
    public String getTimescale() {
        return timescale_;
    }

    /**
     * Returns the reference position identifier.
     *
     * @return  refposition
     */
    public String getRefposition() {
        return refposition_;
    }

    /**
     * @return  human-readable summary of timesys.
     */
    @Override
    public String toString() {
        StringBuffer sbuf = new StringBuffer()
            .append( "TIMESYS(" )
            .append( "timescale=" )
            .append( timescale_ )
            .append( "," )
            .append( ",refposition=" )
            .append( refposition_ );
        if ( ! Double.isNaN( timeorigin_ ) ) {
            sbuf.append( ",timeorigin=" );
            if ( timeorigin_ == 0 ) {
                sbuf.append( "JD-origin" );
            }
            else if ( timeorigin_ == 2400000.5 ) {
                sbuf.append( "MJD-origin" );
            }
            else {
                sbuf.append( timeorigin_ );
            }
        }
        sbuf.append( ")" );
        return sbuf.toString();
    }

    /**
     * Turns the value string found in the TIMESYS/@timeorigin attribute
     * into a numeric value giving JD offset in days.
     * If the text is not legal for the timeorigin attribute,
     * a NumberFormatException will be thrown.
     * 
     * @param  txt  timeorigin attribute value
     * @return   numeric offset value
     * @throws  NumberFormatException  if the value is not one of the
     *          magic strings and is not a valid numerical representation
     */
    public static double decodeTimeorigin( String txt )
            throws NumberFormatException {
        if ( txt == null || txt.trim().length() == 0 ) {
            return Double.NaN;
        }
        String timeorigin = txt.trim();
        if ( "JD-origin".equalsIgnoreCase( timeorigin ) ) {
            return 0;
        }
        else if ( "MJD-origin".equalsIgnoreCase( timeorigin ) ) {
            return 2400000.5;
        }
        else {
            return Double.parseDouble( timeorigin );
        }
    }
}
