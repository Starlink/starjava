/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *    27-MAR-2004 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.iface;

import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;
import uk.ac.starlink.ast.gui.AstDouble;
import uk.ac.starlink.diva.InterpolatedCurveFigure;
import uk.ac.starlink.splat.plot.DivaPlot;
import uk.ac.starlink.splat.util.LineInterpolator;

/**
 * ModelLineTableModel extends AbstractTableModel to provide a description of
 * a set of {@link ModelLine}s. These are interactive graphical
 * representations of types of spectral lines (Gaussian, Lorentzian and Voigt).
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see InterpolatedCurveFigure
 */
public class ModelLineTableModel
    extends AbstractTableModel
{
    /**
     * The plot that displays the figures we're describing. Used to give
     * better control over the formatting.
     */
    private DivaPlot plot = null;

    /**
     * Create an instance of this class.
     */
    public ModelLineTableModel( DivaPlot plot )
    {
        this.plot = plot;
    }

    /**
     * List of the ModelLines we're managing.
     */
    protected ArrayList lineList = new ArrayList();

    //
    //  Implement rest of ListModel interface (listeners are free from
    //  AbstractListModel)
    //
    /**
     * Return the row count (number of figures).
     */
    public int getRowCount()
    {
        return lineList.size();
    }

    /**
     * Return the number of columns.
     */
    public int getColumnCount()
    {
        return 5;
    }

    /**
     * Return the value of an element.
     */
    public Object getValueAt( int row, int column )
    {
        //  Identifier.
        if ( column == 0 ) {
            return new Integer( row );
        }

        //  Gather line properties.
        ModelLine line = (ModelLine) lineList.get( row );
        double[] props = line.getProps();

        if ( column == 1 ) {
            //  X coordinate format using AST.
            return new AstDouble( props[LineInterpolator.CENTRE],
                                  plot.getMapping(), 1 );
        }
        else if ( column == 2 ) {
            return new Double( props[LineInterpolator.SCALE] );
        }
        else if ( column == 3 ) {
            return new Double( props[LineInterpolator.GWIDTH] );
        }
        return new Double( props[LineInterpolator.LWIDTH] );
    }

    /**
     * Set the value of an element (when edited by hand).
     *
     * @param oValue the new value.
     * @param row the new value row.
     * @param column the new value column.
     */
    public void setValueAt( Object oValue, int row, int column )
    {
        if ( column == 0 ) {
            return;
        }
        try {
            double value = 0.0;

            //  oValue can be String or a Number.
            if ( oValue instanceof String ) {
                value = AstDouble.parseDouble( (String) oValue,
                                               plot.getMapping(), 1 );
            }
            else if ( oValue instanceof Number ) {
                value = ( (Number) oValue ).doubleValue();
            }

            //  Gather current line properties.
            ModelLine line = (ModelLine) lineList.get( row );
            double[] props = line.getProps();

            // Set the required one.
            if ( column == 1 ) {
                props[LineInterpolator.CENTRE] = value;
            }
            else if ( column == 2 ) {
                props[LineInterpolator.SCALE] = value;
            }
            else if ( column == 3 ) {
                props[LineInterpolator.GWIDTH] = value;
            }
            else {
                props[LineInterpolator.LWIDTH] = value;
            }
            line.setProps( props );
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * Return if a cell is editable.
     */
    public boolean isCellEditable( int row, int column )
    {
        if ( column == 0 ) {
            return false;
        }
        return true;
    }

    /**
     * Return the column names.
     */
    public String getColumnName( int column )
    {
        switch (column) {
           case 0: {
               return "Id";
           }
           case 1: {
               return "Centre";
           }
           case 2: {
               return "Scale";
           }
           case 3: {
               return "GWidth";
           }
           default: {
               return "LWidth";
           }
        }
    }

    /**
     * Return the column classes.
     */
    public Class getColumnClass( int index )
    {
        if ( index == 0 ) {
            return Integer.class;
        }
        if ( index == 1 ) {
            return AstDouble.class;
        }
        return Double.class;
    }

    //
    //  Bespoke interface. Allow the addition, removal and query of a
    //  new figures.
    //
    /**
     * React to a new ModelLine being added.
     */
    public void addModelLine( ModelLine modelLine )
    {
        lineList.add( modelLine );
        fireTableRowsInserted( getRowCount() - 1, getRowCount() - 1 );
    }

    /**
     * Lookup which ModelLine contains an InterpolatedCurveFigure.
     */
    public int findFigure( InterpolatedCurveFigure figure )
    {
        ModelLine line;
        for ( int i = 0, j = 0; i < getRowCount(); i++, j += 2 ) {
            line = (ModelLine) lineList.get( i );
            if ( line.isFigure( figure ) ) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Return a line by index.
     *
     * @param index index of figure.
     */
    public ModelLine getLine( int index )
    {
        return (ModelLine) lineList.get( index );
    }

    /**
     * React to a line being removed
     *
     * @param index list index of figure to remove.
     */
    public void remove( int index )
    {
        try {
            ModelLine line = (ModelLine) lineList.get( index );
            line.delete();
            lineList.remove( index );
            fireTableRowsDeleted( index, index );
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * React to a line being changed, just need to update its description.
     *
     * @param index list index of the figure to change.
     */
    public void change( int index )
    {
        fireTableDataChanged();
    }
}
