package uk.ac.starlink.ttools.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

/**
 * Provides several factory methods for constructing StyleSets
 * which dispense {@link BarStyle}s.
 *
 * @author   Mark Taylor
 * @since    16 Nov 2005
 */
public class BarStyles {

    /**
     * Abstract style set implementation which does the same shape
     * but different graphical attributes for different members 
     * of the sequence.
     */
    private static class AutoBarStyleSet implements StyleSet {
        private final String name_;
        private final boolean rotateColor_;
        private final boolean rotateDash_;
        private final BarStyle.Form form_;
        private final BarStyle.Placement placement_;

        /**
         * Constructor.
         *
         * @param  name  style set name
         * @param  rotateColor  true iff {@link java.awt.Color}s 
         *         are to be rotated between members of this set
         * @param  rotateDash  true iff dash patterns
         *         are to be rotated between members of this set
         */
        protected AutoBarStyleSet( String name, boolean rotateColor,
                                   boolean rotateDash, BarStyle.Form form,
                                   BarStyle.Placement placement ) {
            name_ = name;
            rotateColor_ = rotateColor;
            rotateDash_ = rotateDash;
            form_ = form;
            placement_ = placement;
        }

        public String getName() {
            return name_;
        }

        public Style getStyle( int index ) {
            Color color = rotateColor_ ? Styles.getColor( index )
                                       : Styles.PLAIN_COLOR;
            BarStyle style = new BarStyle( color, form_, placement_ );
            style.setDash( rotateDash_ ? Styles.getDash( index )
                                       : Styles.getDash( 0 ) );
            return style;
        }
    }

    /**
     * Returns a new style set which draws filled full rectangles.
     *
     * @param  name  style set name
     * @return   style set
     */
    public static StyleSet filled( String name ) {
        return new AutoBarStyleSet( name, true, false,
                                    BarStyle.FORM_FILLED,
                                    BarStyle.PLACE_OVER );
    }

    /**
     * Returns a new style set which draws filled 3d full rectangles.
     *
     * @param  name  style set name
     * @return   style set
     */
    public static StyleSet filled3d( String name ) {
        return new AutoBarStyleSet( name, true, false,
                                    BarStyle.FORM_FILLED3D,
                                    BarStyle.PLACE_OVER );
    }

    /**
     * Returns a new style set which draws open full rectangles.
     *
     * @param  name  style set name
     * @param  rotateColor  whether to have different colours for 
     *                      different bars
     * @param  rotateDash  whether to have different stroke styles
     *                       for different bars
     * @return   style set
     */
    public static StyleSet open( String name, boolean rotateColor,
                                 boolean rotateDash ) {
        return new AutoBarStyleSet( name, rotateColor, rotateDash,
                                    BarStyle.FORM_OPEN,
                                    BarStyle.PLACE_OVER );
    }

    /**
     * Returns a new style set which draws only the tops of bars.
     *
     * @param  name  style set name
     * @param  rotateColor  whether to have different colours for 
     *                      different bars
     * @param  rotateDash  whether to have different stroke styles
     *                       for different bars
     * @return   style set
     */
    public static StyleSet tops( String name, boolean rotateColor,
                                 boolean rotateDash ) {
        return new AutoBarStyleSet( name, rotateColor, rotateDash,
                                    BarStyle.FORM_TOP,
                                    BarStyle.PLACE_OVER );
    }

    /**
     * Returns a new style set which draws a 1-d spike for each subset.
     *
     * @param  name  style set name
     * @param  rotateColor  whether to have different colours for 
     *                      different bars
     * @param  rotateDash  whether to have different stroke styles
     *                       for different bars
     * @return   style set
     */
    public static StyleSet spikes( String name, boolean rotateColor,
                                   boolean rotateDash ) {
        return new AutoBarStyleSet( name, rotateColor, rotateDash,
                                    BarStyle.FORM_SPIKE,
                                    BarStyle.PLACE_ADJACENT );
    }

    /**
     * Returns a new style set which draws filled rectangles side by
     * side (one for each subset).
     *
     * @param  name  style set name
     * @return   style set
     */
    public static StyleSet sideFilled( String name ) {
        return new AutoBarStyleSet( name, true, false,
                                    BarStyle.FORM_FILLED,
                                    BarStyle.PLACE_ADJACENT );
    }

    /**
     * Returns a new style set which draws 3d filled rectangles side by
     * side (one for each subset).
     *
     * @param  name  style set name
     * @return   style set
     */
    public static StyleSet sideFilled3d( String name ) {
        return new AutoBarStyleSet( name, true, false,
                                    BarStyle.FORM_FILLED3D,
                                    BarStyle.PLACE_ADJACENT );
    }

    /**
     * Returns a new style set which draws open rectangles side by
     * side (one for each subset).
     *
     * @param  name  style set name
     * @param  rotateColor  whether to have different colours for 
     *                      different bars
     * @param  rotateDash  whether to have different stroke styles
     *                       for different bars
     * @return   style set
     */
    public static StyleSet sideOpen( String name, boolean rotateColor,
                                     boolean rotateDash ) {
        return new AutoBarStyleSet( name, rotateColor, rotateDash,
                                    BarStyle.FORM_OPEN,
                                    BarStyle.PLACE_ADJACENT );
    }

    /**
     * Generates an icon based on a StyleSet which displensed BarStyles.
     * This icon is suitable for putting in a menu.
     */
    public static Icon getIcon( final StyleSet styleSet ) {
        final int height = 16;
        final double[][] data = { { 0, 4, 7, 12, 15, 9, 0 },
                                  { 0, 2, 6,  8,  9, 3, 0 } };
        final int nSet = data.length;
        final int nBar = data[ 0 ].length - 2;
        final int barWidth = nSet * 7;
        return new Icon() {
            public int getIconHeight() {
                return height;
            }
            public int getIconWidth() {
                return nBar * barWidth;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                for ( int iset = 0; iset < nSet; iset++ ) {
                    for ( int ibar = 0; ibar < nBar; ibar++ ) {
                        int lastDatum = (int) data[ iset ][ ibar ];
                        int datum = (int) data[ iset ][ ibar + 1 ];
                        int nextDatum = (int) data[ iset ][ ibar + 2 ];
                        BarStyle style = (BarStyle) styleSet.getStyle( iset );
                        int xlo = x + ibar * barWidth;
                        int xhi = xlo + barWidth;
                        int ylo = height - datum;
                        int lastYlo = y + height - lastDatum;
                        int nextYlo = y + height - nextDatum;
                        int yhi = y + height;
                        if ( ibar == 0 ) {
                            style.drawEdge( g, xlo, ylo, lastYlo, iset, nSet );
                        }
                        style.drawBar( g, xlo, xhi, ylo, yhi, iset, nSet );
                        style.drawEdge( g, xhi, ylo, nextYlo, iset, nSet );
                    }
                }
            }
        };
    }

    /**
     * Generates an icon based on a BarStlye.Form object.
     * This icon is suitable for putting in a menu.
     */
    public static Icon getIcon( BarStyle.Form form ) {
        final int height = 16;
        final double[] data = { 0, 4, 7, 12, 15, 9, 0 };
        final int nBar = data.length - 2;
        final int barWidth = 10;
        final BarStyle style =
            new BarStyle( Color.BLACK, form, BarStyle.PLACE_OVER );
        return new Icon() {
            public int getIconHeight() {
                return height;
            }
            public int getIconWidth() {
                return nBar * barWidth;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                for ( int ibar = 0; ibar < nBar; ibar++ ) {
                    int lastDatum = (int) data[ ibar ];
                    int datum = (int) data[ ibar + 1 ];
                    int nextDatum = (int) data[ ibar + 2 ];
                    int xlo = x + ibar * barWidth;
                    int xhi = xlo + barWidth;
                    int ylo = y + height - datum;
                    int lastYlo = y + height - lastDatum;
                    int nextYlo = y + height - nextDatum;
                    int yhi = y + height;
                    if ( ibar == 0 ) {
                        style.drawEdge( g, xlo, ylo, lastYlo, 0, 1 );
                    }
                    style.drawBar( g, xlo, xhi, ylo, yhi, 0, 1 );
                    style.drawEdge( g, xhi, ylo, nextYlo, 0, 1 );
                }
            }
        };
    }
}
