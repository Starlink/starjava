package uk.ac.starlink.fits;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import uk.ac.starlink.util.DataBufferedInputStream;
import uk.ac.starlink.util.DataBufferedOutputStream;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.TestCase;

public class BasicInputTest extends TestCase {

    private final int isiz = 44;
    private final int count = 23;
    private final int arrsiz = 7;

    public BasicInputTest() {
        LogUtils.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.WARNING );
    }

    public void testInput() throws IOException {
        File file = File.createTempFile( "tmp", ".dat" );
        DataBufferedOutputStream out =
            new DataBufferedOutputStream( new FileOutputStream( file ) );
        byte[] buf10 = new byte[ 10 ];
        for ( int i = 0; i < count; i++ ) {
            out.write( byteFor( i ) );
            out.writeShort( shortFor( i ) );
            out.writeInt( intFor( i ) );
            out.write( buf10 );
            out.writeLong( longFor( i ) );
            out.writeFloat( floatFor( i ) );
            out.writeDouble( doubleFor( i ) );
            out.write( byteArrayFor( i ), 0, arrsiz );
        } // isiz bytes
        out.close();
        int leng = (int) file.length();
        assertEquals( isiz * count, leng );
        int off1 = isiz;
        int leng1 = leng - isiz;

        FileChannel chan = new RandomAccessFile( file, "r" ).getChannel();
        exerciseInput( seqOffInput( off1, new FileInputStream( file ) ) );
        exerciseInput( seqOffInput( off1,
                                    new DataBufferedInputStream(
                                        new FileInputStream( file ) ) ) );
        exerciseInput( seqOffInput( off1,
                                    new DataBufferedInputStream(
                                        new FileInputStream( file ), 29 ) ) );
        BufferManager simpleMan =
            new BufferManager( chan, off1, leng1, "test", (Unmapper) null );
        exerciseInput( new SimpleMappedInput( simpleMan ) );
        simpleMan.close();
        exerciseBlockInput( chan, off1, leng1, isiz + 3, 0 );
        exerciseBlockInput( chan, off1, leng1, isiz + 8, 10 );
        exerciseBlockInput( chan, off1, leng1, leng1/2, 100 );
        exerciseBlockInput( chan, off1, leng1, leng1, 0 );
        exerciseBlockInput( chan, off1, leng1, leng1*2, 0 );

        Unmapper unmapper = Unmapper.getInstance();
        UnmappableBuffer ubuf = unmapper.mapFile( chan, isiz, isiz );
        assertEquals( (byte) 1, ubuf.getBuffer().get( 0 ) );
        boolean isUnmapped = ubuf.unmapBuffer();

        // This ought to work but if it doesn't it's not catastrophic;
        // it may be that the JRE is just not capable of buffer unmapping.
        // I haven't come across any such so far, e.g. all Oracle JDKs between
        // versions 6 and 26 can do it, but there may be some that don't.
        // In that case this test could be commented out or otherwise skipped.
        assertTrue( isUnmapped );

        Unmapper nopUnmapper = Unmapper.NOP;
        UnmappableBuffer ubuf1 = nopUnmapper.mapFile( chan, isiz * 2, isiz * 2);
        assertEquals( (byte) 2, ubuf1.getBuffer().get( 0 ) );
        assertFalse( ubuf1.unmapBuffer() );

        // Note this one fails: the EOFException is not thrown at the
        // right place.  Hmm.
//      exerciseInput( seqOffInput( off1,
//                                  new BufferedDataInputStream(
//                                      new FileInputStream( file ) ) ) );

    }

    public void testUnmapper() {
        Unmapper unmapper = Unmapper.getInstance();
        String unmapperName = unmapper.toString();
        int jvers = TestCase.getJavaMajorVersion();

        // It's not really critical that these tests pass in exactly this form,
        // but they are here to make sure that different JREs
        // at least don't all use the same Unmapper implementation.
        if ( jvers >= 22 ) {
            assertEquals( "MemorySegment", unmapperName );
        }
        else if ( jvers >= 19 ) {
            assertTrue( unmapperName.equals( "MemorySegment" ) ||
                        unmapperName.equals( "Unsafe" ) );
        }
        else if ( jvers >= 9 ) {
            assertEquals( "Unsafe", unmapperName );
        }
        else {
            assertTrue( unmapperName.equals( "Cleaner" ) ||
                        unmapperName.equals( "Unsafe" ) );
        }
    }

    private void exerciseBlockInput( FileChannel chan, long off, long leng,
                                     int blockSize, int expiryMillis )
            throws IOException {
        BlockManager blockMan =
            new BlockManager( chan, off, leng, "test", Unmapper.getInstance(),
                              blockSize );
        exerciseInput( BlockMappedInput.createInput( blockMan, expiryMillis ) );
        blockMan.close();
    }

    private void exerciseInput( BasicInput in ) throws IOException {
        List<Integer> ixList = new ArrayList<Integer>();
        for ( int i = 1; i < count; i++ ) {
            assertEquals( byteFor( i ), in.readByte() );
            assertEquals( shortFor( i ), in.readShort() );
            assertEquals( intFor( i ), in.readInt() );
            in.skip( 10 );
            assertEquals( longFor( i ), in.readLong() );
            assertEquals( floatFor( i ), in.readFloat() );
            assertEquals( doubleFor( i ), in.readDouble() );
            byte[] barr = new byte[ arrsiz ];
            in.readBytes( barr );
            assertArrayEquals( byteArrayFor( i ), barr );
            ixList.add( Integer.valueOf( i ) );
        }
        try {
            in.readByte();
            fail();
        }
        catch ( EOFException e ) {
            // ok
        }

        if ( in.isRandom() ) {
            Collections.shuffle( ixList, new Random( 555 ) );
            for ( int i : ixList ) {
                int i1 = i - 1;
                in.seek( isiz * i1 + 3 );
                assertEquals( isiz * i1 + 3, in.getOffset() );
                assertEquals( intFor( i ), in.readInt() );
                assertEquals( isiz * i1 + 7, in.getOffset() );
                in.skip( 10 );
                assertEquals( isiz * i1 + 17, in.getOffset() );
                assertEquals( longFor( i ), in.readLong() );
                assertEquals( isiz * i1 + 25, in.getOffset() );
            }
            in.seek( isiz * ( count - 1 ) );
            try {
                in.seek( isiz  * ( count - 1 ) + 1 );
                fail();
            }
            catch ( EOFException e ) {
                // ok
            }
        }
        in.close();
    }

    private static BasicInput seqOffInput( int offset, InputStream in )
            throws IOException {
        BasicInput input = InputFactory.createSequentialInput( in );
        input.skip( offset );
        return input;
    }

    private static byte byteFor( int i ) {
        return (byte) i;
    }
    private static short shortFor( int i ) {
        return (short) ( 1000 + i );
    }
    private static int intFor( int i ) {
        return 100000 + i;
    }
    private static long longFor( int i ) {
        return -100 - i;
    }
    private static float floatFor( int i ) {
        return i + 0.5f;
    }
    private static double doubleFor( int i ) {
        return 100 + i + 0.25;
    }
    private byte[] byteArrayFor( int i ) {
        byte[] barr = new byte[ arrsiz ];
        Arrays.fill( barr, (byte) i );
        return barr;
    }
}
