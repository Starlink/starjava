/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     3-MAR-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.MathMap;
import uk.ac.starlink.ast.WinMap;
import uk.ac.starlink.ast.gui.DecimalField;
import uk.ac.starlink.ast.gui.ScientificFormat;
import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * Modify or generate a new system of coordinates for a
 * spectrum. Various options are available, including a linear
 * transformation (offset and scale) and general one (based on
 * MathMap).
 * <p>
 * A general transformation is assumed not invertable and
 * mean that their AST mappings will be replaced by a look-up-table.
 * If this choice is popular this decision may be re-visited.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class CoordinateGenerator
    extends JPanel
{
    /**
     * The spectrum we're about to modify.
     */
    protected EditableSpecData specData = null;

    /**
     * The listener for our change events?
     */
    protected CoordinateGeneratorListener listener = null;

    /**
     * The Tabbed pane.
     */
    protected JTabbedPane tabbedPane = null;

    /**
     * The linear transformation scale.
     */
    protected DecimalField scale = null;

    /**
     * The linear transformation offset.
     */
    protected DecimalField offset = null;

    // Known expression templates.
    static final String[][] templates = {
        { "Redshift (z)", "coord*(z+1)",
                          "z", "0.0" },
        { "Redshift (v km/s)", "coord*(1.0+v/c)",
                               "c", "299792.458",
                               "v", "0.0" },
        { "Redshift relativisitic (v km/s)", "coord*sqrt((c+v)/(c-v))",
                                             "c", "299792.458",
                                             "v", "0.0" },
        { "Blueshift (z)", "coord/(z+1)",
                           "z", "0.0" },
        { "Blueshift (v km/s)", "coord/(1.0+v/c)",
                                "c", "299792.458",
                                "v", "0.0" },
        { "Blueshift relativistic (v km/s)", "coord/sqrt((c+v)/(c-v))",
                                             "c", "299792.458",
                                             "v", "0.0" },
        { "Log (natural)", "log(coord)" },
        { "Log (base 10)", "log10(coord)" },
    };

    /**
     * Create an instance.
     */
    public CoordinateGenerator( EditableSpecData specData,
                                CoordinateGeneratorListener listener )
    {
        initUI();
        setListener( listener );

        //  Set the EditableSpecData we're taking values from. Note this must
        //  happen after the interface is generated.
        setEditableSpecData( specData );
        setVisible( true );
    }

    /**
     * Initialise the user interface.
     */
    protected void initUI()
    {
        setLayout( new BorderLayout() );
        tabbedPane = new JTabbedPane();
        addLinear( tabbedPane );
        addGeneral( tabbedPane );
        add( tabbedPane, BorderLayout.CENTER );
    }

    /**
     * Add any pre-defined functions to a given menu.
     */
    public void addPreDefined( JMenu function )
    {
        for ( int i = 0; i < templates.length; i++ ) {
            function.add( new TemplateAction( i ) );
        }
    }

    /**
     * Set the spectrum we're working on.
     */
    public void setEditableSpecData( EditableSpecData specData )
    {
        this.specData = specData;
    }

    /**
     * Set the listener for our results.
     */
    public void setListener( CoordinateGeneratorListener listener )
    {
        this.listener = listener;
    }

    protected JRadioButton useCoords = null;
    protected JRadioButton useIndices = null;

    /**
     * Add a page of controls for the linear transformation option.
     */
    protected void addLinear( JTabbedPane pane )
    {
        JPanel panel = new JPanel();

        // Configuration
        useCoords = new JRadioButton( "Transform coordinates" );
        useIndices = new JRadioButton( "Transform indices" );
        ButtonGroup useGroup = new ButtonGroup();
        useGroup.add( useIndices );
        useGroup.add( useCoords );
        useCoords.setSelected( true );

        useCoords.setToolTipText
            ( "Result is transformation of the existing coordinates" );
        useIndices.setToolTipText
            ( "Result is transformation of indices (start at 0)");

        JLabel scaleLabel = new JLabel( "Scale Factor:   " );
        ScientificFormat scientificFormat = new ScientificFormat();
        scale = new DecimalField( 1.0, 5, scientificFormat );
        scale.setToolTipText( "Amount to scale coordinates or indices" );

        JLabel offsetLabel = new JLabel( "Offset:   " );
        scientificFormat = new ScientificFormat();
        offset = new DecimalField( 0.0, 5, scientificFormat );

        offset.setToolTipText( "Offset for new coordinates" );

        //  Layout
        GridBagLayouter layouter = new GridBagLayouter( panel );
        layouter.add( useCoords, false );
        layouter.add( useIndices, true );
        layouter.add( scaleLabel, false );
        layouter.add( scale, true );
        layouter.add( offsetLabel, false );
        layouter.add( offset, true );
        layouter.eatSpare();

        panel.setBorder( BorderFactory.createTitledBorder
                         ("Linear transformation of coordinate or indices" ));

        pane.add( "Linear", panel );
    }

    protected JTextField[] itemNames = null;
    protected JTextField[] itemValues = null;
    protected JLabel mainName = null;
    protected JTextField mainValue = null;
    protected static final int NAMECOUNT = 10;

    /**
     * Add a page of controls for the general transformation option.
     *
     * TODO: add log as known general transform.
     */
    protected void addGeneral( JTabbedPane pane )
    {
        JPanel panel = new JPanel();
        add( panel, BorderLayout.CENTER );

        // Configuration
        itemNames = new JTextField[NAMECOUNT];
        itemValues = new JTextField[NAMECOUNT];
        mainName = new JLabel( "y" );
        mainValue = new JTextField();
        for ( int i = 0; i < NAMECOUNT; i++ ) {
            itemNames[i] = new JTextField();
            itemValues[i]= new JTextField();
        }

        //  Layout
        GridBagLayouter layouter = 
            new GridBagLayouter( panel, GridBagLayouter.SCHEME2 );

        layouter.add( mainName, false );
        layouter.add( new JLabel( " = " ), false );
        layouter.add( mainValue, true );

        for ( int i = 0; i < NAMECOUNT; i++ ) {
            layouter.add( itemNames[i], false );
            layouter.add( new JLabel( " = " ), false );
            layouter.add( itemValues[i], true );
        }
        layouter.eatSpare();

        panel.setBorder( BorderFactory.createTitledBorder
                         ( "Symbolic transformation:" ) );

        pane.add( "General", panel );
    }

    /**
     * Perform the new coordinate generation.
     */
    public void generate()
    {
        //  See what type of transformation we have and start the
        //  appropriate method.
        switch ( tabbedPane.getSelectedIndex() ) {
            case 0: {
                // Linear
                generateLinear();
            }
            break;
            case 1: {
                // General
                generateGeneral();
            }
            break;
        }
    }

    /**
     * Generate new coordinates for linear controls.
     */
    protected void generateLinear()
    {
        //  Create a WinMap for the linear transformation.
        double dscale = scale.getDoubleValue();
        double doffset = offset.getDoubleValue();
        double[] ina = new double[1];
        double[] inb = new double[1];
        double[] outa = new double[1];
        double[] outb = new double[1];
        ina[0] = 0.0;
        inb[0] = 1.0;
        outa[0] = ina[0] * dscale + doffset;
        outb[0] = inb[0] * dscale + doffset;
        WinMap winMap = new WinMap( 1, ina, inb, outa, outb );

        //  Get a copy of the current FrameSet (want to modify
        //  independently to existing one).
        FrameSet frameSet = (FrameSet) specData.getFrameSet().copy();

        //  Get a copy of the current Frame (to keep units etc.).
        Frame frame = (Frame)
            frameSet.getFrame( FrameSet.AST__CURRENT ).copy();

        if ( useCoords.isSelected() ) {

            // Transformation of existing coordinates.
            frameSet.addFrame( FrameSet.AST__CURRENT, winMap, frame );
        }
        else {

            // New coordinates. Use mapping to base coordinates.
            frameSet.addFrame( FrameSet.AST__BASE, winMap, frame );
        }

        //  Reset spectrum to use these coordinates.
        listener.changeFrameSet( frameSet );
    }

    /**
     * Modify the coordinates using a general transformation of either
     * the indices or existing coordinates. Must use a look-up-table
     * as we don't know if the transformation is invertable.
     */
    protected void generateGeneral()
    {
        if ( "".equals( mainValue.getText() ) ) {
            JOptionPane.showMessageDialog( this,
                                           "No main expression is defined",
                                           "No expressions",
                                           JOptionPane.WARNING_MESSAGE );
            return;
        }

        // Count any sub-expressions.
        int count = 0;
        for ( int i = 0; i < NAMECOUNT; i++ ) {
            if ( ! "".equals( itemNames[i].getText() ) ) {
                count++;
            }
        }
        String[] fwd = new String[count+1];
        int j = 0;
        for ( int i = count; i >= 0; i-- ) {
            if ( ! "".equals( itemNames[i].getText() ) ) {
                fwd[j++] = itemNames[i].getText() + " = " +
                    itemValues[i].getText();
            }
        }
        fwd[j++] = "y = " + mainValue.getText();
        double[] newcoords = doAstMathMap( fwd );

        //  Set these as the new coordinates.
        if ( newcoords != null ) {
            listener.acceptGeneratedCoords( newcoords );
        }
    }

    /**
     * Apply a general transformation to either the current
     * coordinates or indices.
     */
    protected double[] doAstMathMap( String[] fwd )
    {
        double[] coords = specData.getXData();
        double[] indices = new double[coords.length];
        for ( int i = 0; i< coords.length; i++ ) {
            indices[i] = (double) i;
        }

        //  Available data to transform. XXX could cache index vector
        //  and/or check if it or the coords are needed by looking for
        //  tokens in the expression strings.
        String inv[] = new String[2];
        inv[0] = "coord";
        inv[1] = "index";

        //  Create the MathMap with coord and index mapping to
        //  newvalue.
        try {
            MathMap map = new MathMap( inv.length, 1, fwd, inv );

            //  Do the transform.
            double[][] in = new double[inv.length][];
            in[0] = coords;
            in[1] = indices;
            double[][] result = map.tranP( coords.length, inv.length,
                                           in, true, 1 );
            return result[0];
        }
        catch (AstException e) {
            ErrorDialog.showError( this, e );
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Inner class defining Action for showing a template.
     */
    protected class TemplateAction extends AbstractAction
    {
        private int index = 0;
        public TemplateAction( int index )
        {
            super( templates[index][0] );
            putValue( SHORT_DESCRIPTION, templates[index][1] );
            this.index = index;
        }

        /**
         * Respond to actions from the buttons.
         */
        public void actionPerformed( ActionEvent ae )
        {
            applyTemplate( index );
        }
    }

    /**
     * Clear the values.
     */
    protected void clearValues()
    {
        mainValue.setText( "" );
        for ( int i = 0; i < NAMECOUNT; i++ ) {
            itemValues[i].setText( "" );
        }
    }

    /**
     * Clear the names.
     */
    protected void clearNames()
    {
        for ( int i = 0; i < NAMECOUNT; i++ ) {
            itemNames[i].setText( "" );
        }
    }

    /**
     * Set the fields to values of a template.
     */
    protected void applyTemplate( int index )
    {
        clearValues();
        clearNames();

        mainValue.setText( templates[index][1] );
        int i = 0;
        for ( int j = 2; j < templates[index].length; j += 2 ) {
            itemNames[i].setText( templates[index][j] );
            itemValues[i].setText( templates[index][j+1] );
            i++;
        }
    }
}
