package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableConsumer;

/**
 * Task which maps one or more input tables to an output table.
 * This class provides methods to acquire the table sources and sink;
 * any actual transformation work is done by a separate 
 * {@link TableMapper} object.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public class MapperTask implements Task {

    private final TableMapper mapper_;
    private final int nIn_;
    private final InputTableParameter[] inTableParams_;
    private final TableConsumerParameter consumerParam_;
    private final Parameter[] params_;

    /**
     * Constructor.
     *
     * @param   mapper   object which defines mapping transformation
     * @param   useOutModes  true iff you want modes other than 
     *          {@link uk.ac.starlink.ttools.mode.CopyMode} (writing the
     *          table out) to be available
     */
    public MapperTask( TableMapper mapper, boolean useOutModes ) {
        mapper_ = mapper;
        nIn_ = mapper.getInCount();
        List paramList = new ArrayList();

        /* Input parameters. */
        inTableParams_ = new InputTableParameter[ nIn_ ];
        if ( nIn_ == 1 ) {
            inTableParams_[ 0 ] = new InputTableParameter( "in" );
            inTableParams_[ 0 ].setUsage( "<table>" );
            inTableParams_[ 0 ].setPrompt( "Location of input table" );
            paramList.add( inTableParams_[ 0 ] );
        }
        else {
            for ( int i = 0; i < nIn_; i++ ) {
                String suffix = nIn_ == 1 ? "" : Integer.toString( i + 1 );
                inTableParams_[ i ] = new InputTableParameter( "in" + suffix );
                inTableParams_[ i ].setUsage( "<table" + suffix + ">" );
                inTableParams_[ i ].setPrompt( "Location of " + getOrdinal( i )
                                               + " input table" );
                paramList.add( inTableParams_[ i ] );
            }
        }
        for ( int i = 0; i < nIn_; i++ ) {
            inTableParams_[ i ].setPosition( i + 1 );
            addElements( paramList,
                         inTableParams_[ i ].getAssociatedParameters() );
        }

        /* Processing parameters. */
        addElements( paramList, mapper.getParameters() );

        /* Output parameters. */
        if ( useOutModes ) {
            OutputModeParameter modeParam = new OutputModeParameter( "mode" );
            paramList.add( modeParam );
            consumerParam_ = modeParam;
        }
        else {
            OutputTableParameter outParam = new OutputTableParameter( "out" );
            paramList.add( outParam );
            paramList.add( outParam.getFormatParameter() );
            consumerParam_ = outParam;
        }
        params_ = (Parameter[]) paramList.toArray( new Parameter[ 0 ] );
    }

    public Parameter[] getParameters() {
        return params_;
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        final StarTable[] inTables = new StarTable[ nIn_ ];
        for ( int i = 0; i < nIn_; i++ ) {
            inTables[ i ] = inTableParams_[ i ].tableValue( env );
        }
        final TableConsumer consumer = consumerParam_.consumerValue( env );
        final TableMapping mapping = mapper_.createMapping( env );
        return new Executable() {
            public void execute() throws IOException {
                mapping.mapTables( inTables, new TableConsumer[] { consumer } );
            }
        };
    }

    /**
     * Convenience method to add parameters to a List.
     *
     * @param   list   list to augment
     * @param   params  array of parameters to add
     */
    private static void addElements( List list, Parameter[] params ) {
        for ( int i = 0; i < params.length; i++ ) {
            list.add( params[ i ] );
        }
    }

    /**
     * Returns the string representation of the ordinal number corresponding
     * to a given integer.
     *
     * @param  i  number
     * @return   ordinal
     */
    private static String getOrdinal( int i ) {
        switch ( i ) {
            case 1: return "first";
            case 2: return "second";
            case 3: return "third";
            case 4: return "fourth";
            case 5: return "fifth";
            default: return "next";
        }
    }
}
