package uk.ac.starlink.array;

import java.io.IOException;
import uk.ac.starlink.util.TestCase;

public class CombineArrayImplTest extends TestCase {

    public CombineArrayImplTest( String name ) {
        super( name );
    }

    public void testCombine() throws IOException {
        Combiner combi = new Combiner() {
            public double combination( double x, double y ) {
                return x + y;
            }
        };
        Type type1 = Type.BYTE;
        Type type2 = type1;
        Type type3 = type1;
        BadHandler abh1 = BadHandler.getHandler( type1,
                                                 type1.defaultBadValue() );
        BadHandler abh2 = BadHandler.getHandler( type2,
                                                 type2.defaultBadValue() );
        NDShape shape1 = new NDShape( new long[] { 0 }, new long[] { 100 } ); 
        NDShape shape2 = new NDShape( new long[] { 50 }, new long[] { 100 } );
        NDShape shape3 = new NDShape( new long[] { 0 }, new long[] { 150 } );
        int np1 = (int) shape1.getNumPixels();
        int np2 = (int) shape2.getNumPixels();
        int np3 = (int) shape3.getNumPixels();
        Object buf1 =  type1.newArray( np1 );
        Object buf2 =  type2.newArray( np2 );
        fillRandom( buf1, -120, 120 );
        fillRandom( buf2, -120, 120 );
        NDArray nda1 = new ScratchNDArray( new OrderedNDShape( shape1, null ),
                                           type1, abh1 );
        NDArray nda2 = new ScratchNDArray( new OrderedNDShape( shape2, null ),
                                           type2, abh2 );
        ArrayImpl impl3 = new CombineArrayImpl( nda1, nda2, combi,
                                                shape3, type3, null );
        NDArray nda3 = new BridgeNDArray( impl3 );
        BadHandler bh1 = nda1.getBadHandler();
        BadHandler bh2 = nda2.getBadHandler();
        BadHandler bh3 = nda3.getBadHandler();
        nda1.getAccess().write( buf1, 0, np1 );
        nda2.getAccess().write( buf2, 0, np2 );

        NDArray nda1w =
            new BridgeNDArray( new WindowArrayImpl( nda1, shape3 ) );
        NDArray nda2w =
            new BridgeNDArray( new WindowArrayImpl( nda2, shape3 ) );
        byte[] b1 = (byte[]) type1.newArray( np3 );
        byte[] b2 = (byte[]) type2.newArray( np3 );
        byte[] b3 = (byte[]) type3.newArray( np3 );
        int npix = (int) shape3.getNumPixels();
        
        nda1w.getAccess().read( b1, 0, npix );
        nda2w.getAccess().read( b2, 0, npix );
        nda3.getAccess().read( b3, 0, npix );
        for ( int i = 0; i < np3; i++ ) {
            if ( i < 50 && i > 99 ) {
                assertTrue( bh1.isBad( b1, i ) ||
                            bh2.isBad( b2, i ) );
                assertTrue( bh3.isBad( b3, i ) );
            }
            else { 
               int sum = (int) b1[ i ] + (int) b2[ i ];
               if ( bh1.isBad( b1, i ) ||
                    bh2.isBad( b2, i ) ||
                    sum < -127 || sum > 127 ) {
                   assertTrue( bh3.isBad( b3, i ) );
               }
               else {
                   assertEquals( (int) b3[ i ], sum );
               }
            }
        }
    }
    
}
