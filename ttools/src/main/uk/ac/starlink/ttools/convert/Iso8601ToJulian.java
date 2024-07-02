package uk.ac.starlink.ttools.convert;

import uk.ac.starlink.pal.Pal;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.ValueInfo;

/**
 * Converts between Strings in ISO-8601 format and numeric date as a
 * Julian Year.
 *
 * @author   Mark Taylor
 * @since    23 Feb 2006
 */
public class Iso8601ToJulian extends Iso8601Converter {

    private final static Pal pal_ = new Pal();

    /**
     * Constructs a new converter from ISO-8601 date Strings to
     * Julian year Doubles.
     *
     * @param   isoInfo  input value metadata (describing ISO-8601 strings)
     */
    public Iso8601ToJulian( ValueInfo isoInfo ) {
        super( isoInfo, getJulianInfo( isoInfo ) );
    }

    protected double toMjd( double number ) {
        return julianToMjd( number );
    }

    protected double fromMjd( double mjd ) {
        return mjdToJulian( mjd );
    }

    public String toString() {
        return "ISO-8601->Julian Year";
    }

    /**
     * Converts a Modified Julian Date to Julian Epoch.
     * For approximate purposes, the result
     * of this routine consists of an integral part which gives the
     * year AD and a fractional part which represents the distance
     * through that year, so that for instance 2000.5 is approximately
     * 1 July 2000.
     *
     * @param  mjd  modified Julian date
     * @return  Julian epoch
     */
    private static double mjdToJulian( double mjd ) {
        return pal_.Epj( mjd );
    }

    /**
     * Converts a Julian Epoch to Modified Julian Date.
     *
     * @param  julianEpoch  Julian epoch
     * @return   modified Julian date
     */
    private static double julianToMjd( double julianEpoch ) {
        return pal_.Epj2d( julianEpoch );
    }

    /**
     * Returns the Julian Epoch metadata object corresponding to the
     * application of this converter to a given ISO-8601 metadata object.
     *
     * @param  isoInfo   ISO-8601 metadata
     * @return  Julian Epoch metadata
     */
    private static ValueInfo getJulianInfo( ValueInfo isoInfo ) {
        DefaultValueInfo numInfo = new DefaultValueInfo( isoInfo );
        numInfo.setContentClass( Double.class );
        numInfo.setUnitString( "yr" );
        numInfo.setDescription( "Julian Year" );
        numInfo.setNullable( true );
        return numInfo;
    }
}
