/*
 * Copyright (C) 2000-2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     10-OCT-2002 (Peter W. Draper):
 *        Original version.
 *     18-FEB-2004 (Peter W. Draper):
 *        Added log axis.
 */
package uk.ac.starlink.ast.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.ast.Frame; // for javadocs
import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * AxesControls creates a "page" of widgets that are a view of an
 * AstAxes object. They provide the ability to configure all the
 * properties of the AstAxes object.
 *
 * @author Peter W. Draper
 * @version $Id$
 *
 * @see AstAxes
 * @see PlotConfigurator
 */
public class AxesControls extends JPanel
    implements PlotControls, ChangeListener
{
    /**
     * AstAxes model for current state.
     */
    private AstAxes astAxes = null;

    /**
     * The PlotController, used to access a {@link Frame} for
     * formatting/unformatting axes correctly.
     */
    protected PlotController controller = null;

    /**
     * Whether the X axis is to be shown.
     */
    private JCheckBox showX = new JCheckBox();

    /**
     * Whether the Y axis is to be shown.
     */
    private JCheckBox showY = new JCheckBox();

    /**
     * Whether the X axis is to drawn with log scaling.
     */
    private JCheckBox logX = new JCheckBox();

    /**
     * Whether the Y axis is to drawn with log scaling.
     */
    private JCheckBox logY = new JCheckBox();

    /**
     * Whether the axes are drawn on the interior.
     */
    private JCheckBox interior = new JCheckBox();

    /**
     * Preferred coordinate for drawing the X axis, i.e. Y origin.
     */
    private AstDoubleField xLabelAt = null;

    /**
     * Preferred coordinate for drawing the Y axis, i.e X origin.
     */
    private AstDoubleField yLabelAt = null;

    /**
     * X axis line properties controls.
     */
    private LineControls xLineControls = null;

    /**
     * Y axis line properties controls.
     */
    private LineControls yLineControls = null;

    /**
     * Whether to inhibit change events from LineControls from recyling.
     */
    private boolean inhibitLineChangeListener = false;

    /**
     * The default title for these controls.
     */
    private static String defaultTitle = "Axes display properties:";

    /**
     * The default short name for these controls.
     */
    private static String defaultName = "Axes";

    /**
     * Create an instance.
     */
    public AxesControls( AbstractPlotControlsModel astAxes,
                         PlotController controller )
    {
        this.controller = controller;
        initUI();
        setAstAxes( (AstAxes) astAxes );
    }

    /**
     * Create and initialise the user interface.
     */
    private void initUI()
    {
        //  Whether X axis is shown or not.
        showX.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchXShow();
                }
            });

        //  Whether Y axis is shown or not.
        showY.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchYShow();
                }
            });

        // Are axes logarithmic.
        logX.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchXLog();
                }
            });

        logY.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchYLog();
                }
            });

        //  Whether to display axes in interior.
        interior.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchInterior();
                }
            });

        //  X and Y origins.
        yLabelAt = new AstDoubleField( 0.0, controller, 2 );
        yLabelAt.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchYLabelAt();
                }
            });

        xLabelAt = new AstDoubleField( 0.0, controller, 1 );
        xLabelAt.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchXLabelAt();
                }
            });


        //  Add components.
        GridBagLayouter layouter = Utilities.getGridBagLayouter( this );

        layouter.add( "Show X:", false );
        layouter.add( showX, true );

        layouter.add( "Show Y:", false );
        layouter.add( showY, true );

        layouter.add( "Log X:", false );
        layouter.add( logX, true );

        layouter.add( "Log Y:", false );
        layouter.add( logY, true );

        layouter.add( "Interior:", false );
        layouter.add( interior, true );

        //  X and Y origins.
        layouter.add( "X origin:", false );
        layouter.add( yLabelAt, true );

        layouter.add( "Y origin:", false );
        layouter.add( xLabelAt, true );

        //  X axis line controls.
        addXLineControls( layouter );

        //  Y axis line controls.
        addYLineControls( layouter );

        //  Eat up all spare vertical space (pushes widgets to top).
        layouter.eatSpare();

        //  Set tooltips.
        showX.setToolTipText( "Display an X axis" );
        showY.setToolTipText( "Display a Y axis" );
        logX.setToolTipText( "Use logarithmic scaling of X axis" );
        logY.setToolTipText( "Use logarithmic scaling of Y axis" );
        interior.setToolTipText( "Display axes in interior, if needed" );
        xLabelAt.setToolTipText(
            "Origin coordinate for Y axis (units of Y), <Return> to accept" );
        yLabelAt.setToolTipText(
            "Origin coordinate for X axis (units of X), <Return> to accept" );
    }

    /**
     * Set the AstAxes object (only after UI is initiliased).
     */
    public void setAstAxes( AstAxes astAxes )
    {
        this.astAxes = astAxes;
        astAxes.addChangeListener( this );
        updateFromAstAxes();
    }

    /**
     * Update interface to reflect values of AstAxes object.
     */
    private void updateFromAstAxes()
    {
        astAxes.removeChangeListener( this );

        showX.setSelected( astAxes.getXShown() );
        showY.setSelected( astAxes.getYShown() );

        logX.setSelected( astAxes.getXLog() );
        logY.setSelected( astAxes.getYLog() );

        interior.setSelected( astAxes.getInterior() );

        xLabelAt.setDoubleValue( astAxes.getXLabelAt() );
        yLabelAt.setDoubleValue( astAxes.getYLabelAt() );

        inhibitLineChangeListener = true;

        xLineControls.setThick( (int) astAxes.getXWidth() );
        xLineControls.setColour( astAxes.getXColour() );
        xLineControls.setStyle( (int) astAxes.getXStyle() );

        yLineControls.setThick( (int) astAxes.getYWidth() );
        yLineControls.setColour( astAxes.getYColour() );
        yLineControls.setStyle( (int) astAxes.getYStyle() );

        inhibitLineChangeListener = false;

        astAxes.setXState( true );
        astAxes.setYState( true );

        astAxes.addChangeListener( this );
    }

    /**
     * Get copy of reference to current AstAxes.
     */
    public AstAxes getAstAxes()
    {
        return astAxes;
    }

    /**
     * Match whether to display the X axis.
     */
    private void matchXShow()
    {
        astAxes.setXShown( showX.isSelected() );
    }

    /**
     * Match whether to display the Y axis.
     */
    private void matchYShow()
    {
        astAxes.setYShown( showY.isSelected() );
    }

    /**
     * Match whether to display X axis logarithmically.
     */
    private void matchXLog()
    {
        astAxes.setXLog( logX.isSelected() );
    }

    /**
     * Match whether to display Y axis logarithmically.
     */
    private void matchYLog()
    {
        astAxes.setYLog( logY.isSelected() );
    }

    /**
     * Match whether to show axes in interior.
     */
    private void matchInterior()
    {
        astAxes.setInterior( interior.isSelected() );
    }

    /**
     * Match X axis origin coordinate.
     */
    protected void matchXLabelAt() 
    {
        astAxes.setXLabelAt( xLabelAt.getDoubleValue() );
    }

    /**
     * Match Y axis origin coordinate.
     */
    protected void matchYLabelAt() 
    {
        astAxes.setYLabelAt( yLabelAt.getDoubleValue() );
    }

    /**
     * Add X line property controls.
     */
    private void addXLineControls( GridBagLayouter layouter )
    {
        xLineControls = new LineControls( layouter, "X" );

        //  Respond to changed of line properties.
        xLineControls.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchXLine();
                }
            });
    }

    /**
     * Add Y line property controls.
     */
    private void addYLineControls( GridBagLayouter layouter )
    {
        yLineControls = new LineControls( layouter, "Y" );

        //  Respond to changed of line properties.
        yLineControls.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchYLine();
                }
            });
    }

    /**
     * Match X line properties.
     */
    protected void matchXLine()
    {
        if ( ! inhibitLineChangeListener ) {

            //  Update AstAxes object to match properties. Take care
            //  as modifying this fires a ChangeEvent that attempts to
            //  synchronize the lineControls (which are actually
            //  storing the state for now).
            astAxes.removeChangeListener( this );

            astAxes.setXWidth( xLineControls.getThick() );
            astAxes.setXStyle( xLineControls.getStyle() );
            astAxes.setXColour( xLineControls.getColour() );

            astAxes.addChangeListener( this );
            updateFromAstAxes();
        }
    }

    /**
     * Match Y line properties.
     */
    protected void matchYLine()
    {
        if ( ! inhibitLineChangeListener ) {

            //  Update AstAxes object to match properties. Take care
            //  as modifying this fires a ChangeEvent that attempts to
            //  synchronize the lineControls (which are actually
            //  storing the state for now).
            astAxes.removeChangeListener( this );

            astAxes.setYWidth( yLineControls.getThick() );
            astAxes.setYStyle( yLineControls.getStyle() );
            astAxes.setYColour( yLineControls.getColour() );

            astAxes.addChangeListener( this );
            updateFromAstAxes();
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
     * Reset controls to defaults.
     */
    public void reset()
    {
        astAxes.setDefaults();
        xLineControls.reset();
        yLineControls.reset();
        updateFromAstAxes();
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
        return astAxes;
    }

    /**
     * Return the class of object that we expect as our model.
     */
    public static Class getControlsModelClass()
    {
        return AstAxes.class;
    }

//
// Implement the ChangeListener interface
//
    /**
     * If the AstGrid object changes then we need to update the
     * interface.
     */
    public void stateChanged( ChangeEvent e )
    {
        updateFromAstAxes();
    }
}
