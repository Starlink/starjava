package uk.ac.starlink.astrogrid;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.astrogrid.store.tree.Container;
import org.astrogrid.store.tree.File;
import org.astrogrid.store.tree.Node;
import org.astrogrid.store.tree.TreeClient;
import org.astrogrid.store.tree.TreeClientException;


public class MyspaceTest extends TestCase {

    static PrintStream out = System.out;
    static boolean VERBOSE = true;

    static {
        System.setProperty( "org.astrogrid.registry.query.endpoint",
                            "http://twmbarlwm.astrogrid.org:8080" +
                            "/astrogrid-registry-SNAPSHOT/" +
                            "services/RegistryQuery" );
        Logger.getLogger( "org" ).setLevel( Level.SEVERE );
    }

    public MyspaceTest( String name ) {
        super( name );
    }

    public TreeClient getClient() throws Exception {
        return new ToadConnector().getConnection();
    }

    public void testData() throws Exception {
        TreeClient client = null;
        try {
            client = getClient();
            Container root = client.getRoot();
            assertTrue( root != null );
            assertTrue( root instanceof Container );
            assertEquals( "toad", root.getName() );
            if ( VERBOSE ) {
                outputContainer( root, 0 );
            }
        }
        finally {
            if ( client != null ) {
                client.logout();
            }
        }
    }

    public void outputContainer( Node node, int level ) throws Exception {
        for ( int i = 0; i < level; i++ ) {
            out.print( "  " );
        }
        out.print( node.getName() );
        if ( node.isContainer() ) {
            out.print( '/' );
        }
        else {
            assertTrue( node instanceof File );
            System.out.print( "    " + ((File) node).getMimeType() );
        }
        out.println();
        if ( node.isContainer() ) {
            for ( Iterator it = ((Container) node).getChildNodes()
                                                  .iterator();
                  it.hasNext(); ) {
                Node child = (Node) it.next();
                outputContainer( child, level + 1 );
            }
        }
    }

}
