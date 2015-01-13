package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * PaintPaperType for 2-dimensional plots.
 * Suitable for output to vector graphics media.
 *
 * @author   Mark Taylor
 * @since    14 Feb 2013
 */
public abstract class PaintPaperType2D extends PaintPaperType
                                       implements PaperType2D {

    /**
     * Constructor.
     */
    protected PaintPaperType2D() {
        super( "Paint2D", true );
    }

    protected Paper createPaper( Graphics g, Rectangle bounds ) {
        return new Paper2D( this, (Graphics2D) g );
    }

    protected void flushPaper( Paper paper ) {
    }

    public void placeDecal( Paper paper, Decal decal ) {
        decal.paintDecal( ((Paper2D) paper).graphics_ );
    }

    /**
     * Constructs an instance of this class.
     *
     * @param  quantise  whether glyph coordinates should be snapped to
     *                   the pixel grid before use
     * @return  instance
     */
    public static PaintPaperType2D createPaperType( boolean quantise ) {
        if ( quantise ) {
            return new PaintPaperType2D() {
                public void placeGlyph( Paper paper, double dx, double dy,
                                        Glyph glyph, Color color ) {
                    int gx = PlotUtil.ifloor( dx );
                    int gy = PlotUtil.ifloor( dy );
                    Graphics g = ((Paper2D) paper).graphics_;
                    Color color0 = g.getColor();
                    g.setColor( color );
                    g.translate( gx, gy );
                    glyph.paintGlyph( g );
                    g.translate( -gx, -gy );
                    g.setColor( color0 );
                }
            };
        }
        else {
            return new PaintPaperType2D() {
                public void placeGlyph( Paper paper, double dx, double dy,
                                        Glyph glyph, Color color ) {
                    Graphics2D g2 = ((Paper2D) paper).graphics_;
                    Color color0 = g2.getColor();
                    g2.setColor( color );
                    g2.translate( dx, dy );
                    glyph.paintGlyph( g2 );
                    g2.translate( -dx, -dy );
                    g2.setColor( color0 );
                }
            };
        }
    }

    /**
     * Paper implementation for use with this class.
     */
    private static class Paper2D implements Paper {

        final PaperType paperType_;
        final Graphics2D graphics_;

        /**
         * Constructor.
         *
         * @param   paperType  paper type instance which created this paper
         * @param   graphics  graphics destination
         */
        Paper2D( PaperType paperType, Graphics2D graphics ) {
            paperType_ = paperType;
            graphics_ = graphics;
        }

        public PaperType getPaperType() {
            return paperType_;
        }
    }
}
