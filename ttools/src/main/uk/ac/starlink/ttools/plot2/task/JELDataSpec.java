package uk.ac.starlink.ttools.plot2.task;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Library;
import java.io.IOException;
import java.util.Arrays;
import uk.ac.starlink.table.DefaultValueInfo;
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
import uk.ac.starlink.ttools.plot2.data.Input;
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
    private final CoordValue[] coordValues_;
    private final ValueInfo[][] userCoordInfos_;
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
     * @param  coordValues  coordinate definitions for which columns
     *                      are required, along with the JEL expressions
     *                      for their values
     */
    public JELDataSpec( StarTable table, String maskExpr,
                        CoordValue[] coordValues )
            throws TaskException {
        table_ = table;
        maskExpr_ = maskExpr;
        coordValues_ = coordValues;
        int nCoord = coordValues.length;
        maskId_ = maskExpr == null || "true".equals( maskExpr.trim() )
                ? ALL_MASK
                : new JELKey( new String[] { maskExpr } );
        coordIds_ = new JELKey[ nCoord ];
        for ( int ic = 0; ic < nCoord; ic++ ) {
            coordIds_[ ic ] = new JELKey( coordValues[ ic ].getExpressions() );
        }

        /* Dry run of creating a data reader.  This checks that the JEL
         * expressions can be compiled, and throws a TaskException if not. */
        JELUserDataReader dataRdr = createJELUserDataReader();

        /* Extract and store column metadata from the data reader. */
        userCoordInfos_ = new ValueInfo[ nCoord ][];
        for ( int ic = 0; ic < nCoord; ic++ ) {
            int nu = coordValues[ ic ].getExpressions().length;
            userCoordInfos_[ ic ] = new ValueInfo[ nu ];
            for ( int iu = 0; iu < nu; iu++ ) {
                userCoordInfos_[ ic ][ iu ] =
                    dataRdr.userCoordReaders_[ ic ][ iu ].getValueInfo();
            }
        }
    }

    public StarTable getSourceTable() {
        return table_;
    }

    public int getCoordCount() {
        return coordValues_.length;
    }

    public Object getCoordId( int ic ) {
        return coordIds_[ ic ];
    }

    public Coord getCoord( int ic ) {
        return coordValues_[ ic ].getCoord();
    }

    public Object getMaskId() {
        return maskId_;
    }

    public ValueInfo[] getUserCoordInfos( int ic ) {
        return userCoordInfos_[ ic ];
    }

    public UserDataReader createUserDataReader() {
        try {
            return createJELUserDataReader();
        }
        catch ( TaskException e ) {
            throw new AssertionError( "Well it worked last time." );
        }
    }

    public boolean isCoordBlank( int icoord ) {
        for ( String expr : coordValues_[ icoord ].getExpressions() ) {
            if ( expr != null && expr.trim().length() > 0 ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the user input strings used to supply the value for a given
     * coordinate in this DataSpec.
     *
     * @param  ic  coordinate index
     * @return   array of JEL expressions entered by the user to provide
     *           data for the coordinate
     */
    public String[] getCoordExpressions( int ic ) {
        return coordIds_[ ic ].exprs_.clone();
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
        return new JELUserDataReader( table_, maskExpr_, coordValues_ );
    }

    /**
     * UserDataReader implementation for use with this class.
     */
    private static class JELUserDataReader implements UserDataReader {
        private final ValueReader maskReader_;
        private final ValueReader[][] userCoordReaders_;
        private final Object[][] userCoordRows_;

        /**
         * Constructor.
         *
         * @param  table   table containing data
         * @param  maskExpr   JEL boolean expression giving mask inclusion
         * @param  coordValues   coordinate definitions with expressions
         * @throws  TaskException   with an informative message
         *                          if compilation fails
         */
        JELUserDataReader( StarTable table, String maskExpr,
                           CoordValue[] coordValues )
                throws TaskException {

            /* Set up for JEL compilation against our table. */
            RowSequenceEvaluator evaluator = new RowSequenceEvaluator( table );
            Library lib = JELUtils.getLibrary( evaluator );

            /* Compile mask expression. */
            maskReader_ = createValueReader( maskExpr, table, evaluator, lib,
                                             Boolean.TRUE, boolean.class );

            /* Compile coord expressions. */
            int nCoord = coordValues.length;
            userCoordRows_ = new Object[ nCoord ][];
            userCoordReaders_ = new ValueReader[ nCoord ][];
            for ( int ic = 0; ic < nCoord; ic++ ) {
                CoordValue coordVal = coordValues[ ic ];
                Input[] inputs = coordVal.getCoord().getInputs();
                String[] ucexprs = coordVal.getExpressions();
                int nu = ucexprs.length;
                userCoordRows_[ ic ] = new Object[ nu ];
                ValueReader[] vrdrs = new ValueReader[ nu ];
                for ( int iu = 0; iu < nu; iu++ ) {
                    vrdrs[ iu ] =
                        createValueReader( ucexprs[ iu ], table, evaluator, 
                                           lib, null,
                                           inputs[ iu ].getValueClass() );
                }
                userCoordReaders_[ ic ] = vrdrs;
            }
        }

        public boolean getMaskFlag( RowSequence rseq, long irow )
                throws IOException {
            return Boolean.TRUE.equals( maskReader_.readValue( rseq, irow ) );
        }

        public Object[] getUserCoordValues( RowSequence rseq, long irow,
                                            int icoord )
                throws IOException {
            ValueReader[] vrdrs = userCoordReaders_[ icoord ];
            int nu = vrdrs.length;
            Object[] userRow = userCoordRows_[ icoord ];
            for ( int iu = 0; iu < nu; iu++ ) {
                userRow[ iu ] = vrdrs[ iu ].readValue( rseq, irow );
            }
            return userRow;
        }
    }

    /**
     * Creates an object that can read values defined by a given expression.
     *
     * @param  expr  JEL expression, column name, or null
     * @param  table   table in whose context expr is to be evaluated
     * @param  evaluator   JEL evaluator for table
     * @param  lib   JEL library associated with evaluator
     * @param  fallback  constant object value read if expression is blank
     * @param  reqClazz  required type for values read by the returned reader
     * @return   new value reader
     */
    private static ValueReader
                   createValueReader( String expr, StarTable table,
                                      RowSequenceEvaluator evaluator,
                                      Library lib, Object fallback,
                                      Class<?> reqClazz )
            throws TaskException {

        /* Null in, fixed value out. */
        if ( expr == null || expr.trim().length() == 0 ) {
            return new FixedValueReader( fallback );
        }

        /* Look for a column with a matching name.  As well as a (possible,
         * small) increase in efficiency over doing it the JEL way, this
         * enables us to get the metadata from the column. */
        int ncol = table.getColumnCount();
        for ( int icol = 0; icol < ncol; icol++ ) {
            ValueInfo info = table.getColumnInfo( icol );
            if ( expr.trim().equalsIgnoreCase( info.getName().trim() ) ) {
                return new ColumnValueReader( table, icol );
            }
        }

        /* If that doesn't work, treat it as a JEL expression.
         * We can't get metadata for this. */
        final CompiledExpression compex;
        try {
            compex = JELUtils.compile( lib, table, expr );
        }
        catch ( CompilationException e ) {
            throw new TaskException( "Bad Expression \"" + expr + "\"", e );
        }
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
        return new JelValueReader( evaluator, compex, expr );
    }

    /**
     * Acquires a value at a given row of a sequence.
     */
    private interface ValueReader {

        /**
         * Acquires a value from the current row of a given RowSequence.
         * The redundancy between the two parameters is intentional.
         *
         * @param   rseq   row sequence positioned at the row of interest
         * @param   irow   index of the row of interest
         * @return  expression value
         */
        Object readValue( RowSequence rseq, long irow ) throws IOException;

        /**
         * Returns metadata associated with the column if known.
         *
         * @return   value info or null
         */
        ValueInfo getValueInfo();
    }

    /**
     * ValueReader implementation whose readValue method always returns null.
     */
    private static class FixedValueReader implements ValueReader {
        private final Object value_;
        private FixedValueReader( Object value ) {
            value_ = value;
        }
        public Object readValue( RowSequence rseq, long irow ) {
            return value_;
        }
        public ValueInfo getValueInfo() {
            return null;
        }
    }

    /**
     * ValueReader implementation that reads a given table column.
     */
    private static class ColumnValueReader implements ValueReader {
        private final int icol_;
        private final ValueInfo info_;

        /**
         * Constructor.
         *
         * @param  icol  table column index to read
         */
        ColumnValueReader( StarTable table, int icol ) {
            icol_ = icol;
            info_ = table.getColumnInfo( icol );
        }

        public Object readValue( RowSequence rseq, long irow )
                throws IOException {
            return rseq.getCell( icol_ );
        }

        public ValueInfo getValueInfo() {
            return info_;
        }
    }

    /**
     * ValueReader implementation that evaluates JEL expressions.
     * Not thread-safe.
     */
    private static class JelValueReader implements ValueReader {
        private final RowSequenceEvaluator evaluator_;
        private final CompiledExpression compex_;
        private final ValueInfo info_;
        private RowSequence rseq_;
        private long irow_;

        /**
         * Constructor.
         *
         * @param   evaluator  evaluator object
         * @param   compex  expression to evaluate
         * @param   expr  expression text
         */
        JelValueReader( RowSequenceEvaluator evaluator,
                        CompiledExpression compex, String expr ) {
            evaluator_ = evaluator;
            compex_ = compex;
            info_ = new DefaultValueInfo( expr, compex.getTypeC(), null );
        }

        public Object readValue( RowSequence rseq, long irow )
                throws IOException {
            return evaluator_.evaluateObject( compex_, rseq, irow );
        }

        public ValueInfo getValueInfo() {
            return info_;
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
                                      RowSequence rseq, long irow )
                throws IOException {

            /* Set the internal state of this JELRowReader object so that
             * the overridden getCurrentRow and getCell methods will retrieve
             * the right results. */
            rseq_ = rseq;
            irow_ = irow;

            /* Perform the evaluation. */
            try {
                return evaluate( compex );
            }
            catch ( IOException e ) {
                throw e;
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
