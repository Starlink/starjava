/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 * History:
 *    16-NOV-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import uk.ac.starlink.ast.gui.AstDouble;
import uk.ac.starlink.ast.gui.AstDoubleField;
import uk.ac.starlink.ast.gui.PlotControls;
import uk.ac.starlink.ast.gui.PlotConfigurator;  // For javadocs
import uk.ac.starlink.ast.gui.AbstractPlotControlsModel;
import uk.ac.starlink.splat.data.DataLimits;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.Percentile;
import uk.ac.starlink.splat.plot.DivaPlot;
import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * DataLimitControls creates a "page" of widgets that are a view of a
 * DataLimits object. They provide the ability to configure all the properties
 * of the DataLimits object (that describe how the axis data display limits an
 * AST plot should be drawn). Special options to set the limits, total and
 * view, to those shown by a PlotControl object are provided.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see DataLimits
 * @see PlotConfigurator
 */
public class DataLimitControls
     extends JPanel
     implements PlotControls, ChangeListener, DocumentListener
{
    /**
     * DataLimits model for current state.
     */
    protected DataLimits dataLimits = null;

    /**
     * Check box for whether X axis should be autoscaled.
     */
    protected JCheckBox xAutoscaled = new JCheckBox();

    /**
     * Check box for whether Y axis should be autoscaled.
     */
    protected JCheckBox yAutoscaled = new JCheckBox();

    /**
     * Check box for whether X axis should be made to fit view.
     */
    protected JCheckBox xFit = new JCheckBox();

    /**
     * Check box for whether Y axis should be made to fit view.
     */
    protected JCheckBox yFit = new JCheckBox();

    /**
     * Check box for whether X axis range should be shown flipped.
     */
    protected JCheckBox xFlipped = new JCheckBox();

    /**
     * Check box for whether Y axis range should be shown flipped.
     */
    protected JCheckBox yFlipped = new JCheckBox();

    /**
     * Entry widget for lower limit of the X axis.
     */
    protected AstDoubleField xLower = null;

    /**
     * Entry widget for lower limit of the Y axis.
     */
    protected AstDoubleField yLower = null;

    /**
     * Entry widget for upper limit of the X axis.
     */
    protected AstDoubleField xUpper = null;

    /**
     * Entry widget for upper limit of the Y axis.
     */
    protected AstDoubleField yUpper = null;

    /**
     * The PlotControl object.
     */
    protected PlotControl control = null;

    /**
     * The DivaPlot object.
     */
    protected DivaPlot plot = null;

    /**
     * Control for request to set limits from plot.
     */
    protected JButton setFromCurrent = new JButton();

    /**
     * Control for request to set limits from plot.
     */
    protected JButton setFromView = new JButton();

    /**
     * JSlider for percentile cuts in Y axis.
     */
    protected FloatJSlider yPercentiles = null;

    /**
     * True when an AstDoubleField is already matched to its text.
     */
    private boolean astDoubleFieldMatched = false;

    /**
     * The default title for these controls.
     */
    protected static String defaultTitle = "Axis Data Limits:";

    /**
     * The default short name for these controls.
     */
    protected static String defaultName = "Limits";

    /**
     * Create an instance.
     *
     * @param dataLimits the DataLimits object that is our data model.
     * @param control used to query about current limits of displayed
     *      spectrum.
     */
    public DataLimitControls( DataLimits dataLimits, PlotControl control )
    {
        setPlot( control );
        initUI();
        setDataLimits( dataLimits );
    }

    /**
     * Set the PlotControl object.
     */
    public void setPlot( PlotControl control )
    {
        this.control = control;
        this.plot = control.getPlot();
    }

    /**
     * Get reference to current PlotControl.
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
        //  Whether axes are autoscaled.
        xAutoscaled.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    matchXAutoscaled();
                }
            } );
        yAutoscaled.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    matchYAutoscaled();
                }
            } );

        //  Whether axes should be fit to width or height.
        xFit.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    matchXFit();
                }
            } );
        yFit.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    matchYFit();
                }
            } );

        //  Whether ranges should be shown flipped.
        xFlipped.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    matchXFlipped();
                }
            } );
        yFlipped.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    matchYFlipped();
                }
            } );

        //  Limits as AstDoubles...
        xLower = new AstDoubleField( 0.0, control, 1 );
        Document doc = xLower.getDocument();
        doc.addDocumentListener( this );
        doc.putProperty( "AstField", xLower );

        yLower = new AstDoubleField( 0.0, control, 2 );
        doc = yLower.getDocument();
        doc.addDocumentListener( this );
        doc.putProperty( "AstField", yLower );

        xUpper = new AstDoubleField( 0.0, control, 1 );
        doc = xUpper.getDocument();
        doc.addDocumentListener( this );
        doc.putProperty( "AstField", xUpper );

        yUpper = new AstDoubleField( 0.0, control, 2 );
        doc = yUpper.getDocument();
        doc.addDocumentListener( this );
        doc.putProperty( "AstField", yUpper );

        // Percentile cuts for Y axis values.
        yPercentiles =
            new FloatJSlider( new FloatJSliderModel( 100.0, 50.0,
                                                     110.0, 0.1 ) );
        yPercentiles.addChangeListener(
            new ChangeListener()
            {
                public void stateChanged( ChangeEvent e )
                {
                    setPercentiles();
                }
            } );

        //  Buttons for setting values from plot.
        setFromView.setText( "Update from view" );
        setFromCurrent.setText( "Update to current" );
        setFromView.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    setLimits( false );
                }
            } );
        setFromCurrent.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    setLimits( true );
                }
            } );

        //  Layout.
        GridBagLayouter layouter = 
            new GridBagLayouter( this, GridBagLayouter.SCHEME3 );
        
        // Match insets to ASTGUI.
        layouter.setInsets( new Insets( 5, 5, 5, 5 ) );

        layouter.add( new JLabel( "Autoscale X:" ), false );
        layouter.add( xAutoscaled, true );

        layouter.add( new JLabel( "Fit X range:" ), false );
        layouter.add( xFit, true );

        layouter.add( new JLabel( "Flip X axis:" ), false );
        layouter.add( xFlipped, true );

        layouter.add( new JLabel( "Lower X:" ), false );
        layouter.add( xLower, true );

        layouter.add( new JLabel( "Upper X:" ), false );
        layouter.add( xUpper, true );

        layouter.add( new JLabel( "Autoscale Y:" ), false );
        layouter.add( yAutoscaled, true );

        layouter.add( new JLabel( "Fit Y range:" ), false );
        layouter.add( yFit, true );

        layouter.add( new JLabel( "Flip Y axis:" ), false );
        layouter.add( yFlipped, true );

        layouter.add( new JLabel( "Lower Y:" ), false );
        layouter.add( yLower, true );

        layouter.add( new JLabel( "Upper Y:" ), false );
        layouter.add( yUpper, true );

        layouter.add( new JLabel( "Y auto cut:" ), false );
        layouter.add( yPercentiles, true );

        //  Limits buttons.
        layouter.add( Box.createHorizontalBox(), false );
        layouter.add( setFromCurrent, false );
        layouter.add( Box.createHorizontalBox(), true );

        layouter.add( Box.createHorizontalBox(), false );
        layouter.add( setFromView, false );
        layouter.add( Box.createHorizontalBox(), true );
        layouter.eatSpare();

        //  Set tooltips.
        xAutoscaled.setToolTipText( "Autoscale X axis to fit all data" );
        xFit.setToolTipText( "Fit X axis limts to viewable surface" );
        xFlipped.setToolTipText( "Flip X axis to run right to left" );
        xLower.setToolTipText( "Lower limit of X axis (axis units)" );
        xUpper.setToolTipText( "Upper limit of X axis (axis units)" );
        yAutoscaled.setToolTipText( "Autoscale Y axis to fit all data" );
        yFit.setToolTipText( "Fit Y axis limits to viewable surface" );
        yFlipped.setToolTipText( "Flip Y axis to run top to bottom" );
        yLower.setToolTipText( "Lower limit of Y axis (axis units)" );
        yUpper.setToolTipText( "Upper limit of Y axis (axis units)" );
        yPercentiles.setToolTipText( "Percentile auto limits for data " +
            "counts (press return to apply edits)" );
        setFromCurrent.setToolTipText
            ( "Set limits to those of whole plot (includes zoomed regions)" );
        setFromView.setToolTipText
            ( "Set limits to those of plot view (including surrounding space)" );
    }

    /**
     * Set the DataLimits object.
     */
    public void setDataLimits( DataLimits dataLimits )
    {
        this.dataLimits = dataLimits;
        dataLimits.addChangeListener( this );
        updateFromDataLimits();

        //  Set some initial limits, so we can autoconfigure safely.
        setLimits( true );
    }

    /**
     * Get copy of reference to current DataLimits.
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
        xAutoscaled.setSelected( dataLimits.isXAutoscaled() );
        yAutoscaled.setSelected( dataLimits.isYAutoscaled() );

        xFit.setSelected( dataLimits.isXFit() );
        yFit.setSelected( dataLimits.isYFit() );

        xFlipped.setSelected( dataLimits.isXFlipped() );
        yFlipped.setSelected( dataLimits.isYFlipped() );

        if ( ! astDoubleFieldMatched ) {
            try {
                xLower.setText( plot.format( 1, dataLimits.getXLower() ) );
                xUpper.setText( plot.format( 1, dataLimits.getXUpper() ) );
                yLower.setText( plot.format( 2, dataLimits.getYLower() ) );
                yUpper.setText( plot.format( 2, dataLimits.getYUpper() ) );
            }
            catch (Exception e) {
                System.out.println( e.getMessage() );
            }
        }

        if ( dataLimits.isXAutoscaled() ) {
            xLower.setEnabled( false );
            xUpper.setEnabled( false );
            xFit.setEnabled( false );
        }
        if ( dataLimits.isYAutoscaled() ) {
            yLower.setEnabled( false );
            yUpper.setEnabled( false );
            yPercentiles.setEnabled( false );
            yFit.setEnabled( false );
        }
        dataLimits.addChangeListener( this );
    }

    /**
     * Match whether to autoscale the X axis.
     */
    protected void matchXAutoscaled()
    {
        dataLimits.setXAutoscaled( xAutoscaled.isSelected() );
        if ( dataLimits.isXAutoscaled() ) {
            xLower.setEnabled( false );
            xUpper.setEnabled( false );
            xFit.setEnabled( false );
        }
        else {
            xLower.setEnabled( true );
            xUpper.setEnabled( true );
            xFit.setEnabled( true );
        }
        repaint();
    }

    /**
     * Match whether to autoscale the Y axis.
     */
    protected void matchYAutoscaled()
    {
        dataLimits.setYAutoscaled( yAutoscaled.isSelected() );
        if ( dataLimits.isYAutoscaled() ) {
            yLower.setEnabled( false );
            yUpper.setEnabled( false );
            yPercentiles.setEnabled( false );
            yFit.setEnabled( false );
        }
        else {
            yLower.setEnabled( true );
            yUpper.setEnabled( true );
            yPercentiles.setEnabled( true );
            yFit.setEnabled( true );
        }
        repaint();
    }

    /**
     * Match X axis view fitting.
     */
    protected void matchXFit()
    {
        dataLimits.setXFit( xFit.isSelected() );
    }

    /**
     * Match Y axis view fitting.
     */
    protected void matchYFit()
    {
        dataLimits.setYFit( yFit.isSelected() );
    }

    /**
     * Match X axis flipped state.
     */
    protected void matchXFlipped()
    {
        dataLimits.setXFlipped( xFlipped.isSelected() );
    }

    /**
     * Match Y axis flipped state.
     */
    protected void matchYFlipped()
    {
        dataLimits.setYFlipped( yFlipped.isSelected() );
    }

    /**
     * Match X axis lower limit.
     */
    protected void matchXLower()
    {
        double value = plot.unFormat( 1, xLower.getText() );
        if ( ! AstDouble.isBad( value ) ) {
            dataLimits.setXLower( value );
        }
    }

    /**
     * Match X axis upper limit.
     */
    protected void matchXUpper()
    {
        dataLimits.setXUpper( plot.unFormat( 1, xUpper.getText() ) );
    }

    /**
     * Match Y axis lower limit.
     */
    protected void matchYLower()
    {
        dataLimits.setYLower( plot.unFormat( 2, yLower.getText() ) );
    }

    /**
     * Match Y axis upper limit.
     */
    protected void matchYUpper()
    {
        dataLimits.setYUpper( plot.unFormat( 2, yUpper.getText() ) );
    }

    /**
     * Set the data limits according to the state of a PlotControl object that
     * is displaying a spectrum.
     *
     * @param whole whether to set from the whole display, or just what is
     *      currently visible (assumes PlotControl has a JViewport).
     */
    protected void setLimits( boolean whole )
    {
        if ( control == null ) {
            return;
        }
        double[] limits;
        if ( whole ) {
            limits = control.getDisplayCoordinates();
        }
        else {
            limits = control.getViewCoordinates();
        }
        dataLimits.setXLower( Math.min( limits[0], limits[2] ) );
        dataLimits.setYLower( Math.min( limits[1], limits[3] ) );
        dataLimits.setXUpper( Math.max( limits[0], limits[2] ) );
        dataLimits.setYUpper( Math.max( limits[1], limits[3] ) );
    }

    /**
     * Get percentile cuts in the Y data values.
     */
    protected void setPercentiles()
    {
        if ( control == null ) {
            return;
        }

        //  Get the cut
        double perc = yPercentiles.getValue();

        //  And set this as a limit.
        SpecData spec = control.getCurrentSpectrum();
        if ( spec != null ) {
            Percentile percEngine = new Percentile( spec.getYData() );
            double upper = percEngine.get( perc );
            double lower = percEngine.get( 100.0 - perc );
            dataLimits.setYUpper( upper );
            dataLimits.setYLower( lower );
        }
    }

// Implement the PlotControls interface
//
    /**
     * Return a title for these controls (for the border).
     */
    public String getControlsTitle()
    {
        return defaultTitle;
    }

    /**
     * Return a short name for these controls (for the tab).
     */
    public String getControlsName()
    {
        return defaultName;
    }

    /**
     * Reset controls to the defaults.
     */
    public void reset()
    {
        dataLimits.setDefaults();
    }

    /**
     * Return a reference to the JComponent sub-class that will be
     * displayed (normally a reference to this).
     */
    public JComponent getControlsComponent()
    {
        return this;
    }

    /**
     * Return reference to the AbstractPlotControlsModel. This defines
     * the actual state of the controls and stores the current values.
     */
    public AbstractPlotControlsModel getControlsModel()
    {
        return dataLimits;
    }

    /**
     * Return the class of object that we expect as our model.
     */
    public static Class getControlsModelClass()
    {
        return DataLimits.class;
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

//
// Implement the DocumentListener interface.
//
    public void changedUpdate( DocumentEvent e )
    {
        matchAstDoubleField( e.getDocument().getProperty( "AstField" ) );
    }
    public void insertUpdate( DocumentEvent e )
    {
        matchAstDoubleField( e.getDocument().getProperty( "AstField" ) );
    }
    public void removeUpdate( DocumentEvent e )
    {
        matchAstDoubleField( e.getDocument().getProperty( "AstField" ) );
    }

    public void matchAstDoubleField( Object property )
    {
        AstDoubleField target = (AstDoubleField) property;
        astDoubleFieldMatched = true; // Inhibit recusive mutation.
        if ( target == xLower ) {
            matchXLower();
        }
        else if ( target == yLower ) {
            matchYLower();
        }
        else if ( target == xUpper ) {
            matchXUpper();
        }
        else if ( target == yUpper ) {
            matchYUpper();
        }
        astDoubleFieldMatched = false;
    }
}
