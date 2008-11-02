package uk.ac.starlink.ttools.plottask;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot.PointSequence;
import uk.ac.starlink.ttools.jel.DummyJELRowReader;
import uk.ac.starlink.ttools.jel.JELRowReader;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.SequentialJELRowReader;
import uk.ac.starlink.ttools.jel.StarTableJELRowReader;

/**
 * PlotData concrete subclass for Cartesian data.
 *
 * @author   Mark Taylor
 * @since    13 Aug 2008
 */
public class CartesianTablePlotData extends TablePlotData {

    private final int ndim_;
    private final String labelExpr_;
    private final String[] setExprs_;
    private final String[] coordExprs_;
    private final String[] errExprs_;
    private final int nerrPairs_;

    /**
     * Constructor.
     *
     * @param   table   table this data is based on
     * @param   setExprs    nset-element array of JEL boolean expressions
     *                      for subset inclusion criteria
     * @param   setNames    nset-element array of subset names
     * @param   setStyles   nset-element array of subset plot styles
     * @param   labelExpr   JEL String expression for text label
     * @param   coordExprs  ndim-element array of JEL numeric expressions
     *                      for coords (numeric = widenable to double)
     * @param   errExprs    array of expression pairs giving
     *                      error lower and/or upper bounds
     */
    public CartesianTablePlotData( StarTable table, String[] setExprs,
                                   String[] setNames, Style[] setStyles,
                                   String labelExpr, String[] coordExprs,
                                   String[] errExprs )
            throws CompilationException {
        super( table, setExprs, setNames, setStyles, labelExpr );
        labelExpr_ = labelExpr;
        setExprs_ = setExprs;
        coordExprs_ = (String[]) coordExprs.clone();
        ndim_ = coordExprs.length;
        errExprs_ = (String[]) errExprs.clone();
        int nerr = 0;
        StarTableJELRowReader dummyReader = new DummyJELRowReader( table );
        for ( int idim = 0; idim < errExprs.length; idim++ ) {
            if ( createErrorReader( idim, errExprs[ idim ], dummyReader )
                 != null ) {
                nerr++;
            }
        }
        nerrPairs_ = nerr;
    }

    public int getNdim() {
        return ndim_;
    }

    public int getNerror() {
        return nerrPairs_ * 2;
    }

    /**
     * Constructs an error reader object for a given axis given a string
     * expression describing the error value(s).
     * The expression may be of the form "lo,hi", "both", "lo," or ",hi".
     * Null is returned for an empty expression.  A compilation error results
     * from an illegal expression.
     *
     * <p>Note that this method is currently called from the constructor,
     * so must not be overridden by subclasses.
     *
     * @param  idim  index of dimension which error affects
     * @param  expr  supplied string expression
     * @param  jelReader   JEL context within which the expression(s) can be
     *                     evaluated
     * @return   error reader object
     */
    private final ErrorReader createErrorReader(
                final int idim, String expr,
                final StarTableJELRowReader jelReader )
            throws CompilationException {

        /* Return null for a blank expression. */
        if ( expr == null || expr.trim().length() == 0
                          || expr.trim().equals( "," ) ) {
            return null;
        }
        expr = expr.trim();

        /* Prepare a JEL library for compiling expressions.
         * This is like a standard one, but contains an additional static
         * library, the PairCreator class.  This is used for evaluating
         * expressions of the form "lo,hi". */
        List staticClassList = new ArrayList( JELUtils.getStaticClasses() );
        staticClassList.add( PairCreator.class );
        Class[] staticLib = (Class[]) staticClassList.toArray( new Class[ 0 ] );
        Class[] dynamicLib = new Class[] { jelReader.getClass(), };
        Class[] dotClasses = new Class[ 0 ];
        Library lib =
            new Library( staticLib, dynamicLib, dotClasses, jelReader, null );
        StarTable table = jelReader.getTable();

        /* Prepare static (well, closure) arrays for use with the newly 
         * created ErrorReader objects. */
        final double[][] errPair = new double[ 2 ][];
        final double[] lo = new double[ ndim_ ];
        final double[] hi = new double[ ndim_ ];

        /* Case ",hi"; upper bound only given. */
        if ( expr.startsWith( "," ) ) {
            String hiExpr = expr.substring( 1 ).trim();
            final CompiledExpression hiCompex =
                JELUtils.compile( lib, table, hiExpr, double.class );
            return new ErrorReader( jelReader ) {
                public double[][] readErrorPair( double[] point ) {
                    double off = evaluateDouble( hiCompex );
                    if ( off > 0 ) {
                        for ( int id = 0; id < ndim_; id++ ) {
                            hi[ id ] = point[ id ];
                        }
                        hi[ idim ] += off;
                        errPair[ 1 ] = hi;
                    }
                    else {
                        errPair[ 1 ] = null;
                    }
                    return errPair;
                }
            };
        }

        /* Case "lo,"; lower bound only given. */
        else if ( expr.endsWith( "," ) ) {
            String loExpr = expr.substring( 0, expr.length() - 1 ).trim();
            final CompiledExpression loCompex =
                JELUtils.compile( lib, table, loExpr, double.class );
            return new ErrorReader( jelReader ) {
                public double[][] readErrorPair( double[] point ) {
                    double off = evaluateDouble( loCompex );
                    if ( off > 0 ) {
                        for ( int id = 0; id < ndim_; id++ ) {
                            lo[ id ] = point[ id ];
                        }
                        lo[ idim ] -= off;
                        errPair[ 0 ] = lo;
                    }
                    else {
                        errPair[ 0 ] = null;
                    }
                    return errPair;
                }
            };
        }

        /* Either a single symmetric bound "both" or "lo,hi" is given,
         * don't know which at this stage. */
        else {

            /* Try to interpret the expression as a scalar value
             * (symmetric error bounds). */
            try {
                final CompiledExpression bothCompex =
                    JELUtils.compile( lib, table, expr, double.class );
                return new ErrorReader( jelReader ) {
                    public double[][] readErrorPair( double[] point ) {
                        double off = evaluateDouble( bothCompex );
                        if ( off > 0 ) {
                            for ( int id = 0; id < ndim_; id++ ) {
                                lo[ id ] = point[ id ];
                                hi[ id ] = point[ id ];
                            }
                            lo[ idim ] -= off;
                            hi[ idim ] += off;
                            errPair[ 0 ] = lo;
                            errPair[ 1 ] = hi;
                        }
                        else {
                            errPair[ 0 ] = null;
                            errPair[ 1 ] = null;
                        }
                        return errPair;
                    }
                };
            }

            /* Scalar value parsing failed, so the only legal possibility
             * is a comma-separated "lo,hi" string. */
            catch ( CompilationException e ) {

                /* If there's no comma at all here, it must be bad syntax. */
                if ( expr.indexOf( ',' ) < 0 ) {
                    throw e;
                }

                /* Otherwise, we try to interpret it as a comma-separated pair.
                 * Parsing the string is non-trivial in this case
                 * (commas may either be separating lower and upper bounds
                 * or may be separators in a function argument list),
                 * so we will get JEL to do the parsing here.
                 * The syntax we are looking for is identical to what is found
                 * between the brackets of a java (JEL) method invocation,
                 * so just whack a couple of brackets round the string we have
                 * and try to pass it off as the argument list of a 2-argument
                 * method we have introduced for that purpose. */
                else {
                    String pairExpr = "createDoublePair(" + expr + ")";
                    final CompiledExpression pairCompex =
                        Evaluator.compile( pairExpr, lib );
                    return new ErrorReader( jelReader ) {
                        public double[][] readErrorPair( double[] point ) {
                            double[] offs;
                            try {
                                offs = (double[])
                                       jelReader.evaluate( pairCompex );
                            }
                            catch ( Error e ) {
                                throw e;
                            }
                            catch ( Throwable e ) {
                                errPair[ 0 ] = null;
                                errPair[ 1 ] = null;
                                return errPair;
                            }
                            double loOff = offs[ 0 ];
                            if ( loOff > 0 ) {
                                for ( int id = 0; id < ndim_; id++ ) {
                                    lo[ id ] = point[ id ];
                                }
                                lo[ idim ] -= loOff;
                                errPair[ 0 ] = lo;
                            }
                            else {
                                errPair[ 0 ] = null;
                            }
                            double hiOff = offs[ 1 ];
                            if ( hiOff > 0 ) {
                                for ( int id = 0; id < ndim_; id++ ) {
                                    hi[ id ] = point[ id ];
                                }
                                hi[ idim ] += hiOff;
                                errPair[ 1 ] = hi;
                            }
                            else {
                                errPair[ 1 ] = null;
                            }
                            return errPair;
                        }
                    };
                }
            }
        }
    }

    protected PointSequence createPointSequence( final
                                                 SequentialJELRowReader rseq )
            throws CompilationException {

        /* Prepare to generate coordinate values. */
        Library lib = JELUtils.getLibrary( rseq );
        final CompiledExpression[] coordCompexs =
            new CompiledExpression[ ndim_ ];
        for ( int idim = 0; idim < ndim_; idim++ ) {
            String expr = coordExprs_[ idim ];
            coordCompexs[ idim ] = expr == null 
                                 ? null
                                 : JELUtils.compile( lib, rseq.getTable(),
                                                     expr, double.class );
        }

        /* Prepare to generate error values. */
        List errReaderList = new ArrayList();
        for ( int idim = 0; idim < errExprs_.length; idim++ ) {
            ErrorReader errReader =
                createErrorReader( idim, errExprs_[ idim ], rseq );
            if ( errReader != null ) {
                errReaderList.add( errReader );
            }
        }
        final ErrorReader[] errorReaders =
            (ErrorReader[]) errReaderList.toArray( new ErrorReader[ 0 ] );
        assert errorReaders.length == nerrPairs_;

        /* Construct and return the point sequence. */
        return new TablePointSequence( rseq, labelExpr_, setExprs_ ) {
            private final double[] coords_ = new double[ ndim_ ];
            private final double[] coords1_ = new double[ ndim_ ];
            private final double[][] errors_ = new double[ nerrPairs_ * 2 ][];
            private final double[][] errors1_ = new double[ nerrPairs_ * 2 ][];
            private final double[][] errors2_ =
                new double[ nerrPairs_ * 2 ][ ndim_ ];
            private boolean hasPoint_;
            private boolean hasErrors_;

            public boolean next() {
                hasPoint_ = false;
                hasErrors_ = false;
                return super.next();
            }

            public double[] getPoint() {

                /* Acquire the point data if necessary. */
                ensureHasPoint();

                /* Return a copy of it.  This is essential as the caller
                 * may scribble on the returned array. */
                for ( int idim = 0; idim < ndim_; idim++ ) {
                    coords1_[ idim ] = coords_[ idim ];
                }
                return coords1_;
            }

            public double[][] getErrors() {

                /* Acquire the error data if necessary. */
                ensureHasErrors();

                /* Return a copy of it. */
                for ( int ierr = 0; ierr < nerrPairs_ * 2; ierr++ ) {
                    double[] err = errors_[ ierr ];
                    final double[] err1;
                    if ( err == null ) {
                        err1 = null;
                    }
                    else {
                        err1 = errors2_[ ierr ];
                        for ( int idim = 0; idim < ndim_; idim++ ) {
                            err1[ idim ] = err[ idim ];
                        }
                    }
                    errors1_[ ierr ] = err1;
                }
                return errors1_;
            }

            /**
             * Following this call, the coords_ array will be populated
             * correctly for the current point.
             */
            private void ensureHasPoint() {
                if ( ! hasPoint_ ) {
                    for ( int idim = 0; idim < ndim_; idim++ ) {
                        coords_[ idim ] =
                            evaluateDouble( coordCompexs[ idim ] );
                    }
                    hasPoint_ = true;
                }
            }

            /**
             * Following this call, the errors_ array will be populated
             * correctly for the current point.
             */
            private void ensureHasErrors() {
                if ( ! hasErrors_ ) {
                    ensureHasPoint();
                    for ( int ierr = 0; ierr < nerrPairs_; ierr++ ) {
                        double[][] errPair =
                            errorReaders[ ierr ].readErrorPair( coords_ );
                        errors_[ ierr * 2 + 0 ] = errPair[ 0 ];
                        errors_[ ierr * 2 + 1 ] = errPair[ 1 ];
                    }
                    hasErrors_ = true;
                }
            }
        };
    }

    /**
     * Abstract class which defines how errors are acquired from a row sequence.
     */
    private static abstract class ErrorReader {
        final JELRowReader jelReader_;

        /**
         * Constructor.
         *
         * @param  jelReader  JEL execution context
         */
        ErrorReader( JELRowReader jelReader ) {
            jelReader_ = jelReader;
        }

        /**
         * Returns a pair of error bar endpoints for a given central data point.
         * These presumably correspond to two error bars in a single
         * dimension.  One or both may be null indicating a bad, inapplicable
         * or zero-sized error in the relevant direction.
         * The returned array and its elements may be overwritten on subsequent
         * calls to this method (so copy the values if you want to keep them).
         *
         * @param  point  central coordinate
         * @return  2-element array of ndim-element coordinate arrays,
         *          giving error bar endpoints
         */
        abstract double[][] readErrorPair( double[] point );

        /**
         * Utility method to evaluate a compiled expression whose type is
         * known to be numeric.  Does not throw checked exceptions
         *
         * @param   compex  expression with numeric type
         * @return   value of compex in this reader's execution context,
         *           or NaN if any exception was thrown during evaluation
         */
        double evaluateDouble( CompiledExpression compex ) {
            try {
                return jelReader_.evaluateDouble( compex );
            }
            catch ( Error e ) {
                throw e;
            }
            catch ( Throwable e ) {
                return Double.NaN;
            }
        }
    }

    /**
     * Class used for JEL manipulations.
     *
     * <p>This class is an implementation detail, but has to be 
     * public for JEL to be able to use it.
     */
    public static class PairCreator {

        /**
         * Returns a 2-element array composed from its 2 arguments.
         *
         * @param  lo  arg 1
         * @param  hi  arg 2
         * @return   {lo,hi}
         */
        public static double[] createDoublePair( double lo, double hi ) {
            return new double[] { lo, hi, };
        }
    }
}
