package uk.ac.starlink.frog.data;

import nom.tam.fits.Header;

/**
 * Interface for TimeSeriesImpl derived classes that can offer access to a
 * set of FITS headers. Derived classes that clone themselves should
 * attempt to preserve the headers if they have the capability.
 *
 * @since $Date$
 * @since 7-MAR-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 * @see TimeSeriesImpl, FITSTimeSeriesImpl
 */
public interface FITSHeaderSource
{
    /**
     * If available offer the FITs headers as a nom.tam.fits.Header
     * object.
     */
    public Header getFitsHeaders();
}
