package uk.ac.starlink.ttools.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.mode.ProcessingMode;
import uk.ac.starlink.util.LoadException;

/**
 * Output mode which can be used to give the user, via the parameter 
 * system, the choice about what mode to use.
 * This mode is not intended for use as one amongst many, but as the 
 * only ProcessingMode for a processing sequence, for instance 
 * as the parameter to the {@link ConsumerTask} constructor.
 *
 * <p><strong>Note:</strong> that use of this mode is used by the
 * {@link uk.ac.starlink.ttools.build.JyStilts} script
 * to identify tasks whose primary output is a table,
 * so that such tables can by default be returned to the jython environment
 * rather than serialised to output files.
 *
 * @author   Mark Taylor
 * @since    3 May 2006
 */
public class ChoiceMode implements ProcessingMode {

    private final OutputModeParameter modeParam_;
    private final Parameter<?>[] params_;
    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    /**
     * Constructor.
     */
    public ChoiceMode() {
        modeParam_ = new OutputModeParameter( "omode" );
        List<Parameter<?>> paramList = new ArrayList<Parameter<?>>();
        paramList.add( modeParam_ );
        paramList.addAll( Arrays.asList( getDefaultAssociatedParameters(
                                              modeParam_ ) ) ); 
        params_ = paramList.toArray( new Parameter<?>[ 0 ] );
    }

    public Parameter<?>[] getAssociatedParameters() {
        return params_;
    }

    /**
     * Returns the parameter used to acquire the chosen output mode.
     *
     * @return  output mode parameter
     */
    public OutputModeParameter getOutputModeParameter() {
        return modeParam_;
    }

    public String getDescription() {
        return new StringBuffer()
           .append( "Provides a choice of output mode options via the\n" )
           .append( modeParam_.getName() )
           .append( "parameter." )
           .toString();
    }

    public TableConsumer createConsumer( Environment env )
            throws TaskException {
        return modeParam_.consumerValue( env );
    }

    /**
     * Returns a list of parameters associated with an OutputModeParameter.
     * These serve a documentation purpose only - the purpose is so that
     * usage messages and other automatically generated documentation
     * feature an 'out' and 'ofmt' parameter which is what people who
     * don't read documentation will be looking for, rather than an
     * 'omode' parameter which people will look straight through.
     *
     * @return   parameters associated with output mode
     */
    private static Parameter<?>[]
            getDefaultAssociatedParameters( OutputModeParameter modeParam ) {

        /* Get the default mode object, which is copy mode. */
        String modeName = modeParam.getStringDefault();
        ProcessingMode mode;
        try {
            mode = Stilts.getModeFactory().createObject( modeName );
        }
        catch ( LoadException e ) {
            logger_.warning( "Can't load default output mode?? " + e );
            return new Parameter<?>[ 0 ];
        }

        /* Get its associated parameters; we know what sort they should be. */
        Parameter<?>[] modeParams = mode.getAssociatedParameters();
        if ( modeParams.length != 2 ||
             ! ( modeParams[ 0 ] instanceof OutputTableParameter ) ||
             ! ( modeParams[ 1 ] instanceof OutputFormatParameter ) ) {
            logger_.warning( "Output mode parameters out of sync?" );
            return new Parameter<?>[ 0 ];
        }
        Parameter<?> outParam =
            new OutputTableParameter( modeParams[ 0 ].getName() );
        Parameter<?> fmtParam =
            new OutputFormatParameter( modeParams[ 1 ].getName() );
        Parameter<?>[] resultParams = new Parameter<?>[] { outParam, fmtParam };

        /* Doctor and return them. */
        for ( int i = 0; i < resultParams.length; i++ ) {
            resultParams[ i ].setDescription( new String[] {
                resultParams[ i ].getDescription(),
                "<p>This parameter must only be given if",
                "<code>" + modeParam.getName() + "</code>",
                "has its default value of \"<code>" + modeName + "</code>\".",
                "</p>",
            } );
        }
        return resultParams;
    }
}
