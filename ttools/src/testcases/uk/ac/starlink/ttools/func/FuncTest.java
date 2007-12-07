package uk.ac.starlink.ttools.func;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.util.TestCase;

public class FuncTest extends TestCase {

    private static final double TINY = 1e-13;
    private static final Random RANDOM = new Random( 1234567L );

    static {
        Logger.getLogger( "uk.ac.starlink.util" ).setLevel( Level.SEVERE );
        Logger.getLogger( "uk.ac.starlink.ast" ).setLevel( Level.SEVERE );
    }

    public FuncTest( String name ) {
        super( name );
    }


    public void testArithmetic() {
        assertEquals( 3, Arithmetic.abs( 3 ) );
        assertEquals( 2.3, Arithmetic.abs( -2.3 ) );

        assertEquals( 5, Arithmetic.max( (short) 5, (short) 4) );
        assertEquals( -4.0, Arithmetic.max( -5.0f, -4.0f ) );

        assertEquals( 4, Arithmetic.min( (short) 5, (short) 4) );
        assertEquals( -5.0, Arithmetic.min( -5.0f, -4.0f ) );

        assertEquals( 12, Arithmetic.roundUp( 11.01 ) );
        assertEquals( 11, Arithmetic.round( 11.01 ) );
        assertEquals( 11, Arithmetic.roundDown( 11.99 ) );
        assertEquals( 12, Arithmetic.round( 11.99 ) );
        assertEquals( 4, Arithmetic.round( 4.5 ) );

        assertEquals( 3.14f, Arithmetic.roundDecimal( Math.PI, 2 ) );
    }

    public void testConversions() {
        assertEquals( (byte) 99, Conversions.parseByte( "99" ) );
        try {
            Conversions.parseByte( "999" );
            fail();
        }
        catch ( NumberFormatException e ) {
        }

        assertEquals( (short) 30000, Conversions.parseShort( "30000" ) );
        try {
            Conversions.parseShort( "40000" );
            fail();
        }
        catch ( NumberFormatException e ) {
        }

        assertEquals( 70000, Conversions.parseInt( "70000" ) );
        try {
            Conversions.parseInt( "7777777777777777" );
            fail();
        }
        catch ( NumberFormatException e ) {
        }

        assertEquals( -1L, Conversions.parseLong( "-1" ) );
        try {
            Conversions.parseLong( "tits" );
            fail();
        }
        catch ( NumberFormatException e ) {
        }

        assertEquals( 101.5f, Conversions.parseFloat( "1015e-1" ) );

        assertEquals( 101.5, Conversions.parseDouble( "1015e-1" ) );
        try {
            Conversions.parseDouble( "No doubles here mate" );
            fail();
        }
        catch ( NumberFormatException e ) {
        }

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

        assertEquals( 256.0, Maths.pow( 2, 8 ) );

        assertEquals( 32.0, Maths.sqrt( 1024.0 ) );
       
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

    public void testStrings() {
        assertEquals( "00023", Strings.padWithZeros( 23, 5 ) );
        assertEquals( "23", Strings.padWithZeros( 23, 2 ) );
        assertEquals( "23", Strings.padWithZeros( 23, 1 ) );

        assertEquals( "starlink", Strings.concat( "star", "link" ) );
        assertEquals( "star", Strings.concat( "star", null ) );
        assertEquals( "link", Strings.concat( "", "link" ) );

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
    }

    public void testCoords() {

        assertEquals( 180.0, Coords.radiansToDegrees( Maths.PI ), TINY );
        assertEquals( 0.0, Coords.radiansToDegrees( 0.0 ) );

        assertEquals( Maths.PI, Coords.degreesToRadians( 180.0 ), TINY );
        assertEquals( 0.0, Coords.degreesToRadians( 0.0 ) );

        assertEquals( 0.0, Coords.skyDistance( 1.4, 2.1, 1.4, 2.1 ), TINY );
        assertEquals( Maths.PI / 2.0, 
                      Coords.skyDistance( -1, 0.0, -1, Maths.PI / 2.0 ), TINY );

        double ra1 = 0.1;
        double dec1 = 1.2;
        double ra2 = 0.2;
        double dec2 = 1.3;
        assertEquals( Coords.skyDistance( ra1, dec1, ra2, dec2 ),
                      Coords.skyDistance( ra2, dec2, ra1, dec1 ) );
        assertEquals( Coords.skyDistanceDegrees( ra1, dec1, ra2, dec2 ),
                      Coords.skyDistanceDegrees( ra2, dec2, ra1, dec1 ) );
        assertEquals(
            Coords.radiansToDegrees( Coords.skyDistance( ra1, dec1,
                                                         ra2, dec2 ) ),
            Coords.skyDistanceDegrees( Coords.radiansToDegrees( ra1 ),
                                       Coords.radiansToDegrees( dec1 ),
                                       Coords.radiansToDegrees( ra2 ),
                                       Coords.radiansToDegrees( dec2 ) ),
            TINY );

        assertEquals( Maths.PI / 4.0, Coords.hmsToRadians( "03:00:00.0" ) );
        assertEquals( -Maths.PI / 4.0, Coords.hmsToRadians( "-03: 0:0" ) );
        assertEquals( Coords.hmsToRadians( "0 0 1" ),
                      -Coords.hmsToRadians( "-0h0m1s" ) );

        assertEquals( "03:00:00", Coords.radiansToHms( Maths.PI / 4.0 ) );
        assertEquals( "12:00:00.000",
                      Coords.radiansToHms( -Maths.PI, 3 ) );
        assertEquals( "12:00:00.500", 
                      Coords.radiansToHms( -Maths.PI*(1+.5000/12/60/60), 3 ) );
        assertEquals( "12:00:00.005", 
                      Coords.radiansToHms( -Maths.PI*(1+.0050/12/60/60), 3 ) );
        assertEquals( "12:00:00.500", 
                      Coords.radiansToHms( -Maths.PI*(1+.5004/12/60/60), 3 ) );
        assertEquals( "12:00:00.500", 
                      Coords.radiansToHms( -Maths.PI*(1+.4996/12/60/60), 3 ) );

        assertEquals( Maths.PI / 4.0, Coords.dmsToRadians( "45:00:00.0" ) );
        assertEquals( -Maths.PI / 4.0, Coords.dmsToRadians( "-45: 0:0" ) );
        assertEquals( Coords.dmsToRadians( "0 0 1" ),
                      -Coords.dmsToRadians( "-0d0m1s" ) );

        assertEquals( "+45:00:00", Coords.radiansToDms( Maths.PI / 4.0 ) );
        assertEquals( "-90:00:00.000",
                      Coords.radiansToDms( -Maths.PI / 2, 3 ) );
        assertEquals( "-45:00:00.500",
                      Coords.radiansToDms( -Maths.PI / 4.0 
                                           * (1+.5000/45/60/60), 3 ) );
        assertEquals( "-45:00:00.005",
                      Coords.radiansToDms( -Maths.PI / 4.0 
                                           * (1+.0050/45/60/60), 3 ) );
        assertEquals( "-45:00:00.500",
                      Coords.radiansToDms( -Maths.PI / 4.0 
                                           * (1+.5004/45/60/60), 3 ) );
        assertEquals( "-45:00:00.500",
                      Coords.radiansToDms( -Maths.PI / 4.0
                                           * (1+.4996/45/60/60), 3 ) );

        double ra1950 = 2.1;
        double de1950 = 1.2;
        double ra2000 = Coords.raFK4toFK5( ra1950, de1950 );
        double de2000 = Coords.decFK4toFK5( ra1950, de1950 );
        assertEquals( ra1950, ra2000, 0.03 );
        assertEquals( de1950, de2000, 0.03 );
        assertEquals( ra1950, Coords.raFK5toFK4( ra2000, de2000 ), 1e-9 );
        assertEquals( de1950, Coords.decFK5toFK4( ra2000, de2000 ), 1e-9 );

    }

    public void testJELClasses() {
        checkClassesLookOK( (Class[]) JELUtils.getStaticClasses()
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
