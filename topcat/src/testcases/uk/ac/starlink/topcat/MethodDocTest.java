package uk.ac.starlink.topcat;

import java.util.Enumeration;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import junit.framework.TestCase;
import uk.ac.starlink.topcat.doc.DocNames;

public class MethodDocTest extends TestCase {

    MethodWindow methodWindow;

    public MethodDocTest( String name ) {
        super( name );
        methodWindow = new MethodWindow( null );
        methodWindow.setVisible( false );
    }

    public void testDocumentationForTree() {
        TreeModel tmodel = methodWindow.getTreeModel();
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
            assertTrue( methodWindow.textFor( userObj ) != null );
            checkNodeChildren( childNode );
        }
    }
}
