package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.JComponent;
import uk.ac.starlink.pal.Pal;

/**
 * Graphics window for viewing 3D scatter plots.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2005
 */
public class Plot3DWindow extends GraphicsWindow {

    private final Plot3D plot_;
    private double[] rotation_;

    /**
     * Constructs a new window.
     *
     * @param   parent  parent component (may be used for postioning)
     */
    public Plot3DWindow( Component parent ) {
        super( "3D", new String[] { "X", "Y", "Z" }, parent );

        /* Construct and place the component which actually displays
         * the 3D data. */
        plot_ = new Plot3D();
        getMainArea().add( plot_, BorderLayout.CENTER );

        /* Arrange that mouse dragging on the plot component will rotate
         * the view. */
        plot_.addMouseMotionListener( new DragListener() );

        /* Add standard toolbar items. */
        addHelp( "Plot3DWindow" );
        setRotation( Plot3DState.UNIT_MATRIX );
        replot();

        /* Make visible. */
        pack();
        setVisible( true );
    }

    /**
     * Sets the viewing angle.
     *
     * @param   matrix  9-element array giving rotation of data space
     */
    public void setRotation( double[] matrix ) {
        rotation_ = (double[]) matrix.clone();
    }

    protected JComponent getPlot() {
        return plot_;
    }

    protected PlotState createPlotState() {
        return new Plot3DState();
    }

    public PlotState getPlotState() {
        Plot3DState state = (Plot3DState) super.getPlotState();

        /* Reset the view angle if the axes have changed.  This is probably
         * what you want, but might not be? */
        if ( ! state.sameAxes( plot_.getState() ) ) {
            setRotation( Plot3DState.UNIT_MATRIX );
        }

        /* Configure the state with this window's current viewing angles. */
        state.setRotation( rotation_ );

        /* Return. */
        return state;
    }

    public StyleSet getDefaultStyles( int npoint ) {
        if ( npoint > 20000 ) {
            return PlotWindow.STYLE_SETS[ 0 ];
        }
        else if ( npoint > 2000 ) {
            return PlotWindow.STYLE_SETS[ 1 ];
        }
        else if ( npoint > 200 ) {
            return PlotWindow.STYLE_SETS[ 2 ];
        }
        else if ( npoint > 20 ) {
            return PlotWindow.STYLE_SETS[ 3 ];
        }
        else if ( npoint >= 1 ) {
            return PlotWindow.STYLE_SETS[ 4 ];
        }
        else {
            return PlotWindow.STYLE_SETS[ 1 ];
        }
    }

    protected void doReplot( PlotState state, Points points ) {
        PlotState lastState = plot_.getState();
        plot_.setPoints( points );
        plot_.setState( (Plot3DState) state );
        if ( ! state.sameAxes( lastState ) || ! state.sameData( lastState ) ) {
            if ( state.getValid() ) {
                plot_.rescale();
            }
        }
        plot_.repaint();
    }

    /**
     * Multiplies two 3x3 matrices together.  The inputs and outputs
     * are 9-element double arrays.
     *
     * @param   a  input matrix 1
     * @param   b  input matrix 2
     * @return  a * b.
     */
    private static double[] matrixMultiply( double[] a, double[] b ) {
        double[] r = new double[ 9 ];
        for ( int i = 0; i < 3; i++ ) {
            for ( int j = 0; j < 3; j++ ) {
                for ( int k = 0; k < 3; k++ ) {
                    r[ 3 * i + j ] += a[ 3 * i + k ] * b[ j + 3 * k ];
                }
            }
        }
        return r;
    }

    /**
     * Takes a view rotation matrix and adds to it the effect of rotations
     * about X and Y directions.
     *
     * @param   base  9-element array giving initial view rotation matrix
     * @param   phi   angle to rotate around Y axis
     * @param   psi   angle to rotate around X axis
     * @return  9-element array giving combined rotation matrix
     */
    private static double[] rotateXY( double[] base, double phi, double psi ) {
        double[] rotX = rotate( base, new double[] { 0., 1., 0. }, phi );
        double[] rotY = rotate( base, new double[] { 1., 0., 0. }, psi );
        return matrixMultiply( matrixMultiply( base, rotX ), rotY );
    }

    /**
     * Calculates a rotation matrix for rotating around a screen axis
     * by a given angle.  Note this axis is in the view space, not the
     * data space.
     * 
     * @param   base  rotation matrix defining the view orientation
     *                (9-element array)
     * @param   screenAxis  axis in view space about which rotation is required
     *                      (3-element array)
     * @param   angle   rotation angle in radians
     */
    private static double[] rotate( double[] base, double[] screenAxis,
                                    double angle ) {
        Pal pal = new Pal();
        double[][] pBase = toPal( base );

        /* Calculate the data space vectors corresponding to the screen axis. */
        double[] axis = pal.Dimxv( pBase, screenAxis );

        /* Give it a magnitude corresponding to the rotation angle. */
        double factor = angle / Math.sqrt( axis[ 0 ] * axis[ 0 ] +
                                           axis[ 1 ] * axis[ 1 ] +
                                           axis[ 2 ] * axis[ 2 ] );
        for ( int i = 0; i < 3; i++ ) {
            axis[ i ] *= factor;
        }

        /* Calculate and return the rotation matrix. */
        double[][] pRot = pal.Dav2m( axis );
        return fromPal( pRot );
    }

    /**
     * Converts a 3-d matrix from Pal-friendly form (3x3) to the form used
     * elsewhere in this class (flat 9-element array).
     *
     * @param   m   flat matrix
     * @return  pal-friendly matrix
     */
    private static double[] fromPal( double[][] m ) {
        return new double[] {
            m[0][0], m[0][1], m[0][2],
            m[1][0], m[1][1], m[1][2],
            m[2][0], m[2][1], m[2][2],
        };
    }

    /**
     * Converts a 3-d matrix from the form used in this class 
     * (flat 9-element array) to Pal-friendly form (3x3).
     *
     * @param   m  flat matrix
     * @return  pal-friendly matrix
     */
    private static double[][] toPal( double[] m ) {
        return new double[][] {
            { m[0], m[1], m[2], },
            { m[3], m[4], m[5], },
            { m[6], m[7], m[8], },
        };
    }

    /**
     * Listener which interprets drag gestures on the plotting surface 
     * as requests to rotate the viewing angles.
     */
    private class DragListener implements MouseMotionListener {

        private Point posBase_;
        private double[] rotBase_;

        public void mouseDragged( MouseEvent evt ) {
            Point pos = evt.getPoint(); 
            if ( posBase_ == null ) {
                posBase_ = pos;
                rotBase_ = Plot3DWindow.this.rotation_;
            }
            else {

                /* Work out the amounts by which the user wants to rotate
                 * in the 'horizontal' and 'vertical' directions respectively
                 * (these directions are relative to the current orientation
                 * of the view). */
                double scale = Math.min( plot_.getWidth(), plot_.getHeight() );
                double xf = ( pos.x - posBase_.x ) / scale;
                double yf = ( pos.y - posBase_.y ) / scale;

                /* Turn these into angles.  Phi and Psi are the rotation
                 * angles around the screen vertical and horizontal axes
                 * respectively. */
                double phi = xf * Math.PI / 2.;
                double psi = yf * Math.PI / 2.;
                setRotation( rotateXY( rotBase_, phi, psi ) );
                replot();
            }
        }

        public void mouseMoved( MouseEvent evt ) {
            posBase_ = null;
            rotBase_ = null;
        }

    }

}
