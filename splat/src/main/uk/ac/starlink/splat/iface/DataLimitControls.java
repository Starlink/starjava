// Copyright (C) 2002 Central Laboratory of the Research Councils

// History:
//    16-NOV-2000 (Peter W. Draper):
//       Original version.

package uk.ac.starlink.splat.iface;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import uk.ac.starlink.splat.ast.AstDouble;
import uk.ac.starlink.splat.data.DataLimits;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.Percentile;
import uk.ac.starlink.splat.plot.DivaPlot;

/**
 * DataLimitControls creates a "page" of widgets that are a view of a
 * DataLimits object. They provide the ability to configure all the properties
 * of the DataLimits object (that describe how the axis data display limits an
 * AST plot should be drawn). Special options to set the limits, total and
 * view, to those shown by a PlotControl object are provided.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see #DataLimits, #PlotConfigFrame.
 */
public class DataLimitControls
     extends JPanel
     implements ChangeListener, DocumentListener
{
    /**
     * DataLimits model for current state.
     */
    protected DataLimits dataLimits = null;

    /**
     * GridBagConstraints object.
     */
    protected GridBagConstraints gbc = new GridBagConstraints();

    /**
     * Label Insets.
     */
    protected Insets labelInsets = new Insets( 10, 5, 5, 10 );

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
     * Reset controls to defaults.
     */
    public void reset()
    {
        dataLimits.setDefaults();
    }

    /**
     * Create and initialise the user interface.
     */
    protected void initUI()
    {
        setLayout( new GridBagLayout() );

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

        //  Limits as AstDoubles...
        xLower = new AstDoubleField( 0.0, plot, 1 );
        Document doc = xLower.getDocument();
        doc.addDocumentListener( this );
        doc.putProperty( "AstField", xLower );

        yLower = new AstDoubleField( 0.0, plot, 2 );
        doc = yLower.getDocument();
        doc.addDocumentListener( this );
        doc.putProperty( "AstField", yLower );

        xUpper = new AstDoubleField( 0.0, plot, 1 );
        doc = xUpper.getDocument();
        doc.addDocumentListener( this );
        doc.putProperty( "AstField", xUpper );

        yUpper = new AstDoubleField( 0.0, plot, 2 );
        doc = yUpper.getDocument();
        doc.addDocumentListener( this );
        doc.putProperty( "AstField", yUpper );

        // Percentile cuts for Y axis values.
        yPercentiles =
            new FloatJSlider( new FloatJSliderModel( 100.0, 50.0,
                                                     100.0, 0.1 ) );
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

        //  Add labels for all fields.
        addLabel( "Autoscale X:", 0 );
        addLabel( "Fit X range:", 1 );
        addLabel( "Lower X:", 2 );
        addLabel( "Upper X:", 3 );
        addLabel( "Autoscale Y:", 4 );
        addLabel( "Fit Y range:", 5 );
        addLabel( "Lower Y:", 6 );
        addLabel( "Upper Y:", 7 );
        addLabel( "Y auto cut:", 8 );

        gbc.insets = new Insets( 0, 0, 0, 0 );
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weighty = 0.0;
        gbc.weightx = 1.0;
        gbc.gridx = 1;

        //  Current row for adding components.
        int row = 0;

        //  Autoscale X.
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy = row++;
        add( xAutoscaled, gbc );

        //  Fit X.
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy = row++;
        add( xFit, gbc );

        //  X limits.
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = row++;
        add( xLower, gbc );
        gbc.gridy = row++;
        add( xUpper, gbc );

        //  Autoscale Y.
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy = row++;
        add( yAutoscaled, gbc );

        //  Fit Y.
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy = row++;
        add( yFit, gbc );

        //  Y limits.
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = row++;
        add( yLower, gbc );
        gbc.gridy = row++;
        add( yUpper, gbc );

        //  Percentile cuts for Y limits.
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = row++;
        add( yPercentiles, gbc );

        //  Eat up some vertical space.
        Component filly1 = Box.createVerticalStrut( 5 );
        gbc.gridy = row++;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        add( filly1, gbc );

        //  Limits buttons.
        gbc.gridy = row++;
        gbc.weighty = 0.0;
        add( setFromCurrent, gbc );
        gbc.gridy = row++;
        add( setFromView, gbc );

        //  Eat up all spare vertical space (pushes widgets to top).
        Component filly2 = Box.createVerticalStrut( 5 );
        gbc.gridy = row++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        add( filly2, gbc );

        //  Set tooltips.
        xAutoscaled.setToolTipText( "Autoscale X axis to fit all data" );
        xFit.setToolTipText( "Fit X axis limts to viewable surface" );
        xLower.setToolTipText( "Lower limit of X axis (axis units)" );
        xUpper.setToolTipText( "Upper limit of X axis (axis units)" );
        yAutoscaled.setToolTipText( "Autoscale Y axis to fit all data" );
        yFit.setToolTipText( "Fit Y axis limits to viewable surface" );
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

        if ( ! astDoubleFieldMatched ) {
            xLower.setText( plot.format( 1, dataLimits.getXLower() ) );
            xUpper.setText( plot.format( 1, dataLimits.getXUpper() ) );
            yLower.setText( plot.format( 2, dataLimits.getYLower() ) );
            yUpper.setText( plot.format( 2, dataLimits.getYUpper() ) );
        }

        if ( dataLimits.isXAutoscaled() ) {
            xLower.disable();
            xUpper.disable();
            xFit.disable();
        }
        if ( dataLimits.isYAutoscaled() ) {
            yLower.disable();
            yUpper.disable();
            yPercentiles.disable();
            yFit.disable();
        }
        dataLimits.addChangeListener( this );
    }

    /**
     * Add a new label. This is added to the front of the given row.
     *
     * @param text The feature to be added to the Label attribute
     * @param row The feature to be added to the Label attribute
     */
    private void addLabel( String text, int row )
    {
        JLabel label = new JLabel( text );
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.insets = labelInsets;
        add( label, gbc );
    }

    /**
     * Match whether to autoscale the X axis.
     */
    protected void matchXAutoscaled()
    {
        dataLimits.setXAutoscaled( xAutoscaled.isSelected() );
        if ( dataLimits.isXAutoscaled() ) {
            xLower.disable();
            xUpper.disable();
            xFit.disable();
        }
        else {
            xLower.enable();
            xUpper.enable();
            xFit.enable();
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
            yLower.disable();
            yUpper.disable();
            yPercentiles.disable();
            yFit.disable();
        }
        else {
            yLower.enable();
            yUpper.enable();
            yPercentiles.enable();
            yFit.enable();
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
