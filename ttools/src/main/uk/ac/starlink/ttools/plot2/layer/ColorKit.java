package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import uk.ac.starlink.ttools.plot2.data.Tuple;

/**
 * Rule for colouring points according to data values.
 *
 * @author   Mark Taylor
 * @since    23 Jul 2018
 */
public interface ColorKit {

    /**
     * Acquires a colour appropriate for a given tuple.
     *
     * @param  tuple  tuple
     * @return  plotting colour, or null to omit point
     */
    Color readColor( Tuple tuple );
}
