package uk.ac.starlink.topcat;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.NodeLoader;
import uk.ac.starlink.table.gui.StarTableNodeChooser;
import uk.ac.starlink.table.gui.TableConsumer;
import uk.ac.starlink.table.gui.TableLoadDialog;

/**
 * Table load dialogue which presents some demonstration data.
 *
 * @author   Mark Taylor (Starlink)
 * @since    1 Dec 2004
 */
public class DemoLoadDialog extends NodeLoader {

    private final StarTableNodeChooser nodeChooser_;
    private Object rootNode_;
    private boolean initialized_;
    private final static Logger logger_ = 
        Logger.getLogger( "uk.ac.starlink.topcat" );

    public static String DEMO_LOCATION = "uk/ac/starlink/topcat/demo";
    public static String DEMO_TABLE = "863sub.fits";
    public static String DEMO_NODES = "demo_list";

    /**
     * Constructor. 
     */
    public DemoLoadDialog() {
        StarTableNodeChooser chooser = getNodeChooser();
        if ( chooser != null ) {
            rootNode_ = getDemoNode();
            if ( rootNode_ != null ) {
                nodeChooser_ = chooser;
            }
            else {
                nodeChooser_ = null;
            }
        }
        else {
            nodeChooser_ = null;
        }
    }

    public String getName() {
        return "Browse Demo Data";
    }

    public String getDescription() {
        return "View a data hierarchy containing some demonstration tables";
    }

    public boolean isEnabled() {
        return nodeChooser_ != null;
    }

    public boolean showLoadDialog( Component parent, StarTableFactory factory,
                                   ComboBoxModel formatModel,
                                   TableConsumer eater ) {
        if ( ! initialized_ ) {
            nodeChooser_.setRootNode( rootNode_ );
        }
        return super.showLoadDialog( parent, factory, formatModel, eater );
    }

    /**
     * Constructs and returns a <tt>uk.ac.starlink.table.DataNode</tt>
     * corresponding to the root of the demo data tree.
     *
     * @return  demo DataNode
     */
    private static Object getDemoNode() {

        /* Get the list of resources which constitute the demo set. */
        List demoList = new ArrayList();
        InputStream
            strm = DemoLoadDialog.class.getClassLoader()
                  .getResourceAsStream( DEMO_LOCATION + "/" + DEMO_NODES );
        BufferedReader rdr =
            new BufferedReader( new InputStreamReader( strm ) );
        try {
            for ( String line; ( line = rdr.readLine() ) != null; ) {
                demoList.add( DEMO_LOCATION + "/" + line );
            }
            rdr.close();
        }
        catch ( IOException e ) {
            logger_.warning( "Couldn't find demo data" );
            return null;
        }

        /* Try to make a new root node based on these. */
        if ( demoList != null ) {
            try {
                return DemoLoadDialog.class.forName(
                           "uk.ac.starlink.treeview.ResourceListDataNode" )
                      .getConstructor( new Class[] { List.class } )
                      .newInstance( new Object[] { demoList } );
            }
            catch ( Exception e ) {
                logger_.warning( "Couldn't find demo data" );
                return null;
            }
        }
        else {
            return null;
        }
    }
}
