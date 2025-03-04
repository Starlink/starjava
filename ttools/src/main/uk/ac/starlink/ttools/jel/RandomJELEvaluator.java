package uk.ac.starlink.ttools.jel;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Library;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.StarTable;

/**
 * Random-access evaluator for JEL expressions evaluated against tables.
 *
 * @author   Mark Taylor
 * @since    11 Dec 2020
 */
public abstract class RandomJELEvaluator implements Closeable {

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
     * Returns a supplier for evaluators.
     * The evaluators returned by the supplier are not safe for concurrent use.
     *
     * @param  table  context for expression evaluation
     * @param  expr   JEL expression
     * @return  supplier of non-thread-safe evaluators
     */
    public static Supplier<RandomJELEvaluator>
            createEvaluatorSupplier( StarTable table, String expr )
            throws CompilationException {
        Function<Library,CompiledExpression> compiler =
            JELUtils.compiler( table, expr, null );
        return () -> {
            final AccessRowReader rdr;
            try {
                rdr = new AccessRowReader( table, compiler );
            }
            catch ( IOException e ) {
                throw new RuntimeException( "Uh oh, rethrown IOException " + e,
                                            e );
            }
            return new RandomJELEvaluator() {
                public Object evaluateObject( long lrow ) throws IOException {
                    rdr.setRowIndex( lrow );
                    return rdr.evaluateObject();
                }
                public double evaluateDouble( long lrow ) throws IOException {
                    rdr.setRowIndex( lrow );
                    return rdr.evaluateDouble();
                }
                public boolean evaluateBoolean( long lrow ) throws IOException {
                    rdr.setRowIndex( lrow );
                    return rdr.evaluateBoolean();
                }
                public void close() throws IOException {
                    rdr.close();
                }
            };
        };
    }

    /**
     * Returns a RandomJELEvaluator instance for a given table and expression.
     * The returned implementation may or may not be suitable for use from
     * multiple threads concurrently, depending on the
     * <code>isConcurrent</code> parameter.
     *
     * @param  table  context for expression evaluation
     * @param  expr   JEL expression
     * @param  isConcurrent  whether result will be suitable for concurrent use
     * @return   evaluator, which is only guaranteed safe for concurrent use
     *           if <code>isConcurrent</code> was set true
     */
    public static RandomJELEvaluator
            createEvaluator( final StarTable table, final String expr,
                             boolean isConcurrent )
            throws CompilationException {

        /* Check that the expression can be compiled without error.
         * If not, a CompilationException will be thrown here. */
        Function<Library,CompiledExpression> compiler =
            JELUtils.compiler( table, expr, null );

        /* Provide a supplier for AccessRowReader instances that will
         * evaluate the expression.  Each of these instances is only safe
         * for use in a single thread. */
        Supplier<AccessRowReader> rdrSupplier = () -> {
            try {
                return new AccessRowReader( table, compiler );
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
        if ( isConcurrent ) {
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
        }

        /* For single-threaded use, we only need one AccessRowReader. */
        else {
            final AccessRowReader rdr = rdrSupplier.get();
            rdrMgr = new RowReaderManager() {
                public AccessRowReader getRowReader() {
                    return rdr;
                }
                public void close() throws IOException {
                    rdr.close();
                }
            };
        }

        /* Return an evaluator based on the AccessRowReader we have. */
        return new RandomJELEvaluator() {
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
            extends StarTableJELRowReader
            implements Closeable {
        private final CompiledExpression compEx_;
        private final RowAccess rowAccess_;
        private long lrow_;

        /**
         * Constructor.
         *
         * @param  table  evaluation context
         * @param  compiler  expression compiler
         */
        AccessRowReader( StarTable table,
                         Function<Library,CompiledExpression> compiler )
                throws IOException {
            super( table );
            compEx_ = compiler.apply( JELUtils.getLibrary( this ) );
            rowAccess_ = table.getRowAccess();
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
            catch ( IOException e ) {
                throw e;
            }
            catch ( Throwable e ) {
                throw new IOException( "Evaluation error: " + e, e );
            }
        }
    }
}
