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
public class FilterParameter extends Parameter<ProcessingStep[]>
        implements ExtraParameter, MultiParameter {

    public FilterParameter( String name ) {
        super( name, ProcessingStep[].class, true );
        setUsage( "<cmds>" );
        setNullPermitted( true );
        setTableDescription( "the table", null, null );
    }

    /**
     * Sets the wording used to refer to the target table for the filters
     * this parameter acquires.  The description and prompt are set.
     * If not set, the wording "the table" is used to describe the table.
     *
     * @param  shortDescrip  text to replace "the table"
     * @param  tableParam  if supplied, gives the table parameter on behalf
     *                     of which this format parameter is operating;
     *                     may be null
     * @param  isBefore    TRUE means filter is applied before any other
     *                     processing, FALSE means after, null don't know
     */
    public final void
            setTableDescription( String shortDescrip,
                                 AbstractInputTableParameter<?> tableParam,
                                 Boolean isBefore ) {
        setPrompt( "Processing command(s) for " + shortDescrip );
        StringBuffer dbuf = new StringBuffer();
        dbuf.append( shortDescrip );
        if ( tableParam != null ) {
            dbuf.append( " as specified by parameter <code>" )
                .append( tableParam.getName() )
                .append( "</code>" );
        }
        String longDescrip = dbuf.toString();
        StringBuffer seqBuf = new StringBuffer();
        if ( isBefore != null ) {
            seqBuf.append( ",\n" )
                  .append( isBefore.booleanValue() ? "before any"
                                                   : "after all" )
                  .append( " other processing has taken place" );
        }
        String seqTxt = seqBuf.toString();
        String indir = String.valueOf( LineEnvironment.INDIRECTION_CHAR );
        setDescription( new String[] {
            "<p>Specifies processing to be performed on",
            longDescrip + seqTxt + ".",
            "The value of this parameter is one or more of the filter",
            "commands described in <ref id=\"filterSteps\"/>.",
            "If more than one is given, they must be separated by",
            "semicolon characters (\";\").",
            "This parameter can be repeated multiple times on the same",
            "command line to build up a list of processing steps.",
            "The sequence of commands given in this way",
            "defines the processing pipeline which is performed on the table.",
            "</p>",
            "<p>Commands may alternatively be supplied in an external file,",
            "by using the indirection character '<code>" + indir + "</code>'.",
            "Thus a value of \"<code>" + indir + "filename</code>\"",
            "causes the file <code>filename</code> to be read for a list",
            "of filter commands to execute.  The commands in the file",
            "may be separated by newline characters and/or semicolons,",
            "and lines which are blank or which start with a",
            "'<code>#</code>' character are ignored.",
            "A backslash character '<code>\\</code>' at the end of a line",
            "joins it with the following line.",
            "</p>",
        } );
    }

    public char getValueSeparator() {
        return ';';
    }

    public ProcessingStep[] stringToObject( Environment env, String sval )
            throws TaskException {
        return StepFactory.getInstance().createSteps( sval );
    }

    /**
     * Returns the value of this parameter as an array of processing steps.
     *
     * @param   env  execution environment
     * @return   array of zero or more processing steps 
     */
    public ProcessingStep[] stepsValue( Environment env ) throws TaskException {
        ProcessingStep[] steps = objectValue( env );
        return ( env == null || steps == null ) ? new ProcessingStep[ 0 ]
                                                : steps;
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
        ObjectFactory<ProcessingFilter> filterFactory =
            StepFactory.getInstance().getFilterFactory();
        String[] fnames = filterFactory.getNickNames();
        for ( int i = 0; i < fnames.length; i++ ) {
            String fname = fnames[ i ];
            try {
                ProcessingFilter filter = filterFactory.createObject( fname );
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
