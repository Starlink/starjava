package uk.ac.starlink.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class ReaderThreadTest extends TestCase {

    public ReaderThreadTest( String name ) {
        super( name );
    }

    public void testThread() throws IOException {
        final int leng = 100;
        byte[] testbuf = new byte[ leng ];
        fillCycle( testbuf, -128, 127 );
        PipedOutputStream out = new PipedOutputStream();
        CopyReaderThread crt = new CopyReaderThread( out );
        crt.start();
        for ( int i = 0; i < leng; i++ ) {
            out.write( testbuf[ i ] );
        }
        out.close();
        crt.finishReading();
        assertArrayEquals( testbuf, crt.data );

        out = new PipedOutputStream();
        ReaderThread errReader = new ReaderThread( out ) {
            protected void doReading( InputStream in ) throws IOException {
                for ( int i = 0; i < leng / 2; i++ ) {
                    in.read();
                }
                throw new IOException();
            }
        };
        errReader.start();
        for ( int i = 0; i < leng; i++ ) {
            out.write( testbuf[ i ] );
        }
        try {
            errReader.finishReading();
            fail();
        }
        catch ( IOException e ) {
            // ok
        }
    }

    class CopyReaderThread extends ReaderThread {
        byte[] data;
        CopyReaderThread( PipedOutputStream out ) throws IOException {
            super( out );
        }
        protected void doReading( InputStream in ) throws IOException {
            ByteArrayOutputStream bufstrm = new ByteArrayOutputStream();
            for ( int b; ( b = in.read() ) >= 0; ) {
                bufstrm.write( b );
            }
            in.close();
            bufstrm.close();
            data = bufstrm.toByteArray();
        }
    }
}
