package uk.ac.starlink.array;

import junit.framework.TestCase;

public class AccessModeTest extends TestCase {

    public AccessModeTest( String name ) {
        super( name );
    }

    public void testRead() {
        AccessMode read = AccessMode.READ;
        assertTrue( read.isReadable() );
        assertTrue( ! read.isWritable() );
    }

    public void testWrite() {
        AccessMode write = AccessMode.WRITE;
        assertTrue( ! write.isReadable() );
        assertTrue( write.isWritable() );
    }

    public void testUpdate() {
        AccessMode update = AccessMode.UPDATE;
        assertTrue( update.isReadable() );
        assertTrue( update.isWritable() );
    }
}
