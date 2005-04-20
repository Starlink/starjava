/*
 * Copyright (C) 2000-2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     28-SEP-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.plot;

import java.util.ArrayList;

import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.util.SplatException;

/**
 * PlotControlList is a singleton object that contains references to
 * all the known Plots and the spectra that they are (should be)
 * displaying. The PlotControlList object should be updated with any
 * new PlotControls when they are created (by PlotControl) and also
 * updated whenever a spectrum is destroyed, added, removed or given 
 * current status.
 *
 * @version $Id$
 * @author Peter W. Draper
 *
 * @see DivaPlot
 * @see PlotControl
 * @see "The Singleton Design Pattern"
 */
public class PlotControlList
{
    /**
     *  Create the single class instance.
     */
    private static final PlotControlList instance = new PlotControlList();

    /**
     *  Hide the constructor from use.
     */
    private PlotControlList() {}

    /**
     *  Return reference to the only allowed instance of this class.
     */
    public static PlotControlList getInstance()
    {
        return instance;
    }

    /**
     *  ArrayList of references to plots.
     */
    protected ArrayList plots = new ArrayList();

    /**
     *  Get the number of plots.
     */
    public int count()
    {
        return plots.size();
    }

    /**
     *  Get the number of spectra a plot is displaying.
     */
    public int specCount( int index )
    {
        return ((PlotControl)plots.get( index )).specCount();
    }

    /**
     *  Add a plot.
     */
    public int add( PlotControl plot )
    {
        plots.add( plot );
        return plots.size() - 1;

    }

    /**
     *  Remove a plot.
     */
    public int remove( PlotControl plot )
    {
        int index = plots.indexOf( plot );
        plots.remove( index );
        return index;
    }

    /**
     *  Remove a plot.
     */
    public void remove( int index )
    {
        plots.remove( index );
    }

    /**
     *  Add a spectrum to a plot.
     */
    public void addSpectrum( PlotControl plot, SpecData spec )
        throws SplatException
    {
        plot.addSpectrum( spec );
    }

    /**
     *  Add a spectrum to a plot.
     */
    public void addSpectrum( int index, SpecData spec )
        throws SplatException
    {
        ((PlotControl)plots.get( index ) ).addSpectrum( spec );
    }

    /**
     *  Remove a spectrum from a plot.
     *  Remove a Plot as view for a spectrum.
     */
    public void removeSpectrum( PlotControl plot, SpecData spec )
    {
        plot.removeSpectrum( spec );
    }

    /**
     *  Remove a spectrum from a plot.
     *  Remove a Plot as view for a spectrum.
     */
    public void removeSpectrum( int index, SpecData spec )
    {
        ((PlotControl)plots.get( index ) ).removeSpectrum( spec );
    }

    /**
     *  Remove a spectrum from a plot.
     */
    public void removeSpectrum( int plotIndex, int specIndex )
    {
        ((PlotControl)plots.get( plotIndex )).removeSpectrum( specIndex );
    }

    /**
     * Return a name for a plot. This is <plotN>, but note that the N
     * isn't the local index, this is some other unique index (which
     * grows by one for each plot created, no account of removed plots
     * is made).
     *
     * @param index the index of the plot whose "name" is required.
     */
    public String plotName( int index )
    {
        return ((PlotControl)plots.get( index )).getName();
    }

    /**
     * Return the unique identifier for a plot. This is the N part of
     * <plotN> and is unique amongst all plots.
     *
     * @param index the index of the plot whose identifier is required.
     */
    public int getPlotIdentifier( int index )
    {
        return ((PlotControl)plots.get( index )).getIdentifier();
    }

    /**
     * See if a plot with the given integer identifier exists, and if
     * so return its index. The identifier of a plot is shown as part
     * of its title (the N part of <plotN>).
     *
     * @param identifier the index of the plot whose "name" is required.
     * @return the plot index, or -1 if not located.
     */
    public int indexOf( int identifier )
    {
        int index = -1;
        for ( int i = 0; i < plots.size(); i++ ) {
            if ( getPlotIdentifier( i ) == identifier ) {
                index = i;
                break;
            }
        }
        return index;
    }

    /**
     *  Get a reference to a plot by index.
     */
    public PlotControl get( int index )
    {
        return (PlotControl)plots.get( index );
    }

    /**
     *  Get the index of a plot.
     */
    public int indexOf( PlotControl plot )
    {
        return plots.indexOf( plot );
    }

    /**
     *  Return if a given plot is displaying a given spectrum.
     */
    public boolean isDisplaying( int plotIndex, SpecData spec )
    {
        return ((PlotControl)plots.get( plotIndex )).isDisplayed( spec );
    }
}
