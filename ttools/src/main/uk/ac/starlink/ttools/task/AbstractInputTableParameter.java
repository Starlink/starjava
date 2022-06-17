package uk.ac.starlink.ttools.task;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.logging.Logger;
import uk.ac.starlink.table.OnceRowPipe;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.UnrepeatableSequenceException;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.util.DataSource;

/**
 * Parameter used to select a table for input.  This abstract superclass
 * provides general facilities for input tables; there are concrete
 * subclasses for single and multiple input table values.
 *
 * @author   Mark Taylor
 * @since    15 Sep 2006
 */
public abstract class AbstractInputTableParameter<T> extends Parameter<T> {

    private InputFormatParameter formatParam_;
    private BooleanParameter streamParam_;
    private final boolean allowSystem_ = true;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );
    private static final String[] KNOWN_PREFIXES =
        { "in", "upload", "animate" };
    
    /**
     * Constructor. 
     *
     * @param  name  parameter name
     */
    protected AbstractInputTableParameter( String name, Class<T> clazz ) {
        super( name, clazz, true );
        String suffix = "";
        for ( String prefix : Arrays.asList( KNOWN_PREFIXES ) ) {
            if ( name.startsWith( prefix ) ) {
                suffix = name.substring( prefix.length() );
                break;
            }
        }
        char labelChar = name.charAt( 0 );
        formatParam_ = new InputFormatParameter( labelChar + "fmt" + suffix );
        streamParam_ = new BooleanParameter( labelChar + "stream" + suffix );
        streamParam_.setBooleanDefault( false );
        setTableDescription( "the input table" );
    }

    /**
     * Returns the parameter which deals with input format.
     *
     * @return  format parameter
     */
    public InputFormatParameter getFormatParameter() {
        return formatParam_;
    }

    /**
     * Returns the stream toggle parameter associated with this one.
     *
     * @return  stream parameter
     */
    public BooleanParameter getStreamParameter() {
        return streamParam_;
    }

    /**
     * Sets the wording used to refer to the input table in parameter
     * descriptions.  This parameter and the associated parameters
     * (format and stream) are affected.
     * If not set, the wording "the input table" is used.
     *
     * @param  inDescrip  text to replace "the input table"
     */
    public final void setTableDescription( String inDescrip ) {
        setDescription( new String[] {
            "<p>The location of " + inDescrip + ".",
            "This may take one of the following forms:",
            getLocationFormList( formatParam_ ),
            "In any case, compressed data in one of the supported compression",
            "formats (gzip, Unix compress or bzip2) will be decompressed",
            "transparently.",
            "</p>",
        } );
        streamParam_.setDescription( new String[] {
            "<p>If set true, " + inDescrip,
            "specified by the <code>" + getName() + "</code> parameter",
            "will be read as a stream.",
            "It is necessary to give the ",
            "<code>" + formatParam_.getName() + "</code> parameter",
            "in this case.",
            "Depending on the required operations and processing mode,",
            "this may cause the read to fail (sometimes it is necessary",
            "to read the table more than once).",
            "It is not normally necessary to set this flag;",
            "in most cases the data will be streamed automatically",
            "if that is the best thing to do.",
            "However it can sometimes result in less resource usage when",
            "processing large files in certain formats (such as VOTable).",
            "This parameter is ignored for scheme-specified tables.",
            "</p>",
        } );
        formatParam_.setTableDescription( inDescrip, this );
    }

    /**
     * Constructs a StarTable from a location string given the current
     * state of this parameter and its associated parameter values.
     *
     * @param   env  execution environment
     * @param   loc  table location string
     * @return  table at loc
     */
    protected StarTable makeTable( Environment env, String loc )
            throws TaskException {
        String fmt = getFormatParameter().stringValue( env );
        boolean stream = getStreamParameter().booleanValue( env );
        StarTableFactory tfact = LineTableEnvironment.getTableFactory( env );
        try {
            return makeTable( loc, fmt, stream, tfact );
        }
        catch ( EOFException e ) {
            throw new ExecutionException( "Premature end of file", e );
        }
        catch ( IOException e ) {
            String msg = e.getMessage();
            if ( msg == null || msg.trim().length() == 0 ) {
                msg = e.toString();
            }
            throw new ExecutionException( msg, e );
        }
    }

    /**
     * Reads a table given fixed values for the various parameters.
     *
     * @param  loc  table location
     * @param  fmt  input format string
     * @param  stream  true for streamed input
     * @param  tfact  table factory
     * @return   table loaded
     */
    public StarTable makeTable( String loc, String fmt, boolean stream,
                                StarTableFactory tfact )
            throws IOException, TaskException {
        if ( loc.equals( "-" ) ) {
            InputStream in =
                new BufferedInputStream( DataSource
                                        .getInputStream( loc, allowSystem_ ) );
            return getStreamedTable( tfact, in, fmt, null );
        }
        else if ( stream ) {
            return getStreamedTable( tfact,
                                     DataSource
                                    .makeDataSource( loc, allowSystem_ ),
                                     fmt );
        }
        else {
            return tfact.makeStarTable( loc, fmt );
        }
    }

    /**
     * Constructs an array of tables from a location string given the current
     * state of this parameter and its associated parameter values.
     * The returned number of tables may only be plural if the table
     * format is capable of supplying multiple tables.
     *
     * @param   env  execution environment
     * @param   loc  table location string
     * @return   tables at loc
     */
    protected StarTable[] makeTables( Environment env, String loc )
            throws TaskException {
        String fmt = getFormatParameter().stringValue( env );
        boolean stream = getStreamParameter().booleanValue( env );
        StarTableFactory tfact = LineTableEnvironment.getTableFactory( env );
        String streamWarning =
            "Can't currently stream multiple tables" +
            " (would need MultiTableBuilder.streamTables method)";
        try {
            if ( loc.equals( "-" ) ) {
                logger_.warning( streamWarning );
                InputStream in =
                    new BufferedInputStream( DataSource
                                            .getInputStream( loc,
                                                             allowSystem_ ) );
                return new StarTable[] {
                    getStreamedTable( tfact, in, fmt, null )
                };
            }
            else {
                if ( stream ) {
                    logger_.warning( streamWarning );
                }
                DataSource datsrc = DataSource.makeDataSource( loc );
                return Tables.tableArray( tfact.makeStarTables( datsrc, fmt ) );
            }
        }
        catch ( EOFException e ) {
            throw new ExecutionException( "Premature end of file", e );
        }
        catch ( IOException e ) {
            String msg = e.getMessage();
            if ( msg == null || msg.trim().length() == 0 ) {
                msg = e.toString();
            }
            throw new ExecutionException( msg, e );
        }
    }

    /**
     * Returns a one-shot StarTable derived from an input stream.
     * This will allow only a single RowSequence to be taken from it.
     * It may fail with a TableFormatException for input formats
     * which can't be made from a stream.
     *
     * @param   tfact   table factory
     * @param   in  input stream
     * @param   inFmt  input format
     * @param   pos   position in data source (may be null for first table)
     * @return  one-shot startable
     */
    private StarTable getStreamedTable( StarTableFactory tfact,
                                        final InputStream in, String inFmt,
                                        final String pos )
            throws IOException, TaskException {
        if ( inFmt == null ||
             inFmt.equals( StarTableFactory.AUTO_HANDLER ) ) {
            String msg = "Must specify input format for streamed table";
            throw new ParameterValueException( getFormatParameter(), msg );
        }
        final TableBuilder tbuilder = tfact.getTableBuilder( inFmt );
        assert tbuilder != null;
        final OnceRowPipe streamStore = new OnceRowPipe( 1024 );
        Thread streamer = new Thread( "Table Streamer" ) {
            public void run() {
                try {
                    tbuilder.streamStarTable( in, streamStore, pos );
                }
                catch ( IOException e ) {
                    streamStore.setError( e );
                }
                finally {
                    try {
                        in.close();
                    }
                    catch ( IOException e ) {
                        // never mind
                    }
                }
            }
        };
        streamer.setDaemon( true );
        streamer.start();
        return streamStore.waitForStarTable();
    }

    /**
     * Returns a StarTable which re-reads the input stream every time
     * the data are required.  This has similar efficiency characteristics to
     * {@link #getStreamedTable(uk.ac.starlink.table.StarTableFactory,
     *                          java.io.InputStream,
     *                          java.lang.String,java.lang.String)
     *        getStreamedTable}
     * but doesn't fall over if more than one RowSequence is taken out on it.
     * It needs a DataSource not an InputStream of course, since the
     * stream of bytes has to be re-readable.
     *
     * @param   tfact   table factory
     * @param   datsrc  data source
     * @return  re-readable streaming startable
     */
    private StarTable getStreamedTable( final StarTableFactory tfact,
                                        final DataSource datsrc,
                                        final String inFmt )
            throws IOException, TaskException {
        InputStream in1 = new BufferedInputStream( datsrc.getInputStream() );
        final String pos = datsrc.getPosition();
        StarTable t1 = getStreamedTable( tfact, in1, inFmt, pos );
        return new WrapperStarTable( t1 ) {
            public RowSequence getRowSequence() throws IOException {
                try {
                    return super.getRowSequence();
                }
                catch ( UnrepeatableSequenceException e ) {
                    InputStream in =
                        new BufferedInputStream( datsrc.getInputStream() );
                    StarTable t2;
                    try {
                        t2 = getStreamedTable( tfact, in, inFmt, pos );
                    }
                    catch ( TaskException e1 ) {
                        /* We've already got this once before, so it shouldn't
                         * be able to fail with a TaskException this time. */
                        throw new AssertionError( e1 );
                    }
                    return t2.getRowSequence();
                }
            }
            public RowSplittable getRowSplittable() throws IOException {
                return Tables.getDefaultRowSplittable( this );
            }
        };
    }

    /**
     * Returns an XML list element enumerating the forms in which a
     * single table may be specified.
     *
     * @param  fmtParam  associated input format parameter
     * @return  ul element
     */
    public static String getLocationFormList( InputFormatParameter fmtParam ) {
        return DocUtils.join( new String[] {
            "<ul>",
            "<li>A filename.</li>",
            "<li>A URL.</li>",
            "<li>The special value \"<code>-</code>\",",
            "    meaning standard input.",
            "    In this case the input format must be given explicitly",
            "    using the <code>" + fmtParam.getName() + "</code>",
            "    parameter.",
            "    Note that not all formats can be streamed in this way.</li>",
            "<li>A <ref id='TableScheme'>scheme specification</ref>",
            "    of the form",
            "    <code>:&lt;scheme-name&gt;:&lt;scheme-args&gt;</code>.</li>",
            "<li>A system command line with",
            "    either a \"<code>&lt;</code>\" character at the start,",
            "    or a \"<code>|</code>\" character at the end",
            "    (\"<code>&lt;syscmd</code>\" or",
            "     \"<code>syscmd|</code>\").",
            "    This executes the given pipeline and reads from its",
            "    standard output.",
            "    This will probably only work on unix-like systems.</li>",
            "</ul>",
        } );
    }
}
