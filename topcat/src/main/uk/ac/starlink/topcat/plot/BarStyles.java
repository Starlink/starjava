package uk.ac.starlink.topcat.plot;

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

        /**
         * Draws the shape of an edge.
         *
         * <p>Arguments are the same as {@link BarStyle#drawEdgeShape}
         */
        protected void drawEdgeShape( Graphics g, int x, int y1, int y2,
                                      int iseq, int nseq ) {
            // no action.
        }

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
                protected void drawEdgeShape( Graphics g, int x, int y1, int y2,
                                              int iseq, int nseq ) {
                    ColoredBarStyleSet.this.drawEdgeShape( g, x, y1, y2,
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
                g.fillRect( x, y, Math.max( width - 1, 1 ), height );
            }
        };
    }

    /**
     * Returns a new style set which draws filled 3d full rectangles.
     *
     * @param  name  style set name
     * @return   style set
     */
    public static StyleSet filled3d( String name ) {
        return new ColoredBarStyleSet( name ) {
            protected void drawBarShape( Graphics g, int x, int y,
                                         int width, int height,
                                         int iseq, int nseq ) {
                g.fill3DRect( x, y, Math.max( width - 1, 1 ), height, true );
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
                g.drawRect( x, y, Math.max( width - 1, 1 ), height );
            }
        };
    }

    /**
     * Returns a new style set which draws the tops of bars using XOR logic.
     * This is pretty ugly for more than one set.
     *
     * @param  name  style set name
     * @return   style set
     */
    public static StyleSet xorTops( String name ) {
        return new ColoredBarStyleSet( name ) {
            protected void drawBarShape( Graphics g, int x, int y,
                                         int width, int height,
                                         int iseq, int nseq ) {
                g.setXORMode( Color.white ) ;
                g.drawLine( x + iseq, y + height, x + iseq, y );
                g.drawLine( x + iseq + width, y + height, x + iseq + width, y );
                g.setPaintMode();
                g.drawLine( x + iseq + 1, y, x + iseq + width - 1, y );
            }
        };
    }

    /**
     * Returns a new style set which draws only the tops of bars.
     *
     * @param  name  style set name
     * @return   style set
     */
    public static StyleSet tops( String name ) {
        return new ColoredBarStyleSet( name ) {
            protected void drawBarShape( Graphics g, int x, int y,
                                         int width, int height,
                                         int iseq, int nseq ) {
                g.drawLine( x, y, x + width, y );
            }
            protected void drawEdgeShape( Graphics g, int x, int y1, int y2,
                                          int iseq, int nseq ) {
                g.drawLine( x, y1, x, y2 );
            }
        };
    }

    /**
     * Returns a new style set which draws a 1-d spike for each subset.
     *
     * @param  name  style set name
     * @return   style set
     */
    public static StyleSet spikes( String name ) {
        return new ColoredBarStyleSet( name ) {
            protected void drawBarShape( Graphics g, int x, int y,
                                         int width, int height,
                                         int iseq, int nseq ) {
                x += iseq * ( width / nseq ) + 
                     ( ( nseq - 1 ) * width / nseq ) / 2;
                g.drawLine( x, y, x, y + height );
            }
        };
    }

    /**
     * Returns a new style set which draws filled rectangles side by
     * side (one for each subset).
     *
     * @param  name  style set name
     * @return   style set
     */
    public static StyleSet sideFilled( String name ) {
        return new ColoredBarStyleSet( name ) {
            protected void drawBarShape( Graphics g, int x, int y,
                                         int width, int height,
                                         int iseq, int nseq ) {
                int gap = ( width - 2 ) / nseq;
                g.fillRect( 1 + x + iseq * gap, y, Math.max( gap, 1 ), height );
            }
        };
    }

    /**
     * Returns a new style set which draws 3d filled rectangles side by
     * side (one for each subset).
     *
     * @param  name  style set name
     * @return   style set
     */
    public static StyleSet sideFilled3d( String name ) {
        return new ColoredBarStyleSet( name ) {
            protected void drawBarShape( Graphics g, int x, int y,
                                         int width, int height,
                                         int iseq, int nseq ) {
                int gap = ( width - 2 ) / nseq;
                g.fill3DRect( 1 + x + iseq * gap, y,
                              Math.max( gap, 1 ), height, true );
            }
        };
    }

    /**
     * Returns a new style set which draws open rectangles side by
     * side (one for each subset).
     *
     * @param  name  style set name
     * @return   style set
     */
    public static StyleSet sideOpen( String name ) {
        return new ColoredBarStyleSet( name ) {
            protected void drawBarShape( Graphics g, int x, int y,
                                         int width, int height,
                                         int iseq, int nseq ) {
                int gap = ( width - 2 ) / nseq;
                g.drawRect( 1 + x + iseq * gap, y,
                            Math.max( gap - 1, 1 ), height );
            }
        };
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
        final int barWidth = nSet * 5;
        final int xPad = barWidth;
        return new Icon() {
            public int getIconHeight() {
                return height;
            }
            public int getIconWidth() {
                return nBar * barWidth + xPad;
            }
            public void paintIcon( Component c, Graphics g,
                                   int xoff, int yoff ) {
                for ( int iset = 0; iset < nSet; iset++ ) {
                    for ( int ibar = 0; ibar < nBar; ibar++ ) {
                        int x = barWidth * ibar;
                        int lastDatum = (int) data[ iset ][ ibar ];
                        int datum = (int) data[ iset ][ ibar + 1 ];
                        int nextDatum = (int) data[ iset ][ ibar + 2 ];
                        int y = height - datum;
                        BarStyle style = (BarStyle) styleSet.getStyle( iset );
                        int xlo = xPad + ibar * barWidth;
                        int xhi = xlo + barWidth;
                        int ylo = height - datum;
                        int lastYlo = height - lastDatum;
                        int nextYlo = height - nextDatum;
                        int yhi = height;
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
}
