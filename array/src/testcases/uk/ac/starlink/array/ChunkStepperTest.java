package uk.ac.starlink.array;

import java.lang.reflect.Array;
import java.util.Iterator;
import junit.framework.TestCase;

public class ChunkStepperTest extends TestCase {

    public ChunkStepperTest( String name ) {
        super( name );
    }


    public void testValidateArguments() {
        for ( int i = 0; i < 3; i++ ) {
            try {
                new ChunkStepper( 100, -i );
                fail( "Should throw exception" );
            }
            catch ( IllegalArgumentException e ) {
            }
        }
    }

    public void testIteration() {
        int dsize = ChunkStepper.defaultChunkSize;
        iterationTest( dsize, dsize / 3 );
        iterationTest( dsize / 3, dsize );
        iterationTest( dsize, dsize );
        iterationTest( dsize - 1, dsize + 1 );
        iterationTest( dsize + 1, dsize - 1 );
        iterationTest( dsize * 10 + 1, dsize + 1 );
        iterationTest( dsize + 1, dsize + 10 + 1 );
    }

    private void iterationTest( long length, int chunkSize ) {
        ChunkStepper cIt = new ChunkStepper( length, chunkSize );
        long nChunks = ( ( length - 1L ) / chunkSize ) + 1L;
        int remainder = (int) ( ( length - 1L ) % (long) chunkSize );
       
        long i = 0;
        long nel = 0L;
        while ( cIt.hasNext() ) {
            assertEquals( Math.min( (long) chunkSize, 
                                    length - ( i * chunkSize ) ),
                          (long) cIt.getSize() );
            assertEquals( i * chunkSize, cIt.getBase() );
            assertEquals( cIt.getBase(), nel );
            nel += cIt.getSize();
            cIt.next();
            i++;
        }
        assertTrue( ! cIt.hasNext() );
        assertEquals( nChunks, i );
        assertEquals( length, nel );

        /* Test BufferIterator too. */
        for ( Iterator tit = Type.allTypes().iterator(); tit.hasNext(); ) {
            Type type = (Type) tit.next();
            BufferIterator bIt = new BufferIterator( length, type, chunkSize );
            try {
                bIt.getBase();
                fail();
            }
            catch ( IllegalStateException e ) {
                // good
            }

            long jnel = 0L;
            while ( bIt.hasNext() ) {
                Object buf = bIt.next();
                assertEquals( buf.getClass().getComponentType(), 
                              type.javaClass() );
                assertEquals( bIt.getBase(), jnel );
                jnel += Array.getLength( buf );
            }
            assertTrue( ! bIt.hasNext() );
            assertEquals( jnel, length );
        }
    }
}
