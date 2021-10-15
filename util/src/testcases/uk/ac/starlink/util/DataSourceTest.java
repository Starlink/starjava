package uk.ac.starlink.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import uk.ac.starlink.util.bzip2.CBZip2OutputStream;
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
    private DataSource rSrc;
    private DataSource[] allSources;

    public DataSourceTest( String name ) {
        super( name );
    }

    public void setUp() throws IOException {
        pSrc = new PlainDataSource( DataSource.DEFAULT_INTRO_LIMIT );
        gSrc = new GzipDataSource( DataSource.DEFAULT_INTRO_LIMIT );
        bSrc = new Bzip2DataSource( DataSource.DEFAULT_INTRO_LIMIT );
        rSrc = new ResourceDataSource( RESOURCE_NAME.substring( 1 ) );
        ((ResourceDataSource) rSrc).setClassLoader( ClassLoader
                                                   .getSystemClassLoader() );
        int lo = 15;
        int hi = 1024 * 1024;
        allSources = new DataSource[] { 
            pSrc, bSrc, gSrc, rSrc,
            new PlainDataSource( lo ),
            new PlainDataSource( hi ),
            new GzipDataSource( lo ),
            new GzipDataSource( hi ),
            new Bzip2DataSource( lo ),
            new Bzip2DataSource( hi ),
        };
    }

    public void testData() throws IOException {

        assertEquals( Compression.NONE, pSrc.getCompression() );
        assertEquals( Compression.GZIP, gSrc.getCompression() );
        assertEquals( Compression.BZIP2, bSrc.getCompression() );
        assertEquals( Compression.NONE, rSrc.getCompression() );

        byte[] pIntro = pSrc.getIntro();
        byte[] gIntro = gSrc.getIntro();
        byte[] bIntro = bSrc.getIntro();
        byte[] rIntro = rSrc.getIntro();
        assertArrayEquals( pIntro, gIntro );
        assertArrayEquals( pIntro, bIntro );
        assertArrayEquals( pIntro, rIntro );
    }

    public void testIntro() throws IOException {
        for ( int i = 0; i < allSources.length; i++ ) {
            DataSource src = allSources[ i ];
            byte[] intro = src.getIntro();
            assertTrue( intro.length <= src.getIntroLimit() );
            byte[] buf = new byte[ magic.length ];
            System.arraycopy( intro, 0, buf, 0, buf.length );
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

    public void testHybridStream() throws IOException {
        int leng = 99;
        for ( int i = 0; i < allSources.length; i++ ) {
            DataSource src = allSources[ i ];
            byte[] buf = new byte[ leng ];
            assertArrayEquals( fillBuffer( src.getInputStream() ),
                               fillBuffer( src.getHybridInputStream() ) );
            assertArrayEquals( fillBuffer( src.getInputStream() ),
                               fillBuffer1( src.getHybridInputStream() ) );
        }
    }

    public void testCombo1() throws IOException {
        testIntro();
        testStream();
        testData();
        testIntro();
        testStream();
        testHybridStream();
        testStream();
        testStream();
        testHybridStream();
    }

    public void testResourceStream() {
        assertEquals( "Object.class", rSrc.getName() );
        assertTrue( rSrc.getURL().toString()
                        .endsWith( "/java/lang/Object.class" ) );
    }

    private static byte[] fillBuffer1( InputStream istrm ) throws IOException {
        ByteArrayOutputStream ostrm = new ByteArrayOutputStream();
        for ( int b; ( b = istrm.read() ) >= 0; ) {
            ostrm.write( b );
        }
        istrm.close();
        ostrm.close();
        return ostrm.toByteArray();
    }

    private static byte[] fillBuffer( InputStream istrm ) throws IOException {
        ByteArrayOutputStream ostrm = new ByteArrayOutputStream();
        int leng = 17;
        byte[] buf = new byte[ leng ];
        for ( int i = 0, n; ( n = istrm.read( buf ) ) >= 0; i += n ) {
            ostrm.write( buf, 0, n );
        }
        istrm.close();
        ostrm.close();
        return ostrm.toByteArray();
    }

    private static InputStream getPlainStream() throws IOException {
        return DataSourceTest.class.getResourceAsStream( RESOURCE_NAME );
    }

    private static class PlainDataSource extends DataSource {
        public PlainDataSource( int introLimit ) {
            super( introLimit );
        }
        protected InputStream getRawInputStream() throws IOException {
            return getPlainStream();
        }
        public String getName() {
            return RESOURCE_NAME;
        }
        public URL getURL() {
            return null;
        }
    } 

    private static class GzipDataSource extends DataSource {
        private byte[] zbuf;
        private GzipDataSource( int introLimit ) throws IOException {
            super( introLimit );
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
        public URL getURL() {
            return null;
        }
    }

    private static class Bzip2DataSource extends DataSource {
        private byte[] zbuf;
        private Bzip2DataSource( int introLimit ) throws IOException {
            super( introLimit );
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
        public URL getURL() {
            return null;
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
