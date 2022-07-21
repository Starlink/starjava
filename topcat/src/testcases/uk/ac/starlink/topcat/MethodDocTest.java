package uk.ac.starlink.topcat;

import java.util.Enumeration;
import java.util.logging.Level;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import uk.ac.starlink.ttools.gui.DocNames;
import uk.ac.starlink.ttools.gui.MethodBrowser;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.TestCase;

public class MethodDocTest extends TestCase {

    MethodBrowser methodBrowser_;

    static {
        LogUtils.getLogger( "uk.ac.starlink.ast" ).setLevel( Level.OFF );
        LogUtils.getLogger( "uk.ac.starlink.util" ).setLevel( Level.OFF );
        LogUtils.getLogger( "org.astrogrid.samp" ).setLevel( Level.WARNING );
    }

    public MethodDocTest( String name ) {
        super( name );
        if ( isHeadless() ) {
            return;
        }
        MethodWindow methodWindow = new MethodWindow( null );
        methodBrowser_ = methodWindow.getBrowser();
        methodWindow.dispose();
    }

    public void testDocumentationForTree() {
        if ( isHeadless() ) {
            System.out.println( "Headless environment - no GUI test" );
            return;
        }
        TreeModel tmodel = methodBrowser_.getTreeModel();
        Object root = tmodel.getRoot();
        assertTrue( root instanceof DefaultMutableTreeNode );
        checkNodeChildren( (DefaultMutableTreeNode) tmodel.getRoot() );
    }

    private void checkNodeChildren( DefaultMutableTreeNode node ) {
        for ( Enumeration en = node.children(); en.hasMoreElements(); ) {
            Object chob = en.nextElement();
            assertTrue( chob instanceof DefaultMutableTreeNode );
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) chob;
            Object userObj = childNode.getUserObject();
            assertTrue( userObj.toString(), 
                        DocNames.docURL( userObj ) != null );
            assertTrue( methodBrowser_.textFor( userObj ) != null );
            checkNodeChildren( childNode );
        }
    }
}
