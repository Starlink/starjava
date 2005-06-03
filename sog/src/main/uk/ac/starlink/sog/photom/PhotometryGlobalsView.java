/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-NOV-2002 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.sog.photom;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.ast.gui.DecimalField;
import uk.ac.starlink.ast.gui.ScientificFormat;

/**
 * Create a page of controls for setting the global measurements
 * parameters associated with the photometry of a set of objects. The
 * most useful global parameter is the magnitude zero point. Others
 * are specific to the AUTOPHOTOM program provided by the PhotomWS
 * service. 
 * <p>
 * The controls are a view/controller for a PhotometryGlobalsModel
 * object that provides persistence facilities (i.e. can be written
 * and restored from an XML representation).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PhotometryGlobalsView
    extends JPanel 
    implements ChangeListener
{
    /** The model of values that are viewed and controlled here */
    private PhotometryGlobals model = null;

    /** GridBagConstraints object. */
    private GridBagConstraints gbc = new GridBagConstraints();

    /** Label Insets */
    protected Insets labelInsets = new Insets( 10, 5, 5, 10 );

    /** Zero point value */
    private DecimalField zeroPoint = 
        new DecimalField( 50.0, 10, new ScientificFormat() );

    /** Centroid positions */
    private JCheckBox centroid = new JCheckBox();

    /**
     * Create an instance of this controller
     */
    public PhotometryGlobalsView()
    {
        this( new PhotometryGlobals() );
    }

    /**
     * Create an instance of this controller
     */
    public PhotometryGlobalsView( PhotometryGlobals model )
    {
        super();
        initUI();
        setPhotometryGlobals( model );
    }

    /**
     * Create the basic set of value display elements and the
     * controller elements and add them to the page
     */
    private void initUI()
    {
        setLayout( new GridBagLayout() );
        setBorder( new TitledBorder( "Global measurement parameters" ) );

        //  Setup controls for interaction.
        zeroPoint.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchZeroPoint();
                }
            });

        centroid.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchCentroid();
                }
            });


        //  Add labels for all fields.
        addLabel( "Zero point:", 0 );
        addLabel( "Centroid:", 1 );

        gbc.insets = new Insets( 0, 0, 0, 0 );
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weighty = 0.0;
        gbc.weightx = 1.0;
        gbc.gridx = 1;

        //  Current row for adding components.
        int row = 0;

        //  Zero point
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( zeroPoint, gbc );

        //  Centroid
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( centroid, gbc );

        //  Eat up all spare vertical space (pushes widgets to top).
        Component filly2 = Box.createVerticalStrut( 5 );
        gbc.gridy = row++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        add( filly2, gbc );
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
     * Update the view to show the current model.
     */
    protected void updateView()
    {
        zeroPoint.setDoubleValue( model.getZeroPoint() );
        centroid.setSelected( model.getCentroid() );
    }

    /**
     * Reset all fields.
     */
    public void reset()
    {
        model.reset();
    }

    /**
     * Match the model zero point to that currently displayed.
     */
    protected void matchZeroPoint()
    {
        model.setZeroPoint( zeroPoint.getDoubleValue() );
    }

    /**
     * Match the model centroid to the currently displayed.
     */
    protected void matchCentroid()
    {
        model.setCentroid( centroid.isSelected() );
    }

    /**
     * Set the PhotometryGlobals object used as a model.
     */
    public void setPhotometryGlobals( PhotometryGlobals model ) 
    {
        this.model = model;
        updateView();
    }

    /**
     * Get the PhotometryGlobals object used as a model.
     */
    public PhotometryGlobals getPhotometryGlobals() 
    {
        return model;
    }

    //
    // Implement the ChangeListener interface.
    //

    /** 
     * Executed when there are changes in the model that should be
     * displayed.
     */
    public void stateChanged( ChangeEvent e )
    {
        updateView();
    }
}
