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
public class TablePipe extends TableTask {

    private String inLoc_;
    private String inFmt_;
    private boolean stream_;
    private ProcessingMode mode_;
    private List pipeline_ = new ArrayList();
    private final ProcessingMode[] modes_;
    private final ProcessingFilter[] filters_;

    private final static String[] MODE_NAMES = new String[] {
        CopyMode.class.getName(),
        JdbcMode.class.getName(),
        MetadataMode.class.getName(),
        CountMode.class.getName(),
    };

    private final static String[] FILTER_NAMES = new String[] {
        SelectFilter.class.getName(),
        EveryFilter.class.getName(),
        AddColumnFilter.class.getName(),
        KeepColumnFilter.class.getName(),
        DeleteColumnFilter.class.getName(),
        ExplodeFilter.class.getName(),
    };

    public TablePipe() {
        modes_ = (ProcessingMode[]) 
                 Loader.getClassInstances( MODE_NAMES, "tpipe.modes",
                                           ProcessingMode.class )
                .toArray( new ProcessingMode[ 0 ] );
        filters_ = (ProcessingFilter[])
                   Loader.getClassInstances( FILTER_NAMES, "tpipe.filters",
                                             ProcessingFilter.class )
                  .toArray( new ProcessingFilter[ 0 ] );
    }

    public String getCommandName() {
        return "tpipe";
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

                    for ( int i = 0; i < filters_.length; i++ ) {
                         ProcessingFilter filter = filters_[ i ];
                         if ( arg.equals( "-" + filter.getName() ) ) {
                             it.remove();
                             ProcessingStep step = null;
                             try { 
                                 step = filter.createStep( it );
                             }
                             catch ( IllegalArgumentException e ) {
                                 System.err.println( e.getMessage() );
                             }
                             if ( step == null ) {
                                 String fname = filter.getName();
                                 String fusage = filter.getFilterUsage();
                                 String msg = fname + " mode usage: \n"
                                            + "      -" + fname;
                                 if ( fusage != null ) {
                                     msg += " " + fusage;
                                 }
                                 System.err.println( msg );
                                 return false;
                             }
                             else {
                                 pipeline_.add( step );
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
            ProcessingStep step = (ProcessingStep) it.next();
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

        help.append( "\n   Mode flags - Use one of:\n" );
        for ( int i = 0; i < modes_.length; i++ ) {
            ProcessingMode mode = modes_[ i ];
            help.append( "      -" )
                .append( mode.getName() );
            String modeUsage = mode.getModeUsage();
            if ( modeUsage != null ) {
                help.append( ' ' )
                    .append( modeUsage );
            }
            help.append( '\n' );
        }

        help.append( "\n   Filter flags - Use any sequence of:\n" );
        for ( int i = 0; i < filters_.length; i++ ) {
            ProcessingFilter filter = filters_[ i ];
            help.append( "      -" )
                .append( filter.getName() );
            String filterUsage = filter.getFilterUsage();
            if ( filterUsage != null ) {
                help.append( ' ' )
                    .append( filterUsage );
            }
            help.append( '\n' );
        }

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
        opts.add( "[-ifmt <in-format> [-stream]]" );
        opts.add( "[<in-table>]" );
        opts.add( "<mode-flags>" );
        opts.add( "[<filter-flags>]" );
        return (String[]) opts.toArray( new String[ 0 ] );
    }

    public static void main( String[] args ) {
        if ( ! new TablePipe().run( args ) ) {
            System.exit( 1 );
        }
    }

}
