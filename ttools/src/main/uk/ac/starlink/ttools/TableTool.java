package uk.ac.starlink.ttools;

import gnu.jel.CompilationException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.ProgressLineStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.Loader;

/**
 * Pipeline tool for generic table manipulation.
 *
 * @author   Mark Taylor
 * @since    11 Feb 2005
 */
public class TableTool extends TableTask {

    private String inLoc_;
    private String inFmt_;
    private boolean stream_;
    private ProcessingMode mode_;
    private List pipeline_ = new ArrayList();
    private final ProcessingMode[] modes_;
    private final static String[] MODE_NAMES = new String[] {
        CopyMode.class.getName(),
        MetadataMode.class.getName(),
        CountMode.class.getName(),
    };

    public TableTool() {
        List modeList = new ArrayList();
        Loader.loadProperties();
        for ( int i = 0; i < MODE_NAMES.length; i++ ) {
            Object mode = Loader.getClassInstance( MODE_NAMES[ i ],
                                                   ProcessingMode.class );
            if ( mode != null ) {
                modeList.add( mode );
            }
        }
        modeList.addAll( Loader.getClassInstances( "ttool.modes",
                                                   ProcessingMode.class ) );
        modes_ = (ProcessingMode[]) modeList.toArray( new ProcessingMode[ 0 ] );
    }

    public String getCommandName() {
        return "ttool";
    }

    public boolean setArgs( List argList ) {
        if ( ! super.setArgs( argList ) ) {
            return false;
        }
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.startsWith( "-" ) && arg.length() > 1 ) {
                if ( arg.equals( "-ifmt" ) ) {
                    it.remove();
                    if ( it.hasNext() ) {
                        inFmt_ = (String) it.next();
                        it.remove();
                    }
                    else {
                        return false;
                    }
                }
                else if ( arg.equals( "-stream" ) ) {
                    it.remove();
                    stream_ = true;
                }
                else if ( arg.equals( "-select" ) ) {
                    it.remove();
                    if ( it.hasNext() ) {
                        final String expr = (String) it.next();
                        it.remove();
                        pipeline_.add( new Step() {
                            public StarTable wrap( StarTable base )
                                    throws IOException {
                                try {
                                    return new JELSelectorTable( base, expr );
                                }
                                catch ( CompilationException e ) {
                                    String msg = "Bad expression \"" + expr +
                                                 "\" (" + e.getMessage() + ")";
                                    throw (IOException) new IOException( msg )
                                                       .initCause( e );
                                }
                            }
                        } );
                    }
                    else {
                        return false;
                    }
                }
                else {
                    for ( int i = 0; i < modes_.length; i++ ) {
                        ProcessingMode mode = modes_[ i ];
                        if ( arg.equals( "-" + mode.getName() ) ) {
                            it.remove();
                            if ( mode_ == null ) {
                                mode_ = mode;
                            }
                            else {
                                return false;
                            }
                        }
                    }
                }
            }
            else if ( inLoc_ == null ) {
                it.remove();
                inLoc_ = arg;
            }
        }
        if ( mode_ == null ) {
            mode_ = new CopyMode();
        }
        return mode_.setArgs( argList );
    }

    /**
     * Returns the table at the end of the pipe ready to be disposed of.
     * This may have had several processing steps performed on it.
     * 
     * @return  processed table for output
     */
    public StarTable getInputTable() throws IOException {
        StarTable table;
        if ( inLoc_ == null ) {
            throw new IOException( "No input table specified" );
        }
        else if ( inLoc_.equals( "-" ) || stream_ ) {
            InputStream in =
                new BufferedInputStream( DataSource.getInputStream( inLoc_ ) );
            table = getStreamedTable( in );
        }
        else {
            table = getTableFactory().makeStarTable( inLoc_, inFmt_ );
        }
        if ( isVerbose() ) {
            table = new ProgressLineStarTable( table, System.err );
        }
        for ( Iterator it = pipeline_.iterator(); it.hasNext(); ) {
            Step step = (Step) it.next();
            table = step.wrap( table );
        }
        return table;
    }

    /**
     * Returns a one-shot StarTable derived from an input stream.
     * This will allow only a single RowSequence to be taken from it.
     * It may fail with a TableFormatException for input formats
     * which can't be made from a stream.
     *
     * @param   in  input stream
     * @return  one-shot startable
     */
    public StarTable getStreamedTable( final InputStream in )
            throws IOException {
        if ( inFmt_ == null ) {
            throw new IOException( "Can't stream without named input format" );
        }
        final TableBuilder tbuilder = getTableFactory()
                                     .getTableBuilder( inFmt_ );
        assert tbuilder != null;
        final StreamRowStore streamStore = new StreamRowStore( 1024 );
        new Thread( "Table Streamer" ) {
            public void run() {
                try {
                    tbuilder.streamStarTable( in, streamStore, null );
                }
                catch ( IOException e ) {
                    streamStore.setError( e );
                }
            }
        }.start();
        return streamStore.getStarTable();
    }

    public void execute() throws IOException {
        mode_.process( getInputTable() );
    }

    public String getHelp() {
        StringBuffer help = new StringBuffer( super.getHelp() );
        help.append( "\n   Auto-detected in-formats:\n" );
        for ( Iterator it = getTableFactory().getDefaultBuilders().iterator();
              it.hasNext(); ) {
            help.append( "      " )
                .append( ((TableBuilder) it.next())
                        .getFormatName().toLowerCase() )
                .append( '\n' );
        }
        help.append( "\n   Known in-formats:\n" );
        for ( Iterator it = getTableFactory().getKnownFormats().iterator();
              it.hasNext(); ) {
            help.append( "      " )
                .append( ((String) it.next()).toLowerCase() )
                .append( '\n' );
        }
        return help.toString();
    }

    public String[] getSpecificOptions() {
        List opts = new ArrayList();
        StringBuffer modeChoice = new StringBuffer( "(" );
        for ( int i = 0; i < modes_.length; i++ ) {
            if ( i > 0 ) {
                modeChoice.append( " | " );
            }
            ProcessingMode mode = modes_[ i ];
            modeChoice.append( '-' )
                      .append( mode.getName() );
        }
        modeChoice.append( ")" );
        opts.add( modeChoice.toString() );
        opts.addAll( Arrays.asList( new String[] {
            "[-ifmt <in-format> [-stream]]",
            "[<in-table>]",
        } ) );
        return (String[]) opts.toArray( new String[ 0 ] );
    }

    public static void main( String[] args ) {
        if ( ! new TableTool().run( args ) ) {
            System.exit( 1 );
        }
    }

    /**
     * Interface defining a processing step for a table pipeline.
     */
    private static abstract class Step {

        /**
         * Wraps an input table to provide an output table.
         *
         * @param  base  input table
         * @return  output table
         */
        public abstract StarTable wrap( StarTable base ) throws IOException;
    }

}
