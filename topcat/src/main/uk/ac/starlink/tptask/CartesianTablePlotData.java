package uk.ac.starlink.tptask;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.tplot.Style;
import uk.ac.starlink.tplot.PointSequence;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.SequentialJELRowReader;

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
     * @param   errExprs    ndim-element array of expression pairs giving
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

        // errExprs currently ignored
    }

    public int getNdim() {
        return ndim_;
    }

    public int getNerror() {
        return 0;
    }

    protected PointSequence createPointSequence( final
                                                 SequentialJELRowReader rseq )
            throws CompilationException {
        Library lib = JELUtils.getLibrary( rseq );
        final CompiledExpression[] coordCompexs =
            new CompiledExpression[ ndim_ ];
        for ( int idim = 0; idim < ndim_; idim++ ) {
            coordCompexs[ idim ] =
                Evaluator.compile( coordExprs_[ idim ], lib, double.class );
        }
        final double[] coords = new double[ ndim_ ];
        return new TablePointSequence( rseq, labelExpr_, setExprs_ ) {
            private boolean hasPoint_;

            public boolean next() {
                hasPoint_ = false;
                return super.next();
            }

            public double[] getPoint() {
                if ( ! hasPoint_ ) {
                    for ( int idim = 0; idim < ndim_; idim++ ) {
                        coords[ idim ] = evaluateDouble( coordCompexs[ idim ] );
                    }
                    hasPoint_ = true;
                }
                return coords;
            }

            public double[][] getErrors() {
                return null;
            }
        };
    }
}
