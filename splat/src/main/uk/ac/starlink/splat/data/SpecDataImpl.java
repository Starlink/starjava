/*
 * Copyright (C) 2000-2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     01-SEP-2000 (Peter W. Draper):
 *        Original version.
 *     26-FEB-2004 (Peter W. Draper):
 *        Added column selection methods (for table-like support). 
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
     * found. Properties which should usually be made available are "units"
     * and "label", which describe the data values, not the
     * coordinates. Values for these can be set using the 
     * {@link #setDataUnits} and {@link setDataLabel} methods.
     */
    public String getProperty( String key );

    /**
     * Set the units of the data values contained in this spectrum. Usually
     * these will be fluxes or counts of some kind. The units should ideally
     * match the ones understood by AST (this those in FITS paper I).
     *
     * @param units the data value units (Jy etc).
     */
    public void setDataUnits( String units );

    /**
     * Get the units of the data values. When not set this should be "unknown".
     */
    public String getDataUnits();

    /**
     * Set the label of the data values contained in this spectrum. This will
     * be used in plots as a label for the data axis and has no other meaning.
     * 
     * @param label the label describing the data values (flux density etc).
     */
    public void setDataLabel( String label );

    /**
     * Get the label of the data values. When not set this should be 
     * "data values". 
     */
    public String getDataLabel();

    /**
     * Whether the class is a FITSHeaderSource. This means it has FITS
     * headers associated with it and implements FITSHeaderSource.
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
     * related to, so that information in the MORE extension can be
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

    /**
     * Get a list of the column names. If the implementation doesn't
     * support this feature (for instance they are immutable), then
     * this method should return null, but the other column name
     * methods should return suitable symbolic names.
     */
    public String[] getColumnNames();

    /**
     * Get the current column name for the coordinates.
     */
    public String getCoordinateColumnName();

    /**
     * If possible set the column name for the coordinate and change
     * the coordinates used in the AST frameset if this is
     * possible. If the modification is not possible then nothing
     * should be done in response to this method. The name must match
     * one of the values returned by
     * {@link #getColumnNames}.
     */
    public void setCoordinateColumnName( String name )
        throws SplatException;

    /**
     * Get the current column name for the data values.
     */
    public String getDataColumnName();

    /**
     * If possible set the column name for the data values and change
     * the data values returned by {@link #getData} if this is
     * possible. If the modification is not possible then nothing
     * should be done in response to this method. The name must match
     * one of the values returned by {@link #getColumnNames}.
     */
    public void setDataColumnName( String name )
        throws SplatException;

    /**
     * Get the current column name for the data value errors.
     */
    public String getDataErrorColumnName();

    /**
     * If possible set the column name for the data value errors and change
     * the errors returned by {@link #getDataErrors} if this is
     * possible. If the modification is not possible then nothing
     * should be done in response to this method. The name must match
     * one of the values returned by {@link #getColumnNames}.
     */
    public void setDataErrorColumnName( String name )
        throws SplatException;
    
    /**
     * Getter for object type that identifies type of object (spectrum or timeseries)
     * FIXME: This is a hacky way for quick and partial timeseries implementation
     * @return
     */
    public ObjectTypeEnum getObjectType();
    
    /**
     * /**
     * Setter for object type that identifies type of object (spectrum or timeseries)
     * FIXME: This is a hacky way for quick and partial timeseries implementation
     
     * @param objectType
     */
    public void setObjectType(ObjectTypeEnum objectType);
}
