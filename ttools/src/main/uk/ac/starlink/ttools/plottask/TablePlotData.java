package uk.ac.starlink.ttools.plottask;

import gnu.jel.CompilationException;
import java.io.IOException;
import uk.ac.starlink.table.EmptyStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.plot.PlotData;
import uk.ac.starlink.ttools.plot.PointSequence;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.jel.DummyJELRowReader;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.SequentialJELRowReader;

/**
 * Abstract superclass for PlotData implementation representing 
 * the data from a single table with associated expressions describing 
 * coordinate selections etc.
 * Concrete subclasses must see to provision of coordinate and error data.
 *
 * @author    Mark Taylor
 * @since     22 Apr 2008
 */
public abstract class TablePlotData implements PlotData {

    private final StarTable table_;
    private final String[] setExprs_;
    private final String[] setNames_;
    private final Style[] setStyles_;
    private final String labelExpr_;
    private final int nset_;

    /**
     * Constructor.
     *
     * @param   table   table this data is based on
     * @param   setExprs    nset-element array of JEL boolean expressions
     *                      for subset inclusion criteria
     * @param   setNames    nset-element array of subset names
     * @param   setStyles   nset-element array of subset plot styles
     * @param   labelExpr   JEL String expression for text label
     */
    protected TablePlotData( StarTable table, String[] setExprs,
                             String[] setNames, Style[] setStyles,
                             String labelExpr ) {
        table_ = table;
        setExprs_ = (String[]) setExprs.clone();
        setNames_ = (String[]) setNames.clone();
        setStyles_ = (Style[]) setStyles.clone();
        nset_ = setExprs_.length;
        labelExpr_ = labelExpr;
        if ( setNames.length != nset_ || setStyles.length != nset_ ) {
            throw new IllegalArgumentException( "Inconsistent set count" );
        }
    }

    /**
     * Constructs a point sequence suitable for dispensing by this plot data.
     *
     * @param  rseq  row reader representing this data's table
     * @return  new point sequence for plotting
     */
    protected abstract PointSequence
                       createPointSequence( SequentialJELRowReader rseq )
            throws CompilationException;

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
        try {
            return createPointSequence( new SequentialJELRowReader( table_ ) );
        }
        catch ( IOException e ) {
            throw new PlotDataException( e );
        }
        catch ( CompilationException e ) {
            throw (AssertionError)
                  new AssertionError( "Compilation error should have showed up"
                                    + " earlier" )
                 .initCause( e );
        }
    }

    /**
     * Checks that any JEL expressions used by the data for this object
     * compile correctly.
     * A dummy call of {@link #createPointSequence} is made.
     *
     * @throws  CompilationException  if one is thrown by createPointSequence
     */
    public void checkExpressions() throws CompilationException {
        SequentialJELRowReader dummyRdr;
        try {
            dummyRdr =
                new SequentialJELRowReader( new EmptyStarTable( table_ ) );
        }
        catch ( IOException e ) {
            throw new AssertionError( "That shouldn't happen" );
        }
        createPointSequence( dummyRdr );
    }
}
