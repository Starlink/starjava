package uk.ac.starlink.treeview;

import java.io.File;
import javax.swing.JFrame;
import javax.swing.JTree;
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
        DataNodeTreeModel model = new DataNodeTreeModel( new DemoDataNode() );
        JFrame window = new JFrame();
        JTree jtree = new DataNodeJTree( model );
        jtree.expandRow( 0 );
        window.getContentPane().add( new DataNodeJTree( model ) );
        window.pack();
        window.setSize( 200, 200 );
        window.setVisible( true );
        Thread.currentThread().sleep( 1000 );
    }
}
