package uk.ac.starlink.ttools.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.mode.ProcessingMode;

/**
 * MapperTask which has a single input table.
 *
 * @author   Mark Taylor
 * @since    14 Sep 2006
 */
public class SingleMapperTask extends MapperTask {

    private final InputTableParameter inTableParam_;
    private final FilterParameter inFilterParam_;

    /**
     * Constructor.
     *
     * @param   mapper   object which defines mapping transformation
     * @param   purpose  one-line description of the purpose of the task
     * @param   outMode  processing mode which determines the destination of
     *          the processed table
     * @param   useInFilter  allow specification of filters for input table
     * @param   useOutFilter allow specification of filters for output table
     */
    public SingleMapperTask( TableMapper mapper, String purpose,
                             ProcessingMode outMode, boolean useInFilter,
                             boolean useOutFilter ) {
        super( mapper, purpose, outMode, useOutFilter );

        List paramList = new ArrayList();

        /* Input table parameter. */
        inTableParam_ = new InputTableParameter( "in" );
        inTableParam_.setPosition( 1 );
        inTableParam_.setUsage( "<table>" );
        inTableParam_.setPrompt( "Location of input table" );
        paramList.add( inTableParam_.getFormatParameter() );
        paramList.add( inTableParam_.getStreamParameter() );
        paramList.add( inTableParam_ );

        /* Input filter parameter. */
        if ( useInFilter ) {
            inFilterParam_ = new FilterParameter( "icmd" );
            inFilterParam_.setPrompt( "Processing command(s) for input table" );
            inFilterParam_.setDescription( new String[] {
                "Commands to operate on the input table,",
                "before any other processing takes place.",
                inFilterParam_.getDescription(),
            } );
            paramList.add( inFilterParam_ );
        }
        else {
            inFilterParam_ = null;
        }

        /* Store full parameter list. */
        paramList.addAll( Arrays.asList( super.getParameters() ) );
        setParameters( (Parameter[]) paramList.toArray( new Parameter[ 0 ] ) );
    }

    protected InputSpec[] getInputSpecs( Environment env )
            throws TaskException {
        return new InputSpec[] {
            new InputSpec( inTableParam_.tableValue( env ),
                           inFilterParam_ == null
                               ? null
                               : inFilterParam_.stepsValue( env ) )
        };
    }
}
