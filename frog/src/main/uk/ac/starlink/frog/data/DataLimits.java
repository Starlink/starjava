package uk.ac.starlink.frog.data;

import org.w3c.dom.Element;
import uk.ac.starlink.ast.gui.AbstractPlotControlsModel;

/**
 * DataLimits defines the limits of a Plot. These can be explicit
 * values two for each axis, or indications to autoscale both or
 * either of these axes.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see SpecDataComp
 */
public class DataLimits extends AbstractPlotControlsModel
{
    /**
     * Whether to fit Plot to match the data X data range into it's
     * visible area.
     */
    protected boolean xFit;

    /**
     * Whether to fit Plot to match the data Y data range into it's
     * visible area.
     */
    protected boolean yFit;

    /**
     * Whether to autoscale the X axis.
     */
    protected boolean xAutoscaled;

    /**
     * Whether to autoscale the Y axis.
     */
    protected boolean yAutoscaled;

    /**
     * Lower limit of the X axis.
     */
    protected double xLower;

    /**
     * Upper limit of the X axis.
     */
    protected double xUpper;

    /**
     * Lower limit of the Y axis.
     */
    protected double yLower;

    /**
     * Upper limit of the Y axis.
     */
    protected double yUpper;

   /**
    * Whether the displayed range should be flipped along any X axis.
    */
    protected boolean xFlipped;

    /**
     * Whether the displayed range should be flipped along any Y axis.  
     */
    protected boolean yFlipped;

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
         this.xFlipped = xFlipped;
         fireChanged();
     }
 
     /**
      * Set if the Y axis should be flipped.
      */
     public void setYFlipped( boolean yFlipped )
     {
         this.yFlipped = yFlipped;
         fireChanged();
     }
 
 
    /**
     * A convenince method to call isYFlipped()
     */
     public boolean isPlotInMags()
     {
       return isYFlipped();
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
        this.xAutoscaled = xAutoscaled;
        fireChanged();
    }

    /**
     * Set if the Y axis should be autoscaled.
     */
    public void setYAutoscaled( boolean yAutoscaled )
    {
        this.yAutoscaled = yAutoscaled;
        fireChanged();
    }

    /**
     * Set the lower limit of the X axis.
     */
    public void setXLower( double xLower )
    {
        this.xLower = xLower;
        fireChanged();
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
        this.xUpper = xUpper;
        fireChanged();
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
        this.yLower = yLower;
        fireChanged();
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
        this.yUpper = yUpper;
        fireChanged();
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
        this.xFit = xFit;
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
        this.yFit = yFit;
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
