/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     01-DEC-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import javax.swing.JComboBox;
import uk.ac.starlink.ast.grf.DefaultGrfMarker;

/**
 * PointTypeBox extends a JComboBox by adding a default set of values
 * that correspond to the available marker types for rendering spectra.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PointTypeBox 
    extends JComboBox 
{
    /**
     * The default constructor.
     */
    public PointTypeBox() 
    {
        super();
        int n = DefaultGrfMarker.getNumMarkers();
        for ( int i = 0; i < n; i++ ) {
            addItem( DefaultGrfMarker.getDescription( i ) );
        }
        setToolTipText( "Type of point to use when drawing spectrum" );
    }

    /**
     * Get the selected type
     */
    public int getSelectedType() 
    {
        return getSelectedIndex();
    }

    /**
     * Set the selected type.
     */
    public void setSelectedType( int type ) 
    {
        setSelectedIndex( type );
    }
}
