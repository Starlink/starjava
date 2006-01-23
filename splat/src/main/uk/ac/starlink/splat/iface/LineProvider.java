/*
 * Copyright (C) 2006 Particle Physics and Astronomy Research Council
 *
 *  History:
 *     09-JAN-2006 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.splat.data.SpecData;

/**
 * Defines an interface to be used for interacting with a {@link LineVisitor}
 * control.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public interface LineProvider
{
    /**
     * Move to a view of a line, possibilty restoring some related state
     * information.
     *
     * @param coords the coordinate of the line in wcs units.
     * @param coordFrame {@link Frame} defining the coordinate system and
     *                   units. This should be used to transform into the
     *                   coordinate system as understood by the LineProvider.
     * @param state previously returned state information, null for none.
     */
    public void viewLine( double coords, Frame coordFrame, Object state );

    /**
     * Display a spectrum. Can replace or add to those already displayed
     * depending on the context. This spectrum will already be in the global
     * list of spectra.
     *
     * @param specData the spectrum to display
     */
    public void viewSpectrum( SpecData specData );

    /**
     * Return any state information about the current line. What
     * this state contains is only understood by the LineProvider.
     *
     * @return an Object defining the current state
     */
    public Object getLineState();
}
