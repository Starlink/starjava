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
        formatParam_ = new InputFormatParameter( name + "fmt" );
        streamParam_ = new BooleanParameter( name + "stream" );
        setDefault( "-" );
    }

    /**
     * Returns parameters associated with this one, which affect the
     * way that the input table is acquired.
     *
     * @return   array of parameters associated with this one
     */
    public Parameter[] getAssociatedParameters() {
        return new Parameter[] { formatParam_, streamParam_ };
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
                throw new ExecutionException( e );
            }
        }
        return table_;
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
        final TableBuilder tbuilder = tfact.getTableBuilder( inFmt );
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

    /**
     * Returns a table factory suitable for this parameter.
     *
     * @return  table factory
     */
    private StarTableFactory getTableFactory( Environment env ) {
        return TableEnvironment.getTableFactory( env );
    }

}
