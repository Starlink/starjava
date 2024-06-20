package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import junit.framework.TestCase;

public class QuantilerTest extends TestCase {

    public void testEmpty() {
        Quantiler[] calcs = {
            new SortQuantiler(),
            new GKQuantiler(),
        };
        for ( Quantiler calc : calcs ) {
            calc.ready();
            assertTrue( Double.isNaN( calc.getValueAtQuantile( 0.5 ) ) );
        }
    }

    public void testSmall() {
        Quantiler[] calcs = {
            new SortQuantiler(),
            new GKQuantiler(),
        };
        for ( Quantiler calc : calcs ) {
            calc.acceptDatum( 23 );
            calc.ready();
            assertEquals( 23.0, calc.getValueAtQuantile( 0.0 ) );
            assertEquals( 23.0, calc.getValueAtQuantile( 0.5 ) );
            assertEquals( 23.0, calc.getValueAtQuantile( 1.0 ) );
        }
    }

    public void testQuantiler() throws IOException {
        int max = 100;
        int[] values = shuffle( triangle( max ) );

        Quantiler[] calcs = new Quantiler[] {
            new SortQuantiler( max / 9 ),
            new SortQuantiler( max / 3 + 5 ),
            new SortQuantiler( max ),
            new SortQuantiler( max - 1 ),
            new SortQuantiler( max + 1 ),
            new SortQuantiler( max * 2 ),
        };

        for ( int ic = 0; ic < calcs.length; ic++ ) {
            for ( int i = 0; i < values.length; i++ ) {
                calcs[ ic ].acceptDatum( Double.NaN );
                calcs[ ic ].acceptDatum( values[ i ] );
            }
        }

        for ( int ic = 0; ic < calcs.length; ic++ ) {
            Quantiler qc = calcs[ ic ];
            qc.ready();
            assertEquals( - ( max - 1. ), qc.getValueAtQuantile( 0.0 ) );
            assertEquals( + ( max - 1. ), qc.getValueAtQuantile( 1.0 ) );
            assertEquals( 0., qc.getValueAtQuantile( 0.5 ) );
            assertEquals( -89., qc.getValueAtQuantile( 0.1 ) );
            assertEquals( +89., qc.getValueAtQuantile( 0.9 ) );
        }
    }

    private static int[] triangle( int max ) {
        int[] values = new int[ max * max ];
        int ix = 0;
        for ( int i = 0; i < max; i++ ) {
            for ( int j = 0; j < i; j++ ) {
                values[ ix++ ] = i;
                values[ ix++ ] = -i;
            }
        }
        return values;
    }

    private static int[] shuffle( int[] array ) {
        List<Integer> list = new ArrayList<>();
        for ( int i = 0; i < array.length; i++ ) {
            list.add( Integer.valueOf( array[ i ] ) );
        }
        Collections.shuffle( list, new Random( 2313214 ) );
        int[] out = new int[ array.length ];
        for ( int i = 0; i < array.length; i++ ) {
            out[ i ] = list.get( i ).intValue();
        }
        return out;
    }
}
