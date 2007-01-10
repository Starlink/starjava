package uk.ac.starlink.ttools.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junit.framework.TestCase;

public class QuantCalcTest extends TestCase {

    public QuantCalcTest( String name ) {
        super( name );
    }

    public void testQuant() {
        int max = 100;
        int[] values = shuffle( triangle( max ) );

        final QuantCalc c1;
        final QuantCalc c2;
        final QuantCalc c3;
        final QuantCalc c4;
        final QuantCalc c5;
        QuantCalc[] calcs = new QuantCalc[] {
            c1 = new QuantCalc.ObjectListQuantCalc( Double.class ),
            c2 = new QuantCalc.FloatArrayQuantCalc( Long.class, values.length ),
            c3 = new QuantCalc.ByteSlotQuantCalc(),
            c4 = new QuantCalc.ShortSlotQuantCalc(),
            c5 = new QuantCalc.CountMapQuantCalc( Integer.class ),
        };

        for ( int ic = 0; ic < calcs.length; ic++ ) {
            for ( int i = 0; i < 55; i++ ) {
                calcs[ ic ].acceptDatum( null );
            }
            c1.acceptDatum( new Double( Double.NaN ) );
        }
        

        for ( int i = 0; i < values.length; i++ ) {
            int ival = values[ i ];
            c1.acceptDatum( new Double( ival ) );
            c2.acceptDatum( new Long( ival ) );
            c3.acceptDatum( new Byte( (byte) ival ) );
            c4.acceptDatum( new Short( (short) ival ) );
            c5.acceptDatum( new Integer( ival ) );
        }

        for ( int ic = 0; ic < calcs.length; ic++ ) {
            QuantCalc qc = calcs[ ic ];
            qc.ready();
            assertEquals( - ( max - 1 ), qc.getQuantile( 0.0 ).intValue() );
            assertEquals( + ( max - 1 ), qc.getQuantile( 1.0 ).intValue() );
            assertEquals( 0, qc.getQuantile( 0.5 ).intValue() );
            assertEquals( -89, qc.getQuantile( 0.1 ).intValue() );
            assertEquals( +89, qc.getQuantile( 0.9 ).intValue() );
        }

        assertEquals( Double.class, c1.getQuantile( 0.5 ).getClass() );
        assertEquals( Long.class, c2.getQuantile( 0.5 ).getClass() );
        assertEquals( Byte.class, c3.getQuantile( 0.5 ).getClass() );
        assertEquals( Short.class, c4.getQuantile( 0.5 ).getClass() );
        assertEquals( Integer.class, c5.getQuantile( 0.5 ).getClass() );
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
        List list = new ArrayList();
        for ( int i = 0; i < array.length; i++ ) {
            list.add( new Integer( array[ i ] ) );
        }
        Collections.shuffle( list );
        int[] out = new int[ array.length ];
        for ( int i = 0; i < array.length; i++ ) {
            out[ i ] = ((Integer) list.get( i )).intValue();
        }
        return out;
    }
}
