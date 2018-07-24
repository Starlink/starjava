package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import uk.ac.starlink.ttools.plot2.data.Tuple;

/**
 * Trivial ColorKit implementation that always returns the same colour.
 *
 * @author   Mark Taylor
 * @since    23 Jul 2018
 */
public class FixedColorKit implements ColorKit {

    private final Color color_;

    /**
     * Constructor.
     *
     * @param  color  colour
     */
    public FixedColorKit( Color color ) {
        color_ = color;
    }

    public Color readColor( Tuple tuple ) {
        return color_;
    }
}
