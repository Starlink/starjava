package uk.ac.starlink.vo;

import java.awt.Component;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.rpc.ServiceException;
import uk.ac.starlink.table.BeanStarTable;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.AbstractTableLoadDialog;
import uk.ac.starlink.table.gui.TableLoader;

/**
 * Table load dialogue implementation for performing a simple query on 
 * a registry.  The user can choose which registry to use, and the text
 * of the query (WHERE clause) to make, and a StarTable is returned which
 * contains all the detail of the resources found.
 *
 * <p>This is not very user-friendly or useful, and hence is somewhat
 * deprecated.  A TAP query on a Relational Registry service (RegTAP) 
 * is usually a better way to acquire registry information.
 *
 * @author   Mark Taylor (Starlink)
 * @since    23 Dec 2004
 * @see   <a href="http://www.ivoa.net/documents/RegistryInterface/20091104/"
 *           >Registry Interface 1.0</a>
 */
public class Ri1RegistryTableLoadDialog extends AbstractTableLoadDialog {

    private Ri1RegistryQueryPanel rqPanel_;

    /** List of preset queries available by default. */
    public static String[] defaultQueries_ = new String[] {
        Ri1RegistryQuery.getAdqlWhere( Capability.CONE ),
        Ri1RegistryQuery.getAdqlWhere( Capability.SIA ),
        Ri1RegistryQuery.getAdqlWhere( Capability.SSA ),
        Ri1RegistryQuery.getAdqlWhere( Capability.TAP ),
    };

    /**
     * Constructor. 
     */
    @SuppressWarnings("this-escape")
    public Ri1RegistryTableLoadDialog() {
        super( "RI1.0 Registry Query", 
               "Imports a table describing the result of querying a registry"
             + " using the RI1.0 interface"
             + "; using RegTAP is usually a better idea." );
        setIcon( ResourceIcon.TLD_REGISTRY );
    }

    protected Component createQueryComponent() {
        rqPanel_ = new Ri1RegistryQueryPanel();
        rqPanel_.setPresetQueries( defaultQueries_ );
        return rqPanel_;
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

    public TableLoader createTableLoader() {
        try {
            final RegistryQuery query = rqPanel_.getRegistryQuery();
            return new TableLoader() {
                public TableSequence loadTables( StarTableFactory factory )
                        throws IOException {
                    return Tables
                       .singleTableSequence( new RegistryStarTable( query ) );
                }
                public String getLabel() {
                    return query.toString();
                }
            };
        }
        catch ( MalformedURLException e ) {
            throw new IllegalStateException( e.getMessage() );
        }
    }
}
