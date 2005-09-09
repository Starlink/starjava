package uk.ac.starlink.connect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import uk.ac.starlink.util.TestCase;

public class FileNodeTest extends TestCase {

    public FileNodeTest( String name ) {
        super( name );
    }

    public void testList() throws IOException {
        File dir = new File( "." ).getCanonicalFile();
        FileBranch branch = new FileBranch( dir );
        assertEquals( branch.getName(), dir.getName() );
        assertEquals( -1, branch.getName().indexOf( '/' ) );
        assertEquals( branch.toString(), dir.getCanonicalPath() );
        Set fchildren = new HashSet( Arrays.asList( dir.listFiles() ) );
        for ( Iterator it = fchildren.iterator(); it.hasNext(); ) {
            if ( ((File) it.next()).isHidden() ) {
                it.remove();
            }
        }
        Set nchildren = new HashSet( Arrays.asList( branch.getChildren() ) );
        assertEquals( fchildren.size(), nchildren.size() );
        assertTrue( "Please run in a non-empty directory",
                    fchildren.size() > 0 );
        for ( Iterator it = fchildren.iterator(); it.hasNext(); ) {
            File f = (File) it.next();
            Node n = FileNode.createNode( f );
            assertTrue( nchildren.remove( n ) );
        }
        assertTrue( nchildren.isEmpty() );
    }

    public void testIO() throws IOException {
        File dir = new File( "." );
        String dummyName = "FileTestNode.dummy-file";
        File dummyFile = new File( dummyName );
        dummyFile.deleteOnExit();
        assertTrue( ! dummyFile.exists() );
        assertTrue( "Please run in writable directory", dir.canWrite() );
        Branch branch = (Branch) FileNode.createNode( dir );
        FileLeaf newLeaf = (FileLeaf) branch.createNode( dummyName );
        assertTrue( ! dummyFile.exists() );
        try {
            newLeaf.getDataSource().getIntro();
            fail();
        }
        catch ( FileNotFoundException e ) {
        }
        assertTrue( ! dummyFile.exists() );
        OutputStream ostrm = newLeaf.getOutputStream();
        ostrm.write( 0 );
        ostrm.close();
        assertTrue( dummyFile.exists() );
        assertArrayEquals( new byte[ 1 ], newLeaf.getDataSource().getIntro() );
        dummyFile.delete();
    }
}
