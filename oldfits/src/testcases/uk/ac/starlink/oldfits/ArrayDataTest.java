package uk.ac.starlink.oldfits;

import java.io.BufferedOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.BufferedDataInputStream;
import nom.tam.util.BufferedFile;
import nom.tam.util.RandomAccess;
import uk.ac.starlink.util.TestCase;

public class ArrayDataTest extends TestCase {

    private final String txt_ = "A" + "b" + "\u0393";
    private final int nc_;
    private final int cblock_;

    public ArrayDataTest( String name ) {
        super( name );
        try {
            nc_ = txt_.getBytes( "UTF-8" ).length + 2; // includes 2-byte length
        }
        catch ( UnsupportedEncodingException e ) {
            throw new AssertionError();
        }
        cblock_ = 8 + 4 + 4 + 2 + 2 + 1 + nc_;
        Logger.getLogger( "uk.ac.starlink.oldfits" ).setLevel( Level.WARNING );
    }

    public void testIo() throws IOException {
        int count = 999;
        File file = File.createTempFile( "iotest", ".fits" );
        file.deleteOnExit();
        ByteBuffer mbuf = ByteBuffer.allocate( cblock_ * count );
        DataOutputStream fout =
            new DataOutputStream(
                new BufferedOutputStream( new FileOutputStream( file ) ) );
        AbstractArrayDataIO mout = new MappedFile( mbuf );
        for ( int i = 0; i < count; i++ ) {
            writeBlock( i, fout );
            writeBlock( i, mout );
        }
        fout.close();
        assertEquals( 0, mout.remaining() );
        mout.close();
        mbuf.rewind();
        assertEquals( cblock_ * count, file.length() );

        ArrayDataInput[] ins = new ArrayDataInput[] {
            new BufferedDataInputStream( new FileInputStream( file ) ),
            new BufferedDataInputStream( new FileInputStream( file ), 31 ), 
            new BufferedFile( file.toString() ),
            new BufferedFile( file.toString(), "r", 29 ),
            new MappedFile( file.toString() ),
            new MultiMappedFile( file, FileChannel.MapMode.READ_ONLY, 1023 ),
            new MappedFile( mbuf ),
        };
        for ( int ii = 0; ii < ins.length; ii++ ) {
            ArrayDataInput in = ins[ ii ];
            for ( int i = 0; i < count; i++ ) {
                readBlock( i, in );
            }
            if ( in instanceof RandomAccess ) {
                RandomAccess rin = (RandomAccess) in;
                for ( int j = count - 1; j >= 0; j-- ) {
                    rin.seek( j * cblock_ );
                    assertEquals( j * cblock_, rin.getFilePointer() );
                    readBlock( j, rin );
                    assertEquals( ( j + 1 ) * cblock_, rin.getFilePointer() );
                }
            }
            in.close();
        }
        file.delete();
    }

    private void writeBlock( int i, DataOutput out ) throws IOException {
        out.writeByte( (byte) i );
        out.writeShort( (short) i );
        out.writeShort( (short) i );
        out.writeInt( i );
        out.writeFloat( (float) i );
        out.writeDouble( (float) i );
        out.writeUTF( txt_ );
    }

    private void readBlock( int i, ArrayDataInput in ) throws IOException {
        int phase = i % 4;
        if ( phase == 0 ) {
            assertEquals( (byte) i, in.readByte() );
            assertEquals( (short) i, in.readShort() );
            assertEquals( (int) i, in.readUnsignedShort() );
            assertEquals( (int) i, in.readInt() );
            assertEquals( (float) i, in.readFloat() );
            assertEquals( (double) i, in.readDouble() );
            assertEquals( txt_, in.readUTF() );
        }
        else if ( phase == 1 ) {
            for ( int skip = cblock_; skip > 0; skip -= in.skip( skip ) );
        }
        else if ( phase == 2 ) {
            byte[] bbuf = new byte[ 1 ];
            short[] sbuf = new short[ 1 ];
            int[] usbuf = new int[ 1 ];
            int[] ibuf = new int[ 2 ];
            float[] fbuf = new float[ 4 ];
            double[] dbuf = new double[ 1 ];
            byte[] cbuf = new byte[ nc_ ];
            in.read( bbuf );
            in.read( sbuf, 0, 1 );
            usbuf[ 0 ] = in.readUnsignedShort();
            in.read( ibuf, 1, 1 );
            in.read( fbuf, 2, 1 );
            in.read( dbuf );
            in.read( cbuf );
            assertEquals( (byte) i, bbuf[ 0 ] );
            assertEquals( (short) i, sbuf[ 0 ] );
            assertEquals( (int) i, usbuf[ 0 ] );
            assertEquals( (int) i, ibuf[ 1 ] );
            assertEquals( (float) i, fbuf[ 2 ] );
            assertEquals( (double) i, dbuf[ 0 ] );
        }
        else if ( phase == 3 ) {
            byte[] buf = new byte[ cblock_ ];
            in.read( buf );
            assertEquals( (byte) i, buf[ 0 ] );
            assertEquals( (short) i,
                          (short) ( ( ( buf[ 1 ] & 0xff ) << 8 )
                                  | ( ( buf[ 2 ] & 0xff ) << 0 ) ) );
        }
        else {
            fail();
        }
    }
}
