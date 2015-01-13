package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Color;
import java.awt.Graphics;
import uk.ac.starlink.ttools.plot2.Glyph;

/**
 * PaperType sub-interface for making 2-dimensional plots.
 *
 * @author   Mark Taylor
 * @since    14 Feb 2013
 */
public interface PaperType2D extends PaperType {

    /**
     * Places a glyph at a 2-d position on the paper.
     *
     * @param   paper  graphics destination specific to this PaperType
     * @param   gx  X coordinate
     * @param   gy  Y coordinate
     * @param   glyph  graphics shape
     * @param   color  colour for glyph
     */
    void placeGlyph( Paper paper, double gx, double gy,
                     Glyph glyph, Color color );
}
