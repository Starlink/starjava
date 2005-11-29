package uk.ac.starlink.topcat.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import uk.ac.starlink.topcat.RowSubset;

/**
 * Component which can display a scatter plot of points.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    17 Jun 2004
 */
public class ScatterPlot extends SurfacePlot {

    private Annotations annotations_;
    private Points lastPoints_;
    private PlotState lastState_;
    private PlotSurface lastSurface_;
    private int lastWidth_;
    private int lastHeight_;
    private Image image_;
    private XYStats[] statSets_;

    /**
     * Constructs a new scatter plot, specifying the initial plotting surface
     * which provides axis plotting and so on.
     *
     * @param  surface  plotting surface implementation
     */
    public ScatterPlot( PlotSurface surface ) {
        super();
        add( new ScatterDataPanel() );
        annotations_ = new Annotations();
        setSurface( surface );
    }
    
    public void setState( PlotState state ) {
        super.setState( state );
        annotations_.validate();
    }

    /**
     * Sets the points at the indices given by the <tt>ips</tt> array 
     * of the Points object as "active".
     * They will be marked out somehow or other when plotted.
     *
     * @param  ips  active point array
     */
    public void setActivePoints( int[] ips ) {
        annotations_.setActivePoints( ips );
    }

    /**
     * Works out the range of coordinates to accommodate all the data
     * points owned by this plot.
     *
     * @return   4-element array (xlo,ylo,xhi,yhi)
     */
    public double[] getFullDataRange() {
        boolean xlog = getState().getLogFlags()[ 0 ];
        boolean ylog = getState().getLogFlags()[ 1 ];
        double xlo = Double.POSITIVE_INFINITY;
        double xhi = xlog ? Double.MIN_VALUE : Double.NEGATIVE_INFINITY;
        double ylo = Double.POSITIVE_INFINITY;
        double yhi = ylog ? Double.MIN_VALUE : Double.NEGATIVE_INFINITY;

        /* Go through all points getting max/min values. */
        int nok = 0;
        Points points = getPoints();
        if ( points != null ) {
            RowSubset[] rsets = getPointSelection().getSubsets();
            int nrset = rsets.length;
            int np = points.getCount();
            double[] coords = new double[ 2 ];
            for ( int ip = 0; ip < np; ip++ ) {

                /* First see if this point will be plotted. */
                boolean use = false;
                long lp = (long) ip;
                for ( int is = 0; ! use && is < nrset; is++ ) {
                    use = use || rsets[ is ].isIncluded( lp );
                }
                if ( use ) {
                    points.getCoords( ip, coords );
                    double xp = coords[ 0 ];
                    double yp = coords[ 1 ];
                    if ( ! Double.isNaN( xp ) && 
                         ! Double.isNaN( yp ) &&
                         ! Double.isInfinite( xp ) && 
                         ! Double.isInfinite( yp ) &&
                         ( ! xlog || xp > 0.0 ) &&
                         ( ! ylog || yp > 0.0 ) ) {
                        nok++;
                        if ( xp < xlo ) {
                            xlo = xp;
                        }
                        if ( xp > xhi ) {
                            xhi = xp;
                        }
                        if ( yp < ylo ) {
                            ylo = yp;
                        }
                        if ( yp > yhi ) {
                            yhi = yp;
                        }
                    }
                }
            }
        }

        /* Return result. */
        return nok == 0 ? null : new double[] { xlo, ylo, xhi, yhi };
    }

    /**
     * Plots the points of this scatter plot onto a given graphics context
     * using its current plotting surface to define the mapping of data
     * to graphics space.
     *
     * @param  graphics  graphics context
     */
    private void drawData( Graphics graphics ) {
        Points points = getPoints();
        PlotState state = getState();
        PlotSurface surface = getSurface();
        if ( points == null || state == null || surface == null ) {
            return;
        }

        /* Clone the graphics context and configure the clip to correspond
         * to the plotting surface. */
        Graphics2D g; 
        g = (Graphics2D) graphics.create();
        g.setClip( getSurface().getClip() );

        /* Draw the points, optionally accumulating statistics for 
         * X-Y correlations if we're going to need to draw regression lines. */
        /* Currently, this method updates the statSets_ member variable - 
         * it's not very good practice to have a paintComponent method
         * updating data structures. */
        int np = points.getCount();
        RowSubset[] sets = getPointSelection().getSubsets();
        Style[] styles = getPointSelection().getStyles();
        int nset = sets.length;
  boolean[] regressions = new boolean[ nset ];
        statSets_ = new XYStats[ nset ];
        double[] coords = new double[ 2 ];
        for ( int is = 0; is < nset; is++ ) {
            MarkStyle style = (MarkStyle) styles[ is ];
            boolean regress = regressions[ is ];
            XYStats stats = null;
            if ( regress ) {
                stats = new XYStats( state.getLogFlags()[ 0 ],
                                     state.getLogFlags()[ 1 ] );
                statSets_[ is ] = stats;
            }
            int maxr = style.getMaximumRadius();
            int maxr2 = maxr * 2;
            for ( int ip = 0; ip < np; ip++ ) {
                if ( sets[ is ].isIncluded( (long) ip ) ) {
                    points.getCoords( ip, coords );
                    double x = coords[ 0 ];
                    double y = coords[ 1 ];
                    Point point = surface.dataToGraphics( x, y, true );
                    if ( point != null ) {
                        int xp = point.x;
                        int yp = point.y;
                        if ( g.hitClip( xp - maxr, yp - maxr, maxr2, maxr2 ) ) {
                            style.drawMarker( g, xp, yp );
                            if ( regress ) {
                                stats.addPoint( x, y );
                            }
                        }
                    }
                }
            }
        }

        /* Draw regression lines as required. */
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON );
        g.setStroke( new BasicStroke( 2, BasicStroke.CAP_ROUND,
                                      BasicStroke.JOIN_ROUND ) );
        for ( int is = 0; is < nset; is++ ) {
            XYStats stats = statSets_[ is ];
            if ( stats != null ) {
                double[] ends = stats.linearRegressionLine();
                if ( ends != null ) {
                    Point p1 = surface.dataToGraphics( ends[ 0 ], ends[ 1 ],
                                                       false );
                    Point p2 = surface.dataToGraphics( ends[ 2 ], ends[ 3 ],
                                                       false );
                    if ( p1 != null && p2 != null ) {
                        Color styleColor =
                            ((MarkStyle) styles[ is ]).getColor();
                        g.setColor( new Color( styleColor.getRed(),
                                               styleColor.getGreen(), 
                                               styleColor.getBlue(), 160 ) );
                        g.drawLine( p1.x, p1.y, p2.x, p2.y );
                    }
                }
            }
        }
    }

    /**
     * Plots the registered annotations of this scatter plot onto a given
     * graphics context using its current plotting surface to define the
     * mapping of data to graphics space.
     *
     * @param  g  graphics context
     */
    private void drawAnnotations( Graphics g ) {
        annotations_.draw( g );
    }

    /**
     * Informs the user what the coefficients are for any regression
     * lines currently plotted.
     *
     * <p>This isn't very beautifully done one way and another - there 
     * should really be a live regression line control window which gives you
     * control over what's plotted and displayed - this just gives you
     * a modal dialogue.  I'm not sure if this is going to be a very
     * widely used facility though, so I don't know if its' worth the
     * effort.  Also, to do it properly would require some reorganisation
     * of how data is distributed/calculated between ScatterPlot and
     * PlotWindow - this needs to be done in tandem with the move to
     * multiplots.
     */
    public void displayRegressionCoefficients() {
        final RowSubset[] sets = getPointSelection().getSubsets();
  boolean[] regressions = new boolean[ sets.length ];
        TableModel tmodel = new AbstractTableModel() {
        // Don't do this - the default renderers are rubbish (represent 
        // values near zero as 0.
        //  public Class getColumnClass( int icol ) {
        //      return icol == 0 ? String.class : Double.class;
        //  }
            public int getColumnCount() {
                return 4;
            }
            public int getRowCount() {
                return sets.length;
            }
            public String getColumnName( int icol ) {
                switch ( icol ) {
                    case 0: return "Subset";
                    case 1: return "Gradient";
                    case 2: return "Intercept";
                    case 3: return "Correlation";
                    default: throw new IllegalArgumentException();
                }
            }
            public Object getValueAt( int irow, int icol ) {
                if ( icol == 0 ) {
                    return sets[ irow ].getName();
                }
                else {
                    XYStats stats = statSets_[ irow ];
                    if ( stats == null ) {
                        return null;
                    }
                    else {
                        switch( icol ) {
                            case 1:
                                double m = stats.getLinearCoefficients()[ 1 ];
                                return new Float( (float) m );
                            case 2:
                                double c = stats.getLinearCoefficients()[ 0 ];
                                return new Float( (float) c );
                            case 3: 
                                double r = stats.getCorrelation();
                                return new Float( (float) r );
                            default:
                                throw new IllegalArgumentException();
                        }
                    }
                }
            }
        };
        JTable jtab = new JTable( tmodel );
        JScrollPane tscroller = new JScrollPane( jtab );
        tscroller.setPreferredSize( new Dimension( 450, 100 ) );
         
        Object[] msg = new Object[] { 
            "Coefficients for plotted regression lines",
            tscroller,
        };
        JOptionPane.showMessageDialog( this, msg, 
                                       "Linear Regression Coefficients",
                                       JOptionPane.INFORMATION_MESSAGE );
    }

    /**
     * This class takes care of all the markings plotted over the top of
     * the plot proper.  It's coded as an extra class just to make it tidy,
     * these workings could equally be in the body of ScatterPlot.
     */
    private class Annotations {

        int[] activePoints_ = new int[ 0 ];
        final MarkStyle cursorStyle_ = MarkStyle.targetStyle();

        /**
         * Sets a number of points to be marked out.
         * Any negative indices in the array, or ones which are not visible
         * in the current plot, are ignored.
         *
         * @param  ips  indices of the points to be marked
         */
        void setActivePoints( int[] ips ) {
            ips = dropInvisible( ips );
            if ( ! Arrays.equals( ips, activePoints_ ) ) {
                activePoints_ = ips;
                repaint();
            }
        }

        /**
         * Paints all the current annotations onto a given graphics context.
         *
         * @param  g  graphics context
         */
        void draw( Graphics graphics ) {
            Graphics2D g2 = (Graphics2D) graphics.create();

            /* Draw any active points. */
            for ( int i = 0; i < activePoints_.length; i++ ) {
                double[] coords = new double[ 2 ];
                getPoints().getCoords( activePoints_[ i ], coords );
                Point p = getSurface().dataToGraphics( coords[ 0 ], coords[ 1 ],
                                                       true );
                if ( p != null ) {
                    cursorStyle_.drawMarker( graphics, p.x, p.y );
                }
            }
        }

        /**
         * Updates this annotations object as appropriate for the current
         * state of the plot.
         */
        void validate() {
            /* If there are active points which are no longer visible in
             * this plot, drop them. */
            activePoints_ = getState().getValid() 
                          ? dropInvisible( activePoints_ )
                          : new int[ 0 ];
        }

        /**
         * Removes any invisible points from an array of point indices.
         *
         * @param  ips   point index array
         * @return  subset of ips
         */
        private int[] dropInvisible( int[] ips ) {
            List ipList = new ArrayList();
            for ( int i = 0; i < ips.length; i++ ) {
                int ip = ips[ i ];
                if ( ip >= 0 && isIncluded( ip ) ) {
                    ipList.add( new Integer( ip ) );
                }
            }
            ips = new int[ ipList.size() ];
            for ( int i = 0; i < ips.length; i++ ) {
                ips[ i ] = ((Integer) ipList.get( i )).intValue();
            }
            return ips;
        }
    }

    /**
     * Determines whether a point with a given index is included in the
     * current plot.  This doesn't necessarily mean it's visible, since
     * it might fall outside the bounds of the current display area,
     * but it means the point does conceptually form part of what is
     * being plotted.
     * 
     * @param  ip  index of point to check
     * @return  true  iff point <tt>ip</tt> is included in this plot
     */
    private boolean isIncluded( int ip ) {
        RowSubset[] sets = getPointSelection().getSubsets();
        int nset = sets.length;
        for ( int is = 0; is < nset; is++ ) {
            if ( sets[ is ].isIncluded( (long) ip ) ) {
                return true;
            }
        } 
        return false;
    }

    /**
     * Graphical component which does the actual plotting of the points.
     * Its basic job is to call {@link @drawData} and {@link drawAnnotations}
     * in its {@link paintComponent} method.  
     * However it makes things a bit more
     * complicated than that for the purposes of efficiency.
     */
    private class ScatterDataPanel extends JComponent {

        ScatterDataPanel() {
            setOpaque( false );
        }

        /**
         * Draws the component to a graphics context which probably 
         * represents the screen, by calling {@link #drawData}
         * and {@link #drawAnnotations}.
         * Since <tt>drawData</tt> might be fairly expensive 
         * (if there are a lot of points), and <tt>paintComponent</tt> 
         * can be called often without the data changing 
         * (e.g. cascading window uncover events) it is advantageous 
         * to cache the drawn points in an Image and effectively
         * blit it to the screen on subsequent paint requests.
         */
        protected void paintComponent( Graphics g ) {
 
            /* If the plotting requirements have changed since last time
             * we painted, we can use the cached image. */
            int width = getBounds().width;
            int height = getBounds().height;
            if ( getState() == lastState_ &&
                 getSurface() == lastSurface_ &&
                 getPoints() == lastPoints_ &&
                 width == lastWidth_ &&
                 height == lastHeight_ ) {
                assert image_ != null;
            }

            /* We need to replot the points. */
            else {

                /* Get an image to plot into.  Take care that this is an
                 * image which can take advantage of hardware acceleration
                 * (e.g. "new BufferedImage()" is no good). */
                image_ = createImage( width, height );
                Graphics ig = image_.getGraphics();

                /* Unfortunately, accelerated-graphics-capable contexts
                 * are unable to handle transparency very well.  This means
                 * that the plotting surface will not show up behind the
                 * plotted points in the cached image buffer, even though
                 * this component is supposed to be transparent. 
                 * So we fudge it by calling the plotting surface's 
                 * <tt>paintSurface</tt> method (introduced for this purpose)
                 * here.  This is not incredibly tidy. */
                getSurface().paintSurface( ig );

                /* Plot the actual points into the cached buffer. */
                drawData( ig );

                /* Record the state which corresponds to the most recent
                 * plot into the cached buffer. */
                lastState_ = getState();
                lastSurface_ = getSurface();
                lastPoints_ = getPoints();
                lastWidth_ = width;
                lastHeight_ = height;
            }

            /* Copy the image from the (new or old) cached buffer onto
             * the graphics context we are being asked to paint into. */
            boolean done = g.drawImage( image_, 0, 0, null );
            assert done;
            drawAnnotations( g );
        }

        /**
         * The normal implementation for this method calls 
         * <tt>paintComponent</tt> somewhere along the way.
         * However, if we are painting the component 
         * for some reason other than posting it
         * on the screen, we don't want to use the cached-image approach
         * used in <tt>paintComponent</tt>, since it might be a 
         * non-pixelised graphics context (e.g. postscript).
         * So for <tt>printComponent</tt>, just invoke <tt>drawData</tt>
         * straightforwardly.
         *
         * <p>A more respectable way to do this might be to test the
         * <tt>Graphics2D.getDeviceConfiguration().getDevice().getType()</tt>
         * value in <tt>paintComponent</tt> - however, for at least one
         * known printer-type graphics context 
         * (<tt>org.jibble.epsgraphics.EpsGraphics2D</tt>) this returns
         * the wrong value (TYPE_IMAGE_BUFFER not TYPE_PRINTER).
         */
        protected void printComponent( Graphics g ) {
            if ( getPoints() != null && getState() != null ) {
                drawData( g );
                drawAnnotations( g );
            }
        }
    }

}
