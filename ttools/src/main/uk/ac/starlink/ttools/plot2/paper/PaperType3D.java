package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Color;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot2.Glyph;

/**
 * PaperType sub-interface for making 3-dimensional plots.
 *
 * <p>Any decals placed by {@link #placeDecal} will be painted in the
 * background first, then any glyphs added by {@link #placeGlyph}
 * will be added to the 3D scene, then 3D rendering will be done
 * obscuring any background decals.
 * Note this is not really 3D for the decals; bear that in mind when
 * writing 3D plotters.
 *
 * @author   Mark Taylor
 * @since    14 Feb 2013
 */
public interface PaperType3D extends PaperType {

    /**
     * Places a glyph at a 3-d position in the space.
     *
     * @param  paper  graphics destination specific to this PaperType
     * @param  gx  graphics X coordinate
     * @param  gy  graphics Y coordinate
     * @param  dz  Z-buffer coordinate; lower values are closer to the viewer
     * @param  glyph  graphics shape
     * @param  color  colour for glyph
     */
    void placeGlyph( Paper paper, double gx, double gy, double dz,
                     Glyph glyph, Color color );
}
