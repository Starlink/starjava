// Copyright (C) 2002 Central Laboratory of the Research Councils

// History:
//    26-JUL-2001 (Peter W. Draper):
//       Original version.

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
 * @see #DataLimits, #PlotConfigFrame.
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
     * ComboBox for percentile cuts in Y axis.
     */
    protected JComboBox yPercentiles = new JComboBox();

    /**
     * Create an instance.
     *
     * @param control used to query about current limits of displayed
     *      spectrum.
     */
    public SimpleDataLimitControls( PlotControl control ) 
    {
        setPlot( control );
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
     * Create and initialise the user interface.
     */
    protected void initUI()
    {
        setLayout( new GridLayout( 1, 0 ) );

        //  Add the default list of cuts.
        yPercentiles.setEditable( false );
        yPercentiles.addItem( "automatic" );
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
        //  Take care with the ChangeListener, we don't want to get
        //  into a loop.
        dataLimits.removeChangeListener( this );
        if ( dataLimits.isYAutoscaled() ) {
            yPercentiles.setSelectedIndex( 0 );
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

        //  Get the selected object. A String is used for the first
        //  item only.
        Object target = yPercentiles.getSelectedItem();
        if ( target instanceof String ) {
            dataLimits.setYAutoscaled( true );
            updatePlot();
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
            Percentile percEngine = new Percentile( spec.getYData() );
            double upper = percEngine.get( perc );
            double lower = percEngine.get( 100.0 - perc );

            //  Special feature of this option is that we fit to
            //  height always.
            dataLimits.setYAutoscaled( false );
            dataLimits.setYUpper( upper );
            dataLimits.setYLower( lower );
            updatePlot();
        }
    }

    /**
     * Cause the plot to redraw itself, i.e. apply our changes. Special case
     * we want to do a fit to height at the same time.
     */
    protected void updatePlot()
    {
        boolean oldFitState = dataLimits.isYFit();
        dataLimits.setYFit( true );
        try {
            control.updatePlot();
        }
        catch ( Exception e ) {
            // Can occur during initialisation. Do nothing.
        }
        dataLimits.setYFit( oldFitState );
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
