package uk.ac.starlink.splat.plot;

/**
 *  Java class to store the current state a Bar. 
 *  
 *  The state is defined as the position, current colour, line style and
 *  width.  
 */

public class BarState 
{
    //  Internal state.
    protected int position = 0;
    protected int style = 0;
    protected int width = 1;
    protected int colour = 0;
    
    //  Constructors
    public BarState() 
    {
        // Do nothing.
    }

    public BarState( int position, int style, int width, int colour ) 
    {
        this.style = style;
        this.width = width;
        this.colour = colour;
    }
    
    //  Getter methods
    public int getPosition() 
    {
        return position;
    }
    public int getWidth() 
    {
        return width;
    }
    public int getColour() 
    {
        return colour;
    }
    public int getStyle() 
    {
        return style;
    }
    
    //  Setter methods
    public void setPosition( int value ) 
    {
        position = value;
    }
    public void setWidth( int value ) 
    {
        width = value;
    }
    public void setColour( int value ) 
    {
        colour = value;
    }
    public void setStyle( int value ) 
    {
        style = value;
    }
}
