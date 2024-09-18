package uk.ac.starlink.topcat;

import uk.ac.starlink.topcat.AngleColumnConverter.Unit;
import junit.framework.TestCase;

public class ConverterTest extends TestCase {

    public void testAngleColumnConverter() {
        checkToRadian( Unit.RADIAN, 2, 2 );
        checkToRadian( Unit.DEGREE, 180, Math.PI );
        checkToRadian( Unit.HOUR, 24, 2 * Math.PI );
        checkToRadian( Unit.ARCMIN, 180 * 60, Math.PI );
        checkToRadian( Unit.ARCSEC, 180 * 3600, Math.PI );

        for ( Unit unit : Unit.values() ) {
            assertEquals( "3", convExpr( unit, unit, "3" ) );
            assertEquals( "blinky", convExpr( unit, unit, "blinky" ) );
        }
        assertEquals( "foo*60", convExpr( Unit.DEGREE, Unit.ARCMIN, "foo" ) );
        assertEquals( "0", convExpr( Unit.DEGREE, Unit.ARCMIN, "0" ) );
        assertEquals( "1*60", convExpr( Unit.DEGREE, Unit.ARCMIN, "1" ) );
        assertEquals( "foo/60.", convExpr( Unit.ARCSEC, Unit.ARCMIN, "foo" ) );
        assertEquals( "1/60.", convExpr( Unit.ARCSEC, Unit.ARCMIN, "1" ) );
        assertEquals( "radiansToDegrees(foo)*60",
                      convExpr( Unit.RADIAN, Unit.ARCMIN, "foo" ) );
        assertEquals( "degreesToRadians((foo+bar)/60.)",
                      convExpr( Unit.ARCMIN, Unit.RADIAN, "foo+bar" ) );
    }

    private String convExpr( Unit fromUnit, Unit toUnit, String inExpr ) {
        return AngleColumnConverter.angleExpression( fromUnit, toUnit, inExpr );
    }

    private void checkToRadian( Unit unit, double in, double out ) {
        assertEquals( out,
                      ((Number) AngleColumnConverter.toRadianConverter( unit )
                               .convertValue( Double.valueOf( in ) ))

                     .doubleValue(), 1e-10 );
    }
}
