/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     14-FEB-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.util.ArrayList;

import uk.ac.starlink.splat.data.SpecData;

/**
 * Manage a list of selected spectra. Provides basic facilities to
 * add, remove and clear the list.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see SpecData
 */
public class SpecSubList
{
    /**
     * Storage for the list of spectral references we're handling.
     */
    protected ArrayList localList = new ArrayList( 5 );

    /**
     *  Reference to GlobalSpecPlotList object.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * Create an instance of this class.
     */
    public SpecSubList()
    {
        //  Nothing to do.
    }

    /**
     * Add a spectrum.
     */
    public void add( SpecData spectrum )
    {
        localList.add( spectrum );
    }

    /**
     * Clear the list deleting the associated spectra. Removes from
     * the global list.
     */
    public void deleteAll()
    {
        for ( int i = 0; i < localList.size(); i++ ) {
            globalList.removeSpectrum( (SpecData)localList.get( i ) );
        }
        localList.clear();
    }

    /**
     * Clear the list.
     */
    public void clear()
    {
        localList.clear();
    }

    /**
     * Get the number of spectra in the list.
     */
    public int size()
    {
        return localList.size();
    }

    /**
     * Get one of the spectra.
     */
    public SpecData get( int index )
    {
        try {
            return (SpecData) localList.get( index );
        } catch (Exception e) {
            return null;
        }
    }
}

