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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.splat.ast.AstNumberLabels;

/**
 * AxisNumLabControls.Java creates a "page" of widgets that are a view of an
 * AstAxisLabel object. They provide the ability to configure all the
 * properties of the AstAxisLabel object (that describe how the axis
 * labels of an AST plot should be drawn) and show a current rendering
 * of them.
 *
 * @since $Date$
 * @since 10-NOV-2000
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 * @see AstNumerLabels, PlotConfigFrame.
 */
public class AxisNumLabControls extends JPanel implements ChangeListener
{
    /**
     * AstAxisLabel model for current state.
     */
    protected AstNumberLabels astNumberLabels = null;

    /**
     * Label showing current font.
     */
    protected JLabel display = new JLabel( "123456789.0E1" );

    /**
     * GridBagConstraints object.
     */
    protected GridBagConstraints gbc = new GridBagConstraints();

    /**
     * Label Insets.
     */
    protected Insets labelInsets = new Insets( 10, 5, 5, 10 );

    /**
     * Control for toggling display of X numbers.
     */
    protected JCheckBox xShowNumbers = new JCheckBox();

    /**
     * Control for toggling display of Y numbers.
     */
    protected JCheckBox yShowNumbers = new JCheckBox();

    /**
     * Slider for controlling the position of the X numbers.
     */
    protected FloatJSlider xGapSlider = null;

    /**
     * Slider for controlling the position of the Y numbers.
     */
    protected FloatJSlider yGapSlider = null;

   /**
     * X Slider model.
     */
    protected FloatJSliderModel xSliderModel = new FloatJSliderModel();

   /**
     * Y Slider model.
     */
    protected FloatJSliderModel ySliderModel = new FloatJSliderModel();

    /**
     * Colour button (same for both labels).
     */
    protected JButton colourButton = new JButton();

    /**
     * Colour Icon of colour button.
     */
    protected ColourIcon colourIcon = new ColourIcon( Color.black );

    /**
     * FontControls (same for both labels).
     */
    protected FontControls fontControls = null;

    /**
     * Number of digits used in label precision.
     */
    protected JComboBox digitsField = new JComboBox();

    /**
     * Create an instance.
     */
    public AxisNumLabControls( AstNumberLabels astNumberLabels )
    {
        initUI();
        setAstNumberLabels( astNumberLabels );
    }

    /**
     * Reset interface to defaults.
     */
    public void reset()
    {
        astNumberLabels.setDefaults();
        fontControls.setDefaults();
        setTextColour( Color.black );
        updateFromAstNumberLabels();
    }

    /**
     * Create and initialise the user interface.
     */
    protected void initUI()
    {
        setLayout( new GridBagLayout() );

        //  Act on request to display/remove a label.
        xShowNumbers.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchXShown();
                }
            });
        yShowNumbers.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchYShown();
                }
            });

        //  Set ColourIcon of colour button.
        colourButton.setIcon( colourIcon );
        colourButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                chooseColour();
            }
        });

        //  New gaps.
        xGapSlider = new FloatJSlider( xSliderModel );
        xGapSlider.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchXGap();
                }
            });

        yGapSlider = new FloatJSlider( ySliderModel );
        yGapSlider.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchYGap();
                }
            });

        //  Number of digits used in label precision.
        digitsField.addItem( "Default" );
        for ( int i = 0; i < 18; i++ ) {
            digitsField.addItem( new Integer( i ) );
        }
        digitsField.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchDigits();
                }
            });


        //  Add labels for all fields.
        addLabel( "Sample:", 0 );
        addLabel( "Show X:", 1 );
        addLabel( "Show Y:", 2 );
        addLabel( "Font:", 3 );
        addLabel( "Style:", 4 );
        addLabel( "Size:", 5 );
        addLabel( "Colour:", 6 );
        addLabel( "X Gap:", 7 );
        addLabel( "Y Gap:", 8 );
        addLabel( "Digits:", 9 );

        gbc.insets = new Insets( 0, 0, 0, 0 );
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weighty = 0.0;
        gbc.weightx = 1.0;
        gbc.gridx = 1;

        //  Current row for adding components.
        int row = 0;

        //  Add the sample display.
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( display, gbc );

        //  Add show checkboxes.
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( xShowNumbers, gbc );
        gbc.gridy = row++;
        add( yShowNumbers, gbc );


        //  Font family selection.
        row = addFontControls( row );

        //  Colour selector.
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( colourButton, gbc );

        //  Gap sliders.
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = row++;
        add( xGapSlider, gbc );
        gbc.gridy = row++;
        add( yGapSlider, gbc );

        //  Digits field.
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy = row++;
        add( digitsField, gbc );

        //  Eat up all spare vertical space (pushes widgets to top).
        Component filly = Box.createVerticalStrut( 5 );
        gbc.gridy = row++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        add( filly, gbc );

        //  Set tooltips.
        colourButton.setToolTipText( "Select a colour" );
        xGapSlider.setToolTipText( "Set the gap between numbers and axis" );
        yGapSlider.setToolTipText( "Set the gap between numbers and axis" );
        digitsField.setToolTipText(
            "Digits of precision used in formatted numbers" );
    }

    /**
     * Set the AstNumberLabels object.
     */
    public void setAstNumberLabels( AstNumberLabels astNumberLabels )
    {
        this.astNumberLabels = astNumberLabels;
        astNumberLabels.addChangeListener( this );
        updateFromAstNumberLabels();
    }

    /**
     * Update interface to reflect values of the current AstAxisLabel.
     */
    protected void updateFromAstNumberLabels()
    {
        astNumberLabels.removeChangeListener( this );

        xShowNumbers.setSelected( astNumberLabels.getXShown() );
        yShowNumbers.setSelected( astNumberLabels.getYShown() );

        display.setFont( astNumberLabels.getFont() );
        fontControls.setFont( astNumberLabels.getFont() );
        
        colourIcon.setMainColour( astNumberLabels.getColour() );
        colourButton.repaint();
        display.setForeground( astNumberLabels.getColour() );

        xSliderModel.setApparentValues( astNumberLabels.getXGap(),
                                        AstNumberLabels.GAP_MIN,
                                        AstNumberLabels.GAP_MAX,
                                        AstNumberLabels.GAP_STEP );
        ySliderModel.setApparentValues( astNumberLabels.getYGap(),
                                        AstNumberLabels.GAP_MIN,
                                        AstNumberLabels.GAP_MAX,
                                        AstNumberLabels.GAP_STEP );

        int digits = astNumberLabels.getDigits();
        if ( digits != -1 ) {
            digitsField.setSelectedItem( new Integer( digits ) );
        } else {
            digitsField.setSelectedItem( "Default" );
        }

        astNumberLabels.addChangeListener( this );
    }

    /**
     * Get copy of reference to current AstNumberLabels.
     */
    public AstNumberLabels getAstNumberLabels()
    {
        return astNumberLabels;
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
     * Update the displayed font.
     */
    protected void updateFont( FontChangedEvent e )
    {
        setTextFont( e.getFont() );
    }

    /**
     * Set the font.
     */
    protected void setTextFont( Font font )
    {
        if ( font != null ) {
            astNumberLabels.setFont( font );
            display.setFont( font );
        }
    }

    /**
     * Match shown state of X numbers to that selected.
     */
    protected void matchXShown()
    {
        astNumberLabels.setXShown( xShowNumbers.isSelected() );
    }

    /**
     * Match shown state of Y numbers to that selected.
     */
    protected void matchYShown()
    {
        astNumberLabels.setYShown( yShowNumbers.isSelected() );
    }

    /**
     * Match the X AstAxisLabels gap to that shown.
     */
    protected void matchXGap()
    {
        astNumberLabels.setXGap( xGapSlider.getValue() );
    }

    /**
     * Match the Y AstAxisLabels gap to that shown.
     */
    protected void matchYGap()
    {
        astNumberLabels.setYGap( yGapSlider.getValue() );
    }

    /**
     * Match digits to those selected.
     */
    protected void matchDigits()
    {
        Object object = digitsField.getSelectedItem();
        int value = -1;
        if ( ! object.equals( "Default" ) ) {
            value = ((Integer) object).intValue();
        }
        astNumberLabels.setDigits( value );
    }

    /**
     * Update the text colour.
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
            colourIcon.setMainColour( colour );
            colourButton.repaint();
            astNumberLabels.setColour( colour );
            display.setForeground( colour );
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
        updateFromAstNumberLabels();
    }
}
