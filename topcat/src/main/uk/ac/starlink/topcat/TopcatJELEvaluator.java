package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.jel.JELRowReader;

/**
 * Random-access evaluator for a JEL expression evaluated against a
 * TopcatModel.
 *
 * <p><strong>Note</strong> this is more or less a copy of
 * {@link uk.ac.starlink.ttools.jel.RandomJELEvaluator}.
 * It's very difficult to make that code reusable here because this class
 * needs to use a TopcatJELRowReader rather than a StarTableRowReader
 * (enhanced expression parsing, e.g. RowSubsets) and the differing
 * functionality is implemented using inheritance rather than composition.
 * The right thing would probably be to rework the JEL usage to
 * use composition, but it's not straightforward.
 * Live with duplicated code instead.
 *
 * @author   Mark Taylor
 * @since    11 Dec 2020
 */
public abstract class TopcatJELEvaluator implements Closeable {

    /**
     * Returns the text of the expression that this evaluator can
     * evaluate.
     *
     * @return  expression
     */
    public abstract String getExpression();

    /**
     * Returns the actual result type that JEL has determined the
     * compiled expression to have.  This will be (at least compatible with)
     * the return type of the evaluations.  It will return wrapper types,
     * not primitive types.
     * 
     * @return   non-primitive result type
     */
    public abstract Class<?> getResultType();

    /**
     * Returns the value of the expression at a given table row as an Object.
     *
     * @param  lrow  evaluation row index
     * @return   object value at given row
     */
    public abstract Object evaluateObject( long lrow ) throws IOException;

    /**
     * Returns the value of the expression at a given table row as a double.
     * Behaviour is undefined if the expression is not numeric.
     *
     * @param  lrow  evaluation row index
     * @return   numeric value at given row
     */
    public abstract double evaluateDouble( long lrow ) throws IOException;

    /**
     * Returns the value of the expression at a given table row as a boolean.
     * Behaviour is undefined if the expression is not boolean-typed.
     *
     * @param  lrow  evaluation row index
     * @return   boolean value at given row
     */
    public abstract boolean evaluateBoolean( long lrow ) throws IOException;

    /**
     * Returns a TopcatJELEvaluator instance for a given table and expression.
     * The returned implementation is suitable for use from
     * multiple threads concurrently.
     *
     * @param  tcModel  context for expression evaluation
     * @param  expr   JEL expression
     * @param  activation   true to include activation functions
     * @param  reqType   required result type, or null to accept any
     * @return   evaluator safe for concurrent use
     * @throws  CompilationException  if the expression doesn't compile
     *          or doesn't have the (non-null) required type
     */
    public static TopcatJELEvaluator
            createEvaluator( final TopcatModel tcModel, final String expr,
                             final boolean activation, Class<?> reqType )
            throws CompilationException {
        Function<TopcatJELRowReader,Library> libFunc =
            rr -> TopcatJELUtils.getLibrary( rr, activation );

        /* Check that the expression can be compiled without error.
         * If not, a CompilationException will be thrown here. */
        Library lib =
            libFunc.apply( TopcatJELRowReader.createDummyReader( tcModel ) );
        CompiledExpression compEx = Evaluator.compile( expr, lib, reqType );
        final Class<?> actualType = compEx.getTypeC();

        /* Provide a supplier for AccessRowReader instances that will
         * evaluate the expression.  Each of these instances is only safe
         * for use in a single thread. */
        Supplier<AccessRowReader> rdrSupplier = () -> {
            try {
                return new AccessRowReader( tcModel, expr, libFunc, reqType );
            }
            catch ( CompilationException e ) {
                throw new RuntimeException( "Unexpected compilation failure"
                                          + "; it worked last time", e );
            }
            catch ( IOException e ) {
                throw new RuntimeException( "Uh oh, rethrown IOException " + e,
                                            e );
            }
        };

        /* Prepare an object with appropriate thread safety characteristics
         * that can provide AccessRowReader instances. */
        final RowReaderManager rdrMgr;

        /* For concurrent use, use a ThreadLocal to ensure that
         * AccessRowReaders are not shared between threads. */
        Collection<AccessRowReader> rdrList = new CopyOnWriteArrayList<>();
        final ThreadLocal<AccessRowReader> rdrLocal =
                new ThreadLocal<AccessRowReader>() {
            protected AccessRowReader initialValue() {
                AccessRowReader rdr = rdrSupplier.get();
                rdrList.add( rdr );
                return rdr;
            }
        };
        rdrMgr = new RowReaderManager() {
            public AccessRowReader getRowReader() {
                return rdrLocal.get();
            }
            public void close() throws IOException {
                for ( AccessRowReader rdr : rdrList ) {
                    rdr.close();
                }
            }
        };

        /* Return an evaluator based on the AccessRowReader we have. */
        return new TopcatJELEvaluator() {
            public String getExpression() {
                return expr;
            }
            public Class<?> getResultType() {
                return actualType;
            }
            public Object evaluateObject( long lrow ) throws IOException {
                return getReader( lrow ).evaluateObject();
            }
            public double evaluateDouble( long lrow ) throws IOException {
                return getReader( lrow ).evaluateDouble();
            }
            public boolean evaluateBoolean( long lrow ) throws IOException {
                return getReader( lrow ).evaluateBoolean();
            }
            public void close() throws IOException {
                rdrMgr.close();
            }
            AccessRowReader getReader( long lrow ) throws IOException {
                AccessRowReader rdr = rdrMgr.getRowReader();
                rdr.setRowIndex( lrow );
                return rdr;
            }
        };
    }

    /**
     * Supplies and disposes of AccessRowReaders.
     */
    private interface RowReaderManager extends Closeable {

        /**
         * Returns a suitable AccessRowReader.
         * Thread-safety depends on implementation.
         */
        AccessRowReader getRowReader();
    }

    /**
     * StarTableJELRowReader based on a RowAccess object.
     * Not suitable for concurrent use from multiple threads.
     */
    private static class AccessRowReader
            extends TopcatJELRowReader
            implements Closeable {
        private final CompiledExpression compEx_;
        private final RowAccess rowAccess_;
        private long lrow_;

        /**
         * Constructor.
         *
         * @param  tcModel  evaluation context
         * @param  expr   expression to evaluate
         * @param  libFunc  provides a JEL Library
         * @param  reqType   required result type, or null to accept any
         */
        AccessRowReader( TopcatModel tcModel, String expr,
                         Function<TopcatJELRowReader,Library> libFunc,
                         Class<?> reqType )
                throws CompilationException, IOException {
            super( tcModel );
            compEx_ = Evaluator.compile( expr, libFunc.apply( this ), reqType );
            rowAccess_ = tcModel.getDataModel().getRowAccess();
            lrow_ = -1;
        }

        /**
         * Prepares this object to read values from a given row.
         *
         * @param  lrow  row index
         */
        void setRowIndex( long lrow ) throws IOException {
            rowAccess_.setRowIndex( lrow );
            lrow_ = lrow;
        }

        public long getCurrentRow() {
            return lrow_;
        }

        public Object getCell( int icol ) throws IOException {
            return rowAccess_.getCell( icol );
        }

        // not called; not implemented
        public Object evaluateAtRow( CompiledExpression compEx, long lrow ) {
            throw new UnsupportedOperationException();
        }

        // not called; not implemented
        public boolean evaluateBooleanAtRow( CompiledExpression compEx,
                                             long lrow ) {
            throw new UnsupportedOperationException();
        }

        public void close() throws IOException {
            rowAccess_.close();
        }

        /**
         * Evaluates the expression at the current row as an object.
         *
         * @return   expression value as an object
         */
        public Object evaluateObject() throws IOException {
            try {
                return evaluate( compEx_ );
            }
            catch ( RuntimeException e ) {
                throw e;
            }
            catch ( IOException e ) {
                throw e;
            }
            catch ( Throwable e ) {
                throw new IOException( "Evaluation error: " + e, e );
            }
        }

        /**
         * Evaluates the expression at the current row as a double.
         *
         * @return  numeric expression value
         */
        public double evaluateDouble() throws IOException {
            try {
                return evaluateDouble( compEx_ );
            }
            catch ( RuntimeException e ) {
                throw e;
            }
            catch ( IOException e ) {
                throw e;
            }
            catch ( Throwable e ) {
                throw new IOException( "Evaluation error: " + e, e );
            }
        }

        /**
         * Evaluates the expression at the current row as a boolean.
         *
         * @return  boolean expression value
         */
        public boolean evaluateBoolean() throws IOException {
            try {
                return evaluateBoolean( compEx_ );
            }
            catch ( RuntimeException e ) {
                throw e;
            }
            catch ( IOException e ) {
                throw e;
            }
            catch ( Throwable e ) {
                throw new IOException( "Evaluation error: " + e, e );
            }
        }
    }
}
