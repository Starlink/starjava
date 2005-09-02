package uk.ac.starlink.ttools.task;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.StreamRowStore;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.util.DataSource;

/**
 * Parameter used to select a table for input.
 */
public class InputTableParameter extends Parameter {

    private InputFormatParameter formatParam_;
    private BooleanParameter streamParam_;
    private StarTable table_;
    
    public InputTableParameter( String name ) {
        super( name );
        setUsage( "<in-table>" );
        String suffix = name.startsWith( "in" ) ? name.substring( 2 ) : "";
        formatParam_ = new InputFormatParameter( "ifmt" + suffix );
        streamParam_ = new BooleanParameter( "istream" + suffix );
        setDefault( "-" );

        setDescription( new String[] {
            "The location of the input table.",
            "This is usually a filename or URL, and may point to a file",
            "compressed in one of the supported compression formats",
            "(Unix compress, gzip or bzip2).",
            "If it is omitted, or equal to the special value \"-\",",
            "the input table will be read from standard input.",
            "In this case the input format must be given explicitly",
            "using the <code>" + formatParam_.getName() + "</code> parameter.",
        } );

        streamParam_.setDescription( new String[] {
            "If set true, the <code>" + getName() + "</code> table",
            "will be read as a stream.",
            "It is necessary to give the ",
            "<code>" + formatParam_.getName() + "</code> parameter",
            "in this case.",
            "Depending on the required operations and processing mode,",
            "this may cause the read to fail (sometimes it is necessary",
            "to read the input table more than once).",
            "It is not normally useful or necessary to set this flag -",
            "in most cases the data will be streamed automatically",
            "if that is the best thing to do.",
        } );
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
     * Returns the input table which has been selected by this parameter.
     *
     * @param  env  execution environment
     */
    public StarTable tableValue( Environment env ) throws TaskException {
        checkGotValue( env );
        if ( table_ == null ) {
            try {
                String loc = stringValue( env );
                String fmt = formatParam_.stringValue( env );
                boolean stream = streamParam_.booleanValue( env );
                StarTableFactory tfact = getTableFactory( env );
                if ( stream || loc.equals( "-" ) ) {
                    InputStream in =
                        new BufferedInputStream( DataSource
                                                .getInputStream( loc ) );
                    table_ = getStreamedTable( tfact, in, fmt );
                }
                else {
                    table_ = tfact.makeStarTable( loc, fmt );
                }
            }
            catch ( IOException e ) {
                throw new ExecutionException( e.getMessage(), e );
            }
        }
        return table_;
    }

    /**
     * Sets the table value of this parameter directly.
     *
     * @param   table
     */
    void setValueFromTable( StarTable table ) {
        table_ = table;
        setGotValue( true );
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
    StarTable getStreamedTable( StarTableFactory tfact, final InputStream in,
                                String inFmt ) throws IOException {
        if ( inFmt == null ) {
            throw new IOException(
                "Must specify input format for streamed table" );
        }
        final TableBuilder tbuilder = tfact.getTableBuilder( inFmt );
        assert tbuilder != null;
        final StreamRowStore streamStore = new StreamRowStore( 1024 );
        Thread streamer = new Thread( "Table Streamer" ) {
            public void run() {
                try {
                    tbuilder.streamStarTable( in, streamStore, null );
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
     * Returns a table factory suitable for this parameter.
     *
     * @return  table factory
     */
    private StarTableFactory getTableFactory( Environment env ) {
        return TableEnvironment.getTableFactory( env );
    }

}
