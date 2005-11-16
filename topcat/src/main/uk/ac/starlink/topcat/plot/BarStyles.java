package uk.ac.starlink.topcat.plot;

import java.awt.Graphics;

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
     * but different colours for different members of the sequence.
     */
    private static abstract class ColoredBarStyleSet implements StyleSet {
        private final String name_;

        /**
         * Constructor.
         *
         * @param  name  style set name
         */
        protected ColoredBarStyleSet( String name ) {
            name_ = name;
        }

        /**
         * Draws the shape of a bar.
         *
         * <p>Arguments are the same as {@link BarStyle#drawBarShape}
         */
        protected abstract void drawBarShape( Graphics g, int x, int y,
                                              int width, int height, 
                                              int iseq, int nseq );

        public String getName() {
            return name_;
        }

        public Style getStyle( int index ) {
            return new BarStyle( Styles.getColor( index ) ) {
                protected void drawBarShape( Graphics g, int x, int y,
                                             int width, int height,
                                             int iseq, int nseq ) {
                    ColoredBarStyleSet.this.drawBarShape( g, x, y,
                                                          width, height,
                                                          iseq, nseq );
                }
            };
        }
    }

    /**
     * Returns a new style set which draws filled full rectangles.
     *
     * @param  name  style set name
     * @return   style set
     */
    public static StyleSet filled( String name ) {
        return new ColoredBarStyleSet( name ) {
            protected void drawBarShape( Graphics g, int x, int y,
                                         int width, int height,
                                         int iseq, int nseq ) {
                g.fillRect( x, y, width - 1, height );
            }
        };
    }

    /**
     * Returns a new style set which draws open full rectangles.
     *
     * @param  name  style set name
     * @return   style set
     */
    public static StyleSet open( String name ) {
        return new ColoredBarStyleSet( name ) {
            protected void drawBarShape( Graphics g, int x, int y,
                                         int width, int height,
                                         int iseq, int nseq ) {
                g.drawRect( x, y, width - 1, height );
            }
        };
    }
}
