package uk.ac.starlink.ttools.task;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableConsumer;

/**
 * Task which copies a single table from input to output.
 *
 * @author   Mark Taylor 
 * @since    15 Aug 2005
 */
public class TableCopy implements Task {

    private final InputTableParameter inParam_;
    private final InputFormatParameter ifmtParam_;
    private final OutputTableParameter outParam_;
    private final OutputFormatParameter ofmtParam_;

    public TableCopy() {
        inParam_ = new InputTableParameter( "in" );
        inParam_.setPrompt( "Location of input table" );
        inParam_.setPosition( 1 );

        outParam_ = new OutputTableParameter( "out" );
        outParam_.setPosition( 2 );

        ifmtParam_ = inParam_.getFormatParameter();
        ifmtParam_.setPrompt( "Format of input table" );
        ifmtParam_.setName( "ifmt" );

        ofmtParam_ = outParam_.getFormatParameter();
        ofmtParam_.setPrompt( "Format of output table" );
        ofmtParam_.setName( "ofmt" );
    }

    public String getPurpose() {
        return "Converts between table formats";
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            inParam_,
            outParam_,
            ifmtParam_,
            ofmtParam_,
        };
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        final StarTable inTable = inParam_.tableValue( env );
        final TableConsumer consumer = outParam_.consumerValue( env );
        return new Executable() {
            public void execute() throws IOException {
                consumer.consume( inTable );
            }
        };
    }
}
