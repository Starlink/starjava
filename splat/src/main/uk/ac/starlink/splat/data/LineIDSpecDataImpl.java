/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     15-APR-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import uk.ac.starlink.splat.util.SplatException;

/**
 * Interface for accessing line identification data stored
 * in various formats. Implementers of this interface are expected to
 * offer editable services, so a good target to extend is
 * {@link MEMSpecDataImpl}.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public interface LineIDSpecDataImpl
    extends EditableSpecDataImpl
{
    /**
     * Return the line identification labels.
     *
     * @return an array of Strings that are the line identification
     * labels.
     */
    public String[] getLabels();

    /**
     * Set all the line identification labels. The size must match ant
     * existing local coordinate arrays, otherwise a {@link SplatException}
     * will be thrown.
     *
     * @param labels the line identification labels.
     */
    public void setLabels( String[] labels )
        throws SplatException;

    /**
     * Get a specific label for a line by index.
     *
     * @param index the index of the label required.
     */
    public String getLabel( int index );

    /**
     * Set a specific line identification label.
     *
     * @param index the index of the line. If out of range nothing is
     *              done.
     * @param label the line identification string.
     */
    public void setLabel( int index, String label );

    /**
     * Return if the implementation has any data positions.
     */
    public boolean haveDataPositions();
}

