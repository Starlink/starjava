// Copyright (C) 2002 Central Laboratory of the Research Councils

// History:
//    26-OCT-2000 (Peter W. Draper):
//       Original version.

package uk.ac.starlink.splat.iface;

import javax.swing.JTextField;
import uk.ac.starlink.splat.plot.DivaPlot;

/**
 * AstDoubleField extends JTextField to enforce the entry of valid AST
 * coordinates (that is doubles or formatted strings).
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see #AstDouble, #AstDoubleDocument, #ASTJ.
 */
public class AstDoubleField
    extends JTextField
{
    /**
     * Reference to the plot that describes the coordinates that can be used.
     */
    protected DivaPlot plot = null;

    /**
     * The axis of the Plot that the coordinates relate too.
     */
    protected int axis = 0;

    /**
     * Create an instance. Requires the initial value (as a double), the Plot
     * and related axis.
     */
    public AstDoubleField( double value, DivaPlot plot, int axis )
    {
        super();
        setDocument( new AstDoubleDocument( plot, axis ) );
        this.plot = plot;
        this.axis = axis;
        setDoubleValue( value );
    }

    /**
     * Get the current value as double precision. If this fails then
     * AstDouble.BAD is returned.
     */
    public double getDoubleValue()
    {
        return plot.unFormat( axis, getText() );
    }

    /**
     * Set the current value.
     */
    public void setDoubleValue( double value )
    {
        setText( plot.format( axis, value ) );
    }
}
