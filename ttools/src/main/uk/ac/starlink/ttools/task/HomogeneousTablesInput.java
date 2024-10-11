package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.filter.ProcessingStep;

/**
 * TablesInput which has a variable number of input tables,
 * treating them all the same as each other.
 * A single, possibly repeated, parameter is used to specify any
 * number of input tables, and a single (not repeated) filter
 * parameter is used to specify a processing pipeline which operates
 * identically on all the inputs.
 *
 * @author   Mark Taylor
 * @since    1 Jul 2010
 */
public class HomogeneousTablesInput implements TablesInput {

    private final InputTablesParameter inTablesParam_;
    private final FilterParameter inFilterParam_;
    private final Parameter<?>[] params_;
    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    /**
     * Constructor.
     *
     * @param   useInFilter  whether preprocessing filters are permitted
     */
    public HomogeneousTablesInput( boolean useInFilter ) {
        List<Parameter<?>> paramList = new ArrayList<Parameter<?>>();

        /* Input tables parameter. */
        inTablesParam_ = new InputTablesParameter( "in" );
        inTablesParam_.setUsage( "<table> [<table> ...]" );
        inTablesParam_.setPrompt( "Location of input table(s)" );
        paramList.add( inTablesParam_ );
        paramList.add( inTablesParam_.getFormatParameter() );
        paramList.add( inTablesParam_.getMultiParameter() );
        paramList.add( inTablesParam_.getStreamParameter() );

        /* Input filter parameter. */
        if ( useInFilter ) {
            inFilterParam_ = new FilterParameter( "icmd" );
            inFilterParam_.setTableDescription( "each input table",
                                                inTablesParam_, Boolean.TRUE );
            paramList.add( inFilterParam_ );
        }
        else {
            inFilterParam_ = null;
        }
        params_ = paramList.toArray( new Parameter<?>[ 0 ] );
    }

    public Parameter<?>[] getParameters() {
        return params_;
    }

    public InputTableSpec[] getInputSpecs( final Environment env )
            throws TaskException {
        ProcessingStep[] steps = inFilterParam_ == null
                               ? null
                               : inFilterParam_.stepsValue( env );
        TableProducer[] tprods = inTablesParam_.objectValue( env );
        final int nIn = tprods.length;
        InputTableSpec[] specs = new InputTableSpec[ nIn ];
        for ( int i = 0; i < nIn; i++ ) {
            final int index = i;
            final TableProducer tprod = tprods[ i ];
            specs[ i ] = new InputTableSpec( tprod.toString(), steps ) {
                public StarTable getInputTable() throws TaskException {
                    try {
                        logger_.config( "Input table " + ( index + 1 )
                                      + "/" + nIn );
                        return tprod.getTable();
                    }
                    catch ( IOException e ) {
                        throw new TaskException( e.getMessage(), e );
                    }
                }
            };
        }
        return specs;
    }

    public InputTablesParameter getInputTableParameter( int i ) {
        return inTablesParam_;
    }

    public FilterParameter getFilterParameter( int i ) {
        return inFilterParam_;
    }
}
