package uk.ac.starlink.ttools.plot2.task;

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

    /**
     * Constructor.
     *
     * @param  coord  coordinate definition
     * @param  exprs  array of user-supplied expressions, one for each input
     *                value associated with the coordinate
     */
    public CoordValue( Coord coord, String[] exprs ) {
        coord_ = coord;
        exprs_ = exprs;
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
     * The returned array has coord.getInputs().length elements.
     *
     * @return  expressions array
     */
    public String[] getExpressions() {
        return exprs_;
    }
}
