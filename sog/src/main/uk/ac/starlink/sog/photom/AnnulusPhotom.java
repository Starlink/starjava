/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-OCT-2002 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.sog.photom;

import org.w3c.dom.Element;

/**
 * Extends BasePhotom to add facilities for storing the inner and
 * outer scale of an annular region around the aperture. This is
 * usually used to estimate the local sky value.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AnnulusPhotom extends BasePhotom
{
    /** The inner scale of the annulus */
    private Double innerscale = new Double( 1.5 );

    /** The outer scale of the annulus */
    private Double outerscale = new Double( 2.5 );

    /**
     * Create an instance with default values all around.
     */
    public AnnulusPhotom()
    {
        super();
    }

    /**
     * Get the innerscale
     *
     * @return the value
     */
    public double getInnerscale()
    {
        return innerscale.doubleValue();
    }

    /**
     * Set the innerscale
     *
     * @param value The innerscale
     */
    public void setInnerscale( double value )
    {
        if ( Double.compare( innerscale.doubleValue(), value ) != 0 ) {
            this.innerscale = new Double( value );
            fireChanged();
        }
    }

    /**
     * Get the outerscale
     *
     * @return the value
     */
    public double getOuterscale()
    {
        return outerscale.doubleValue();
    }

    /**
     * Sets the outerscale
     *
     * @param value the value
     */
    public void setOuterscale( double value )
    {
        if ( Double.compare( outerscale.doubleValue(), value ) != 0 ) {
            this.outerscale = new Double( value );
            fireChanged();
        }
    }

    //  Compare the values of this object with another of the same
    //  type.
    public boolean sameValue( AnnulusPhotom comparison )
    {
        if ( Double.compare( getOuterscale(),
                             comparison.getOuterscale() ) != 0 ) {
            return false;
        }
        if ( Double.compare( getInnerscale(),
                             comparison.getInnerscale() ) != 0 ) {
            return false;
        }
        return super.sameValue( comparison );
    }

    //  Create a clone of this object.
    public Object clone()
    {
        AnnulusPhotom clone = (AnnulusPhotom) super.clone();
        clone.setOuterscale( getOuterscale() );
        clone.setInnerscale( getInnerscale() );
        return (Object) clone;
    }

    /**
     * Return a string representation of this object.
     */
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( super.toString() );
        buffer.append( ",outerscale[" + outerscale + "]," );
        buffer.append( "innerscale[" + innerscale + "]," );
        return buffer.toString();
    }

    /**
     * Return a count of the number of values used by this class.
     */
    public static int getNumberValues()
    {
        return 2 + BasePhotom.getNumberValues();
    }
    public int getSpecificNumberValues()
    {
        return AnnulusPhotom.getNumberValues();
    }

    /**
     * Return a value stored by this class by index. Starts from 0 to
     * getNumberValues(), in the same order as the short descriptions
     * returned by getDescription. Returns an Object, which will be a
     * {@link Number} of some kind for numeric values and a String
     * otherwise.
     */
    public Object getValue( int index )
    {
        int base = super.getNumberValues();
        if ( index < base ) {
            return super.getValue( index );
        }
        if ( index == base ) {
            return innerscale;
        }
        else if ( index == base + 1 ) {
            return outerscale;
        }
        throw new ArrayIndexOutOfBoundsException( index );
    }

    /**
     * Return a description of a value returned by index.
     */
    public static String getDescription( int index )
    {
        int base = BasePhotom.getNumberValues();
        if ( index < base ) {
            return BasePhotom.getDescription( index );
        }
        if ( index == base ) {
            return "innerscale";
        }
        else if ( index == base + 1 ) {
            return "outerscale";
        }
        throw new ArrayIndexOutOfBoundsException( index );
    }
    public String getSpecificDescription( int index )
    {
        return AnnulusPhotom.getDescription( index );
    }

//
// The XML encode/decode parts. Note these wrap BasePhotom
//
    // Report our tagname
    public String getTagName()
    {
        return "annulus-" + super.getTagName();
    }

    public void encode( Element rootElement )
    {
        addChildElement( rootElement, "outerscale", getOuterscale() );
        addChildElement( rootElement, "innerscale", getInnerscale() );
        super.encode( rootElement );
    }

    public void setFromString( String name, String value )
    {
        if ( name.equals( "outerscale" ) ) {
            setOuterscale( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "innerscale" ) ) {
            setInnerscale( doubleFromString( value ) );
            return;
        }
        super.setFromString( name, value );
    }
}
