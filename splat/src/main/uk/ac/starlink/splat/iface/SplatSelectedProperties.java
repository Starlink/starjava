package uk.ac.starlink.splat.iface;

import java.awt.Color;
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
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.ast.gui.AstStyleBox;
import uk.ac.starlink.ast.gui.ColourIcon;
import uk.ac.starlink.ast.gui.DecimalField;

/**
 * SplatSelectedProperties defines objects for viewing the image
 * symbolic and full names, data type, and display properties of
 * a list of selected spectra. When more than one spectrum is selected
 * the current properties shown are those of the first, but
 * modifications of any properties are applied to all spectra.
 *
 * @since $Date$
 * @since 02-OCT-2000
 * @version $Id$
 * @author Peter W. Draper
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 *
 */
public class SplatSelectedProperties extends JPanel
{
    /**
     *  Reference to GlobalSpecPlotList object.
     */
    protected GlobalSpecPlotList globalList =
                                    GlobalSpecPlotList.getReference();

    /**
     *  The JList containing names of all the available spectra.
     */
    protected JList list = null;

    /**
     *  Various components used in the interface.
     */
    protected GridBagLayout mainLayout = new GridBagLayout();
    protected JLabel shortNameLabel = new JLabel();
    protected JLabel fullNameLabel = new JLabel();
    protected JLabel formatLabel = new JLabel();
    protected JLabel errorLabel = new JLabel();
    protected JLabel thicknessLabel = new JLabel();
    protected JLabel lineStyleLabel = new JLabel();
    protected JLabel lineTypeLabel = new JLabel();
    protected JLabel lineColourLabel = new JLabel();
    protected JTextField shortName = new JTextField();
    protected JLabel fullName = new JLabel();
    protected JLabel format = new JLabel();
    protected JCheckBox errors = new JCheckBox();
    protected JComboBox thickness = new JComboBox();
    protected AstStyleBox lineStyle = new AstStyleBox();
    protected PlotStyleBox lineType = new PlotStyleBox();
    protected ColourIcon linesColourIcon = new ColourIcon( Color.blue );
    protected JButton lineColour = new JButton();
    protected ColourIcon errorsColourIcon = new ColourIcon( Color.red );
    protected JButton errorsColour = new JButton();
    protected GridBagConstraints gbc = new GridBagConstraints();
    protected Insets globalInsets = new Insets( 2, 0, 0, 2 );
    protected FloatJSlider alphaSlider = null;
    protected JLabel alphaLabel = new JLabel();

    /**
     * Stop updates of properties from propagating to other listeners
     * and controls.
     */
    protected boolean inhibitChanges = false;

    /**
     *  Creates an instance. Tracking the current spectrum shown in a
     *  JList.
     */
    public SplatSelectedProperties( JList list )
    {
        this.list = list;
        initUI();
    }

    /**
     *  Add all the components for display the spectrum properties.
     */
    protected void initUI()
    {
        setLayout( mainLayout );

        //  Set up the two name display controls. These are different
        //  from others in that they fill all remaining columns,
        //  rather than allowing a strut to take up all the horizontal
        //  space.
        shortNameLabel.setAlignmentY( (float) 0.0 );
        shortNameLabel.setText( "Short Name:" );
        fullNameLabel.setAlignmentY( (float) 0.0 );
        fullNameLabel.setText( "Full Name:" );

        gbc.insets = globalInsets;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridwidth = 1;
        add( shortNameLabel, gbc );

        add( Box.createHorizontalStrut( 5 ) );

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        add( shortName, gbc );
        shortName.setToolTipText
            ( "Symbolic name of spectrum, press return to accept edits" );

        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        add( fullNameLabel, gbc );

        add( Box.createHorizontalStrut( 5 ) );

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        add( fullName, gbc );
        fullName.setToolTipText( "Full name of spectrum (usually filename)" );

        //  The name field can be editted.
        shortName.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    updateShortName();
                }
            });

        //  Set up the spectrum data format control.
        formatLabel.setText( "Format:" );

        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        add( formatLabel, gbc );

        add( Box.createHorizontalStrut( 5 ) );

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        add( format, gbc );
        format.setToolTipText( "Data type used for storage of spectrum" );

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;  //  First strut takes all remaining space,
                            //  other just follow suite.
        add( Box.createHorizontalStrut( 5 ), gbc );
        gbc.weightx = 0.0;

        //  Set up the line colour control.
        lineColourLabel.setText( "Colour:" );

        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        add( lineColourLabel, gbc );

        add( Box.createHorizontalStrut( 5 ) );

        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        add( lineColour, gbc );
        lineColour.setToolTipText( "Choose a colour for spectrum" );
        lineColour.setIcon( linesColourIcon );

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add( Box.createHorizontalStrut( 5 ), gbc );

        lineColour.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    updateLineColour( e );
                }
            });

        //  Alpha blending value. TODO: not sure this is useful, is
        //  very slow.
//         alphaLabel.setText("Alpha Blending:");
//         FloatJSliderModel alphaModel = new FloatJSliderModel
//             ( 1.0, 0.0, 1.0, 0.05 );
//         alphaSlider = new FloatJSlider( alphaModel, false );
        
//         gbc.gridwidth = 1;
//         add( alphaLabel, gbc );
//         add( Box.createHorizontalStrut( 5 ) );
//         add( alphaSlider, gbc );
//         gbc.gridwidth = GridBagConstraints.REMAINDER;
//         add( Box.createHorizontalStrut( 5 ), gbc );
        
//         alphaSlider.addChangeListener( new ChangeListener() {
//                 public void stateChanged( ChangeEvent e ) {
//                     updateAlpha();
//                 }
//             });


        //  Set up the line type control.
        lineTypeLabel.setText( "Line Type:" );

        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        add( lineTypeLabel, gbc );

        add( Box.createHorizontalStrut( 5 ) );

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        add( lineType, gbc );
        lineType.setToolTipText( "Type of line used to show spectrum" );

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add( Box.createHorizontalStrut( 5 ), gbc );

        lineType.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    updatePlotStyle( e );
                }
            });

        //  Set up the line thickness control.
        thicknessLabel.setText( "Thickness:" );

        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        add( thicknessLabel, gbc );

        add( Box.createHorizontalStrut( 5 ) );

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        add( thickness, gbc );
        thickness.setToolTipText( "Thickness of spectrum line" );

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add( Box.createHorizontalStrut( 5 ), gbc );

        for ( int i = 1; i < 20; i++ ) {
            thickness.addItem( new Integer( i ) );
        }
        thickness.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    updateThickness( e );
                }
            });

        //  Set up the line style control.
        lineStyleLabel.setText( "Line Style:" );

        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        add( lineStyleLabel, gbc );

        add( Box.createHorizontalStrut( 5 ) );

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        add( lineStyle, gbc );
        lineStyle.setToolTipText( "Type of line style used when drawing spectrum" );

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add( Box.createHorizontalStrut( 5 ), gbc );
        lineStyle.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    updateLineStyle( e );
                }
            });


        //  Set up the errorbar display control.
        errorLabel.setText( "Error bars:" );

        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        add( errorLabel, gbc );

        add( Box.createHorizontalStrut( 5 ) );

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        add( errors, gbc );
        errors.setToolTipText
            ( "Enabled if errors available, ticked to display error bars" );
        errors.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    updateErrors();
                }
            });

        //  Add additional button for setting the error bar colour.
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        add( errorsColour, gbc );
        errorsColour.setToolTipText( "Choose a colour for error bars" );
        errorsColour.setIcon( errorsColourIcon );

        errorsColour.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    updateErrorColour( e );
                }
            });


        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add( Box.createHorizontalStrut( 5 ), gbc );
        add( Box.createVerticalStrut( 5 ), gbc );

        //  Set up the listSelectionListener so that we can update
        //  interface.
        list.addListSelectionListener( new ListSelectionListener()  {
                public void valueChanged( ListSelectionEvent e ) {
                    update( e );
                }
            });
    }

    /**
     *  Update the value of all components to reflect the values of
     *  the first selected spectrum. Version invoked when list is
     *  selected.
     */
    protected void update( ListSelectionEvent e )
    {
        if ( ! e.getValueIsAdjusting() ) {
            update();
        }
    }

    /**
     * Update all values to reflect those of the selected
     * spectrum. Used when external events change spectral properties.
     */
    public void update()
    {
        int size = list.getModel().getSize();
        int index = list.getMinSelectionIndex();
        if ( size > 0 && index > -1 ) {
            inhibitChanges = true;
            
            SpecData spec = globalList.getSpectrum( index );
            shortName.setText( spec.getShortName() );
            fullName.setText( spec.getFullName() );
            format.setText( spec.getDataFormat() );
            thickness.setSelectedIndex( (int)spec.getLineThickness()-1 );
            lineStyle.setSelectedStyle( (int)spec.getLineStyle() );
            lineType.setSelectedStyle( (int)spec.getPlotStyle() );
            errors.setEnabled( spec.haveYDataErrors() );
            
            //  Update the line colour,
            linesColourIcon.setMainColour
                ( new Color( (int)spec.getLineColour() ) );
            lineColour.repaint();
            
            //  And the error colour.
            errorsColourIcon.setMainColour
                ( new Color( (int)spec.getErrorColour() ) );
            errorsColour.repaint();
            
            inhibitChanges = false;
        }
    }

    /**
     *  Update (i.e. change) the short name of all the selected spectra.
     */
    public void updateShortName()
    {
        if ( inhibitChanges ) return;

        int[] indices = list.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            String name = shortName.getText();
            if ( name != null && ! "".equals( name.trim() ) ) {
                for ( int i = 0; i < indices.length; i++ ) {
                    globalList.setShortName( indices[i], name );
                }
            }
            else {
                //  Blank name, so reset to previous name.
                for ( int i = 0; i < indices.length; i++ ) {
                    globalList.setShortName
                        ( indices[i], globalList.getShortName(indices[i]));
                }
            }
        }
    }

    /**
     *  Change the line thickness of all selected spectra.
     */
    protected void updateThickness( ActionEvent e )
    {
        if ( inhibitChanges ) return;

        int[] indices = list.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            SpecData spec = null;
            Integer thick = (Integer) thickness.getSelectedItem();
            for ( int i = 0; i < indices.length; i++ ) {
                spec = globalList.getSpectrum( indices[i] );
                if ( spec != null ) {
                    globalList.setKnownNumberProperty( spec,
                                                       SpecData.LINE_THICKNESS,
                                                       thick );
                }
            }
        }
    }

    /**
     *  Change the line style.
     */
    protected void updateLineStyle( ActionEvent e )
    {
        if ( inhibitChanges ) return;

        int[] indices = list.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            SpecData spec = null;
            int style = lineStyle.getSelectedStyle();
            for ( int i = 0; i < indices.length; i++ ) {
                spec = globalList.getSpectrum( indices[i] );
                if ( spec != null ) {
                    globalList.setKnownNumberProperty( spec,
                                                       SpecData.LINE_STYLE,
                                                       new Double(style));
                }
            }
        }
    }

    /**
     *  Change the line type.
     */
    protected void updatePlotStyle( ActionEvent e )
    {
        if ( inhibitChanges ) return;

        int[] indices = list.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            SpecData spec = null;
            int type = lineType.getSelectedStyle();
            for ( int i = 0; i < indices.length; i++ ) {
                spec = globalList.getSpectrum( indices[i] );
                if ( spec != null ) {
                    globalList.setKnownNumberProperty( spec,
                                                       SpecData.PLOT_STYLE,
                                                       new Double(type));
                }
            }
        }
    }

    /**
     *  Change the line colour, allow user to select using
     *  JColorChooser dialog.
     */
    protected void updateLineColour( ActionEvent e )
    {
        if ( inhibitChanges ) return;

        int[] indices = list.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            Color newColour = JColorChooser.showDialog
                ( this, "Select Line Colour",
                  linesColourIcon.getMainColour() );
            if ( newColour != null ) {
                linesColourIcon.setMainColour( newColour );
                SpecData spec = null;
                for ( int i = 0; i < indices.length; i++ ) {
                    spec = globalList.getSpectrum( indices[i] );
                    if ( spec != null ) {
                        globalList.setKnownNumberProperty
                            ( spec, SpecData.LINE_COLOUR,
                              new Integer( newColour.getRGB() ) );
                    }
                }
            }
        }
    }

    /**
     *  Change the error bar colour, allow user to select using
     *  JColorChooser dialog.
     */
    protected void updateErrorColour( ActionEvent e )
    {
        int[] indices = list.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            Color newColour = JColorChooser.showDialog
                ( this, "Select Error Bar Colour",
                  errorsColourIcon.getMainColour() );
            if ( newColour != null ) {
                errorsColourIcon.setMainColour( newColour );
                SpecData spec = null;
                for ( int i = 0; i < indices.length; i++ ) {
                    spec = globalList.getSpectrum( indices[i] );
                    if ( spec != null ) {
                        globalList.setKnownNumberProperty
                            ( spec, SpecData.ERROR_COLOUR,
                              new Integer( newColour.getRGB() ) );
                    }
                }
            }
        }
    }

    /**
     *  Change the alpha blending factor.
     */
    protected void updateAlpha()
    {
        if ( inhibitChanges ) return;

        int[] indices = list.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            SpecData spec = null;
            double alphaBlend = alphaSlider.getValue();
            for ( int i = 0; i < indices.length; i++ ) {
                spec = globalList.getSpectrum( indices[i] );
                if ( spec != null ) {
                    globalList.setKnownNumberProperty
                        ( spec, SpecData.LINE_ALPHA_BLEND,
                          new Double( alphaBlend ) );
                }
            }
        }
    }

    /**
     *  Update whether we're displaying data errorbars or not.
     */
    protected void updateErrors()
    {
        if ( inhibitChanges ) return;

        int[] indices = list.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            SpecData spec = null;
            boolean showing = errors.isSelected();
            for ( int i = 0; i < indices.length; i++ ) {
                spec = globalList.getSpectrum( indices[i] );
                if ( spec != null ) {
                    globalList.setDrawErrorBars( spec, showing );
                }
            }
        }
    }
}
