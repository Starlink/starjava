/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     18-JAN-2005 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.ast.gui.AstAxes;
import uk.ac.starlink.ast.gui.PlotController;

/**
 * A simple control (visually a {@link JCheckBox}) for setting and unsetting
 * the X or Y log properties of an {@link AstAxes} object and making the
 * change visible in a related {@link PlotController}. Normally the
 * AstAxes object will be part of a model control the display of a spectrum.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see AstAxes
 */
public class LogAxesControl
     extends JPanel
     implements ChangeListener
{
    /** AstAxes model for current state */
    private AstAxes astAxes = null;

    /** PlotController for applying the current state */
    private PlotController controller = null;

    /** CheckBox for changing and viewing log axis state */
    private JCheckBox logAxisBox = new JCheckBox( ":log" );

    /** The axis */
    private int axis = 1;

    /**
     * Create an instance.
     *
     * @param controller the object that is performing the display (set update
     *                   when the state changes).
     * @param astAxes the object that controls the use of log axes.
     * @param axis the axis to control (AST indices, 1 or 2).
     */
    public LogAxesControl( PlotController controller, AstAxes astAxes, 
                           int axis )
    {
        setPlotController( controller );
        setAxis( axis );
        setAstAxes( astAxes );
        initUI();
    }

    /**
     * Create an instance. Must set the {@link AstAxes} object, before
     * making any use of this object and probably the {@link PlotController}.
     */
    public LogAxesControl()
    {
        initUI();
    }

    /**
     * Set the {@link PlotController} object.
     */
    public void setPlotController( PlotController controller )
    {
        this.controller = controller;
    }

    /**
     * Get the {@link PlotController} object.
     */
    public PlotController getPlotController()
    {
        return controller;
    }

    /**
     * Set the {@link AstAxes} object.
     */
    public void setAstAxes( AstAxes astAxes )
    {
        if ( this.astAxes != null ) {
            this.astAxes.removeChangeListener( this );
        }
        this.astAxes = astAxes;
        if ( this.astAxes != null ) {
            astAxes.addChangeListener( this );
            updateFromAstAxes();
        }
    }

    /**
     * Get reference to current {@link AstAxes}.
     */
    public AstAxes getAstAxes()
    {
        return astAxes;
    }

    /**
     * Set the axis.
     */
    public void setAxis( int axis )
    {
        this.axis = axis;
    }

    /**
     * Get the axis.
     */
    public int getAxis()
    {
        return axis;
    }

    /**
     * Create and initialise the user interface.
     */
    protected void initUI()
    {
        add( logAxisBox );
        logAxisBox.setToolTipText("Set axis to use log spacing if possible");

        //  Add listener for changes.
        logAxisBox.addActionListener( new ActionListener() 
            {
                public void actionPerformed( ActionEvent e )
                {
                    matchControl();
                }
            });
    }

    /**
     * Match appropriate axis to the control state.
     */
    protected void matchControl()
    {
        if ( axis == 1 ) {
            astAxes.setXLog( logAxisBox.isSelected() );
        }
        else {
            astAxes.setYLog( logAxisBox.isSelected() );
        }

        //  Controller performs an update.
        if ( controller != null ) {
            controller.updatePlot();
        }
    }

    /**
     * Update interface to reflect values of the current AstAxes.
     */
    protected void updateFromAstAxes()
    {
        //  Take care with the ChangeListener, we don't want to get into a
        //  loop.
        astAxes.removeChangeListener( this );
        if ( axis == 1 ) {
            logAxisBox.setSelected( astAxes.getXLog() );
        }
        else {
            logAxisBox.setSelected( astAxes.getYLog() );
        }
        astAxes.addChangeListener( this );
    }

//
// Implement the ChangeListener interface
//
    /**
     * If the visible object changes then we need to update the interface.
     */
    public void stateChanged( ChangeEvent e )
    {
        updateFromAstAxes();
    }
}
