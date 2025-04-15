package uk.ac.starlink.ttools.plot2;

import java.util.function.DoubleUnaryOperator;
import uk.ac.starlink.ttools.jel.JELFunction;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.ScaleConfigKey;
import uk.ac.starlink.util.TestCase;

public class ScaleTest extends TestCase {

    public void testAsinhScales() {
        double[] values = {
            99, 1.5e2, Math.PI, 0, 1e-6, 400, 3.2e8,
        };
        Scale[] asinhScales = {
            createAsinhScale( 0.01 ),
            createAsinhScale( Math.E ),
            createAsinhScale( 6.6 ),
            createAsinhScale( 50 ),
        };
        for ( Scale scale : asinhScales ) {
            for ( double v : values ) {
                assertEquals( v, scale.scaleToData( scale.dataToScale( v ) ),
                              v * 1e-6 );
                assertEquals( -v, scale.scaleToData( scale.dataToScale( -v ) ),
                              v * 1e-6 );
            }
        }
    }

    public void testSymlogScales() {
        double[] values = {
            99, 1.5e2, Math.PI, 0, 1e-6, 1000, 23e9,
        };
        Scale[] symlogScales = {
            createSymlogScale( 1, 1 ),
            createSymlogScale( .01, 5 ),
            createSymlogScale( 1e6, 6 ),
        };
        for ( Scale scale : symlogScales ) {
            for ( double v : values ) {
                assertEquals( v, scale.scaleToData( scale.dataToScale( v ) ),
                              v * 1e-9 );
                assertEquals( -v, scale.scaleToData( scale.dataToScale( -v ) ),
                              v * 1e-9 );
            }
        }
    }

    public void testScales() throws Exception {
        Scale[] scales = new Scale[] {
            Scale.LINEAR,
            Scale.LOG,
            createSymlogScale( 1, 1 ),
            createSymlogScale( 0.1, 0.5 ),
            createSymlogScale( 1e3, 2 ),
            createAsinhScale( 1 ),
            createAsinhScale( 19.9 ),
            createAsinhScale( 0.3 ),
        };
        ScaleConfigKey ck =
            new ScaleConfigKey( new ConfigMeta( "scale", "Scale" ) );
        for ( Scale scale : scales ) {
            assertEquals( scale, ck.stringToValue( ck.valueToString( scale ) ));
            DoubleUnaryOperator exprFunc =
                new JELFunction( "x", scale.dataToScaleExpression( "x" ) );
            for ( double i = 0; i < 10; i++ ) {
                double value = 0.1 + 1.5 * i;
                assertEquals( scale.dataToScale( value ),
                              exprFunc.applyAsDouble( value ),
                              1e-8 );
                if ( !scale.isPositiveDefinite() ) {
                    assertEquals( scale.dataToScale( -value ),
                                  exprFunc.applyAsDouble( -value ),
                                  1e-8 );
                }
            }
        }
    }

    public void testKeySerialization() throws ConfigException {
        ScaleConfigKey ck =
            new ScaleConfigKey( new ConfigMeta( "scale", "Scale" ) );
        assertEquals( Scale.LOG, ck.stringToValue( "log" ) );
        assertEquals( Scale.LINEAR, ck.stringToValue( "linear()" ) );
        assertEquals( ScaleType.SYMLOG.createScale( new double[] {} ),
                      ck.stringToValue( "symlog" ) );
        assertEquals( ScaleType.SYMLOG.createScale( new double[] { 0.25 } ),
                      ck.stringToValue( "symlog(.25)" ) );
        assertEquals( ScaleType.SYMLOG.createScale( new double[] { 0.25, 3 } ),
                      ck.stringToValue( "symlog( .25 , 3.0 )" ) );
        for ( String txt : new String[] {
                 "doofus", "linear(3)", "asinh(12/3)", "symlog(.25,3,9)",
                 "asinh(0)", "symlog(1,-1)", "symlog(0)",
              } ) {
            try {
                ck.stringToValue( txt );
                fail( "Valid scale specification??: " + txt );
            }
            catch ( ConfigException e ) {
                // good
            }
        }
    }

    public void testParsedFunctionCall() {
        ParsedFunctionCall pfc = ParsedFunctionCall.fromString( "abc(2,3.5)" );
        assertArrayEquals( new double[] { 2, 3.5 }, pfc.getArguments() );
        assertEquals( "abc", pfc.getFunctionName() );
        for ( String txt : new String[] { "xyz(99,3)", "abc(1)", "f()", "f" } ){
            assertEquals( txt,
                          ParsedFunctionCall.fromString( txt ).toString() );
        }
    }

    public static Scale createSymlogScale( double linthresh, double linscale ) {
        return ScaleType.SYMLOG.createScale( new double[] { linthresh,
                                                            linscale } );
    }

    public static Scale createAsinhScale( double a0 ) {
        return ScaleType.ASINH.createScale( new double[] { a0 } );
    }
}
