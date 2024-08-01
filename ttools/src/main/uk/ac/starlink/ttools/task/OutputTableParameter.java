package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.mode.CopyMode;

/**
 * Parameter to hold an output table.
 */
public class OutputTableParameter extends Parameter<TableConsumer>
                                  implements TableConsumerParameter {

    private final OutputFormatParameter formatParam_;

    @SuppressWarnings("this-escape")
    public OutputTableParameter( String name ) {
        super( name, TableConsumer.class, true );
        formatParam_ = new OutputFormatParameter( "ofmt" );
        setUsage( "<out-table>" );
        setPrompt( "Location of output table" );
        setStringDefault( "-" );

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

    public TableConsumer stringToObject( Environment env, String sval )
            throws TaskException {
        String loc = sval;
        String fmt = formatParam_.stringValue( env );
        return CopyMode.createConsumer( env, loc, fmt );
    }

    public TableConsumer consumerValue( Environment env )
            throws TaskException {
        return objectValue( env );
    }

    public void setValueFromConsumer( Environment env, TableConsumer consumer )
            throws TaskException {
        setValueFromObject( env, consumer );
    }
}
