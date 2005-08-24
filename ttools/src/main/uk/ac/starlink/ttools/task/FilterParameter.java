package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.LoadException;
import uk.ac.starlink.ttools.ObjectFactory;
import uk.ac.starlink.ttools.filter.ProcessingFilter;
import uk.ac.starlink.ttools.filter.ProcessingStep;
import uk.ac.starlink.ttools.filter.StepFactory;

/**
 * Parameter which contains a value representing one or more
 * {@link uk.ac.starlink.ttools.filter.ProcessingStep}s.
 *
 * @author   Mark Taylor
 * @since    17 Aug 2005
 */
public class FilterParameter extends Parameter
        implements ExtraParameter, MultiParameter {

    private ProcessingStep[] steps_;

    public FilterParameter( String name ) {
        super( name );
    }

    public void setValueFromString( Environment env, String sval )
            throws TaskException {
        if ( sval != null ) {
            steps_ = StepFactory.getInstance().createSteps( sval );
        }
        else {
            steps_ = new ProcessingStep[ 0 ];
        }
        super.setValueFromString( env, sval );
    }

    /**
     * Returns the value of this parameter as an array of processing steps.
     *
     * @param   env  execution environment
     * @return   array of zero or more processing steps 
     */
    public ProcessingStep[] stepsValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return steps_;
    }

    public String getExtraUsage( TableEnvironment env ) {
        StringBuffer sbuf = new StringBuffer()
            .append( "\n   Known filter commands:\n" );
        ObjectFactory filterFactory = StepFactory.getInstance()
                                                 .getFilterFactory();
        String[] fnames = filterFactory.getNickNames();
        for ( int i = 0; i < fnames.length; i++ ) {
            String fname = fnames[ i ];
            try {
                ProcessingFilter filter = (ProcessingFilter)
                                          filterFactory.createObject( fname );
                sbuf.append( "      " )
                    .append( fname )
                    .append( filter.getUsage() )
                    .append( '\n' );
            }
            catch ( LoadException e ) {
                if ( env.isDebug() ) {
                    sbuf.append( "    ( " )
                        .append( fname )
                        .append( " - not available: " )
                        .append( e )
                        .append( " )\n" );
                }
            }
        }
        sbuf.append( "\n   commands can be separated on one line using ';'\n" );
        return sbuf.toString();
    }
}
