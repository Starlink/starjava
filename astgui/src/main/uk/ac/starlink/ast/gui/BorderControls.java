/*
 * Copyright (C) 2000-2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     13-NOV-2000 (Peter W. Draper):
 *        Original version.
 *     18-FEB-2004 (Peter W. Draper):
 *        Changed to use GridBagLayouter.
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
 * BorderControls creates a "page" of widgets that are a view of an
 * AstBorder object. They provide the ability to configure all the
 * properties of the AstBorder object.
 *
 * @author Peter W. Draper
 * @version $Id$
 *
 * @see AstBorder
 * @see PlotConfigurator
 */
public class BorderControls extends JPanel 
    implements PlotControls, ChangeListener
{
    /**
     * AstBorder model for current state.
     */
    protected AstBorder astBorder = null;

    /**
     * Whether the border is to be shown.
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
    protected static String defaultTitle = "Border line properties:";

    /**
     * The default short name for these controls.
     */
    protected static String defaultName = "Border lines";

    /**
     * Create an instance.
     */
    public BorderControls( AbstractPlotControlsModel astBorder ) 
    {
        initUI();
        setAstBorder( (AstBorder) astBorder );
    }

    /**
     * Create and initialise the user interface.
     */
    protected void initUI() 
    {
        //  Whether border is shown or not.
        show.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchShow();
                }
            });

        //  Add components.
        GridBagLayouter layouter =  Utilities.getGridBagLayouter( this );

        layouter.add( "Show:", false );
        layouter.add( show, true );

        addLineControls( layouter );

        layouter.eatSpare();

        //  Set tooltips.
        show.setToolTipText( "Display border lines in plot" );
    }

    /**
     * Set the AstBorder object (only after UI is initiliased).
     */
    public void setAstBorder( AstBorder astBorder ) 
    {
        this.astBorder = astBorder;
        astBorder.addChangeListener( this );
        updateFromAstBorder();
    }

    /**
     * Update interface to reflect values of AstBorder object.
     */
    protected void updateFromAstBorder() 
    {
        astBorder.removeChangeListener( this );

        show.setSelected( astBorder.getShown() );
        
        inhibitLineChangeListener = true;
        lineControls.setThick( (int) astBorder.getWidth() );
        lineControls.setColour( astBorder.getColour() );
        lineControls.setStyle( (int) astBorder.getStyle() );
        inhibitLineChangeListener = false;

        astBorder.setState( true );

        astBorder.addChangeListener( this );
    }

    /**
     * Get copy of reference to current AstBorder.
     */
    public AstBorder getAstBorder() 
    {
        return astBorder;
    }

    /**
     * Match whether to display the border.
     */
    protected void matchShow() 
    {
        astBorder.setShown( show.isSelected() );
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
            //  Update AstBorder object to match properties. Take care
            //  as modifying this fires a ChangeEvent that attempts to
            //  synchronize the lineControls (which are actually
            //  storing the state for now).
            astBorder.removeChangeListener( this );
            
            astBorder.setWidth( lineControls.getThick() );
            astBorder.setStyle( lineControls.getStyle() );
            astBorder.setColour( lineControls.getColour() );
            
            astBorder.addChangeListener( this );
            updateFromAstBorder();
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
        lineControls.reset();
        astBorder.setDefaults();
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
        return astBorder;
    }

    /**
     * Return the class of object that we expect as our model.
     */
    public static Class getControlsModelClass()
    {
        return AstBorder.class;
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
        updateFromAstBorder();
    }
}
