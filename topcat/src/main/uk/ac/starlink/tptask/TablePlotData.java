package uk.ac.starlink.tptask;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.tplot.PlotData;
import uk.ac.starlink.tplot.PointSequence;
import uk.ac.starlink.tplot.Style;
import uk.ac.starlink.ttools.jel.DummyJELRowReader;
import uk.ac.starlink.ttools.jel.JELRowReader;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.SequentialJELRowReader;

/**
 * PlotData implementation representing the data from a single table with
 * associated expressions describing coordinate selections etc.
 *
 * @author    Mark Taylor
 * @since     22 Apr 2008
 */
public class TablePlotData implements PlotData {

    private final StarTable table_;
    private final String[] coordExprs_;
    private final String labelExpr_;
    private final String[] setExprs_;
    private final String[] setNames_;
    private final Style[] setStyles_;
    private final int ndim_;
    private final int nset_;

    /**
     * Constructor.
     *
     * @param   table   table this data is based on
     * @param   coordExprs  ndim-element array of JEL numeric expressions
     *                      for coords (numeric = widenable to double)
     * @param   labelExpr   JEL String expression for text label
     * @param   setExprs    nset-element array of JEL boolean expressions
     *                      for subset inclusion criteria
     * @param   setNames    nset-element array of subset names
     * @param   setStyles   nset-element array of subset plot styles
     */
    public TablePlotData( StarTable table, String[] coordExprs,
                          String labelExpr, String[] setExprs,
                          String[] setNames, Style[] setStyles )
            throws CompilationException {
        table_ = table;
        coordExprs_ = (String[]) coordExprs.clone();
        ndim_ = coordExprs_.length;
        labelExpr_ = labelExpr;
        setExprs_ = (String[]) setExprs.clone();
        nset_ = setExprs_.length;
        if ( setNames.length != nset_ || setStyles.length != nset_ ) {
            throw new IllegalArgumentException( "Inconsistent set count" );
        }
        setNames_ = (String[]) setNames.clone();
        setStyles_ = (Style[]) setStyles.clone();

        /* Create a dummy ExpressionSet based on the JEL expressions we have.
         * This is not used, but its construction will check that the 
         * expressions are compilable.  A compilation error will be generated
         * if any of them is not. */
        new ExpressionSet( new DummyJELRowReader( table ) );
    }

    public int getNdim() {
        return ndim_;
    }

    public int getNerror() {
        return 0;
    }

    public int getSetCount() {
        return nset_;
    }

    public String getSetName( int iset ) {
        return setNames_[ iset ];
    }

    public Style getSetStyle( int iset ) {
        return setStyles_[ iset ];
    }

    public boolean hasLabels() {
        return labelExpr_ != null;
    }

    public PointSequence getPointSequence() {
        return new TablePointSequence();
    }

    /**
     * PointSequence implementation for TablePlotData.
     */
    private class TablePointSequence implements PointSequence {
        private final SequentialJELRowReader rseq_;
        private final ExpressionSet eset_;
        private final double[] point_;
        private final boolean[] isIncluded_;
        private String label_;
        private boolean hasPoint_;
        private boolean hasLabel_;
        private boolean hasIncluded_;

        /**
         * Constructor.
         */
        TablePointSequence() {
            try {
                rseq_ = new SequentialJELRowReader( table_ );
            }
            catch ( IOException e ) {
                throw new PlotDataException( e );
            }
            try {
                eset_ = new ExpressionSet( rseq_ );
            }
            catch ( CompilationException e ) {
                throw (AssertionError)
                      new AssertionError( "But it compiled last time..." )
                     .initCause( e );
            }
            point_ = new double[ ndim_ ];
            isIncluded_ = new boolean[ nset_ ];
        }

        public boolean next() {
            hasPoint_ = false;
            hasIncluded_ = false;
            hasLabel_ = false;
            try {
                return rseq_.next();
            }
            catch ( IOException e ) {
                throw new PlotDataException( e );
            }
        }

        public double[] getPoint() {
            if ( ! hasPoint_ ) {
                for ( int i = 0; i < ndim_; i++ ) {
                    Object p;
                    try {
                        p = rseq_.evaluate( eset_.coordCompexs_[ i ] );
                    }
                    catch ( IOException e ) {
                        throw new PlotDataException( e );
                    }
                    catch ( Throwable e ) {
                        throw new PlotDataException( "Unexpected error", e );
                    }
                    point_[ i ] = ( p instanceof Number )
                                ? ((Number) p).doubleValue()
                                : Double.NaN;
                }
                hasPoint_ = true;
            }
            return point_;
        }

        public double[][] getErrors() {
            return null;
        }

        public String getLabel() {
            if ( ! hasLabel_ ) {
                Object lab;
                try {
                    lab = rseq_.evaluate( eset_.labelCompex_ );
                }
                catch ( IOException e ) {
                    throw new PlotDataException( e );
                }
                catch ( Throwable e ) {
                    throw new PlotDataException( "Unexpected error", e );
                }
                label_ = lab instanceof String ? (String) lab
                                               : null;
                hasLabel_ = true;
            }
            return label_;
        }

        public boolean isIncluded( int iset ) {
            if ( ! hasIncluded_ ) {
                for ( int i = 0; i < nset_; i++ ) {
                    Object inc;
                    try {
                        inc = rseq_.evaluate( eset_.setCompexs_[ i ] );
                    }
                    catch ( IOException e ) {
                        throw new PlotDataException( e );
                    }
                    catch ( Throwable e ) {
                        throw new PlotDataException( "Unexpected error", e );
                    }
                    isIncluded_[ i ] = inc instanceof Boolean
                                     ? ((Boolean) inc).booleanValue()
                                     : false;
                }
                hasIncluded_ = true;
            }
            return isIncluded_[ iset ];
        }

        public void close() {
            try {
                rseq_.close();
            }
            catch ( IOException e ) {
                throw new PlotDataException( e );
            }
        }
    }

    /**
     * Aggregates the JEL expressions whose evaluation is required by this
     * PlotData object.  The constructor compiles the expressions as required.
     * The member arrays of CompiledExpressions are therefor available for
     * use from the constructed object.
     */
    private class ExpressionSet {
        final CompiledExpression[] coordCompexs_;
        final CompiledExpression labelCompex_;
        final CompiledExpression[] setCompexs_;

        /**
         * Constructor.  
         * This constructor will fail with a JEL CompilationException if any 
         * of the expressions contains an error, so that such errors can
         * be caught early rather than at expression evaluation time.
         * Note that compilation for any JELRowReader with based on the 
         * same table should fail in the same way (or succeed), so that
         * success for one rdr can be taken as a guarantee of success of
         * subsequent similar ones.
         *
         * @param   rdr   table reader object which provides the 
         *                environment for JEL expression evaluation
         * @throws   CompilationException  if any of the expressions cannot be
         *           compiled for <code>rdr</code>
         */
        ExpressionSet( JELRowReader rdr ) throws CompilationException {
            Library lib = JELUtils.getLibrary( rdr );
            coordCompexs_ = new CompiledExpression[ coordExprs_.length ];
            for ( int i = 0; i < coordExprs_.length; i++ ) {
                coordCompexs_[ i ] =
                    Evaluator.compile( coordExprs_[ i ], lib, double.class );
            }
            labelCompex_ = labelExpr_ == null
                ? null 
                : Evaluator.compile( labelExpr_, lib, String.class );
            setCompexs_ = new CompiledExpression[ setExprs_.length ];
            for ( int i = 0; i < setExprs_.length; i++ ) {
                setCompexs_[ i ] =
                    Evaluator.compile( setExprs_[ i ], lib, boolean.class );
            }
        }
    }
}
