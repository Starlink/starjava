package uk.ac.starlink.ttools.plottask;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot.Matrices;
import uk.ac.starlink.ttools.plot.Plot3D;
import uk.ac.starlink.ttools.plot.Plot3DState;
import uk.ac.starlink.ttools.plot.PlotState;

/**
 * PlotStateFactory for 3D plots.
 *
 * @author   Mark Taylor
 * @since    20 Oct 2008
 */
public class Plot3DStateFactory extends PlotStateFactory {

    /**
     * Constructor.
     *
     * @param  dimNames names of main plot dimensions (typically "X", "Y", etc);
     * @param  useAux  whether auxiliary axes are used
     * @param  useLabel  whether point text labelling is used
     * @param  errNdim  number of axes for which errors can be plotted
     */
    public Plot3DStateFactory( String[] dimNames, boolean useAux,
                               boolean useLabel, int errNdim ) {
        super( dimNames, useAux, useLabel, errNdim );
    }

    protected PlotState createPlotState() {
        return new Plot3DState();
    }

    protected void configurePlotState( PlotState state, Environment env )
            throws TaskException {
        super.configurePlotState( state, env );
        Plot3DState state3 = (Plot3DState) state;
        state3.setFogginess( 1 );
        state3.setRotating( false );
        state3.setZoomScale( 1.0 );
        double[] matrix = new double[] { 1, 0, 0,  0, 1, 0,  0, 0, -1, };
        matrix = Plot3D.rotateXY( matrix, 0.5, 0.5 * Math.PI );
        matrix = Plot3D.rotateXY( matrix, 0, -0.1 * Math.PI );
        state3.setRotation( matrix );
        state3.setAntialias( true );
    }
}
