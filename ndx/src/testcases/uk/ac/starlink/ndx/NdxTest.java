package uk.ac.starlink.ndx;

import java.io.File;
import java.io.IOException;
import javax.xml.transform.Source;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayImpl;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.ConvertArrayImpl;
import uk.ac.starlink.array.Converter;
import uk.ac.starlink.array.DeterministicArrayImpl;
import uk.ac.starlink.array.Function;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.array.TypeConverter;
import uk.ac.starlink.util.TestCase;

public class NdxTest extends TestCase {

    private NdxFactory factory;
    private boolean hdsPresent = false;
    private boolean fitsPresent = false;
    private String ndxname;

    public NdxTest( String name ) {
        super( name );
    }

    public void setUp() {
        factory = new NdxFactory();
        ndxname = System.getProperty( "java.io.tmpdir" )
                + File.separatorChar
                + "vndx";
        try {
            Class.forName( "uk.ac.starlink.hds.NDFNdxBuilder" );
            hdsPresent = true;
        }
        catch ( ClassNotFoundException e ) {
        }
        try {
            Class.forName( "uk.ac.starlink.fits.FitsNdxBuilder" );
            fitsPresent = true;
        }
        catch ( ClassNotFoundException e ) {
        }
    }

    public void testNdx() throws IOException {

        /* Construct a virtual NDX. */
        NDShape shape = new NDShape( new long[] { 50, 40 },
                                     new long[] { 100, 200 } );
        Type type = Type.FLOAT;
        final NDArray vimage = 
            new BridgeNDArray( new DeterministicArrayImpl( shape, type ) );

        BadHandler bh = vimage.getBadHandler();
        Function sqfunc = new Function() {
            public double forward( double x ) { return x * x; }
            public double inverse( double y ) { return Math.sqrt( y ); }
        };
        Converter sconv = new TypeConverter( type, bh, type, bh, sqfunc );
        final NDArray vvariance = 
            new BridgeNDArray( new ConvertArrayImpl( vimage, sconv ) );
        final NDArray vquality = null;

        final NdxImpl vimpl = new NdxImpl() {
            public BulkDataImpl getBulkData() {
                return new ArraysBulkDataImpl( vimage, vvariance, vquality );
            }
            public byte getBadBits() { return (byte) 0; }
            public boolean hasTitle() { return true; }
            public String getTitle() { return "Mark's test NDX"; }
            public boolean hasWCS() { return false; }
            public Object getWCS() { return null; }
            public boolean hasEtc() { return false; }
            public Source getEtc() { return null; }
        };
        Ndx vndx = new BridgeNdx( vimpl );

        /* Write it to various output types. */
        factory.createNewNdx( ndxname + ".xml", vndx );

        if ( hdsPresent ) {
            String hname = ndxname + ".sdf";
            factory.createNewNdx( hname, vndx );
            Ndx hndx = factory.makeNdx( hname, AccessMode.READ );
            factory.createNewNdx( ndxname + "-hds.xml", hndx );
        }
        if ( fitsPresent ) {
            String fname = ndxname + ".fits";
            factory.createNewNdx( fname, vndx );
            Ndx fndx = factory.makeNdx( fname, AccessMode.READ );
            factory.createNewNdx( ndxname + "-fits.xml", fndx );
        }

    }
}
