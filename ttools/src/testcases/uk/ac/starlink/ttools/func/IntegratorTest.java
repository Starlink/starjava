package uk.ac.starlink.ttools.func;

import junit.framework.TestCase;

public class IntegratorTest extends TestCase {

    public IntegratorTest( String name ) {
        super( name );
    }

    public void testSquare() {
        Integrator sqint = new Integrator( 0.0, 0.01 ) {
            public double function( double x ) {
                return x * x;
            }
        };

        assertEquals( 4 * 4 * 4 / 3.0, sqint.integral( 4 ), 1e-4 );
        assertEquals( - 4 * 4 * 4 / 3.0, sqint.integral( -4 ), 1e-4 );
        assertEquals( 10 * 10 * 10 / 3.0, sqint.integral( 10 ), 1e-4 );
    }

    public void testConst() {
        final double C = 10;
        Integrator cint = new Integrator( 0.0, 0.5 ) {
            public double function( double x ) {
                return C;
            }
        };
        assertEquals( C * 1.0, cint.integral( 1.0 ), 1e-10 );
        assertEquals( C * Math.E, cint.integral( Math.E ), 1e-10 );
        assertEquals( - C * Math.E, cint.integral( - Math.E ), 1e-10 );
        assertEquals( C * 999.9, cint.integral( 999.9 ), 1e-10 );

        exerciseConst( Math.PI, 99, 109 );
        exerciseConst( 99, Math.PI, 1e5 );
        exerciseConst( -99, Math.PI, 1e5 );
    }

    public void exerciseConst( double base, double step,
                               final double constant ) {
        Integrator cint = new Integrator( base, step ) {
            public double function( double x ) {
                return constant;
            }
        };
        double delta = 1e-6;
        assertEquals( 0.0, cint.integral( base ), delta );
        assertEquals( constant,
                      cint.integral( 1.0 ) - cint.integral( 0.0 ), delta );
        assertEquals( constant * Math.E,
                      cint.integral( Math.E ) - cint.integral( 0.0 ), delta );
        assertEquals( - constant * Math.E,
                      cint.integral ( - Math.E ) - cint.integral( 0.0 ),
                      delta );
        assertEquals( constant * 999.9,
                      cint.integral( 999.9 ) - cint.integral( 0.0 ), delta );
    }
}
