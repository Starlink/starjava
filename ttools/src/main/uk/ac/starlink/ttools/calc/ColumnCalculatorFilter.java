package uk.ac.starlink.ttools.calc;

import gnu.jel.CompilationException;
import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.RowPipe;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.filter.ArgException;
import uk.ac.starlink.ttools.filter.BasicFilter;
import uk.ac.starlink.ttools.filter.ProcessingFilter;
import uk.ac.starlink.ttools.filter.ProcessingStep;
import uk.ac.starlink.ttools.jel.JELTable;

/**
 * ProcessingFilter implementation superclass which adds to a table
 * columns produced by a ColumnCalculator.
 *
 * @author   Mark Taylor
 * @since    14 Oct 2011
 */
public abstract class ColumnCalculatorFilter<S> extends BasicFilter {

    private final ColumnCalculator<S> calc_;

    /**
     * Constructor.
     *
     * @param   name  filter name
     * @param   usage  filter usage
     * @param   calc  column calculator to produce results
     */
    public ColumnCalculatorFilter( String name, String usage,
                                   ColumnCalculator<S> calc ) {
        super( name, usage );
        calc_ = calc;
    }

    /**
     * Creates a processing step given a list of JEL expressions
     * corresponding to the input tuple values for a table,
     * and a calculation specification object
     *
     * @param  tupleExpressions  JEL expressions giving input tuple values
     * @param  spec  calculator-specific specification object
     */
    protected ProcessingStep createCalcStep( String[] tupleExpressions,
                                             S spec ) {
        if ( calc_.getTupleInfos().length != tupleExpressions.length ) {
            throw new IllegalArgumentException( "tuple length mismatch" );
        }
        return new CalcStep<S>( calc_, tupleExpressions, spec );
    }

    /**
     * Implements ProcessingStep for this filter.
     */
    private static class CalcStep<S> implements ProcessingStep {
        private final ColumnCalculator calc_;
        private final String[] tupleExprs_;
        private final S spec_;

        /**
         * Constructor.
         *
         * @param  calc  calculator
         * @param  tupleExprs  one JEL expression per input tuple element,
         *                     gives tuple values for each table input row
         * @param  spec    calculator-specific specification object
         */
        CalcStep( ColumnCalculator calc, String[] tupleExprs, S spec ) {
            calc_ = calc;
            tupleExprs_ = tupleExprs;
            spec_ = spec;
        }

        public StarTable wrap( StarTable base ) throws IOException {
            StarTable tupleTable = toTupleTable( base );
            RowPipe cache = new CacheRowPipe();
            calc_.calculateColumns( spec_, tupleTable, cache );
            StarTable addcolTable = cache.waitForStarTable();
            return new JoinStarTable( new StarTable[] { base, addcolTable } );
        }

        /**
         * Turns an input table into a table with exactly one column for each
         * tuple element.
         *
         * @param  base  input table
         * @return  tuple table suitable for submitting to the calculator
         */
        private StarTable toTupleTable( StarTable base ) throws IOException {
            ValueInfo[] tupleInfos = calc_.getTupleInfos();
            int nTuple = tupleInfos.length;
            ColumnInfo[] colInfos = new ColumnInfo[ nTuple ];
            for ( int ic = 0; ic < nTuple; ic++ ) {
                colInfos[ ic ] = new ColumnInfo( tupleInfos[ ic ] );
            }
            try {
                return new JELTable( base, colInfos, tupleExprs_ );
            }
            catch ( CompilationException e ) {
                throw (IOException)
                      new IOException( e.getMessage() ).initCause( e );
            }
        }
    }
}
