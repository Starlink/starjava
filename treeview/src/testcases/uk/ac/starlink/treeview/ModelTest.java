package uk.ac.starlink.treeview;

import java.io.File;
import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import junit.framework.TestCase;

public class ModelTest extends TestCase {

    public void setUp() {
        String basedir = System.getProperty( "ant.basedir" );
        System.setProperty( "uk.ac.starlink.treeview.demodir",
                            basedir + File.separator +
                            "etc" + File.separator +
                            "treeview" + File.separator +
                            "demo" );
    }

    public ModelTest( String name ) {
        super( name );
    }

    public void testModel() throws NoSuchDataException, InterruptedException {
        final DataNode root = new DemoDataNode();
        DataNodeTreeModel model = new DataNodeTreeModel( root );
        assertEquals( model.getRoot(), root );
        JFrame window = new JFrame();
        final DataNodeJTree jtree = new DataNodeJTree( model );
        window.getContentPane().add( jtree );
        window.pack();
        window.setSize( 400, 1000 );
        window.setVisible( true );

        // doesn't work (doesn't appear to display) - why not??
        Thread expander = jtree.recursiveExpand( root );
        expander.join();
        // assertEquals( 253, model.getNodeCount() - 1 );
        assertEquals( model.getRoot(), root );
        Thread.currentThread().sleep( 4000 );
    }
}
