/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     04-APR-2005 (Peter W. Draper):
 *       Original version, based on Mark's Dialog in vo package.
 */
package uk.ac.starlink.splat.vo;

import java.awt.Component;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.xml.rpc.ServiceException;

import uk.ac.starlink.table.BeanStarTable;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.BasicTableConsumer;
import uk.ac.starlink.vo.RegResource;
import uk.ac.starlink.vo.RegistryQuery;
import uk.ac.starlink.vo.RegistryQueryPanel;
import uk.ac.starlink.vo.RegistryTableLoadDialog;

import net.ivoa.registry.RegistryAccessException;

/**
 * Dialog for performing a simple query on a registry for its SSAP servers.
 * The user can choose which registry to use. A StarTable is returned which
 * contains all the details of the resources found.
 *
 * @author Peter W. Draper
 * @author Mark Taylor (Starlink)
 * @version $Id$
 */
public class SSARegistryQueryDialog
    extends RegistryTableLoadDialog
{
    private RegistryQueryPanel rqPanel_;
    private static Boolean available_;
    private StarTable table_;

    /** The SSAP query. */
    public static String[] defaultQuery_ = new String[]
        {
            "capability/@standardID = 'ivo://ivoa.net/std/SSA'"
        };

    protected Component createQueryPanel()
    {
        rqPanel_ = new RegistryQueryPanel();
        rqPanel_.setPresetQueries( defaultQuery_ );
        return rqPanel_;
    }

    public String getName()
    {
        return "SSAP Registry Query";
    }

    public String getDescription()
    {
        return "Query a registry for all known SSAP services";
    }

    public boolean showLoadDialog( Component parent, StarTableFactory factory )
    {
        return super.showLoadDialog( parent, factory,
                                     new DefaultComboBoxModel( defaultQuery_ ),
                                     new QueryConsumer( parent ) );
    }

    protected TableSupplier getTableSupplier()
    {
        try {
            final RegistryQuery query = rqPanel_.getRegistryQuery();
            return new TableSupplier()
                {
                    public StarTable getTable( StarTableFactory factory,
                                               String format )
                        throws IOException
                    {
                        RegResource[] resources;
                        try {
                            resources = query.getQueryResources();
                        }
                        catch ( RegistryAccessException e ) {
                            throw asIOException( e );
                        }
                        BeanStarTable st;
                        try {
                            st = new BeanStarTable( RegResource.class );
                        }
                        catch ( IntrospectionException e ) {
                            throw asIOException( e );
                        }
                        DescribedValue[] metadata = query.getMetadata();
                        for ( int i = 0; i < metadata.length; i++ ) {
                            st.setParameter( metadata[ i ] );
                        }
                        st.setData( resources );
                        return st;
                    }
                    public String getTableID()
                    {
                        return query.toString();
                    }
                };
        }
        catch ( MalformedURLException e ) {
            throw new IllegalStateException( e.getMessage() );
        }
    }

    public class QueryConsumer
        extends BasicTableConsumer
    {
        public QueryConsumer( Component parent )
        {
            super( parent );
        }
        protected boolean tableLoaded( StarTable table )
        {
            table_ = table;
            return true;
        }
    }

    /**
     * Return the StarTable containing the RegResource objects supplied by
     * the registry.
     */
    public StarTable getStarTable()
    {
        return table_;
    }
}
