package uk.ac.starlink.topcat;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
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
        List<Field> fieldFailures = new ArrayList<>();
        for ( Enumeration en = node.children(); en.hasMoreElements(); ) {
            Object chob = en.nextElement();
            assertTrue( chob instanceof DefaultMutableTreeNode );
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) chob;
            Object userObj = childNode.getUserObject();
            URL docUrl = DocNames.docURL( userObj );
            if ( userObj instanceof Field ) {
                if ( docUrl == null ) {
                    fieldFailures.add( (Field) userObj );
                }
            }
            else {
                assertNotNull( userObj.toString(), docUrl );
            }
            assertTrue( methodBrowser_.textFor( userObj ) != null );
            checkNodeChildren( childNode );
        }

        /* If the filesystem on which the build is done is case-insensitive
         * (e.g. MacOS), then there will be collisions of the HTML files
         * containing javadocs that appear in the MethodBrowser window.
         * The effect is that some of the members are not properly documented.
         * At time of writing this applies to a couple of the public static
         * field members in Fluxes and KCorrections,
         * e.g.  Fluxes.JOHNSON_AB_i and Fluxes.JOHNSON_AB_I.
         * Spot this here, and as long as it's just fields and not too many
         * of them, let the build continue with a warning rather than
         * fail altogether.  Some proper fix should be found for this if
         * the build of the public release ever gets done on a
         * case-insensitive platform. */
        assertTrue( fieldFailures.size() < 5 );
        if ( fieldFailures.size() > 0 ) {
            String msg = new StringBuffer()
               .append( "Missing MethodBrowser Field doc URLs: " )
               .append( fieldFailures.stream()
                       .map( f -> f.getDeclaringClass().getSimpleName()
                                  + "." + f.getName() )
                       .collect( Collectors.toList() ) )
               .append( "\n" )
               .append( "Case-folding collisions may be caused by building " )
               .append( "on a case-insensitive filesystem" )
               .toString();
            System.out.println( msg );
        }
    }
}
