package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Rectangle;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * Translates Glyph paint operations into pixel iterators.
 * This class provides an implementation of the
 * {@link uk.ac.starlink.ttools.plot2.paper.Paper} interface
 * which means it can be painted on by a {@link ShapePainter}.
 * Concrete subclasses are handed a Pixer giving the pixels actually
 * painted by each received glyph.
 *
 * @author   Mark Taylor
 * @since    1 Oct 2015
 */
public abstract class GlyphPaper implements Paper {

    private final Rectangle bounds_;
    private final GlyphPaperType paperType_;

    /**
     * Constructor.
     *
     * @param  plotBounds   bounds within which all pixels must be contained
     */
    public GlyphPaper( Rectangle plotBounds ) {
        bounds_ = new Rectangle( plotBounds );
        paperType_ = createGlyphPaperType();
    }

    /**
     * For each glyph painted on this paper, a pixer will be passed to
     * this method that iterates over all the pixels within this paper's bounds.
     * The supplied pixer will not contain any pixels outside the plot bounds.
     *
     * @param  pixer  pixel iterator
     */
    public abstract void glyphPixels( Pixer pixer );

    /**
     * Returns a partial PaperType implementation to use with this object.
     * The returned value is private to this paper instance.
     */
    public GlyphPaperType getPaperType() {
        return paperType_;
    }

    /**
     * Type of paper used by this object.  It implements both PaperType2D
     * and PaperType3D, but does not support the full contract of PaperType;
     * The methods not concerned with painting glyphs may throw
     * <code>UnsupportedOperationException</code>s.
     */
    public interface GlyphPaperType extends PaperType2D, PaperType3D {
    }

    /**
     * Returns a PaperType implementation for use with an instance of
     * this class.
     */
    private GlyphPaperType createGlyphPaperType() {
        return new GlyphPaperType() {
            public void placeGlyph( Paper paper, double dx, double dy,
                                    Glyph glyph, Color color ) {

                /* Get the base position at which the glyph is drawn. */
                int gx = PlotUtil.ifloor( dx );
                int gy = PlotUtil.ifloor( dy );

                /* Get the presented glyph's pixels clipped to the bounds
                 * of this paper. */
                Rectangle cbox = new Rectangle( bounds_ );
                cbox.translate( -gx, -gy );
                Pixer pixer = glyph.createPixer( cbox );

                /* Pass the pixels on to the subclass implementation. */
                if ( pixer != null ) {
                    GlyphPaper.this
                   .glyphPixels( Pixers.translate( pixer, gx, gy ) );
                }
            }

            public void placeGlyph( Paper paper, double dx, double dy,
                                    double dz, Glyph glyph, Color color ) {
                placeGlyph( paper, dx, dy, glyph, color );
            }

            public boolean isBitmap() {
                return true;
            }

            public void placeDecal( Paper paper, Decal decal ) {
                throw new UnsupportedOperationException();
            }

            public Icon createDataIcon( Surface surface, Drawing[] drawing,
                                        Object[] plans, DataStore dataStore,
                                        boolean requireCached ) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
