package uk.ac.starlink.ndx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.net.MalformedURLException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Node;
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
import uk.ac.starlink.util.SourceReader;

public class NdxTest extends TestCase {

    private NdxIO ndxio;
    private boolean hdsPresent = false;
    private boolean fitsPresent = false;
    private String ndxname;
    private URL remoteNDX;
    private String rname;

    public NdxTest( String name ) {
        super( name );
    }

    public void setUp() throws MalformedURLException {
        ndxio = new NdxIO();
        ndxname = System.getProperty( "java.io.tmpdir" )
                + File.separatorChar
                + "vndx";
        remoteNDX = 
            new URL( "http://andromeda.star.bris.ac.uk/~mbt/data/m31.sdf" );
        rname = System.getProperty( "java.io.tmpdir" )
              + File.separatorChar
              + "m31-from-network";
        try {
            Class.forName( "uk.ac.starlink.hds.NDFNdxHandler" );
            hdsPresent = true;
        }
        catch ( ClassNotFoundException e ) {
        }
        try {
            Class.forName( "uk.ac.starlink.fits.FitsNdxHandler" );
            fitsPresent = true;
        }
        catch ( ClassNotFoundException e ) {
        }
    }

    public void testNdx() throws IOException, TransformerException {

        /* Construct a virtual NDX. */
        NDShape shape = new NDShape( new long[] { 50, 40 },
                                     new long[] { 100, 200 } );
        Type type = Type.FLOAT;
        NDArray vimage = 
            new BridgeNDArray( new DeterministicArrayImpl( shape, type ) );

        BadHandler bh = vimage.getBadHandler();
        Function sqfunc = new Function() {
            public double forward( double x ) { return x * x; }
            public double inverse( double y ) { return Math.sqrt( y ); }
        };
        Converter sconv = new TypeConverter( type, bh, type, bh, sqfunc );
        NDArray vvariance = 
            new BridgeNDArray( new ConvertArrayImpl( vimage, sconv ) );
        NDArray vquality = null;

        String etcText = 
              "<etc>"
            + "<favouriteFood>Fish cakes</favouriteFood>"
            + "<pets>"
            + "<hedgehog/>"
            + "<herd>"
            + "<cow name='daisy'/>"
            + "<cow name='dobbin' colour='brown'/>"
            + "</herd>"
            + "</pets>"
            + "</etc>";
        Source etcSrc = new StreamSource( new StringReader( etcText ) );

        BulkDataImpl bulkdat = 
            new ArraysBulkDataImpl( vimage, vvariance, vquality );

        MutableNdx vndx = new DefaultMutableNdx( bulkdat );
        vndx.setTitle( "Mark's first test NDX" );
        vndx.setEtc( new SourceReader().getDOM( etcSrc ) );

        /* Write it to various output types. */
        String filename = ndxname + ".xml";
        if ( fitsPresent ) {
            ndxio.outputNdx( filename, vndx );
        }
        else {
            SourceReader sr = new SourceReader();
            sr.setIndent( 2 );
            new SourceReader().writeSource( vndx.toXML(), 
                                            new FileOutputStream( filename ) ); 
        }

        if ( fitsPresent ) {
            String fname = ndxname + ".fits";
            ndxio.outputNdx( fname, vndx );
            Ndx fndx = ndxio.makeNdx( fname, AccessMode.READ );
            ndxio.outputNdx( ndxname + "-fits.xml", fndx );
        }

        if ( hdsPresent ) {
            String hname = ndxname + ".sdf";
            ndxio.outputNdx( hname, vndx );
            Ndx hndx = ndxio.makeNdx( hname, AccessMode.READ );
            ndxio.outputNdx( ndxname + "-hds.xml", hndx );
        }

        /* Have a go at data across the network. */
        Ndx rndx = ndxio.makeNdx( remoteNDX, AccessMode.READ );
        ndxio.outputNdx( rname + ".sdf", rndx );
 
    }
}
