package uk.ac.starlink.frog.iface;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import uk.ac.starlink.frog.data.DataLimits;
import uk.ac.starlink.frog.data.TimeSeries;
import uk.ac.starlink.frog.plot.PlotControl;
import uk.ac.starlink.frog.util.Percentile;
import uk.ac.starlink.frog.util.FrogDebug;
import uk.ac.starlink.frog.plot.DivaPlot;
import uk.ac.starlink.frog.plot.DivaPlot;


/**
 * SimpleDataLimitMenu is a simple view of a DataLimits object, offering
 * only the ability to select from a pre-defined set of Y coordinates
 * percentile cuts, or to choose automatic scaling.
 *
 * @author Alasdair Allan
 * @version $Id$
 * @see #DataLimits, #PlotConfigFrame.
 */
public class SimpleDataLimitMenu implements ChangeListener
{

   /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();
    
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
    protected JMenu yPercentilesMenu = new JMenu("Plot Scaling");

   /**
     * The parent of the menu that we're populating.
     */
    protected Window parentWindow = null;
    
    /**
     * The menu to populate with scaling menu
     */
    protected JMenu targetMenu = null;
    
    /**
     * Radio Buttons to pupulate the menu
     */    
    protected JRadioButtonMenuItem[] percItems = new JRadioButtonMenuItem[5];

    /**
     * Group for RadioButtonMeniItems
     */
    protected ButtonGroup group = new ButtonGroup();
    
    /**
     * Create an instance.
     *
     * @param dataLimits the DataLimits object that is our data model.
     * @param control used to query about current limits of displayed series.
     * @param targetMenu menu where class will be instanced   
     */
    public SimpleDataLimitMenu( PlotControl control, JMenu targetMenu )
    {
        debugManager.print( "            Calling SimpleDataLimitMenu()...");

        dataLimits = control.getDataLimits();
        setPlot( control );        
        this.targetMenu = targetMenu;
       // this.parentWindow = SwingUtilities.getWindowAncestor( parent );
        initUI();
        setDataLimits( dataLimits );
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
        debugManager.print( "              Calling initUI()...");
        
        percItems[0] = new JRadioButtonMenuItem( "automatic", false );
        percItems[1] = new JRadioButtonMenuItem( "99.0", false );
        percItems[2] = new JRadioButtonMenuItem( "98.0", false );
        percItems[3] = new JRadioButtonMenuItem( "95.0", false );
        percItems[4] = new JRadioButtonMenuItem( "80.0", false );

        for( int i = 0; i < 5; i++ ) {
           group.add(percItems[i]);
           percItems[i].addActionListener(
              new ActionListener()
              {
                  public void actionPerformed( ActionEvent e )
                  {
                      JRadioButtonMenuItem button = 
                               (JRadioButtonMenuItem) e.getSource();
                      setPercentiles( button.getText() );
                  }
              } );
           yPercentilesMenu.add( percItems[ i ] );
        }   
 
        targetMenu.add( yPercentilesMenu );

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
              percItems[0].setSelected(true);
        }
        dataLimits.addChangeListener( this );
    }

    /**
     * Get percentile cuts in the Y data values. The first item is special and
     * is equivalent to autoscaling for Y.
     */
    protected void setPercentiles(String percent)
    {
        debugManager.print( "Calling setPercentiles() in DataLimitMenu...");
        if ( control == null ) {
            return;
        }

        debugManager.print( "  button.getText: " + percent );
        
        // Auto scaling
        if ( percent == "automatic" ) {
            dataLimits.setYAutoscaled( true );
            dataLimits.setXAutoscaled( true );
            updatePlot();
            return;
        } 
        
        // Must be a cut, switch off autoscale and apply the value
        double perc = 100.0;
        if ( percent == "99.0" ) perc = 99.0;
        else if ( percent == "98.0" ) perc = 98.0;
        else if ( percent == "95.0" ) perc = 95.0;
        else if ( percent == "80.0" ) perc = 80.0;
        
        TimeSeries series = control.getCurrentSeries();
        if ( series != null ) {
            Percentile percEngine = new Percentile( series.getYData() );
            double upper = percEngine.get( perc );
            double lower = percEngine.get( 100.0 - perc );
            debugManager.print( "  Upper = " + upper + "  Lower = " + lower );

            //  Special feature of this option is that we fit to height always.
            dataLimits.setYAutoscaled( false );
            dataLimits.setXAutoscaled( true );
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
