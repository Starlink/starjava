package uk.ac.starlink.hds;

import java.io.IOException;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayAccess;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.DeterministicArrayImpl;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.ndx.DefaultMutableNdx;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.Ndxs;
import uk.ac.starlink.util.TestCase;

public class NdfMakerTest extends TestCase {

    public NdfMakerTest( String name ) {
        super( name );
    }

    public void testNdfMaker() throws IOException, HDSException {
        NdfMaker maker = new NdfMaker();
        NDFNdxHandler handler = NDFNdxHandler.getInstance();

        NDShape shape = new NDShape( new long[] { 11, 21 }, 
                                     new long[] { 10, 20 } );
        Type type = Type.INT;
        NDArray image1 = 
           new BridgeNDArray( new DeterministicArrayImpl( shape, type ) );
        Ndx ndx1 = new DefaultMutableNdx( image1 );

        HDSReference ref = maker.makeTempNDF( ndx1 );
        HDSObject obj2 = ref.getObject( "READ" );
        Ndx ndx2 = handler.makeNdx( obj2, null, AccessMode.READ );
        NDArray image2 = Ndxs.getMaskedImage( ndx2 );

        int npix = (int) shape.getNumPixels();
        Object data1 = type.newArray( npix );
        Object data2 = type.newArray( npix );
        ArrayAccess acc1 = image1.getAccess();
        ArrayAccess acc2 = image2.getAccess();
        acc1.read( data1, 0, npix );
        acc2.read( data2, 0, npix );
        acc1.close();
        acc2.close();

        assertArrayEquals( data1, data2 );
    }
}
