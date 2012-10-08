/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     04-APR-2005 (Peter W. Draper):
 *       Original version, based on Mark's Dialog in vo package.
 *     15-SEP-2010 (Mark Taylor):
 *       Adjusted to use revised load dialog framework.
 */
package uk.ac.starlink.splat.vo;

import java.awt.Component;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.net.MalformedURLException;

import uk.ac.starlink.table.BeanStarTable;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.TableLoader;
import uk.ac.starlink.vo.RegResource;
//import uk.ac.starlink.vo.RegistryQuery;
import uk.ac.starlink.vo.RegistryQueryPanel;
import uk.ac.starlink.vo.RegistryTableLoadDialog;

/**
 * Dialog for performing a simple query on a registry for its SSAP servers.
 * The user can choose which registry to use. A StarTable is returned which
 * contains all the details of the resources found.
 *
 * @author Peter W. Draper
 * @author Mark Taylor (Starlink)
 * @author Margarida Castro Neves (adapted source to SSAP queries with extended parameters)
 * @version $Id: SSARegistryQueryDialog.java 9052 2009-12-09 18:48:37Z mbt $
 */
public class SSARegistryQueryDialog
    extends RegistryTableLoadDialog
{
    private SSAPRegistryQueryPanel rqPanel_;
    private static Boolean available_;
    private StarTable table_;

    /** The SSAP query. */
    public static String[] defaultQuery_ = new String[]
        {
            "capability/@standardID = 'ivo://ivoa.net/std/SSA'"
        };

    
    public String getName()
    {
        return "SSAP Registry Query";
    }

    public String getDescription()
    {
        return "Query a registry for all known SSAP services";
    }

    protected Component createQueryComponent()
    {
        rqPanel_ = new SSAPRegistryQueryPanel();
        rqPanel_.setPresetQueries( defaultQuery_ );
        return rqPanel_;
    }

    public TableLoader createTableLoader()
    {
        try {
            final SSAPRegistryQuery query = rqPanel_.getRegistryQuery();
          
            return new TableLoader()
                {
                    public TableSequence loadTables( StarTableFactory factory )
                            throws IOException
                    {
                        SSAPRegResource[] resources = query.getQueryResources();
                        BeanStarTable st;
                        try {
                            st = new BeanStarTable( SSAPRegResource.class );
                        }
                        catch ( IntrospectionException e ) {
                            throw (IOException)
                                  new IOException( e.getMessage() )
                                 .initCause( e );
                        }
                        DescribedValue[] metadata = query.getMetadata();
                        for ( int i = 0; i < metadata.length; i++ ) {
                            st.setParameter( metadata[ i ] );
                        }
                        st.setData( resources );
                        return Tables.singleTableSequence( st );
                    }
                    public String getLabel()
                    {
                        return query.toString();
                    }
                };
        }
        catch ( MalformedURLException e ) {
            throw new IllegalStateException( e.getMessage() );
        }
    }
}
