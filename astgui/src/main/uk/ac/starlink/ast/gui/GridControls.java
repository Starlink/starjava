/*
 * Copyright (C) 2000-2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     13-NOV-2000 (Peter W. Draper):
 *        Original version.
 *     18-FEB-2004 (Peter W. Draper):
 *        Added GridBagLayouter.
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

import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * GridControls creates a "page" of widgets that are a view of an
 * AstGrid object. They provide the ability to configure all the
 * properties of the AstGrid object.
 *
 * @author Peter W. Draper
 * @version $Id$
 *
 * @see AstGrid
 * @see PlotConfigurator
 */
public class GridControls extends JPanel 
    implements PlotControls, ChangeListener
{
    /**
     * AstGrid model for current state.
     */
    protected AstGrid astGrid = null;

    /**
     * Whether the grid is to be shown.
     */
    protected JCheckBox show = new JCheckBox();

    /**
     * Line properties controls.
     */
    protected LineControls lineControls = null;

    /**
     * Whether to inhibit change events from LineControls from recyling.
     */
    protected boolean inhibitLineChangeListener = false;

    /**
     * The default title for these controls.
     */
    protected static String defaultTitle = "Grid line properties:";

    /**
     * The default short name for these controls.
     */
    protected static String defaultName = "GridLines";

    /**
     * Create an instance.
     */
    public GridControls( AbstractPlotControlsModel astGrid )
    {
        initUI();
        setAstGrid( (AstGrid) astGrid );
    }

    /**
     * Create and initialise the user interface.
     */
    protected void initUI()
    {
        //  Whether grid is shown or not.
        show.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchShow();
                }
            });

        //  Add components.
        GridBagLayouter layouter = Utilities.getGridBagLayouter( this );

        layouter.add( "Show:", false );
        layouter.add( show, true );

        addLineControls( layouter );

        layouter.eatSpare();

        //  Set tooltips.
        show.setToolTipText( "Display grid lines in plot" );
    }

    /**
     * Set the AstGrid object (only after UI is initiliased).
     */
    public void setAstGrid( AstGrid astGrid )
    {
        this.astGrid = astGrid;
        astGrid.addChangeListener( this );
        updateFromAstGrid();
    }

    /**
     * Update interface to reflect values of AstGrid object.
     */
    protected void updateFromAstGrid()
    {
        astGrid.removeChangeListener( this );

        show.setSelected( astGrid.getShown() );

        inhibitLineChangeListener = true;
        lineControls.setThick( (int) astGrid.getWidth() );
        lineControls.setColour( astGrid.getColour() );
        lineControls.setStyle( (int) astGrid.getStyle() );
        inhibitLineChangeListener = false;

        astGrid.setState( true );

        astGrid.addChangeListener( this );

    }

    /**
     * Get copy of reference to current AstGrid.
     */
    public AstGrid getAstGrid()
    {
        return astGrid;
    }

    /**
     * Match whether to display the grid.
     */
    protected void matchShow()
    {
        astGrid.setShown( show.isSelected() );
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
     * Match line properties.
     */
    protected void matchLine()
    {
        if ( ! inhibitLineChangeListener ) {

            //  Update AstGrid object to match properties. Take care
            //  as modifying this fires a ChangeEvent that attempts to
            //  synchronize the lineControls (which are actually
            //  storing the state for now).
            astGrid.removeChangeListener( this );

            astGrid.setWidth( lineControls.getThick() );
            astGrid.setStyle( lineControls.getStyle() );
            astGrid.setColour( lineControls.getColour() );

            astGrid.addChangeListener( this );
            updateFromAstGrid();
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
        astGrid.setDefaults();
        lineControls.reset();
        updateFromAstGrid();
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
        return astGrid;
    }

    /**
     * Return the class of object that we expect as our model.
     */
    public static Class getControlsModelClass()
    {
        return AstGrid.class;
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
        updateFromAstGrid();
    }
}
