/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     27-AUG-2004 (Peter W. Draper):
 *        Original version.
 */
package uk.ac.starlink.splat.data;

import java.net.URL;

import uk.ac.starlink.splat.util.SplatException;

/**
 * This class extends {@link SpecData} so that a local cached copy of a remote
 * spectrum looks superficially like the remote data, not the cached copy.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see SpecData
 * @see SpecDataFactory
 */
public class RemoteSpecData
    extends SpecData
{
    /**
     * The URL of the remote spectrum.
     */
    protected URL url = null;

    //  Copy of the SpecData constructor. This provides no new functionality.
    public RemoteSpecData( SpecDataImpl impl )
        throws SplatException
    {
        super( impl, false );
        setURL( null );
    }

    /**
     * Create an instance using the data in a given SpecDataImpl object which
     * represents a remote spectrum.
     *
     * @param impl a concrete implementation of a SpecDataImpl class that is
     *             accessing spectral data in of some format.
     * @param url  the URL of the remote spectrum.
     * @exception SplatException thrown if there are problems obtaining
     *      spectrum.
     */
    public RemoteSpecData( SpecDataImpl impl, URL url )
        throws SplatException
    {
        super( impl, false );
        setURL( url );
    }

    //  Override setSpecDataImpl as when this occurs we need to reset the URL.
    protected void setSpecDataImpl( SpecDataImpl impl, boolean check )
        throws SplatException
    {
        setSpecDataImpl( impl, check, null );
    }

    /**
     * Set the {@link SpecDataImpl} instance of a remote spectrum.
     *
     * @param impl a concrete implementation of a SpecDataImpl class that
     *             will be used for spectral data in of some format.
     * @param check if true then a check for the presence of data will be
     *              made, before attempting a read. Otherwise no check will be
     *              made and problems will be indicated by throwing an error
     *              at a later time.
     * @param url  the URL of the remote spectrum.
     * @exception SplatException thrown if there are problems obtaining
     *            spectrum information.
     */
    protected void setSpecDataImpl( SpecDataImpl impl, boolean check, URL url )
        throws SplatException
    {
        super.setSpecDataImpl( impl, check );
        setURL( url );
    }

    /**
     * Set the URL of the remote spectrum.
     */
    protected void setURL( URL url )
    {
        this.url = url;
    }

    /**
     * Get the full name for spectrum. For a remote spectrum this is the URL.
     *
     * @return the full name.
     */
    public String getFullName()
    {
        if ( url != null ) {
            return url.toString();
        }
        return super.getFullName();
    }
}
