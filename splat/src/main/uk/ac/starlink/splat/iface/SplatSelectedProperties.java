/*
 * Copyright (C) 2000-2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     02-OCT-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.Color;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.util.GridBagLayouter;
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
 * @version $Id$
 * @author Peter W. Draper
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
    protected JList specList = null;

    /**
     *  Various components used in the interface.
     */
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
    public SplatSelectedProperties( JList specList )
    {
        this.specList = specList;
        initUI();
    }

    /**
     *  Add all the components for display the spectrum properties.
     */
    protected void initUI()
    {
        GridBagLayouter layouter = 
            new GridBagLayouter( this, GridBagLayouter.SCHEME3 );

        //  Set up the two name display controls. These are different
        //  from others in that they fill all remaining columns,
        //  rather than allowing a strut to take up all the horizontal
        //  space.
        shortNameLabel.setAlignmentY( (float) 0.0 );
        shortNameLabel.setText( "Short name:" );
        fullNameLabel.setAlignmentY( (float) 0.0 );
        fullNameLabel.setText( "Full name:" );

        layouter.add( shortNameLabel, false );
        layouter.add( Box.createHorizontalStrut( 5 ), false );
        layouter.add( shortName, true );
        shortName.setToolTipText
            ( "Symbolic name of spectrum, press return to accept edits" );

        layouter.add( fullNameLabel, false );
        layouter.add( Box.createHorizontalStrut( 5 ), false );
        layouter.add( fullName, true );
        fullName.setToolTipText( "Full name of spectrum (usually filename)" );

        //  The name field can be editted.
        shortName.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    updateShortName();
                }
            });

        //  Set up the spectrum data format control.
        formatLabel.setText( "Format:" );

        layouter.add( formatLabel, false );
        layouter.add( Box.createHorizontalStrut( 5 ), false );
        layouter.add( format, false );
        layouter.add( Box.createHorizontalStrut( 5 ), true );

        format.setToolTipText( "Data type used for storage of spectrum" );

        //  Set up the line colour control.
        lineColourLabel.setText( "Colour:" );

        layouter.add( lineColourLabel, false );
        layouter.add( Box.createHorizontalStrut( 5 ), false );
        layouter.add( lineColour, false );
        layouter.add( Box.createHorizontalStrut( 5 ), true );

        lineColour.setToolTipText( "Choose a colour for spectrum" );
        lineColour.setIcon( linesColourIcon );

        lineColour.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    updateLineColour( e );
                }
            });

        //  Alpha blending value. TODO: not sure this is useful, is
        //  very slow.
//         alphaLabel.setText( "Alpha blending:" );
//         FloatJSliderModel alphaModel = new FloatJSliderModel
//             ( 1.0, 0.0, 1.0, 0.05 );
//         alphaSlider = new FloatJSlider( alphaModel, false );
        
//         layouter.add( alphaLabel, false );
//         layouter.add( Box.createHorizontalStrut( 5 ), false );
//         layouter.add( alphaSlider, false );
//         layouter.add( Box.createHorizontalStrut( 5 ), true );
        
//         alphaSlider.addChangeListener( new ChangeListener() {
//                 public void stateChanged( ChangeEvent e ) {
//                     updateAlpha();
//                 }
//             });


        //  Set up the line type control.
        lineTypeLabel.setText( "Line type:" );

        layouter.add( lineTypeLabel, false );
        layouter.add( Box.createHorizontalStrut( 5 ), false );
        layouter.add( lineType, false );
        layouter.add( Box.createHorizontalStrut( 5 ), true );
        lineType.setToolTipText( "Type of line used to show spectrum" );

        lineType.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    updatePlotStyle( e );
                }
            });

        //  Set up the line thickness control.
        thicknessLabel.setText( "Thickness:" );

        layouter.add( thicknessLabel, false );
        layouter.add( Box.createHorizontalStrut( 5 ), false );
        layouter.add( thickness, false );
        layouter.add( Box.createHorizontalStrut( 5 ), true );
        thickness.setToolTipText( "Thickness of spectrum line" );

        for ( int i = 1; i < 20; i++ ) {
            thickness.addItem( new Integer( i ) );
        }
        thickness.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    updateThickness( e );
                }
            });

        //  Set up the line style control.
        lineStyleLabel.setText( "Line style:" );

        layouter.add( lineStyleLabel, false );
        layouter.add( Box.createHorizontalStrut( 5 ), false );
        layouter.add( lineStyle, false );
        layouter.add( Box.createHorizontalStrut( 5 ), true );
        lineStyle.setToolTipText( "Type of line style used when drawing spectrum" );

        lineStyle.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    updateLineStyle( e );
                }
            });


        //  Set up the errorbar display control.
        errorLabel.setText( "Error bars:" );

        layouter.add( errorLabel, false );
        layouter.add( Box.createHorizontalStrut( 5 ), false );
        layouter.add( errors, false );

        errors.setToolTipText
            ( "Enabled if errors available, ticked to display error bars" );
        errors.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    updateErrors();
                }
            });

        //  Add additional button for setting the error bar colour.
        layouter.add( errorsColour, false );
        layouter.add( Box.createHorizontalStrut( 5 ), true );
        errorsColour.setToolTipText( "Choose a colour for error bars" );
        errorsColour.setIcon( errorsColourIcon );

        errorsColour.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    updateErrorColour( e );
                }
            });

        layouter.eatSpare();

        //  Set up the listSelectionListener so that we can update
        //  interface.
        specList.addListSelectionListener( new ListSelectionListener()  {
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
        int size = specList.getModel().getSize();
        int index = specList.getMinSelectionIndex();
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

        int[] indices = specList.getSelectedIndices();
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

        int[] indices = specList.getSelectedIndices();
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

        int[] indices = specList.getSelectedIndices();
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

        int[] indices = specList.getSelectedIndices();
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

        int[] indices = specList.getSelectedIndices();
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
        int[] indices = specList.getSelectedIndices();
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

        int[] indices = specList.getSelectedIndices();
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

        int[] indices = specList.getSelectedIndices();
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
