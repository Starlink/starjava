package uk.ac.starlink.astrogrid;

import java.util.logging.Logger;
import org.astrogrid.store.tree.TreeClient;
import org.apache.axis.client.Service;
import uk.ac.starlink.vo.RemoteTreeTableLoadDialog;

/**
 * Table load dialogue which allows browsing through MySpace for files.
 *
 * @author   Mark Taylor (Starlink)
 * @since    26 Nov 2005
 */
public class MyspaceTableLoadDialog extends RemoteTreeTableLoadDialog {

    private static Boolean isAvailable_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.astrogrid" );

    /**
     * Constructor.
     */
    public MyspaceTableLoadDialog() {
        super( "MySpace Browser", "Load table from files in MySpace",
               new MyspaceTreeBrowser() );
    }

    protected TableSupplier makeTableSupplier( Object node ) {
        MyspaceTreeNode tnode = (MyspaceTreeNode) node;
        org.astrogrid.store.tree.File msFile =
            (org.astrogrid.store.tree.File) tnode.getMyspaceNode();
        return new DataSourceTableSupplier( new MyspaceDataSource( msFile ) );
    }

    public boolean isAvailable() {
        if ( isAvailable_ == null ) {
            String msg = getFailureMessage();
            if ( msg != null ) {
                logger_.info( msg + " - no MySpace" );
            }
            isAvailable_ = Boolean.valueOf( msg == null );
        }
        return isAvailable_.booleanValue();
    }

    /**
     * Returns a string indicating why MySpace access isn't going to work.
     * If the return is null, we're ready to go!
     *
     * @return  failure message or null
     */
    private static String getFailureMessage() {
        String endpointProperty = "org.astrogrid.registry.query.endpoint";
        try {
            String endPoint = System.getProperty( endpointProperty );
            if ( endPoint == null ) {
                return "Property " + endpointProperty + " undefined";
            }
            try {
                Service.class.getName();
            }
            catch ( Throwable th ) {
                return "AXIS not on path";
            }
            try {
                TreeClient.class.getName();
            }
            catch ( Throwable th ) {
                return "Astrogrid CDK not on path";
            }
        }
        catch ( Throwable th2 ) {
            return th2.toString();
        }
        return null;
    }

}
