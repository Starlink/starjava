package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Graphics;

/**
 * Marker interface labelling objects which are used to store rendering
 * data specific to a given PaperType.
 *
 * @author   Mark Taylor
 * @since    14 Feb 2013
 * @see   PaperType
 */
public interface Paper {

    /**
     * Returns the PaperType which generated and can write to this paper.
     * This method is not essential, but could be useful for assertions.
     *
     * @return   paper type
     */
    PaperType getPaperType();
}
