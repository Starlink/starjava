/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     3-MAR-2003 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import java.text.DecimalFormat;

import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.WinMap;
import uk.ac.starlink.ast.gui.DecimalField;
import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.splat.util.Utilities;


/**
 * Modify or generate a new system of coordinates for a
 * spectrum. Various options are available, including a linear
 * transformation (offset and scale) and general one (based on
 * MathMap).
 * <p>
 * A general transformation may or may not be invertable. If it isn't
 * then the current AST mappings will be replaced by a look-up-table.
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
        DecimalFormat decimalFormat = new DecimalFormat();
        scale = new DecimalField( 1.0, 5, decimalFormat );
        scale.setToolTipText( "Amount to scale coordinates or indices" );

        JLabel offsetLabel = new JLabel( "Offset:   " );
        decimalFormat = new DecimalFormat();
        offset = new DecimalField( 0.0, 5, decimalFormat );

        offset.setToolTipText( "Offset for new coordinates" );

        //  Layout
        panel.setLayout( new GridBagLayout() );
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        panel.add( useCoords, gbc );
        gbc.anchor = GridBagConstraints.WEST;
        panel.add( useIndices, gbc );
        eatLine( panel, gbc );

        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        panel.add( scaleLabel, gbc );
        gbc.anchor = GridBagConstraints.WEST;
        panel.add( scale, gbc );
        eatLine( panel, gbc );

        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        panel.add( offsetLabel, gbc );
        gbc.anchor = GridBagConstraints.WEST;
        panel.add( offset, gbc );
        eatLine( panel, gbc );

        //  Eat remaining space to keep packed at top.
        eatSpare( panel, gbc );

        panel.setBorder( BorderFactory.createTitledBorder
                         ("Linear transformation of coordinate or indices" ));

        pane.add( "Linear", panel );
    }
    
    /**
     * Add a page of controls for the general transformation option.
     */
    protected void addGeneral( JTabbedPane pane )
    {
        
    }
    
    /**
     * Eat to end of current line using GridBagLayout.
     */
    private void eatLine( JPanel panel, GridBagConstraints gbc )
    {
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add( Box.createHorizontalGlue(), gbc );
    }
    
    /**
     * East spare space at bottom of panel using GridBagLayout.
     */
    private void eatSpare( JPanel panel, GridBagConstraints gbc )
    {
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH    ;
        panel.add( Box.createVerticalGlue(), gbc );
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
        listener.generatedCoordinates();
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

        //  Get the current FrameSet.
        FrameSet frameSet = specData.getFrameSet();

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
        frame.annul();
        winMap.annul();

        //  Reset spectrum to use these coordinates.
        try {
            specData.setFrameSet( frameSet );
            listener.generatedCoordinates();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void generateGeneral()
    {
        //  ... XXX todo.
    }
}
