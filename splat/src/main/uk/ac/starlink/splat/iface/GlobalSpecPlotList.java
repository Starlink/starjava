/*
 * Copyright (C) 2000-2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     29-SEP-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.SourceDataLine;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecList;
import uk.ac.starlink.splat.iface.SpectrumIO.SourceType;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.plot.PlotControlList;
import uk.ac.starlink.splat.util.SplatException;

/**
 * GlobalSpecPlotList is an aggregate singleton class that provides access
 * to the SpecList and PlotControlList objects. It provides integrated
 * control interfaces to both these objects and provides listeners for
 * objects (i.e. views) that want to be updated about changes in the
 * lists of spectra or plots.
 *
 * @version $Id$
 * @author Peter W. Draper
 *
 * @see PlotControlList
 * @see SpecList
 * @see "The Singleton Design Pattern"
 */
public class GlobalSpecPlotList
{
//
//  Implement the singleton interface.
//
    /**
     *  Creates the single class instance.
     */
    private static final GlobalSpecPlotList instance= new GlobalSpecPlotList();

    /**
     *  Hide the constructor from use.
     */
    private GlobalSpecPlotList() {}

    /**
     *  Return reference to the only allowed instance of this class.
     *
     *  @return reference to the only instance of this class.
     */
    public static GlobalSpecPlotList getInstance()
    {
        return instance;
    }

    /**
     *  Reference to the SpecList object.
     */
    protected SpecList specList = SpecList.getInstance();

    /**
     *  Reference to the PlotControl object.
     */
    protected PlotControlList plotList = PlotControlList.getInstance();

//
//  Spectra changes interface.
//
    protected EventListenerList specListeners = new EventListenerList();
    
    /**
     * Map of PlotControls last used for specific source types
     */
    protected Map<SourceType, PlotControl> sourceTypesLastPlots = new HashMap<SourceType, PlotControl>();

    /**
     *  Return the number of spectra in the global list.
     *
     *  @return the number of spectra in the global list.
     */
    public int specCount()
    {
        return specList.specCount();
    }

    /**
     *  Return the full (usually diskfile) name of a spectrum.
     *
     *  @param index index of the spectrum.
     *
     *  @return full name of the spectrum.
     */
    public String getFullName( int index )
    {
        return specList.getFullName( index );
    }

    /**
     *  Return the symbolic name of a spectrum.
     *
     *  @param index index of th spectrum.
     *
     *  @return short name of te spectrum.
     */
    public String getShortName( int index )
    {
        return specList.getShortName( index );
    }

    /**
     *  Set the symbolic name of a spectrum.
     *
     *  @param index index of the spectrum to modify.
     *  @param name the new short name for the spectrum.
     */
    public void setShortName( int index, String name )
    {
        specList.setShortName( index, name );
        fireSpectrumChanged( index );
    }

    /**
     *  See if a spectrum is already known by a specification. This
     *  corresponds to the full name, so is usually the file name.
     *
     *  @param name name to check.
     *
     *  @return index if known, -1 otherwise.
     */
    public int specKnown( String name )
    {
        return specList.known( name );
    }

    /**
     *  Get a spectrum reference by index.
     *
     *  @param index index of spectrum to retrieve.
     *
     *  @return the SpecData object or null.
     */
    public SpecData getSpectrum( int index )
    {
        return specList.get( index );
    }

    /**
     *  Get an index for a spectrum.
     *
     *  @param spectrum SpecData object whose index is needed.
     *
     *  @return index of the spectrum or -1.
     */
    public int getSpectrumIndex( SpecData spectrum )
    {
        return specList.indexOf( spectrum );
    }
    /**
     *  Get an index for a spectrum using its short name.
     *
     *  @param shortName spectrum short name.
     *
     *  @return index of spectrum if found, -1 otherwise.
     */
    public int getSpectrumIndex( String shortName )
    {
        return specList.indexOf( shortName );
    }

    public SourceType getSourceType(SpecData spectrum) {
    	return specList.getSourceType(spectrum);
    }
    
    /**
     *  Add a spectrum to the global list. Informs any listeners.
     *
     *  @param spectrum reference to a SpecData object.
     *
     *  @return index of the spectrum in global list.
     */
    public int add( SpecData spectrum ) {
    	return add(spectrum, SourceType.UNDEFINED);
    }
    
    /**
     *  Add a spectrum to the global list. Informs any listeners.
     *
     *  @param spectrum reference to a SpecData object.
     *  @param sourceType source typefrom which the spectra came from
     *
     *  @return index of the spectrum in global list.
     */
    public int add( SpecData spectrum, SourceType sourceType )
    {
        if ( spectrum != null ) {
            int index = specList.add( spectrum , sourceType);
            fireSpectrumAdded( index );
            return index;
        }
        return -1;
    }

    /**
     *  Replace or add a spectrum. Informs any listeners of change.
     *
     *  @param index index of the spectrum to replace. Appended to end
     *               if index not used.
     *  @param spectrum reference to a SpecData object.
     *
     *  @return index of the spectrum in global list, or -1.
     */
    public int add( int index, SpecData spectrum ) {
    	return add(index, spectrum, SourceType.UNDEFINED);
    }
    
    /**
     *  Replace or add a spectrum. Informs any listeners of change.
     *
     *  @param index index of the spectrum to replace. Appended to end
     *               if index not used.
     *  @param spectrum reference to a SpecData object.
     *  @param sourceType source typefrom which the spectra came from
     *
     *  @return index of the spectrum in global list, or -1.
     */
    public int add( int index, SpecData spectrum, SourceType sourceType )
    {
        if ( spectrum != null ) {
            index = specList.add( index, spectrum, sourceType );
            fireCurrentSpectrumChanged();
            return index;
        }
        return -1;
    }

    /**
     *  Remove a spectrum. Note listeners are informed immediately
     *  before the spectrum is removed (so the lists remain current).
     *
     *  @param spectrum reference to the spectrum to remove.
     *
     *  @return index of the spectrum that was removed.
     */
    public int removeSpectrum( SpecData spectrum )
    {
        int index = getSpectrumIndex( spectrum );
        return removeSpectrum( index );
    }

    /**
     *  Remove a spectrum. Note listeners are informed immediately
     *  before the spectrum is removed (so the lists remain current).
     *
     *  @param index index of the spectrum to remove.
     *
     *  @return index of the spectrum that was removed.
     */
    public int removeSpectrum( int index )
    {
        fireSpectrumRemoved( index );
        specList.remove( index );
        return index;
    }

    /**
     *  Set a known Number property of a spectrum.
     *
     *  @param spectrum SpecData object to change.
     *
     *  @param what the property to change, see
     *               SpecData.setKnownNumberProperty.
     *  @param value Object containing the new value.
     *
     *  @see SpecData#setKnownNumberProperty
     */
    public void setKnownNumberProperty( SpecData spectrum, int what,
                                        Number value )
    {
        spectrum.setKnownNumberProperty( what, value );
        int index = specList.indexOf( spectrum );
        fireSpectrumChanged( index );
    }

    /**
     *  Set if a spectrum should be displaying it's error bars or
     *  not.
     *
     *  @param spectrum the spectrum to modify.
     *  @param show whether to show errorbars.
     */
    public void setDrawErrorBars( SpecData spectrum, boolean show )
    {
        spectrum.setDrawErrorBars( show );
        fireSpectrumChanged( specList.indexOf( spectrum ) );
    }

    /**
     *  Index of the "current" spectrum.
     */
    protected int currentSpectrum = 0;

    /**
     * Set the index of the current spectrum. Changes to this are
     * sent to any SpecListeners.
     *
     * @param index index of the current spectrum.
     */
    public void setCurrentSpectrum( int index )
    {
        if ( index != -1 ) {
            currentSpectrum = index;
            fireCurrentSpectrumChanged();
        }
    }

    /**
     *  Notify any listeners that the spectrum data has been changed.
     *
     *  @param spectrum the SpecData object that has changed.
     */
    public void notifySpecListenersChange( SpecData spectrum )
    {
        int index = specList.indexOf( spectrum );
        fireSpectrumChanged( index );
    }

    /**
     *  Notify any listeners that the spectrum data has been modified.
     *
     *  @param spectrum the SpecData object that has changed.
     */
    public void notifySpecListenersModified( SpecData spectrum )
    {
        int index = specList.indexOf( spectrum );
        fireSpectrumModified( index );
    }

    /**
     *  Registers a listener for to be informed when spectra are added
     *  or removed from the global list.
     *
     *  @param l the SpecListener
     */
    public void addSpecListener( SpecListener l )
    {
        specListeners.add( SpecListener.class, l );
    }

    /**
     * Remove a listener for changes in the global list of spectra.
     *
     * @param l the SpecListener
     */
    public void removeSpecListener( SpecListener l )
    {
        specListeners.remove( SpecListener.class, l );
    }

    /**
     * Send a SpecChangedEvent object specifying that a spectrum has
     * been added to all listeners for the global list of spectra.
     *
     * @param index Index of the spectrum that changed.
     */
    protected void fireSpectrumAdded( int index )
    {
        Object[] listeners = specListeners.getListenerList();
        SpecChangedEvent e = null;
        for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
            if ( listeners[i] == SpecListener.class ) {
                if ( e == null ) {
                    e = new SpecChangedEvent( this,
                                               SpecChangedEvent.ADDED,
                                               index );
                }
                ((SpecListener)listeners[i+1]).spectrumAdded( e );
            }
        }
    }

    /**
     * Send a SpecChangedEvent object specifying that a spectrum has
     * been removed to all listeners for the global list of spectra.
     *
     * @param index Index of the spectrum that changed.
     */
    protected void fireSpectrumRemoved( int index )
    {
        Object[] listeners = specListeners.getListenerList();
        SpecChangedEvent e = null;
        for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
            if ( listeners[i] == SpecListener.class ) {
                if ( e == null ) {
                    e = new SpecChangedEvent( this,
                                               SpecChangedEvent.REMOVED,
                                               index );
                }
                ((SpecListener)listeners[i+1]).spectrumRemoved( e );
            }
        }
    }

    /**
     * Send a SpecChangedEvent object specifying that a spectrum has
     * been changed to all listeners for the global list of spectra.
     *
     * @param index Index of the spectrum that changed.
     */
    protected void fireSpectrumChanged( int index )
    {
        Object[] listeners = specListeners.getListenerList();
        SpecChangedEvent e = null;
        for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
            if ( listeners[i] == SpecListener.class ) {
                if ( e == null ) {
                    e = new SpecChangedEvent( this, SpecChangedEvent.CHANGED,
                                              index );
                }
                ((SpecListener)listeners[i+1]).spectrumChanged( e );
            }
        }
    }

    /**
     * Send a SpecChangedEvent object specifying that a spectrum has
     * been modified to all listeners for the global list of spectra.
     *
     * @param index Index of the spectrum that changed.
     */
    protected void fireSpectrumModified( int index )
    {
        Object[] listeners = specListeners.getListenerList();
        SpecChangedEvent e = null;
        for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
            if ( listeners[i] == SpecListener.class ) {
                if ( e == null ) {
                    e = new SpecChangedEvent( this, SpecChangedEvent.MODIFIED,
                                              index );
                }
                ((SpecListener)listeners[i+1]).spectrumModified( e );
            }
        }
    }


    /**
     * Send a SpecChangedEvent object specifying that the current
     * spectrum has been changed to all listeners for the global list
     * of spectra.
     */
    protected void fireCurrentSpectrumChanged()
    {
        Object[] listeners = specListeners.getListenerList();
        SpecChangedEvent e = null;
        for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
            if ( listeners[i] == SpecListener.class ) {
                if ( e == null ) {
                    e = new SpecChangedEvent( this,
                                               SpecChangedEvent.CURRENT,
                                               currentSpectrum );
                }
                ((SpecListener)listeners[i+1]).spectrumCurrent( e );
            }
        }
    }

//
//  Plot changes interface.
//
    protected EventListenerList plotListeners = new EventListenerList();

    /**
     * Return the number of plots in the global list.
     *
     * @return number of current plots.
     */
    public int plotCount()
    {
        return plotList.count();
    }

    /**
     * Return the name of a plot.
     *
     * @param index index of spectrum.
     *
     * @return name of the plot.
     */
    public String getPlotName( int index )
    {
        return plotList.plotName( index );
    }

    /**
     * Get a plot reference by index.
     *
     * @param index index of required plot.
     *
     * @return reference to PlotControl object.
     */
    public PlotControl getPlot( int index )
    {
        return plotList.get( index );
    }

    /**
     * Get an index for a plot.
     *
     * @param plot PlotControl object to look up.
     *
     * @return global index of object, if found.
     */
    public int getPlotIndex( PlotControl plot )
    {
        return plotList.indexOf( plot );
    }

    /**
     * See if a plot with the given identifier exists, and if so
     * return its index. Note identifiers are a unique integer among
     * all plots and are not the plot index (which varies as plots are
     * removed).
     *
     * @param identifier the identifier of the plot whose existence is
     *                   to be checked.
     * @return the plot index, or -1 if not located.
     */
    public int getPlotIndex( int identifier )
    {
        return plotList.indexOf( identifier );
    }

    /**
     * 
     * @param sourceType
     * @return PlotControl that has been used for the sourceType last time (or NULL if none)
     */
    public PlotControl getLastPlotForSourceType(SourceType sourceType) {

    	PlotControl plotControl = sourceTypesLastPlots.get(sourceType);
    	if (plotControl != null) {
    		if (!((JFrame) SwingUtilities.getWindowAncestor(plotControl)).isVisible())
    			return null;
    	}

    	return plotControl;
    }
    
    public void setLastPlotForSourceType(SourceType sourceType, PlotControl plot) {
    	sourceTypesLastPlots.put(sourceType, plot);
    }
    
    /**
     *  Add a plot (immediately after creation).
     *
     *  @param plot reference to a PlotControl object.
     *
     *  @return global index of the plot.
     */
    public int add( PlotControl plot )
    {
        int index = plotList.add( plot );
        noteLastPlotForSourceType(plot, plot.getSpecDataComp().get());
        firePlotCreated( index );
        return index;
    }

    /**
     *  Remove a plot (immediately after destruction).
     *
     *  @param plot reference to the plot that has been deleted.
     *
     *  @return index of the removed plot.
     */
    public int remove( PlotControl plot )
    {
        int index = plotList.remove( plot );
        firePlotRemoved( index );
        return index;
    }

    /**
     *  Add a spectrum to a known plot.
     *
     *  @param plotIndex index of the plot to add spectrum to.
     *  @param spectrum the spectrum to add.
     */
    public void addSpectrum( int plotIndex, SpecData spectrum )
        throws SplatException
    {
        PlotControl plot = ((PlotControl)plotList.get( plotIndex ));
        plot.addSpectrum( spectrum );
        noteLastPlotForSourceType(plot, spectrum);
        firePlotChanged( plotIndex );
    }

    /**
     *  Add a list of spectra to a plot.
     *
     *  @param plotIndex index of the plot to add spectra to.
     *  @param spectra the spectra to add.
     */
    public void addSpectra( int plotIndex, SpecData spectra[] )
        throws SplatException
    {
    	PlotControl plot = ((PlotControl)plotList.get( plotIndex ));
    	plot.addSpectra( spectra );
        noteLastPlotForSourceType(plot, spectra);
        firePlotChanged( plotIndex );
    }

    /**
     *  Add a spectrum to a plot.
     *
     *  @param plot the plot to add spectrum to.
     *  @param spectrum the spectrum to add.
     */
    public void addSpectrum( PlotControl plot, SpecData spectrum )
        throws SplatException
    {
        plot.addSpectrum( spectrum );
        noteLastPlotForSourceType(plot, spectrum);
        int index = plotList.indexOf( plot );
        firePlotChanged( index );
    }
    
   

    /**
     *  Add a list of spectra to a plot.
     *
     *  @param plot the plot to add spectra to.
     *  @param spectra the spectra to add.
     */
    public void addSpectra( PlotControl plot, SpecData spectra[] )
        throws SplatException
    {
    	plot.addSpectra( spectra );
        noteLastPlotForSourceType(plot, spectra);
        int index = plotList.indexOf( plot );
        firePlotChanged( index );
    }

    /**
     *  Add a known spectrum to a plot.
     *
     *  @param plot the plot to add the spectrum to.
     *  @param specIndex global index of the spectrum.
     */
    public void addSpectrum( PlotControl plot, int specIndex )
        throws SplatException
    {
        SpecData spectrum = getSpectrum( specIndex );
        plot.addSpectrum( spectrum );
        noteLastPlotForSourceType(plot, spectrum);
        int index = plotList.indexOf( plot );
        firePlotChanged( index );
    }

    /**
     *  Add a list of known spectra to a plot.
     *
     *  @param plot the plot to add the spectra to.
     *  @param specIndices global indices of the spectra.
     */
    public void addSpectra( PlotControl plot, int specIndices[] )
        throws SplatException
    {
        SpecData spectra[] = new SpecData[specIndices.length];
        for ( int i = 0; i < specIndices.length; i++ ) {
            spectra[i] = getSpectrum( specIndices[i] );
        }
        plot.addSpectra( spectra );
        noteLastPlotForSourceType(plot, spectra);
        int index = plotList.indexOf( plot );
        firePlotChanged( index );
    }

    /**
     *  Add a known spectrum to a known plot.
     *
     *  @param plotIndex global index of the plot to add the spectrum to.
     *  @param specIndex global index of the spectrum.
     */
    public void addSpectrum( int plotIndex, int specIndex )
        throws SplatException
    {
        SpecData spectrum = getSpectrum( specIndex );
        PlotControl plot = getPlot( plotIndex );
        plot.addSpectrum( spectrum );
        noteLastPlotForSourceType(plot, spectrum);
        firePlotChanged( plotIndex );
    }

    /**
     *  Add a list of known spectra to a known plot.
     *
     *  @param plotIndex global index of the plot to add the spectra to.
     *  @param specIndices global indices of the spectra.
     */
    public void addSpectra( int plotIndex, int specIndices[] )
        throws SplatException
    {
        SpecData spectra[] = new SpecData[specIndices.length];
        for ( int i = 0; i < specIndices.length; i++ ) {
            spectra[i] = getSpectrum( specIndices[i] );
        }
        PlotControl plot = getPlot( plotIndex );
        plot.addSpectra( spectra );
        noteLastPlotForSourceType(plot, spectra);
        firePlotChanged( plotIndex );
    }

    /**
     *  Remove a known spectrum from a plot.
     *
     *  @param plotIndex global index of the plot.
     *  @param spectrum the spectrum to remove.
     */
    public void removeSpectrum( int plotIndex, SpecData spectrum )
    {
        ((PlotControl)plotList.get( plotIndex)).removeSpectrum( spectrum );
        firePlotChanged( plotIndex );
    }

    /**
     *  Remove a known spectrum from a plot.
     *
     *  @param plot the plot to remove the spectrum from.
     *  @param spectrum the spectrum to remove.
     */
    public void removeSpectrum( PlotControl plot, SpecData spectrum )
    {
        int plotIndex = plotList.indexOf( plot );
        plot.removeSpectrum( spectrum );
        firePlotChanged( plotIndex );
    }

    /**
     *  Remove a known spectrum from a plot.
     *
     *  @param plot the plot to remove the spectrum from.
     *  @param specIndex global index of the spectrum.
     */
    public void removeSpectrum( PlotControl plot, int specIndex )
    {
        int plotIndex = plotList.indexOf( plot );
        SpecData spectrum = getSpectrum( specIndex );
        plot.removeSpectrum( spectrum );
        firePlotChanged( plotIndex );
    }

    /**
     *  Remove a known spectrum from a plot.
     *
     *  @param plotIndex global index of the plot to remove the
     *                   spectrum from.
     *  @param specIndex global index of the spectrum.
     */
    public void removeSpectrum( int plotIndex, int specIndex )
    {
        SpecData spectrum = getSpectrum( specIndex );
        ((PlotControl)getPlot(plotIndex)).removeSpectrum( spectrum );
        firePlotChanged( plotIndex );
    }

    /**
     *  Return if a plot is displaying a given spectrum.
     *
     *  @param plotIndex global index of the plot to check.
     *  @param spectrum the spectrum to check.
     *
     *  @return true if the plot is displaying the spectrum.
     */
    public boolean isDisplaying( int plotIndex, SpecData spectrum )
    {
        return plotList.isDisplaying( plotIndex, spectrum );
    }

    /**
     *  Return if a plot is displaying a given spectrum.
     *
     *  @param plotIndex global index of the plot to check.
     *  @param specIndex global index of the spectrum.
     *
     *  @return true of the plot is displaying the spectrum.
     */
    public boolean isDisplaying( int plotIndex, int specIndex )
    {
        SpecData spectrum = getSpectrum( specIndex );
        return plotList.isDisplaying( plotIndex, spectrum );
    }

    /**
     *  Registers a listener for to be informed when plots are
     *  created, changed or removed.
     *
     *  @param l the PlotListener
     */
    public void addPlotListener( PlotListener l )
    {
        plotListeners.add( PlotListener.class, l );
    }

    /**
     * Remove a listener for plot changes.
     *
     * @param l the PlotListener
     */
    public void removePlotListener( PlotListener l )
    {
        plotListeners.remove( PlotListener.class, l );
    }

    /**
     * Send a PlotChangedEvent.CREATED event to all plot listeners.
     *
     * @param index Index of the plot that changed.
     */
    protected void firePlotCreated( int index )
    {
        Object[] listeners = plotListeners.getListenerList();
        PlotChangedEvent e = null;
        for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
            if ( listeners[i] == PlotListener.class ) {
                if ( e == null ) {
                    e = new PlotChangedEvent( this,
                                              PlotChangedEvent.CREATED,
                                              index );
                }
                ((PlotListener)listeners[i+1]).plotCreated( e );
            }
        }
    }

    /**
     * Send a PlotChangedEvent.REMOVED event to all plot listeners.
     *
     * @param index Index of the plot that changed.
     */
    protected void firePlotRemoved( int index )
    {
        Object[] listeners = plotListeners.getListenerList();
        PlotChangedEvent e = null;
        for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
            if ( listeners[i] == PlotListener.class ) {
                if ( e == null ) {
                    e = new PlotChangedEvent( this,
                                              PlotChangedEvent.REMOVED,
                                              index );
                }
                ((PlotListener)listeners[i+1]).plotRemoved( e );
            }
        }
    }

    /**
     * Send a PlotChangedEvent.CHANGED event to all plot listeners.
     *
     * @param index Index of the plot that changed.
     */
    protected void firePlotChanged( int index )
    {
        Object[] listeners = plotListeners.getListenerList();
        PlotChangedEvent e = null;
        for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
            if ( listeners[i] == PlotListener.class ) {
                if ( e == null ) {
                    e = new PlotChangedEvent( this,
                                              PlotChangedEvent.CHANGED,
                                              index );
                }
                ((PlotListener)listeners[i+1]).plotChanged( e );
            }
        }
    }
    
    /**
     * Creates a reference to the PlotControl that has been
     * used for a spectra's source type last time
     */
    protected void noteLastPlotForSourceType(PlotControl plot, SpecData spectrum) {
    	sourceTypesLastPlots.put(getSourceType(spectrum), plot);
    }
    
    /**
     * Creates a reference to the PlotControl that has been
     * used for a spectra's source type last time
     */
    protected void noteLastPlotForSourceType(PlotControl plot, SpecData[] spectra) {
    	for (int i = 0; i < spectra.length; i++) {
    		sourceTypesLastPlots.put(getSourceType(spectra[i]), plot);
    	}
    }
}
