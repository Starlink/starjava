package uk.ac.starlink.ttools.plot2;

import java.awt.Component;
import java.awt.Graphics;
import java.util.Arrays;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.util.IconUtils;

/**
 * Aggregates a plot style or group of styles and a label to be
 * paired together as one entry in a plot legend.
 *
 * @author   Mark Taylor
 * @since    13 Feb 2013
 */
@Equality
public class LegendEntry {
    private final String label_;
    private final Style[] styles_;
    private final Icon icon_;

    /**
     * Constructs a legend entry for a group of styles.
     * This would typically be used where the same data set is represented
     * by several different layers in a plot.
     *
     * @param   label   dataset label
     * @param   styles  dataset styles
     */
    public LegendEntry( String label, Style[] styles ) {
        label_ = label;
        styles_ = styles;
        int nstyle = styles.length;
        if ( nstyle == 0 ) {
            icon_ = IconUtils.emptyIcon( 4, 4 );
        }
        else if ( nstyle == 1 ) {
            icon_ = styles[ 0 ].getLegendIcon();
        }
        else {
            Icon[] icons = new Icon[ nstyle ];
            for ( int i = 0; i < nstyle; i++ ) {
                icons[ i ] = styles[ i ].getLegendIcon();
            }
            icon_ = new MultiIcon( icons );
        }
    }

    /**
     * Constructs a legend entry for a single style.
     *
     * @param   style   dataset style
     * @param   label   dataset label
     */
    public LegendEntry( String label, Style style ) {
        this( label, new Style[] { style } );
    }

    /**
     * Returns the icon associated with this entry.
     *
     * @return  icon
     */
    public Icon getIcon() {
        return icon_;
    }

    /**
     * Returns the text label associated with this entry.
     *
     * @return  label
     */
    public String getLabel() {
        return label_;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof LegendEntry ) {
            LegendEntry other = (LegendEntry) o;
            return this.label_.equals( other.label_ )
                && Arrays.equals( this.styles_, other.styles_ );
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int code = 29119;
        code = 23 * code + label_.hashCode();
        code = 23 * code + Arrays.hashCode( styles_ );
        return code;
    }

    /**
     * Icon which superimposes a sequence of other icons on top of
     * each other, trying to center them.
     */
    private static class MultiIcon implements Icon {
        private final Icon[] icons_;
        private final int width_;
        private final int height_;

        /**
         * Constructor.
         *
         * @param icons  constituent icons
         */
        MultiIcon( Icon[] icons ) {
            icons_ = icons;
            int w = 0;
            int h = 0;
            for ( int i = 0; i < icons.length; i++ ) {
                w = Math.max( w, icons[ i ].getIconWidth() );
                h = Math.max( h, icons[ i ].getIconHeight() );
            }
            width_ = w;
            height_ = h;
        }

        public int getIconWidth() {
            return width_;
        }

        public int getIconHeight() {
            return height_;
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            int xmid = width_ / 2;
            int ymid = height_ / 2;
            for ( int i = 0; i < icons_.length; i++ ) {
                Icon icon = icons_[ i ];
                int xoff = xmid - icon.getIconWidth() / 2;
                int yoff = ymid - icon.getIconHeight() / 2;
                icon.paintIcon( c, g, x + xoff, y + yoff );
            }
        }
    }
}
