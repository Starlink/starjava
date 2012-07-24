/*
 * Copyright (C) 2001-2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     14-FEB-2001 (Peter W. Draper):
 *       Original version.
 */      
package uk.ac.starlink.ast.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * Create controls for setting the colour of a component. The actual
 * setting is performed by an object that implements the 
 * {@link PlotController} interface.
 * <p>
 * The Color is stored in a ColourStore object that provides
 * persistence and maintains a list of listeners for changes in the
 * colour.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class ComponentColourControls
    extends JPanel
    implements PlotControls, ChangeListener
{
    /**
     * The controller of the component that we're controlling.
     */
    protected PlotController controller = null;

    /**
     * Colour Icon of colour button, shows the current colour.
     */
    protected ColourIcon colourIcon = new ColourIcon( Color.black );

    /**
     * The button used to request the colour dialog.
     */
    protected JButton colourButton = new JButton();

    /**
     * Label showing the description of the colour that will be
     * changed.
     */
    protected JLabel colourLabel = new JLabel();

    /**
     * Description of the colour that will be changed.
     */
    protected String description = null;

    /**
     * Title for a page of these controls.
     */
    protected String title = "Colour controls:";

    /**
     * Short name of these controls (for a tabbed label).
     */
    protected String name = "Colour";

    /**
     * ColourStore that provides change notifications and permanent
     * store.
     */
    protected ColourStore colourStore = null;

    /**
     * The original colour of the component. Kept for resetting.
     * Will be initialised when the PlotController is set.
     */
    protected Color originalColour = Color.black;

    /**
     * Create an instance. Requires a PlotController to send change
     * requests to and a short description of the property
     * (this is shown in a label).
     *
     * @param controller a controller for the object that is to be
     *                   coloured. 
     * @param colourStore the storage for the actual Colour.
     * @param description a short description of the property.
     */
    public ComponentColourControls( PlotController controller,
                                    AbstractPlotControlsModel colourStore,
                                    String title,
                                    String name,
                                    String label )
        throws RuntimeException
    {
        initUI();
        setController( controller );
        setColourStore( (ColourStore) colourStore );
        setDescription( title, name, label );
    }


    /**
     * Place the UI components.
     */
    protected void initUI()
    {
        colourButton.setIcon( colourIcon );
        colourButton.setToolTipText( "Press to select a new colour" );
        colourButton.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    chooseColour();
                }
            });

        GridBagLayouter layouter =  Utilities.getGridBagLayouter( this );
        layouter.add( colourLabel, false );
        layouter.add( colourButton, false );
        layouter.eatLine();
        layouter.eatSpare();
    }

    /**
     * Set the controller of the component to be coloured.
     */
    public void setController( PlotController controller )
    {
        this.controller = controller;
        originalColour = controller.getPlotColour();
    }

    /**
     * Set the ColourStore used to store the colour.
     */
    public void setColourStore( ColourStore colourStore )
    {
        this.colourStore = colourStore;
        colourStore.addChangeListener( this );
    }

    /**
     * Get the ColourStore.
     */
    public ColourStore getColourStore()
    {
        return this.colourStore;
    }

    /**
     * Get the controller for the applying the colour changes.
     */
    public PlotController getPlotController()
    {
        return controller;
    }

    /**
     * Set the descriptions, title, name and label.
     */
    public void setDescription( String title, String name, String label )
    {
        this.description = label;
        this.title = title;
        this.name = name;
        colourLabel.setText( label );
    }

    /**
     * Get the description used in the label.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Choose a colour for the component using a chooser dialog.
     */
    protected void chooseColour()
    {
        Color[] newColour = new Color[1];
        newColour[0] = JColorChooser.showDialog( this, "Select Colour",
                                                 colourIcon.getMainColour() );
        if ( newColour != null ) {
            colourStore.setColor( newColour[0] );
        }
    }

    /**
     * Update the component colour to match the one in store.
     */
    public void matchColour()
    {
        try {
            Color colour = colourStore.getColour();
            controller.setPlotColour( colour );
            colourIcon.setMainColour( colour );
        }
        catch (Exception e) {
            // Do nothing.
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
        return title;
    }

    /**
     * Return a short name for these controls (for the tab).
     */
    public String getControlsName()
    {
        return name;
    }

    /**
     * Reset the colour to original.
     */
    public void reset()
    {
        try {
            colourStore.setColor( originalColour );
        }
        catch (Exception e) {
            //  Tough, do nothing.
        }
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
        return colourStore;
    }

    /**
     * Return the class of object that we expect as our model.
     */
    public static Class getControlsModelClass()
    {
        return ColourStore.class;
    }

//
// ChangeListener interface.
//
    /**
     * Listen for ColourStore issuing ChangeEvents.
     */
    public void stateChanged( ChangeEvent e )
    {
        matchColour();
    }
}
