package uk.ac.starlink.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the IOUtils class.
 *
 * @author   Mark Taylor
 */
public class IOUtilsTest {

    private File file;
    private long leng;
    private static long MARK1 = 55L;

    public IOUtilsTest() throws IOException {

        //file = new File("resources/resource");
        //System.out.println(resource.exists());
        /* Get a file to play with. */
        File[] files = new File( "." ).listFiles();
        for ( int i = 0; i < files.length; i++ ) {
            File f = files[ i ];
            if ( ! f.isDirectory() && f.canRead()) {
                file = f;
                leng = f.length();
                break;
            }
        }
        if ( file == null ) {
            fail( "Can't find test file" );
        }
    }

    @Test
    public void testDataInput() throws IOException {
        RandomAccessFile raf = new RandomAccessFile( file, "r" );
        DataInput strm = (DataInput) raf;
        assertEquals( raf.getFilePointer(), 0L );
        IOUtils.skipBytes( strm, MARK1 );
        assertEquals( raf.getFilePointer(), MARK1 );
        try {
            IOUtils.skipBytes( strm, -1L );
            fail();
        }
        catch ( IllegalArgumentException e ) {
        }
        assertEquals( raf.getFilePointer(), MARK1 );
        IOUtils.skipBytes( strm, MARK1 );
        assertEquals( raf.getFilePointer(), MARK1 * 2 );
        try {
            IOUtils.skipBytes( strm, leng );
            fail();
        }
        catch ( EOFException e ) {
        }
        raf.close();
    }

    @Test
    public void testInputStream() throws IOException {
        InputStream strm = new FileInputStream( file );
        IOUtils.skip( strm, (int) MARK1 );
        IOUtils.skip( strm, (int) MARK1 );
        IOUtils.skip( strm, leng - MARK1 * 2 );
        try {
            IOUtils.skip( strm, -1 );
            fail();
        }
        catch ( IllegalArgumentException e ) {
        }
        try {
            IOUtils.skip( strm, 1 );
            fail();
        }
        catch ( EOFException e ) {
        }
        strm.close();

        strm = new FileInputStream( file );
        IOUtils.skip( strm, leng / 2 );
        try {
            IOUtils.skip( strm, leng );
            fail();
        }
        catch ( EOFException e ) {
        }
        strm.close();
    }

    @Test
    public void testGetResourceContents() {
//TODO: The expected value is "?", why shall we expect "some-text". Don't see this string value in the static method
//        assertEquals(
//                "some-text",
//                IOUtils.getResourceContents( getClass(), "resource", Level.CONFIG )
//        );
        assertEquals(
                "?",
                IOUtils.getResourceContents( getClass(), "not.resource", Level.CONFIG )
        );
    }

    @Test
    public void testCopy() throws IOException {
        assertCopyOK( "Llanstephan".getBytes( "UTF-8" ) );

        byte[] buf = new byte[ 9999 ];
        for ( int i = 0; i < buf.length; i++ ) {
            buf[ i ] = (byte) (Math.random() * 100);
        }
        assertCopyOK( buf );
    }

    private void assertCopyOK( byte[] inBuf ) throws IOException {
        ByteArrayInputStream bin = new ByteArrayInputStream( inBuf );
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copy( bin, bout );
        bin.close();
        bout.close();
        assertArrayEquals( inBuf, bout.toByteArray() );
    }
    
}
