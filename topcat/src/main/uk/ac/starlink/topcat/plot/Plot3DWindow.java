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
    private double theta_;
    private double phi_;

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
        replot();

        /* Make visible. */
        pack();
        setVisible( true );
    }

    /**
     * Resets the viewing angle to a standard position.
     */
    public void resetView() {
        theta_ = 0.0;
        phi_ = 0.0;
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
            resetView();
        }

        /* Configure the state with this window's current viewing angles. */
        state.setTheta( theta_ );
        state.setPhi( phi_ );

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
     * Listener which interprets drag gestures on the plotting surface 
     * as requests to rotate the viewing angles.
     */
    private class DragListener implements MouseMotionListener {

        private Point lastPos_;

        public void mouseDragged( MouseEvent evt ) {
            Point pos = evt.getPoint(); 
            if ( lastPos_ != null ) {
                double xf = ( lastPos_.x - pos.x ) / (double) plot_.getWidth();
                double yf = ( lastPos_.y - pos.y ) / (double) plot_.getHeight();
                theta_ += yf * Math.PI;
                phi_ += xf * Math.PI;
            }
            replot();
            lastPos_ = pos;
        }

        public void mouseMoved( MouseEvent evt ) {
            lastPos_ = null;
        }
    }

}
