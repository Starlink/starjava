package uk.ac.starlink.tptask;

import uk.ac.starlink.tplot.*;

/**
 * Defines an iterator over plotting styles.
 *
 * @author   Mark Taylor
 * @since    22 Apr 2008
 */
public interface StyleFactory {

    /**
     * Returns the next unused style in the sequence.
     *
     * @return  plotting style
     */
    Style getNextStyle();
}
