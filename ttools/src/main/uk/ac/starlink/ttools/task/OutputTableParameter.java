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
        setPrompt( "Location of output table" );
        setDefault( "-" );

        setDescription( new String[] {
            "<p>The location of the output table.  This is usually a filename",
            "to write to.",
            "If it is equal to the special value \"-\" (the default)",
            "the output table will be written to standard output.",
            "</p>",
        } );
    }

    public OutputFormatParameter getFormatParameter() {
        return formatParam_;
    }

    /**
     * Sets the value directly from a given TableConsumer.
     *
     * @param  consumer  table consumer  
     */
    public void setValueFromConsumer( TableConsumer consumer ) {
        consumer_ = consumer;
        setStringValue( consumer.toString() );
        setGotValue( true );
    }

    public void setValueFromString( Environment env, String sval ) 
            throws TaskException {
        if ( sval != null ) {
            String loc = sval;
            String fmt = formatParam_.stringValue( env );
            consumer_ = CopyMode.createConsumer( env, loc, fmt );
        }
        super.setValueFromString( env, sval );
    }

    /**
     * Returns a TableConsumer which corresponds to the value of this
     * parameter.
     *
     * @param  env  execution environment
     */
    public TableConsumer consumerValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return consumer_;
    }
}
