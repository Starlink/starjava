/*
 * Copyright (C) 2000-2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     20-AUG-2000 (Peter W. Draper):
 *        Original version.
 *     18-FEB-2004 (Peter W. Draper):
 *        Added GridBagLayouter and log spacing.
 */
package uk.ac.starlink.ast.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractSpinnerModel;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import uk.ac.starlink.ast.Frame;  // for javadocs
import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * TickControls.Java creates a "page" of widgets that are a view of an
 * AstTicks object. They provide the ability to configure all the
 * properties of the AstTicks object and show a current rendering
 * of them.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see AstTicks
 * @see PlotConfigurator
 */
public class TickControls extends JPanel
    implements PlotControls, ChangeListener
{
    /**
     * AstTicks model for current state.
     */
    protected AstTicks astTicks = null;

    /**
     * The PlotController, used to access a {@link Frame} for
     * formatting/unformatting axes correctly.
     */
    protected PlotController controller = null;

    /**
     * Check box for whether ticks should be shown or not.
     */
    protected JCheckBox show = new JCheckBox();

    /**
     * Check box for whether log ticks setting should be applied.
     */
    protected JCheckBox logSpacingSet = new JCheckBox();

    /**
     * Check box for whether to use log spacing for X axis.
     */
    protected JCheckBox xLogSpacing = new JCheckBox();

    /**
     * Check box for whether to use log spacing for Y axis.
     */
    protected JCheckBox yLogSpacing = new JCheckBox();

    /**
     * Entry for ratio gap between major ticks for log X axis.
     */
    protected ScientificSpinner xLogGap = null;
    protected SpinnerNumberModel xLogGapModel =
        new SpinnerNumberModel( 0, 0, 10, 1 );

    /**
     * Entry for the gap between major ticks on the Y axis.
     */
    protected ScientificSpinner yLogGap = null;
    protected SpinnerNumberModel yLogGapModel =
        new SpinnerNumberModel( 0, 0, 10, 1 );

    /**
     * Entry for gap between major ticks on the X axis.
     */
    protected AstDoubleField xMajorGap = null;

    /**
     * Entry for the gap between major ticks on the Y axis.
     */
    protected AstDoubleField yMajorGap = null;

    /**
     * Spinner for controlling the length of X axis major tick marks.
     */
    protected ScientificSpinner xMajorLength = null;

    /**
     * Spinner model for length of X axis major tick marks.
     */
    protected SpinnerNumberModel xMajorLengthModel =
        new SpinnerNumberModel( 0.0,
                                AstTicks.MIN_LENGTH,
                                AstTicks.MAX_LENGTH,
                                AstTicks.STEP_LENGTH );

    /**
     * Spinner for controlling the length of Y axis major tick marks.
     */
    protected ScientificSpinner yMajorLength = null;

    /**
     * Spinner model for length of Y axis major tick marks.
     */
    protected SpinnerNumberModel yMajorLengthModel =
        new SpinnerNumberModel( 0.0,
                                AstTicks.MIN_LENGTH,
                                AstTicks.MAX_LENGTH,
                                AstTicks.STEP_LENGTH );

    /**
     * Spinner for controlling the length of X axis minor tick marks.
     */
    protected ScientificSpinner xMinorLength = null;

    /**
     * Spinner model for length of X axis minor tick marks.
     */
    protected SpinnerNumberModel xMinorLengthModel =
        new SpinnerNumberModel( 0.0,
                                AstTicks.MIN_LENGTH,
                                AstTicks.MAX_LENGTH,
                                AstTicks.STEP_LENGTH );

    /**
     * Spinner for controlling the length of Y axis minor tick marks.
     */
    protected ScientificSpinner yMinorLength = null;

    /**
     * Spinner model for length of X axis minor tick marks.
     */
    protected SpinnerNumberModel yMinorLengthModel =
        new SpinnerNumberModel( 0.0,
                                AstTicks.MIN_LENGTH,
                                AstTicks.MAX_LENGTH,
                                AstTicks.STEP_LENGTH );

    /*
     * JComboBox for selecting the number of intervals between major
     * tick marks along the X axis.
     */
    protected JComboBox xMinorDivisions = new JComboBox();

    /*
     * JComboBox for controlling the number of intervals between major
     * tick marks along the Y axis.
     */
    protected JComboBox yMinorDivisions = new JComboBox();

    /**
     * Check box for  whether ticks should be shown on all axes.
     */
    protected JCheckBox tickAll = new JCheckBox();

    /**
     * Line controls widget for all line specific properties.
     */
    protected LineControls lineControls = null;

    /**
     * Whether to inhibit change events from LineControls from recyling.
     */
    protected boolean inhibitLineChangeListener = false;

    /**
     * The default title for these controls.
     */
    protected static String defaultTitle = "Tick properties:";

    /**
     * The default short name for these controls.
     */
    protected static String defaultName = "Ticks";

    /**
     * Create an instance.
     *
     * @param astTicks the model that we're a view of
     * @param controller used to gain access to the Plot, this is
     *                   needed for the configuration of any AstDouble
     */
    public TickControls( AbstractPlotControlsModel astTicks,
                         PlotController controller )
    {
        this.controller = controller;
        initUI();
        setAstTicks( (AstTicks) astTicks );
    }

    /**
     * Create and initialise the user interface.
     */
    protected void initUI()
    {
        //  Whether ticks are shown or not.
        show.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchShow();
                }
            });

        //  Whether log spacing preferences are used.
        logSpacingSet.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchLogSpacingSet();
                }
            });

        //  Whether log spacing is used on X axis.
        xLogSpacing.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchXLogSpacing();
                }
            });

        //  Whether log spacing is used on Y axis.
        yLogSpacing.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchYLogSpacing();
                }
            });

        //  Major gap spacing for log axes. A ratio.
        xLogGap = new ScientificSpinner( xLogGapModel );
        xLogGap.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchXLogGap();
                }
            });
        yLogGap = new ScientificSpinner( yLogGapModel );
        yLogGap.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchYLogGap();
                }
            });


        //  Separation between major ticks. Use an AstDouble related
        //  control to get values in the units of the X axis.
        xMajorGap = new AstDoubleField( 0.0, controller, 1 );
        xMajorGap.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchXMajorGap();
                }
            });

        yMajorGap = new AstDoubleField( 0.0, controller, 2 );
        yMajorGap.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchYMajorGap();
                }
            });

        //  Length of major ticks.
        xMajorLength = new ScientificSpinner( xMajorLengthModel );
        xMajorLength.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchXMajorLength();
                }
            });
        yMajorLength = new ScientificSpinner( yMajorLengthModel );
        yMajorLength.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchYMajorLength();
                }
            });

        //  Length of minor ticks.
        xMinorLength = new ScientificSpinner( xMinorLengthModel );
        xMinorLength.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchXMinorLength();
                }
            });
        yMinorLength = new ScientificSpinner( yMinorLengthModel );
        yMinorLength.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchYMinorLength();
                }
            });

        //  Number of divisions between major ticks.
        xMinorDivisions.addItem( "Default" );
        for ( int i = 1; i < 21; i++ ) {
            xMinorDivisions.addItem( Integer.valueOf( i ) );
        }
        xMinorDivisions.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchXMinorDivisions();
                }
            });

        yMinorDivisions.addItem( "Default" );
        for ( int i = 1; i < 21; i++ ) {
            yMinorDivisions.addItem( Integer.valueOf( i ) );
        }
        yMinorDivisions.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchYMinorDivisions();
                }
            });

        //  Whether all axes are ticked.
        tickAll.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchTickAll();
                }
            });


        //  Add components.
        GridBagLayouter layouter = Utilities.getGridBagLayouter( this );

        layouter.add( "Show ticks:", false );
        layouter.add( show, true );

        layouter.add( "X spacing:", false );
        layouter.add( xMajorGap, true );

        layouter.add( "Y spacing:", false );
        layouter.add( yMajorGap, true );

        layouter.add( "Set log spacing:", false );
        layouter.add( logSpacingSet, true );

        layouter.add( "X log spacing:", false );
        layouter.add( xLogSpacing, true );

        layouter.add( "Y log spacing:", false );
        layouter.add( yLogSpacing, true );

        layouter.add( "X log gap:", false );
        layouter.add( xLogGap, false );
        layouter.eatLine();

        layouter.add( "Y log gap:", false );
        layouter.add( yLogGap, false );
        layouter.eatLine();

        addLineControls( layouter );

        layouter.add( "X major len:", false );
        layouter.add( xMajorLength, false );
        layouter.eatLine();

        layouter.add( "Y major len:", false );
        layouter.add( yMajorLength, false );
        layouter.eatLine();

        layouter.add( "X minor len:", false );
        layouter.add( xMinorLength, false );
        layouter.eatLine();

        layouter.add( "Y minor len:", false );
        layouter.add( yMinorLength, false );
        layouter.eatLine();

        layouter.add( "X divisions:", false );
        layouter.add( xMinorDivisions, false );
        layouter.eatLine();

        layouter.add( "Y divisions:", false );
        layouter.add( yMinorDivisions, false );
        layouter.eatLine();

        layouter.add( "Tick all:", false );
        layouter.add( tickAll, true );

        layouter.eatSpare();

        //  Set tooltips.
        show.setToolTipText( "Show tick marks in plot" );
        logSpacingSet.setToolTipText
            ( "Use log spacing settings to override defaults" );
        xLogSpacing.setToolTipText( "Use log spacing for X axis ticks" );
        yLogSpacing.setToolTipText( "Use log spacing for Y axis ticks" );
        xLogGap.setToolTipText
          ("Gap ratio for major ticks when X is log axis, <Return> to accept");
        yLogGap.setToolTipText
          ("Gap ratio for major ticks when Y is log axis, <Return> to accept");

        xMajorGap.setToolTipText
            ("Gap between major ticks (units of X axis), <Return> to accept");
        yMajorGap.setToolTipText
            ("Gap between major ticks (units of Y axis), <Return> to accept");
        xMajorLength.setToolTipText( "Length of X major tick marks" );
        yMajorLength.setToolTipText( "Length of Y major tick marks" );
        xMinorLength.setToolTipText( "Length of X minor tick marks" );
        yMinorLength.setToolTipText( "Length of Y minor tick marks" );
        xMinorDivisions.setToolTipText
            ( "Number of divisions between major ticks" );
        yMinorDivisions.setToolTipText
            ( "Number of divisions between major ticks" );
        tickAll.setToolTipText( "Add ticks to all axes" );
    }

    /**
     * Set the AstTicks object.
     */
    public void setAstTicks( AstTicks astTicks )
    {
        this.astTicks = astTicks;
        astTicks.addChangeListener( this );
        updateFromAstTicks();
    }

    /**
     * Update interface to reflect values of the current AstAxisLabel.
     */
    protected void updateFromAstTicks()
    {
        astTicks.removeChangeListener( this );

        show.setSelected( astTicks.getShown() );

        logSpacingSet.setSelected( astTicks.getLogSpacingSet() );
        xLogSpacing.setSelected( astTicks.getXLogSpacing() );
        yLogSpacing.setSelected( astTicks.getYLogSpacing() );
        matchLogSpacingSet();

        xLogGap.setValue( Integer.valueOf( astTicks.getXLogGap() ) );
        yLogGap.setValue( Integer.valueOf( astTicks.getYLogGap() ) );

        inhibitLineChangeListener = true;
        lineControls.setThick( (int) astTicks.getWidth() );
        lineControls.setColour( astTicks.getColour() );
        lineControls.setStyle( (int) astTicks.getStyle() );
        inhibitLineChangeListener = false;

        xMajorGap.setDoubleValue( astTicks.getXGap() );
        yMajorGap.setDoubleValue( astTicks.getYGap() );

        xMajorLengthModel.setValue(Double
                                  .valueOf(astTicks.getMajorXTicklength()));
        yMajorLengthModel.setValue(Double
                                  .valueOf(astTicks.getMajorYTicklength()));
        xMinorLengthModel.setValue(Double
                                  .valueOf(astTicks.getMinorXTicklength()));
        yMinorLengthModel.setValue(Double
                                  .valueOf(astTicks.getMinorYTicklength()));

        int div = astTicks.getMinorXDivisions();
        if ( div == 0 ) {
            xMinorDivisions.setSelectedItem( "Default" );
        } else {
            xMinorDivisions.setSelectedItem( Integer.valueOf( div ) );
        }
        div = astTicks.getMinorYDivisions();
        if ( div == 0 ) {
            yMinorDivisions.setSelectedItem( "Default" );
        } else {
            yMinorDivisions.setSelectedItem( Integer.valueOf( div ) );
        }

        tickAll.setSelected( astTicks.getTickAll() );

        astTicks.setState( true );

        astTicks.addChangeListener( this );
    }

    /**
     * Get copy of reference to current AstTicks.
     */
    public AstTicks getAstTicks()
    {
        return astTicks;
    }

    /**
     * Add line property controls.
     */
    private void addLineControls( GridBagLayouter layouter )
    {
        lineControls = new LineControls( layouter, "" );

        //  Respond to changed of line properties.
        lineControls.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchLine();
                }
            });
    }

    /**
     * Match whether to display the ticks.
     */
    protected void matchShow()
    {
        astTicks.setShown( show.isSelected() );
    }

    /**
     * Match whether to apply log spacing values.
     */
    protected void matchLogSpacingSet()
    {
        boolean set = logSpacingSet.isSelected();
        astTicks.setLogSpacingSet( set );
        xLogSpacing.setEnabled( set );
        yLogSpacing.setEnabled( set );
        xLogGap.setEnabled( set );
        yLogGap.setEnabled( set );
    }

    /**
     * Match whether to use log spacing along X axis.
     */
    protected void matchXLogSpacing()
    {
        astTicks.setXLogSpacing( xLogSpacing.isSelected() );
    }

    /**
     * Match whether to use log spacing along Y axis.
     */
    protected void matchYLogSpacing()
    {
        astTicks.setYLogSpacing( yLogSpacing.isSelected() );
    }

    /**
     * Match X log axis ratio for the major gap.
     */
    protected void matchXLogGap()
    {
        astTicks.setXLogGap( xLogGapModel.getNumber().intValue() );
    }

    /**
     * Match Y log axis ratio for the major gap.
     */
    protected void matchYLogGap()
    {
        astTicks.setYLogGap( yLogGapModel.getNumber().intValue() );
    }

    /**
     * Match line properties.
     */
    protected void matchLine()
    {
        if ( ! inhibitLineChangeListener ) {
            //  Update AstTicks object to match properties. Take care
            //  as modifying this fires a ChangeEvent that attempts to
            //  synchronize the lineControls (which are actually
            //  storing the state for now).
            astTicks.removeChangeListener( this );

            astTicks.setWidth( lineControls.getThick() );
            astTicks.setStyle( lineControls.getStyle() );
            astTicks.setColour( lineControls.getColour() );

            astTicks.addChangeListener( this );
            updateFromAstTicks();
        }
    }

    /**
     * Match X axis major gap.
     */
    protected void matchXMajorGap()
    {
        astTicks.setXGap( xMajorGap.getDoubleValue() );
    }

    /**
     * Match Y axis major gap.
     */
    protected void matchYMajorGap()
    {
        astTicks.setYGap( yMajorGap.getDoubleValue() );
    }

    /**
     * Match length of major ticks on X axis.
     */
    protected void matchXMajorLength()
    {
        astTicks.setMajorXTicklength(xMajorLengthModel.getNumber().doubleValue());
    }

    /**
     * Match length of major ticks on Y axis.
     */
    protected void matchYMajorLength()
    {
        astTicks.setMajorYTicklength(yMajorLengthModel.getNumber().doubleValue());
    }

    /**
     * Match length of minor ticks on X axis.
     */
    protected void matchXMinorLength()
    {
        astTicks.setMinorXTicklength(xMinorLengthModel.getNumber().doubleValue());
    }

    /**
     * Match length of minor ticks on Y axis.
     */
    protected void matchYMinorLength()
    {
        astTicks.setMinorYTicklength(yMinorLengthModel.getNumber().doubleValue());
    }

    /**
     * Match number of divisions between major ticks on X axis.
     */
    protected void matchXMinorDivisions()
    {
        Object object = xMinorDivisions.getSelectedItem();
        int value = 0;
        if ( ! object.equals( "Default" ) ) {
            value = ((Integer) object).intValue();
        }
        astTicks.setMinorXDivisions( value );
    }

    /**
     * Match number of divisions between major ticks on Y axis.
     */
    protected void matchYMinorDivisions()
    {
        Object object = yMinorDivisions.getSelectedItem();
        int value = 0;
        if ( ! object.equals( "Default" ) ) {
            value = ((Integer) object).intValue();
        }
        astTicks.setMinorYDivisions( value );
    }

    /**
     * Match whether to display ticks on all axes.
     */
    protected void matchTickAll()
    {
        astTicks.setTickAll( tickAll.isSelected() );
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
     * Reset interface to default configuration.
     */
    public void reset() {
        lineControls.reset();
        astTicks.setDefaults();
        updateFromAstTicks();
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
        return astTicks;
    }

    /**
     * Return the class of object that we expect as our model.
     */
    public static Class getControlsModelClass()
    {
        return AstTicks.class;
    }

//
// Implement the ChangeListener interface
//
    /**
     * If the AstTicks object changes then we need to update the
     * interface.
     */
    public void stateChanged( ChangeEvent e )
    {
        updateFromAstTicks();
    }

}
