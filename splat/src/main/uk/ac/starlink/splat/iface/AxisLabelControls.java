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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import uk.ac.starlink.splat.ast.AstAxisLabels;

/**
 * AxisLabelControls creates a "page" of widgets that are a view of an
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
 * @see AstAxisLabel, PlotConfigFrame.
 */
public class AxisLabelControls extends JPanel implements ChangeListener
{
    /**
     * AstAxisLabel model for current state.
     */
    protected AstAxisLabels astAxisLabels = null;

    /**
     * Control for toggling display of X label.
     */
    protected JCheckBox xShowLabel = new JCheckBox();

    /**
     * Control for toggling display of Y label.
     */
    protected JCheckBox yShowLabel = new JCheckBox();

    /**
     * The X label text field (this also allows access to special
     * characters that cannot be easily typed in).
     */
    protected SelectTextField xTextField = new SelectTextField();

    /**
     * The Y label text field (this also allows access to special
     * characters that cannot be easily typed in).
     */
    protected SelectTextField yTextField = new SelectTextField();

    /**
     * GridBagConstraints object.
     */
    protected GridBagConstraints gbc = new GridBagConstraints();

    /**
     * Label Insets.
     */
    protected Insets labelInsets = new Insets( 10, 5, 5, 10 );

    /**
     * Slider for controlling the position of the X label.
     */
    protected FloatJSlider xGapSlider = null;

    /**
     * Slider for controlling the position of the Y label.
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
     * Chooser for X label edge (TOP or BOTTOM).
     */
    protected JComboBox xEdge = new JComboBox();

    /**
     * Chooser for Y label edge (LEFT or RIGHT).
     */
    protected JComboBox yEdge = new JComboBox();

    /**
     * Chooser for whether X label has any units shown.
     */
    protected JCheckBox xUnits = new JCheckBox();

    /**
     * Chooser for whether Y label has any units shown.
     */
    protected JCheckBox yUnits = new JCheckBox();

    /**
     * Stop feedback to the text areas from themselves.
     */
    protected boolean inhibitXDocumentListener = false;
    protected boolean inhibitYDocumentListener = false;

    /**
     * Create an instance.
     */
    public AxisLabelControls( AstAxisLabels astAxisLabels ) 
    {
        initUI();
        setAstAxisLabel( astAxisLabels );
    }

    /**
     * Reset interface to defaults.
     */
    public void reset() 
    {
        astAxisLabels.setDefaults();
        fontControls.setDefaults();
        setTextColour( Color.black );
        astAxisLabels.addChangeListener( this );
        updateFromAstAxisLabels();
    }

    /**
     * Create and initialise the user interface.
     */
    protected void initUI() 
    {
        setLayout( new GridBagLayout() );

        //  Choices for label positioning.
        xEdge.addItem( "BOTTOM" );
        xEdge.addItem( "TOP" );
        yEdge.addItem( "LEFT" );
        yEdge.addItem( "RIGHT" );

        //  Act on request to display/remove a label.
        xShowLabel.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchXShown();
                }
            });
        yShowLabel.addActionListener( new ActionListener() {
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

        //  New labels are copied to AstAxisLabel.... Need to intercept
        //  keystrokes.
        Document doc = xTextField.getDocument();
        doc.addDocumentListener ( new DocumentListener() {
                public void changedUpdate( DocumentEvent e ) {
                    matchXText();
                }
                public void insertUpdate( DocumentEvent e ) {
                    matchXText();
                }
                public void removeUpdate( DocumentEvent e ) {
                    matchXText();
                }
            });
        doc = yTextField.getDocument();
        doc.addDocumentListener ( new DocumentListener() {
                public void changedUpdate( DocumentEvent e ) {
                    matchYText();
                }
                public void insertUpdate( DocumentEvent e ) {
                    matchYText();
                }
                public void removeUpdate( DocumentEvent e ) {
                    matchYText();
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

        //  Edge selection.
        xEdge.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchXEdge();
                }
            });
        yEdge.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchYEdge();
                }
            });

        //  Units display.
        xUnits.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchXUnits();
                }
            });
        yUnits.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchYUnits();
                }
            });

        //  Add labels for all fields.
        addLabel( "Show X:", 0 );
        addLabel( "X Label:", 1 );
        addLabel( "Show Y:", 2 );
        addLabel( "Y Label:", 3 );
        addLabel( "Font:", 4 );
        addLabel( "Style:", 5 );
        addLabel( "Size:", 6 );
        addLabel( "Colour:", 7 );
        addLabel( "X Gap:", 8 );
        addLabel( "Y Gap:", 9 );
        addLabel( "X Edge:", 10 );
        addLabel( "Y Edge:", 11 );
        addLabel( "X Units:", 12 );
        addLabel( "Y Units:", 13 );

        gbc.insets = new Insets( 0, 0, 0, 0 );
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weighty = 0.0;
        gbc.weightx = 1.0;
        gbc.gridx = 1;

        //  Current row for adding components.
        int row = 0;

        //  X label controls.
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( xShowLabel, gbc );
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.BOTH;
        add( xTextField, gbc );

        //  Y label controls.
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( yShowLabel, gbc );
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.BOTH;
        add( yTextField, gbc );

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

        //  Sides for the labels.
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy = row++;
        add( xEdge, gbc );
        gbc.gridy = row++;
        add( yEdge, gbc );

        //  Whether to append units (if any).
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy = row++;
        add( xUnits, gbc );
        gbc.gridy = row++;
        add( yUnits, gbc );

        //  Eat up all spare vertical space (pushes widgets to top).
        Component filly = Box.createVerticalStrut( 5 );
        gbc.gridy = row++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        add( filly, gbc );

        //  Set tooltips.
        colourButton.setToolTipText( "Select a colour" );
        xTextField.setToolTipText( "Type in the X axis label" );
        yTextField.setToolTipText( "Type in the Y axis label" );
        xGapSlider.setToolTipText( "Set the gap between label and axis" );
        yGapSlider.setToolTipText( "Set the gap between label and axis" );
        xEdge.setToolTipText( "Set edge for displaying label" );
        yEdge.setToolTipText( "Set edge for displaying label" );
        xUnits.setToolTipText( "Append any X axis units to label" );
        xUnits.setToolTipText( "Append any Y axis units to label" );
    }

    /**
     * Set the AstAxisLabel object.
     */
    public void setAstAxisLabel( AstAxisLabels astAxisLabels ) 
    {
        this.astAxisLabels = astAxisLabels;
        updateFromAstAxisLabels();
    }

    /**
     * Update interface to reflect values of the current AstAxisLabel.
     */
    protected void updateFromAstAxisLabels() 
    {
        //  Nothing in this method should change astAxisLabels, but
        //  we'll switch off the ChangeListener anyway.
        astAxisLabels.removeChangeListener( this );

        xShowLabel.setSelected( astAxisLabels.getXShown() );
        if ( ! inhibitXDocumentListener ) {
            xTextField.setText( astAxisLabels.getXLabel() );
        }
        
        yShowLabel.setSelected( astAxisLabels.getYShown() );
        if ( ! inhibitYDocumentListener ) {
            yTextField.setText( astAxisLabels.getYLabel() );
        }

        xTextField.setTextFont( astAxisLabels.getFont() );
        yTextField.setTextFont( astAxisLabels.getFont() );
        fontControls.setFont( astAxisLabels.getFont() );
        xTextField.setTextColour( astAxisLabels.getColour() );
        yTextField.setTextColour( astAxisLabels.getColour() );
        colourIcon.setMainColour( astAxisLabels.getColour() );
        colourButton.repaint();

        xSliderModel.setApparentValues( astAxisLabels.getXGap(),
                                        AstAxisLabels.GAP_MIN,
                                        AstAxisLabels.GAP_MAX,
                                        AstAxisLabels.GAP_STEP );
        ySliderModel.setApparentValues( astAxisLabels.getYGap(),
                                        AstAxisLabels.GAP_MIN,
                                        AstAxisLabels.GAP_MAX,
                                        AstAxisLabels.GAP_STEP );

        if ( astAxisLabels.getXEdge() == AstAxisLabels.BOTTOM ) {
            xEdge.setSelectedItem( "BOTTOM" );
        } else {
            xEdge.setSelectedItem( "TOP" );
        }
        if ( astAxisLabels.getYEdge() == AstAxisLabels.LEFT ) {
            yEdge.setSelectedItem( "LEFT" );
        } else {
            yEdge.setSelectedItem( "RIGHT" );
        }

        xUnits.setSelected( astAxisLabels.getShowXUnits() );
        yUnits.setSelected( astAxisLabels.getShowYUnits() );

        astAxisLabels.addChangeListener( this );
    }

    /**
     * Get copy of reference to current AstAxisLabel.
     */
    public AstAxisLabels getAstAxisLabels() 
    {
        return astAxisLabels;
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
     * Set the X label.
     */
    public void setXText( String text ) 
    {
        xTextField.setText( text );
    }

    /**
     * Set the Y label.
     */
    public void setYText( String text ) 
    {
        yTextField.setText( text );
    }

    /**
     * Match show state of X label to that selected.
     */
    protected void matchXShown() 
    {
        astAxisLabels.setXShown( xShowLabel.isSelected() );
    }

    /**
     * Match the X AstAxisLabels text to that displayed.
     */
    protected void matchXText() 
    {
        inhibitXDocumentListener = true;
        astAxisLabels.setXLabel( xTextField.getText() );
        inhibitXDocumentListener = false;
    }

    /**
     * Match show state of Y label to that selected.
     */
    protected void matchYShown() 
    {
        astAxisLabels.setYShown( yShowLabel.isSelected() );
    }

    /**
     * Match the Y AstAxisLabels text to that displayed.
     */
    protected void matchYText() 
    {
        inhibitYDocumentListener = true;
        astAxisLabels.setYLabel( yTextField.getText() );
        inhibitYDocumentListener = false;
    }

    /**
     * Match the X AstAxisLabels gap to that shown.
     */
    protected void matchXGap() 
    {
        astAxisLabels.setXGap( xGapSlider.getValue() );
    }

    /**
     * Match the Y AstAxisLabels gap to that shown.
     */
    protected void matchYGap() 
    {
        astAxisLabels.setYGap( yGapSlider.getValue() );
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
        if ( font != null ) {
            xTextField.setTextFont( font );
            yTextField.setTextFont( font );
            astAxisLabels.setFont( font );
        }
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
            xTextField.setTextColour( colour );
            yTextField.setTextColour( colour );
            colourIcon.setMainColour( colour );
            colourButton.repaint();
            astAxisLabels.setColour( colour );
        }
    }

    /**
     * Match the X edge setting to the current value.
     */
    protected void matchXEdge() 
    {
        String edge = (String) xEdge.getSelectedItem();
        if ( edge != null ) {
            if ( edge == "BOTTOM" ) {
                astAxisLabels.setXEdge( AstAxisLabels.BOTTOM );
            } else {
                astAxisLabels.setXEdge( AstAxisLabels.TOP );
            }
        }
    }

    /**
     * Match the Y edge setting to the current value.
     */
    protected void matchYEdge() 
    {
        String edge = (String) yEdge.getSelectedItem();
        if ( edge != null ) {
            if ( edge == "LEFT" ) {
                astAxisLabels.setYEdge( AstAxisLabels.LEFT );
            } else {
                astAxisLabels.setYEdge( AstAxisLabels.RIGHT );
            }
        }
    }

    /**
     * Match whether to display the X units (if any).
     */
    protected void matchXUnits() 
    {
        astAxisLabels.setShowXUnits( xUnits.isSelected() );
    }

    /**
     * Match whether to display the Y units (if any).
     */
    protected void matchYUnits() 
    {
        astAxisLabels.setShowYUnits( yUnits.isSelected() );
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
        updateFromAstAxisLabels();
    }

}
