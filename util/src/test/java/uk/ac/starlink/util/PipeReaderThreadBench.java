package uk.ac.starlink.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class PipeReaderThreadBench {

    public void testReaders() throws IOException {
        System.out.println( "FastPiped*Stream: " + timeReader(
            new PipeReaderThread() {
                public void doReading( InputStream dataIn ) throws IOException {
                    disposeOf( dataIn );
                }
            }
        ) );
        System.out.println( "Piped*Stream: " + timeReader(
            new PipeReaderThread() {
                InputStream in = new PipedInputStream();
                OutputStream out = 
                    new PipedOutputStream( (PipedInputStream) in );
                public void doReading( InputStream dataIn ) throws IOException {
                    disposeOf( dataIn );
                }
                protected InputStream getInputStream() {
                    return in;
                }
                public OutputStream getOutputStream() {
                    return out;
                }
            }
        ) );
    }

    private void disposeOf( InputStream dataIn ) throws IOException {
        byte[] buf = new byte[ 1500 ];
        while ( dataIn.read( buf ) >= 0 ) {}
    }

    public long timeReader( PipeReaderThread reader ) throws IOException {
        int nblock = 1024 * 4;
        byte[] buf = new byte[ 1024 ];
        OutputStream out = reader.getOutputStream();

        long start = System.currentTimeMillis();
        reader.start();
        for ( int i = 0; i < nblock; i++ ) {
            out.write( buf );
        }
        out.close();
        reader.finishReading();
        long finish = System.currentTimeMillis();
        return System.currentTimeMillis() - start;
    }
}
