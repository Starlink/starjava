package uk.ac.starlink.ndtools;

import java.io.IOException;
import uk.ac.starlink.ndx.Ndx;

/**
 * Disposes of an Ndx.
 *
 * @author   Mark Taylor
 * @since    19 Aug 2005
 */
public interface NdxConsumer {

    /**
     * Consumes an Ndx.
     *
     * @param  ndx  Ndx to consume
     */
    public void consume( Ndx ndx ) throws IOException;
}
