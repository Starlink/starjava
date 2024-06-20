package uk.ac.starlink.ttools.plot2.data;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.TestCase;

public class CachedColumnFactoryTest extends TestCase {

    public void testColumnFactories() throws IOException {
        exerciseColumnFactory( new MemoryColumnFactory() );
        exerciseColumnFactory( new SmartColumnFactory(
                                   new MemoryColumnFactory() ) );
        exerciseColumnFactory( new ByteStoreColumnFactory(
                                   StoragePolicy.PREFER_MEMORY ) );
    }

    private void exerciseColumnFactory( CachedColumnFactory fact )
            throws IOException {
        int nr = 100;
        Map<StorageType,CachedColumn> colMap =
            new EnumMap<StorageType,CachedColumn>( StorageType.class );
        for ( StorageType type : StorageType.values() ) {
            colMap.put( type, fact.createColumn( type, nr ) );
        }
        boolean[] booleans = new boolean[ nr ];
        byte[] bytes = new byte[ nr ];
        long[] longs = new long[ nr ];
        double[] doubles = new double[ nr ];
        float[] floats = new float[ nr ];
        int[] ints = new int[ nr ];
        short[] shorts = new short[ nr ];
        String[] strings = new String[ nr ];
        double[][] doubleArrays = new double[ nr ][];
        float[][] floatArrays = new float[ nr ][];
        double[][] double3s = new double[ nr ][];
        float[][] float3s = new float[ nr ][];
        int[][] int3s = new int[ nr ][];
        for ( int i = 0; i < nr; i++ ) {
            int sgn = i % 2 == 0 ? +1 : -1;
            boolean isBad = i % 7 == 0;
            booleans[ i ] = i % 3 == 0;
            bytes[ i ] = (byte) Math.abs( i );
            shorts[ i ] = (short) ( sgn * i );
            ints[ i ] = sgn * i;
            longs[ i ] = sgn * i;
            doubles[ i ] = isBad ? Double.NaN : 0.5 + i;
            floats[ i ] = isBad ? Float.NaN : 0.5f + i;
            strings[ i ] = isBad ? "" : ( 10 * ( i / 10 ) ) + addText( i % 10 );
            int aleng = i % 5;
            doubleArrays[ i ] = new double[ aleng ];
            floatArrays[ i ] = new float[ aleng ];
            for ( int j = 0; j < aleng; j++ ) {
                doubleArrays[ i ][ j ] = isBad ? Double.NaN : j + i;
                floatArrays[ i ][ j ] = isBad ? Float.NaN : j + i;
            }
            double3s[ i ] = new double[ 3 ];
            float3s[ i ] = new float[ 3 ];
            int3s[ i ] = new int[ 3 ];
            for ( int k = 0; k < 3; k++ ) {
                double3s[ i ][ k ] = isBad ? Double.NaN : i + k;
                float3s[ i ][ k ] = isBad ? Float.NaN : i + k;
                int3s[ i ][ k ] = sgn * ( i + k );
            }
        }
        for ( int i = 0; i < nr; i++ ) {
            colMap.get( StorageType.BOOLEAN )
                  .add( Boolean.valueOf( booleans[ i ] ) );
            colMap.get( StorageType.BYTE )
                  .add( Byte.valueOf( bytes[ i ] ) );
            colMap.get( StorageType.DOUBLE )
                  .add( Double.valueOf( doubles[ i ] ) );
            colMap.get( StorageType.DOUBLE_ARRAY ).add( doubleArrays[ i ] );
            colMap.get( StorageType.DOUBLE3 ).add( double3s[ i ] );
            colMap.get( StorageType.FLOAT ).add( Float.valueOf( floats[ i ] ) );
            colMap.get( StorageType.FLOAT_ARRAY ).add( floatArrays[ i ] );
            colMap.get( StorageType.FLOAT3 ).add( float3s[ i ] );
            colMap.get( StorageType.INT ).add( Integer.valueOf( ints[ i ] ) );
            colMap.get( StorageType.INT3 ).add( int3s[ i ] );
            colMap.get( StorageType.LONG ).add( Long.valueOf( longs[ i ] ) );
            colMap.get( StorageType.SHORT ).add( Short.valueOf( shorts[ i ] ) );
            colMap.get( StorageType.STRING ).add( strings[ i ] );
        }

        Map<StorageType,CachedReader> rdrMap =
            new EnumMap<StorageType,CachedReader>( StorageType.class );
        for ( StorageType type : StorageType.values() ) {
            CachedColumn col = colMap.get( type );
            assertEquals( nr, col.getRowCount() );
            col.endAdd();
            assertEquals( nr, col.getRowCount() );
            rdrMap.put( type, col.createReader() );
        }

        Runnable[] iCheckers = new Runnable[ nr ];
        for ( int ir = 0; ir < nr; ir++ ) {
            final int i = ir;
            iCheckers[ i ] = () -> {
                assertEquals( booleans[ i ],
                    rdrMap.get( StorageType.BOOLEAN ).getBooleanValue( i ) );
                assertEquals( (int) bytes[ i ],
                    rdrMap.get( StorageType.BYTE ).getIntValue( i ) );
                assertEquals( doubles[ i ],
                    rdrMap.get( StorageType.DOUBLE ).getDoubleValue( i ) );
                assertArrayEquivalent( doubleArrays[ i ],
                    rdrMap.get( StorageType.DOUBLE_ARRAY ).getObjectValue( i ));
                assertArrayEquals( double3s[ i ],
                    rdrMap.get( StorageType.DOUBLE3 ).getObjectValue( i ) );
                assertEquals( (double) floats[ i ],
                    rdrMap.get( StorageType.FLOAT ).getDoubleValue( i ) );
                assertArrayEquivalent( floatArrays[ i ],
                    rdrMap.get( StorageType.FLOAT_ARRAY ).getObjectValue( i ) );
                assertArrayEquals( float3s[ i ],
                    rdrMap.get( StorageType.FLOAT3 ).getObjectValue( i ) );
                assertEquals( ints[ i ],
                    rdrMap.get( StorageType.INT ).getIntValue( i ) );
                assertArrayEquals( int3s[ i ],
                    rdrMap.get( StorageType.INT3 ).getObjectValue( i ) );
                assertEquals( longs[ i ],
                    rdrMap.get( StorageType.LONG ).getLongValue( i ) );
                assertEquals( (int) shorts[ i ],
                    rdrMap.get( StorageType.SHORT ).getIntValue( i ) );
                assertEquivalent( strings[ i ],
                    rdrMap.get( StorageType.STRING ).getObjectValue( i ) );
            };
        }

        iCheckers[ 23 ].run();
        for ( int i = 0; i < nr; i++ ) {
            iCheckers[ i ].run();
        }
        for ( int i = nr - 1; i >= 0; i-- ) {
            iCheckers[ i ].run();
        }
    }

    private void assertEquivalent( Object obj1, Object obj2 ) {
        if ( ! ( Tables.isBlank( obj1 ) && Tables.isBlank( obj2 ) ) ) {
            assertEquals( obj1, obj2 );
        }
    }

    private void assertArrayEquivalent( Object array1, Object array2 ) {
        if ( ! ( Tables.isBlank( array1 ) && Tables.isBlank( array2 ) ) ) {
            assertArrayEquals( array1, array2 );
        } 
    }

    private static String addText( int c ) {
        switch ( c ) {
            case 0: return "";
            case 1: return "+one";
            case 2: return "+two";
            case 3: return "+three";
            case 4: return "+four";
            case 5: return "+five";
            case 6: return "+six";
            case 7: return "+seven";
            case 8: return "+eight";
            case 9: return "+nine";
        }
        throw new AssertionError();
    }
}
