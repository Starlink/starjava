package uk.ac.starlink.ttools.plot2.data;

import uk.ac.starlink.table.DefaultValueInfo;
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

    /**
     * Constructor.
     *
     * @param   name   user-directed coordinate name
     * @param   description  user-directed coordinate description
     * @param   isRequired  true if this coordinate is required for plotting
     * @param   infoClass   class of user coordinate quantity
     * @param   storageType  storage type object
     */
    protected SingleCoord( String name, String description, boolean isRequired,
                           Class infoClass, StorageType storageType ) {
        isRequired_ = isRequired;
        storageType_ = storageType;
        setCoordInfo( new DefaultValueInfo( name, infoClass, description ) );
    }

    public ValueInfo[] getUserInfos() {
        return new ValueInfo[] { getUserInfo() };
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
