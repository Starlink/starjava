/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     01-SEP-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import java.io.Serializable;

import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.ast.FrameSet;

/**
 * Interface for accessing spectral data stored in various data formats.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see "The Bridge Design Pattern"
 */
public interface SpecDataImpl
{
    /**
     * Return a complete array of data values.
     */
    public double[] getData();

    /**
     * Return a complete array of data error values.
     */
    public double[] getDataErrors();

    /**
     * Get the dimensionality of the spectrum.
     */
    public int[] getDims();

    /**
     * Return a symbolic name for the spectrum.
     */
    public String getShortName();

    /**
     * Return a full name for the spectrum (this should be the disk
     * file is associated, otherwise a URL or null).
     */
    public String getFullName();

    /**
     * Return reference to AST frameset that specifies the
     * coordinates associated with the data value positions.
     */
    public FrameSet getAst();

    /**
     * Return the data format as a recognisable short name.
     */
    public String getDataFormat();

    /**
     * Ask spectrum if it can save itself to disk file. Throws
     * SplatException is this fails, or is not available.
     */
    public void save() throws SplatException;

    /**
     * Return a keyed value from the FITS headers. Returns "" if not
     * found.
     */
    public String getProperty( String key );

    /**
     * Whether the class is a FITSHeaderSource, i.e.<!-- --> has FITS headers
     * associated with it and implements FITSHeaderSource.
     * Implementations that can store FITS headers may want to access
     * these.
     */
    public boolean isFITSHeaderSource();

    /**
     * Reference to another SpecDataImpl that is a "parent" of this
     * instance. This facility is provided so that data formats that
     * have extra information that is outside of the model inferred by
     * SpecData and SpecDataImpl may be able to recover this when
     * saving themselves to disk file (the case in mind is actually the
     * NDF, a new NDF should really be a copy of any NDF that it is
     * related too, so that information in the MORE extension can be
     * preserved).
     */
    public SpecDataImpl getParentImpl();

    /**
     * Set reference to another SpecDataImpl that is a "parent" of this
     * instance.
     *
     * @see #getParentImpl
     */
    public void setParentImpl( SpecDataImpl parentImpl );
}
