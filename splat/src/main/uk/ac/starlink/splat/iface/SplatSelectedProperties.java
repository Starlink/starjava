/*
 * Copyright (C) 2000-2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     02-OCT-2000 (Peter W. Draper):
 *        Original version.
 *     02-MAR-2004 (Peter W. Draper):
 *        Converted to match view to spectral properties.
 */
package uk.ac.starlink.splat.iface;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.starlink.ast.gui.AstStyleBox;
import uk.ac.starlink.ast.gui.ColourIcon;
import uk.ac.starlink.ast.gui.DecimalField;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.util.gui.GridBagLayouter;

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
public class SplatSelectedProperties
    extends JPanel
    implements ActionListener
{
    /**
     *  Reference to GlobalSpecPlotList object.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     *  The JList containing names of all the available spectra.
     */
    protected JList specList = null;

    /**
     *  Various components used in the interface.
     */
    protected AstStyleBox lineStyle = new AstStyleBox();
    protected ColourIcon errorsColourIcon = new ColourIcon( Color.red );
    protected ColourIcon linesColourIcon = new ColourIcon( Color.blue );
    protected JButton errorsColour = new JButton();
    protected JButton lineColour = new JButton();
    protected JCheckBox errors = new JCheckBox();
    protected JComboBox coordColumn = new JComboBox();
    protected JComboBox dataColumn = new JComboBox();
    protected JComboBox errorColumn = new JComboBox();
    protected JComboBox thickness = new JComboBox();
    protected JLabel format = new JLabel();
    protected JLabel fullName = new JLabel();
    protected JTextField shortName = new JTextField();
    protected PlotStyleBox lineType = new PlotStyleBox();

    /**
     * Stop updates of properties from propagating to other listeners
     * and controls.
     */
    protected boolean inhibitChanges = false;

    /**
     * Names for the possible composite values.
     */
    protected static final String[] COMPOSITE_NAMES =
    {
        "0%","10%","20%","30%","40%","50%","60%","70%","80%","90%","100%"
    };
    protected JComboBox alphaValue = new JComboBox( COMPOSITE_NAMES );

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
        layouter.setInsets( new Insets( 2, 5, 0, 2 ) );

        //  Set up the two name display controls. These are different
        //  from others in that they fill all remaining columns,
        //  rather than allowing a strut to take up all the horizontal
        //  space.
        layouter.add( "Short name:" , false );
        layouter.add( shortName, true );
        shortName.setToolTipText
            ( "Symbolic name of spectrum, press return to accept edits" );

        layouter.add( "Full name:" , false );
        layouter.add( fullName, true );
        fullName.setToolTipText( "Full name of spectrum (usually filename)" );

        //  The name field can be editted.
        shortName.addActionListener( this );

        //  Set up the spectrum data format control.
        layouter.add( "Format:", false );
        layouter.add( format, true );

        format.setToolTipText( "Data type used for storage of spectrum" );

        //  Column selection.
        layouter.add( "Columns:", false );
        JPanel columnPanel = new JPanel( new GridLayout( 2, 3 ) );
        columnPanel.add( new JLabel( "Coordinates", SwingConstants.CENTER ) );
        columnPanel.add( new JLabel( "Data", SwingConstants.CENTER  ) );
        columnPanel.add( new JLabel( "Errors", SwingConstants.CENTER  ) );
        columnPanel.add( coordColumn );
        columnPanel.add( dataColumn );
        columnPanel.add( errorColumn );
        layouter.add( columnPanel, false );
        layouter.eatLine();

        coordColumn.addActionListener( this );
        dataColumn.addActionListener( this );
        errorColumn.addActionListener( this );

        coordColumn.setToolTipText( "Name of coordinates column" );
        dataColumn.setToolTipText( "Name of data values column" );
        errorColumn.setToolTipText( "Name of data errors column" );
        coordColumn.setEnabled( false );
        dataColumn.setEnabled( false );
        errorColumn.setEnabled( false );


        //  Set up the line colour control.
        layouter.add( "Colour:" , false );
        layouter.add( lineColour, false );
        layouter.eatLine();
        lineColour.setToolTipText( "Choose a colour for spectrum" );
        lineColour.setIcon( linesColourIcon );

        lineColour.addActionListener( this );

        //  AlphaComposite value.
        layouter.add( "Composite:", false );
        layouter.add( alphaValue, false );
        layouter.eatLine();

        alphaValue.setSelectedIndex( COMPOSITE_NAMES.length - 1 );
        alphaValue.addActionListener( this );

        //  Set up the line type control.
        layouter.add( "Line type:", false );
        layouter.add( lineType, false );
        layouter.eatLine();
        lineType.setToolTipText( "Type of line used to show spectrum" );

        lineType.addActionListener( this );

        //  Set up the line thickness control.
        layouter.add( "Thickness:", false );
        layouter.add( thickness, false );
        layouter.eatLine();
        thickness.setToolTipText( "Thickness of spectrum line" );

        for ( int i = 1; i < 20; i++ ) {
            thickness.addItem( new Integer( i ) );
        }
        thickness.addActionListener( this );

        //  Set up the line style control.
        layouter.add( "Line style:", false );
        layouter.add( lineStyle, false );
        layouter.eatLine();
        lineStyle.setToolTipText
            ( "Type of line style used when drawing spectrum" );

        lineStyle.addActionListener( this );

        //  Set up the errorbar display control.
        layouter.add( "Error bars:", false );
        JPanel colourPanel = new JPanel( new GridLayout() );
        layouter.add( colourPanel, false );
        colourPanel.add( errors );

        errors.setToolTipText
            ( "Enabled if errors available, ticked to display error bars" );
        errors.addActionListener( this );

        //  Add additional button for setting the error bar colour.
        colourPanel.add( errorsColour );
        layouter.eatLine();
        errorsColour.setToolTipText( "Choose a colour for error bars" );
        errorsColour.setIcon( errorsColourIcon );

        errorsColour.addActionListener( this );

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
            try {
                SpecData spec = globalList.getSpectrum( index );
                shortName.setText( spec.getShortName() );
                fullName.setText( spec.getFullName() );
                format.setText( spec.getDataFormat() );
                thickness.setSelectedIndex( (int)spec.getLineThickness()-1 );
                lineStyle.setSelectedStyle( (int)spec.getLineStyle() );
                lineType.setSelectedStyle( (int)spec.getPlotStyle() );
                errors.setEnabled( spec.haveYDataErrors() );            
                errors.setSelected( spec.isDrawErrorBars() );

                //  Update the line colour,
                linesColourIcon.setMainColour
                    ( new Color( (int)spec.getLineColour() ) );
                lineColour.repaint();

                //  And the error colour.
                errorsColourIcon.setMainColour
                    ( new Color( (int)spec.getErrorColour() ) );
                errorsColour.repaint();

                //  Update the column names.
                String[] names = spec.getColumnNames();
                if ( names != null ) {
                    coordColumn.setEnabled( true );
                    dataColumn.setEnabled( true );
                    errorColumn.setEnabled( true );
                    coordColumn.setModel( new DefaultComboBoxModel( names ) );
                    dataColumn.setModel( new DefaultComboBoxModel( names ) );
                    errorColumn.setModel( new DefaultComboBoxModel( names ) );

                    //  Error may not be set.
                    errorColumn.insertItemAt( "", 0 );
                    coordColumn.setSelectedItem( spec.getXDataColumnName() );
                    dataColumn.setSelectedItem( spec.getYDataColumnName() );
                    errorColumn.setSelectedItem
                        ( spec.getYDataErrorColumnName() );
                }
                else {
                    coordColumn.setEnabled( false );
                    dataColumn.setEnabled( false );
                    errorColumn.setEnabled( false );
                    ((DefaultComboBoxModel)coordColumn.getModel())
                        .removeAllElements();
                    ((DefaultComboBoxModel)dataColumn.getModel())
                        .removeAllElements();
                    ((DefaultComboBoxModel)errorColumn.getModel())
                        .removeAllElements();
                }

            }
            finally {
                inhibitChanges = false;
            }
        }
    }

    /**
     *  Set the short name of the selected spectra.
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
    protected void updateThickness()
    {
        if ( inhibitChanges ) return;

        int[] indices = specList.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            Integer thick = (Integer) thickness.getSelectedItem();
            applyProperty( indices, SpecData.LINE_THICKNESS, thick );
        }
    }

    /**
     *  Change the line style.
     */
    protected void updateLineStyle()
    {
        if ( inhibitChanges ) return;

        int[] indices = specList.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            Double value = new Double( lineStyle.getSelectedStyle() );
            applyProperty( indices, SpecData.LINE_STYLE, value );
        }
    }

    /**
     *  Change the line type.
     */
    protected void updatePlotStyle()
    {
        if ( inhibitChanges ) return;

        int[] indices = specList.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            Double value = new Double( lineType.getSelectedStyle() );
            applyProperty( indices, SpecData.PLOT_STYLE, value );
        }
    }

    /**
     *  Change the line colour, allow user to select using
     *  JColorChooser dialog.
     */
    protected void updateLineColour()
    {
        if ( inhibitChanges ) return;

        int[] indices = specList.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            Color newColour =
                JColorChooser.showDialog( this, "Select Line Colour",
                                          linesColourIcon.getMainColour() );
            if ( newColour != null ) {
                linesColourIcon.setMainColour( newColour );
                Integer colour = new Integer( newColour.getRGB() );
                applyProperty( indices, SpecData.LINE_COLOUR, colour );
            }
        }
    }

    /**
     *  Change the error bar colour, allow user to select using
     *  JColorChooser dialog.
     */
    protected void updateErrorColour()
    {
        int[] indices = specList.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            Color newColour =
                JColorChooser.showDialog( this, "Select Error Bar Colour",
                                          errorsColourIcon.getMainColour() );
            if ( newColour != null ) {
                errorsColourIcon.setMainColour( newColour );
                Integer colour = new Integer( newColour.getRGB() );
                applyProperty( indices, SpecData.ERROR_COLOUR, colour );
            }
        }
    }

    /**
     *  Change the alpha composite value.
     */
    protected void updateAlpha()
    {
        if ( inhibitChanges ) return;
        int[] indices = specList.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            int index = alphaValue.getSelectedIndex();
            Double alpha = new Double( 0.1 * (double) index );
            applyProperty( indices, SpecData.LINE_ALPHA_COMPOSITE, alpha );
        }
    }

    /**
     * Apply a "known" property value to a list of spectra.
     */
    protected void applyProperty( int[] indices, int what, Number value )
    {
        SpecData spec = null;
        for ( int i = 0; i < indices.length; i++ ) {
            spec = globalList.getSpectrum( indices[i] );
            if ( spec != null ) {
                globalList.setKnownNumberProperty( spec, what, value );
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

    /**
     *  Set the column used for the coordinate values.
     */
    protected void updateCoordColumn()
    {
        if ( inhibitChanges ) return;

        int[] indices = specList.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            SpecData spec = null;
            String column = (String) coordColumn.getSelectedItem();
            for ( int i = 0; i < indices.length; i++ ) {
                spec = globalList.getSpectrum( indices[i] );
                if ( spec != null ) {
                    try {
                        spec.setXDataColumnName( column );
                        globalList.notifySpecListeners( spec );
                    }
                    catch (SplatException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     *  Set the column used for the data values.
     */
    protected void updateDataColumn()
    {
        if ( inhibitChanges ) return;

        int[] indices = specList.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            SpecData spec = null;
            String column = (String) dataColumn.getSelectedItem();
            for ( int i = 0; i < indices.length; i++ ) {
                spec = globalList.getSpectrum( indices[i] );
                if ( spec != null ) {
                    try {
                        spec.setYDataColumnName( column );
                        globalList.notifySpecListeners( spec );
                    }
                    catch (SplatException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     *  Set the column used for the data errors.
     */
    protected void updateErrorColumn()
    {
        if ( inhibitChanges ) return;

        int[] indices = specList.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            SpecData spec = null;
            String column = (String) errorColumn.getSelectedItem();
            for ( int i = 0; i < indices.length; i++ ) {
                spec = globalList.getSpectrum( indices[i] );
                if ( spec != null ) {
                    try {
                        spec.setYDataErrorColumnName( column );
                        globalList.notifySpecListeners( spec );
                    }
                    catch (SplatException e) {
                        e.printStackTrace();
                    }
                }
            }
            update(); // To error button.
        }
    }

    //
    // ActionListener interface.
    //
    public void actionPerformed( ActionEvent e )
    {
        Object source = e.getSource();
        if ( source.equals( shortName ) ) {
            updateShortName();
            return;
        }

        if ( source.equals( coordColumn ) ) {
            updateCoordColumn();
            return;
        }
        if ( source.equals( dataColumn ) ) {
            updateDataColumn();
            return;
        }
        if ( source.equals( errorColumn ) ) {
            updateErrorColumn();
            return;
        }

        if ( source.equals( lineColour ) ) {
            updateLineColour();
            return;
        }

        if ( source.equals( alphaValue ) ) {
            updateAlpha();
            return;
        }

        if ( source.equals( lineType ) ) {
            updatePlotStyle();
            return;
        }

        if ( source.equals( thickness ) ) {
            updateThickness();
            return;
        }

        if ( source.equals( lineStyle ) ) {
            updateLineStyle();
            return;
        }

        if ( source.equals( errors ) ) {
            updateErrors();
            return;
        }

        if ( source.equals( errorsColour ) ) {
            updateErrorColour();
            return;
        }
    }
}
