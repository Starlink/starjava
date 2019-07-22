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
import uk.ac.starlink.vo.RegTapRegistryQuery;
import uk.ac.starlink.vo.RegistryQuery;
import uk.ac.starlink.vo.Ri1RegistryTableLoadDialog;

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
    extends Ri1RegistryTableLoadDialog
{
    private SSAPRegistryQueryPanel rqPanel_;
    private static Boolean available_;
    private StarTable table_;
    private int protocol_;

    SSARegistryQueryDialog( int proto) {
       super();
       protocol_=proto; 
    }
    
    /** The SSAP query. */
    public static String[] defaultSSAPQuery_ = new String[]
            {
                    "capability/@standardID = 'ivo://ivoa.net/std/SSA'"
            };

    public static String[] defaultOBSCoreQuery_ = new String[]
            {
                    "capability/@standardID = 'ivo://ivoa.net/std/ObsCore'"
            };
    
    public static String[] defaultSLAPQuery_ = new String[]
            {
                    "capability/@standardID = 'ivo://ivoa.net/std/slap'"
            };


    public String getName()
    {
        return "SPLAT Registry Query";
    }

    public String getDescription()
    {
        return "Query a registry for all known  services";
    }

    protected Component createQueryComponent()
    {
        rqPanel_ = new SSAPRegistryQueryPanel(protocol_);
        if (protocol_ == SplatRegistryQuery.SSAP)
            rqPanel_.setPresetQueries( defaultSSAPQuery_ );
        else if (protocol_ == SplatRegistryQuery.OBSCORE)
            rqPanel_.setPresetQueries( defaultOBSCoreQuery_ );
        return rqPanel_;
    }

    public TableLoader createTableLoader()
    {
        try {
           // final SplatRegistryQuery query = rqPanel_.getRegistryQuery();
            final RegistryQuery query = rqPanel_.getRegistryQuery(protocol_);
            return new TableLoader()
                {
                    public TableSequence loadTables( StarTableFactory factory )
                            throws IOException
                    {
                        SSAPRegResource[] resources = (SSAPRegResource[]) query.getQueryResources();
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
