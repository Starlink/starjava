package uk.ac.starlink.ttools.plot2.data;

import uk.ac.starlink.table.DomainMapper;

/**
 * Partial Coord implementation for quantities that are represented
 * as scalars both to the user and internally.
 *
 * @author   Mark Taylor
 * @since    4 Feb 2013
 */
public abstract class SingleCoord implements Coord {

    private final Input input_;
    private final boolean isRequired_;
    private final StorageType storageType_;

    /**
     * Constructor.
     *
     * @param   meta   descriptive metadata for single user coordinate
     * @param   isRequired  true if this coordinate is required for plotting
     * @param   valueClass   class of input coordinate quantity
     * @param   storageType  storage type object
     * @param   domain  DomainMapper subtype for this coord, or null
     */
    protected SingleCoord( InputMeta meta, boolean isRequired,
                           Class valueClass, StorageType storageType,
                           Class<? extends DomainMapper> domain ) {
        input_ = new Input( meta, valueClass, domain );
        isRequired_ = isRequired;
        storageType_ = storageType;
    }

    public Input[] getInputs() {
        return new Input[] { getInput() };
    }

    /**
     * Returns the single user data input object.
     *
     * @return   modifiable info object
     */
    public Input getInput() {
        return input_;
    }

    public StorageType getStorageType() {
        return storageType_;
    }

    public boolean isRequired() {
        return isRequired_;
    }
}
