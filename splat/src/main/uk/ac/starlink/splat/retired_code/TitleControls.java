package uk.ac.starlink.splat.iface;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import uk.ac.starlink.splat.ast.AstTitle;

/**
 * TitleControls creates a "page" of widgets that are a view of an
 * AstTitle object. They provide the ability to configure all the
 * properties of the AstTitle object (that describe how the title of
 * an AST plot should be drawn) and show a current rendering of the
 * title.
 *
 * @since $Date$
 * @since 08-NOV-2000
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 * @see AstTitle, PlotConfigFrame.
 */
public class TitleControls extends JPanel
    implements ChangeListener, DocumentListener
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
     * GridBagConstraints object.
     */
    protected GridBagConstraints gbc = new GridBagConstraints();

    /**
     * Label Insets.
     */
    protected Insets labelInsets = new Insets( 10, 5, 5, 10 );

    /**
     * Slider for controlling title gap.
     */
    protected FloatJSlider gapSlider = null;

    /**
     * Slider model.
     */
    protected FloatJSliderModel sliderModel = new FloatJSliderModel();

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
     * Create an instance.
     */
    public TitleControls( AstTitle astTitle )
    {
        initUI();
        setAstTitle( astTitle );
    }

    /**
     * Reset controls to defaults.
     */
    public void reset()
    {
        fontControls.setDefaults();
        setTextColour( Color.black );
        astTitle.setDefaults();
    }

    /**
     * Create and initialise the user interface.
     */
    protected void initUI()
    {
        setLayout( new GridBagLayout() );

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
        gapSlider = new FloatJSlider( sliderModel );
        gapSlider.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchGap();
                }
            });

        //  Add labels for all fields.
        addLabel( "Show:", 0 );
        addLabel( "Label:", 1 );
        addLabel( "Font:", 2 );
        addLabel( "Style:", 3 );
        addLabel( "Size:", 4 );
        addLabel( "Colour:", 5 );
        addLabel( "Gap:", 6 );

        gbc.insets = new Insets( 0, 0, 0, 0 );
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weighty = 0.0;
        gbc.weightx = 1.0;
        gbc.gridx = 1;

        //  Current row for adding components.
        int row = 0;

        //  Show field.
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( showTitle, gbc );

        //  Text field.
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.BOTH;
        add( textField, gbc );

        //  Font family selection.
        row = addFontControls( row );

        //  Colour selector.
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( colourButton, gbc );

        //  Gap slider.
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = row++;
        add( gapSlider, gbc );

        //  Eat up all spare vertical space (pushes widgets to top).
        Component filly = Box.createVerticalStrut( 5 );
        gbc.gridy = row++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        add( filly, gbc );

        //  Set tooltips.
        colourButton.setToolTipText( "Select a colour" );
        textField.setToolTipText( "Type in the plot title" );
        gapSlider.setToolTipText( "Set the gap between title and top of plot" );
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

        sliderModel.setApparentValues( astTitle.getGap(),
                                       AstTitle.GAP_MIN,
                                       AstTitle.GAP_MAX,
                                       AstTitle.GAP_STEP );

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
     * Add the font controls.
     */
    private int addFontControls( int row )
    {
        fontControls = new FontControls( this, row, 1 );
        fontControls.addListener( new FontChangedListener() {
            public void fontChanged( FontChangedEvent e ) {
                updateFont( e );
            }
        });
        return row + 3;
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
        astTitle.setGap( gapSlider.getValue() );
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
            this, "Select Text Colour", colourIcon.getMainColour() );
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
