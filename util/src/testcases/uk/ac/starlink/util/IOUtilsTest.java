package uk.ac.starlink.util;

import java.io.DataInput;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import junit.framework.TestCase;

/**
 * Tests for the IOUtils class.
 *
 * @author   Mark Taylor
 */
public class IOUtilsTest extends TestCase {

    private File file;
    private long leng;
    private static long MARK1 = 55L;

    public IOUtilsTest( String name ) throws IOException {
        super( name );

        /* Get a file to play with. */
        File[] files = new File( "." ).listFiles();
        for ( int i = 0; i < files.length; i++ ) {
            File f = files[ i ];
            if ( ! f.isDirectory() && f.canRead() && f.length() > MARK1 * 2 ) {
                file = f;
                leng = f.length();
                break;
            }
        }
        if ( file == null ) {
            fail( "Can't find test file" );
        }
    }

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
    
}
