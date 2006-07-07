package uk.ac.starlink.vo;

import java.beans.IntrospectionException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.xml.rpc.ServiceException;
import org.us_vo.www.SimpleResource;
import uk.ac.starlink.table.BeanStarTable;

/**
 * Table representing the results of a registry query.
 *
 * @author   Mark Taylor
 * @since    6 Jul 2006
 */
public class RegistryStarTable extends BeanStarTable {
    public RegistryStarTable( RegistryQuery query )
            throws IntrospectionException, RemoteException, ServiceException {
        super( SimpleResource.class );
        setParameters( new ArrayList( Arrays.asList( query.getMetadata() ) ) );
        setData( query.performQuery() );
    }
}
