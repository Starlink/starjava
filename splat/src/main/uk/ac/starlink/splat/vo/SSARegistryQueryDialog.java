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

import org.us_vo.www.SimpleResource;

import uk.ac.starlink.table.BeanStarTable;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.BasicTableLoadDialog;
import uk.ac.starlink.table.gui.BasicTableConsumer;
import uk.ac.starlink.vo.RegistryInterrogator;
import uk.ac.starlink.vo.RegistryQuery;
import uk.ac.starlink.vo.RegistryQueryPanel;

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
    extends BasicTableLoadDialog
{
    private final RegistryQueryPanel rqPanel_;
    private static Boolean available_;
    private StarTable table_;

    /** The SSAP query. */
    public static String[] defaultQuery_ = new String[]
        {
            "serviceType like 'SSAP'"
        };

    /**
     * Constructor.
     */
    public SSARegistryQueryDialog()
    {
        super( "SSAP Registry Query",
               "Query a registry for all known SSAP services" );
        rqPanel_ = new RegistryQueryPanel();
        rqPanel_.setPresetQueries( defaultQuery_ );
        add( rqPanel_ );
        rqPanel_.getQuerySelector().addActionListener( getOkAction() );
    }

    public String getName()
    {
        return "SSAP Registry Query";
    }

    public String getDescription()
    {
        return "Query a registry for all known SSAP services";
    }

    public boolean isAvailable()
    {
        if ( available_ == null ) {
            try {
                available_ = Boolean.valueOf
                    ( RegistryInterrogator.isAvailable() );
            }
            catch ( Throwable th ) {
                available_ = Boolean.FALSE;
            }
        }
        return available_.booleanValue();
    }

    public void setEnabled( boolean enabled )
    {
        super.setEnabled( enabled );
        rqPanel_.setEnabled( enabled );
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
                        SimpleResource[] resources;
                        try {
                            resources = query.performQuery();
                        }
                        catch ( RemoteException e ) {
                            throw asIOException( e );
                        }
                        catch ( ServiceException e ) {
                            throw asIOException( e );
                        }
                        BeanStarTable st;
                        try {
                            st = new BeanStarTable( SimpleResource.class );
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
        protected void tableLoaded( StarTable table )
        {
            System.out.println( "tableLoaded: " + table );
            table_ = table;
        }
    }

    /**
     * Return the StarTable containing the SimpleResource objects supplied by
     * the registry.
     */
    public StarTable getStarTable()
    {
        return table_;
    }
}
