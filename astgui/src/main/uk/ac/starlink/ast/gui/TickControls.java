/*
 * Copyright (C) 2000-2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     20-AUG-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import uk.ac.starlink.ast.Frame;  // for javadocs

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
     * GridBagConstraints object.
     */
    protected GridBagConstraints gbc = new GridBagConstraints();

    /**
     * Label Insets.
     */
    protected Insets labelInsets = new Insets( 10, 5, 5, 10 );

    /**
     * Check box for  whether ticks should be shown or not.
     */
    protected JCheckBox show = new JCheckBox();

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
    protected JSpinner xMajorLength = null;

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
    protected JSpinner yMajorLength = null;

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
    protected JSpinner xMinorLength = null;

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
    protected JSpinner yMinorLength = null;

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
    protected static String defaultTitle = "Tick Properties:";

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
        setLayout( new GridBagLayout() );

        //  Whether ticks are shown or not.
        show.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchShow();
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
        xMajorLength = new JSpinner( xMajorLengthModel );
        xMajorLength.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchXMajorLength();
                }
            });
        yMajorLength = new JSpinner( yMajorLengthModel );
        yMajorLength.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchYMajorLength();
                }
            });

        //  Length of minor ticks.
        xMinorLength = new JSpinner( xMinorLengthModel );
        xMinorLength.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchXMinorLength();
                }
            });
        yMinorLength = new JSpinner( yMinorLengthModel );
        yMinorLength.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchYMinorLength();
                }
            });

        //  Number of divisions between major ticks.
        xMinorDivisions.addItem( "Default" );
        for ( int i = 1; i < 21; i++ ) {
            xMinorDivisions.addItem( new Integer( i ) );
        }
        xMinorDivisions.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchXMinorDivisions();
                }
            });

        yMinorDivisions.addItem( "Default" );
        for ( int i = 1; i < 21; i++ ) {
            yMinorDivisions.addItem( new Integer( i ) );
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


        //  Add labels for all fields.
        addLabel( "Show Ticks:", 0 );
        addLabel( "X Spacing:", 1 );
        addLabel( "Y Spacing:", 2 );
        addLabel( "Thickness:" , 3 );
        addLabel( "Style:", 4 );
        addLabel( "Colour:", 5 );
        addLabel( "X Major Len:", 6 );
        addLabel( "Y Major Len:", 7 );
        addLabel( "X Minor Len:", 8 );
        addLabel( "Y Minor Len:", 9 );
        addLabel( "X Divisions:", 10 );
        addLabel( "Y Divisions:", 11 );
        addLabel( "Tick All:", 12 );

        gbc.insets = new Insets( 0, 0, 0, 0 );
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weighty = 0.0;
        gbc.weightx = 1.0;
        gbc.gridx = 1;

        //  Current row for adding components.
        int row = 0;

        //  Show
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy = row++;
        add( show, gbc );

        //  Gaps
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = row++;
        add( xMajorGap, gbc );

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = row++;
        add( yMajorGap, gbc );

        //  Line controls.
        row = addLineControls( row );

        //  Lengths
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy = row++;
        add( xMajorLength, gbc );

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy = row++;
        add( yMajorLength, gbc );

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy = row++;
        add( xMinorLength, gbc );

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy = row++;
        add( yMinorLength, gbc );

        //  Divisions
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy = row++;
        add( xMinorDivisions, gbc );

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy = row++;
        add( yMinorDivisions, gbc );

        //  Tick all
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy = row++;
        add( tickAll, gbc );

        //  Eat up all spare vertical space (pushes widgets to top).
        Component filly = Box.createVerticalStrut( 5 );
        gbc.gridy = row++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        add( filly, gbc );

        //  Set tooltips.
        show.setToolTipText( "Show tick marks in plot" );
        xMajorGap.setToolTipText(
            "Gap between major ticks (units of X axis), <Return> to accept" );
        yMajorGap.setToolTipText(
            "Gap between major ticks (units of Y axis), <Return> to accept" );
        xMajorLength.setToolTipText(
            "Length of X major tick marks" );
        yMajorLength.setToolTipText(
            "Length of Y major tick marks" );
        xMinorLength.setToolTipText(
            "Length of X minor tick marks" );
        yMinorLength.setToolTipText(
            "Length of Y minor tick marks" );
        xMinorDivisions.setToolTipText(
            "Number of divisions between major ticks" );
        yMinorDivisions.setToolTipText(
            "Number of divisions between major ticks" );
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

        inhibitLineChangeListener = true;
        lineControls.setThick( (int) astTicks.getWidth() );
        lineControls.setColour( astTicks.getColour() );
        lineControls.setStyle( (int) astTicks.getStyle() );
        inhibitLineChangeListener = false;

        xMajorGap.setDoubleValue( astTicks.getXGap() );
        yMajorGap.setDoubleValue( astTicks.getYGap() );

        xMajorLengthModel.setValue(new Double(astTicks.getMajorXTicklength()));
        yMajorLengthModel.setValue(new Double(astTicks.getMajorYTicklength()));
        xMinorLengthModel.setValue(new Double(astTicks.getMinorXTicklength()));
        yMinorLengthModel.setValue(new Double(astTicks.getMinorYTicklength()));

        int div = astTicks.getMinorXDivisions();
        if ( div == 0 ) {
            xMinorDivisions.setSelectedItem( "Default" );
        } else {
            xMinorDivisions.setSelectedItem( new Integer( div ) );
        }
        div = astTicks.getMinorYDivisions();
        if ( div == 0 ) {
            yMinorDivisions.setSelectedItem( "Default" );
        } else {
            yMinorDivisions.setSelectedItem( new Integer( div ) );
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
     * Add a new label. This is added to the front of the given row.
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
     * Add line property controls.
     */
    private int addLineControls( int row ) 
    {
        lineControls = new LineControls( this, row, 1 );

        //  Respond to changed of line properties.
        lineControls.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchLine();
                }
            });
        return row + 3;
    }

    /**
     * Match whether to display the ticks.
     */
    protected void matchShow() 
    {
        astTicks.setShown( show.isSelected() );
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
