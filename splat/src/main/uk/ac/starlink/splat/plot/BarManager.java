/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     13-JUN-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.plot;

import java.awt.Graphics;
import java.util.Vector;

/**
 *  Manager of interactive vertical bars shown in on a JComponent.
 *
 *  @author Peter W. Draper
 *  @version $Id$
 */
public class BarManager 
{
    /**
     *  Vector of BarState objects. These contain the properties
     *  of the bars (coordinates, colour etc).
     */
    protected Vector props = new Vector();

    /**
     *  Lower graphics position of all bars.
     */
    protected int yMin = 0;

    /**
     *  Highest graphics position of all bars.
     */
    protected int yMax = 100;

    /**
     *  Default constructor.
     */
    public BarManager() {
    }

    /**
     *  Constructor.
     *
     *  @param yMin lowest Y position of bars.
     *  @param yMax highest Y position of bars.
     */
    public BarManager( int yMin, int yMax ) {
        this.yMin = yMin;
        this.yMax = yMax;
    }

    /**
     *  Add a new bar. Returns index of bar.
     */
    public int addBar( int x, int style, int width, int colour ) {
        BarState state = new BarState( x, style, width, colour );
        props.add( state );
        return props.size() - 1;
    }
    
    /**
     *  Reset the position of a bar.
     */
    public void setPosition( int index, int x ) {
        BarState state = (BarState) props.get( index );
        state.setPosition( x );
    }
    
    /**
     *  Reset the width of a bar.
     */
    public void setWidth( int index, int width ) {
        BarState state = (BarState) props.get( index );
        state.setWidth( width );
    }
    
    /**
     *  Reset the style of a bar.
     */
    public void setSytle( int index, int style ) {
        BarState state = (BarState) props.get( index );
        state.setWidth( style );
    }
    
    /**
     *  Reset the colour of a bar.
     */
    public void setColour( int index, int colour ) {
        BarState state = (BarState) props.get( index );
        state.setColour( colour );
    }

    /**
     *  Remove a bar.
     */
    public void removeBar( int index ) {
        props.set( index, null );
    }

    /**
     *  Remove all bars.
     */
    public void removeBars() {
        props.clear();
    }
    
    /**
     *  Redraw a bar
     */
    public void paint( Graphics g, int index ) {
        redraw( g, index );
    }

    /**
     *  Redraw all bars
     */
    public void paint( Graphics g ) {
        for ( int i = 0; i < props.size(); i++ ) {
            redraw( g, i );
        }
    }

    /**
     *  Draw a specified bar.
     */
    public void redraw( Graphics g, int index ) {
    }

    
    /**
     *  List of predefined colours.
     */
    

}
