/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     7-MAR-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import nom.tam.fits.Header;

/**
 * Interface for SpecDataImpl derived classes that can offer access to a
 * set of FITS headers. Derived classes that clone themselves should
 * attempt to preserve the headers if they have the capability.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see SpecDataImpl
 * @see NDFSpecDataImpl
 * @see FITSSpecDataImpl
 */
public interface FITSHeaderSource
{
    /**
     * If available offer the FITs headers as a nom.tam.fits.Header
     * object.
     */
    public Header getFitsHeaders();
}
