/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     31-JUL-2003 (Peter W. Draper):
 *       Original version.
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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * StringsControls creates a "page" of widgets that are a view of an
 * AstStrings object. They provide the ability to configure all the
 * properties of the AstStrings object (that describe how the any
 * strings drawn in an AST plot should be rendered) and show a current
 * rendering of what the text would look like.
 *
 * @author Peter W. Draper
 * @version $Id$
 *
 * @see AstStrings
 * @see PlotConfigurator
 */
public class StringsControls extends JPanel
    implements PlotControls, ChangeListener
{
    /**
     * AstStrings model for current state.
     */
    protected AstStrings astStrings = null;

    /**
     * The sample text field.
     */
    protected JLabel sampleText = new JLabel( "The quick brown fox" );

    /**
     * Colour button.
     */
    protected JButton colourButton = new JButton();

    /**
     * Colour Icon of colour button.
     */
    protected ColourIcon colourIcon = new ColourIcon( Color.black );

    /**
     * FontControls.
     */
    protected FontControls fontControls = null;

    /**
     * The default title for these controls.
     */
    protected static String defaultTitle = "General text properties:";

    /**
     * The default short name for these controls.
     */
    protected static String defaultName = "Text";

    /**
     * Create an instance.
     */
    public StringsControls( AbstractPlotControlsModel astStrings )
    {
        initUI();
        setAstStrings( (AstStrings) astStrings );
    }

    /**
     * Create and initialise the user interface.
     */
    protected void initUI()
    {
        //  Set ColourIcon of colour button.
        colourButton.setIcon( colourIcon );
        colourButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                chooseColour();
            }
        });

        //  Add components.
        GridBagLayouter layouter =  Utilities.getGridBagLayouter( this );

        layouter.add( "Sample:", false );
        layouter.add( sampleText, true );

        addFontControls( layouter );

        layouter.add( "Colour:", false );
        layouter.add( colourButton, false );
        layouter.eatLine();

        layouter.eatSpare();

        //  Set tooltips.
        colourButton.setToolTipText( "Select a colour" );
    }

    /**
     * Set the AstStrings object.
     */
    public void setAstStrings( AstStrings astStrings )
    {
        this.astStrings = astStrings;
        astStrings.addChangeListener( this );
        updateFromAstStrings();
    }

    /**
     * Update interface to reflect values of the current AstStrings.
     */
    protected void updateFromAstStrings()
    {
        astStrings.removeChangeListener( this );
        sampleText.setFont( astStrings.getFont() );
        fontControls.setFont( astStrings.getFont() );
        sampleText.setForeground( astStrings.getColour() );
        colourIcon.setMainColour( astStrings.getColour() );
        colourButton.repaint();
        astStrings.addChangeListener( this );
    }

    /**
     * Get copy of reference to current AstStrings.
     */
    public AstStrings getAstStrings()
    {
        return astStrings;
    }

    /**
     * Add the font controls.
     */
    private void addFontControls( GridBagLayouter layouter )
    {
        fontControls = new FontControls( layouter, "" );
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
     * Set the text font.
     */
    protected void setTextFont( Font font )
    {
        astStrings.setFont( font );
    }

    /**
     * Choose a text colour.
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
            astStrings.setColour( colour );
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
     * Reset controls to the defaults.
     */
    public void reset()
    {
        fontControls.setDefaults();
        setTextColour( Color.black );
        astStrings.setDefaults();
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
        return astStrings;
    }

    /**
     * Return the class of object that we expect as our model.
     */
    public static Class getControlsModelClass()
    {
        return AstStrings.class;
    }


//
// Implement the ChangeListener interface
//
    /**
     * If the AstStrings object changes then we need to update the
     * interface.
     */
    public void stateChanged( ChangeEvent e )
    {
        updateFromAstStrings();
    }
}
