package uk.ac.starlink.treeview;

import java.awt.HeadlessException;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import junit.framework.TestCase;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.NoSuchDataException;
import uk.ac.starlink.datanode.tree.DataNodeJTree;
import uk.ac.starlink.datanode.tree.DataNodeTreeModel;

public class ModelTest extends TestCase {

    static {
        Logger.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
        Logger.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.WARNING );
    }

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
        try {
            final DataNode root = new DemoDataNode();
            DataNodeTreeModel model = new DataNodeTreeModel( root );
            assertEquals( model.getRoot(), root );
            final DataNodeJTree jtree = new DataNodeJTree( model );
            Thread expander = jtree.recursiveExpand( root );
            expander.join();
            assertEquals( model.getRoot(), root );
        }
        catch ( HeadlessException e ) {
            System.out.println( "Headless environment - no GUI test" );
        }
    }
}
