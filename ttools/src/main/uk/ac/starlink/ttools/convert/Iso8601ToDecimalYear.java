package uk.ac.starlink.ttools.convert;

import java.util.Calendar;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.ValueInfo;

/**
 * Converts between Strings in ISO-8601 format and numeric date as a
 * Decimal Year.
 *
 * @author   Mark Taylor
 * @since    29 Aug 2006
 */
public class Iso8601ToDecimalYear extends Iso8601Converter {

    /**
     * Constructs a new converter from ISO-8601 date Strings to
     * Decimal Year Doubles.
     *
     * @param   isoInfo  input value metadata (describing ISO-8601 strings)
     */
    public Iso8601ToDecimalYear( ValueInfo isoInfo ) {
        super( isoInfo, getDecimalYearInfo( isoInfo ) );
    }

    protected double toMjd( double number ) {
        return decYearToMjd( number );
    }

    protected double fromMjd( double mjd ) {
        return mjdToDecYear( mjd );
    }

    public String toString() {
        return "ISO-8601->Decimal year";
    }

    /**
     * Converts a Decimal Year to Modified Julian Date.
     *
     * @param   decYear  decimal yearo
     * @return  modified Julian date
     */
    private static double decYearToMjd( double decYear ) {
        int year = (int) Math.floor( decYear );
        double frac = decYear - year;
        Calendar cal = getKit().calendar_;
        cal.clear();
        cal.set( Calendar.YEAR, year );
        long y0 = cal.getTimeInMillis();
        cal.set( Calendar.YEAR, year + 1 );
        long y1 = cal.getTimeInMillis();
        long t = y0 + Math.round( frac * ( y1 - y0 ) );
        return unixMillisToMjd( t );
    }

    /**
     * Converts a Modified Julian Date to decimal year.
     *
     * @param  mjd  modified Julian date
     * @return  decimal year
     */
    private static double mjdToDecYear( double mjd ) {
        Calendar cal = getKit().calendar_;
        cal.clear();
        long t = mjdToUnixMillis( mjd );
        cal.setTimeInMillis( t );
        int year = cal.get( Calendar.YEAR );
        cal.clear();
        cal.set( Calendar.YEAR, year );
        long y0 = cal.getTimeInMillis();
        cal.set( Calendar.YEAR, year + 1 );
        long y1 = cal.getTimeInMillis();
        return (double) year + (double) ( t - y0 ) / (double) ( y1 - y0 );
    }

    /**
     * Returns the Decimal Year metadata object corresponding to the
     * application of this converter to a given ISO-8601 metadata object.
     *
     * @param  isoInfo  ISO-8601 metadata
     * @return  decimal year metadata
     */
    private static ValueInfo getDecimalYearInfo( ValueInfo isoInfo ) {
        DefaultValueInfo numInfo = new DefaultValueInfo( isoInfo );
        numInfo.setContentClass( Double.class );
        numInfo.setUnitString( "yr" );
        numInfo.setDescription( "Decimal Year" );
        numInfo.setNullable( true );
        return numInfo;
    }
}
