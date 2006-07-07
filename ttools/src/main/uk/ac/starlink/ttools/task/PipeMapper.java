package uk.ac.starlink.ttools.task;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.InputStreamParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.filter.ProcessingStep;
import uk.ac.starlink.ttools.filter.StepFactory;

/**
 * TableMapper which does the work for the tpipe command. 
 * It can stack several processing filters to edit the table.
 *
 * @author   Mark Taylor
 * @since    16 Aug 2005
 */
public class PipeMapper implements TableMapper {

    private final InputStreamParameter scriptParam_;
    private final FilterParameter stepParam_;

    public PipeMapper() {
        scriptParam_ = new FilterScriptParameter( "script" );
        scriptParam_.setUsage( "<script-file>" );
        scriptParam_.setNullPermitted( true );
        scriptParam_.setPrompt( "File containing table filter commands" );


        stepParam_ = new FilterParameter( "cmd" );
        stepParam_.setNullPermitted( true );
        stepParam_.setPrompt( "Command describing a table processing step" );

        scriptParam_.setDescription( new String[] {
            "Location of a file containing table processing commands.",
            "Each line of this file contains one of the filter commands",
            "described in <ref id=\"filterSteps\"/>.",
            "The sequence of commands given by the lines of this file",
            "defines the processing pipeline which is performed on the table.",
            "The",
            "<code>" + scriptParam_.getName() + "</code> and",
            "<code>" + stepParam_.getName() + "</code>",
            "flags should not be mixed in the same invocation.",
        } );

        stepParam_.setDescription( new String[] {
            "Text of table processing commands.",
            stepParam_.getDescription(),
            "The",
            "<code>" + scriptParam_.getName() + "</code> and",
            "<code>" + stepParam_.getName() + "</code>",
            "flags should not be mixed in the same invocation.",
        } );
    }

    public int getInCount() {
        return 1;
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            scriptParam_,
            stepParam_,
        };
    }

    public TableMapping createMapping( Environment env ) throws TaskException {
        ProcessingStep[] steps;

        /* See if a script has been specified. */
        try {
            InputStream in = scriptParam_.inputStreamValue( env );
            if ( in != null ) {
                try {
                    steps = readSteps( in );
                }
                finally {
                    in.close();
                }
            }
            else {
                steps = stepParam_.stepsValue( env );
            }
        }
        catch ( IOException e ) {
            throw new UsageException( e.getMessage(), e );
        }
        return new PipeMapping( steps );
    }

    /**
     * Reads a sequence of processing steps from an external input stream.
     * These should be arranged one per line.
     *
     * @param   istrm  input stream
     * @return  array of processing steps
     */
    private static ProcessingStep[] readSteps( InputStream istrm )
            throws TaskException {
        try {
            BufferedReader in = 
                new BufferedReader( new InputStreamReader( istrm ) );
            List stepList = new ArrayList();

            for ( String line; ( line = in.readLine() ) != null; ) {
                ProcessingStep step = StepFactory.getInstance()
                                                 .createStep( line );
                if ( step != null ) {
                    stepList.add( step );
                }
            }
            return (ProcessingStep[]) 
                   stepList.toArray( new ProcessingStep[ 0 ] );
        }
        catch ( IOException e ) {
            throw new ExecutionException( "Error reading script", e );
        }
    }

    /**
     * Class which implements the input-to-output table mapping itself.
     */
    private static class PipeMapping implements TableMapping {

        final ProcessingStep[] steps_;

        PipeMapping( ProcessingStep[] steps ) {
            steps_ = steps;
        }

        public void mapTables( StarTable[] in, TableConsumer[] out )
                throws IOException {
            if ( in.length != 1 || out.length != 1 ) {
                throw new IllegalArgumentException();
            }
            StarTable table = in[ 0 ];
            for ( int i = 0; i < steps_.length; i++ ) {
                table = steps_[ i ].wrap( table );
            }
            out[ 0 ].consume( table );
        }
    }
}
