package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.JComponent;

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
        return Matrices.mmMult( Matrices.mmMult( base, rotX ), rotY );
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
     * @param   theta   rotation angle in radians
     */
    private static double[] rotate( double[] base, double[] screenAxis,
                                    double theta ) {

        /* Calculate the unit vector in data space corresponding to the 
         * given screen axis. */
        double[] axis = Matrices.mvMult( Matrices.invert( base ), screenAxis );
        double[] a = Matrices.normalise( axis );
        double x = a[ 0 ];
        double y = a[ 1 ];
        double z = a[ 2 ];

        /* Calculate and return the rotation matrix (Euler angles).
         * This algebra copied from SLALIB DAV2M (Pal version). */
        double s = Math.sin( theta );
        double c = Math.cos( theta );
        double w = 1.0 - c;
        return new double[] {
            x * x * w + c,
            x * y * w + z * s,
            x * z * w - y * s,
            x * y * w - z * s,
            y * y * w + c,
            y * z * w + x * s,
            x * z * w + y * s,
            y * z * w - x * s,
            z * z * w + c,
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
