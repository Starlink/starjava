package uk.ac.starlink.hds;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.DoubleBuffer;
import uk.ac.starlink.util.TestCase;

public class HDSObjectTest extends TestCase {

    public static String NDF_FILE = "uk/ac/starlink/hds/reduced_data1.sdf";
    public static String containerName;
    public static File containerFile;
    public static HDSObject top;
    public static boolean tried;
    public static HDSObject dataArray;
    public static HDSObject wcsArray;
    public static HDSObject moreObj;
    public static HDSObject cell;
    public static long[] shape;

    public HDSObjectTest( String name ) {
        super( name );
    }

    protected void setUp() throws HDSException, IOException {
        assertTrue( HDSPackage.isAvailable() );
        if ( ! tried ) {
            tried = true;
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
            top = HDSObject.hdsOpen( containerName, "UPDATE" );
            dataArray = top.datFind( "DATA_ARRAY" ).datFind( "DATA" );
            wcsArray = top.datFind( "WCS" ).datFind( "DATA" );
            moreObj = top.datFind( "MORE" );
            cell = dataArray.datCell( new long[] { 10L, 10L } );
            shape = dataArray.datShape();
        }
    }

    public void testTuning() throws HDSException {

        int defaultMapMode = HDSObject.hdsGtune( "MAP" );
        HDSObject.hdsTune( "MAP", defaultMapMode );
        int newMapMode = HDSObject.hdsGtune( "MAP" );
        assertTrue( defaultMapMode == newMapMode );

        //  64bit/32bit mode. Leave switched on for further tests. Should
        //  be on by default (behaviour has changed).
        int default64BitMode = HDSObject.hdsGtune( "64BIT" );
        System.out.println( "default 64bit mode = " + 
                            ( ( default64BitMode == 1 ) ? "on" : "off" ) );
        HDSObject.hdsTune( "64BIT", 0 );
        int new64BitMode = HDSObject.hdsGtune( "64BIT" );
        System.out.println( "test 64bit mode = " +
                            ( ( new64BitMode == 1 ) ? "on" : "off" ) );
        assertTrue( default64BitMode != new64BitMode );

        //  Back to 64bit for tests.
        HDSObject.hdsTune( "64BIT", 1 );
    }

    public void testConstants() throws HDSException {
        assertEquals( 15, HDSObject.getHDSConstantI( "DAT__SZNAM" ) );
        assertTrue( 15 <= HDSObject.getHDSConstantI( "DAT__SZLOC" ) );
    }

    public void testFileOperations() throws HDSException {

        // hdsOpen
        try {
            HDSObject dummy = 
                HDSObject.hdsOpen( "Sir_Not-appearing-in-this-directory",
                                   "READ" );
            fail( "Should have thrown exception" );
        }
        catch ( HDSException e ) {
            // Open fails as expected.
            // Details depend on HDS version.
        }

        // hdsNew
        String newName = "newHDS";
        File newFile = new File( System.getProperty( "java.io.tmpdir" ) 
                               + File.separatorChar + newName + ".sdf" );
        newFile.deleteOnExit();
        long[] newD = new long[] { 10 };
        HDSObject newHDS = 
            HDSObject.hdsNew( newFile.toString(), newName, "_DOUBLE", newD );
        assertArrayEquals( newD, newHDS.datShape() );
        assertEquals( "_DOUBLE", newHDS.datType() );
        newHDS.datAnnul();
    }

    public void testObject() throws HDSException {

        // datClone
        HDSObject top2 = top.datClone();

        // datPrmry
        assertTrue( ! top2.datPrmry() );
        top2.datPrmry( true );
        assertTrue( top2.datPrmry() );

        // datAnnul
        assertTrue( top2.datValid() );
        top2.datAnnul();
        assertTrue( ! top2.datValid() );
    }

    public void testProperties() throws HDSException {

        // datValid
        assertTrue( top.datValid() );

        // datName
        assertEquals( "REDUCED_DATA1", top.datName() );

        // datType
        assertEquals( "NDF", top.datType() );

        // datIndex
        int ncomp = 0;
        assertEquals( top.datIndex( ++ncomp ).datName(), "DATA_ARRAY" );
        assertEquals( top.datIndex( ++ncomp ).datName(), "VARIANCE" );
        assertEquals( top.datIndex( ++ncomp ).datName(), "MORE" );
        assertEquals( top.datIndex( ++ncomp ).datName(), "TITLE" );
        assertEquals( top.datIndex( ++ncomp ).datName(), "WCS" );

        // datNcomp
        assertTrue( top.datNcomp() == ncomp );

        // datThere
        assertTrue( top.datThere( "DATA_ARRAY" ) );
        assertTrue( ! top.datThere( "Sir N.A.I.T.H." ) );

        // datParen
        assertEquals( moreObj.datName(),
                      moreObj.datIndex( 1 ).datParen().datName() );
        try {
            top.datParen();
            fail( "Should have thrown" );
        }
        catch ( HDSException e ) {}

        // datShape
        long[] sh = dataArray.datShape();
        assertEquals( 2, sh.length );
        assertEquals( 114L, sh[ 0 ] );
        assertEquals( 128L, sh[ 1 ] );
        long[] wcsShape = wcsArray.datShape();
        assertEquals( 1, wcsShape.length );

        // datSize
        assertEquals( wcsArray.datGetvc().length, wcsArray.datSize() );
        assertEquals( sh[ 0 ] * sh[ 1 ], (long) dataArray.datSize() );

        // datStruc
        assertTrue( top.datStruc() );
        assertTrue( ! cell.datStruc() );

        // datRef
        assertTrue( top.datFind( "VARIANCE" ).datFind( "ORIGIN" ).datRef()
                       .endsWith( "VARIANCE.ORIGIN" ) );

        // hdsTrace
        String[] sres = new String[ 2 ];
        int nlev = 
            top.datFind( "VARIANCE" ).datFind( "ORIGIN" ).hdsTrace( sres );
        assertEquals( 3, nlev );
        assertEquals( "REDUCED_DATA1.VARIANCE.ORIGIN", sres[ 0 ] );
        assertEquals( containerFile.getAbsolutePath(),
                      new File( sres[ 1 ] ).getAbsolutePath() );
    }

    public void testGet() throws HDSException {

        // datGet0c
        long[] first = new long[] { 1L };
        long[] last = wcsArray.datShape();
        HDSObject firstWcsLine = wcsArray.datCell( first );
        HDSObject lastWcsLine = wcsArray.datCell( last );
        assertTrue( firstWcsLine.datGet0c().startsWith( " Begin " ) );
        assertTrue( lastWcsLine.datGet0c().startsWith( " End " ) );

        // datGet0x
        double dval = cell.datGet0d();
        float rval = cell.datGet0r();
        assertEquals( rval, (float) dval );
        int ival = cell.datGet0i();
        assertEquals( ival, (int) dval );
        boolean lval = cell.datGet0l();

        // datGet0c
        HDSObject fitsobj = moreObj.datFind( "FITS" );
        long[] fshape = fitsobj.datShape();
        String[] fitses = (String[]) fitsobj.datGetc( fshape );
        for ( int i = 5; i < 10; i++ ) {
            assertEquals( fitsobj.datCell( new long[] { i + 1 } ).datGet0c(), 
                          fitses[ i ] );
        }
        String tit = (String) top.datFind( "TITLE" ).datGetc( new long[ 0 ] );
        assertTrue( tit.startsWith( "Output" ) );

        // datGetx
        float[][] fdat = (float[][]) dataArray.datGetr( shape );
        double[][] ddat = (double[][]) dataArray.datGetd( shape );
        int[][] idat = (int[][]) dataArray.datGeti( shape );
        long[] pos = new long[ 2 ];
        for ( int i = 5; i < 10; i++ ) {
            for ( int j = 21; j < 26; j++ ) {
                pos[ 0 ] = j + 1;
                pos[ 1 ] = i + 1;
                HDSObject celpt = dataArray.datCell( pos );
                assertEquals( celpt.datGet0r(), fdat[ i ][ j ] );
                assertEquals( celpt.datGet0d(), ddat[ i ][ j ] );
                assertEquals( celpt.datGet0i(), idat[ i ][ j ] );
            }
        }
        HDSObject origin = top.datFind( "VARIANCE" ).datFind( "ORIGIN" );
        int[] oc = (int[]) origin.datGeti( origin.datShape() );
        assertEquals( 6, oc[ 0 ] );
        assertEquals( 1, oc[ 1 ] );

        // datGetvx
        float[] fvdat = dataArray.datGetvr();
        double[] dvdat = dataArray.datGetvd();
        int[] ivdat = dataArray.datGetvi();
        int size = (int) dataArray.datSize();
        assertEquals( size, fvdat.length );
        assertEquals( size, dvdat.length );
        assertEquals( size, ivdat.length );
        for ( int i = 20; i < 23; i++ ) {
            assertEquals( fdat[ 0 ][ i ], fvdat[ i ] );
            assertEquals( ddat[ 0 ][ i ], dvdat[ i ] );
            assertEquals( idat[ 0 ][ i ], ivdat[ i ] );
        }

        // datGetvc
        String[] svdat = wcsArray.datGetvc();
        long[] spos = new long[ 1 ];
        for ( int i = 0; i < svdat.length; i++ ) {
            spos[ 0 ] = i + 1;
            assertEquals( svdat[ i ], wcsArray.datCell( spos ).datGet0c() );
        }



    }

    public void testPut() throws HDSException {

        // datPutvc
        String[] svdat = wcsArray.datGetvc();
        String[] svdat1 = (String[]) svdat.clone();
        svdat1[ 0 ] = "hey";
        svdat1[ 2 ] = "nonny";
        svdat1[ svdat1.length - 1 ] = "no";
        wcsArray.datPutvc( svdat1 );
        String[] svdat2 = wcsArray.datGetvc();
        assertArrayNotEquals( svdat, svdat2 );
        assertArrayEquals( svdat1, svdat2 );
        wcsArray.datPutvc( svdat );  // restore it

        // datPut0c
        HDSObject scell = wcsArray.datCell( new long[] { 3L } );
        String celval = scell.datGet0c();
        assertTrue( ! scell.datGet0c().equals( "Eric" ) );
        scell.datPut0c( "Eric" );
        assertEquals( "Eric", scell.datGet0c() );
        scell.datPut0c( celval );  // restore it

        // datPutvx
        float[] fvdat = dataArray.datGetvr();
        double[] dvdat = dataArray.datGetvd();
        int[] ivdat = dataArray.datGetvi();
        int size = (int) dataArray.datSize();
        float[] fvdat1 = (float[]) fvdat.clone();
        double[] dvdat1 = (double[]) dvdat.clone();
        int[] ivdat1 = (int[]) ivdat.clone();
        fvdat1[ 2 ] += 1.0;
        dvdat1[ 2 ] += 2.0;
        dvdat1[ 0 ] -= 100.;
        ivdat1[ 2 ] += 3;
        ivdat1[ size - 1 ] -= 10;

        dataArray.datPutvd( dvdat1 );
        double[] dvdat2 = dataArray.datGetvd();
        assertArrayNotEquals( dvdat, dvdat2 );
        assertArrayEquals( dvdat1, dvdat2 );

        dataArray.datPutvr( fvdat1 );
        float[] fvdat2 = dataArray.datGetvr();
        assertArrayNotEquals( fvdat, fvdat2 );
        assertArrayEquals( fvdat1, fvdat2 );

        dataArray.datPutvi( ivdat1 );
        int[] ivdat2 = dataArray.datGetvi();
        assertArrayNotEquals( ivdat, ivdat2 );
        assertArrayEquals( ivdat1, ivdat2 );

        dataArray.datPutvd( dvdat );  // restore it.

        // datPut0x
        double dcval = cell.datGet0d();
        cell.datPut0d( dcval + 3.5 );
        assertEquals( dcval + 3.5, cell.datGet0d() );
        cell.datPut0r( (float) (dcval + 4.5) );
        assertEquals( (float) (dcval + 4.5), cell.datGet0r() );
        cell.datPut0i( (int) (dcval + 23) );
        assertEquals( (int) (dcval + 23), cell.datGet0i() );
        cell.datPut0d( dcval );  // restore it.
    }

    public void testWrite() throws HDSException {

        // datNew
        String newExt = "HDSTEST";
        assertTrue( ! moreObj.datThere( newExt ) );
        long[] newDims = new long[] { 3, 4, 5, 6 };
        moreObj.datNew( newExt, "_REAL", newDims );
        assertTrue( moreObj.datThere( newExt ) );
        HDSObject newObj = moreObj.datFind( newExt );
        assertArrayEquals( newObj.datShape(), newDims );

        // datState
        assertTrue( ! newObj.datState() );
        newObj.datPutvr( new float[ (int) newObj.datSize() ] );
        assertTrue( newObj.datState() );

        // datErase
        newObj.datAnnul();
        assertTrue( moreObj.datThere( newExt ) );
        moreObj.datErase( newExt );
        assertTrue( ! moreObj.datThere( newExt ) );
    }

    public void testMap() throws HDSException {

        // datMapv
        DoubleBuffer datbuf = (DoubleBuffer)
                              dataArray.datMapv( "_DOUBLE", "READ" );
        double[] dvdat = dataArray.datGetvd();
        int size = dvdat.length;
        assertEquals( datbuf.capacity(), size );
        assertTrue( datbuf.isDirect() );
        assertTrue( ! datbuf.hasArray() );
        int nel = 0;
        while ( datbuf.hasRemaining() ) {
            assertEquals( dvdat[ nel++ ], datbuf.get() );
        }
        assertEquals( nel, size );
        assertEquals( nel, datbuf.capacity() );

        // datUnmap
        dataArray.datUnmap();

    }

    //  Commented out because it's verbose.
    //  public void testShow() throws HDSException {
    //      HDSObject.hdsShow( "DATA" );
    //      HDSObject.hdsShow( "FILES" );
    //      HDSObject.hdsShow( "LOCATORS" );
    //  }


    public void testCopy() throws HDSException {

        // datCopy of CCDPACK extension
        String newExt = "CCDPACKCOPY";
        String oldExt = "CCDPACK";
        assertTrue( moreObj.datThere( oldExt ) );
        assertTrue( ! moreObj.datThere( newExt ) );
        HDSObject oldObj = moreObj.datFind( oldExt );
        oldObj.datCopy( moreObj, newExt );
        assertTrue( moreObj.datThere( newExt ) );
        
        oldObj.datAnnul();
    }
}
