package uk.ac.starlink.table.storage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import uk.ac.starlink.util.TestCase;

public class ByteStoreAccessTest extends TestCase {

    private final int cblock = 8 + 4 + 4 + 2 + 1;

    public void testIo() throws IOException {
        int count = 999;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream( bout );
        for ( int i = 0; i < count; i++ ) {
            writeBlock( i, dout );
        }
        dout.flush();
        dout.close();
        byte[] data = bout.toByteArray();
        assertEquals( cblock * count, data.length );

        ByteStoreAccess[] accs = new ByteStoreAccess[] {
            new SingleNioAccess( ByteBuffer.wrap( data ) ),
            new MultiNioAccess( toEvenBufs( data, data.length ) ),
            new MultiNioAccess( toEvenBufs( data, cblock * 333 ) ),
            new MultiNioAccess( toEvenBufs( data, 1024 ) ),
            new MultiNioAccess( toEvenBufs( data, 371 ) ),
        };

        for ( int ia = 0; ia < accs.length; ia++ ) {
            ByteStoreAccess acc = accs[ ia ];
            byte[] d1 = new byte[ data.length ];
            acc.readBytes( d1, 0, data.length );
            assertArrayEquals( data, d1 );
            acc.seek( 0 );
            for ( int i = 0; i < count; i++ ) {
                readBlock( i, acc );
            }
            try {
                acc.readByte();
                fail();
            }
            catch ( IOException e ) {
            }
            for ( int j = count - 1; j >= 0; j-- ) {
                acc.seek( j * cblock );
                readBlock( j, acc );
            }
            acc.seek( data.length - 3 );
            try {
                acc.readFloat();
                fail();
            }
            catch ( IOException e ) {
            }
            try {
                acc.seek( data.length );
                fail();
            }
            catch ( IOException e ) {
            }
        }
    }

    private void writeBlock( int i, DataOutput out ) throws IOException {
        out.writeByte( (byte) i );
        out.writeShort( (short) i );
        out.writeInt( i );
        out.writeFloat( (float) i );
        out.writeDouble( (float) i );
    }

    private void readBlock( int i, ByteStoreAccess acc ) throws IOException {
        int phase = i % 3;
        if ( phase == 0 ) {
            assertEquals( (byte) i, acc.readByte() );
            assertEquals( (short) i, acc.readShort() );
            assertEquals( (int) i, acc.readInt() );
            assertEquals( (float) i, acc.readFloat() );
            assertEquals( (double) i, acc.readDouble() );
        }
        else if ( phase == 1 ) {
            acc.skip( cblock );
        }
        else if ( phase == 2 ) {
            byte[] buf = new byte[ cblock ];
            acc.readBytes( buf, 0, buf.length );
            assertEquals( (byte) i, buf[ 0 ] );
            assertEquals( (short) i,
                          (short) ( ( ( buf[ 1 ] & 0xff ) << 8 )
                                  | ( ( buf[ 2 ] & 0xff ) << 0 ) ) );
        }
        else {
            fail();
        }
    }

    private ByteBuffer[] toEvenBufs( byte[] in, int size ) {
        int nbuf = ( in.length - 1 ) / size + 1;
        ByteBuffer[] bufs = new ByteBuffer[ nbuf ];
        int c = 0;
        for ( int ib = 0; ib < nbuf; ib++ ) {
            int start = ib * size;
            int len = Math.min( size, in.length - start );
            bufs[ ib ] = ByteBuffer.wrap( in, start, len ).slice();
            c += bufs[ ib ].limit();
        }
        assertEquals( in.length, c );
        return bufs;
    }
}
