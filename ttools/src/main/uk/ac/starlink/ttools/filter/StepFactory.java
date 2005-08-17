package uk.ac.starlink.ttools.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.LoadException;
import uk.ac.starlink.ttools.ObjectFactory;

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

    private final ObjectFactory filterFactory_;
    private static StepFactory instance_;

    /**
     * Private sole constructor.
     */
    private StepFactory() {
        filterFactory_ = new ObjectFactory( ProcessingFilter.class );
        String pkg = "uk.ac.starlink.ttools.filter.";
        filterFactory_.register( "addcol", pkg + "AddColumnFilter" );
        filterFactory_.register( "cache", pkg + "CacheFilter" );
        filterFactory_.register( "delcols", pkg + "DeleteColumnFilter" );
        filterFactory_.register( "every", pkg + "EveryFilter" );
        filterFactory_.register( "explodecols", pkg + "ExplodeColumnFilter" );
        filterFactory_.register( "explodeall", pkg + "ExplodeAllFilter" );
        filterFactory_.register( "head", pkg + "HeadFilter" );
        filterFactory_.register( "keepcols", pkg + "KeepColumnFilter" );
        filterFactory_.register( "progress", pkg + "ProgressFilter" );
        filterFactory_.register( "random", pkg + "RandomFilter" );
        filterFactory_.register( "replacecol", pkg + "ReplaceColumnFilter" );
        filterFactory_.register( "select", pkg + "SelectFilter" );
        filterFactory_.register( "sequential", pkg + "SequentialFilter" );
        filterFactory_.register( "sort", pkg + "ColumnSortFilter" );
        filterFactory_.register( "sortexpr", pkg + "ExpressionSortFilter" );
        filterFactory_.register( "tail", pkg + "TailFilter" );
    }

    /**
     * Returns the object factory which can create {@link ProcessingFilter}
     * instances from their nicknames.
     *
     * @return  ProcessingFilter factory
     */
    public ObjectFactory getFilterFactory() {
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
        String[] lines = tokenizeLines( text );
        List stepList = new ArrayList();
        for ( int i = 0; i < lines.length; i++ ) {
            ProcessingStep step = createStep( lines[ i ] );
            if ( step != null ) {
                stepList.add( step );
            }
        }
        return (ProcessingStep[]) stepList.toArray( new ProcessingStep[ 0 ] );
    }

    /**
     * Creates a processing filter from a line of text.
     * The general format for the line is 
     * <code>&lt;filter-name&gt; &lt;ilter-args&gt;</code>
     *
     * @param   line  line of text representing filter commands
     * @return  processing step; may be null if <code>line</code>
     *          contains no tokens
     */
    public ProcessingStep createStep( String line ) throws TaskException {

        /* Chop the line up into tokens. */
        String[] tokens = tokenizeWords( line );

        /* Return null if there is no lexical content. */
        if ( tokens.length == 0 ) {
            return null;
        }

        /* Get the name of the command and the other arguments. */
        String cmd = tokens[ 0 ];
        List argList = new ArrayList( Arrays.asList( tokens ) );
        argList.remove( 0 );

        /* Try to make and return a filter command from the tokens. */
        if ( filterFactory_.isRegistered( cmd ) ) {
            ProcessingFilter filter; 
            try {
                filter = (ProcessingFilter) filterFactory_.createObject( cmd );
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
                    StringBuffer extraArgs = new StringBuffer();
                    for ( Iterator it = argList.iterator(); it.hasNext(); ) {
                        extraArgs.append( ' ' )
                                 .append( it.next() );
                    }
                    throw new ArgException( "Spurious arguments:" + extraArgs );
                }
            }
            catch ( ArgException e ) {
                StringBuffer sbuf = new StringBuffer();
                sbuf.append( e.getMessage() )
                    .append( '\n' )
                    .append( "line was: " )
                    .append( line )
                    .append( '\n' )
                    .append( "Usage: " )
                    .append( cmd )
                    .append( filter.getUsage() );
                throw new UsageException( sbuf.toString(), e );
            }
        }
        else {
            throw new UsageException( "Unknown processing command " + cmd );
        }
    }

    /**
     * Returns the singleton instance of this class.
     *
     * @param  StepFactory instance
     */
    public static StepFactory getInstance() {
        if ( instance_ == null ) {
            instance_ = new StepFactory();
        }
        return instance_;
    }

    /**
     * Splits a string up into lines, separated by semicolons or newlines.
     * Semicolons may appear inside quoted strings without terminating
     * a line.
     *
     * @param  text  input string
     * @return  array of lines
     */
    static String[] tokenizeLines( String text ) throws UsageException {
        List tokenList = new ArrayList();
        String text1 = text + ";";
        char delim = 0;
        StringBuffer token = new StringBuffer();
        for ( int i = 0; i < text1.length(); i++ ) {
            char chr = text1.charAt( i );
            switch ( chr ) {
                case ';':
                case '\n':
                    if ( delim == 0 ) {
                        tokenList.add( token.toString() );
                        token = new StringBuffer();
                    }
                    else {
                        token.append( chr );
                    }
                    break;
                case '\'':
                case '"':
                    if ( delim == chr ) {
                        delim = 0;
                    }
                    else if ( delim == 0 ) {
                        delim = chr;
                    }
                    token.append( chr );
                    break;
                default:
                    token.append( chr );
            }
        }
        if ( token.length() > 0 || delim != 0 ) {
            throw new UsageException( "Badly formed input: " + text );
        }
        return (String[]) tokenList.toArray( new String[ 0 ] );
    }


    /**
     * Chops up a line of text into tokens.
     * Works roughly like the shell, as regards quotes, whitespace and
     * comments.
     *
     * @param  line  line of text
     * @return  array of words corresponding to <code>line</code>
     */
    static String[] tokenizeWords( String line ) throws UsageException {
        String line1 = line + '\n';
        List tokenList = new ArrayList();
        StringBuffer token = null;
        char delim = 0;
        boolean done = false;
        for ( int i = 0; i < line1.length() && ! done; i++ ) {
            char chr = line1.charAt( i );
            switch ( chr ) {
                case '#':
                    if ( delim == 0 ) {
                        done = true;
                    }
                    else {
                        token.append( chr );
                    }
                    break;
                case ' ':
                case '\t':
                case '\n':
                    if ( token != null ) {
                        if ( delim == 0 ) {
                            tokenList.add( token.toString() );
                            token = null;
                        }
                        else {
                            token.append( chr );
                        }
                    }
                    break;
                case '\'':
                case '"':
                    if ( token == null ) {
                        token = new StringBuffer();
                        delim = chr;
                    }
                    else if ( delim == chr ) {
                        tokenList.add( token.toString() );
                        token = null;
                        delim = 0;
                    }
                    else if ( delim == 0 ) {
                        delim = chr;
                    }
                    else {
                        token.append( chr );
                    }
                    break;
                default:
                    if ( token == null ) {
                        token = new StringBuffer();
                    }
                    token.append( chr );
            }
        }
        if ( token != null || delim != 0 ) {
            throw new UsageException( "Badly formed line: " + line );
        }
        return (String[]) tokenList.toArray( new String[ 0 ] );
    }
}
