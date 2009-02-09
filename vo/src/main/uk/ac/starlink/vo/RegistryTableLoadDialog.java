package uk.ac.starlink.vo;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.rpc.ServiceException;
import net.ivoa.registry.RegistryAccessException;
import uk.ac.starlink.table.BeanStarTable;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.BasicTableLoadDialog;

/**
 * Table load dialogue implementation for performing a simple query on 
 * a registry.  The user can choose which registry to use, and the text
 * of the query (WHERE clause) to make, and a StarTable is returned which
 * contains all the detail of the resources found.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    23 Dec 2004
 */
public class RegistryTableLoadDialog extends BasicTableLoadDialog {

    private final RegistryQueryPanel rqPanel_;
    private static Boolean available_;

    /** List of preset queries available by default. */
    public static String[] defaultQueries_ = new String[] {
        "capability/@standardID = '" + RegCapabilityInterface.CONE_STDID + "'",
        "capability/@standardID = '" + RegCapabilityInterface.SIA_STDID + "'",
        "capability/@standardID = '" + RegCapabilityInterface.SSA_STDID + "'",
        "capability/@standardID = '" + RegCapabilityInterface.REG_STDID + "'",
    };

    /**
     * Constructor. 
     */
    public RegistryTableLoadDialog() {
        super( "Registry Query", 
               "Imports a table describing the result of querying a registry" );
        rqPanel_ = new RegistryQueryPanel();
        rqPanel_.setPresetQueries( defaultQueries_ );
        add( rqPanel_ );
    }

    public String getName() {
        return "Registry Query";
    }

    public String getDescription() {
        return "Returns a table describing the resources in a registry " +
               "for a given query";
    }

    public boolean isAvailable() {
        return true;
    }

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        rqPanel_.setEnabled( enabled );
    }

    protected TableSupplier getTableSupplier() {
        try {
            final RegistryQuery query = rqPanel_.getRegistryQuery();
            return new TableSupplier() {
                public StarTable getTable( StarTableFactory factory,
                                           String format )
                        throws IOException {
                    try {
                        return new RegistryStarTable( query );
                    }
                    catch ( RegistryAccessException e ) {
                        throw asIOException( e );
                    }
                }
                public String getTableID() {
                    return query.toString();
                }
            };
        }
        catch ( MalformedURLException e ) {
            throw new IllegalStateException( e.getMessage() );
        }
    }

}
