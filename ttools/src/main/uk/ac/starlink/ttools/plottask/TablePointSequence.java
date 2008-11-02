package uk.ac.starlink.ttools.plottask;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Library;
import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.plot.PointSequence;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.SequentialJELRowReader;

/**
 * PointSequence abstract superclass implementation for getting sequential
 * plot data from a table.
 *
 * @author   Mark Taylor
 * @since    13 Aug 2008
 */
public abstract class TablePointSequence implements PointSequence {

    private final SequentialJELRowReader rseq_;
    private final CompiledExpression labelCompex_;
    private final CompiledExpression[] setCompexs_;
    private final int nset_;
    private boolean hasLabel_;
    private boolean hasIncludeds_;
    private String label_;
    private boolean[] isIncludeds_;

    /**
     * Constructor.
     *
     * @param  rseq  row sequence representing the table which contains the
     *               actual data
     * @param  labelExpr  JEL expression for text labellling each point;
     *                    may be null for no label; may have any type
     *                    (converted to string before use)
     * @param  setExprs   nset-element array of boolean-valued JEL expressions
     *                    giving per-set point inclusion status
     */
    protected TablePointSequence( SequentialJELRowReader rseq,
                                  String labelExpr, String[] setExprs )
            throws CompilationException {
        rseq_ = rseq;
        Library lib = JELUtils.getLibrary( rseq );
        StarTable table = rseq.getTable();
        labelCompex_ = labelExpr == null
                     ? null
                     : JELUtils.compile( lib, table, labelExpr );
        nset_ = setExprs.length;
        setCompexs_ = new CompiledExpression[ nset_ ];
        for ( int is = 0; is < nset_; is++ ) {
            setCompexs_[ is ] =
                JELUtils.compile( lib, table, setExprs[ is ], boolean.class );
        }
        isIncludeds_ = new boolean[ nset_ ];
    }

    public boolean next() {
        hasIncludeds_ = false;
        hasLabel_ = false;
        try {
            return rseq_.next();
        }
        catch ( IOException e ) {
            throw new PlotDataException( e );
        }
    }

    public String getLabel() {
        if ( ! hasLabel_ ) {
            Object lab = evaluate( labelCompex_ );
            label_ = lab == null ? null
                                 : lab.toString();
            hasLabel_ = true;
        }
        return label_;
    }

    public boolean isIncluded( int iset ) {
        if ( ! hasIncludeds_ ) {
            for ( int is = 0; is < nset_; is++ ) {
                Object inc = evaluate( setCompexs_[ is ] );
                isIncludeds_[ is ] = inc instanceof Boolean
                                   ? ((Boolean) inc).booleanValue()
                                   : false;
            }
            hasIncludeds_ = true;
        }
        return isIncludeds_[ iset ];
    }

    public void close() {
        try {
            rseq_.close();
        }
        catch ( IOException e ) {
            throw new PlotDataException( e );
        }
    }

    /**
     * Convenience method which evaluates an Object-valued compiled expression.
     * Any resulting exceptions are rethrown as PlotDataExceptions.
     *
     * @param  compex  compiled expression
     * @return   expression value
     */
    protected Object evaluate( CompiledExpression compex ) {
        try {
            return rseq_.evaluate( compex );
        }
        catch ( IOException e ) {
            throw new PlotDataException( e );
        }
        catch ( Error e ) {
            throw (Error) e;
        }
        catch ( Throwable e ) {
            throw new PlotDataException( "Unexpected error", e );
        }
    }

    /**
     * Convenience method which evaluates a numeric-valued compiled expression.
     * Any resulting exceptions are rethrown as PlotDataExceptions.
     *
     * @param  compex  compiled expression
     * @return   expression value
     */
    protected double evaluateDouble( CompiledExpression compex ) {
        try {
            return rseq_.evaluateDouble( compex );
        }
        catch ( IOException e ) {
            throw new PlotDataException( e );
        }
        catch ( Error e ) {
            throw (Error) e;
        }
        catch ( Throwable e ) {
            throw new PlotDataException( "Unexpected error", e );
        }
    }
}
