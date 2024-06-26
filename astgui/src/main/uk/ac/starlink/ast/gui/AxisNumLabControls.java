/*
 * Copyright (C) 2001-2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     10-NOV-2001 (Peter W. Draper):
 *        Original version.
 *     19-FEB-2004 (Peter W. Draper):
 *        Added log label.
 */
package uk.ac.starlink.ast.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import javax.swing.SpinnerNumberModel;

import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * AxisNumLabControls.Java creates a "page" of widgets that are a view of an
 * AstAxisLabel object. They provide the ability to configure all the
 * properties of the AstAxisLabel object (that describe how the axis
 * labels of an AST plot should be drawn) and show a current rendering
 * of them.
 *
 * @author Peter W. Draper
 * @version $Id$
 *
 * @see AstNumberLabels
 * @see PlotConfigurator
 */
public class AxisNumLabControls extends JPanel 
    implements PlotControls, ChangeListener
{
    /**
     * AstAxisLabel model for current state.
     */
    protected AstNumberLabels astNumberLabels = null;

    /**
     * Label showing current font.
     */
    protected JLabel display = new JLabel( "123456789.0E1" );

    /**
     * Control for toggling display of X numbers.
     */
    protected JCheckBox xShowNumbers = new JCheckBox();

    /**
     * Control for toggling display of Y numbers.
     */
    protected JCheckBox yShowNumbers = new JCheckBox();

    /**
     * Control for whether log labelling values should be applied. If not
     * defaults are used.
     */
    protected JCheckBox logLabelSet = new JCheckBox();

    /**
     * Control for toggling display of X log-like labels.
     */
    protected JCheckBox xLogLabel = new JCheckBox();

    /**
     * Control for toggling display of Y log-like labels.
     */
    protected JCheckBox yLogLabel = new JCheckBox();

    /**
     * Control for toggling rotation of X numbers.
     */
    protected JCheckBox xRotateNumbers = new JCheckBox();

    /**
     * Control for toggling rotation of Y numbers.
     */
    protected JCheckBox yRotateNumbers = new JCheckBox();

    /**
     * Spinner for controlling the position of the X numbers.
     */
    protected ScientificSpinner xGapSpinner = null;

    /**
     * Spinner for controlling the position of the Y numbers.
     */
    protected ScientificSpinner yGapSpinner = null;

   /**
     * X Spinner model.
     */
    protected SpinnerNumberModel xSpinnerModel =
        new SpinnerNumberModel( 0.0,
                                AstNumberLabels.GAP_MIN,
                                AstNumberLabels.GAP_MAX,
                                AstNumberLabels.GAP_STEP );

   /**
     * Y Spinner model.
     */
    protected SpinnerNumberModel ySpinnerModel =
        new SpinnerNumberModel( 0.0,
                                AstNumberLabels.GAP_MIN,
                                AstNumberLabels.GAP_MAX,
                                AstNumberLabels.GAP_STEP );

    /**
     * Colour button (same for both labels).
     */
    protected JButton colourButton = new JButton();

    /**
     * Colour Icon of colour button.
     */
    protected ColourIcon colourIcon = new ColourIcon( Color.black );

    /**
     * FontControls (same for both labels).
     */
    protected FontControls fontControls = null;

    /**
     * Number of digits used in label precision.
     */
    protected JComboBox digitsField = new JComboBox();

    /**
     * The default title for these controls.
     */
    protected static String defaultTitle = "Axis number label properties:";

    /**
     * The default short name for these controls.
     */
    protected static String defaultName = "Axis numbers";

    /**
     * Create an instance.
     */
    public AxisNumLabControls( AbstractPlotControlsModel astNumberLabels )
    {
        initUI();
        setAstNumberLabels( (AstNumberLabels) astNumberLabels );
    }

    /**
     * Create and initialise the user interface.
     */
    protected void initUI()
    {
        //  Act on request to display/remove a label.
        xShowNumbers.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchXShown();
                }
            });
        yShowNumbers.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchYShown();
                }
            });

        //  Log label actions.
        logLabelSet.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchLogLabelSet();
                }
            });
        xLogLabel.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchXLogLabel();
                }
            });
        yLogLabel.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchYLogLabel();
                }
            });

        //  Act on request to show axis labels as rotated
        xRotateNumbers.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchXRotate();
                }
            });
        yRotateNumbers.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchYRotate();
                }
            });

        //  Set ColourIcon of colour button.
        colourButton.setIcon( colourIcon );
        colourButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                chooseColour();
            }
        });

        //  New gaps.
        xGapSpinner = new ScientificSpinner( xSpinnerModel );
        xGapSpinner.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchXGap();
                }
            });

        yGapSpinner = new ScientificSpinner( ySpinnerModel );
        yGapSpinner.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchYGap();
                }
            });

        //  Number of digits used in label precision.
        digitsField.addItem( "Default" );
        for ( int i = 0; i < 18; i++ ) {
            digitsField.addItem( Integer.valueOf( i ) );
        }
        digitsField.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchDigits();
                }
            });


        //  Add components.
        GridBagLayouter layouter =  Utilities.getGridBagLayouter( this );

        layouter.add( "Sample:", false );
        layouter.add( display, true );

        layouter.add( "Show X:", false );
        layouter.add( xShowNumbers, true );

        layouter.add( "Show Y:", false );
        layouter.add( yShowNumbers, true );

        layouter.add( "Set log labelling:", false );
        layouter.add( logLabelSet, true );

        layouter.add( "X log labelling:", false );
        layouter.add( xLogLabel, true );

        layouter.add( "Y log labelling:", false );
        layouter.add( yLogLabel, true );

        layouter.add( "Rotate X:", false );
        layouter.add( xRotateNumbers, true );

        layouter.add( "Rotate Y:", false );
        layouter.add( yRotateNumbers, true );

        addFontControls( layouter );

        layouter.add( "Colour:", false );
        layouter.add( colourButton, false );
        layouter.eatLine();

        layouter.add( "X gap:", false );
        layouter.add( xGapSpinner, false );
        layouter.eatLine();

        layouter.add( "Y gap:", false );
        layouter.add( yGapSpinner, false );
        layouter.eatLine();

        layouter.add( "Digits:", false );
        layouter.add( digitsField, false );
        layouter.eatLine();

        layouter.eatSpare();

        //  Set tooltips.
        colourButton.setToolTipText( "Select a colour" );
        xGapSpinner.setToolTipText( "Set the gap between numbers and axis" );
        yGapSpinner.setToolTipText( "Set the gap between numbers and axis" );

        logLabelSet.setToolTipText
            ( "Use log/exponential labelling values to override defaults" );
        xLogLabel.setToolTipText( "Use log/exponential labelling for X axis ticks" );
        yLogLabel.setToolTipText( "Use log/exponentail labelling for Y axis ticks" );

        digitsField.setToolTipText
            ( "Digits of precision used in formatted numbers" );
        xShowNumbers.setToolTipText( "Display X labels" );
        yShowNumbers.setToolTipText( "Display Y labels" );
        xRotateNumbers.setToolTipText
            ( "Display re-oriented X labels if needed" );
        yRotateNumbers.setToolTipText
            ( "Display re-oriented Y labels if needed" );
    }

    /**
     * Set the AstNumberLabels object.
     */
    public void setAstNumberLabels( AstNumberLabels astNumberLabels )
    {
        this.astNumberLabels = astNumberLabels;
        astNumberLabels.addChangeListener( this );
        updateFromAstNumberLabels();
    }

    /**
     * Update interface to reflect values of the current AstAxisLabel.
     */
    protected void updateFromAstNumberLabels()
    {
        astNumberLabels.removeChangeListener( this );

        xShowNumbers.setSelected( astNumberLabels.getXShown() );
        yShowNumbers.setSelected( astNumberLabels.getYShown() );

        logLabelSet.setSelected( astNumberLabels.getLogLabelSet() );
        xLogLabel.setSelected( astNumberLabels.getXLogLabel() );
        yLogLabel.setSelected( astNumberLabels.getYLogLabel() );
        matchLogLabelSet();

        xRotateNumbers.setSelected( astNumberLabels.getXRotated() );
        yRotateNumbers.setSelected( astNumberLabels.getYRotated() );

        display.setFont( astNumberLabels.getFont() );
        fontControls.setFont( astNumberLabels.getFont() );

        colourIcon.setMainColour( astNumberLabels.getColour() );
        colourButton.repaint();
        display.setForeground( astNumberLabels.getColour() );

        xSpinnerModel.setValue( Double.valueOf( astNumberLabels.getXGap() ) );
        ySpinnerModel.setValue( Double.valueOf( astNumberLabels.getYGap() ) );

        int digits = astNumberLabels.getDigits();
        if ( digits != -1 ) {
            digitsField.setSelectedItem( Integer.valueOf( digits ) );
        } else {
            digitsField.setSelectedItem( "Default" );
        }
        
        astNumberLabels.setXState( true );
        astNumberLabels.setYState( true );

        astNumberLabels.addChangeListener( this );
    }

    /**
     * Get copy of reference to current AstNumberLabels.
     */
    public AstNumberLabels getAstNumberLabels()
    {
        return astNumberLabels;
    }

    /**
     * Add the font controls.
     */
    private void addFontControls( GridBagLayouter layouter )
    {
        fontControls = new FontControls(  layouter, "" );
        fontControls.addListener( new FontChangedListener() {
            public void fontChanged( FontChangedEvent e ) {
                updateFont( e );
            }
        });
    }

    /**
     * Update the displayed font.
     */
    protected void updateFont( FontChangedEvent e )
    {
        setTextFont( e.getFont() );
    }

    /**
     * Set the font.
     */
    protected void setTextFont( Font font )
    {
        if ( font != null ) {
            astNumberLabels.setFont( font );
            display.setFont( font );
        }
    }

    /**
     * Match shown state of X numbers to that selected.
     */
    protected void matchXShown()
    {
        astNumberLabels.setXShown( xShowNumbers.isSelected() );
    }

    /**
     * Match shown state of Y numbers to that selected.
     */
    protected void matchYShown()
    {
        astNumberLabels.setYShown( yShowNumbers.isSelected() );
    }

    /*
     * Match whether to apply log labelling values.
     */
    protected void matchLogLabelSet()
    {
        boolean set = logLabelSet.isSelected();
        astNumberLabels.setLogLabelSet( set );
        xLogLabel.setEnabled( set );
        yLogLabel.setEnabled( set );
    }

    /**
     * Match whether to use log labelling along X axis.
     */
    protected void matchXLogLabel()
    {
        astNumberLabels.setXLogLabel( xLogLabel.isSelected() );
    }

    /**
     * Match whether to use log labelling along Y axis.
     */
    protected void matchYLogLabel()
    {
        astNumberLabels.setYLogLabel( yLogLabel.isSelected() );
    }



    /**
     * Match rotated state of X numbers to that selected.
     */
    protected void matchXRotate()
    {
        astNumberLabels.setXRotated( xRotateNumbers.isSelected() );
    }

    /**
     * Match rotated state of Y numbers to that selected.
     */
    protected void matchYRotate()
    {
        astNumberLabels.setYRotated( yRotateNumbers.isSelected() );
    }

    /**
     * Match the X AstAxisLabels gap to that shown.
     */
    protected void matchXGap()
    {
        astNumberLabels.setXGap( xSpinnerModel.getNumber().doubleValue() );
    }

    /**
     * Match the Y AstAxisLabels gap to that shown.
     */
    protected void matchYGap()
    {
        astNumberLabels.setYGap( ySpinnerModel.getNumber().doubleValue() );
    }

    /**
     * Match digits to those selected.
     */
    protected void matchDigits()
    {
        Object object = digitsField.getSelectedItem();
        int value = -1;
        if ( ! object.equals( "Default" ) ) {
            value = ((Integer) object).intValue();
        }
        astNumberLabels.setDigits( value );
    }

    /**
     * Update the text colour.
     */
    protected void chooseColour()
    {
        Color newColour = JColorChooser.showDialog(
            this, "Select text colour", colourIcon.getMainColour() );
        if ( newColour != null ) {
            setTextColour( newColour );
        }
    }

    /**
     * Set the text colour.
     */
    protected void setTextColour( Color colour )
    {
        if ( colour != null ) {
            colourIcon.setMainColour( colour );
            colourButton.repaint();
            astNumberLabels.setColour( colour );
            display.setForeground( colour );
        }
    }

//
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
     * Reset interface to defaults.
     */
    public void reset()
    {
        astNumberLabels.setDefaults();
        fontControls.setDefaults();
        setTextColour( Color.black );
        updateFromAstNumberLabels();
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
        return astNumberLabels;
    }

    /**
     * Return the class of object that we expect as our model.
     */
    public static Class getControlsModelClass()
    {
        return AstNumberLabels.class;
    }

//
// Implement the ChangeListener interface
//
    /**
     * If the AstNumberLabels object changes then we need to update the
     * interface.
     */
    public void stateChanged( ChangeEvent e )
    {
        updateFromAstNumberLabels();
    }
}
