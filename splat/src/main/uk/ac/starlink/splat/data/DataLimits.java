/*
 * Copyright (C) 2000-2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     27-OCT-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import org.w3c.dom.Element;
import uk.ac.starlink.ast.gui.AbstractPlotControlsModel;

/**
 * DataLimits defines the limits of a Plot. These can be explicit values two
 * for each axis, or indications to autoscale both or either of these axes.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see SpecDataComp
 */
public class DataLimits 
    extends AbstractPlotControlsModel
{
    /**
     * Whether to fit Plot to match the data X data range into it's
     * visible area.
     */
    private boolean xFit;

    /**
     * Whether to fit Plot to match the data Y data range into it's
     * visible area.
     */
    private boolean yFit;

    /**
     * Whether to autoscale the X axis.
     */
    private boolean xAutoscaled;

    /**
     * Whether to autoscale the Y axis.
     */
    private boolean yAutoscaled;

    /**
     * Whether the displayed range should be flipped along any X axis.
     */
    private boolean xFlipped;

    /**
     * Whether the displayed range should be flipped along any Y axis.
     */
    private boolean yFlipped;

    /**
     * Lower limit of the X axis.
     */
    private double xLower;

    /**
     * Upper limit of the X axis.
     */
    private double xUpper;

    /**
     * Lower limit of the Y axis.
     */
    private double yLower;

    /**
     * Upper limit of the Y axis.
     */
    private double yUpper;

    /**
     * Create an instance.
     */
    public DataLimits()
    {
        setDefaults();
    }

    /**
     * Set object back to its defaults.
     */
    public void setDefaults()
    {
        xAutoscaled = true;
        yAutoscaled = true;
        xLower = 0.0;
        xUpper = 0.0;
        yLower = 0.0;
        yUpper = 0.0;
        xFit = false;
        yFit = false;
        xFlipped = false;
        yFlipped = false;
        fireChanged();
    }

    /**
     * See if the X axis should be autoscaled.
     */
    public boolean isXAutoscaled()
    {
        return xAutoscaled;
    }

    /**
     * See if the Y axis should be autoscaled.
     */
    public boolean isYAutoscaled()
    {
        return yAutoscaled;
    }

    /**
     * Set if the X axis should be autoscaled.
     */
    public void setXAutoscaled( boolean xAutoscaled )
    {
        if ( this.xAutoscaled != xAutoscaled ) {
            this.xAutoscaled = xAutoscaled;
            fireChanged();
        }
    }

    /**
     * Set if the Y axis should be autoscaled.
     */
    public void setYAutoscaled( boolean yAutoscaled )
    {
        if ( this.yAutoscaled != yAutoscaled ) {
            this.yAutoscaled = yAutoscaled;
            fireChanged();
        }
    }


    /**
     * See if the X axis should be flipped.
     */
    public boolean isXFlipped()
    {
        return xFlipped;
    }

    /**
     * See if the Y axis should be flipped.
     */
    public boolean isYFlipped()
    {
        return yFlipped;
    }

    /**
     * Set if the X axis should be flipped.
     */
    public void setXFlipped( boolean xFlipped )
    {
        if ( this.xFlipped != xFlipped ) {
            this.xFlipped = xFlipped;
            fireChanged();
        }
    }

    /**
     * Set if the Y axis should be flipped.
     */
    public void setYFlipped( boolean yFlipped )
    {
        if ( this.yFlipped != yFlipped ) {
            this.yFlipped = yFlipped;
            fireChanged();
        }
    }

    /**
     * Set the lower limit of the X axis.
     */
    public void setXLower( double xLower )
    {
        if ( this.xLower != xLower ) {
            this.xLower = xLower;
            fireChanged();
        }
    }

    /**
     * Set the lower limit of the X axis, without an firing an change event. 
     * When used a eventually call that causes an update must be made.
     */
    public void setXLowerValue( double xLower )
    {
        this.xLower = xLower;
    }

    /**
     * Get the lower limit of the X axis.
     */
    public double getXLower()
    {
        return xLower;
    }

    /**
     * Set the upper limit of the X axis.
     */
    public void setXUpper( double xUpper )
    {
        if ( this.xUpper != xUpper ) {
            this.xUpper = xUpper;
            fireChanged();
        }
    }

    /**
     * Set the upper limit of the X axis, without an firing an change event. 
     * When used a eventually call that causes an update must be made.
     */
    public void setXUpperValue( double xUpper )
    {
        this.xUpper = xUpper;
    }

    /**
     * Get the upper limit of X axis.
     */
    public double getXUpper()
    {
        return xUpper;
    }

    /**
     * Set the lower limit of the Y axis.
     */
    public void setYLower( double yLower )
    {
        if ( this.yLower != yLower ) {
            this.yLower = yLower;
            fireChanged();
        }
    }

    /**
     * Set the lower limit of the Y axis, without an firing an change event. 
     * When used a eventually call that causes an update must be made.
     */
    public void setYLowerValue( double yLower )
    {
        this.yLower = yLower;
    }

    /**
     * Get the lower limit of the Y axis.
     */
    public double getYLower()
    {
        return yLower;
    }

    /**
     * Set the upper limit of the Y axis.
     */
    public void setYUpper( double yUpper )
    {
        if ( this.yUpper != yUpper ) {
            this.yUpper = yUpper;
            fireChanged();
        }
    }

    /**
     * Set the upper limit of the Y axis, without an firing an change event. 
     * When used a eventually call that causes an update must be made.
     */
    public void setYUpperValue( double yUpper )
    {
        this.yUpper = yUpper;
    }

    /**
     * Get the upper limit of Y axis.
     */
    public double getYUpper()
    {
        return yUpper;
    }

    /**
     * Find out if the Plot should be made to match the X data range
     * to it's visible surface. If not then the current zoom factors
     * are preserved.
     */
    public boolean isXFit()
    {
        return xFit;
    }

    /**
     * Set if the Plot should be made to match the X data range
     * to it's visible surface. If not then the current zoom factors
     * are preserved.
     */
    public void setXFit( boolean xFit )
    {
        if ( this.xFit != xFit ) {
            this.xFit = xFit;
        }
    }

    /**
     * Find out if the Plot should be made to match the Y data range
     * to it's visible surface. If not then the current zoom factors
     * are preserved.
     */
    public boolean isYFit()
    {
        return yFit;
    }

    /**
     * Set if the Plot should be made to match the Y data range
     * to it's visible surface. If not then the current zoom factors
     * are preserved.
     */
    public void setYFit( boolean yFit )
    {
        if ( this.yFit != yFit ) {
            this.yFit = yFit;
        }
    }

//
// Encode and decode this object to/from XML representation.
//
    /**
     * The name of our enclosing tag.
     */
    public String getTagName()
    {
        return "datalimits";
    }

    public void encode( Element rootElement )
    {
        addChildElement( rootElement, "xAutoscaled", xAutoscaled );
        addChildElement( rootElement, "yAutoscaled", yAutoscaled );
        addChildElement( rootElement, "xFlipped", xFlipped );
        addChildElement( rootElement, "yFlipped", yFlipped );
        addChildElement( rootElement, "xLower", xLower );
        addChildElement( rootElement, "yLower", yLower );
        addChildElement( rootElement, "xUpper", xUpper );
        addChildElement( rootElement, "yUpper", yUpper );
        addChildElement( rootElement, "xFit", xFit );
        addChildElement( rootElement, "yFit", yFit );
    }

    /**
     * Set the value of a member variable by matching its name to a
     * known local property string.
     */
    public void setFromString( String name, String value )
    {
    	if ( name.equals( "xAutoscaled" ) ) {
            setXAutoscaled( booleanFromString( value ) );
            return;
        }
        if ( name.equals( "yAutoscaled" ) ) {
            setYAutoscaled( booleanFromString( value ) );
            return;
        }
        if ( name.equals( "xFlipped" ) ) {
            setXFlipped( booleanFromString( value ) );
            return;
        }
        if ( name.equals( "yFlipped" ) ) {
            setYFlipped( booleanFromString( value ) );
            return;
        }
        if ( name.equals( "xLower" ) ) {
            setXLower( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "yLower" ) ) {
            setYLower( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "xUpper" ) ) {
            setXUpper( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "yUpper" ) ) {
            setYUpper( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "xFit" ) ) {
            setXFit( booleanFromString( value ) );
            return;
        }
        if ( name.equals( "yFit" ) ) {
            setYFit( booleanFromString( value ) );
            return;
        }
        System.err.println( "DataLimits: unknown configuration property:" +
                            name +" (" + value + ")" );

    }
}
