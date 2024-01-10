package uk.ac.starlink.ttools.func;

import java.util.Random;
import uk.ac.starlink.pal.Pal;
import uk.ac.starlink.pal.mjDate;
import uk.ac.starlink.pal.palError;
import uk.ac.starlink.table.TimeMapper;
import uk.ac.starlink.util.TestCase;

public class TimesTest extends TestCase {

    private static Random RANDOM = new Random( 1234567L );
    private static int NTEST = 10000;

    public TimesTest( String name ) {
        super( name );
    }

    public void testFixed() {
        assertEquals( 53303.5, Times.isoToMjd( "2004-10-25T12:00:00.00" ) );
        assertEquals( 53303.5, Times.isoToMjd( "2004-10-25T12:00:00." ) );
        assertEquals( 53303.5, Times.isoToMjd( "2004-10-25T12:00:00" ) );
        assertEquals( 53303.5, Times.isoToMjd( "2004-10-25 12:00:00" ) );
        assertEquals( 53303.5, Times.isoToMjd( "2004-10-25T12:00" ) );
        assertEquals( 53303.5, Times.isoToMjd( "2004-10-25T12" ) );
        assertEquals( 53303.0, Times.isoToMjd( "2004-10-25" ) );
        assertEquals( 53303.0, Times.isoToMjd( "2004-10-25Z" ) );

        assertEquals( "2004-10-25T12:00:00", Times.mjdToIso( 53303.5 ) );
        assertEquals( "18:00:00", Times.mjdToTime( -0.25 ) );
        assertEquals( "1858-11-17", Times.mjdToDate( 0.1 ) );

        assertEquals( "(BCE)4713-01-01T12:00:00",
                      Times.mjdToIso( Times.jdToMjd( 0 ) ) );
    } 

    public void testFormat() {
        double frac;
        double milliTolerance = 1.0 / ( 24 * 60 * 60 * 1000 );
        double secTolerance = 1.0 / ( 24 * 60 * 60 );
        String mfmt = "yyyy-MM-dd'T'HH:mm:ss.SSS";
        for ( int i = 0; i < NTEST; i++ ) {
            double mjd = 1e5 * rnd();
            assertEquals( mjd, Times.isoToMjd( Times.mjdToIso( mjd ) ), 
                          secTolerance );
            assertEquals( mjd, Times.isoToMjd( Times.formatMjd( mjd, mfmt ) ),
                          milliTolerance );
            assertEquals( mjd, Times.dateToMjd( Times.mjdYear( mjd ),
                                                Times.mjdMonth( mjd ),
                                                Times.mjdDayOfMonth( mjd ),
                                                Times.mjdHour( mjd ),
                                                Times.mjdMinute( mjd ),
                                                Times.mjdSecond( mjd ) ),
                          milliTolerance );
            assertEquals( Times.mjdToIso( mjd ), Times.mjdToDate( mjd ) + 
                                                 "T" +
                                                 Times.mjdToTime( mjd ) );
        }
    }

    public void testPalCmp() throws palError {
        Pal pal = new Pal();
        for ( int i = 0; i < NTEST; i++ ) {
            double mjd = 1e5 * rnd();
            mjDate palMjd = pal.Djcal( mjd );
            assertEquals( (int) mjd, Times.dateToMjd( palMjd.getYear(), 
                                                      palMjd.getMonth(),
                                                      palMjd.getDay() ) );
        }
    }

    public void testMappers() {
        for ( int i = 0; i < NTEST; i++ ) {
            long sec = (long) ( ( rnd() - 0.5 ) * 1e6 );
            assertEquals(
                (double) sec,
                (double) TimeMapper.MJD
               .toUnixSeconds( Times.unixMillisToMjd( sec * 1000 ) ),
                0.001 );
        }
    }

    public void testBlanks() {
        assertNull( Times.mjdToIso( Double.NaN ) );
        assertNull( Times.mjdToDate( Double.NaN ) );
        assertNull( Times.mjdToTime( Double.NaN ) );
        assertTrue( Double.isNaN( Times.isoToMjd( "" ) ) );
        assertTrue( Double.isNaN( Times.isoToMjd( null ) ) );
        try {
            Times.isoToMjd( "not-an-iso8601-epoch" );
            fail();
        }
        catch ( IllegalArgumentException e ) {
        }
    }

    public void testEpoch() {
        for ( double mjd = 0.; mjd < 1e5; mjd += 100. ) {
            assertEquals( Times.mjdToJulian( mjd ),
                          Times.mjdToBesselian( mjd ), 1e-2 );
        }
    }

    public void testExamples() {
        assertEquals( 53303.75, Times.isoToMjd("2004-10-25T18:00:00") );
        assertEquals( 40587.0, Times.isoToMjd( "1970-01-01" ) );

        assertEquals( Times.dateToMjd( 1999, 12, 31, 23, 59, 59.0 ), 
                      51543.999988,
                          0.000001 );
        assertEquals( Times.dateToMjd( 1999, 12, 31 ), 51543.0 );

        assertEquals( "2005-06-30T17:30:00", Times.mjdToIso( 53551.72917 ) );
        assertEquals( "2005-06-30", Times.mjdToDate( 53551.72917 ) );
        assertEquals( "17:30:00", Times.mjdToTime( 53551.72917 ) );

        // Note that the output for some formatting characters such as
        // EEE (day of week) are sensitive to the default Locale.
        // So avoid them in this test.
        assertEquals( "283/001995[UTC]", 
                      Times.formatMjd( 50000.3, "DD/yyyyyy[z]" ) );
        assertEquals( "time 2:57:41.760", 
                      Times.formatMjd( 50000.1234, "'time 'H:mm:ss.SSS" ) );

        assertEquals( 1858.87885, Times.mjdToJulian( 0.0 ), 1e-5 );
        assertEquals( 51544.5, Times.julianToMjd( 2000.0 ) );
        assertEquals( 1858.87711, Times.mjdToBesselian( 0.0 ), 1e-5 );
        assertEquals( 33281.92346, Times.besselianToMjd( 1950.0 ), 1e-5 );

        assertEquals( 1098727200, Times.isoToUnixSec("2004-10-25T18:00:00") );
        assertEquals( 0, Times.isoToUnixSec( "1970-01-01" ) );
        assertEquals( 946684800, Times.decYearToUnixSec( 2000.0 ) );
        assertEquals( 0, Times.decYearToUnixSec( 1970 ) );
    }

    private static double rnd() {
        return RANDOM.nextDouble();
    }

}
