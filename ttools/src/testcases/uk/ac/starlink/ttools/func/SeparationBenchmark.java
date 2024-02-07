package uk.ac.starlink.ttools.func;

import java.util.LinkedHashMap;
import java.util.Map;

public class SeparationBenchmark {

    private static final SepCalc HAVERSINE =
        CoordsRadians::haversineSeparationFormula;
    private static final SepCalc VINCENTY =
        CoordsRadians::vincentySeparationFormula;

    private static double calculateSamples( SepCalc calc, int n ) {
        double sum = 0;
        double lon1 = 0;
        double colat1 = 0;
        double lon2 = 0;
        double colat2 = 0;
        for ( int i = 0; i < n; i++ ) {
            lon1 = ( lon1 + .001 ) % ( 2 * Math.PI );
            if ( i % 3 == 0 ) {
                colat1 = ( colat1 + .03 ) % Math.PI;
            }
            if ( i % 7 == 0 ) {
                lon2 = ( lon2 + .1 ) % ( 2 * Math.PI );
            }
            if ( i % 11 == 0 ) {
                colat2 = ( colat2 + .07 ) % Math.PI;
            }
            sum += calc.separation( lon1, colat1 - 0.5 * Math.PI,
                                    lon2, colat2 - 0.5 * Math.PI );
        }
        return sum;
    }

    private static void bench( int n ) {
        Map<String,SepCalc> impls = new LinkedHashMap<>();
        impls.put( "Haversine", CoordsRadians::haversineSeparationFormula );
        impls.put( "Vincenty", CoordsRadians::vincentySeparationFormula );
        impls.put( "default", CoordsRadians::skyDistanceRadians );
        for ( Map.Entry<String,SepCalc> entry : impls.entrySet() ) {
            long t0 = System.currentTimeMillis();
            calculateSamples( entry.getValue(), n );
            long time = System.currentTimeMillis() - t0;
            System.out.println( n + "  " + entry.getKey() + ":\t" + time );
        }
    }

    @FunctionalInterface
    private interface SepCalc {
        double separation( double ra1, double dec1, double ra2, double dec2 );
    }

    public static void main( String[] args ) {
        int n = args.length > 0 ? Integer.parseInt( args[ 0 ] ) : 10000000;
        bench( n );
    }
}
