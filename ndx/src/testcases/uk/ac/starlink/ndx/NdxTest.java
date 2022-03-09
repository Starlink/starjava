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
import uk.ac.starlink.array.ArrayAccess;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayImpl;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.ChunkStepper;
import uk.ac.starlink.array.ConvertArrayImpl;
import uk.ac.starlink.array.Converter;
import uk.ac.starlink.array.DeterministicArrayImpl;
import uk.ac.starlink.array.Function;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.array.TypeConverter;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.hds.HDSPackage;
import uk.ac.starlink.hdx.HdxFacade;
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
        assertTrue( "JNIHDS shared library", HDSPackage.isAvailable() );
        remoteNDX = 
            new URL( "http://java.starlink.ac.uk/data/m31.sdf" );
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
            Class.forName( "uk.ac.starlink.oldfits.FitsNdxHandler" );
            fitsPresent = true;
        }
        catch ( ClassNotFoundException e ) {
        }
    }


    private MutableNdx virtualNdx() throws IOException, TransformerException {

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

        MutableNdx vndx = new DefaultMutableNdx( vimage );
        vndx.setVariance( vvariance );
        vndx.setQuality( vquality );
        vndx.setTitle( "Mark's first test NDX" );
        vndx.setUnits( "fortnights" );
        vndx.setEtc( new SourceReader().getDOM( etcSrc ) );

        return vndx;
    }

    public void testNdx() throws Exception {

        /* Get an Ndx. */
        Ndx vndx = virtualNdx();

        /* Write it to various output types. */
        String filename = ndxname + ".xml";
        if ( fitsPresent ) {
            ndxio.outputNdx( filename, vndx );
        }
        else {
            new SourceReader().writeSource( vndx.getHdxFacade().getSource( null ), 
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

        // /* Have a go at data across the network. */
        // Skip this test - it's too vulnerable to the web server 
        // being down.
        // Ndx rndx = ndxio.makeNdx( remoteNDX, AccessMode.READ );
        // ndxio.outputNdx( rname + ".sdf", rndx );

        // Confirm HdxFacade assertions
        HdxFacade hf = vndx.getHdxFacade();
        Class c = hf.getHdxResourceType().getConstructedClass();
        assertTrue(c != Object.class);
        assertTrue(c.isInstance(vndx));
        
    }

    public void testNdxs() throws IOException, TransformerException {
        MutableNdx vndx = virtualNdx();

        vndx.setWCS( null );
        assertTrue( ! vndx.hasWCS() );

        FrameSet fset = Ndxs.getAst( vndx );
        assertEquals( fset, Ndxs.getDefaultAst( vndx ) );
        vndx.setWCS( (Object) fset );
        assertTrue( vndx.hasWCS() );
        assertEquals( fset, vndx.getAst() );

        NDShape shape = vndx.getImage().getShape();
        Type qtype = Type.BYTE;
        ArrayImpl qimpl = new DeterministicArrayImpl( shape, qtype ) {
            public Number getBadValue() {
                return null;
            }
            protected double offsetToValue( long off ) {
                return (double) (byte) off;
            }
        };
        NDArray qnda = new BridgeNDArray( qimpl );
 
        vndx.setQuality( qnda );
        vndx.setBadBits( 0x01 );

        ArrayAccess miacc = Ndxs.getMaskedImage( vndx ).getAccess();
        ArrayAccess mvacc = Ndxs.getMaskedVariance( vndx ).getAccess();
        ArrayAccess meacc = Ndxs.getMaskedErrors( vndx ).getAccess();
        ArrayAccess iacc = vndx.getImage().getAccess();
        ArrayAccess vacc = vndx.getVariance().getAccess();
        ArrayAccess qacc = vndx.getQuality().getAccess();

        BadHandler mibh = miacc.getBadHandler();
        BadHandler mvbh = mvacc.getBadHandler();
        BadHandler mebh = meacc.getBadHandler();
        BadHandler ibh = iacc.getBadHandler();
        BadHandler vbh = vacc.getBadHandler();
        BadHandler qbh = qacc.getBadHandler();

        int badbits = vndx.getBadBits();

        long npix = shape.getNumPixels();
        ChunkStepper cit = new ChunkStepper( npix );
        int size = cit.getSize();
        Object mibuf = miacc.getType().newArray( size );
        Object mvbuf = mvacc.getType().newArray( size );
        Object mebuf = meacc.getType().newArray( size );
        Object ibuf = iacc.getType().newArray( size );
        Object vbuf = vacc.getType().newArray( size );
        Object qbuf = qacc.getType().newArray( size );

        for ( ; cit.hasNext(); cit.next() ) {
            size = cit.getSize();
            miacc.read( mibuf, 0, size );
            mvacc.read( mvbuf, 0, size );
            meacc.read( mebuf, 0, size );
            iacc.read( ibuf, 0, size );
            vacc.read( vbuf, 0, size );
            qacc.read( qbuf, 0, size );

            for ( int i = 0; i < size; i++ ) {
                if ( ( cit.getBase() + i ) % 2 == 1 ) {
                    assertTrue( ( qbh.makeNumber( qbuf, i ).intValue() 
                                  & badbits ) > 0 );
                    assertTrue( mibh.isBad( mibuf, i ) );
                    assertTrue( mvbh.isBad( mvbuf, i ) );
                    assertTrue( mebh.isBad( mebuf, i ) );
                }
                else {
                    assertTrue( ( qbh.makeNumber( qbuf, i ).intValue()
                                  & badbits ) == 0 );
                    assertEquals( ibh.makeNumber( ibuf, i ),
                                  mibh.makeNumber( mibuf, i ) );
                    assertEquals( vbh.makeNumber( vbuf, i ),
                                  mvbh.makeNumber( mvbuf, i ) );
                    if ( ! vbh.isBad( vbuf, i ) ) {
                        assertEquals( (float) 
                                      Math.sqrt( vbh.makeNumber( vbuf, i )
                                                    .doubleValue() ),
                                      mebh.makeNumber( mebuf, i )
                                          .floatValue() );
                    }
                }
            }
        }

        miacc.close();
        mvacc.close();
        meacc.close();
        iacc.close();
        vacc.close();
        qacc.close();
        
    }
}
