package uk.ac.starlink.ttools.plot2.data;

import uk.ac.starlink.table.Domain;

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
    private final Domain<?> domain_;

    /**
     * Constructor.
     *
     * @param  meta   user-directed metadata
     * @param  domain  data value domain
     */
    public Input( InputMeta meta, Domain<?> domain ) {
        meta_ = meta;
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
     * Returns the value domain which this input represents.
     * This can also be used to determine what input value types are
     * acceptable.
     *
     * @return   value domain
     */
    public Domain<?> getDomain() {
        return domain_;
    }

    /**
     * Returns an object that behaves like this one but has different
     * metadata as supplied.
     *
     * @param  meta  new metadata object
     * @return   new Input instance with given metadata
     */
    public Input withMeta( InputMeta meta ) {
        return new Input( meta, getDomain() );
    }
}
