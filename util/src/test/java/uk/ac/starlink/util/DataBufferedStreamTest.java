package uk.ac.starlink.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataBufferedStreamTest {

    private static final int N0 = 100;
    private static final int NC = 500;

    @Test
    public void testIO() throws IOException {
        exerciseIO( 986234L, 99, 105 );
        exerciseIO( -232353L, 1024, 1024 );
        exerciseIO( -891255122L, 97, 89 );
    }

    private void exerciseIO( long seed, int isiz, int osiz )
            throws IOException {
        Random ornd = new Random( seed );
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataBufferedOutputStream dout =
            new DataBufferedOutputStream( bout, osiz );
        Exerciser oex = new Exerciser( ornd );
        oex.repeat( () -> dout.writeBoolean( ornd.nextInt() > 0 ) );
        oex.repeat( () -> dout.writeByte( (byte) ornd.nextInt() ) );
        oex.repeat( () -> dout.writeShort( (short) ornd.nextInt() ) );
        oex.repeat( () -> dout.writeChar( (char) ornd.nextInt() ) );
        oex.repeat( () -> dout.writeInt( ornd.nextInt() ) );
        oex.repeat( () -> dout.writeLong( ornd.nextLong() ) );
        oex.repeat( () -> dout.writeFloat( ornd.nextFloat() ) );
        oex.repeat( () -> dout.writeDouble( ornd.nextDouble() ) );
        byte[] obuf = new byte[ 3 * osiz ];
        ornd.nextBytes( obuf );
        dout.write( obuf );
        for ( int nb = osiz - 2; nb < osiz + 2; nb++ ) {
            dout.write( obuf, 0, nb );
            dout.write( obuf, 0, nb * 2 );
            dout.write( obuf, 0, nb * 2 );
        }
        dout.write( 23 );
        dout.writeInt( 99 );
        dout.writeDouble( Double.NaN );
        dout.writeFloat( Float.NaN );
        dout.close();

        DataBufferedInputStream din =
            new DataBufferedInputStream(
                new ByteArrayInputStream( bout.toByteArray() ), osiz );
        Random irnd = new Random( seed );
        Exerciser iex = new Exerciser( irnd );
        iex.repeat( () ->
                    assertEquals( irnd.nextInt() > 0, din.readBoolean() ) );
        iex.repeat( () ->
                    assertEquals( (byte) irnd.nextInt(), din.readByte() ) );
        iex.repeat( () ->
                    assertEquals( (short) irnd.nextInt(), din.readShort() ) );
        iex.repeat( () ->
                    assertEquals( (char) irnd.nextInt(), din.readChar() ) );
        iex.repeat( () -> assertEquals( irnd.nextInt(), din.readInt() ) );
        iex.repeat( () -> assertEquals( irnd.nextLong(), din.readLong() ) );
        iex.repeat( () -> assertEquals( irnd.nextFloat(), din.readFloat() ) );
        iex.repeat( () -> assertEquals( irnd.nextDouble(), din.readDouble() ) );
        byte[] rbuf = new byte[ 3 * osiz ];
        irnd.nextBytes( rbuf );
        assertBufEquals( obuf, rbuf, 3 * osiz );
        byte[] ibuf3 = new byte[ 3 * osiz ];
        din.readFully( ibuf3, 0, 3 * osiz );
        assertBufEquals( obuf, ibuf3, 3 * osiz );
        for ( int nb = osiz - 2; nb < osiz + 2; nb++ ) {
            byte[] ibuf1 = new byte[ nb ];
            din.readFully( ibuf1 );
            assertBufEquals( obuf, ibuf1, nb );
            byte[] ibuf2 = IOUtils.readBytes( din, nb * 2 );
            assertBufEquals( obuf, ibuf2, nb * 2 );
            IOUtils.skip( (InputStream) din, nb );
            IOUtils.skipBytes( (DataInput) din, nb );
        }
        assertEquals( 23, din.read() );
        assertEquals( 99, din.readInt() );
        assertTrue( Double.isNaN( din.readDouble() ) );
        assertTrue( Float.isNaN( din.readFloat() ) );
        assertEquals( -1, din.read() ); // EOF
    }

    private void assertBufEquals( byte[] buf1, byte[] buf2, int n ) {
        for ( int i = 0; i < n; i++ ) {
            assertEquals( buf1[ i ], buf2[ i ] );
        }
    }

    private static class Exerciser {
        final Random rnd_;
        Exerciser( Random rnd ) {
            rnd_ = rnd;
        }
        public void repeat( IORunnable runnable ) throws IOException {
            for ( int i = nextCount( rnd_ ); i > 0; i-- ) {
                runnable.run();
            }
        }
    }

    private static int nextCount( Random rnd ) {
        return N0 + rnd.nextInt( NC );
    }

    @FunctionalInterface
    private static interface IORunnable {
        void run() throws IOException;
    }
}
