/*
 * Copyright (C) 2000-2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     28-NOV-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * GraphicsEdgesControls creates a "page" of widgets that are a view
 * of a GraphicsHints object. They provide the ability to configure
 * all the properties of the object (i.e. whether the graphics
 * are clipped and how much space to reserve for the labelling).
 *
 * @author Peter W. Draper
 * @version $Id$
 *
 * @see GraphicsHints
 * @see PlotConfigurator
 */
public class GraphicsEdgesControls extends JPanel 
    implements PlotControls, ChangeListener
{
    /**
     * GraphicsEdges model for current state.
     */
    protected GraphicsEdges edges = null;

    /**
     * Whether graphics should be clipped.
     */
    protected JCheckBox clip = new JCheckBox();

    /**
     * Spinner for controlling the space reserved for X labels.
     */
    protected JSpinner xFraction = null;

    /**
     * Spinner model for X fraction.
     */
    protected SpinnerNumberModel xFractionModel = 
        new SpinnerNumberModel( 0.0,
                                GraphicsEdges.GAP_MIN,
                                GraphicsEdges.GAP_MAX,
                                GraphicsEdges.GAP_STEP );

    /**
     * Spinner for controlling the space reserved for Y labels.
     */
    protected JSpinner yFraction = null;

    /**
     * Spinner model for Y fraction.
     */
    protected SpinnerNumberModel yFractionModel = 
        new SpinnerNumberModel( 0.0,
                                GraphicsEdges.GAP_MIN,
                                GraphicsEdges.GAP_MAX,
                                GraphicsEdges.GAP_STEP );

    /**
     * The default title for these controls.
     */
    protected static String defaultTitle = "Edge drawing properties:";

    /**
     * The default short name for these controls.
     */
    protected static String defaultName = "Edges";

    /**
     * Create an instance.
     */
    public GraphicsEdgesControls( AbstractPlotControlsModel edges )
    {
        initUI();
        setGraphicsEdges( (GraphicsEdges) edges );
    }

    /**
     * Create and initialise the user interface.
     */
    protected void initUI()
    {
        //  Clip graphics to within border.
        clip.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchClip();
                }
            });

        //  Set X fraction.
        xFraction = new JSpinner( xFractionModel );
        xFraction.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchXFraction();
                }
            });

        //  Set Y fraction.
        yFraction = new JSpinner( yFractionModel );
        yFraction.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchYFraction();
                }
            });

        //  Add labels for all fields.
        GridBagLayouter layouter =  Utilities.getGridBagLayouter( this );

        layouter.add( "Clip graphics:", false );
        layouter.add( clip, true );

        layouter.add( "X reserve:", false );
        layouter.add( xFraction, false );
        layouter.eatLine();

        layouter.add( "Y reserve:", false );
        layouter.add( yFraction, false );
        layouter.eatLine();

        layouter.eatSpare();

        //  Set tooltips.
        clip.setToolTipText( "Clip graphics to lie within border" );
        xFraction.setToolTipText
            ( "Set space reserved for X labels (fraction)" );
        yFraction.setToolTipText
            ( "Set space reserved for Y labels (fraction)" );
    }

    /**
     * Set the GraphicsEdges object (only after UI is initiliased).
     */
    public void setGraphicsEdges( GraphicsEdges edges )
    {
        this.edges = edges;
        edges.addChangeListener( this );
        updateFromGraphicsEdges();
    }

    /**
     * Update interface to reflect values of GraphicsEdges object.
     */
    protected void updateFromGraphicsEdges()
    {
        edges.removeChangeListener( this );
        clip.setSelected( edges.isClipped() );
        xFractionModel.setValue( new Double( edges.getXFrac() ) );
        yFractionModel.setValue( new Double( edges.getYFrac() ) );
        edges.addChangeListener( this );
    }

    /**
     * Get copy of reference to current GraphicsEdges
     */
    public GraphicsEdges getGraphicsEdges()
    {
        return edges;
    }

    /**
     * Match whether to clip.
     */
    protected void matchClip()
    {
        edges.setClipped( clip.isSelected() );
    }

    /**
     * Match X label fraction.
     */
    protected void matchXFraction()
    {
        edges.setXFrac( xFractionModel.getNumber().doubleValue() );
    }

    /**
     * Match Y label fraction.
     */
    protected void matchYFraction()
    {
        edges.setYFrac( yFractionModel.getNumber().doubleValue() );
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
    public void reset()
    {
        edges.setDefaults();
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
        return edges;
    }

    /**
     * Return the class of object that we expect as our model.
     */
    public static Class getControlsModelClass()
    {
        return GraphicsEdges.class;
    }

//
// Implement the ChangeListener interface
//
    /**
     * If the GraphicsEdges object changes then we need to update the
     * interface.
     */
    public void stateChanged( ChangeEvent e )
    {
        updateFromGraphicsEdges();
    }
}
