package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.mode.CopyMode;

/**
 * Parameter to hold an output table.
 */
public class OutputTableParameter extends Parameter
                                  implements TableConsumerParameter {

    private final OutputFormatParameter formatParam_;
    private TableConsumer consumer_;

    public OutputTableParameter( String name ) {
        super( name );
        formatParam_ = new OutputFormatParameter( "ofmt" );
        setUsage( "<out-table>" );
        setDefault( "-" );

        setDescription( new String[] {
            "The location of the output table.  This is usually a filename",
            "to write to.",
            "If it is equal to the special value \"-\" (the default)",
            "the output table will be written to standard output.",
        } );
    }

    public OutputFormatParameter getFormatParameter() {
        return formatParam_;
    }

    /**
     * Returns a TableConsumer which corresponds to the value of this
     * parameter.
     *
     * @param  env  execution environment
     */
    public TableConsumer consumerValue( Environment env ) throws TaskException {
        checkGotValue( env );
        if ( consumer_ == null ) {
            String loc = stringValue( env );
            String fmt = formatParam_.stringValue( env );
            consumer_ = CopyMode.createConsumer( env, loc, fmt );
        }
        return consumer_;
    }
}
