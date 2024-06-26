/*
 * Copyright (C) 2000 - 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     08-NOV-2000 (Peter W. Draper):
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import javax.swing.SpinnerNumberModel;

import uk.ac.starlink.util.gui.SelectTextField;
import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * TitleControls creates a "page" of widgets that are a view of an
 * AstTitle object. They provide the ability to configure all the
 * properties of the AstTitle object (that describe how the title of
 * an AST plot should be drawn) and show a current rendering of the
 * title.
 *
 * @author Peter W. Draper
 * @version $Id$
 *
 * @see AstTitle
 * @see PlotConfigurator
 */
public class TitleControls extends JPanel
    implements PlotControls, ChangeListener, DocumentListener
{
    /**
     * AstTitle model for current state.
     */
    protected AstTitle astTitle = null;

    /**
     * Whether to show the title or not.
     */
    protected JCheckBox showTitle = new JCheckBox();

    /**
     * The title text field (this also allows access to special
     * characters that cannot be easily typed in).
     */
    protected SelectTextField textField = new SelectTextField();

    /**
     * Spinner for controlling title gap.
     */
    protected ScientificSpinner gapSpinner = null;

    /**
     * Spinner model.
     */
    protected SpinnerNumberModel spinnerModel =
        new SpinnerNumberModel( 0.0, AstTitle.GAP_MIN, AstTitle.GAP_MAX,
                                AstTitle.GAP_STEP );

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
     * Whether to inhibit listening for Document events.
     */
    protected boolean inhibitDocumentListener = false;

    /**
     * The default title for these controls.
     */
    protected static String defaultTitle = "Title properties:";

    /**
     * The default short name for these controls.
     */
    protected static String defaultName = "Title";

    /**
     * Create an instance.
     */
    public TitleControls( AbstractPlotControlsModel astTitle )
    {
        initUI();
        setAstTitle( (AstTitle) astTitle );
    }

    /**
     * Create and initialise the user interface.
     */
    protected void initUI()
    {
        //  Toggle whether title is to be shown or not.
        showTitle.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchShow();
                }
            });

        //  Set ColourIcon of colour button.
        colourButton.setIcon( colourIcon );
        colourButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                chooseColour();
            }
        });

        //  New titles are copied to AstTitle.... Need to intercept
        //  keystrokes to do this.
        textField.getDocument().addDocumentListener( this );

        //  New gaps.
        gapSpinner = new ScientificSpinner( spinnerModel );
        gapSpinner.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchGap();
                }
            });

        //  Add components.
        GridBagLayouter layouter =  Utilities.getGridBagLayouter( this );

        layouter.add( "Show:", false );
        layouter.add( showTitle, true );

        layouter.add( "Label:", false );
        layouter.add( textField, true );

        addFontControls( layouter  );

        layouter.add( "Colour:", false );
        layouter.add( colourButton, false );
        layouter.eatLine();

        layouter.add( "Gap:", false );
        layouter.add( gapSpinner, false );
        layouter.eatLine();

        layouter.eatSpare();

        //  Set tooltips.
        colourButton.setToolTipText( "Select a colour" );
        textField.setToolTipText( "Type in the plot title" );
        gapSpinner.setToolTipText( "Set the gap between title and top of plot" );
    }

    /**
     * Set the AstTitle object.
     */
    public void setAstTitle( AstTitle astTitle )
    {
        this.astTitle = astTitle;
        astTitle.addChangeListener( this );
        updateFromAstTitle();
    }

    /**
     * Update interface to reflect values of the current AstTitle.
     */
    protected void updateFromAstTitle()
    {
        astTitle.removeChangeListener( this );
        showTitle.setSelected( astTitle.getShown() );
        if ( ! inhibitDocumentListener ) {
            textField.setText( astTitle.getTitle() );
        }

        textField.setTextFont( astTitle.getFont() );
        fontControls.setFont( astTitle.getFont() );

        textField.setTextColour( astTitle.getColour() );
        colourIcon.setMainColour( astTitle.getColour() );
        colourButton.repaint();

        spinnerModel.setValue( Double.valueOf( astTitle.getGap() ) );

        //astTitle.setState( true );

        astTitle.addChangeListener( this );
    }

    /**
     * Get copy of reference to current AstTitle.
     */
    public AstTitle getAstTitle()
    {
        return astTitle;
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
     * Set the displayed text;
     */
    public void setText( String text )
    {
        astTitle.setTitle( text );
    }

    /**
     * Match whether to display the title.
     */
    protected void matchShow()
    {
        astTitle.setShown( showTitle.isSelected() );
    }

    /**
     * Match the AstTitle gap to that shown.
     */
    protected void matchGap()
    {
        astTitle.setGap( spinnerModel.getNumber().doubleValue() );
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
        astTitle.setFont( font );
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
            astTitle.setColour( colour );
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
        astTitle.setDefaults();
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
        return astTitle;
    }

    /**
     * Return the class of object that we expect as our model.
     */
    public static Class getControlsModelClass()
    {
        return AstTitle.class;
    }


//
// Implement the ChangeListener interface
//
    /**
     * If the AstTitle object changes then we need to update the
     * interface.
     */
    public void stateChanged( ChangeEvent e )
    {
        updateFromAstTitle();
    }

//
// Implement the DocumentListener interface.
//
    public void changedUpdate( DocumentEvent e ) {
        matchText();
    }
    public void insertUpdate( DocumentEvent e ) {
        matchText();
    }
    public void removeUpdate( DocumentEvent e ) {
        matchText();
    }

    /**
     * Match the AstTitle text to that displayed.
     */
    protected void matchText()
    {
        //  This particular method needs to inhibit further
        //  DocumentListener events, otherwise this feeds back...
        inhibitDocumentListener = true;
        astTitle.setTitle( textField.getText() );
        inhibitDocumentListener = false;
    }
}
