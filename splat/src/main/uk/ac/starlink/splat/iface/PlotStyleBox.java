/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     07-FEB-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import javax.swing.JComboBox;

/**
 * PlotStyleBox extends a JComboBox by adding a default set of values
 * that correspond to the available line plotting styles.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PlotStyleBox 
    extends JComboBox 
{
    /**
     * The known plot styles. Note these correspond to
     * SpecData.POLYLINE, SpecData.HISTOGRAM and SpecData.POINT.
     */
    protected static String[] styles = { "polyline", "histogram", "point" };

    /**
     * The default constructor.
     */
    public PlotStyleBox() 
    {
        super();
        for ( int i = 0; i < styles.length; i++ ) {
            addItem( styles[i] );
        }
        setToolTipText( "Select a line plotting style" );
    }

    /**
     * Get the selected style.
     */
    public int getSelectedStyle() 
    {
        return getSelectedIndex() + 1;
    }

    /**
     * Set the selected style.
     */
    public void setSelectedStyle( int style ) 
    {
        setSelectedIndex( style - 1 );
    }
}

