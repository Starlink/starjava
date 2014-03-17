package uk.ac.starlink.ttools.plot2;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Captioner implementation that uses Swing text drawing classes.
 *
 * @author   Mark Taylor
 * @since    12 Feb 2013
 */
public class BasicCaptioner implements Captioner {

    private final Font font_;
    private final Boolean antialias_;
    private static Graphics dummyGraphics_;

    /**
     * Constructs a captioner that will use a default font.
     */
    public BasicCaptioner() {
        this( null, false );
    }

    /**
     * Constructs a captioner that uses a given font.
     *
     * @param   font  font
     */
    public BasicCaptioner( Font font, Boolean antialias ) {
        font_ = font;
        antialias_ = antialias;
    }

    public void drawCaption( String label, Graphics g ) {
        Graphics2D g2 = (Graphics2D) g;
        Font font0 = g2.getFont();
        Object aaHint0 =
            g2.getRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING );
        if ( font_ != null ) {
            g2.setFont( font_ );
        }
        if ( antialias_ != null ) {
            g2.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
                                 antialias_.booleanValue()
                               ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                               : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF );
        }
        g2.drawString( label, 0, 0 );
        g2.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, aaHint0 );
        g2.setFont( font0 );
    }

    public Rectangle getCaptionBounds( String label ) {

        /* This follows the FontMetrics documentation to find the bounding box.
         * However, it seems to overestimate the required height by several
         * pixels, so positioning is not very good.  It may be related to
         * the fact that the Leading always seems to be zero for java fonts.
         * Using FontMetrics.getStringBounds doesn't do any better. */
        FontMetrics fm = getFontMetrics();
        int descent = fm.getDescent();
        int ascent = fm.getAscent();
        return new Rectangle( 0, -ascent, fm.stringWidth( label ),
                              ascent + descent );
    }

    public int getPad() {
        return getFontMetrics().charWidth( '0' ) / 2;
    }

    /**
     * Returns a FontMetrics for use with this object.
     *
     * @return  font metrics
     */
    private FontMetrics getFontMetrics() {
        Graphics g = getDummyGraphics();
        return g.getFontMetrics( font_ == null ? g.getFont() : font_ );
    }

    /**
     * Returns a fake graphics context.  Some methods need it, but
     * I think any old graphics will do the positioning required,
     * so just use one from a BufferedImage.
     *
     * @return   dummy graphics context
     */
    private synchronized static Graphics getDummyGraphics() {
        if ( dummyGraphics_ == null ) {
            dummyGraphics_ =
                new BufferedImage( 1, 1, BufferedImage.TYPE_INT_RGB )
               .createGraphics();
        }
        return dummyGraphics_;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof BasicCaptioner ) {
            BasicCaptioner other = (BasicCaptioner) o;
            return PlotUtil.equals( this.font_, other.font_ )
                && PlotUtil.equals( this.antialias_, other.antialias_ );
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int code = 55921;
        code = 23 * code + PlotUtil.hashCode( font_ );
        code = 23 * code + PlotUtil.hashCode( antialias_ );
        return code;
    }
}
