/*
 * Copyright (C) 2006 Particle Physics and Astronomy Research Council
 *
 *  History:
 *     19-JAN-2006 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import uk.ac.starlink.splat.util.SplatException;

/**
 * Interface for accessing line identification-like data that may have an
 * additional association column (usually a spectral specification).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public interface AssociatedLineIDSpecDataImpl
    extends LineIDSpecDataImpl
{
    /**
     * Return the associations.
     *
     * @return an array of Strings that are the associations.
     */
    public String[] getAssociations();

    /**
     * Set all the associations. The size must match any existing local
     * coordinate arrays, otherwise a {@link SplatException} will be thrown.
     *
     * @param associations the association strings.
     */
    public void setAssociations( String[] associations )
        throws SplatException;

    /**
     * Get a specific assocation of a line by index.
     *
     * @param index the index of the association required.
     */
    public String getAssociation( int index );

    /**
     * Set a specific line association.
     *
     * @param index the index of the line. If out of range nothing is
     *              done.
     * @param label the association string.
     */
    public void setAssociation( int index, String association );

    /**
     * Return if the implementation has associations.
     */
    public boolean haveAssociations();
}

