package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.Match1Type;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.join.Match1Mapping;
import uk.ac.starlink.ttools.join.Match1TypeParameter;
import uk.ac.starlink.ttools.join.MatchEngineParameter;

/**
 * Performs an internal (single-table) crossmatch.
 *
 * @author   Mark Taylor
 * @since    16 Nov 2007
 */
public class TableMatch1 extends SingleMapperTask {

    private final MatchEngineParameter matcherParam_;
    private final WordsParameter tupleParam_;
    private final Match1TypeParameter type1Param_;

    /**
     * Constructor.
     */
    public TableMatch1() {
        super( "Performs a crossmatch internal to a single table",
               new ChoiceMode(), true, true );
        List paramList = new ArrayList();

        matcherParam_ = new MatchEngineParameter( "matcher" );
        paramList.add( matcherParam_ );
        paramList.add( matcherParam_.getMatchParametersParameter() );

        tupleParam_ = matcherParam_.createMatchTupleParameter( "" );
        paramList.add( tupleParam_ );

        type1Param_ = new Match1TypeParameter( "action" );
        paramList.add( type1Param_ );

        getParameterList().addAll( 0, paramList );
    }

    protected TableProducer createProducer( Environment env )
            throws TaskException {

        /* Get the matcher. */
        MatchEngine matcher = matcherParam_.matchEngineValue( env );

        /* Find the number and characteristics of the columns required
         * for this matcher. */
        ValueInfo[] tinfos = matcher.getTupleInfos();
        int ncol = tinfos.length;
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < ncol; i++ ) {
            if ( i > 0 ) {
                sbuf.append( ' ' );
            }
            sbuf.append( tinfos[ i ].getName() );
        }
        String colNames = sbuf.toString();

        /* Assemble the array of supplied expressions which will supply 
         * the tuple of values to the matcher. */
        tupleParam_.setRequiredWordCount( ncol );
        tupleParam_.setPrompt( "Match columns (" + colNames + ")" );
        String[] tupleExprs = tupleParam_.wordsValue( env );

        /* Get the matching type. */
        Match1Type type1 = type1Param_.typeValue( env );

        /* Get the logging stream. */
        PrintStream logStrm = env.getErrorStream();

        /* Construct and return a table producer which will do the work. */
        final SingleTableMapping mapping = 
            new Match1Mapping( matcher, type1, tupleExprs, logStrm );
        final TableProducer inProd = createInputProducer( env );
        return new TableProducer() {
            public StarTable getTable() throws IOException, TaskException {
                return mapping.map( inProd.getTable() );
            }
        };
    }
}
