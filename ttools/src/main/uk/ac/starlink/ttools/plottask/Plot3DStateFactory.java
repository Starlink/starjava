package uk.ac.starlink.ttools.plottask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.task.DoubleParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
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

    private final DoubleParameter fogParam_;
    private final DoubleParameter phiParam_;
    private final DoubleParameter thetaParam_;

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

        fogParam_ = new DoubleParameter( "fog" );
        fogParam_.setPrompt( "Depth fog level" );
        fogParam_.setNullPermitted( false );
        fogParam_.setMinimum( 0.0, true );
        fogParam_.setDescription( new String[] {
            "<p>Sets the level of fogging used to provide a visual",
            "indication of depth.",
            "Object plotted further away from the viewer appear more",
            "washed-out by a white fog.",
            "The default value gives a bit of fogging; increase it to",
            "make the fog thicker, or set to zero if no fogging is required.",
            "</p>",
        } );
        fogParam_.setDefault( new Double( 1.0 ).toString() );

        phiParam_ = new DoubleParameter( "phi" );
        phiParam_.setPrompt( "Rotation around Z axis" );
        phiParam_.setNullPermitted( false );
        phiParam_.setDescription( new String[] {
            "<p>Angle in degrees through which the 3D plot is rotated",
            "abound the Z axis prior to drawing.",
            "</p>",
        } );
        phiParam_.setDefault( new Double( 30. ).toString() );

        thetaParam_ = new DoubleParameter( "theta" );
        thetaParam_.setPrompt( "Rotation around plane horizontal" );
        thetaParam_.setNullPermitted( false );
        thetaParam_.setDescription( new String[] {
            "<p>Angle in degrees through which the 3D plot is rotated",
            "towards the viewer",
            "(i.e. about the horizontal axis of the viewing plane)",
            "prior to drawing.",
            "</p>",
        } );
        thetaParam_.setDefault( new Double( 15. ).toString() );
    }

    public Parameter[] getParameters() {
        List paramList = new ArrayList();
        paramList.addAll( Arrays.asList( super.getParameters() ) );
        paramList.add( fogParam_ );
        paramList.add( phiParam_ );
        paramList.add( thetaParam_ );
        return (Parameter[]) paramList.toArray( new Parameter[ 0 ] );
    }

    protected PlotState createPlotState() {
        return new Plot3DState();
    }

    protected void configurePlotState( PlotState state, Environment env )
            throws TaskException {
        super.configurePlotState( state, env );
        Plot3DState state3 = (Plot3DState) state;

        /* Set state values which are fixed. */
        state3.setRotating( false );
        state3.setZoomScale( 1.0 );

        /* Set foggging. */
        state3.setFogginess( fogParam_.doubleValue( env ) );

        /* Set plot 3D rotation. */
        double[] matrix = new double[] { 1, 0, 0,  0, 1, 0,  0, 0, -1, };
        double theta = thetaParam_.doubleValue( env );
        double phi = phiParam_.doubleValue( env );
        matrix = Plot3D.rotateXY( matrix, 0.0, 0.5 * Math.PI );
        matrix = Plot3D.rotateXY( matrix, Math.toRadians( phi ), 0.0 );
        matrix = Plot3D.rotateXY( matrix, 0.0, - Math.toRadians( theta ) );
        state3.setRotation( matrix );
    }
}
