package uk.ac.starlink.ttools.plot2.task;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Library;
import java.io.IOException;
import java.util.Arrays;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.StarTableJELRowReader;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.data.AbstractDataSpec;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.UserDataReader;

/**
 * DataSpec implementation that works with JEL expressions.
 * The mask and coord ID values are based on strings which are evaluated
 * as JEL expressions in the context of the DataSpec's table.
 * The constraints on ID equality are therefore met since equal expression
 * strings applied against the same table must yield the same values.
 *
 * @author   Mark Taylor
 * @since    1 Mar 2013
 */
public class JELDataSpec extends AbstractDataSpec {

    private final StarTable table_;
    private final String maskExpr_;
    private final Coord[] coords_;
    private final String[][] userCoordExprs_;
    private final JELKey maskId_;
    private final JELKey[] coordIds_;

    /** Mask ID corresponding to all rows. */
    private static final JELKey ALL_MASK =
        new JELKey( new String[] { new String( "true" ) } );

    /**
     * Constructor.
     *
     * @param  table   table containing data
     * @param  maskExpr   JEL boolean expression giving mask inclusion;
     *                    null may be used to indicate unconditional inclusion
     * @param  coords  coordinate definitions for which columns are required
     * @param  userCoordExprs   nCoord-element array, each element an array of
     *                          JEL expressions corresponding to the user
     *                          values for the cooresponding Coord
     */
    public JELDataSpec( StarTable table, String maskExpr,
                        Coord[] coords, String[][] userCoordExprs )
            throws TaskException {
        int nCoord = coords.length;
        if ( userCoordExprs.length != nCoord ) {
            throw new IllegalArgumentException( "coord count mismatch" );
        }
        table_ = table;
        maskExpr_ = maskExpr;
        coords_ = coords;
        userCoordExprs_ = userCoordExprs;
        maskId_ = maskExpr == null || "true".equals( maskExpr.trim() )
                ? ALL_MASK
                : new JELKey( new String[] { maskExpr } );
        coordIds_ = new JELKey[ nCoord ];
        for ( int ic = 0; ic < nCoord; ic++ ) {
            coordIds_[ ic ] = new JELKey( userCoordExprs[ ic ] );
        }

        /* Dry run of creating a data reader.  This checks that the JEL
         * expressions can be compiled, and throws a TaskException if not. */
        createJELUserDataReader();
    }

    public StarTable getSourceTable() {
        return table_;
    }

    public int getCoordCount() {
        return coords_.length;
    }

    public Object getCoordId( int ic ) {
        return coordIds_[ ic ];
    }

    public Coord getCoord( int ic ) {
        return coords_[ ic ];
    }

    public Object getMaskId() {
        return maskId_;
    }

    public UserDataReader createUserDataReader() {
        try {
            return createJELUserDataReader();
        }
        catch ( TaskException e ) {
            throw new AssertionError( "Well it worked last time." );
        }
    }

    /**
     * Attempts to create a UserDataReader which evaluates the JEL expressions
     * for this spec.  If compilation of the expressions fails, a
     * TaskException is thrown.
     *
     * @return  user data reader
     * @throws TaskException if JEL compilation fails
     */
    private JELUserDataReader createJELUserDataReader() throws TaskException {
        return new JELUserDataReader( table_, maskExpr_, userCoordExprs_,
                                      coords_ );
    }

    /**
     * UserDataReader implementation for use with this class.
     */
    private static class JELUserDataReader implements UserDataReader {
        private final RowSequenceEvaluator evaluator_;
        private final CompiledExpression maskCompex_;
        private final Object[][] userCoordRows_;
        private final CompiledExpression[][] userCoordCompexs_;

        /**
         * Constructor.
         *
         * @param  table   table containing data
         * @param  maskExpr   JEL boolean expression giving mask inclusion
         * @param  userCoordExprs   nCoord-element array, each element an array
         *                          of JEL expressions corresponding to
         *                          the user values for the cooresponding Coord
         * @param  coords    nCoord-element array of coordinate definitions
         * @throws  TaskException   with an informative message
         *                          if compilation fails
         */
        JELUserDataReader( StarTable table, String maskExpr,
                           String[][] userCoordExprs, Coord[] coords )
                throws TaskException {

            /* Set up for JEL compilation against our table. */
            evaluator_ = new RowSequenceEvaluator( table );
            Library lib = JELUtils.getLibrary( evaluator_ );

            /* Compile mask expression. */
            try {
                maskCompex_ = maskExpr == null
                            ? null
                            : JELUtils
                             .compile( lib, table, maskExpr, boolean.class );
            }
            catch ( CompilationException e ) {
                throw new TaskException( "Bad expression \"" + maskExpr + "\"",
                                         e );
            }

            /* Compile coord expressions. */
            int nCoord = userCoordExprs.length;
            userCoordRows_ = new Object[ nCoord ][];
            userCoordCompexs_ = new CompiledExpression[ nCoord ][];
            for ( int ic = 0; ic < nCoord; ic++ ) {
                ValueInfo[] infos = coords[ ic ].getUserInfos();
                String[] ucexprs = userCoordExprs[ ic ];
                int nu = ucexprs.length;
                userCoordRows_[ ic ] = new Object[ nu ];
                CompiledExpression[] compexs = new CompiledExpression[ nu ];
                for ( int iu = 0; iu < nu; iu++ ) {
                    String expr = ucexprs[ iu ];
                    final CompiledExpression compex;
                    if ( expr == null ) {
                        compex = null;
                    }
                    else {
                        try {
                            compex = JELUtils.compile( lib, table, expr );
                        }
                        catch ( CompilationException e ) {
                            throw new TaskException( "Bad Expression \""
                                                   + expr + "\"", e );
                        }
                        Class reqClazz = infos[ iu ].getContentClass();
                        Class exprClazz = compex.getTypeC();
                        if ( ! reqClazz.isAssignableFrom( exprClazz ) ) {
                            String msg = new StringBuffer()
                                .append( "Expression wrong type: " )
                                .append( '"' )
                                .append( expr )
                                .append( '"' )
                                .append( " is " )
                                .append( exprClazz.getName() )
                                .append( " not " )
                                .append( reqClazz.getName() )
                                .toString();
                            throw new TaskException( msg );
                        }
                    }
                    compexs[ iu ] = compex;
                }
                userCoordCompexs_[ ic ] = compexs;
            }
        }

        public boolean getMaskFlag( RowSequence rseq, long irow )
                throws IOException {
            return maskCompex_ == null
                || Boolean.TRUE
                  .equals( evaluator_
                          .evaluateObject( maskCompex_, rseq, irow ) );
        }

        public Object[] getUserCoordValues( RowSequence rseq, long irow,
                                            int icoord )
                throws IOException {
            CompiledExpression[] compexs = userCoordCompexs_[ icoord ];
            int nu = compexs.length;
            Object[] userRow = userCoordRows_[ icoord ];
            for ( int iu = 0; iu < nu; iu++ ) {
                CompiledExpression compex = compexs[ iu ];
                userRow[ iu ] = compex == null
                              ? null
                              : evaluator_.evaluateObject( compex, rseq, irow );
            }
            return userRow;
        }
    }

    /**
     * Object which can evaluate expressions at the current row of a given
     * RowSequence.  The hard work is done by StarTableJELRowReader,
     * from which it inherits.
     * Like the RowSequence it uses, instances of this class are not
     * thread-safe.
     */
    private static class RowSequenceEvaluator extends StarTableJELRowReader {
        private RowSequence rseq_;
        private long irow_;

        /**
         * Constructor.
         *
         * @param   table  table for which this row reader reads data,
         *                 used for expression evaluation
         */
        RowSequenceEvaluator( StarTable table ) {
            super( table );
        }

        @Override
        public long getCurrentRow() {
            return irow_;
        }

        @Override
        protected Object getCell( int icol ) throws IOException {
            return rseq_.getCell( icol );
        }

        /**
         * Evaluates a compiled expression at the current row of a given
         * RowSequence.
         *
         * @param   compex   expression to evaluate
         * @param   rseq   row sequence positioned at the row of interest
         * @param   irow   index of the row of interest
         * @return  expression value
         */
        public Object evaluateObject( CompiledExpression compex,
                                      RowSequence rseq, long irow ) {

            /* Set the internal state of this JELRowReader object so that
             * the overridden getCurrentRow and getCell methods will retrieve
             * the right results. */
            rseq_ = rseq;
            irow_ = irow;

            /* Perform the evaluation. */
            try {
                return evaluate( compex );
            }
            catch ( Throwable e ) {
                return null;
            }
        }
    }

    /**
     * Object used as mask or coord ID for DataSpec.
     * Equality evaluation is based on string equality of an array of one or
     * more JEL expression strings.
     */
    @Equality
    private static class JELKey {
        private final String[] exprs_;

        /**
         * Constructor.
         *
         * @param  exprs   expression strings
         */
        JELKey( String[] exprs ) {
            exprs_ = exprs.clone();
        }

        @Override
        public boolean equals( Object other ) {
            return other instanceof JELKey
                && Arrays.equals( this.exprs_, ((JELKey) other).exprs_ );
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode( exprs_ );
        }
    }
}
