package uk.ac.starlink.splat.iface;

import javax.swing.JComboBox;

/**
 * PlotStyleBox extends a JComboBox by adding a default set of values
 * that correspond to the available line plotting styles.
 *
 * @since $Date$
 * @since 07-FEB-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 */
public class PlotStyleBox extends JComboBox 
{
    /**
     * The known plot styles. Note these correspond to
     * SpecData.POLYLINE and SpecData.HISTOGRAM.
     */
    protected static String[] styles = { "polyline", "histogram" };

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

