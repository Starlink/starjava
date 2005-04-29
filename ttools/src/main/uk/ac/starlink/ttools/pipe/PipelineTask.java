package uk.ac.starlink.ttools.pipe;

import gnu.jel.CompilationException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.ArgException;
import uk.ac.starlink.ttools.StreamRowStore;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.Loader;

/**
 * Pipeline tool for generic table manipulation.
 *
 * @author   Mark Taylor
 * @since    11 Feb 2005
 */
public class PipelineTask extends TableTask {

    private String inLoc_;
    private String inFmt_;
    private boolean stream_;
    private ProcessingMode mode_;
    private List pipeline_ = new ArrayList();
    private final ProcessingMode[] modes_;
    private final ProcessingFilter[] filters_;

    /**
     * List of ProcessingMode implementations known by default by this command.
     */
    private final static String[] MODE_NAMES = new String[] {
        CopyMode.class.getName(),
        JdbcMode.class.getName(),
        MetadataMode.class.getName(),
        StatsMode.class.getName(),
        CountMode.class.getName(),
        "uk.ac.starlink.ttools.pipe.TopcatMode",
    };

    /**
     * List of ProcessingFilter implementations known by default
     * by this command.
     */
    private final static String[] FILTER_NAMES = new String[] {
        SelectFilter.class.getName(),
        ColumnSortFilter.class.getName(),
        ExpressionSortFilter.class.getName(),
        EveryFilter.class.getName(),
        HeadFilter.class.getName(),
        TailFilter.class.getName(),
        AddColumnFilter.class.getName(),
        KeepColumnFilter.class.getName(),
        DeleteColumnFilter.class.getName(),
        ExplodeFilter.class.getName(),
        ProgressFilter.class.getName(),
        CacheFilter.class.getName(),
        RandomFilter.class.getName(),
        SequentialFilter.class.getName(),
    };

    public PipelineTask() {
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

    public void setArgs( List argList ) throws ArgException {
        super.setArgs( argList );
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.startsWith( "-" ) && arg.length() > 1 ) {
                if ( arg.equals( "-ifmt" ) ) {
                    it.remove();
                    if ( it.hasNext() ) {
                        inFmt_ = (String) it.next();
                        it.remove();
                        if ( inFmt_ != null && inFmt_.trim().length() > 0 ) {
                            try {
                                getTableFactory().getTableBuilder( inFmt_ );
                            }
                            catch ( TableFormatException e ) {
                                String msg = e.getMessage();
                                if ( msg == null ) {
                                    msg = "Unknown input format " + inFmt_;
                                }
                                String ufrag = "-ifmt <in-format>\n\n"
                                             + "   Known in-formats:";
                                for ( Iterator fmtIt = getTableFactory()
                                                      .getKnownFormats()
                                                      .iterator();
                                      fmtIt.hasNext(); ) {
                                    ufrag += "\n      " + 
                                          ((String) fmtIt.next()).toLowerCase();
                                }
                                throw new ArgException( msg, ufrag );
                            }
                        }
                    }
                    else {
                        throw new ArgException( "No format",
                                                "-ifmt <in-format>" );
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
                                String ufrag = "tpipe <mode-flags>\n" + 
                                               "\n   Known modes:";
                                for ( int j = 0; j < modes_.length; j++ ) {
                                    ufrag += "\n      " 
                                           + getUsageFragment( modes_[ j ] );
                                }
                                throw new ArgException( "Can only specify " +
                                                        "one mode", ufrag );
                            }
                        }
                    }

                    for ( int i = 0; i < filters_.length; i++ ) {
                         ProcessingFilter filter = filters_[ i ];
                         if ( arg.equals( "-" + filter.getName() ) ) {
                             it.remove();
                             try {
                                 pipeline_.add( filter.createStep( it ) );
                             }
                             catch ( ArgException e ) {
                                 if ( e.getUsageFragment() == null ) {
                                     e.setUsageFragment( 
                                         getUsageFragment( filter ) );
                                 }
                                 throw e;
                             }
                         }
                    }
                }
            }
        }
        if ( mode_ == null ) {
            mode_ = new CopyMode();
        }
        try {
            mode_.setArgs( argList );
        }
        catch ( ArgException e ) {
            if ( e.getUsageFragment() == null ) {
                e.setUsageFragment( getUsageFragment( mode_ ) );
            }
            throw e;
        }
        if ( inLoc_ == null && argList.size() > 0 ) {
            String arg = (String) argList.get( 0 );
            if ( arg.startsWith( "-" ) && arg.length() > 1 ) {
                // unknown flag - leave it
            }
            else {
                argList.remove( 0 );
                inLoc_ = arg;
            }
        }
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
            pipeline_.add( 0, new ProgressFilter( System.err ) );
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
            help.append( "      " + getUsageFragment( modes_[ i ] ) + "\n" );
        }

        help.append( "\n   Filter flags - Use any sequence of:\n" );
        for ( int i = 0; i < filters_.length; i++ ) {
            help.append( "      " + getUsageFragment( filters_[ i ] ) + "\n" );
        }

        /* I think we'll skip information about input formats here - 
         * it makes the help too verbose. */
        if ( false ) {
            help.append( "\n   Auto-detected in-formats:\n" );
            for ( Iterator it = getTableFactory().getDefaultBuilders()
                                                .iterator();
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
        }

        /* Return the help string. */
        return help.toString();
    }

    public String[] getSpecificOptions() {
        List opts = new ArrayList();
        opts.add( "[-ifmt <in-format> [-stream]]" );
        opts.add( "<in-table>" );
        opts.add( "<mode-flags>" );
        opts.add( "<filter-flags>" );
        return (String[]) opts.toArray( new String[ 0 ] );
    }

    private static String getUsageFragment( ProcessingMode mode ) {
        String frag = "-" + mode.getName();
        String modeUsage = mode.getModeUsage();
        if ( modeUsage != null ) {
            frag += " " + modeUsage;
        }
        return frag;
    }

    private static String getUsageFragment( ProcessingFilter filter ) {
        String frag = "-" + filter.getName();
        String filterUsage = filter.getFilterUsage();
        if ( filterUsage != null ) {
            frag += " " + filterUsage;
        }
        return frag;
    }

}
