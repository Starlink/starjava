package uk.ac.starlink.hds;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Order;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.util.TestCase;

public class HDSTest extends TestCase {

    public static String NDF_FILE = "uk/ac/starlink/hds/reduced_data1.sdf";
    public static String containerName;
    public static File containerFile;

    public HDSTest( String name ) {
        super( name );
    }

    public void setUp() throws IOException {
        if ( containerFile == null ) {
            String tmpdir = System.getProperty( "java.io.tmpdir" );
            containerName = tmpdir + File.separatorChar + "test_ndf";
            containerFile = new File( containerName + ".sdf" );
            InputStream istrm = getClass()
                               .getClassLoader()
                               .getResourceAsStream( NDF_FILE );
            assertNotNull( "Failed to open " + NDF_FILE, istrm );
            OutputStream ostrm = new FileOutputStream( containerFile );
            containerFile.deleteOnExit();

            istrm = new BufferedInputStream( istrm );
            ostrm = new BufferedOutputStream( ostrm );
            int b;
            while ( ( b = istrm.read() ) >= 0 ) {
                ostrm.write( b );
            }
            istrm.close();
            ostrm.close();
        }
    }

    public void testHDSReference()
            throws IOException, MalformedURLException, HDSException {

        HDSReference r1 = new HDSReference( containerFile,
                                            new String[] { "more",
                                                           "ccdpack" } );
        HDSReference r2 = new HDSReference( r1.getURL() );

        // getContainerFile
        assertEquals( r1.getContainerFile().getCanonicalPath(),
                      r2.getContainerFile().getCanonicalPath() );

        // getContainerName
        assertEquals( r1.getContainerName(), r2.getContainerName() );

        // getPath
        assertArrayEquals( r1.getPath(), r2.getPath() );

        // getURL
        assertEquals( r1.getURL().toString(), r2.getURL().toString() );

        // clone
        HDSReference r4 = (HDSReference) r1.clone();
        assertTrue( ! ( r4 == r1 ) );
        assertTrue( r4.getURL().equals( r1.getURL() ) );
        r4.push( "ELEMENT" );
        assertTrue( ! r4.getURL().equals( r1.getURL() ) );

        // getObject
        HDSObject o = r1.getObject( "READ" );
        assertEquals( o.datName(), "CCDPACK" );
        o.datAnnul();

        HDSReference r3 = new HDSReference( containerName + ".more.ccdpack" );
        assertEquals( r3.getContainerFile().getCanonicalPath(),
                     r1.getContainerFile().getCanonicalPath() );
        assertArrayEquals( r3.getPath(), r1.getPath() );
    }

    public void testArrayStructure() throws HDSException, IOException {
        HDSReference cref = new HDSReference( containerFile );
        HDSObject top = cref.getObject( "READ" );
        ArrayStructure simpAry = 
            new ArrayStructure( top.datFind( "VARIANCE" ) );
        ArrayStructure primAry =
            new ArrayStructure( simpAry.getData() );

        assertEquals( "SIMPLE", simpAry.getStorage() );
        assertEquals( "PRIMITIVE", primAry.getStorage() );

        OrderedNDShape ssh = simpAry.getShape();
        OrderedNDShape psh = primAry.getShape();
        assertEquals( ssh.getOrder(), Order.COLUMN_MAJOR );
        assertTrue( ! ssh.equals( psh ) );
        assertArrayEquals( ssh.getDims(), psh.getDims() );
        assertArrayNotEquals( ssh.getOrigin(), psh.getOrigin() );

        HDSType styp = simpAry.getType();
        HDSType ptyp = primAry.getType();
        assertEquals( styp, ptyp );

        String[] trace0 = new String[ 2 ];
        String[] trace1 = new String[ 2 ];
        String[] trace2 = new String[ 2 ];
        String[] trace3 = new String[ 2 ];
        simpAry.getData().hdsTrace( trace0 );
        primAry.getData().hdsTrace( trace1 );
        primAry.getHDSObject().hdsTrace( trace2 );
        simpAry.getHDSObject().hdsTrace( trace3 );
        assertArrayEquals( trace0, trace1 );
        assertArrayEquals( trace0, trace2 );
        assertArrayNotEquals( trace0, trace3 );
        
        top.datAnnul();
 
        HDSObject wtop = cref.getObject( "UPDATE" );
        String name = "new1";
        HDSType htype = HDSType._INTEGER;
        NDShape shape = new NDShape( new long[] { 21, 31, 41, },
                                     new long[] { 2, 4, 8 } );
        int npix = (int) shape.getNumPixels();
        ArrayStructure wary = new ArrayStructure( wtop, name, htype, shape );
        int[] wbuf = new int[ npix ];
        fillRandom( wbuf, -100, 100 );
        wary.getData().datPutvi( wbuf );
        wtop.datAnnul();

        HDSObject rtop = cref.getObject( "READ" );
        ArrayStructure rary = new ArrayStructure( rtop.datFind( name ) );
        int[] rbuf = rary.getData().datGetvi();
        rtop.datAnnul();

        assertArrayEquals( wbuf, rbuf );
    }

    public void testHDSType() {
        HDSType htype;

        htype = HDSType._REAL;
        assertNotNull( htype.getBadValue() );

        assertEquals( HDSType.fromName( "_BYTE" ), HDSType._BYTE );
        assertEquals( HDSType.fromName( "_UBYTE" ), HDSType._UBYTE );
        assertEquals( HDSType.fromName( "_WORD" ), HDSType._WORD );
        assertEquals( HDSType.fromName( "_UWORD" ), HDSType._UWORD );
        assertEquals( HDSType.fromName( "_INTEGER" ), HDSType._INTEGER );
        assertEquals( HDSType.fromName( "_real" ), HDSType._REAL );
        assertEquals( HDSType.fromName( "_double" ), HDSType._DOUBLE );
        assertNull( HDSType.fromName( "_CHAR*80" ) );
        assertNull( HDSType.fromName( "_logical" ) );
        assertNull( HDSType.fromName( "NDF" ) );
    }

}
