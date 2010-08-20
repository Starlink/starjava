package uk.ac.starlink.fits;

import java.io.BufferedOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.BufferedDataInputStream;
import nom.tam.util.BufferedFile;
import nom.tam.util.RandomAccess;
import uk.ac.starlink.util.TestCase;

public class ArrayDataTest extends TestCase {

    private final int cblock = 8 + 4 + 4 + 2 + 1;

    public ArrayDataTest( String name ) {
        super( name );
        Logger.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.WARNING );
    }

    public void testIo() throws IOException {
        int count = 999;
        File file = File.createTempFile( "iotest", ".fits" );
        file.deleteOnExit();
        DataOutputStream out =
            new DataOutputStream(
                new BufferedOutputStream( new FileOutputStream( file ) ) );
        for ( int i = 0; i < count; i++ ) {
            writeBlock( i, out );
        }
        out.close();
        assertEquals( cblock * count, file.length() );

        ArrayDataInput[] ins = new ArrayDataInput[] {
            new BufferedDataInputStream( new FileInputStream( file ) ),
            new BufferedDataInputStream( new FileInputStream( file ), 31 ), 
            new BufferedFile( file.toString() ),
            new BufferedFile( file.toString(), "r", 29 ),
            new MappedFile( file.toString() ),
            new MultiMappedFile( file, FileChannel.MapMode.READ_ONLY, 1023 ),
        };
        for ( int ii = 0; ii < ins.length; ii++ ) {
            ArrayDataInput in = ins[ ii ];
            for ( int i = 0; i < count; i++ ) {
                readBlock( i, in );
            }
            if ( in instanceof RandomAccess ) {
                RandomAccess rin = (RandomAccess) in;
                for ( int j = count - 1; j >= 0; j-- ) {
                    rin.seek( j * cblock );
                    assertEquals( j * cblock, rin.getFilePointer() );
                    readBlock( j, rin );
                    assertEquals( ( j + 1 ) * cblock, rin.getFilePointer() );
                }
            }
            if ( in instanceof CopyableRandomAccess ) {
                CopyableRandomAccess crin = (CopyableRandomAccess) in;
                CopyableRandomAccess crin1 = crin.copyAccess();
                assertEquals( crin.getFilePointer(), crin1.getFilePointer() );
                assertEquals( crin.readLong(), crin1.readLong() );
                assertEquals( crin.readLong(), crin1.readLong() );
                crin1.readLong();
                assertEquals( crin.getFilePointer() + 8,
                              crin1.getFilePointer() );
                assertTrue( crin.readLong() != crin1.readLong() );
                crin.seek( 155 );
                long m0 = crin.readLong();
                crin1.seek( 155 );
                assertEquals( m0, crin1.readLong() );
            }
            in.close();
        }
        file.delete();
    }

    private void writeBlock( int i, DataOutput out ) throws IOException {
        out.writeByte( (byte) i );
        out.writeShort( (short) i );
        out.writeInt( i );
        out.writeFloat( (float) i );
        out.writeDouble( (float) i );
    }

    private void readBlock( int i, ArrayDataInput in ) throws IOException {
        int phase = i % 4;
        if ( phase == 0 ) {
            assertEquals( (byte) i, in.readByte() );
            assertEquals( (short) i, in.readShort() );
            assertEquals( (int) i, in.readInt() );
            assertEquals( (float) i, in.readFloat() );
            assertEquals( (double) i, in.readDouble() );
        }
        else if ( phase == 1 ) {
            for ( int skip = cblock; skip > 0; skip -= in.skip( skip ) );
        }
        else if ( phase == 2 ) {
            byte[] bbuf = new byte[ 1 ];
            short[] sbuf = new short[ 1 ];
            int[] ibuf = new int[ 2 ];
            float[] fbuf = new float[ 4 ];
            double[] dbuf = new double[ 1 ];
            in.read( bbuf );
            in.read( sbuf, 0, 1 );
            in.read( ibuf, 1, 1 );
            in.read( fbuf, 2, 1 );
            in.read( dbuf );
            assertEquals( (byte) i, bbuf[ 0 ] );
            assertEquals( (short) i, sbuf[ 0 ] );
            assertEquals( (int) i, ibuf[ 1 ] );
            assertEquals( (float) i, fbuf[ 2 ] );
            assertEquals( (double) i, dbuf[ 0 ] );
        }
        else if ( phase == 3 ) {
            byte[] buf = new byte[ cblock ];
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
