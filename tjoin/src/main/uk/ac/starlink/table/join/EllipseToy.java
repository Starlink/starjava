package uk.ac.starlink.table.join;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Formatter;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.event.MouseInputAdapter;

/**
 * Provides an interactive graphical representation of the Ellipse Matching
 * algorithms used in this package.
 * Executing the {@link #main} method of this class posts a window with
 * two ellipses whose position, shape and orientation can be dragged around
 * using the mouse, and displays the match status that the ellipse
 * matching algorithms calculate for them.  This provides a sanity check
 * that the algorithms are working as they should do.
 *
 * @author   Mark Taylor
 * @since    30 Aug 2011
 */
public class EllipseToy extends JComponent {

    private static Stroke DASHES =
        new BasicStroke( 1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND,
                         1f, new float[] { 3, 3 }, 0f );

    private static void setAntialias( Graphics2D g2, boolean on ) {
        g2.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
                             on ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                                : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF );
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                             on ? RenderingHints.VALUE_ANTIALIAS_ON
                                : RenderingHints.VALUE_ANTIALIAS_OFF );
    }

    /**
     * Component which shows ellipses on a Cartesian plane.
     */
    public static class CartesianEllipseToy extends JComponent {
        private final boolean recogniseCircles_;
        private final Ellipse e1_;
        private final Ellipse e2_;

        /**
         * Constructor.
         *
         * @param  recogniseCircles  whether to take short cuts in the
         *                           calculations for circular ellipses
         */
        @SuppressWarnings("this-escape")
        public CartesianEllipseToy( boolean recogniseCircles ) {
            recogniseCircles_ = recogniseCircles;
            setPreferredSize( new Dimension( 400, 400 ) );
            e1_ = new Ellipse( 100, 100, 60, 90, 0 );
            e2_ = new Ellipse( 200, 50, 70, 40, Math.PI / 6. );
            Dragger dragger = new Dragger( new Ellipse[] { e1_, e2_ }, this );
            addMouseListener( dragger );
            addMouseMotionListener( dragger );
        }

        @Override
        protected void paintComponent( Graphics g ) {
            Graphics2D g2 = (Graphics2D) g.create();
            setAntialias( g2, true );
            paintEllipse( g2, e1_ );
            paintEllipse( g2, e2_ );
            EllipseCartesianMatchEngine.Match match =
                EllipseCartesianMatchEngine.getMatch( adaptEllipse( e1_ ),
                                                      adaptEllipse( e2_ ),
                                                      recogniseCircles_ );
            if ( match != null ) {
                g2.setColor( Color.RED );
                g2.drawString( new Formatter()
                              .format( "%4.2f", match.score_ ).toString(),
                               ( e1_.x_ + e2_.x_ ) / 2,
                               ( e1_.y_ + e2_.y_ ) / 2 );
                Point p1 = Double.isNaN( match.x1_ + match.y1_ )
                         ? null
                         : new Point( (int) match.x1_, (int) match.y1_ );
                Point p2 = Double.isNaN( match.x2_ + match.y2_ )
                         ? null
                         : new Point( (int) match.x2_, (int) match.y2_ );
                if ( p1 != null ) {
                    g2.drawLine( e1_.x_, e1_.y_, p1.x, p1.y );
                }
                if ( ! Double.isNaN( match.x2_ + match.y2_ ) ) {
                    g2.drawLine( e2_.x_, e2_.y_, p2.x, p2.y );
                }
                if ( p1 != null && p2 != null ) {
                    Stroke str = g2.getStroke();
                    g2.setStroke( DASHES );
                    g2.drawLine( p1.x, p1.y, p2.x, p2.y );
                    g2.setStroke( str );
                }
            }
        }

        /**
         * Paints a single ellipse.
         */
        private void paintEllipse( Graphics g, Ellipse e ) {
            Graphics2D g2 = (Graphics2D) g.create();
            int thDeg = (int) ( e.theta_ * 180. / Math.PI );
            g2.setFont( g2.getFont().deriveFont( 10f ) );
            g2.translate( e.x_, e.y_ );
            Color c = g2.getColor();
            g2.setColor( new Color( 0xd0d0d0 ) );
            g2.fillArc( -20, -20, 40, 40, 0, thDeg );
            g2.setColor( c );
            g2.drawString( "" + thDeg, 0, 0 );
            g2.rotate( - e.theta_ );
            g2.drawLine( -e.a_, 0, e.a_, 0 );
            g2.drawLine( 0, -e.b_, 0, e.b_ );
            g2.drawOval( -e.a_, -e.b_, 2 * e.a_, 2 * e.b_ );
            g2.drawString( "" + e.a_, e.a_ / 2, 0 );
            g2.rotate( Math.PI / 2. );
            g2.drawString( "" + e.b_, e.b_ / 2, 0 );
            g2.rotate( - Math.PI / 2. );
        }

        /**
         * Turns an ellipse obtained from the GUI into a Cartesian ellipse
         * suitable for use by the matching algorithm.
         */
        private EllipseCartesianMatchEngine.Ellipse adaptEllipse( Ellipse e ) {
            return new EllipseCartesianMatchEngine
                      .Ellipse( e.x_, e.y_, e.a_, e.b_, e.theta_ );
        }
    }

    /**
     * Component which shows ellipses on a spherical surface.
     * The display is on a cylindrical (plate carree) projection.
     */
    public static class SkyEllipseToy extends JComponent {
        private final boolean recogniseCircles_;
        private final Ellipse e1_;
        private final Ellipse e2_;

        /**
         * Constructor.
         *
         * @param  recogniseCircles  whether to take short cuts in the
         *                           calculations for circular ellipses
         */
        @SuppressWarnings("this-escape")
        public SkyEllipseToy( boolean recogniseCircles ) {
            recogniseCircles_ = recogniseCircles;
            int qp = 180;
            setPreferredSize( new Dimension( qp * 4, qp * 2 ) );
            e1_ = new Ellipse( qp, qp, (int) (qp * .15), (int) (qp * .1),
                               Math.PI / 2 );
            e2_ = new Ellipse( (int) (qp * 1.5), (int) (qp * 1.1),
                               (int) (qp * 0.08), (int) (qp * 0.06),
                               Math.PI / 6 );
            Dragger dragger = new Dragger( new Ellipse[] { e1_, e2_ }, this ) {
                public void mouseDragged( MouseEvent evt ) {
                    super.mouseDragged( evt );
                    int qp = getQuarterPixelCount();
                    normalizeEllipse( e1_, qp );
                    normalizeEllipse( e2_, qp );
                }
            };
            addMouseListener( dragger );
            addMouseMotionListener( dragger );
        }

        @Override
        protected void paintComponent( Graphics g ) {
            int qp = getQuarterPixelCount();
            Graphics2D g2 = (Graphics2D) g.create();
            paintSurface( g2, qp, e1_, e2_ );
            g2.drawRect( 0, 0, qp * 4, qp * 2 );
            g2.setColor( new Color( 0x808080 ) );
            g2.drawLine( 0, qp, qp * 4, qp );
            g2.setColor( Color.BLACK );
            setAntialias( g2, true );
            paintEllipse( g2, e1_, qp );
            paintEllipse( g2, e2_, qp );
            EllipseSkyMatchEngine.Match match =
                EllipseSkyMatchEngine.getMatch( adaptEllipse( e1_, qp ),
                                                adaptEllipse( e2_, qp ), true );
            if ( match != null ) {
                g2.setColor( Color.RED );
                g2.drawString( new Formatter()
                              .format( "%4.2f", match.score_ ).toString(),
                               Math.max( e1_.x_ + Math.max( e1_.a_, e1_.b_ ),
                                         e2_.x_ + Math.max( e2_.a_, e2_.b_ ) ),
                               Math.max( e1_.y_ + Math.max( e1_.a_, e1_.b_ ),
                                         e2_.y_ + Math.max( e2_.a_, e2_.b_ ) ));
                Point p1 = Double.isNaN( match.alpha1_ + match.delta1_ )
                         ? null
                         : new Point( alphaToX( match.alpha1_, qp ),
                                      deltaToY( match.delta1_, qp ) );
                Point p2 = Double.isNaN( match.alpha2_ + match.delta2_ )
                         ? null
                         : new Point( alphaToX( match.alpha2_, qp ),
                                      deltaToY( match.delta2_, qp ) );
                if ( p1 != null ) {
                    g2.drawLine( e1_.x_, e1_.y_, p1.x, p1.y );
                }
                if ( p2 != null ) {
                    g2.drawLine( e2_.x_, e2_.y_, p2.x, p2.y );
                }
                if ( p1 != null && p2 != null ) {
                    Stroke str = g2.getStroke();
                    g2.setStroke( DASHES );
                    g2.drawLine( p1.x, p1.y, p2.x, p2.y );
                    g2.setStroke( str );
                }
            }
            paintProjections( g2, qp );
        }

        /**
         * Returns the number of pixels along a line of latitude or longitude
         * corresponding to a quarter revolution (pi/2).
         *
         * @return  pixels per 90 degrees
         */
        private int getQuarterPixelCount() {
            Rectangle bounds = getBounds();
            return Math.min( bounds.width / 4, bounds.height / 2 );
        }

        /**
         * Paints an ellipse.
         */
        private void paintEllipse( Graphics g, Ellipse e, int qp ) {
            Graphics2D g2 = (Graphics2D) g.create();
            EllipseSkyMatchEngine.SkyEllipse se = adaptEllipse( e, qp );
            g2.setFont( g2.getFont().deriveFont( 8f ) );
            g2.translate( e.x_, e.y_ );
            g2.drawString( "(" + (int) (se.alpha_ * 180 / Math.PI)
                         + "," + (int) (se.delta_ * 180 / Math.PI) + ")",
                          e.a_, e.b_ );
            g2.rotate( - e.theta_ );
            g2.drawLine( -e.a_, 0, e.a_, 0 );
            g2.drawLine( 0, -e.b_, 0, e.b_ );
            g2.drawOval( -e.a_, -e.b_, 2 * e.a_, 2 * e.b_ );

            g2 = (Graphics2D) g.create();
            g2.setFont( g2.getFont().deriveFont( 10f ) );
            g2.setColor( new Color( 0x404040 ) );
            g2.translate( e.x_, e.y_ );
            int paDeg = (int) ( thetaToZeta( e.theta_ ) / Math.PI * 180 );
            g2.drawArc( -20, -20, 40, 40, 90, - paDeg );
            g2.drawString( "" + paDeg, 0, 0 );
        }

        /**
         * Paints the Cartesian projections of the two ellipses over the
         * ellipses themselves.  These will diverge increasingly from the
         * original ellipses as the latitude increases away from the equator.
         */
        private void paintProjections( Graphics g, int qp ) {
            Graphics2D g2 = (Graphics2D) g.create();
            EllipseSkyMatchEngine.SkyEllipse se1 = adaptEllipse( e1_, qp );
            EllipseSkyMatchEngine.SkyEllipse se2 = adaptEllipse( e2_, qp );
            double[] pt =
                EllipseSkyMatchEngine.bisect( se1.alpha_, se1.delta_,
                                              se2.alpha_, se2.delta_ );
            EllipseSkyMatchEngine.Projector projector =
                new EllipseSkyMatchEngine.Projector( pt[ 0 ], pt[ 1 ] );
            EllipseCartesianMatchEngine.Ellipse ce1 = se1.project( projector );
            EllipseCartesianMatchEngine.Ellipse ce2 = se2.project( projector );
            g2.translate( ( e1_.x_ + e2_.x_ ) / 2, ( e1_.y_ + e2_.y_ ) / 2 );
            paintCartesianEllipse( g2, ce1, qp );
            paintCartesianEllipse( g2, ce2, qp );
        }

        /**
         * Paints a cartesian ellipse that has dimensions in radians 
         * onto the spherical projection.
         */
        private void
                paintCartesianEllipse( Graphics g,
                                       EllipseCartesianMatchEngine.Ellipse ce,
                                       int qp ) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor( Color.GREEN );
            double f = 2 * qp / Math.PI;
            int x = (int) ( ce.x_ * f );
            int y = (int) ( ce.y_ * f );
            int a = (int) ( ce.a_ * f );
            int b = (int) ( ce.b_ * f );
            double theta = ce.theta_;
            g2.translate( x, -y );
            g2.rotate( theta );
            g2.drawOval( -a, -b, 2 * a, 2 * b );
        }

        /**
         * Converts graphical X coordinate to spherical RA coordinate.
         */
        private double xToAlpha( int x, int qp ) {
            double alpha = x * Math.PI / 2 / qp;
            return alpha;
        }

        /**
         * Converts graphical Y coordinate to spherical Dec coordinate.
         */
        private double yToDelta( int y, int qp ) {
            double delta = ( qp - y ) * Math.PI / 2 / qp;
            return delta;
        }

        /**
         * Converts graphical orientation angle (positive from horizontal)
         * to position angle.
         */
        private double thetaToZeta( double theta ) {
            double zeta = - theta + 0.5 * Math.PI;
            zeta = ( ( zeta + 2 * Math.PI ) % ( 2 * Math.PI ) ) - Math.PI;
            return zeta;
        }

        /**
         * Converts spherical RA coordinate to graphical X coordinate.
         */
        private int alphaToX( double alpha, int qp ) {
            return (int) ( alpha / Math.PI * 2 * qp );
        }

        /**
         * Converts spherical Dec coordinate to graphical Y coordinate.
         */
        private int deltaToY( double delta, int qp ) {
            return (int) ( - delta / Math.PI * 2 * qp ) + qp;
        }

        /**
         * Normalizes ellipse coordinates to standard intervals.
         */
        private void normalizeEllipse( Ellipse e, int qp ) {
            if ( e.y_ < 0 ) {
                e.y_ = - e.y_;
                e.x_ += qp * 2;
            }
            if ( e.y_ > qp * 2 ) {
                e.y_ = qp * 4 - e.y_;
                e.x_ -= qp * 2;
            }
            e.x_ = ( e.x_ + qp * 4 ) % ( qp * 4 );
        }

        /**
         * Converts a graphical ellipse to a spherical one suitable for
         * the matching algorithm.
         */
        private EllipseSkyMatchEngine.SkyEllipse adaptEllipse( Ellipse e,
                                                               int qp ) {
            double alpha = xToAlpha( e.x_, qp );
            double delta = yToDelta( e.y_, qp );
            double mu = e.a_ * Math.PI / 2 / qp;
            double nu = e.b_ * Math.PI / 2 / qp;
            double zeta = thetaToZeta( e.theta_ );
            return EllipseSkyMatchEngine
                  .createSkyEllipse( alpha, delta, mu, nu, zeta,
                                     recogniseCircles_ );
        }

        /**
         * Paints each pixel of the projected sky according to its distance
         * from the centre of one or both of the ellipses.
         * Unlike the drawn ellipses, this remains correct near the poles.
         */
        private void paintSurface( Graphics g, int qp,
                                   Ellipse e1, Ellipse e2 ) {
            Graphics2D g2 = (Graphics2D) g.create();
            setAntialias( g2, false );
            EllipseSkyMatchEngine.SkyEllipse se1 = adaptEllipse( e1, qp );
            EllipseSkyMatchEngine.SkyEllipse se2 = adaptEllipse( e2, qp );
            Rectangle bounds = getBounds();
            int xmax = bounds.x + 4 * qp;
            int ymax = bounds.y + 2 * qp;
            int step = 4;
            for ( int y = bounds.y; y < ymax; y += step ) {
                double delta = yToDelta( y, qp );
                for ( int x = bounds.x; x < xmax; x += step ) {
                    double alpha = xToAlpha( x, qp );
                    double d1 = se1.getScaledDistance( alpha, delta );
                    double d2 = se2.getScaledDistance( alpha, delta );
                    double c1 = d1 < 1 ? d1 * 0.8 : 1;
                    double c2 = d2 < 1 ? d2 * 0.8 : 1;
                    float lev = (float) ( ( c1 * c2 ) * 0.6 + 0.4 );
                    Color c = new Color( lev, lev, 1f );
                    g2.setColor( c );
                    g2.fillRect( x, y, step, step );
                }
            }
        }
    }

    /**
     * Class which can interpret user mouse actions to drag the position and
     * dimensions of an ellipse around the screen.
     */
    private static class Dragger extends MouseInputAdapter {

        private final Ellipse[] ellipses_;
        private final JComponent comp_;
        private Point p0_;
        private Ellipse e0_;
        private Changer changer_;
        private static final Cursor defaultCursor_ = Cursor.getDefaultCursor();
        private static final Cursor moveCursor_ =
            Cursor.getPredefinedCursor( Cursor.MOVE_CURSOR );
        private static final Cursor targetCursor_ =
            Cursor.getPredefinedCursor( Cursor.HAND_CURSOR );

        /**
         * Constructor.
         *
         * @param  ellipses  ellipses that will be draggable
         * @param  comp   component in which this works
         */
        Dragger( Ellipse[] ellipses, JComponent comp ) {
            ellipses_ = ellipses;
            comp_ = comp;
        }

        @Override
        public void mousePressed( MouseEvent evt ) {
            Changer changer = getChanger( evt );
            if ( changer != null ) {
                changer_ = changer;
                p0_ = evt.getPoint();
                e0_ = new Ellipse( changer.ellipse_ );
                comp_.setCursor( moveCursor_ );
            }
        }

        @Override
        public void mouseDragged( MouseEvent evt ) {
            if ( changer_ != null ) {
                changer_.submitChange( evt.getPoint() );
                comp_.repaint( comp_.getBounds() );
            }
        }

        @Override
        public void mouseMoved( MouseEvent evt ) {
            Changer changer = getChanger( evt );
            comp_.setCursor( changer == null ? defaultCursor_ : targetCursor_ );
        }

        @Override
        public void mouseReleased( MouseEvent evt ) {
            changer_ = null;
            p0_ = null;
            e0_ = null;
            comp_.setCursor( defaultCursor_ );
        }

        @Override
        public void mouseClicked( MouseEvent evt ) {
            Changer changer = getChanger( evt );
            int nclick = evt.getClickCount();
            if ( changer != null && nclick > 1 ) {
                changer.multiClick( nclick );
                comp_.repaint( comp_.getBounds() );
            }
        }

        /**
         * Returns the changer, if any, appropriate for the position
         * in a mouse event.
         *
         * @param  evt  event
         * @return   indiated changer or null
         */
        private Changer getChanger( MouseEvent evt ) {
            Point p = evt.getPoint();
            for ( int ie = 0; ie < ellipses_.length; ie++ ) {
                Changer changer = ellipses_[ ie ].getChanger( p, 4 );
                if ( changer != null ) {
                    return changer;
                }
            }
            return null;
        }
    }

    /**
     * Private interface for object which can modify an ellipse based on
     * an updated position.
     */
    private static abstract class Changer {

        private final Ellipse ellipse_;

        Changer( Ellipse ellipse ) {
            ellipse_ = ellipse;
        }

        /**
         * Change the state of some object according to a given screen position.
         *
         * @param  p1  updated position
         */
        abstract void submitChange( Point p1 );

        /**
         * Change the state of some object given a multiClick event.
         *
         * @param  nclick  number of clicks
         */
        abstract void multiClick( int nclick );
    }

    /**
     * Represents an ellipse displayed on the screen.
     */
    private static class Ellipse {

        /** X coordinate of centre. */
        int x_;

        /** Y coordinate of centre. */
        int y_;

        /** Major radius in pixels. */
        int a_;

        /** Minor radius in pixels. */
        int b_;

        /** Orientation from positive screen X axis to semi-major axis,
         *  towards positive screen Y axis, in radians. */
        double theta_;

        /**
         * Constructor from coordinates.
         *
         * @param  x  X coordinate of centre
         * @param  y  Y coordinate of centre
         * @param  a  major radius
         * @param  b  minor radius
         * @param  theta  orientation in radians
         */
        Ellipse( int x, int y, int a, int b, double theta ) {
            x_ = x;
            y_ = y;
            a_ = a;
            b_ = b;
            theta_ = theta;
        }

        /**
         * Copy constructor.
         */
        Ellipse( Ellipse e ) {
            this( e.x_, e.y_, e.a_, e.b_, e.theta_ );
        }

        public String toString() {
            return new Formatter()
                  .format( "a=%3d b=%3d x=%3d y=%3d theta=%3d",
                           a_, b_, x_, y_, (int) (theta_ * 180. / Math.PI) )
                  .toString();
        }

        /**
         * Returns a changer object associated with a particular screen
         * position, if any.
         *
         * @param   p   screen position
         * @param  tol  tolerance in pixels
         * @return   changer, or null if not near any active point
         */
        public Changer getChanger( Point p, int tol ) {
            final Point p0 = p;
            final Ellipse e0 = new Ellipse( this );
            final double ct = Math.cos( theta_ );
            final double st = Math.sin( theta_ );
            Point center = new Point( x_, y_ );
            Point aPlus =
                new Point( (int) ( x_ + a_ * ct ), (int) ( y_ - a_ * st ) );
            Point bPlus =
                new Point( (int) ( x_ + b_ * st ), (int) ( y_ + b_ * ct ) );
            Point aMinus =
                new Point( (int) ( x_ - a_ * ct ), (int) ( y_ + a_ * st ) );
            Point bMinus =
                new Point( (int) ( x_ - b_ * st ), (int) ( y_ - b_ * ct ) );
            boolean isPoint = a_ == 0 && b_ == 0;
            if ( isClose( p, aPlus, tol ) && ! isPoint ) {
                return new Changer( this ) {
                    public void submitChange( Point p1 ) {
                        a_ = e0.a_ + (int) ( ( p1.x - p0.x ) * ct -
                                             ( p1.y - p0.y ) * st );
                    }
                    public void multiClick( int nclick ) {
                    }
                };
            }
            else if ( isClose( p, bPlus, tol ) && ! isPoint ) {
                return new Changer( this ) {
                    public void submitChange( Point p1 ) {
                        b_ = e0.b_ + (int) ( + ( p1.x - p0.x ) * st
                                             + ( p1.y - p0.y ) * ct );
                    }
                    public void multiClick( int nclick ) {
                    }
                };
            }
            else if ( isClose( p, center, tol ) ) {
                return new Changer( this ) {
                    public void submitChange( Point p1 ) {
                        x_ = e0.x_ + p1.x - p0.x;
                        y_ = e0.y_ + p1.y - p0.y;
                    }
                    public void multiClick( int nclick ) {
                        if ( nclick == 2 ) {
                            int maxr = Math.max( a_, b_ );
                            a_ = maxr;
                            b_ = maxr;
                        }
                    }
                };
            }
            else if ( isClose( p, aMinus, tol ) ||
                      isClose( p, bMinus, tol ) ) {
                return new Changer( this ) {
                    public void submitChange( Point p1 ) {
                        theta_ = e0.theta_
                               - Math.atan2( p1.y - e0.y_, p1.x - e0.x_ )
                               + Math.atan2( p0.y - e0.y_, p0.x - e0.x_ );
                        theta_ = ( theta_ + 2 * Math.PI ) % ( 2 * Math.PI );
                    }
                    public void multiClick( int nclick ) {
                    }
                };
            }
            else {
                return null;
            }
        }

        /**
         * Indicates if two screen points are close to each other.
         *
         * @param  p1  point 1
         * @param  p2  point 2
         * @param  tol   tolerance in pixels
         * @return  true iff p1 and p2 are as close as <code>tol</code>
         */
        boolean isClose( Point p1, Point p2, int tol ) {
            return ( ( p1.x - p2.x ) * ( p1.x - p2.x ) +
                     ( p1.y - p2.y ) * ( p1.y - p2.y ) ) <= tol * tol;
        }
    }

    /**
     * Main method.  Use "-h" flag for help.
     */
    public static void main( String[] args ) {
        String usage = "Usage: " + EllipseToy.class.getName()
                     + " [-sky]"
                     + " [-[no]circles]";
        JFrame frame = new JFrame();
        List<String> argList = new ArrayList<String>( Arrays.asList( args ) );
        boolean sky = false;
        boolean recogniseCircles = true;
        for ( Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String arg = it.next();
            if ( arg.equals( "-sky" ) ) {
                it.remove();
                sky = true;
            }
            else if ( arg.startsWith( "-circ" ) ) {
                it.remove();
                recogniseCircles = true;
            }
            else if ( arg.startsWith( "-nocirc" ) ) {
                it.remove();
                recogniseCircles = false;
            }
            else if ( arg.startsWith( "-h" ) ) {
                System.out.println( usage );
                return;
            }
            else {
                System.err.println( usage );
                System.exit( 1 );
            }
        }
        if ( ! argList.isEmpty() ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        
        frame.add( sky ? new SkyEllipseToy( recogniseCircles )
                       : new CartesianEllipseToy( recogniseCircles ) );
        frame.pack();
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setVisible( true );
    }
}
