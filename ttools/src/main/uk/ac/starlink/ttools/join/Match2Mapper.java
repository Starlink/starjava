package uk.ac.starlink.ttools.join;

import gnu.jel.CompilationException;
import java.io.PrintStream;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.table.join.JoinType;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.task.InputTableSpec;
import uk.ac.starlink.ttools.task.TableMapper;
import uk.ac.starlink.ttools.task.TableMapping;
import uk.ac.starlink.ttools.task.WordsParameter;

/**
 * TableMapper which does the work for pair matching (tmatch2).
 *
 * @author   Mark Taylor
 * @since    2 Sep 2005
 */
public class Match2Mapper implements TableMapper {

    private final MatchEngineParameter matcherParam_;
    private final WordsParameter[] tupleParams_;
    private final JoinTypeParameter joinParam_;
    private final FindModeParameter modeParam_;
    private final Parameter[] duptagParams_;

    /**
     * Constructor.
     */
    public Match2Mapper() {

        matcherParam_ = new MatchEngineParameter( "matcher" );

        tupleParams_ = new WordsParameter[] {
            matcherParam_.createMatchTupleParameter( "1" ),
            matcherParam_.createMatchTupleParameter( "2" ),
        };

        duptagParams_ = new Parameter[ 2 ];
        for ( int i = 0; i < 2; i++ ) {
            int i1 = i + 1;
            duptagParams_[ i ] = new Parameter( "duptag" + i1 );
            duptagParams_[ i ].setUsage( "<trail-string>" );
            duptagParams_[ i ].setPrompt( "Column deduplication string " +
                                          "for table " + i1 );
            duptagParams_[ i ].setDefault( "_" + i1 );
            duptagParams_[ i ].setNullPermitted( true );
            duptagParams_[ i ].setDescription( new String[] {
                "<p>If the same column name appears in both of the input",
                "tables, those columns are renamed in the output table",
                "to avoid ambiguity.",
                "The output column name of such a duplicated column",
                "is formed by appending the value of this parameter",
                "to its name in the input table.",
                "</p>",
            } );
        }

        joinParam_ = new JoinTypeParameter( "join" );
        modeParam_ = new FindModeParameter( "find" );
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            matcherParam_,
            tupleParams_[ 0 ],
            tupleParams_[ 1 ],
            matcherParam_.getMatchParametersParameter(),
            joinParam_,
            modeParam_,
            duptagParams_[ 0 ],
            duptagParams_[ 1 ],
        };
    }

    public TableMapping createMapping( Environment env, int nin )
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

        /* Assemble the arrays of supplied expressions which will supply
         * the values to the matcher for each table. */
        String[][] tupleExprs = new String[ 2 ][];
        for ( int i = 0; i < 2; i++ ) {
            int i1 = i + 1;
            tupleParams_[ i ].setRequiredWordCount( ncol );
            tupleParams_[ i ].setPrompt( "Table " + i1 + " match columns ("
                                       + colNames + ")" );
            tupleExprs[ i ] = tupleParams_[ i ].wordsValue( env );
        }

        /* Get other parameter values. */
        JoinType join = joinParam_.joinTypeValue( env );
        boolean bestOnly = modeParam_.bestOnlyValue( env );
        JoinFixAction[] fixacts = new JoinFixAction[ 2 ];
        for ( int i = 0; i < 2; i++ ) {
            String duptag = duptagParams_[ i ].stringValue( env );
            fixacts[ i ] = ( duptag == null || duptag.trim().length() == 0 )
                ? JoinFixAction.NO_ACTION
                : JoinFixAction.makeRenameDuplicatesAction( duptag,
                                                            false, true );
        }
        PrintStream logStrm = env.getErrorStream();

        /* Construct and return a mapping based on this lot. */
        return new Match2Mapping( matcher, tupleExprs[ 0 ], tupleExprs[ 1 ],
                                  join, bestOnly,
                                  fixacts[ 0 ], fixacts[ 1 ], logStrm );
    }
}
