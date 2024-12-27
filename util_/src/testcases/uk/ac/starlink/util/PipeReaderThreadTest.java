package uk.ac.starlink.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PipeReaderThreadTest extends TestCase {

    byte[] outArray;
    int nOut = 1280;

    public PipeReaderThreadTest( String name ) {
        super( name );
        outArray = new byte[ nOut ];
        fillCycle( outArray, 0, 63 );
    }

    public void testPipe1() throws IOException {
        final byte[] inArray = new byte[ nOut ];
        PipeReaderThread reader = new PipeReaderThread() {
            protected void doReading( InputStream in ) throws IOException {
                int i = 0;
                for ( int b; ( b = in.read() ) >= 0; ) {
                    inArray[ i++ ] = (byte) b;
                }
            }
        };
        reader.start();
        OutputStream out = reader.getOutputStream();
        for ( int i = 0; i < outArray.length; i++ ) {
            out.write( outArray[ i ] & 0x7f );
        }
        out.close();
        reader.finishReading();
        assertArrayEquals( outArray, inArray );
    }

    public void testPipeBlock() throws IOException {
        final byte[] inArray = new byte[ nOut ];
        PipeReaderThread reader = new PipeReaderThread() {
            protected void doReading( InputStream in ) throws IOException {
                byte[] buf = new byte[ 99 ];
                int pos = 0;
                for ( int num; 
                      ( num = in.read( inArray, pos, 
                                       Math.min( 17, nOut - pos ) ) ) >= 0; ) {
                    pos += num;
                    if ( pos == nOut ) {
                        break;
                    }
                }
                assertEquals( pos, nOut );
                assertArrayEquals( outArray, inArray );
                assertEquals( -1, in.read() );
            }
        };
        reader.start();
        OutputStream out = reader.getOutputStream();
        int inc = 44;
        for ( int pos = 0; pos < nOut; pos += inc ) {
            out.write( outArray, pos, Math.min( inc, nOut - pos ) );
        }
        out.close();
        reader.finishReading();
        assertArrayEquals( outArray, inArray );
    }
}
