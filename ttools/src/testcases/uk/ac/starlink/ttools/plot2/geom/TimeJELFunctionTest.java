package uk.ac.starlink.ttools.plot2.geom;

import java.util.Random;
import junit.framework.TestCase;
import uk.ac.starlink.ttools.func.Times;

public class TimeJELFunctionTest extends TestCase {

    public void testFuncs() throws Exception {
        assertEquals( 1970.0,
                      new TimeJELFunction( "decYear" ).applyAsDouble( 0 ) );
        assertEquals( 1970.0,
                      new TimeJELFunction( "DecyeaR" ).applyAsDouble( 0 ) );
        assertEquals( 1971.0,
                      new TimeJELFunction( "1+decYear" ).applyAsDouble( 0 ) );
        Random r = new Random( -312747 );
        TimeJELFunction mjdFunc = new TimeJELFunction( "mjd+0" );
        TimeJELFunction jdFunc = new TimeJELFunction( "jd*1" );
        TimeJELFunction yearFunc = new TimeJELFunction( "decYear" );
        TimeJELFunction unixFunc = new TimeJELFunction( "unixSec" );
        for ( int i = 0; i < 100; i++ ) {
            double unixSec =
                ( r.nextDouble() - 0.5 ) * 60 * 60 * 24 * 365 * 100;
            assertEquals( unixSec, unixFunc.applyAsDouble( unixSec ) );
            assertEquals( Times.unixMillisToMjd( (long) ( unixSec * 1000 ) ),
                          mjdFunc.applyAsDouble( unixSec ), 1e-7 );
            assertEquals( Times.mjdToJd(
                             Times.unixMillisToMjd( (long) (unixSec * 1000) ) ),
                          jdFunc.applyAsDouble( unixSec ), 1e-7 );
            assertEquals( Times.mjdToDecYear(
                             Times.unixMillisToMjd( (long) (unixSec * 1000) ) ),
                          yearFunc.applyAsDouble( unixSec ) );
        }
    }
}
