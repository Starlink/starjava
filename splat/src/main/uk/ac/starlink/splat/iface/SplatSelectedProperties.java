/*
 * Copyright (C) 2000-2004 Central Laboratory of the Research Councils
 * Copyright (C) 2009 Science and Technology Facilities Council
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
import java.util.prefs.Preferences;

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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.starlink.ast.gui.AstStyleBox;
import uk.ac.starlink.ast.gui.ColourIcon;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * SplatSelectedProperties defines objects for viewing the image
 * symbolic and full names, data type, and display properties of
 * a list of selected spectra. When more than one spectrum is selected
 * the current properties shown are those of the first, but
 * modifications of any properties are applied to all spectra.
 * <p>
 * Instances of this class also offer the ability to save the current
 * rendering settings as the defaults. These can be used when opening
 * new spectra and will be restored between sessions.
 *
 * @version $Id$
 * @author Peter W. Draper
 *
 */
public class SplatSelectedProperties
    extends JPanel
    implements ActionListener
{
    private static Preferences prefs =
        Preferences.userNodeForPackage( SplatSelectedProperties.class );

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
    protected JButton saveProp = new JButton();
    protected JButton resetProp = new JButton();
    protected JCheckBox errors = new JCheckBox();
    protected JComboBox coordColumn = new JComboBox();
    protected JComboBox dataColumn = new JComboBox();
    protected JComboBox errorColumn = new JComboBox();
    protected JComboBox errorFrequency = new JComboBox();
    protected JComboBox errorScale = new JComboBox();
    protected JComboBox pointSize = new JComboBox();
    protected JComboBox thickness = new JComboBox();
    protected JLabel format = new JLabel();
    protected JLabel fullName = new JLabel();
    protected JTextField shortName = new JTextField();
    protected PlotStyleBox lineType = new PlotStyleBox();
    protected PointTypeBox pointType = new PointTypeBox();

    /**
     *  Storage handler for rendering properties defaults and persistence.
     */
    private RenProps renProps = new RenProps();

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

        //  Set up the line colour control. Note it, saveProp and resetProp go
        //  on same line.
        JPanel colourSaveResetPanel = new JPanel();
        layouter.add( "Colour:" , false );
        layouter.add( colourSaveResetPanel, false );
        layouter.eatLine();

        colourSaveResetPanel.add( lineColour );
        lineColour.setToolTipText( "Choose a colour for spectrum" );
        lineColour.setIcon( linesColourIcon );
        lineColour.addActionListener( this );

        //  Save rendering properties button.
        saveProp.setText( "Save" );
        colourSaveResetPanel.add( saveProp );
        saveProp.setToolTipText
            ( "Save spectrum renderingproperties as default" );
        saveProp.addActionListener( this );

        //  Reset rendering properties button.
        resetProp.setText( "Reset" );
        colourSaveResetPanel.add( resetProp );
        resetProp.setToolTipText
            ( "Reset spectrum rendering properties to default" );
        resetProp.addActionListener( this );

        //  AlphaComposite value.         
        layouter.add( "Composite:", false );
        layouter.add( alphaValue, false );
        alphaValue.setSelectedIndex( COMPOSITE_NAMES.length - 1 );
        alphaValue.addActionListener( this );

        layouter.eatLine();

        //  Set up the line type control.
        layouter.add( "Line type:", false );
        layouter.add( lineType, false );
        layouter.eatLine();
        lineType.setToolTipText( "Type used to render the spectrum" );

        lineType.addActionListener( this );

        //  The thickness and style are joined into a single line.
        JPanel lineProps = new JPanel();
        GridBagLayouter layouter2 =
            new GridBagLayouter( lineProps, GridBagLayouter.SCHEME3 );

        //  Set up the line thickness control.
        thickness.setToolTipText( "Width of spectrum when draw as a line" );
        for ( int i = 1; i < 20; i++ ) {
            thickness.addItem( new Integer( i ) );
        }
        thickness.addActionListener( this );

        //  Set up the line style control.
        lineStyle.setToolTipText
            ( "Type of line style used when drawing spectrum" );
        lineStyle.addActionListener( this );

        //  Add controls to this combined line.
        layouter2.add( thickness, false );
        layouter2.add( "Style:", false );
        layouter2.add( lineStyle, false );
        layouter2.eatLine();

        //  Label this line (need this for aligned purposes) and add the 
        //  combined thickness and style component.
        layouter.add( "Line width:", false );
        layouter.add( lineProps, false );
        layouter.eatLine();

        //  The point type and size are joined into a single line.
        JPanel pointProps = new JPanel();
        layouter2 = new GridBagLayouter( pointProps, GridBagLayouter.SCHEME3 );

        //  Set up the point type control.
        pointType.setToolTipText("Type of points used when drawing spectrum");
        pointType.addActionListener( this );

        //  And point size.
        pointSize.setToolTipText
            ( "Size of the points used when drawing spectrum" );
        for ( int i = 1; i < 32; i++ ) {
            pointSize.addItem( new Double( i ) );
        }
        pointSize.addActionListener( this );

        layouter2.add( pointType, false );
        layouter2.add( "Size:", false );
        layouter2.add( pointSize, false );
        layouter2.eatLine();

        layouter.add( "Point type:", false );
        layouter.add( pointProps, false );
        layouter.eatLine();

        //  Set up the errorbar display control.
        JPanel errorControls = new JPanel();
        layouter2 = new GridBagLayouter( errorControls, 
                                         GridBagLayouter.SCHEME3 );
        errorControls.add( errors );

        errors.setToolTipText
            ( "Enabled if errors available, ticked to display error bars" );
        errors.addActionListener( this );

        //  Add additional button for setting the error bar colour.
        errorsColour.setIcon( errorsColourIcon );
        layouter2.add( errorsColour, false );

        errorsColour.setToolTipText( "Choose a colour for error bars" );
        errorsColour.addActionListener( this );

        //  Number of sigma plotted for error bars.
        for ( int i = 1; i < 6; i++ ) {
            errorScale.addItem( new Integer( i ) );
        }
        errorScale.setToolTipText("Set number of sigma shown for error bars");
        errorScale.addActionListener( this );
        layouter2.add( errorScale, false );

        //  Frequency of error bars (1, 2, 3, 4, 5, 6, 7, 8, 9 ... 20)
        for ( int i = 1; i < 21; i++ ) {
            errorFrequency.addItem( new Integer( i ) );
        }
        errorFrequency.setToolTipText("Set frequency for drawing error bars");
        errorFrequency.addActionListener( this );
        layouter2.add( errorFrequency, false );
        layouter2.eatLine();

        //  Add the main error control component.
        layouter.add( "Error bars:", false );
        layouter.add( errorControls, false );
        layouter.eatSpare();

        //  Set the default values.
        renProps.restore();

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
                thickness.setSelectedIndex( (int)spec.getLineThickness() - 1 );
                pointType.setSelectedType( (int)spec.getPointType() );
                pointSize.setSelectedIndex( (int)spec.getPointSize() - 1 );
                lineStyle.setSelectedStyle( (int)spec.getLineStyle() );
                lineType.setSelectedStyle( (int)spec.getPlotStyle() );

                alphaValue.setSelectedIndex
                    ( (int) ( 10.0 * spec.getAlphaComposite() ) );

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

                //  Error bar nsigma.
                errorScale.setSelectedIndex( (int)spec.getErrorNSigma() - 1 );
                errorFrequency.setSelectedIndex
                    ( (int)spec.getErrorFrequency() - 1 );

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
     *  Get the line thickness.
     */
    public int getThickness()
    {
        Integer thick = (Integer) thickness.getSelectedItem();
        return thick.intValue();
    }

    /**
     *  Change the point type of all selected spectra.
     */
    protected void updatePointType()
    {
        if ( inhibitChanges ) return;

        int[] indices = specList.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            Integer type = new Integer( pointType.getSelectedType() );
            applyProperty( indices, SpecData.POINT_TYPE, type );
        }
    }

    /**
     *  Get point type.
     */
    public int getPointType()
    {
        return pointType.getSelectedType();
    }

    /**
     *  Change the point size of all selected spectra.
     */
    protected void updatePointSize()
    {
        if ( inhibitChanges ) return;

        int[] indices = specList.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            Double size = (Double) pointSize.getSelectedItem();
            applyProperty( indices, SpecData.POINT_SIZE, size );
        }
    }

    /**
     *  Get the point size.
     */
    public double getPointSize()
    {
        Double size = (Double) pointSize.getSelectedItem();
        return size.doubleValue();
    }

    /**
     *  Change the line style (dashed etc.).
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
     *  Get the line style.
     */
    public double getLineStyle()
    {
        return lineStyle.getSelectedStyle();
    }

    /**
     *  Change the plot style (type of connection, polyline, histogram, point).
     */
    protected void updatePlotStyle()
    {
        if ( inhibitChanges ) return;

        int[] indices = specList.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            Integer value = new Integer( lineType.getSelectedStyle() );
            applyProperty( indices, SpecData.PLOT_STYLE, value );
        }
    }

    /**
     *  Get the plot style.
     */
    public int getPlotStyle()
    {
        return lineType.getSelectedStyle();
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
     *  Get the line colour.
     */
    public int getLineColour()
    {
        Color color = linesColourIcon.getMainColour();
        return color.getRGB();
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
     *  Get the error bar colour.
     */
    public int getErrorColour()
    {
        Color color = errorsColourIcon.getMainColour();
        return color.getRGB();
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
     *  Get the alpha componsite value.
     */
    protected double getAlpha()
    {
        return 0.1 * (double) alphaValue.getSelectedIndex();
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
     *  Change the number of sigma plotted for the selected spectra.
     */
    protected void updateErrorScale()
    {
        if ( inhibitChanges ) return;

        int[] indices = specList.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            Integer nsigma = (Integer) errorScale.getSelectedItem();
            applyProperty( indices, SpecData.ERROR_NSIGMA, nsigma );
        }
    }

    /**
     *  Get the number of sigma displayed when drawing a spectrum.
     */
    public int getErrorScale()
    {
        Integer nsigma = (Integer) errorScale.getSelectedItem();
        return nsigma.intValue();
    }

    /**
     *  Change the frequency used for drawing error bars.
     */
    protected void updateErrorFrequency()
    {
        if ( inhibitChanges ) return;

        int[] indices = specList.getSelectedIndices();
        if ( indices.length > 0 && indices[0] > -1 ) {
            Integer freq = (Integer) errorFrequency.getSelectedItem();
            applyProperty( indices, SpecData.ERROR_FREQUENCY, freq );
        }
    }

    /**
     *  Get the frequency used for drawing error bars.
     */
    public int getErrorFrequency()
    {
        Integer freq = (Integer) errorFrequency.getSelectedItem();
        return freq.intValue();
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
                        spec.setXDataColumnName( column, true );
                        globalList.notifySpecListenersModified( spec );
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
                        globalList.notifySpecListenersModified( spec );
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
                        globalList.notifySpecListenersModified( spec );
                    }
                    catch (SplatException e) {
                        e.printStackTrace();
                    }
                }
            }
            update(); // To error button.
        }
    }

    /**
     *  Save the current rendering properties, making them the defaults
     *  to be applied to new spectra if required and preserving between
     *  sessions.
     */
    public void saveRenderingProps()
    {
        renProps.save();
    }

    /**
     *  Restore the saved rendering properties from backing store, or
     *  create some defaults.
     */
    public void restoreRenderingProps()
    {
        renProps.restore();
    }
    
    /**
     *  Reset the current rendering properties to the defaults.
     */
    public void resetRenderingProps()
    {
        renProps.reset();

        //  Apply changes to the selected spectra and update their
        //  properties.
        int[] indices = specList.getSelectedIndices();
        SpecData spec = null;
        for ( int i = 0; i < indices.length; i++ ) {
            spec = globalList.getSpectrum( indices[ i ] );
            renProps.apply( spec );
            globalList.notifySpecListenersChange( spec );
        }

        //  Reflect this in the interface.
        update();
        renProps.save();
    }

    /**
     *  Apply the current default rendering properties to a spectrum.
     */
    public void applyRenderingProps( SpecData spectrum )
    {
        renProps.apply( spectrum );
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

        if ( source.equals( pointType ) ) {
            updatePointType();
            return;
        }

        if ( source.equals( pointSize ) ) {
            updatePointSize();
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

        if ( source.equals( errorScale ) ) {
            updateErrorScale();
            return;
        }

        if ( source.equals( errorFrequency ) ) {
            updateErrorFrequency();
            return;
        }

        if ( source.equals( saveProp ) ) {
            saveRenderingProps();
            return;
        }

        if ( source.equals( resetProp ) ) {
            resetRenderingProps();
            return;
        }
    }

    //  Simple class to encapsulate all the spectral rendering properties we
    //  want to save, restore and apply.
    private class RenProps
    {
        public RenProps()
        {
        	reset();
        }

        private double alpha;
        private double lineStyle;
        private double pointSize;
        private int errorColour;
        private int errorFrequency;
        private int errorScale;
        private int lineColour;
        private int lineThickness;
        private int plotStyle;
        private int pointType;

        public void save() 
        {
            alpha = getAlpha();
            errorColour = getErrorColour();
            errorFrequency = getErrorFrequency();
            errorScale = getErrorScale();
            lineColour = getLineColour();
            lineStyle = getLineStyle();
            lineThickness = getThickness();
            plotStyle = getPlotStyle();
            pointSize = getPointSize();
            pointType = getPointType();
            
            prefs.putDouble( "SplatSelectedProperties_alpha", alpha );
            prefs.putInt( "SplatSelectedProperties_errorcolour", errorColour );
            prefs.putInt( "SplatSelectedProperties_errorfrequency", 
                          errorFrequency );
            prefs.putInt( "SplatSelectedProperties_errorscale", errorScale );
            prefs.putInt( "SplatSelectedProperties_linecolour", lineColour );
            prefs.putDouble( "SplatSelectedProperties_linestyle", lineStyle );
            prefs.putInt( "SplatSelectedProperties_linethickness", 
                          lineThickness );
            prefs.putInt( "SplatSelectedProperties_plotstyle", plotStyle );
            prefs.putDouble( "SplatSelectedProperties_pointsize", pointSize );
            prefs.putInt( "SplatSelectedProperties_pointtype", pointType );
       }

        public void restore()
        {
            alpha = prefs.getDouble
                ( "SplatSelectedProperties_alpha", 1.0 );
            errorColour = prefs.getInt
                ( "SplatSelectedProperties_errorcolour", Color.red.getRGB() );
            errorFrequency = prefs.getInt
                ( "SplatSelectedProperties_errorfrequency", 1 );
            errorScale = prefs.getInt
                ( "SplatSelectedProperties_errorscale", 1 );
            lineColour = prefs.getInt
                ( "SplatSelectedProperties_linecolour", Color.blue.getRGB() );
            lineStyle = prefs.getDouble
                ( "SplatSelectedProperties_linestyle", 1.0 );
            lineThickness = prefs.getInt
                ( "SplatSelectedProperties_linethickness", 1 );
            plotStyle = prefs.getInt
                ( "SplatSelectedProperties_plotstyle", SpecData.POLYLINE );
            pointSize = prefs.getDouble
                ( "SplatSelectedProperties_pointsize", 5.0 );
            pointType = prefs.getInt
                ( "SplatSelectedProperties_pointtype", 0 );
        }

        public void reset()
        {
            alpha = 1.0;
            errorColour = Color.red.getRGB();
            errorFrequency = 1;
            errorScale = 1;
            lineColour = Color.blue.getRGB();
            lineStyle = 1.0;
            lineThickness = 1;
            plotStyle = SpecData.POLYLINE;
            pointSize = 5.0;
            pointType = 0;
        }

        public void apply( SpecData spectrum ) 
        {
            // default properties - only some of them are used, the rest is customizable
        	DefaultRenderingProperties defaultProperties = DefaultRenderingPropertiesFactory.create(spectrum);
        	
        	spectrum.setAlphaComposite( alpha );
            spectrum.setErrorColour( errorColour );
            spectrum.setErrorFrequency( errorFrequency );
            spectrum.setErrorNSigma( errorScale );
            spectrum.setLineColour( lineColour );
            spectrum.setLineStyle( lineStyle );
            spectrum.setLineThickness( lineThickness );
//            System.out.println("and146: #3: " + plotStyle + " / " + spectrum.getPrefferedPlotType() + " [" + spectrum.getObjectType() + "]");
            spectrum.setPlotStyle( defaultProperties.getPlotStyle() );
            spectrum.setPointSize( defaultProperties.getPointSize() );
            spectrum.setPointType( defaultProperties.getPointType() );
        }
    }
}
