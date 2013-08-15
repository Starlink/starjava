package uk.ac.starlink.ttools.plot2.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.ValueInfo;

/**
 * Partial Coord implementation for quantities that are represented
 * as scalars both to the user and internally.
 *
 * @author   Mark Taylor
 * @since    4 Feb 2013
 */
public abstract class SingleCoord implements Coord {

    private DefaultValueInfo coordInfo_;
    private final boolean isRequired_;
    private final StorageType storageType_;
    private final List<Class<? extends DomainMapper>> domains_;

    /**
     * Constructor.
     *
     * @param   name   user-directed coordinate name
     * @param   description  user-directed coordinate description
     * @param   isRequired  true if this coordinate is required for plotting
     * @param   infoClass   class of user coordinate quantity
     * @param   storageType  storage type object
     * @param   domain  DomainMapper subtype for this coord, or null
     */
    protected SingleCoord( String name, String description, boolean isRequired,
                           Class infoClass, StorageType storageType,
                           Class<? extends DomainMapper> domain ) {
        isRequired_ = isRequired;
        storageType_ = storageType;
        List<Class<? extends DomainMapper>> domainList =
            new ArrayList<Class<? extends DomainMapper>>();
        domainList.add( domain );
        domains_ = Collections.unmodifiableList( domainList );
        setCoordInfo( new DefaultValueInfo( name, infoClass, description ) );
    }

    public ValueInfo[] getUserInfos() {
        return new ValueInfo[] { getUserInfo() };
    }

    public List<Class<? extends DomainMapper>> getUserDomains() {
        return domains_;
    }

    /**
     * Returns the single coordinate metadata object.
     * This may be modified as part of configuration.
     *
     * @return   modifiable info object
     */
    public DefaultValueInfo getUserInfo() {
        return coordInfo_;
    }

    /**
     * Sets the single coordinate metadata object
     * This may be written to modify configuration.
     *
     * @param  info  replaces existing user metadata
     */
    public void setCoordInfo( DefaultValueInfo info ) {
        coordInfo_ = info;
    }

    public StorageType getStorageType() {
        return storageType_;
    }

    public boolean isRequired() {
        return isRequired_;
    }
}
