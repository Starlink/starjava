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
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.DefaultDataNode;
import uk.ac.starlink.datanode.nodes.ErrorDataNode;
import uk.ac.starlink.datanode.nodes.NoSuchDataException;
import uk.ac.starlink.datanode.nodes.ResourceListDataNode;
import uk.ac.starlink.datanode.tree.TreeTableLoadDialog;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.TableConsumer;
import uk.ac.starlink.table.gui.TableLoadDialog;

/**
 * Table load dialogue which presents some demonstration data.
 *
 * @author   Mark Taylor (Starlink)
 * @since    1 Dec 2004
 */
public class DemoLoadDialog extends TreeTableLoadDialog {

    private DataNode rootNode_;
    private boolean initialized_;
    private final static Logger logger_ = 
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructor. 
     */
    public DemoLoadDialog() {
        rootNode_ = getDemoNode();
    }

    public String getName() {
        return "Browse Demo Data";
    }

    public String getDescription() {
        return "View a data hierarchy containing some demonstration tables";
    }

    public boolean isEnabled() {
        return true;
    }

    public boolean showLoadDialog( Component parent, StarTableFactory factory,
                                   ComboBoxModel formatModel,
                                   TableConsumer eater ) {
        if ( ! initialized_ ) {
            clear();
            setRoot( rootNode_ );
        }
        return super.showLoadDialog( parent, factory, formatModel, eater );
    }

    /**
     * Constructs and returns a DataNode
     * corresponding to the root of the demo data tree.
     *
     * @return  demo DataNode
     */
    private static DataNode getDemoNode() {

        /* Get the list of resources which constitute the demo set. */
        List demoList = new ArrayList();
        InputStream
            strm = DemoLoadDialog.class.getClassLoader()
                  .getResourceAsStream( TopcatUtils.DEMO_LOCATION + "/" +
                                        TopcatUtils.DEMO_NODES );
        BufferedReader rdr =
            new BufferedReader( new InputStreamReader( strm ) );
        try {
            for ( String line; ( line = rdr.readLine() ) != null; ) {
                demoList.add( TopcatUtils.DEMO_LOCATION + "/" + line );
            }
            rdr.close();
        }
        catch ( IOException e ) {
            return new ErrorDataNode( e );
        }

        /* Try to make a new root node based on these. */
        return demoList.size() > 0 
             ? new ResourceListDataNode( demoList )
             : new DefaultDataNode( "No demo resources found" );
    }
}
