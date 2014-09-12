package uk.ac.starlink.ttools.plot2.data;

import uk.ac.starlink.table.DomainMapper;

/**
 * Characterises a coordinate value as specified by the user.
 * There may be multiple Input values corresponding to a single
 * coordinate ({@link Coord} as used by the plotting system.
 *
 * @author   Mark Taylor
 * @since    12 Sep 2014
 */
public class Input {

    private final InputMeta meta_;
    private final Class valueClazz_;
    private final Class<? extends DomainMapper> domain_;

    /**
     * Constructor.
     *
     * @param  meta   user-directed metadata
     * @param  valueClazz  data value class
     * @param  domain  data value domain, may be null
     */
    public Input( InputMeta meta, Class valueClazz,
                  Class<? extends DomainMapper> domain ) {
        meta_ = meta;
        valueClazz_ = valueClazz;
        domain_ = domain;
    }

    /**
     * Returns user-directed metadata describing this input.
     *
     * @return  metadata
     */
    public InputMeta getMeta() {
        return meta_;
    }

    /**
     * Returns the data (super-)type of values described by this input.
     *
     * @return  value data type
     */
    public Class getValueClass() {
        return valueClazz_;
    }

    /**
     * Returns the common value domain in which this user coordinate
     * will be used.
     * The return value is a DomainMapper abstract sub-type.
     * This sub-type effectively defines a target value domain.
     * Null entries for this list are the norm,
     * indicating that the user values will just be interpreted as numeric
     * values, but a non-null domain value can be used if a particular
     * interpretation (for instance time) is going to be imposed.
     *
     * @return  domain mapper subtype, or null
     */
    public Class<? extends DomainMapper> getDomain() {
        return domain_;
    }
}
