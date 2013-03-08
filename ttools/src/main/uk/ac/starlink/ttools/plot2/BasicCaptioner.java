package uk.ac.starlink.ttools.plot2;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
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
    private static Graphics dummyGraphics_;

    /**
     * Constructs a captioner that will use a default font.
     */
    public BasicCaptioner() {
        this( null );
    }

    /**
     * Constructs a captioner that uses a given font.
     *
     * @param   font  font
     */
    public BasicCaptioner( Font font ) {
        font_ = font;
    }

    public void drawCaption( String label, Graphics g ) {
        Font oldFont = null;
        if ( font_ != null ) {
            oldFont = g.getFont();
            g.setFont( font_ );
        }
        g.drawString( label, 0, 0 );
        if ( oldFont != null ) {
            g.setFont( oldFont );
        }
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
            return Arrays.equals( new Font[] { other.font_ },
                                  new Font[] { this.font_ } );
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode( new Font[] { font_ } );
    }
}
