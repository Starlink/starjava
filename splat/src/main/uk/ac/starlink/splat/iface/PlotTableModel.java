/*
 * Copyright (C) 2000-2004 Central Laboratory of the Research Councils
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     04-OCT-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import javax.swing.table.AbstractTableModel;

import uk.ac.starlink.splat.util.FuzzyBoolean;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * PlotTableModel is an implementation of the TableModel interface for
 * mediating between the GlobalSpecPlotList object and a viewer of the
 * available plots and what spectra they are displaying.
 *
 * @version $Id$
 * @author Peter W. Draper
 */
public class PlotTableModel extends AbstractTableModel
    implements PlotListener
{
    /**
     * Reference to the GlobalSpecPlotList object. This is an
     * interface to all information about plot and spectra availability.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * Indices of the current spectra.
     */
    protected int[] specIndices = null;

    /**
     *  Create an instance of this class.
     */
    public PlotTableModel()
    {
        // Register ourselves as interested in plot changes.
        globalList.addPlotListener( this );
    }

    /**
     * Free any locally allocated resources.
     */
    public void finalize() throws Throwable
    {
        globalList.removePlotListener( this );
        super.finalize();
    }

//
//  Implement rest of ListModel interface (listeners are free from
//  AbstractTableModel)
//
    /**
     *  Returns the number of records managed by the data source
     *  object (i.e.<!-- --> the number of plots).
     */
    public int getRowCount()
    {
        return globalList.plotCount();
    }

    /**
     *  Returns the number of columns. Always two, the plot name and
     *  whether it is displaying the "current" spectrum.
     */
    public int getColumnCount()
    {
        return 2;
    }

    /**
     *  Return the value of a given cell.
     */
    public Object getValueAt( int row, int column )
    {
        if ( column == 0 ) {
            return globalList.getPlotName( row );
        }

        if ( specIndices != null ) {
            int ndisp = 0;
            for ( int i = 0; i < specIndices.length; i++ ) {
                if ( globalList.isDisplaying( row, specIndices[i] ) ) {
                    ndisp++;
                }
            }
            if ( ndisp == specIndices.length ) {
                return new FuzzyBoolean( FuzzyBoolean.TRUE );
            }
            else if ( ndisp > 0 ) {
                return new FuzzyBoolean( FuzzyBoolean.MAYBE );
            }
        }
        return new FuzzyBoolean( FuzzyBoolean.FALSE );
    }

    /**
     *  Return the column names.
     */
    public String getColumnName( int index )
    {
        if ( index == 0 ) {
            return "View";
        }
        return "Displayed";
    }

    /**
     *  Return the column classes.
     */
    public Class getColumnClass( int index )
    {
        return getValueAt( 0, index ).getClass();
    }

    /**
     *  Displayed field is editable.
     */
    public boolean isCellEditable( int row, int column )
    {
        if ( column == 1 ) {
            return true;
        }
        return false;
    }

    /**
     *  Must be able to change displayed status of the selected spectra.
     */
    public void setValueAt( Object value, int row, int column )
    {
        if ( specIndices != null ) {
            if ( column == 1 ) {
                FuzzyBoolean add = (FuzzyBoolean) value;
                if ( add.isTrue() || add.isMaybe() ) {
                    for ( int i = 0; i < specIndices.length; i++ ) {
                        try {
                            globalList.addSpectrum( row, specIndices[i] );
                        }
                        catch (SplatException e) {
                            // Not happy to do this, but these
                            // messages should probably not just be
                            // dumped to the terminal.
                            //e.printStackTrace();
                            ErrorDialog.showError( null, e );
                        }
                    }
                }
		else {
		    // Remove.
                    for ( int i = 0; i < specIndices.length; i++ ) {
                        globalList.removeSpectrum( row, specIndices[i] );
                        if ( specIndices == null ) break;
                    }
                }
            }
        }
    }

//
//  Implement the PlotListener interface.
//
    /**
     *  React to a new plot being created.
     */
    public void plotCreated( PlotChangedEvent e )
    {
        int index = e.getIndex();
        fireTableRowsInserted( index, index );
    }

    /**
     *  React to a plot being removed.
     */
    public void plotRemoved( PlotChangedEvent e )
    {
        int index = e.getIndex();
        fireTableRowsDeleted( index, index );
    }

    /**
     *  React to a plot change, i.e.<!-- --> a spectrum added. Don't know
     *  which one (could be current added to a plot), so redraw
     *  everything.
     */
    public void plotChanged( PlotChangedEvent e )
    {
        fireTableDataChanged();
    }

//
//  Customised support interface.
//
    /**
     * Set the indices of the currently selected spectra. Keep this
     * up to date.
     *
     * @param specIndices array of indices of the selected spectra.
     */
    public void setSpectraSelectedIndices( int[] specIndices )
    {
        this.specIndices = specIndices;
        fireTableDataChanged();
    }
}
