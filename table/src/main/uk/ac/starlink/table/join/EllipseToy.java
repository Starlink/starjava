package uk.ac.starlink.table.join;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.util.Formatter;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.event.MouseInputAdapter;

public class EllipseToy extends JComponent {

    private final Ellipse e1_;
    private final Ellipse e2_;

    public EllipseToy() {
        setPreferredSize( new Dimension( 400, 400 ) );
        e1_ = new Ellipse( 100, 100, 60, 90, 0 );
        e2_ = new Ellipse( 200, 50, 70, 40, Math.PI / 6. );
        Dragger dragger = new Dragger( new Ellipse[] { e1_, e2_ }, this );
        addMouseListener( dragger );
        addMouseMotionListener( dragger );
    }

    protected void paintComponent( Graphics g ) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
                             RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON );
        paintEllipse( g2, e1_ );
        paintEllipse( g2, e2_ );
        EllipseMatchEngine.Match match =
            EllipseMatchEngine.getMatch( adaptEllipse( e1_ ),
                                         adaptEllipse( e2_ ) );
        if ( match != null ) {
            g2.setColor( Color.RED );
            g2.drawString( "" + new Formatter().format( "%4.2f", match.score_ ),
                           ( e1_.x_ + e2_.x_ ) / 2, ( e1_.y_ + e2_.y_ ) / 2 );
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
                g2.setStroke( new BasicStroke( 1f, BasicStroke.CAP_SQUARE,
                                               BasicStroke.JOIN_ROUND, 1f,
                                               new float[] { 3, 3 }, 0f ) );
                g2.drawLine( p1.x, p1.y, p2.x, p2.y );
                g2.setStroke( str );
            }
        }
    }

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

    private static EllipseMatchEngine.Ellipse adaptEllipse( Ellipse e ) {
        return new EllipseMatchEngine.Ellipse( toTuple( e ) );
    }

    private static Object[] toTuple( Ellipse e ) {
        return new Object[] {
            new Integer( e.x_ ),
            new Integer( e.y_ ),
            new Integer( e.a_ ),
            new Integer( e.b_ ),
            new Double( e.theta_ ),
        };
    }

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

        Dragger( Ellipse[] ellipses, JComponent comp ) {
            ellipses_ = ellipses;
            comp_ = comp;
        }

        public void mousePressed( MouseEvent evt ) {
            Point p = evt.getPoint();
            for ( int ie = 0; ie < ellipses_.length; ie++ ) {
                Changer changer = ellipses_[ ie ].getChanger( p, 4 );
                if ( changer != null ) {
                    changer_ = changer;
                    p0_ = p;
                    e0_ = new Ellipse( ellipses_[ ie ] );
                    comp_.setCursor( moveCursor_ );
                    return;
                }
            }
        }

        public void mouseDragged( MouseEvent evt ) {
            if ( changer_ != null ) {
                changer_.submitChange( evt.getPoint() );
                comp_.repaint( comp_.getBounds() );
            }
        }

        public void mouseMoved( MouseEvent evt ) {
            Point p = evt.getPoint();
            Changer changer = null;
            for ( int ie = 0; changer == null && ie < ellipses_.length; ie++ ) {
                changer = ellipses_[ ie ].getChanger( p, 4 );
            }
            comp_.setCursor( changer == null ? defaultCursor_ : targetCursor_ );
        }

        public void mouseReleased( MouseEvent evt ) {
            changer_ = null;
            p0_ = null;
            e0_ = null;
            comp_.setCursor( defaultCursor_ );
        }
    }

    private static abstract class Changer {
        abstract void submitChange( Point p1 );
    }

    private static class Ellipse {
        int x_;
        int y_;
        int a_;
        int b_;
        double theta_;

        Ellipse() {
            this( 0, 0, 0, 0, 0. );
        }

        Ellipse( int x, int y, int a, int b, double theta ) {
            x_ = x;
            y_ = y;
            a_ = a;
            b_ = b;
            theta_ = theta;
        }

        Ellipse( Ellipse e ) {
            this();
            copy( e );
        }

        void copy( Ellipse e ) {
            x_ = e.x_;
            y_ = e.y_;
            a_ = e.a_;
            b_ = e.b_;
            theta_ = e.theta_;
        }

        public String toString() {
            return new Formatter()
                  .format( "a=%3d b=%3d x=%3d y=%3d theta=%3d",
                           a_, b_, x_, y_, (int) (theta_ * 180. / Math.PI) )
                  .toString();
        }

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
            if ( isClose( p, center, tol ) ) {
                return new Changer() {
                    public void submitChange( Point p1 ) {
                        x_ = e0.x_ + p1.x - p0.x;
                        y_ = e0.y_ + p1.y - p0.y;
                    }
                };
            }
            else if ( isClose( p, aPlus, tol ) ) {
                return new Changer() {
                    public void submitChange( Point p1 ) {
                        a_ = e0.a_ + (int) ( ( p1.x - p0.x ) * ct -
                                             ( p1.y - p0.y ) * st );
                    }
                };
            }
            else if ( isClose( p, bPlus, tol ) ) {
                return new Changer() {
                    public void submitChange( Point p1 ) {
                        b_ = e0.b_ + (int) ( + ( p1.x - p0.x ) * st
                                             + ( p1.y - p0.y ) * ct );
                    }
                };
            }
            else if ( isClose( p, aMinus, tol ) ||
                      isClose( p, bMinus, tol ) ) {
                return new Changer() {
                    public void submitChange( Point p1 ) {
                        theta_ = e0.theta_
                               - Math.atan2( p1.y - e0.y_, p1.x - e0.x_ )
                               + Math.atan2( p0.y - e0.y_, p0.x - e0.x_ );
                        theta_ = ( theta_ + 2 * Math.PI ) % ( 2 * Math.PI );
                    }
                };
            }
            else {
                return null;
            }
        }

        boolean isClose( Point p1, Point p2, int tol ) {
            return ( ( p1.x - p2.x ) * ( p1.x - p2.x ) +
                     ( p1.y - p2.y ) * ( p1.y - p2.y ) ) <= tol * tol;
        }
    }

    public static void main( String[] args ) {
        JFrame frame = new JFrame();
        frame.add( new EllipseToy() );
        frame.pack();
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setVisible( true );
    }
}
