package uk.ac.starlink.ttools.func;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Level;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.TestCase;

public class FuncTest extends TestCase {

    private static final double TINY = 1e-13;
    private static final Random RANDOM = new Random( 1234567L );

    static {
        LogUtils.getLogger( "uk.ac.starlink.ttools.plot2" )
                .setLevel( Level.WARNING );
    }

    public FuncTest( String name ) {
        super( name );
    }


    public void testArithmetic() {
        assertEquals( 3, Arithmetic.abs( 3 ) );
        assertEquals( 2.3, Arithmetic.abs( -2.3 ) );

        assertEquals( 5, Arithmetic.max( (short) 5, (short) 4) );
        assertEquals( -4.0, Arithmetic.maxNaN( -5.0f, -4.0f ) );

        assertEquals( 4, Arithmetic.min( (short) 5, (short) 4) );
        assertEquals( -5.0, Arithmetic.minNaN( -5.0f, -4.0f ) );

        assertEquals( 4.0, Arithmetic.maxReal( 4.0, Double.NaN ) );
        assertEquals( 4.0, Arithmetic.maxReal( Double.NaN, 4.0 ) );
        assertEquals( Math.PI, Arithmetic.minReal( Math.PI, Double.NaN ) );
        assertEquals( Math.PI, Arithmetic.minReal( Double.NaN, Math.PI ) );
        assertTrue( Double.isNaN( Arithmetic.maxNaN( 4.0, Double.NaN ) ) );
        assertTrue( Double.isNaN( Arithmetic.maxNaN( Double.NaN, 4.0 ) ) );
        assertTrue( Double.isNaN( Arithmetic.minNaN( Math.PI, Double.NaN ) ) );
        assertTrue( Double.isNaN( Arithmetic.minNaN( Double.NaN, Math.PI ) ) );
        assertEquals( 4.0, Arithmetic.maxReal( 4.0, Math.PI ) );
        assertEquals( 4.0, Arithmetic.maxReal( Math.PI, 4.0 ) );
        assertEquals( Math.PI, Arithmetic.minReal( 4.0, Math.PI ) );
        assertEquals( Math.PI, Arithmetic.minReal( Math.PI, 4.0 ) );

        assertEquals( 12, Arithmetic.roundUp( 11.01 ) );
        assertEquals( 11, Arithmetic.round( 11.01 ) );
        assertEquals( 11, Arithmetic.roundDown( 11.99 ) );
        assertEquals( 12, Arithmetic.round( 11.99 ) );
        assertEquals( 4, Arithmetic.round( 4.5 ) );

        assertEquals( 3.14f, Arithmetic.roundDecimal( Math.PI, 2 ) );

        assertEquals( 3, Arithmetic.mod( 23, 10 ) );
        assertEquals( 3, Arithmetic.mod( 23, -10 ) );
        assertEquals( -3, -23 % 10 );
        assertEquals( 7, Arithmetic.mod( -23, 10 ) );
        assertEquals( 0, Arithmetic.mod( 320, 16 ) );
        assertEquals( 0, Arithmetic.mod( -320, 16 ) );
        assertEquals( 0.5, Arithmetic.mod( 5.5, 1.25 ) );

        assertEquals( 0.5, Arithmetic.phase( 112.5, 25 ) );
        assertEquals( 0.75, Arithmetic.phase( 7, 4 ) );
        assertEquals( 0.8, Arithmetic.phase( -1000.5, 2.5 ) );
        assertEquals( 0, Arithmetic.phase( -3300, 33 ) );

        assertEquals( 0.5, Arithmetic.phase( 112.5, 25, 0 ) );
        assertEquals( 0.03, Arithmetic.phase( 5003, 100, 0 ) );
        assertEquals( 0.01, Arithmetic.phase( 5003, 100, 2 ) );
        assertEquals( 0.99, Arithmetic.phase( 5003, 100, 4 ) );

        assertEquals( 1.0, Arithmetic.phase( -8.6, 0.2, 0, 0.5 ),
                      1e-9 );
        assertEquals( 0.0, Arithmetic.phase( 8.6125, 0.2, 0.0125, -0.7 ),
                      1e-9 );
        assertEquals( -0.5, Arithmetic.phase( 8.6125, 0.2, 0.1125, -0.7 ),
                      1e-9 );
        assertEquals( 99.2, Arithmetic.phase( 23, 10, 1, 99 ) );
    }

    public void testBits() {
        assertEquals( 1, Bits.bitCount( 64 ) );
        assertEquals( 2, Bits.bitCount( 3 ) );
        assertEquals( 64, Bits.bitCount( -1 ) );

        assertEquals( "101010", Bits.toBinary( 42 ) );
        assertEquals( 42, Bits.fromBinary( "101010" ) );
        assertEquals( "11111000", Bits.toBinary( 255 ^ 7 ) );

        assertTrue( Bits.hasBit( 5, 0 ) );
        assertFalse( Bits.hasBit( 5, 1 ) );
        assertTrue( Bits.hasBit( 64, 6 ) );
        assertFalse( Bits.hasBit( 63, 6 ) );
        assertTrue( Bits.hasBit( Integer.MAX_VALUE, 30 ) );
        assertFalse( Bits.hasBit( Integer.MAX_VALUE, 31 ) );
        assertTrue( Bits.hasBit( Long.MAX_VALUE, 62 ) );
        assertTrue( Bits.hasBit( -1L, 63 ) );
    }

    private static double maxtedPhase( double t, double period, double t0,
                                       double phase0 ) {
        return ((1 - phase0 + ((t-t0)/period % 1)) % 1) + phase0;
    }

    public void testList() {
        assertEquals( 103, Lists.sum( 1, 3, 99 ) );
        assertEquals( 4, Lists.sum( 1f, 3.0, Double.NaN ) );
        assertEquals( 5, Lists.mean( 2, 4, (byte) 6, 8L ) );
        assertEquals( 100, Lists.mean( 100.5, 99.5, Float.NaN ) );
        assertEquals( 2.8, Lists.variance( 0, 3, 4, 3, 0 ) );
        assertEquals( 2, Lists.variance( 0, 3, Double.NaN, 3, Float.NaN ) );
        assertEquals( 2.8, Lists.stdev( -3, -2, 0, 0, 1, 2, 3, 4, 5, 6 ) );
        assertEquals( Math.PI, Lists.min( Math.PI ) );
        assertEquals( -50, Lists.min( 20, 25, -50., Double.NaN, 101 ) );
        assertEquals( Math.E, Lists.max( Math.E ) );
        assertEquals( 101, Lists.max( 20, 25, -50, Float.NaN, 101 ) );
        assertEquals( -5.25, Lists.median( -5.25 ) );
        assertEquals( 6, Lists.median( -1000000, 5, 7, 8, 6 ) );
    }

    public void testArray() {
        int[] i1 = { -3, -2, 0, 0, 1, 2, 3, 4, 5, 6, };
        long[] l1 = { 6, 5, 4, 3, 2, 1, 0, 0, -2, -3 };
        float[] f1 = { -3, -2, 1, 0, 0, 2, 3, 4, 5, 6 };
        float[] f2 = { -3, -2, 1, Float.NaN, Float.NaN, 2, 3, 4, 5, 6 };
        double[] d1 = { 6, 5, 4, 3, 2, 1, 0, 0, -2, -3 };
        double[] d2 = { 6, 5, 4, 3, 2, 1, Double.NaN, Double.NaN, -2, -3 };
        Object[] a1s = new Object[] { i1, l1, f1, d1, };
        for ( int ia = 0; ia < a1s.length; ia++ ) {
            Object array = a1s[ ia ];
            assertEquals( 10, Arrays.size( array ) );
            assertEquals( 10, Arrays.size( array ) );
            assertEquals( 16.0, Arrays.sum( array ) );
            assertEquals( -3.0, Arrays.minimum( array ) );
            assertEquals( 6.0, Arrays.maximum( array ) );
            assertEquals( 1.6, Arrays.mean( array ) );
            assertEquals( 2.8, Arrays.stdev( array ) );
            assertEquals( 7.84, Arrays.variance( array ) );
            assertEquals( Arrays.minimum( array ),
                          Arrays.quantile( array, 0.0 ) );
            assertEquals( Arrays.maximum( array ),
                          Arrays.quantile( array, 1.0 ) );
        }

        assertEquals( 16.0, Lists.sum( d1 ) );
        assertEquals( -3.0, Lists.min( d1 ) );
        assertEquals( 6.0, Lists.max( d1 ) );
        assertEquals( 1.6, Lists.mean( d1 ) );
        assertEquals( 2.8, Lists.stdev( d1 ) );
        assertEquals( 7.84, Lists.variance( d1 ) );

        Object[] a2s = new Object[] { f2, d2, };
        for ( int ia = 0; ia < a2s.length; ia++ ) {
            Object array = a2s[ ia ];
            assertEquals( 10, Arrays.size( array ) );
            assertEquals( 8, Arrays.count( array ) );
            assertEquals( 16.0, Arrays.sum( array ) );
            assertEquals( -3.0, Arrays.minimum( array ) );
            assertEquals( 6.0, Arrays.maximum( array ) );
            assertEquals( 2.0, Arrays.mean( array ) );
            assertEquals( 3.0, Arrays.stdev( array ) );
            assertEquals( 9.0, Arrays.variance( array ) );
            assertEquals( Arrays.minimum( array ),
                          Arrays.quantile( array, 0.0 ) );
            assertEquals( Arrays.maximum( array ),
                          Arrays.quantile( array, 1.0 ) );
        }

        assertEquals( 16.0, Lists.sum( d2 ) );
        assertEquals( -3.0, Lists.min( d2 ) );
        assertEquals( 6.0, Lists.max( d2 ) );
        assertEquals( 2.0, Lists.mean( d2 ) );
        assertEquals( 3.0, Lists.stdev( d2 ) );
        assertEquals( 9.0, Lists.variance( d2 ) );

        double[][] a5s = new double[][] {
            Arrays.array( 1, 5, 4, 3, Math.E ),
            Arrays.array( 5, 4, 3, Math.E, 1 ),
            Arrays.array( Double.NaN, 1, Math.E, 3, 4, 5 ),
            Arrays.array( Double.NaN, Double.NaN, 1, Math.E, 3, 4, 5 ),
            Arrays.array( 4, 5, Float.NaN, Float.NaN, Float.NaN, 3, Math.E, 1 ),
        };
        for ( int i = 0; i < a5s.length; i++ ) {
            double[] a5 = a5s[ i ];
            assertEquals( 3.0, Arrays.median( a5 ) );
            assertEquals( 1.0, Arrays.quantile( a5, 0 ) );
            assertEquals( 5.0, Arrays.quantile( a5, 1 ) );
            assertEquals( Math.E, Arrays.quantile( a5, 0.25 ), 1e-8 );
        }

        assertEquals( 1, Arrays.median( Arrays.array( 1 ) ) );
        assertEquals( 1, Arrays.median( Arrays.array( 1, 1 ) ) );
        assertEquals( 1, Arrays.median( Arrays.array( 1, 0, 2 ) ) );
        assertEquals( 1, Arrays.median( Arrays.array( 1, 1, 0, 2 ) ) );
        assertEquals( 1, Arrays.median( Arrays.array( 1, 0, 0, 2, 2 ) ) );
        assertEquals( 1, Arrays.median( Arrays.array( 1, 1, 0, 0, 2, 2 ) ) );
        assertEquals( 1, Arrays.median( Arrays.array( 1, 0, 0, 0, 2, 2, 2 ) ) );
        assertEquals( 1, Arrays.median( Arrays
                                       .array( 1, 1, 0, 0, 0, 2, 2, 2 ) ) );

        assertEquals( 2,
                      Arrays.countTrue( new boolean[] { true, false, true } ) );
        assertEquals( 2, Lists.countTrue( true, false, true ) );
        assertEquals( 2, Arrays.count( new Object[] {
                             Float.NaN, Double.NaN, new Integer( 23 ),
                             "abc", null, null
                         } ) );

        assertEquals( "1; 2; 4", Arrays.join( new int[] { 1, 2, 4, }, "; " ) );

        assertArrayEquals( new double[] { 1.5, 3.5, Double.NaN },
                           Arrays.add( new int[] { 1, 2, 3, },
                                       new float[] { .5f, 1.5f, Float.NaN } ) );
        assertArrayEquals( new double[] { 3, 4, Double.NaN },
                           Arrays.add( new float[] { 1f, 2f, Float.NaN, },
                                       2 ) );
        assertArrayEquals( new double[] { 3, 4, Double.NaN },
                           Arrays.add( 2,
                                       new float[] { 1f, 2f, Float.NaN, } ) );
        assertNull( Arrays.add( "no array", new int[] { 1, 3 } ) );
        assertNull( Arrays.add( new int[] { 1, 3 }, "no array" ) );
        assertNull( Arrays.add( new double[ 10 ], new double[ 11 ] ) );

        assertArrayEquals( new double[] { 1.0, 1.75, 2.5 },
                           Arrays.subtract( new int[] { 1, 2, 3 },
                                            new float[] { 0, 0.25f, 0.5f } ) );
        assertArrayEquals( new double[] { 7, 6, 5 },
                           Arrays.subtract( new float[] { 10, 9, 8 },
                                            (short) 3 ) );

        assertArrayEquals( new double[] { 0.5, 3.0, Double.NaN },
                           Arrays.multiply( new int[] { 1, 2, 3, },
                                            new float[] { .5f, 1.5f,
                                                          Float.NaN } ) );
        assertArrayEquals( new double[] { 2, 4, Double.NaN },
                           Arrays.multiply( new float[] { 1f, 2f, Float.NaN, },
                                            2 ) );
        assertArrayEquals( new double[] { 2, 4, Double.NaN },
                           Arrays.multiply( 2,
                                            new float[] { 1f, 2f, Float.NaN} ));
        assertNull( Arrays.multiply( "no array", new int[] { 1, 3 } ) );
        assertNull( Arrays.multiply( new int[] { 1, 3 }, "no array" ) );

        assertEquals( 26, Arrays.dotProduct( new double[] { 3, 4, 5 },
                                             new short[] { 1, 2, 3 } ) );
        assertTrue( Double.isNaN( Arrays.dotProduct( new float[ 2 ],
                                                     new float[ 3 ] ) ) );

        assertArrayEquals( new double[] { 3, 4, 3 },
                           Arrays
                          .condition( new boolean[] { true, false, true },
                                      3, 4 ) );
        assertArrayEquals( new double[] { 1, 0.5, 4 },
                           Arrays.reciprocal( new float[] { 1, 2, 0.25f, } ) );
        assertArrayEquals( new double[] { 1, 0.5, 4 },
                           Arrays.divide( 1, new float[] { 1, 2, 0.25f, } ) );
        assertArrayEquals( new double[] { 0, 3, 0.5 },
                           Arrays.divide( new short[] { 0, 9, 4 },
                                          new double[] { 1, 3, 8 } ) );

        assertArrayEquals( new double[] { 1.0, Float.NaN, Math.PI },
                           Arrays.array( 1, Double.NaN, Math.PI ) );
        assertArrayEquals( new String[] { "Geddy", "Neil", "Alex", null },
                           Arrays.stringArray( "Geddy", "Neil", "Alex",
                                               null ) );
        assertArrayEquals( new int[] { 7, 10, 12 },
                           Arrays.intArray( 7, 10, 12 ) );

        assertArrayEquals( new int[] { 0, 1, 2, 3 }, Arrays.sequence( 4 ) );

        double[] da = new double[] { 10., 11., 12., 13., 14., 15., };
        int[] ia = new int[] { 10, 11, 12, 13, 14, 15, };
        String[] sa = new String[] { "Tess", "Armadillo", "Scout", };

        assertArrayEquals( new double[] { 10, 11, 12 },
                           Arrays.slice(Arrays.array(10,11,12,13), 0, 3) );
        assertArrayEquals( new double[] { 12, 13 },
                           Arrays.slice(Arrays.array(10,11,12,13), -2, 999) );
        assertArrayEquals( new String[] { "A", "B", "C", },
               Arrays.slice(Arrays.stringArray("A","B","C","D"), 0, 3));
        assertArrayEquals( new String[] { "C", "D", },
               Arrays.slice(Arrays.stringArray("A","B","C","D"),-2,999));

        assertArrayEquals( new double[] { 11, 12, 13, 14 },
                           Arrays.slice( da, 1, -1 ) );
        assertArrayEquals( new int[] { 10, 11, 12, 13, },
                           Arrays.slice( ia, -99, -2 ) );
        assertArrayEquals( new String[ 0 ],
                           Arrays.slice( sa, 2, 1 ) );
        assertArrayEquals( new String[] { "Tess", },
                           Arrays.slice( sa, 0, 1 ) );

        assertArrayEquals( new double[] { 10, 13 },
                           Arrays.pick(Arrays.array(10,11,12,13), 0, 3) );
        assertArrayEquals( new double[] { 13, 12, 11 },
                           Arrays.pick(Arrays.array(10,11,12,13), -1, -2, -3 ));
        assertArrayEquals( new int[] { 10, 13 },
                           Arrays.pick(Arrays.intArray(10,11,12,13), 0, 3));
        assertArrayEquals( new int[] { 13, 12, 11 },
              Arrays.pick( Arrays.intArray(10,11,12,13),-1,-2,-3));
        assertArrayEquals( new String[] { "A", "D", },
              Arrays.pick(Arrays.stringArray("A","B","C","D"), 0, 3 ));
        assertArrayEquals( new String[] { "D", "C", "B", },
              Arrays.pick(Arrays.stringArray("A","B","C","D"),-1,-2,-3));

        assertArrayEquals( new double[] { 11, 15, 12 },
                           Arrays.pick( da, 1, 5, 2 ) );
        assertArrayEquals( new int[] { 11, 15, 12 },
                           Arrays.pick( ia, 1, 5, 2 ) );
        assertArrayEquals( new double[] { 15, 14, 13 },
                           Arrays.pick( da, -1, -2, -3 ) );
        try {
            assertArrayEquals( null, Arrays.pick( da, -99 ) );
            fail();
        }
        catch ( IndexOutOfBoundsException e ) {
        }
        try {
            assertArrayEquals( null, Arrays.pick( ia, 99 ) );
            fail();
        }
        catch ( IndexOutOfBoundsException e ) {
        }
        assertArrayEquals( new String[] { "Armadillo", "Scout", },
                           Arrays.pick( sa, 1, -1 ) );

        assertEquals( 1, Arrays.indexOf( sa, "Armadillo" ) );
        assertEquals( -1, Arrays.indexOf( sa, "Housepop" ) );
        assertEquals( 5, Arrays.indexOf( da, 15 ) );
        assertEquals( -1, Arrays.indexOf( da, Double.NaN ) );
        assertEquals( -1, Arrays.indexOf( da, 99.e9 ) );
        assertEquals( 5, Arrays.indexOf( ia, 15 ) );
        assertEquals( -1, Arrays.indexOf( ia, (short) -1 ) );

        assertEquals( 1, Arrays.indexOf(Arrays.stringArray("QSO", "BCG", "SNR"),
                                        "BCG"));
        assertEquals(-1, Arrays.indexOf(Arrays.stringArray("QSO", "BCG", "SNR"),
                                        "TLA"));

        assertArrayEquals( new double[] { 5.5, 5.5, 5.5, 5.5, },
                           Arrays.constant( 4, 5.5 ) );
        assertArrayEquals( new int[] { 23, 23, 23 },
                           Arrays.constant( 3, 23 ) );

        assertArrayEquals( new int[] { 1, 11, 102 },
            Arrays.intArrayFunc( "x+i", new int[] { 1,10,100 }));
        assertArrayEquals( new int[] { 0, -1, -2, -3, -4 },
            Arrays.intArrayFunc( "-x", Arrays.sequence( 5 ) ) );

        assertArrayEquals( new double[] { 101., 102., 103. },
            Arrays.arrayFunc( "100.+x", new int[] { 1,2,3 } ) );
        assertArrayEquals( new double[] { 0, 3, 6, 9, Double.NaN },
            Arrays.arrayFunc( "3*x", new double[] { 0,1,2,3,Double.NaN } ) );
        assertArrayEquals( new double[] { 1.5, 2.5, 4.5, 8.5 },
            Arrays.arrayFunc( "pow(2,i)+x", new double[] { .5,.5,.5,.5 } ) );
    }

    public void testConversions() {
        assertEquals( (byte) 99, Conversions.parseByte( "99" ) );
        assertEquals( (byte) 23, Conversions.parseByte( " 23 " ) );
        try {
            Conversions.parseByte( "999" );
            fail();
        }
        catch ( NumberFormatException e ) {
        }

        assertEquals( (short) 30000, Conversions.parseShort( "30000" ) );
        assertEquals( (short) 23, Conversions.parseShort( " 23" ) );
        try {
            Conversions.parseShort( "40000" );
            fail();
        }
        catch ( NumberFormatException e ) {
        }

        assertEquals( 70000, Conversions.parseInt( "70000" ) );
        assertEquals( 23, Conversions.parseInt( "23 " ) );
        try {
            Conversions.parseInt( "7777777777777777" );
            fail();
        }
        catch ( NumberFormatException e ) {
        }

        assertEquals( -1L, Conversions.parseLong( "-1" ) );
        assertEquals( 23L, Conversions.parseLong( " 23" ) );
        try {
            Conversions.parseLong( "tits" );
            fail();
        }
        catch ( NumberFormatException e ) {
        }

        assertEquals( 101.5f, Conversions.parseFloat( "1015e-1" ) );
        assertEquals( 23.0f, Conversions.parseFloat( " +23" ) );

        assertEquals( 101.5, Conversions.parseDouble( "1015e-1" ) );
        assertEquals( 23.0, Conversions.parseFloat( " 23 " ) );
        try {
            Conversions.parseDouble( "No doubles here mate" );
            fail();
        }
        catch ( NumberFormatException e ) {
        }

        assertArrayEquals( new int[] { 9, 8, -23, },
                           Conversions.parseInts( "9 8 -23" ) );
        assertEquals( 0, Conversions.parseInts( "tiddly-pom" ).length );

        assertArrayEquals( new double[] { 1.3, 990.0, Double.NaN, -23.0 },
                           Conversions.parseDoubles( "1.3, 99e1, NaN, -23" ) );
        assertArrayEquals( new double[] { 0.8, 2.1, 9.0, 2.1, 6.2, 8.6 },
             Conversions.parseDoubles("POLYGON(0.8, 2.1, 9.0, 2.1, 6.2, 8.6)"));
        assertArrayEquals( new double[] { 0.8, 2.1, 9.0, 2.1, 6.2, 8.6 },
             Conversions.parseDoubles("Polygon ICRS 0.8 2.1 9.0 2.1 6.2 8.6"));
        assertEquals( 0, Conversions.parseDoubles( "La la la" ).length );

        assertEquals( (byte) 3, Conversions.toByte( 3.99 ) );
        try {
            Conversions.toByte( -190 );
            fail();
        }
        catch ( NumberFormatException e ) {
        }

        assertEquals( (short) 3, Conversions.toShort( 3.901f ) );
        try {
            Conversions.toShort( Integer.MAX_VALUE );
            fail();
        }
        catch ( NumberFormatException e ) {
        }

        assertEquals( 300, Conversions.toInteger( 300 ) );
        try {
            Conversions.toInteger( Float.POSITIVE_INFINITY );
            fail();
        }
        catch ( NumberFormatException e ) {
        }

        assertEquals( (long) -25, Conversions.toLong( (short) -25 ) );
        try {
            Conversions.toLong( Double.NaN );
            fail();
        }
        catch ( NumberFormatException e ) {
        }

        assertEquals( 2.75f, Conversions.toFloat( 2.75 ) );

        assertEquals( -Math.PI, Conversions.toDouble( -Math.PI ) );

        assertEquals( "101", Conversions.toString( (byte) 101 ) );
        assertEquals( "101", Conversions.toString( (short) 101 ) );
        assertEquals( "101", Conversions.toString( (int) 101 ) );
        assertEquals( "101", Conversions.toString( (long) 101 ) );
        assertEquals( "101", Conversions.toString( (float) 101 ) );
        assertEquals( "101", Conversions.toString( (double) 101 ) );

        assertEquals( "2a", Conversions.toHex( 42 ) );
        assertEquals( 42, Conversions.fromHex( "2a" ) );
        assertEquals( 42, Conversions.fromHex( "2A" ) );
        assertEquals( 42, Conversions.fromHex( " 2a" ) );
        assertEquals( 42, Conversions.fromHex( "2a " ) );

        assertEquals( 101, Conversions.parseBigDecimal( "101" ).doubleValue() );
        assertEquals( 101, Conversions.parseBigInteger( "101" )
                                      .intValueExact() );
        assertEquals( -2e19,
                      Conversions
                     .parseBigInteger("-20000000000000000023").doubleValue() );
        assertTrue( Conversions.parseBigInteger("18446744073709551616")
                               .testBit(64) );

        assertEquals( -1, Conversions.parseBigDecimal("101")
                         .compareTo(Conversions.parseBigDecimal("102")) );
    }

    public void testDistances() {

        /* The expected values here have been obtained by screen-scraping
         * results from Ned Wright's Cosmology Calculator at
         * http://www.astro.ucla.edu/~wright/CosmoCalc.html. */
        compareDistances( 3.0, 71.0, 0.270, 0.730,
                          11.476, 6460.6, 1129.454, 1615.1, 25841.7 );
        compareDistances( 0.1, 71.0, 0.270, 0.730,
                          1.286, 413.5, 0.296, 375.9, 454.8 );
        compareDistances( 5.5, 100.0, 1.0, 0.0,
                          6.125, 3643.9, 202.672, 560.6, 23685.3 );
        compareDistances( 2.0, 60.0, 0.05, 0.0,
                          10.694, 5380.8, 812.374, 2141.5, 19273.6 );
        compareDistances( 1.7, 75, 0.6, 0.6,
                          8.069, 3837.2, 228.082, 1377.9, 10044.9 );
    }

    public void compareDistances( double z, double h0, double oM, double oL,
                                  double lookbackTime,
                                  double comovingDistanceL,
                                  double comovingVolume,
                                  double angularDiameterDistance,
                                  double luminosityDistance ) {
        double delta = 1e-3;
        assertEquals( 1.0, lookbackTime
               / Distances.lookbackTime( z, h0, oM, oL ), delta );
        assertEquals( 1.0, comovingDistanceL
               / Distances.comovingDistanceL( z, h0, oM, oL ), delta );
        assertEquals( 1.0, comovingVolume
               / Distances.comovingVolume( z, h0, oM, oL ), delta );
        assertEquals( 1.0, angularDiameterDistance
               / Distances.angularDiameterDistance( z, h0, oM, oL ), delta );
        assertEquals( 1.0, luminosityDistance
               / Distances.luminosityDistance( z, h0, oM, oL ), delta );
    }

    public void testFluxes() {
        assertEquals( 21.4, Fluxes.janskyToAb( 10e-6 ), 1e-8 );
        assertEquals( 10e-6, Fluxes.abToJansky( 21.4 ), 1e-16 );
        assertEquals( 1, Fluxes.luminosityToFlux( 4.0 * Math.PI, 1 ) );
        double dist = 99;
        for ( int i = 1; i < 40; i++ ) {
            double mag = i * 1.0;
            double flux = i * 1e-6;
            assertEquals( 1.0,
                          Fluxes.janskyToAb( Fluxes.abToJansky( mag ) ) / mag,
                          1e-12 );
            assertEquals( 1.0,
                          Fluxes.abToJansky( Fluxes.janskyToAb( flux ) ) / flux,
                          1e-12 );
            assertEquals( 1.0,
                Fluxes.luminosityToFlux( Fluxes.fluxToLuminosity( flux, dist ),
                                         dist ) / flux,
                1e-12 );
            assertEquals( 1.0,
                Fluxes.fluxToLuminosity( Fluxes.luminosityToFlux( flux, dist ),
                                         dist ) / flux,
                1e-12 );
        }
    }

    public void testFormats() {
        assertEquals( "3.", Formats.formatDecimal( Math.PI, 0 ) );
        assertEquals( ".0000000000", Formats.formatDecimal( 0, 10 ) );
        assertEquals( "27.183", Formats.formatDecimal( Math.E * 10, 3 ) );

        assertEquals( "99.000", Formats.formatDecimal( 99, "#.000" ) );
        assertEquals( "+3.14",
                      Formats.formatDecimal( Math.PI, "+0.##;-0.##" ) );
        Locale locale = Locale.getDefault();
        Locale.setDefault( Locale.ENGLISH );
        Formats.reset();
        assertEquals( "99.000", Formats.formatDecimalLocal( 99, 3 ) );
        assertEquals( "99.000", Formats.formatDecimalLocal( 99, "#.000" ) );
        Locale.setDefault( Locale.FRENCH );
        Formats.reset();
        assertEquals( "99,000", Formats.formatDecimalLocal( 99, 3 ) );
        assertEquals( "99,000", Formats.formatDecimalLocal( 99, "#.000" ) );
        Locale.setDefault( locale );
        Formats.reset();
    }
 
    public void testMaths() {
        assertEquals( 3.1415, Maths.PI, 0.01 );
        assertEquals( 2.718, Maths.E, 0.01 );

        assertEquals( Math.acos( 0.1 ), Maths.acos( 0.1 ) );
        assertEquals( Math.asin( 0.1 ), Maths.asin( 0.1 ) );
        assertEquals( Math.atan( 0.1 ), Maths.atan( 0.1 ) );
        assertEquals( Math.atan2( 0.2, 0.4 ), Maths.atan2( 0.2, 0.4 ) );

        assertEquals( 0.0, Maths.cos( Maths.PI / 2. ), TINY );
        assertEquals( 0.0, Maths.sin( 0.0 ), TINY );
        assertEquals( 0.0, Maths.tan( 0.0 ), TINY );

        assertEquals( Maths.E, Maths.exp( 1.0 ), TINY );
        assertEquals( 1.0, Maths.ln( Maths.E ), TINY );
        assertEquals( 2.0, Maths.log10( 100.0 ), TINY );
        assertEquals( 1000, Maths.exp10( 3 ), TINY );
        assertEquals( 0.001, Maths.exp10( -3 ), TINY );
        assertEquals( Maths.PI, Maths.exp10( Maths.log10( Maths.PI ) ), TINY );
        assertEquals( Maths.E, Maths.log10( Maths.exp10( Maths.E ) ), TINY );

        assertEquals( 256.0, Maths.pow( 2, 8 ) );

        assertEquals( 32.0, Maths.sqrt( 1024.0 ) );

        assertEquals( 5.0, Maths.hypot( 3, -4 ) );
        assertEquals( 4.0, Maths.hypot( 2, 2, 2, -2 ) );
        assertEquals( Math.E, Maths.hypot( Math.E ) );
       
        double delta = 1e-7;
        for ( int i = 0; i < 1000; i++ ) {
            double theta = ( RANDOM.nextDouble() - 0.5 ) * Maths.PI;
            assertEquals( 1.0, Maths.pow( Maths.sin( theta ), 2 ) +
                               Maths.pow( Maths.cos( theta ), 2 ), delta );
            assertEquals( theta, Maths.asin( Maths.sin( theta ) ), delta );
            assertEquals( Arithmetic.abs( theta ), 
                          Maths.acos( Maths.cos( theta ) ), delta );
            assertEquals( theta, Maths.asinh( Maths.sinh( theta ) ), delta );
            assertEquals( Arithmetic.abs( theta ),
                          Maths.acosh( Maths.cosh( theta ) ), delta );
            assertEquals( theta, Maths.atanh( Maths.tanh( theta ) ), delta );
        }
    }

    public void testTrigDegrees() {
        assertEquals( Math.toDegrees( Math.acos( 0.1 ) ),
                      TrigDegrees.acosDeg( 0.1 ), TINY );
        assertEquals( Math.toDegrees( Math.asin( 0.1 ) ),
                      TrigDegrees.asinDeg( 0.1 ), TINY );
        assertEquals( Math.toDegrees( Math.atan( 0.1 ) ),
                      TrigDegrees.atanDeg( 0.1 ), TINY );
        assertEquals( Math.toDegrees( Math.atan2( 0.2, 0.4 ) ),
                      TrigDegrees.atan2Deg( 0.2, 0.4 ), TINY );

        assertEquals( 0.0, TrigDegrees.cosDeg( 90 ), TINY );
        assertEquals( 1.0, TrigDegrees.sinDeg( 90 ), TINY );
        assertEquals( 0.0, TrigDegrees.tanDeg( 180 ), TINY );
    }

    public void testStrings() {
        assertEquals( "00023", Strings.padWithZeros( 23, 5 ) );
        assertEquals( "23", Strings.padWithZeros( 23, 2 ) );
        assertEquals( "23", Strings.padWithZeros( 23, 1 ) );

        assertEquals( "starlink", Strings.concat( "star", "link" ) );
        assertEquals( "star", Strings.concat( "star", null ) );
        assertEquals( "link", Strings.concat( "", "link" ) );
        assertEquals( "1223334444",
                      Strings.concat( "1", 22, "", "333", null, "4444" ) );

        assertEquals( "A big gale", Strings.join( " ", "A", "big", "gale" ) );
        assertEquals( "one, 2, 3.0, 4",
                      Strings.join( ", ", "one", "2", 3.0, 4L ) );

        assertEquals( "One2Three4.0Five999",
                      Strings.concat( "One", new Integer( 2 ), "Three",
                                      new Double( 4.0 ), "Five", null,
                                      new Long( 999L ) ) );
        assertEquals( "One2Three4.0Five999",
                      Strings.concat( "One", 2, "Three", 4.0, "Five",
                                      null, 999L ) );

        assertTrue( Strings.contains( "awkward", "awk" ) );
        assertTrue( Strings.contains( "hawkwind", "awk" ) );
        assertTrue( Strings.contains( "gawk", "awk" ) );
        assertTrue( ! Strings.contains( "pork", "awk" ) );

        assertTrue( Strings.endsWith( "parsed", "sed" ) );
        assertTrue( ! Strings.endsWith( "compiled", "sed" ) );

        assertTrue( Strings.startsWith( "seditious", "sed" ) );
        assertTrue( ! Strings.startsWith( "rebellious", "sed" ) );

        assertTrue( Strings.equalsIgnoreCase( "StarLink", "Starlink" ) );
        assertTrue( ! Strings.equalsIgnoreCase( "Starlink", "AstroGrid" ) );

        assertEquals( 4, Strings.length( "Mark" ) );
        assertEquals( 0, Strings.length( "" ) );

        assertEquals( "28948", Strings.matchGroup("NGC28948b","NGC([0-9]*)") );

        assertTrue( Strings.matches( "Hubble", "ub" ) );

        assertEquals( "1x2x3x4", 
                      Strings.replaceAll( "1-2--3---4", "--*", "x" ) );

        assertEquals( "M-61", 
                      Strings.replaceFirst("Messier 61", "Messier ", "M-") );

        assertEquals( "laxy", Strings.substring( "Galaxy", 2 ) );
 
        assertEquals( "lax", Strings.substring( "Galaxy", 2, 5 ) );

        assertEquals( "universe", Strings.toLowerCase( "Universe" ) );

        assertEquals( "UNIVERSE", Strings.toUpperCase( "Universe" ) );

        assertEquals( "some text", Strings.trim( "  some text  " ) );
        assertEquals( "some text", Strings.trim( "some text" ) );

        assertEquals( "RR%20Lyr", URLs.urlEncode( "RR Lyr" ) );
        assertEquals( "RR Lyr", URLs.urlDecode( "RR%20Lyr" ) );

        // These examples from sec 3.5.1 of IAU-endorsed document
        // "Specifications concerning designations" at
        // http://cds.u-strasbg.fr/vizier/Dic/iau-spec.htx
        checkDesig( "14:26:48", "+69:50:00", "RX J1426.8+6950" );
        checkDesig( "13:02:00", "-63:50:00", "PSR J1302-6350" );
        checkDesig( "17:49:37", "-28:03:52", "PN G001.2-00.3" );

        checkDesig( "00:48:00", "-42:42:00", "QSO J0048-427" );
        checkDesig( "00:51:08", "-42:26:29", "QSO 004848-4242.8" );
        checkDesig( "12:00:00", "+45:00:00", "PSR J120000.0+450000.0" );
        checkDesig( "12:02:33", "+44:43:18", "PSR B120000.0+450000.0" );
        checkDesig( "12:02:33", "+44:43:18", "PSR 120000.0+450000.0 (O-RLY)" );

        assertEquals( null, Strings.desigToIcrs( "NGC 4993" ) );
        assertTrue( Double.isNaN( Strings.desigToRa( "HIP Z190012-230210" ) ) );
        assertTrue( Double.isNaN( Strings.desigToDec( "TYC J190012230210" ) ) );

        assertEquals( "https://ui.adsabs.harvard.edu/abs/2018A%26A...616A...2L",
                      URLs.bibcodeUrl( "2018A&A...616A...2L" ) );
        assertNull( URLs.bibcodeUrl( null ) );
        assertNull( URLs.bibcodeUrl( "Fredor" ) );
        assertEquals( "https://doi.org/10.3390/informatics4030018",
                      URLs.doiUrl( "10.3390/informatics4030018" ) );
        assertEquals( "https://doi.org/10.3390/informatics4030018",
                      URLs.doiUrl( "doi:10.3390/informatics4030018" ) );
        assertNull( URLs.doiUrl( "Fredor" ) );
        assertNull( URLs.doiUrl( null ) );
        assertEquals( "https://arxiv.org/abs/1804.09379",
                      URLs.arxivUrl( "1804.09379" ) );
        assertEquals( "https://arxiv.org/abs/1804.09379",
                      URLs.arxivUrl( "arXiv:1804.09379" ) );
        assertEquals( "https://arxiv.org/abs/1110.0528",
                      URLs.arxivUrl( "arxiv:1110.0528" ) );
        assertNull( URLs.arxivUrl( null ) );
        assertNull( URLs.arxivUrl( "Fredor" ) );

        assertEquals( "http://simbad.u-strasbg.fr/simbad/sim-id?Ident="
                    + "Beta%20Pictoris",
                      URLs.simbadUrl( "Beta Pictoris" ) );
        assertNull( URLs.simbadUrl( null ) );
        assertNull( URLs.simbadUrl( " " ) );
        assertEquals( "http://ned.ipac.caltech.edu/byname?objname=NGC%203952",
                      URLs.nedUrl( "NGC 3952" ) );
        assertNull( URLs.nedUrl( null ) );
        assertNull( URLs.nedUrl( "    " ) );

        assertEquals( "http://alasky.u-strasbg.fr/hips-image-services/hips2fits"
                    + "?hips=CDS/P/DSS2/color&format=png"
                    + "&ra=56.75&dec=24.1125"
                    + "&fov=1.50&width=300&height=300&projection=SIN",
                      URLs.hips2fitsUrl( "CDS/P/DSS2/color", "png",
                                         56.75, 24.1125, 1.5, 300 ) );
    }

    public void testURLs() {
        assertEquals( "http://x.org/?a=1&b=two&c=3%264",
                      URLs.paramsUrl( "http://x.org/",
                                      "a", "1", "b", "two", "c", "3&4" ) );
    }

    private void checkDesig( String raSex, String decSex, String desig ) {
        assertEquals( raSex,
                      CoordsDegrees
                     .degreesToHms( Strings.desigToRa( desig ) ) );
        assertEquals( decSex,
                      CoordsDegrees
                     .degreesToDms( Strings.desigToDec( desig ) ) );
    }

    public void testCoordsRadians() {

        assertEquals( 180.0, CoordsRadians.radiansToDegrees( Maths.PI ), TINY );
        assertEquals( 0.0, CoordsRadians.radiansToDegrees( 0.0 ) );

        assertEquals( Maths.PI, CoordsRadians.degreesToRadians( 180.0 ), TINY );
        assertEquals( 0.0, CoordsRadians.degreesToRadians( 0.0 ) );

        assertEquals( 0.0, CoordsRadians
                          .skyDistanceRadians( 1.4, 2.1, 1.4, 2.1 ), TINY );
        assertEquals( Maths.PI / 2.0, 
                      CoordsRadians
                     .skyDistanceRadians( -1, 0.0, -1, Maths.PI / 2.0 ), TINY );

        assertEquals( Maths.PI / 2.0,
                      CoordsRadians.posAngRadians( 0, 0, 0.1, 0 ), TINY );
        assertEquals( - Maths.PI / 2.0,
                      CoordsRadians.posAngRadians( 0.1, 0, -0.1, 0 ), TINY );
        assertEquals( 0.0, CoordsRadians.posAngRadians( 1, .2, 1, .3 ), TINY );

        assertEquals( 2.0,
                      CoordsDegrees.polarDistanceDegrees( 50.5, -29, 1,
                                                          50.5, -29, 3 ),
                      TINY );
        assertEquals( 4.0,
                      CoordsDegrees.polarDistanceDegrees(  23, +18.5, 2,
                                                          203, -18.5, 2 ),
                      TINY );

        double ra1 = 0.1;
        double dec1 = 1.2;
        double ra2 = 0.2;
        double dec2 = 1.3;
        assertEquals( CoordsRadians.skyDistanceRadians( ra1, dec1, ra2, dec2 ),
                      CoordsRadians.skyDistanceRadians( ra2, dec2, ra1, dec1 ));
        assertEquals(
            CoordsRadians
           .radiansToDegrees( CoordsRadians.skyDistanceRadians( ra1, dec1,
                                                                ra2, dec2 ) ),
            CoordsDegrees
           .skyDistanceDegrees( CoordsRadians.radiansToDegrees( ra1 ),
                                CoordsRadians.radiansToDegrees( dec1 ),
                                CoordsRadians.radiansToDegrees( ra2 ),
                                CoordsRadians.radiansToDegrees( dec2 ) ),
            TINY );

        assertEquals( Maths.PI / 4.0,
                      CoordsRadians.hmsToRadians( "03:00:00.0" ),
		      TINY );
        assertEquals( -Maths.PI / 4.0,
                      CoordsRadians.hmsToRadians( "-03: 0:0" ),
		      TINY );
        assertEquals( CoordsRadians.hmsToRadians( "0 0 1" ),
                      -CoordsRadians.hmsToRadians( "-0h0m1s" ),
		      TINY );

        assertEquals( "03:00:00",
                      CoordsRadians.radiansToHms( Maths.PI / 4.0 ) );
        assertEquals( "12:00:00.000",
                      CoordsRadians.radiansToHms( -Maths.PI, 3 ) );
        assertEquals( "11:59:59.500", 
                      CoordsRadians.radiansToHms( -Maths.PI*(1+.5000/12/60/60),
                                                  3 ) );
        assertEquals( "12:00:00.500", 
                      CoordsRadians.radiansToHms( -Maths.PI*(1-.5000/12/60/60),
                                                  3 ) );
        assertEquals( "12:00:00.005", 
                      CoordsRadians.radiansToHms( Maths.PI*(1+.0050/12/60/60),
                                                  3 ) );
        assertEquals( "12:00:00.500", 
                      CoordsRadians.radiansToHms( Maths.PI*(1+.5004/12/60/60),
                                                  3 ) );
        assertEquals( "12:00:00.500", 
                      CoordsRadians.radiansToHms( Maths.PI*(1+.4996/12/60/60),
                                                  3 ) );

        assertEquals( Maths.PI / 4.0,
                      CoordsRadians.dmsToRadians( "45:00:00.0" ) );
        assertEquals( -Maths.PI / 4.0,
                      CoordsRadians.dmsToRadians( "-45: 0:0" ) );
        assertEquals( CoordsRadians.dmsToRadians( "0 0 1" ),
                      -CoordsRadians.dmsToRadians( "-0d0m1s" ) );

        assertEquals( "+45:00:00",
                      CoordsRadians.radiansToDms( Maths.PI / 4.0 ) );
        assertEquals( "-90:00:00.000",
                      CoordsRadians.radiansToDms( -Maths.PI / 2, 3 ) );
        assertEquals( "-45:00:00.500",
                      CoordsRadians.radiansToDms( -Maths.PI / 4.0 
                                                  * (1+.5000/45/60/60), 3 ) );
        assertEquals( "-45:00:00.005",
                      CoordsRadians.radiansToDms( -Maths.PI / 4.0 
                                                  * (1+.0050/45/60/60), 3 ) );
        assertEquals( "-45:00:00.500",
                      CoordsRadians.radiansToDms( -Maths.PI / 4.0 
                                                  * (1+.5004/45/60/60), 3 ) );
        assertEquals( "-45:00:00.500",
                      CoordsRadians.radiansToDms( -Maths.PI / 4.0
                                                  * (1+.4996/45/60/60), 3 ) );

        double ra1950 = 2.1;
        double de1950 = 1.2;
        double ra2000 = CoordsRadians.raFK4toFK5radians( ra1950, de1950 );
        double de2000 = CoordsRadians.decFK4toFK5radians( ra1950, de1950 );
        assertEquals( ra1950, ra2000, 0.03 );
        assertEquals( de1950, de2000, 0.03 );
        assertEquals( ra1950, CoordsRadians.raFK5toFK4radians( ra2000, de2000 ),
                      1e-9 );
        assertEquals( de1950, CoordsRadians.decFK5toFK4radians( ra2000, de2000),
                      1e-9 );
    }

    public void testCoordsDegrees() {
        assertEquals( 0.0, CoordsDegrees
                          .skyDistanceDegrees( 1.4, 2.1, 1.4, 2.1 ), TINY );
        assertEquals( 90, CoordsDegrees.skyDistanceDegrees( -23, 0, -23, 90 ),
                      TINY );
        double ra1 = 195;
        double dec1 = -28;
        double ra2 = 38;
        double dec2 = +88;
        assertEquals( CoordsDegrees.skyDistanceDegrees( ra1, dec1, ra2, dec2 ),
                      CoordsDegrees.skyDistanceDegrees( ra2, dec2, ra1, dec1 ));

        assertEquals( +90, CoordsDegrees.posAngDegrees( 0, 0, 15, 0 ), TINY );
        assertEquals( -90, CoordsDegrees.posAngDegrees( 15, 0, -15, 0 ), TINY );
        assertEquals( 0., CoordsDegrees.posAngDegrees( 35, 8, 35, 12 ), TINY );

        assertEquals( 45, CoordsDegrees.hmsToDegrees( "03:00:00.0" ), TINY );
        assertEquals( -45, CoordsDegrees.hmsToDegrees( "-03: 0:0" ), TINY );
        assertEquals( CoordsDegrees.hmsToDegrees( "0 0 1" ),
                      -CoordsDegrees.hmsToDegrees( "-0h0m1s" ), TINY );

        assertEquals( "03:00:00", CoordsDegrees.degreesToHms( 45 ) );
        assertEquals( "12:00:00.000", CoordsDegrees.degreesToHms( -180, 3 ) );

        assertEquals( 45, CoordsDegrees.dmsToDegrees( "45:00:00.0" ) );
        assertEquals( -45, CoordsDegrees.dmsToDegrees( "-45: 0:0" ) );
        assertEquals( CoordsDegrees.dmsToDegrees( "0 0 1" ),
                      -CoordsDegrees.dmsToDegrees( "-0d0m1s" ) );

        assertEquals( "+45:00:00", CoordsDegrees.degreesToDms( 45 ) );
        assertEquals( "-90:00:00.000", CoordsDegrees.degreesToDms( -90, 3 ) );
    }

    public void testShapes() {
        assertTrue( Shapes.isInside(0.5,0.5, 0,0, 0,1, 1,1, 1,0) );
        assertFalse( Shapes.isInside(0,0, Arrays.array(10,20, 20,20, 20,10)) );

        assertEquals( 23, Shapes.polyLine( -100, new double[] { -5,23 } ) );
        assertEquals( 23, Shapes.polyLine( +100, new double[] { -5,23 } ) );
        assertEquals( 23, Shapes.polyLine( -5, new double[] { -5,23 } ) );

        assertEquals( 1, Shapes.polyLine( 1, new double[] { 0,0, 2,2, } ) );
        assertEquals( -1, Shapes.polyLine( -1, new double[] { Math.PI,Math.PI,
                                                              -1e9,-1e9 } ) );
        assertEquals( -2000, Shapes.polyLine( 1000, -5,10., 5,-10 ) );

        double[] line = new double[] { 0,0, 1,1, 3,-1, 10,-1 };
        for ( int i = 0; i < line.length / 2; i++ ) {
            assertEquals( line[ 2 * i + 1 ],
                          Shapes.polyLine( line[ 2 * i + 0 ], line ) );
        }
        assertEquals( -5, Shapes.polyLine( -5, line ) );
        assertEquals( 0.1, Shapes.polyLine( 0.1, line ) );
        assertEquals( 0.99, Shapes.polyLine( 0.99, line ) );
        assertEquals( 0, Shapes.polyLine( 2, line ) );
        assertEquals( -1, Shapes.polyLine( 4, line ) );
        assertEquals( -1, Shapes.polyLine( 1e100, line ) );

        double[] tri = new double[] { -1,-1, 1,1, -1,2, };
        assertTrue( Shapes.isInside( -0.1, 0, tri ) );
        assertTrue( !Shapes.isInside( +0.1, 0, tri ) );
        assertTrue( Shapes.isInside( 0, 1, tri ) );
        assertTrue( !Shapes.isInside( 0, 2, tri ) );
        assertTrue( !Shapes.isInside( 0, -1, tri ) );
        assertTrue( !Shapes.isInside( 1, 0, tri ) );
    }

    public void testTilings() {
        final double pi4 = 4.0 * Math.PI;
        assertEquals( pi4, Tilings.healpixSteradians( 0 ) * 12 );
        assertEquals( pi4, Tilings.healpixSteradians( 2 ) * 12 * 4 * 4 );
        assertEquals( 360*360/Math.PI, Tilings.healpixSqdeg( 0 ) * 12 );
        assertEquals( Tilings.healpixResolution( 9 ),
                      Math.sqrt( Tilings.healpixSqdeg( 9 ) ) );
        assertEquals( pi4, Tilings.sqdegToSteradians( 129600 / Math.PI ),
                      1e-6 );
        assertEquals( 41253, Tilings.steradiansToSqdeg( pi4 ), 1. );
        assertEquals( 279401, Tilings.healpixNestIndex( 8, 23, -12 ) );
        assertEquals( -12.0247, Tilings.healpixNestLat( 8, 279401 ), 1e-4 );
        assertEquals( 23.0273, Tilings.healpixNestLon( 8, 279401 ), 1e-4 );
        assertEquals( 111, Tilings.healpixRingToNest( 2, 48 ) );
        assertEquals( 48, Tilings.healpixNestToRing( 2, 111 ) );
        for ( int ik = 0; ik < 4; ik++ ) {
            long npix = 12L << 2*ik;
            for ( long ip = 0; ip < npix; ip++ ) {
                checkHealpix( ik, ip );
            }
        }
        checkHealpix( 2, 23 );
        checkHealpix( 9, 1110787 );
    }

    public void testVO() {
        assertEquals( "OK", VO.ucdStatus( "pos.eq.ra;meta.main" ) );
        assertEquals( "VOX", VO.ucdStatus( "VOX:Image_Naxes" ) );
        assertEquals( "BAD_SEQUENCE", VO.ucdStatus( "meta.main;pos.eq.ra" ) );
        assertNull( VO.ucdMessage( "pos.eq.ra;meta.main" ) );
        assertTrue( VO.ucdMessage( "not a ucd" ).trim().length() > 0 );

        assertEquals( "OK", VO.vounitStatus( "kg/m**2" ) );
        assertEquals( "BAD_SYNTAX", VO.vounitStatus( "kg/m^2" ) );
        assertEquals( "UNKNOWN_UNIT", VO.vounitStatus( "bag/fortnight" ) );
        assertEquals( "WHITESPACE", VO.vounitStatus( "kg / m**2" ) );
        assertEquals( "OK", VO.vounitStatus( "deg" ) );
        assertEquals( "GUESSED_UNIT", VO.vounitStatus( "degree" ) );
        assertTrue( VO.vounitMessage( "degree" ).indexOf( "\"deg\"" ) >= 0 );
        assertNull( VO.vounitMessage( "kg/m**2" ) );
        assertTrue( VO.vounitMessage( "kg/m^2" ).trim().length() > 0 );
    }

    private void checkHealpix( int ik, long ipix ) {
        assertEquals( ipix, Tilings.healpixRingToNest( ik,
                            Tilings.healpixNestToRing( ik, ipix ) ) );
        assertEquals( ipix, Tilings.healpixNestToRing( ik,
                            Tilings.healpixRingToNest( ik, ipix ) ) );
        assertEquals( Tilings.healpixNestLat( ik, ipix ),
                      Tilings.healpixRingLat( ik,
                           Tilings.healpixNestToRing( ik, ipix ) ),
                      1e-6 );
        assertEquals( Tilings.healpixNestLon( ik, ipix ),
                      Tilings.healpixRingLon( ik,
                           Tilings.healpixNestToRing( ik, ipix ) ),
                      1e-6 );
        assertEquals( ipix,
                      Tilings.healpixNestIndex( ik,
                                 Tilings.healpixNestLon( ik, ipix ),
                                 Tilings.healpixNestLat( ik, ipix ) ) );
        assertEquals( ipix,
                      Tilings.healpixRingIndex( ik,
                                 Tilings.healpixRingLon( ik, ipix ),
                                 Tilings.healpixRingLat( ik, ipix ) ) );
    }

    public void testJELClasses() {
        checkClassesLookOK( JELUtils.getStaticClasses()
                                    .toArray( new Class[ 0 ] ) );
    }

    public void checkClassesLookOK( Class[] classes ) {
        for ( int i = 0; i < classes.length; i++ ) {
            Class clazz = classes[ i ];

            /* Check there's one private no-arg constructor to prevent
             * instantiation (not really essential, but good practice). */
            Constructor[] constructors = clazz.getDeclaredConstructors();
            assertEquals( 1, constructors.length );
            Constructor pcons = constructors[ 0 ];
            assertEquals( 0, pcons.getParameterTypes().length );
            assertTrue( Modifier.isPrivate( pcons.getModifiers() ) );

            /* Check there are no non-static members (would probably indicate
             * missing the 'static' modifier by accident). */
            Field[] fields = clazz.getDeclaredFields();
            for ( int j = 0; j < fields.length; j++ ) {
                assertTrue( Modifier.isStatic( fields[ j ].getModifiers() ) );
            }
            Method[] methods = clazz.getDeclaredMethods();
            for ( int j = 0; j < methods.length; j++ ) {
                assertTrue( Modifier.isStatic( methods[ j ].getModifiers() ) );
            }
        }
    }
}
