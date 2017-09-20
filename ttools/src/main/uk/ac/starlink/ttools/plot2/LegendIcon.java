package uk.ac.starlink.ttools.plot2;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.util.Arrays;
import javax.swing.Icon;

/**
 * Icon containing legend information for a plot.
 *
 * @author   Mark Taylor
 * @since    13 Feb 2013
 */
@Equality
public class LegendIcon implements Icon {

    private final LegendEntry[] entries_;
    private final Captioner captioner_;
    private final boolean border_;
    private final Color bgColor_;
    private final int width_;
    private final int height_;
    private final int iconWidth_;
    private final int labelWidth_;
    private final int lineHeight_;

    /**
     * Constructor.
     *
     * @param  entries   items to display in legend
     * @param  captioner  text renderer
     * @param  border   true to draw a line border around the legend
     * @param  bgColor   opaque background colour, null for transparent
     */
    public LegendIcon( LegendEntry[] entries, Captioner captioner,
                       boolean border, Color bgColor ) {
        entries_ = entries;
        captioner_ = captioner;
        border_ = border;
        bgColor_ = bgColor;
        int maxHeight = 0;
        int maxIconWidth = 0;
        int maxLabelWidth = 0;

        /* Work out geometry. */
        for ( int ie = 0; ie < entries_.length; ie++ ) {
            LegendEntry entry = entries_[ ie ];
            Rectangle bounds = captioner.getCaptionBounds( entry.getLabel() );
            maxHeight = Math.max( bounds.height, maxHeight );
            maxIconWidth = Math.max( entry.getIcon().getIconWidth(),
                                     maxIconWidth );
            maxLabelWidth = Math.max( bounds.width, maxLabelWidth );
        }
        lineHeight_ = maxHeight;
        iconWidth_ = maxIconWidth;
        labelWidth_ = maxLabelWidth;
        int gap = captioner.getPad();
        width_ = iconWidth_ + labelWidth_ + gap * 3;
        height_ = ( lineHeight_ + gap ) * entries_.length + gap;
    }

    /**
     * Returns this legend's captioner.
     *
     * @return captioner
     */
    public Captioner getCaptioner() {
        return captioner_;
    }

    /**
     * Indicates whether this legend has a border.
     *
     * @return  true for border, false for not
     */
    public boolean hasBorder() {
        return border_;
    }

    /**
     * Returns the background colour of this legend.
     *
     * @return  background colour
     */
    public Color getBackground() {
        return bgColor_;
    }

    public int getIconWidth() {
        return width_;
    }

    public int getIconHeight() {
        return height_;
    }

    public void paintIcon( Component c, Graphics g, int x0, int y0 ) {
        Color color0 = g.getColor();
        if ( bgColor_ != null ) {
            g.setColor( bgColor_ );
            g.fillRect( x0, y0, width_ - 1, height_ - 1 );
            g.setColor( color0 );
        }
        if ( border_ ) {
            g.setColor( Color.BLACK );
            g.drawRect( x0, y0, width_ - 1, height_ - 1 );
            g.setColor( color0 );
        }
        int gap = captioner_.getPad();
        g.setColor( Color.BLACK );
        for ( int ie = 0; ie < entries_.length; ie++ ) {
            LegendEntry entry = entries_[ ie ];
            Icon icon = entry.getIcon();
            int ixp = x0 + gap
                         + ( iconWidth_ - icon.getIconWidth() ) / 2;
            int iyp = y0 + gap
                         + ( gap + lineHeight_ ) * ie
                         + ( lineHeight_ - icon.getIconHeight() ) / 2;
            icon.paintIcon( c, g, ixp, iyp );
            int lxp = x0 + gap + iconWidth_ + gap;
            int lyp = y0 + ( gap + lineHeight_ ) * ( ie + 1 );
            g.translate( lxp, lyp );
            captioner_.drawCaption( entry.getLabel(), g );
            g.translate( -lxp, -lyp );
        }
        g.setColor( color0 );
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof LegendIcon ) {
            LegendIcon other = (LegendIcon) o;
            return Arrays.equals( this.entries_, other.entries_ )
                && this.captioner_.equals( other.captioner_ )
                && this.border_ == other.border_
                && PlotUtil.equals( this.bgColor_, other.bgColor_ );
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int code = 90125;
        code = 23 * code + Arrays.hashCode( entries_ );
        code = 23 * code + captioner_.hashCode();
        code = 23 * code + ( border_ ? 5 : 1 );
        code = 23 * code + PlotUtil.hashCode( bgColor_ );
        return code;
    }
}
