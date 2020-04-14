package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.ttools.plot2.data.Coord;

/**
 * Aggregates a coordinate specification and the expression strings
 * that give its values.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2013
 */
public class CoordValue {

    private final Coord coord_;
    private final String[] exprs_;
    private final DomainMapper[] dms_;

    /**
     * Constructor.
     *
     * @param  coord  coordinate definition
     * @param  exprs  array of user-supplied expressions, one for each input
     *                value associated with the coordinate
     * @param  dms    array of DomainMappers, one for each input value;
     *                individual elements may be null if no DomainMapper
     *                is known
     */
    public CoordValue( Coord coord, String[] exprs, DomainMapper[] dms ) {
        coord_ = coord;
        exprs_ = exprs;
        dms_ = dms;
    }

    /**
     * Returns the coordinate definition.
     *
     * @return  coord
     */
    public Coord getCoord() {
        return coord_;
    }

    /**
     * Returns the user-supplied expressions for the coordinate's values.
     *
     * <p>The returned array has coord.getInputs().length elements.
     *
     * @return  expressions array
     */
    public String[] getExpressions() {
        return exprs_;
    }

    /**
     * Returns the domain mappers used to decode the coordinate's input values,
     * where known.  Note that individual elements may be null if the
     * domain mapper is not known; in this case downstream code will
     * need to come up with a plausible value.
     *
     * <p>The returned array has coord.getInputs().length elements.
     *
     * @return   domain mapper array
     */
    public DomainMapper[] getDomainMappers() {
        return dms_;
    }
}
