/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-NOV-2002 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.sog.photom;

import org.w3c.dom.Element;
import uk.ac.starlink.util.PrimitiveXMLEncodeDecode;

/**
 * Provides a model of the global parameters that may be required 
 * when performing photometric measurements. 
 * <p>
 * An important task of this class is to provide the facilities to
 * save and restore an object of this type to and from an XML stream.

 * outer scale of an annular region around the aperture. This is
 * usually used to estimate the local sky value.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PhotometryGlobals 
    extends PrimitiveXMLEncodeDecode
    implements Cloneable
{
    /** The photometric zero point */
    private double zeroPoint;

    /** Whether apertures should be centroided */
    private boolean centroid;

    /**
     * Create an instance with default values.
     */
    public PhotometryGlobals()
    {
        super();
        reset();
    }

    /**
     * Reset all values to their defaults.
     */
    public void reset()
    {
        zeroPoint = 50.0;
        centroid = true;
        fireChanged();
    }

    /**
     * Get the photometry zero point.
     *
     * @return the value
     */
    public double getZeroPoint()
    {
        return zeroPoint;
    }

    /**
     * Set the photometry zero point.
     *
     * @param value The zero point
     */
    public void setZeroPoint( double value )
    {
        if ( Double.compare( zeroPoint, value ) != 0 ) {
            zeroPoint = value;
            fireChanged();
        }
    }

    /**
     * Get whether apertures are centroided.
     *
     * @return the value
     */
    public boolean getCentroid()
    {
        return centroid;
    }

    /**
     * Set whether to centroid the apertures.
     *
     * @param value the value
     */
    public void setCentroid( boolean value )
    {
        if ( centroid != value ) {
            centroid = value;
            fireChanged();
        }
    }

    //  Compare the values of this object with another of the same
    //  type.
    public boolean sameValue( PhotometryGlobals comparison )
    {
        if ( Double.compare( zeroPoint, comparison.getZeroPoint() ) != 0 ) {
            return false;
        }
        if ( centroid != comparison.getCentroid() ) {
            return false;
        }
        return true;
    }

    //  Create a clone of this object.
    public Object clone()
    {
        try {
            PhotometryGlobals clone = (PhotometryGlobals) super.clone();
            clone.setZeroPoint( zeroPoint );
            clone.setCentroid( centroid );
            return (Object) clone;
        }
        catch (CloneNotSupportedException e) {
            //  Should never happen, just removing need to trap from
            //  caller.
            throw new RuntimeException( "Clone failed: " + e.getMessage() );
        }
    }

    /**
     * Return a string representation of this object.
     */
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "zeropoint[" + zeroPoint + "]" );
        buffer.append( ",centroid[" + centroid + "]" );
        return buffer.toString();
    }
    
    /**
     * Return a string representation that can be used for configuring
     * an external application (AUTOPHOTOM).
     */
    public String toApplicationString()
    {
        return PhotomEncoderAndDecoder.toApplicationString( this );
    }

//
// The XML encode/decode parts.
//
    // Report our tagname
    public String getTagName()
    {
        return "photometry-globals";
    }

    public void encode( Element rootElement )
    {
        addChildElement( rootElement, "zeropoint", zeroPoint );
        addChildElement( rootElement, "centroid", centroid );
    }

    public void setFromString( String name, String value )
    {
        if ( name.equals( "zeropoint" ) ) {
            setZeroPoint( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "centroid" ) ) {
            setCentroid( booleanFromString( value ) );
            return;
        }
    }
}
