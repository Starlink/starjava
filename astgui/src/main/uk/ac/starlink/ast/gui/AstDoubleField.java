/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 * 
 *  History:
 *     26-OCT-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.gui;

import javax.swing.JTextField;
import uk.ac.starlink.ast.Plot;

/**
 * AstDoubleField extends JTextField to enforce the entry of valid AST
 * coordinates (that is doubles or formatted strings).
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see AstDouble
 * @see AstDoubleDocument
 */
public class AstDoubleField
    extends JTextField
{
    /**
     * Reference to the {@link PlotController} that supplies a reference to
     * the current {@link Frame} of an associated {@link Plot}.
     */
    protected PlotController controller = null;

    /**
     * The axis of the Plot that the coordinates relate to.
     */
    protected int axis = 0;

    /**
     * Create an instance. Requires the initial value (as a double),
     * the PlotController and related axis.
     */
    public AstDoubleField( double value, PlotController controller, int axis )
    {
        super();
        setDocument( new AstDoubleDocument( controller, axis ) );
        this.controller = controller;
        this.axis = axis;
        setDoubleValue( value );
    }

    /**
     * Get the current value as double precision. If this fails then
     * AstDouble.BAD is returned.
     */
    public double getDoubleValue()
    {
        String text = getText();
        if ( text.equals( "" ) || text.equals( "<bad>" ) ) {
            return AstDouble.BAD;
        }
        return controller.getPlotCurrentFrame().unformat( axis, text );
    }

    /**
     * Set the current value. If AST fails then a simple double value
     * is shown.
     */
    public void setDoubleValue( double value )
    {
        if ( value == AstDouble.BAD ) {
            setText( "" );
        }
        else {
            setText( controller.getPlotCurrentFrame().format( axis, value ) );
        }
    }
}
