package uk.ac.starlink.ttools.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.Tokenizer;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.ObjectFactory;

/**
 * Factory which can create ProcessingStep objects from strings which
 * represent the filter specifications.
 *
 * <p>This is currently a singleton class.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Aug 2005
 */
public class StepFactory {

    private final ObjectFactory<ProcessingFilter> filterFactory_;
    private static StepFactory instance_;

    /**
     * Private sole constructor.
     */
    private StepFactory() {
        filterFactory_ =
            new ObjectFactory<ProcessingFilter>( ProcessingFilter.class );
        String pkg = "uk.ac.starlink.ttools.filter.";
        filterFactory_.register( "addcol", pkg + "AddColumnFilter" );
        filterFactory_.register( "addpixsample", pkg + "PixSampleFilter" );
        filterFactory_.register( "addresolve", pkg + "ResolverFilter" );
        filterFactory_.register( "addskycoords", pkg + "AddSkyCoordsFilter" );
        filterFactory_.register( "assert", pkg + "AssertFilter" );
        filterFactory_.register( "badval", pkg + "BadValueFilter" );
        filterFactory_.register( "cache", pkg + "CacheFilter" );
        filterFactory_.register( "check", pkg + "CheckFilter" );
        filterFactory_.register( "clearparams", pkg + "ClearParameterFilter" );
        filterFactory_.register( "collapsecols", pkg + "CollapseColsFilter" );
        filterFactory_.register( "colmeta", pkg + "ColumnMetadataFilter" );
        filterFactory_.register( "constcol", pkg + "ConstFilter" );
        filterFactory_.register( "delcols", pkg + "DeleteColumnFilter" );
        filterFactory_.register( "every", pkg + "EveryFilter" );
        filterFactory_.register( "explodecols", pkg + "ExplodeColsFilter" );
        filterFactory_.register( "explodeall", pkg + "ExplodeAllFilter" );
        filterFactory_.register( "fixcolnames", pkg + "FixNamesFilter" );
        filterFactory_.register( "group", pkg + "GroupFilter" );
        filterFactory_.register( "head", pkg + "HeadFilter" );
        filterFactory_.register( "healpixmeta", pkg + "HealpixMetadataFilter" );
        filterFactory_.register( "keepcols", pkg + "KeepColumnFilter" );
        filterFactory_.register( "meta", pkg + "MetadataFilter" );
        filterFactory_.register( "progress", pkg + "ProgressFilter" );
        filterFactory_.register( "random", pkg + "RandomFilter" );
        filterFactory_.register( "randomview", pkg + "RandomViewFilter" );
        filterFactory_.register( "repeat", pkg + "RepeatFilter" );
        filterFactory_.register( "replacecol", pkg + "ReplaceColumnFilter" );
        filterFactory_.register( "replaceval", pkg + "ReplaceValueFilter" );
        filterFactory_.register( "rowrange", pkg + "RangeFilter" );
        filterFactory_.register( "select", pkg + "SelectFilter" );
        filterFactory_.register( "seqview", pkg + "SequentialViewFilter" );
        filterFactory_.register( "setparam", pkg + "SetParameterFilter" );
        filterFactory_.register( "shuffle", pkg + "ShuffleFilter" );
        filterFactory_.register( "sort", pkg + "SortFilter" );
        filterFactory_.register( "sorthead", pkg + "SortHeadFilter" );
        filterFactory_.register( "stats", pkg + "StatsFilter" );
        filterFactory_.register( "tablename", pkg + "NameFilter" );
        filterFactory_.register( "tail", pkg + "TailFilter" );
        filterFactory_.register( "transpose", pkg + "TransposeFilter" );
        filterFactory_.register( "uniq", pkg + "UniqueFilter" );
    }

    /**
     * Returns the factory which can create filters from their nicknames.
     *
     * @return  ProcessingFilter factory
     */
    public ObjectFactory<ProcessingFilter> getFilterFactory() {
        return filterFactory_;
    }

    /**
     * Creates an array of processing filters from an input string.
     * The string may contain zero or more lines, separated by 
     * semicolons or newlines.
     *
     * @param  text  input string
     * @return   array of steps
     */
    public ProcessingStep[] createSteps( String text ) throws TaskException {
        String[] lines = Tokenizer.tokenizeLines( text );
        List<ProcessingStep> stepList = new ArrayList<ProcessingStep>();
        for ( int i = 0; i < lines.length; i++ ) {
            ProcessingStep step = createStep( lines[ i ] );
            if ( step != null ) {
                stepList.add( step );
            }
        }
        return stepList.toArray( new ProcessingStep[ 0 ] );
    }

    /**
     * Creates a processing filter from a line of text.
     * The general format for the line is 
     * <code>&lt;filter-name&gt; &lt;filter-args&gt;</code>
     *
     * @param   line  line of text representing filter commands
     * @return  processing step; may be null if <code>line</code>
     *          contains no tokens
     */
    public ProcessingStep createStep( String line ) throws TaskException {

        /* Chop the line up into tokens. */
        String[] tokens = Tokenizer.tokenizeWords( line );

        /* Return null if there is no lexical content. */
        if ( tokens.length == 0 ) {
            return null;
        }

        /* Get the name of the command and the other arguments. */
        String cmd = tokens[ 0 ];
        List<String> argList = new ArrayList<String>( Arrays.asList( tokens ) );
        argList.remove( 0 );

        /* Try to make and return a filter command from the tokens. */
        if ( filterFactory_.isRegistered( cmd ) ) {
            ProcessingFilter filter; 
            try {
                filter = filterFactory_.createObject( cmd );
            }
            catch ( LoadException e ) {
                throw new TaskException( "Trouble loading command " + cmd, e );
            }
            try {
                ProcessingStep step = filter.createStep( argList.iterator() );
                if ( argList.isEmpty() ) {
                    return step;
                }
                else {
                    boolean containSpace = false;
                    StringBuffer msg = new StringBuffer( "Unused arguments:" ); 
                    for ( String arg : argList ) {
                        containSpace = containSpace || arg.indexOf( ' ' ) >= 0;
                        msg.append( " '" )
                           .append( arg )
                           .append( "'" );
                    }
                    if ( ! containSpace && 
                         line.indexOf( '\'' ) < 0 &&
                         line.indexOf( '"' ) < 0 ) {
                        msg.append( "\n(Hint: arguments containing spaces" )
                           .append( " must be quoted)" );
                    }
                    throw new ArgException( msg.toString() );
                }
            }
            catch ( ArgException e ) {
                StringBuffer sbuf = new StringBuffer();
                sbuf.append( e.getMessage() )
                    .append( '\n' )
                    .append( '\n' )
                    .append( "Command was: " )
                    .append( line )
                    .append( '\n' )
                    .append( '\n' )
                    .append( "Usage: " )
                    .append( cmd );
                String pad = ( "Usage: " + cmd + " " ).replaceAll( ".", " " );
                String fusage = filter.getUsage();
                if ( fusage != null ) {
                    fusage = fusage.replaceAll( "\n", "\n" + pad );
                }
                if ( fusage != null ) {
                    sbuf.append( ' ' )
                        .append( fusage );
                }
                throw new TaskException( sbuf.toString(), e );
            }
        }
        else {
            throw new TaskException( "Unknown processing command: " 
                                   + cmd + "\n\n" + getFilterUsages() );
        }
    }

    /**
     * Returns a formatted list of the available filter commands with
     * their usages.
     *
     * @return   filter usage descriptions
     */
    private String getFilterUsages() {
        StringBuffer sbuf = new StringBuffer()
            .append( "Available filters:\n" );
        String[] fnames = filterFactory_.getNickNames();
        for ( int i = 0; i < fnames.length; i++ ) {
            try {
                String fname = fnames[ i ];
                ProcessingFilter filter = filterFactory_.createObject( fname );
                String fintro = "   " + fname;
                sbuf.append( fintro );
                String fusage = filter.getUsage();
                if ( fusage != null ) {
                    String pad = fintro.replaceAll( ".", " " ) + " ";
                    sbuf.append( ' ' )
                        .append( fusage.trim().replaceAll( "\n", "\n" + pad ) );
                }
                sbuf.append( '\n' );
            }
            catch ( LoadException e ) {
                // not available
            }
        }
        return sbuf.toString();
    }

    /**
     * Returns the singleton instance of this class.
     *
     * @return  StepFactory instance
     */
    public static StepFactory getInstance() {
        if ( instance_ == null ) {
            instance_ = new StepFactory();
        }
        return instance_;
    }
}
