package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Glyph;

/**
 * PaintPaperType for 2-dimensional plots.
 * Suitable for output to vector graphics media.
 *
 * @author   Mark Taylor
 * @since    14 Feb 2013
 */
public class PaintPaperType2D extends PaintPaperType
                              implements PaperType2D {

    public PaintPaperType2D() {
        super( "Paint2D", true );
    }

    protected Paper createPaper( Graphics g, Rectangle bounds ) {
        return new Paper2D( this, g );
    }

    protected void flushPaper( Paper paper ) {
    }

    public void placeDecal( Paper paper, Decal decal ) {
        decal.paintDecal( ((Paper2D) paper).graphics_ );
    }

    public void placeGlyph( Paper paper, int gx, int gy,
                            Glyph glyph, Color color ) {
        Graphics g = ((Paper2D) paper).graphics_;
        Color color0 = g.getColor();
        g.setColor( color );
        g.translate( gx, gy );
        glyph.paintGlyph( g );
        g.translate( -gx, -gy );
        g.setColor( color0 );
    }

    /**
     * Paper implementation for use with this class.
     */
    private static class Paper2D implements Paper {

        final PaperType paperType_;
        final Graphics graphics_;

        /**
         * Constructor.
         *
         * @param   paperType  paper type instance which created this paper
         * @param   graphics  graphics destination
         */
        Paper2D( PaperType paperType, Graphics graphics ) {
            paperType_ = paperType;
            graphics_ = graphics;
        }

        public PaperType getPaperType() {
            return paperType_;
        }
    }
}
