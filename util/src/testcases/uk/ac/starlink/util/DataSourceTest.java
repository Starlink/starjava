package uk.ac.starlink.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;
import uk.ac.starlink.util.TestCase;

public class DataSourceTest extends TestCase {

    private static final String RESOURCE_NAME = "/java/lang/Object.class";
    private static final byte[] magic = new byte[] {
        (byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe,
    };
    private static final int MAXBUF = 512;

    private DataSource pSrc;
    private DataSource gSrc;
    private DataSource bSrc;
    private DataSource[] allSources;

    public DataSourceTest( String name ) {
        super( name );
    }

    public void setUp() throws IOException {
        pSrc = new PlainDataSource();
        gSrc = new GzipDataSource();
        bSrc = new Bzip2DataSource();
        allSources = new DataSource[] { pSrc, bSrc, gSrc, };
        // try {
        //  outputTestStreams( new File( "/mbt/data/treeview/compress" ) );
        // }
        // catch ( IOException e ) {
        //     // just diagnostic - doesn't matter
        // }
    }

    public void testData() throws IOException {

        assertEquals( Compression.NONE, pSrc.getCompression() );
        assertEquals( Compression.GZIP, gSrc.getCompression() );
        assertEquals( Compression.BZIP2, bSrc.getCompression() );

        int nMagic = 23;
        byte[] pMagic = new byte[ nMagic ];
        byte[] gMagic = new byte[ nMagic ];
        byte[] bMagic = new byte[ nMagic ];
        assertEquals( nMagic, pSrc.getMagic( pMagic ) );
        assertEquals( nMagic, gSrc.getMagic( gMagic ) );
        assertEquals( nMagic, bSrc.getMagic( bMagic ) );
        assertArrayEquals( pMagic, gMagic );
        assertArrayEquals( pMagic, bMagic );
    }

    public void testMagic() throws IOException {
        for ( int i = 0; i < allSources.length; i++ ) {
            DataSource src = allSources[ i ];
            byte[] buf = new byte[ magic.length ];
            assertEquals( magic.length, src.getMagic( buf ) );
            assertArrayEquals( magic, buf );
        }
    }

    public void testStream() throws IOException {
        for ( int i = 0; i < allSources.length; i++ ) {
            DataSource src = allSources[ i ];
            byte[] buf = new byte[ magic.length ];
            InputStream strm = src.getInputStream();
            assertEquals( magic.length, strm.read( buf ) );
            strm.close();
            assertArrayEquals( magic, buf );
        }
    }

    public void testFlags() throws IOException {
        for ( int i = 0; i < allSources.length; i++ ) {
            DataSource src = allSources[ i ];
            assertTrue( ! src.isASCII() );
            assertTrue( ! src.isEmpty() );
            assertTrue( ! src.isHTML() );
        }
    }

    public void testCombo1() throws IOException {
        testFlags();
        testMagic();
        testStream();
        testData();
        testFlags();
        testMagic();
        testStream();
        testStream();
        testStream();
    }

    private static InputStream getPlainStream() throws IOException {
        return DataSourceTest.class.getResourceAsStream( RESOURCE_NAME );
    }

    private static class PlainDataSource extends DataSource {
        protected InputStream getRawInputStream() throws IOException {
            return getPlainStream();
        }
        public String getName() {
            return RESOURCE_NAME;
        }
    } 

    private static class GzipDataSource extends DataSource {
        private byte[] zbuf;
        private GzipDataSource() throws IOException {
            ByteArrayOutputStream bstrm = new ByteArrayOutputStream();
            OutputStream zstrm = new GZIPOutputStream( bstrm );
            InputStream istrm = getPlainStream();

            byte[] buf = new byte[ MAXBUF ];
            for ( int nbyte; (nbyte = istrm.read( buf )) > 0; ) {
                zstrm.write( buf, 0, nbyte );
            }
            istrm.close();
            zstrm.close();
            zbuf = bstrm.toByteArray();
        }
        protected InputStream getRawInputStream() throws IOException {
            return new ByteArrayInputStream( zbuf );
        }
        public String getName() {
            return RESOURCE_NAME + ".gz";
        }
    }

    private static class Bzip2DataSource extends DataSource {
        private byte[] zbuf;
        private Bzip2DataSource() throws IOException {
            ByteArrayOutputStream bstrm = new ByteArrayOutputStream();
            bstrm.write( (byte) 'B' );
            bstrm.write( (byte) 'Z' );
            OutputStream zstrm = new CBZip2OutputStream( bstrm );
            InputStream istrm = getPlainStream();

            byte[] buf = new byte[ MAXBUF ];
            for ( int nbyte; (nbyte = istrm.read( buf )) > 0; ) {
                zstrm.write( buf, 0, nbyte );
            }
            istrm.close();
            zstrm.close();
            zbuf = bstrm.toByteArray();
        }
        protected InputStream getRawInputStream() throws IOException {
            return new ByteArrayInputStream( zbuf );
        }
        public String getName() {
            return RESOURCE_NAME + ".bz2";
        }
    }

    private void outputTestStreams( File dir ) throws IOException {
        for ( int i = 0; i < allSources.length; i++ ) {
            DataSource src = allSources[ i ];
            InputStream istrm = src.getRawInputStream();
            File dest = new File( dir, src.getName() );
            OutputStream ostrm = new FileOutputStream( dest );
            byte[] buf = new byte[ MAXBUF ];
            for ( int nbyte; (nbyte = istrm.read( buf )) > 0; ) {
                ostrm.write( buf, 0, nbyte );
            }
            ostrm.close();
        }
    }

}
