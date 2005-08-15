package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.mode.TableConsumer;

/**
 * Task which maps one or more input tables to an output table.
 * This class provides methods to acquire the table sources and sink;
 * any actual transformation work is done by a separate 
 * {@param TableMapper} object.
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
    private final String usage_;

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
        StringBuffer usage = new StringBuffer();

        /* Input parameters. */
        inTableParams_ = new InputTableParameter[ nIn_ ];
        for ( int i = 0; i < nIn_; i++ ) {
            String suffix = nIn_ == 1 ? "" : Integer.toString( i + 1 );
            inTableParams_[ i ] = new InputTableParameter( "in" + suffix );
            paramList.add( inTableParams_[ i ] );
            addElements( paramList,
                         inTableParams_[ i ].getAssociatedParameters() );
            usage.append( inTableParams_[ i ].getUsage() );
        }

        /* Processing parameters. */
        addElements( paramList, mapper.getParameters() );
        usage.append( mapper.getUsage() );

        /* Output parameters. */
        if ( useOutModes ) {
            OutputModeParameter outParam = new OutputModeParameter( "mode" );
            paramList.add( outParam );
            usage.append( outParam.getUsage() );
            consumerParam_ = outParam;
        }
        else {
            OutputTableParameter outParam = new OutputTableParameter( "out" );
            paramList.add( outParam );
            addElements( paramList, outParam.getAssociatedParameters() );
            usage.append( outParam.getUsage() );
            consumerParam_ = outParam;
        }
        params_ = (Parameter[]) paramList.toArray( new Parameter[ 0 ] );
        usage_ = usage.toString();
    }

    public Parameter[] getParameters() {
        return params_;
    }

    public String getUsage() {
        return null;
    }

    public void invoke( Environment env ) throws TaskException {
        StarTable[] inTables = new StarTable[ nIn_ ];
        for ( int i = 0; i < nIn_; i++ ) {
            inTables[ i ] = inTableParams_[ i ].tableValue( env );
        }
        TableConsumer consumer = consumerParam_.consumerValue( env );
        TableMapping mapping = mapper_.createMapping( env );
        try {
            mapping.mapTables( inTables, new TableConsumer[] { consumer } );
        }
        catch ( IOException e ) {
            throw new ExecutionException( e );
        }
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
}
