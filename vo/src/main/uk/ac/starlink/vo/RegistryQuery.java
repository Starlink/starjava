package uk.ac.starlink.vo;

import java.net.URL;
import java.rmi.RemoteException;
import javax.xml.rpc.ServiceException;
import org.us_vo.www.SimpleResource;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Describes a query on a registry.
 *
 * @author   Mark Taylor (Starlink)
 * @since    4 Jan 2005
 */
public class RegistryQuery {

    private final URL registry_;
    private final String text_;

    /** Text of special query represening a dump of all registry records. */
    public static final String ALL_RECORDS = "All records";

    /** Description of metadata item describing registry location. */
    public final static ValueInfo REGISTRY_INFO = new DefaultValueInfo(
        "Registry Location", URL.class, "URL of registry queried"
    );

    /** Description of metadata item describing query text. */
    public final static ValueInfo TEXT_INFO = new DefaultValueInfo(
        "Registry Query", String.class, "Text of query made to the registry"
    );

    /**
     * Constructs a new query object.
     * The text of the query can either be the WHERE clause of an SQL query
     * or the special value {@link #ALL_RECORDS} to indicate a dump of
     * the whole registry.
     *
     * @param  registry  URL of the registry service
     * @param  text   text for the registry query
     *
     */
    public RegistryQuery( URL registry, String text ) {
        registry_ = registry;
        text_ = text;
    }

    /**
     * Executes the query described by this object and returns the
     * result.
     *
     * @return   query result
     */
    public SimpleResource[] performQuery()
            throws RemoteException, ServiceException {
        return new RegistryInterrogator( registry_ )
              .getResources( text_ == ALL_RECORDS ? null : text_ );
    }

    /**
     * Returns the query text.
     *
     * @return  query
     */
    public String getText() {
        return text_;
    }

    /**
     * Returns the registry URL.
     *
     * @return  url
     */
    public URL getRegistry() {
        return registry_;
    }

    /**
     * Returns a set of DescribedValue objects which characterise this query.
     * These would be suitable for use in the parameter list of a 
     * {@link uk.ac.starlink.table.StarTable} resulting from the execution
     * of this query.
     */
    public DescribedValue[] getMetadata() {
        return new DescribedValue[] {
            new DescribedValue( REGISTRY_INFO, getRegistry() ),
            new DescribedValue( TEXT_INFO, getText() ),
        };
    }

    public String toString() {
        return text_;
    }
}
