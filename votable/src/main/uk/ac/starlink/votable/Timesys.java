package uk.ac.starlink.votable;

import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;

/**
 * Utility class for working with VOTable TIMESYS elements.
 *
 * @author   Mark Taylor
 * @since    26 Apr 2019
 */
public class Timesys {

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

    /**
     * Extracts a Timesys instance from a ValueInfo.
     * If the metadata contains insufficient or incorrect information
     * to define a Timesys, null is returned.
     *
     * @param  info   value metadata
     * @return   timesys instance or null
     */
    public static Timesys getTimesys( ValueInfo info ) {
        String timeoriginTxt =
            Tables.getAuxDatumValue( info, VOStarTable.TIMESYS_TIMEORIGIN_INFO,
                                     String.class );
        String timescale =
            Tables.getAuxDatumValue( info, VOStarTable.TIMESYS_TIMESCALE_INFO,
                                     String.class );
        String refposition =
            Tables.getAuxDatumValue( info, VOStarTable.TIMESYS_REFPOSITION_INFO,
                                     String.class );
        if ( timescale != null && timescale.trim().length() > 0 &&
             refposition != null && refposition.trim().length() > 0 ) {
            double timeorigin;
            try {
                timeorigin = decodeTimeorigin( timeoriginTxt );
            }
            catch ( NumberFormatException e ) {
                return null;
            }
            return new Timesys( timeorigin, timescale, refposition );
        }
        else {
            return null;
        }
    }
}
