/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-SEP-2002 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.sog.photom;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.w3c.dom.Element;

import uk.ac.starlink.util.PrimitiveXMLEncodeDecode;

/**
 * Photom is a base-model of the data associated with a photometry
 * object. It attempts to be general about the measurements that such
 * an aperture could have associated with, but more specialized classes
 * should be used to extend the data model found here (for instance
 * for objects that have annular or sky region details).
 * <p>
 * An important task of this class is to provide the facilities to
 * save and restore an object of this type to and from an XML stream.
 * <p>
 * Note that there is no real meaning associated with the magnitude
 * value, so this can be used for storing any measurement that
 * describes the aperture (e.g. counts, rather than mags). This also
 * applies to the X and Y coordinates (could be pixel, radians,
 * degrees, whatever).
 * <p>
 * Sub-classes should override the following methods:
 * <ul>
 *   <li>{@link sameValue}</li>
 *   <li>{@link clone}</li>
 *   <li>{@link toString}</li>
 *   <li>{@link getTagName}</li>
 *   <li>{@link encode}</li>
 *   <li>{@link setFromString}</li>
 *   <li>{@link getNumberValues}</li>
 *   <li>{@link getSpecificNumberValues}</li>
 *   <li>{@link getValue}</li>
 *   <li>{@link getDescription}</li>
 *   <li>{@link getSpecificDescription}</li>
 * </ul>
 * @author Peter W. Draper
 * @version $Id$
 */
public class BasePhotom
    extends PrimitiveXMLEncodeDecode
    implements Cloneable
{
    /** A unique number used to generate unique identifiers (when one
     *  is not supplied). */
    private static int idCounter = 1;

    /** A default Double for initializations */
    protected Double defaultDouble = new Double( 0.0 );

    /** An identifier for this object */
    private Integer ident = new Integer( idCounter++ );

    /** The X coordinate */
    private Double xcoord = defaultDouble;

    /** The Y coordinate */
    private Double ycoord = defaultDouble;

    /** The magnitude */
    private Double mag = defaultDouble;

    /** The magnitude error */
    private Double magerr = defaultDouble;

    /** The sky value */
    private Double sky = defaultDouble;

    /** The signal in the aperture */
    private Double signal = defaultDouble;

    /** The shape of the aperture, circle or ellipse */
    private Integer shape = new Integer( CIRCLE );

    /** The semi-major axis length of the aperture */
    private Double semimajor = defaultDouble;

    /** The semi-minor axis length of the aperture */
    private Double semiminor = defaultDouble;

    /** The angle of the aperture, X through Y */
    private Double angle = defaultDouble;

    /** A status indicator, any string */
    private String status = "?";

    /** Shape of aperture is circular */
    public final static int CIRCLE = 0;
    public final static Integer INTEGER_CIRCLE = new Integer( CIRCLE );

    /** Shape of aperture is elliptical */
    public final static int ELLIPSE = 1;
    public final static Integer INTEGER_ELLIPSE = new Integer( ELLIPSE );

    /**
     * Default constructor. All values set to their defaults.
     */
    public BasePhotom()
    {
        // Nothing to do.
    }

    /**
     * Get the identifier
     */
    public int getIdent()
    {
        return ident.intValue();
    }

    /**
     * Set the identifier of this aperture.
     */
    public void setIdent( int value )
    {
        if ( ident.intValue() != value ) {
            ident = new Integer( value );
            fireChanged();
        }
    }

    /** Get the X coordinate value */
    public double getXcoord()
    {
        return xcoord.doubleValue();
    }

    /**
     * Set the value of the X coordinate.
     *
     * @param value The X coordinate of the aperture.
     */
    public void setXcoord( double value )
    {
        if ( Double.compare( xcoord.doubleValue(), value ) != 0 ) {
            xcoord = new Double( value );
            fireChanged();
        }
    }

    /** Get the Y coordinate value */
    public double getYcoord()
    {
        return ycoord.doubleValue();
    }

    /**
     * Set the value of the Y coordinate.
     *
     * @param value The Y coordinate of the aperture.
     */
    public void setYcoord( double value )
    {
        if ( Double.compare( ycoord.doubleValue(), value ) != 0 ) {
            ycoord = new Double( value );
            fireChanged();
        }
    }

    /** Get the magnitude value */
    public double getMagnitude()
    {
        return mag.doubleValue();
    }

    /**
     * Set the value of the magnitude.
     *
     * @param value The magnitude of the aperture.
     */
    public void setMagnitude( double value )
    {
        if ( Double.compare( mag.doubleValue(), value ) != 0 ) {
            mag = new Double( value );
            fireChanged();
        }
    }

    /** Get the error in the magnitude */
    public double getMagnitudeError()
    {
        return magerr.doubleValue();
    }

    /**
     * Set the value of the magnitude error.
     *
     * @param value The error in the magnitude of the aperture.
     */
    public void setMagnitudeError( double value )
    {
        if ( Double.compare( magerr.doubleValue(), value ) != 0 ) {
            magerr = new Double( value );
            fireChanged();
        }
    }

    /**
     * Get the sky value.
     *
     * @return the value
     */
    public double getSky()
    {
        return sky.doubleValue();
    }

    /**
     * Set the value of the sky.
     *
     * @param value the sky value
     */
    public void setSky( double value )
    {
        if ( Double.compare( sky.doubleValue(), value ) != 0 ) {
            sky = new Double( value );
            fireChanged();
        }
    }

    /**
     * Get the value of the total signal.
     *
     * @return the value of signal
     */
    public double getSignal()
    {
        return signal.doubleValue();
    }

    /**
     * Set the value of the total signal.
     *
     * @param value the value of the total signal
     */
    public void setSignal( double value )
    {
        if ( Double.compare( signal.doubleValue(), value ) != 0 ) {
            signal = new Double( value );
            fireChanged();
        }
    }

    /**
     * Get the shape
     *
     * @return the shape (CIRCLE or ELLIPSE)
     */
    public int getShape()
    {
        return shape.intValue();
    }

    /**
     * Set the shape, CIRCLE or ELLIPSE. The default/failsafe
     * is CIRCLE.
     *
     * @param value the value
     */
    public void setShape( int value )
    {
        if ( shape.intValue() != value ) {
            if ( value == ELLIPSE ) {
                shape = INTEGER_ELLIPSE;
            }
            else {
                shape = INTEGER_CIRCLE;
            }
            fireChanged();
        }
    }

    /**
     * Get the value of the semi-major axis.
     *
     * @return the value
     */
    public double getSemimajor()
    {
        return semimajor.doubleValue();
    }

    /**
     * Set the value of the semi-major axis.
     *
     * @param value the value
     */
    public void setSemimajor( double value )
    {
        if ( Double.compare( semimajor.doubleValue(), value ) != 0 ) {
            semimajor = new Double( value );
            fireChanged();
        }
    }

    /**
     * Get the value of semi-minor axis.
     *
     * @return the value
     */
    public double getSemiminor()
    {
        return semiminor.doubleValue();
    }

    /**
     * Set the semi-minor axis
     *
     * @param value the value
     */
    public void setSemiminor( double value )
    {
        if ( Double.compare( semiminor.doubleValue(), value ) != 0 ) {
            semiminor = new Double( value );
            fireChanged();
        }
    }

    /**
     * Get the position angle
     *
     * @return the value
     */
    public double getAngle()
    {
        return angle.doubleValue();
    }

    /**
     * Set the position angle
     *
     * @param value the value
     */
    public void setAngle( double value )
    {
        if ( Double.compare( angle.doubleValue(), value ) != 0 ) {
            angle = new Double( value );
            fireChanged();
        }
    }

    /**
     * Get the status record
     *
     * @return the value
     */
    public String getStatus()
    {
        return status;
    }

    /**
     * Set the status record. Use this as a store for any
     * information about the status of the aperture measurements (such
     * as contains bad values, touches an image edge etc).
     *
     * @param value the status record
     */
    public void setStatus( String value )
    {
        if ( ! status.equals( value ) ) {
            status = value;
            fireChanged();
        }
    }

    // ==================================
    // Routines sub-classes should extend
    // ==================================

    /**
     * Determine if another BasePhotom instance has the same values as
     * this one. Note this isn't the same as equal() as equal (in the
     * sense of this method), doesn't make them interchangeable (there
     * may be genuine or temporary reasons for two objects to have the
     * same values, including the ident). Sub-classes should also
     * implement this.
     */
    public boolean sameValue( BasePhotom comparison )
    {
        if ( comparison.getIdent() != getIdent() ) {
            return false;
        }
        if ( comparison.getXcoord() != getXcoord()) {
            return false;
        }
        if ( comparison.getYcoord() != getYcoord() ) {
            return false;
        }
        if ( comparison.getMagnitude() != getMagnitude() ) {
            return false;
        }
        if ( comparison.getMagnitudeError() != getMagnitudeError() ) {
            return false;
        }
        if ( comparison.getSky() != getSky() ) {
            return false;
        }
        if ( comparison.getSignal() != getSignal() ) {
            return false;
        }
        if ( comparison.getShape() != getShape() ) {
            return false;
        }
        if ( comparison.getSemimajor() != getSemimajor() ) {
            return false;
        }
        if ( comparison.getSemiminor() != getSemiminor() ) {
            return false;
        }
        if ( comparison.getAngle() != getAngle() ) {
            return false;
        }

        //  Status can be null so needs special treatment.
        String targetStatus = comparison.getStatus();
        if ( targetStatus != null ) {
            if ( ! targetStatus.equals( status ) ) {
                return false;
            }
        }
        if ( targetStatus == null && status != null ) {
            return false;
        }
        return true;
    }

    /**
     * Create a clone of this object. This does a deep copy of the
     * current state. Sub-classes should also implement this.
     */
    public Object clone()
    {
        try {
            BasePhotom clone = (BasePhotom) super.clone();
            clone.setIdent( getIdent() );
            clone.setXcoord( getXcoord() );
            clone.setYcoord( getYcoord() );
            clone.setMagnitude( getMagnitude() );
            clone.setMagnitudeError( getMagnitudeError() );
            clone.setSky( getSky() );
            clone.setSignal( getSignal() );
            clone.setShape( getShape() );
            clone.setSemimajor( getSemimajor() );
            clone.setSemiminor( getSemiminor() );
            clone.setAngle( getAngle() );
            clone.setStatus( getStatus() );
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
        buffer.append( "ident[" + ident + "]," );
        buffer.append( "xcoord[" + xcoord + "]," );
        buffer.append( "ycoord[" + ycoord + "]," );
        buffer.append( "mag[" + mag + "]," );
        buffer.append( "magerr[" + magerr + "]," );
        buffer.append( "sky[" + sky + "]," );
        buffer.append( "signal[" + signal + "]," );
        buffer.append( "shape[" + shape + "]," );
        buffer.append( "semimajor[" + semimajor + "]," );
        buffer.append( "semiminor[" + semiminor + "]," );
        buffer.append( "angle[" + angle + "]," );
        buffer.append( "status[" + status + "]" );
        return buffer.toString();
    }

    /** Array of descriptions for all values stored */
    private static String[] descriptions = {
        "ident", "xcoord", "ycoord", "mag", "magerr", "sky", "signal",
        "shape", "semimajor", "semiminor", "angle", "status"
    };

    /**
     * Return a count of the number of values used by this class.
     */
    public static int getNumberValues()
    {
        return descriptions.length;
    }

    /**
     * Return a count of the number of values used by this
     * object. This same as getNumberValues, except it will work
     * correctly for polymorphic objects.
     */
    public int getSpecificNumberValues()
    {
        return BasePhotom.getNumberValues();
    }

    /**
     * Return a value stored by this class by index. Starts from 0 to
     * getNumberValues(), in the same order as the short descriptions
     * returned by getDescription. Returns an Object, which will be a
     * {@link Number } of some kind for numeric values and a String
     * otherwise.
     */
    public Object getValue( int index )
    {
        switch ( index )
        {
           case 0:
               return ident;
           case 1:
               return xcoord;
           case 2:
               return ycoord;
           case 3:
               return mag;
           case 4:
               return magerr;
           case 5:
               return sky;
           case 6:
               return signal;
           case 7:
               return shape;
           case 8:
               return semimajor;
           case 9:
               return semiminor;
           case 10:
               return angle;
           case 11:
               return status;
           default:
               throw new ArrayIndexOutOfBoundsException( index );
        }
    }

    /**
     * Return a description of a value returned by index.
     */
    public static String getDescription( int index )
    {
        return descriptions[index];
    }

    /**
     * Return a description for the column. This same as
     * getDescription, except it will work correctly for polymorphic
     * objects.
     */
    public String getSpecificDescription( int index )
    {
        return BasePhotom.getDescription( index );
    }


//
// Implement abstract parts of PrimitiveXMLEncodeDecode
//

    public void encode( Element rootElement )
    {
        addChildElement( rootElement, "ident", getIdent() );
        addChildElement( rootElement, "xcoord", getXcoord() );
        addChildElement( rootElement, "ycoord", getYcoord() );
        addChildElement( rootElement, "mag", getMagnitude() );
        addChildElement( rootElement, "magerr", getMagnitudeError() );
        addChildElement( rootElement, "sky", getSky() );
        addChildElement( rootElement, "signal", getSignal() );
        addChildElement( rootElement, "shape", getShape() );
        addChildElement( rootElement, "semimajor", getSemimajor() );
        addChildElement( rootElement, "semiminor", getSemiminor() );
        addChildElement( rootElement, "angle", getAngle() );
        addChildElement( rootElement, "status", getStatus() );
    }

    /**
     * Set the value of a member variable by matching its name to a known
     * local property string.
     *
     * @param name symbolic name of the value
     * @param value the value encoded as a String
     */
    public void setFromString( String name, String value )
    {
        if ( name.equals( "ident" ) ) {
            setIdent( intFromString( value ) );
            return;
        }
        if ( name.equals( "xcoord" ) ) {
            setXcoord( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "ycoord" ) ) {
            setYcoord( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "mag" ) ) {
            setMagnitude( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "magerr" ) ) {
            setMagnitudeError( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "sky" ) ) {
            setSky( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "signal" ) ) {
            setSignal( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "shape" ) ) {
            setShape( intFromString( value ) );
            return;
        }
        if ( name.equals( "semimajor" ) ) {
            setSemimajor( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "semiminor" ) ) {
            setSemiminor( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "angle" ) ) {
            setAngle( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "status" ) ) {
            setStatus( value );
        }
        return;
    }

    // Report our tagname
    public String getTagName()
    {
        return "photom-base";
    }
}
