package uk.ac.starlink.ttools.task;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.MultiStarTableWriter;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.OutputStreamParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.util.Destination;

/**
 * Task which writes multiple tables to a single output file.
 * Concrete subclasses must implement the
 * {@link #createTableSequence(uk.ac.starlink.task.Environment)} method.
 *
 * @author   Mark Taylor
 * @since    6 Jul 2010
 */
public abstract class MultiOutputTask implements Task {

    private final String purpose_;
    private final List<Parameter> paramList_;
    private final OutputStreamParameter outParam_;
    private final OutputFormatParameter ofmtParam_;

    /**
     * Constructor.
     *
     * @param  purpose  task purpose
     */
    protected MultiOutputTask( String purpose ) {
        purpose_ = purpose;
        paramList_ = new ArrayList<Parameter>();

        outParam_ = new OutputStreamParameter( "out" );
        paramList_.add( outParam_ );

        ofmtParam_ = new MultiOutputFormatParameter( "ofmt" );
        paramList_.add( ofmtParam_ );
    }

    public String getPurpose() {
        return purpose_;
    }

    public Parameter[] getParameters() {
        return paramList_.toArray( new Parameter[ 0 ] );
    }

    /**
     * Returns the parameter list for this task; it may be modified.
     *
     * @return  parameter list
     */
    protected List<Parameter> getParameterList() {
        return paramList_;
    }

    /**
     * Interrogates the environment to produce a sequence of tables which
     * will be written as the output of this task.
     *
     * @param  env  execution environment
     * @return   sequence of tables to write
     */
    protected abstract TableSequence createTableSequence( Environment env )
            throws TaskException;

    public Executable createExecutable( Environment env )
             throws TaskException {
         final TableSequence tseq = createTableSequence( env );
         final String loc = outParam_.stringValue( env );
         String ofmt = ofmtParam_.stringValue( env );
         final StarTableOutput sto = LineTableEnvironment.getTableOutput( env );
         StarTableWriter writer;
         try {
             writer = sto.getHandler( ofmt, loc );
         }
         catch ( TableFormatException e ) {
             String msg = "Unknown output format";
             throw new ParameterValueException( ofmtParam_, msg, e );
         }
         if ( ! ( writer instanceof MultiStarTableWriter ) ) {
             String msg = "Format " + ofmt + " can't write multiple tables";
             throw new ParameterValueException( ofmtParam_, msg );
         }
         final MultiStarTableWriter multiWriter = (MultiStarTableWriter) writer;
         return new Executable() {
             public void execute() throws IOException {
                 multiWriter.writeStarTables( tseq, loc, sto );
             }
         };
    }

    /**
     * Constructs a table sequence based on an array of input table
     * specifications.
     *
     * @param   inSpecs   array of input tables
     */
    public static TableSequence createTableSequence( InputTableSpec[] inSpecs )
            throws TaskException {
        final Iterator<InputTableSpec> it = Arrays.asList( inSpecs ).iterator();
        return new TableSequence() {
            private int index;
            public StarTable nextTable() throws IOException {
                if ( it.hasNext() ) {
                    index++;
                    InputTableSpec inSpec = it.next();
                    try {
                        return inSpec.getWrappedTable();
                    }
                    catch ( TaskException e ) {
                        throw (IOException)
                              new IOException( "Load error for table #" + index
                                             + ": " + inSpec.getLocation() )
                             .initCause( e );
                    }
                }
                else {
                    return null;
                }
            }
        };
    }
}
