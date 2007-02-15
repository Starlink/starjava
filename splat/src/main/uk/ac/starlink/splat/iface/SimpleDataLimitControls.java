/*
 * Copyright (C) 2001-2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     26-JUL-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.splat.data.DataLimits;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.Percentile;
import uk.ac.starlink.splat.plot.DivaPlot;

/**
 * SimpleDataLimitControls is a simple view of a DataLimits object, offering
 * only the ability to select from a pre-defined set of Y coordinates
 * percentile cuts, or to choose automatic scaling.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see DataLimits
 */
public class SimpleDataLimitControls
     extends JPanel
     implements ChangeListener
{
    /**
     * DataLimits model for current state.
     */
    protected DataLimits dataLimits = null;

    /**
     * The PlotControl object.
     */
    protected PlotControl control = null;

    /**
     * The DivaPlot object.
     */
    protected DivaPlot plot = null;

    /**
     * Whether the Y axis should be autoranged when a cut is applied.
     */
    protected boolean autofit = false;

    /**
     * ComboBox for percentile cuts in Y axis.
     */
    protected JComboBox yPercentiles = new JComboBox();

    /**
     * Create an instance.
     *
     * @param control used to query about current limits of displayed
     *      spectrum.
     */
    public SimpleDataLimitControls( PlotControl control, boolean autofit )
    {
        setPlot( control );
        setAutoFit( autofit );
        initUI();
    }

    /**
     * Create an instance. Must set the PlotControl object, before
     * making any use of this object.
     */
    public SimpleDataLimitControls()
    {
        initUI();
    }

    /**
     * Set the PlotControl object.
     *
     * @param control The new plot value
     */
    public void setPlot( PlotControl control )
    {
        this.control = control;
        this.plot = control.getPlot();
        setDataLimits( plot.getDataLimits() );
    }

    /**
     * Get reference to current PlotControl.
     *
     * @return The plot value
     */
    public PlotControl getPlot()
    {
        return control;
    }

    /**
     * Set if a cut will be followed by an autofit to the Y axis.
     */
    public void setAutoFit( boolean autofit )
    {
        this.autofit = autofit;
    }

    /**
     * Get if a cut will be followed by an autofit to the Y axis.
     */
    public boolean isAutoFit()
    {
        return autofit;
    }

    /**
     * Create and initialise the user interface.
     */
    protected void initUI()
    {
        setLayout( new GridLayout( 1, 0 ) );

        //  Add the default list of cuts.
        yPercentiles.setEditable( false );
        yPercentiles.addItem( "automatic" );
        yPercentiles.addItem( new Double( 200.0 ) );
        yPercentiles.addItem( new Double( 150.0 ) );
        yPercentiles.addItem( new Double( 120.0 ) );
        yPercentiles.addItem( new Double( 110.0 ) );
        yPercentiles.addItem( new Double( 105.0 ) );
        yPercentiles.addItem( new Double( 99.9 ) );
        yPercentiles.addItem( new Double( 99.5 ) );
        yPercentiles.addItem( new Double( 99.0 ) );
        yPercentiles.addItem( new Double( 98.0 ) );
        yPercentiles.addItem( new Double( 95.0 ) );
        yPercentiles.addItem( new Double( 90.0 ) );
        yPercentiles.addItem( new Double( 80.0 ) );

        yPercentiles.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    setPercentiles();
                }
            } );

        //  Percentile cuts for Y limits.
        JPanel perc = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
        perc.add( new JLabel( "Y limits (%):" ) );
        perc.add( yPercentiles );
        add( perc );

        //  Set tooltips.
        yPercentiles.setToolTipText( "Percentile auto limits for Y coordinates" );
    }

    /**
     * Set the DataLimits object.
     *
     * @param dataLimits The new dataLimits value
     */
    public void setDataLimits( DataLimits dataLimits )
    {
        this.dataLimits = dataLimits;
        dataLimits.addChangeListener( this );
        updateFromDataLimits();
    }

    /**
     * Get copy of reference to current DataLimits.
     *
     * @return The dataLimits value
     */
    public DataLimits getDataLimits()
    {
        return dataLimits;
    }

    /**
     * Update interface to reflect values of the current DataLimits.
     */
    protected void updateFromDataLimits()
    {
        //  Take care with the ChangeListener, we don't want to get into a
        //  loop.
        dataLimits.removeChangeListener( this );
        if ( dataLimits.isYAutoscaled() ) {
            if ( yPercentiles.getSelectedIndex() != 0 ) {
                yPercentiles.setSelectedIndex( 0 );
            }
        }
        dataLimits.addChangeListener( this );
    }

    /**
     * Get percentile cuts in the Y data values. The first item is special and
     * is equivalent to autoscaling for Y.
     */
    protected void setPercentiles()
    {
        if ( control == null ) {
            return;
        }

        //  Get the selected object. A String is used for the first item only.
        Object target = yPercentiles.getSelectedItem();
        if ( target instanceof String ) {
            if ( ! dataLimits.isYAutoscaled() ) {
                dataLimits.setYAutoscaled( true );
                updatePlot();
            }
            return;
        }

        // Must be a cut, switch off autoscale and apply the value.
        double perc = 100.0;
        try {
            perc = ( (Double) target ).doubleValue();
        }
        catch ( Exception e ) {
            e.printStackTrace();
            return;
        }
        SpecData spec = control.getCurrentSpectrum();
        if ( spec != null ) {
            double data[] = spec.getYData();
            if ( ! dataLimits.isXAutoscaled() ) {
                //  Just use the data within the selected limits.
                double range[] = new double[2];
                range[0] = dataLimits.getXLower();
                range[1] = dataLimits.getXUpper();
                SpecData slice = spec.getSect( "tmp", range );
                data = slice.getYData();
            }
            Percentile percEngine = new Percentile( data );
            double upper = percEngine.get( perc );
            double lower = percEngine.get( 100.0 - perc );

            dataLimits.setYAutoscaled( false );
            dataLimits.setYUpper( upper );
            dataLimits.setYLower( lower );
            updatePlot();
        }
    }

    /**
     * Cause the plot to redraw itself, that is apply our changes. Special
     * case is if we want to do a fit to height at the same time.
     */
    protected void updatePlot()
    {
        boolean oldFitState = dataLimits.isYFit();
        if ( autofit ) {
            dataLimits.setYFit( true );
        }
        try {
            control.updatePlot();
        }
        catch ( Exception e ) {
            // Can occur during initialisation. Best to do nothing.
        }
        if ( autofit ) {
            dataLimits.setYFit( oldFitState );
        }
    }

//
// Implement the ChangeListener interface
//
    /**
     * If the DataLimits object changes then we need to update the interface.
     */
    public void stateChanged( ChangeEvent e )
    {
        updateFromDataLimits();
    }
}
