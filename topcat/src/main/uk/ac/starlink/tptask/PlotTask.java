package uk.ac.starlink.tptask;

import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.tplot.PlotState;
import uk.ac.starlink.tplot.TablePlot;
import uk.ac.starlink.ttools.task.InputTableSpec;

/**
 * Abstract superclass for tasks which generate plots from table data.
 *
 * @author   Mark Taylor
 * @since    22 Apr 2008
 */
public abstract class PlotTask implements Task {

    private final String purpose_;
    private final PlotStateFactory stateFactory_;
    private final TablePlot plot_;
    private final List paramList_;
    private final IntegerParameter xpixParam_;
    private final IntegerParameter ypixParam_;
    private final PainterParameter painterParam_;

    /**
     * Constructor.
     *
     * @param  purpose  short (one-line) task purpose
     * @param  stateFactory   PlotStateFactory for use with this plot
     * @param  plot   plot component for use with this plot
     */
    public PlotTask( String purpose, PlotStateFactory stateFactory,
                     TablePlot plot ) {
        purpose_ = purpose;
        stateFactory_ = stateFactory;
        plot_ = plot;
        paramList_ = new ArrayList();

        xpixParam_ = new IntegerParameter( "xpix" );
        xpixParam_.setMinimum( 1 );
        xpixParam_.setPrompt( "Width of plot in pixels" );
        xpixParam_.setDescription( new String[] {
            "<p>The width of the output graphic in pixels.",
            "</p>",
        } );
        xpixParam_.setDefault( "400" );
        paramList_.add( xpixParam_ );

        ypixParam_ = new IntegerParameter( "ypix" );
        ypixParam_.setMinimum( 1 );
        ypixParam_.setPrompt( "Height of plot in pixels" );
        ypixParam_.setDescription( new String[] {
            "<p>The height of the output graphic in pixels.",
            "</p>",
        } );
        ypixParam_.setDefault( "300" );
        paramList_.add( ypixParam_ );

        painterParam_ = new PainterParameter( "ofmt" );
        paramList_.add( painterParam_ );

        paramList_.addAll( Arrays.asList( stateFactory_.getParameters() ) );
    }

    protected List getParameterList() {
        return paramList_;
    }

    public String getPurpose() {
        return purpose_;
    }

    public Parameter[] getParameters() {
        return (Parameter[]) paramList_.toArray( new Parameter[ 0 ] );
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        final int xpix = xpixParam_.intValue( env );
        final int ypix = ypixParam_.intValue( env );
        final PlotState state = stateFactory_.getPlotState( env );
        final Painter painter = painterParam_.painterValue( env );
        return new Executable() {
            public void execute() throws TaskException, IOException {
                stateFactory_.configureRanges( state );
                state.setValid( true );
                plot_.setSize( new Dimension( xpix, ypix ) );
                plot_.setState( state );
                painter.paintPlot( plot_ );
            }
        };
    }
}
