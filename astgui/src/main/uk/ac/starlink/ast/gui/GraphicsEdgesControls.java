/*
 * Copyright (C) 2000-2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     28-NOV-2000 (Peter W. Draper):
 *        Original version.
 *     14-APR-2004 (Peter W. Draper):
 *        Differentiated top/bottom and left/right from X and Y spacing.
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
     * Spinner for controlling the space reserved for X labelling on left.
     */
    protected ScientificSpinner xLeft = null;

    /**
     * Spinner for controlling the space reserved for X labelling on right.
     */
    protected ScientificSpinner xRight = null;

    /**
     * Spinner model for X left fraction.
     */
    protected SpinnerNumberModel xLeftModel =
        new SpinnerNumberModel( 0.0,
                                GraphicsEdges.GAP_MIN,
                                GraphicsEdges.GAP_MAX,
                                GraphicsEdges.GAP_STEP );

    /**
     * Spinner model for X right fraction.
     */
    protected SpinnerNumberModel xRightModel =
        new SpinnerNumberModel( 0.0,
                                GraphicsEdges.GAP_MIN,
                                GraphicsEdges.GAP_MAX,
                                GraphicsEdges.GAP_STEP );

    /**
     * Spinner for controlling the space reserved for Y labelling at the top.
     */
    protected ScientificSpinner yTop = null;

    /**
     * Spinner for controlling the space reserved for Y labelling at the
     * bottom.
     */
    protected ScientificSpinner yBottom = null;

    /**
     * Spinner model for Y top fraction.
     */
    protected SpinnerNumberModel yTopModel =
        new SpinnerNumberModel( 0.0,
                                GraphicsEdges.GAP_MIN,
                                GraphicsEdges.GAP_MAX,
                                GraphicsEdges.GAP_STEP );

    /**
     * Spinner model for Y bottom fraction.
     */
    protected SpinnerNumberModel yBottomModel =
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

        //  Set X fractions.
        xLeft = new ScientificSpinner( xLeftModel );
        xLeft.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchXLeft();
                }
            });

        xRight = new ScientificSpinner( xRightModel );
        xRight.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchXRight();
                }
            });

        //  Set Y fractions.
        yTop = new ScientificSpinner( yTopModel );
        yTop.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchYTop();
                }
            });

        yBottom = new ScientificSpinner( yBottomModel );
        yBottom.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchYBottom();
                }
            });

        //  Add labels for all fields.
        GridBagLayouter layouter =  Utilities.getGridBagLayouter( this );

        layouter.add( "Clip graphics:", false );
        layouter.add( clip, true );

        layouter.add( "X left reserve:", false );
        layouter.add( xLeft, false );
        layouter.eatLine();

        layouter.add( "X right reserve:", false );
        layouter.add( xRight, false );
        layouter.eatLine();

        layouter.add( "Y top reserve:", false );
        layouter.add( yTop, false );
        layouter.eatLine();

        layouter.add( "Y bottom reserve:", false );
        layouter.add( yBottom, false );
        layouter.eatLine();

        layouter.eatSpare();

        //  Set tooltips.
        clip.setToolTipText( "Clip graphics to lie within border" );
        xLeft.setToolTipText
            ( "Set space reserved on the left for X labels (fraction)" );
        xRight.setToolTipText
            ( "Set space reserved on the right for X labels (fraction)" );
        yTop.setToolTipText
            ( "Set space reserved on the top for Y labels (fraction)" );
        yBottom.setToolTipText
            ( "Set space reserved on the bottom for Y labels (fraction)" );
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
        xLeftModel.setValue( Double.valueOf( edges.getXLeft() ) );
        xRightModel.setValue( Double.valueOf( edges.getXRight() ) );
        yTopModel.setValue( Double.valueOf( edges.getYTop() ) );
        yBottomModel.setValue( Double.valueOf( edges.getYBottom() ) );
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
     * Match left X label fraction.
     */
    protected void matchXLeft()
    {
        edges.setXLeft( xLeftModel.getNumber().doubleValue() );
    }

    /**
     * Match right X label fraction.
     */
    protected void matchXRight()
    {
        edges.setXRight( xRightModel.getNumber().doubleValue() );
    }

    /**
     * Match top Y label fraction.
     */
    protected void matchYTop()
    {
        edges.setYTop( yTopModel.getNumber().doubleValue() );
    }

    /**
     * Match bottom Y label fraction.
     */
    protected void matchYBottom()
    {
        edges.setYBottom( yBottomModel.getNumber().doubleValue() );
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
