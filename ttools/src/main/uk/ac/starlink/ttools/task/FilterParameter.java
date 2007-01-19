package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.LineEnvironment;
import uk.ac.starlink.task.MultiParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.filter.ProcessingFilter;
import uk.ac.starlink.ttools.filter.ProcessingStep;
import uk.ac.starlink.ttools.filter.StepFactory;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.ObjectFactory;

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
        setUsage( "<cmds>" );
        setNullPermitted( true );

        String indir = String.valueOf( LineEnvironment.INDIRECTION_CHAR );
        setDescription( new String[] {
            "<p>The value of this parameter is one or more of the filter",
            "commands described in <ref id=\"filterSteps\"/>.",
            "If more than one is given, they must be separated by",
            "semicolon characters (\";\").",
            "This parameter can be repeated multiple times on the same",
            "command line to build up a list of processing steps.",
            "The sequence of commands given in this way",
            "defines the processing pipeline which is performed on the table.",
            "</p>",
            "<p>Commands may alteratively be supplied in an external file,",
            "by using the indirection character '" + indir + "'.",
            "Thus \"<code>" + getName() + "=" + indir + "filename</code>\"",
            "causes the file <code>filename</code> to be read for a list",
            "of filter commands to execute.  The commands in the file",
            "may be separated by newline characters and/or semicolons.",
            "</p>",
        } );
    }

    public char getValueSeparator() {
        return ';';
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
        return getFiltersUsage( env );
    }

    /**
     * Returns a formatted string listing available filter commands
     * with their usage.
     *
     * @param   env  execution environment
     * @return  usge string
     */
    public static String getFiltersUsage( TableEnvironment env ) {
        StringBuffer sbuf = new StringBuffer()
            .append( "   Known filter commands:\n" );
        ObjectFactory filterFactory = StepFactory.getInstance()
                                                 .getFilterFactory();
        String[] fnames = filterFactory.getNickNames();
        for ( int i = 0; i < fnames.length; i++ ) {
            String fname = fnames[ i ];
            try {
                ProcessingFilter filter = (ProcessingFilter)
                                          filterFactory.createObject( fname );
                String fusage = filter.getUsage();
                sbuf.append( "      " )
                    .append( fname );
                String pad = ( "      " + fname ).replaceAll( ".", " " );
                if ( fusage != null ) {
                    sbuf.append( ' ' )
                        .append( fusage.replaceAll( "\n", "\n " + pad ) );
                }
                sbuf.append( '\n' );
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
        sbuf.append( "\n" )
            .append( "   Commands can be separated on one line using " )
            .append(    "semicolons (;).\n" )
            .append( "   Arguments containing spaces should be " )
            .append(    "'quoted' or \"quoted\".\n" );
        return sbuf.toString();
    }
}
