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
 * @author   Mark Taylor (Starlink)
 * @since    23 Dec 2004
 */
public class RegistryTableLoadDialog extends AbstractTableLoadDialog {

    private RegistryQueryPanel rqPanel_;

    /** List of preset queries available by default. */
    public static String[] defaultQueries_ = new String[] {
        Capability.CONE.getAdql(),
        Capability.SIA.getAdql(),
        Capability.SSA.getAdql(),
    };

    /**
     * Constructor. 
     */
    public RegistryTableLoadDialog() {
        super( "Registry Query", 
               "Imports a table describing the result of querying a registry" );
        setIconUrl( RegistryTableLoadDialog.class
                                           .getResource( "registry.gif" ) );
    }

    protected Component createQueryComponent() {
        rqPanel_ = new RegistryQueryPanel();
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
