/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     24-OCT-2002 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.sog.photom;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Create a view/controller for an instance of an AnnulusPhotom
 * object. The AnnulusPhotom object is the current on stored in a
 * PhotomList.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class ApertureController extends JPanel implements ChangeListener
{
    /** The PhotomList */
    private PhotomList photomList;

    /** GridBagConstraints object. */
    private GridBagConstraints gbc = new GridBagConstraints();

    /** Label Insets */
    protected Insets labelInsets = new Insets( 10, 5, 5, 10 );

    /** Identifier */
    private JLabel identifier = new JLabel();

    /** X coordinate */
    private JLabel xcoord = new JLabel();

    /** Y coordinate */
    private JLabel ycoord = new JLabel();

    /** Magnitude */
    private JLabel mag = new JLabel();

    /** Magnitude Error */
    private JLabel magerr = new JLabel();

    /** Sum */
    private JLabel sum = new JLabel();

    /** Status */
    private JLabel status = new JLabel();

    /** Sky */
    private JLabel sky = new JLabel();

    /** Shape */
    private JLabel shape = new JLabel();

    /** Semi-major */
    private JLabel semimajor = new JLabel();

    /** Semi-minor */
    private JLabel semiminor = new JLabel();

    /** Position angle */
    private JLabel angle = new JLabel();

    /** Innerscale  */
    private SpinnerNumberModel innerscaleModel = null;

    /** Outerscale */
    private SpinnerNumberModel outerscaleModel = null;

    /**
     * Create an instance of this controller
     *
     * @param photomList the PhotomList of AnnulusPhotom objects that
     *        we may want to view and control.
     */
    public ApertureController( PhotomList photomList )
    {
        this.photomList = photomList;
        initUI();

        //  Register to read the details from the current aperture
        //  when that changes.
        photomList.addChangeListener( this );
    }

    /**
     * Create the basic set of value display elements and the
     * controller elements and add them to the page
     */
    private void initUI()
    {
        setLayout( new GridBagLayout() );
        setBorder( new TitledBorder( "Current aperture details" ) );

        //  Setup controls for interaction.
        innerscaleModel = new SpinnerNumberModel( 2.0, 1.0, 20.0, 0.05 );
        JSpinner innerscale = new JSpinner( innerscaleModel );
        innerscale.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchInnerscale();
                }
            });

        outerscaleModel = new SpinnerNumberModel( 3.0, 1.0, 20.0, 0.05 );
        JSpinner outerscale = new JSpinner( outerscaleModel );
        outerscale.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchOuterscale();
                }
            });

        //  Add labels for all fields.
        addLabel( "Identifier:", 0 );
        addLabel( "X coordinate:", 1 );
        addLabel( "Y coordinate:", 2 );
        addLabel( "Magnitude:", 3 );
        addLabel( "Magnitude error:", 4 );
        addLabel( "Sum in aperture:", 5 );
        addLabel( "Sky:", 6 );
        addLabel( "Shape:", 7 );
        addLabel( "Status:", 8 );
        addLabel( "Semimajor axis:", 9 );
        addLabel( "Semiminor axis:", 10 );
        addLabel( "Position angle:", 11 );
        addLabel( "Annulus inner scale:", 12 );
        addLabel( "Annulus outer scale:", 13 );

        gbc.insets = new Insets( 0, 0, 0, 0 );
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weighty = 0.0;
        gbc.weightx = 1.0;
        gbc.gridx = 1;

        //  Current row for adding components.
        int row = 0;

        //  Identifier
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( identifier, gbc );

        //  X coordinate (TODO: make AST aware/editable).
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( xcoord, gbc );

        //  Y coordinate.
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( ycoord, gbc );

        //  Magnitude.
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( mag, gbc );

        //  Magnitude error.
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( magerr, gbc );

        //  Sum.
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( sum, gbc );

        //  Sky.
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( sky, gbc );

        //  Shape.
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( shape, gbc );

        //  Status.
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( status, gbc );

        //  Semimajor
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( semimajor, gbc );

        //  Semiminor
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( semiminor, gbc );

        //  Angle.
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( angle, gbc );

        //  Annulus innerscale.
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( innerscale, gbc );

        //  Annulus outerscale.
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( outerscale, gbc );
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
     * Update the view to show the current aperture.
     */
    protected void updateView()
    {
        AnnulusPhotom current = (AnnulusPhotom) photomList.getCurrent();
        if ( current == null ) {
            reset();
        }
        else {
            identifier.setText( Integer.toString( current.getIdent() ) );
            xcoord.setText( Double.toString( current.getXcoord() ) );
            ycoord.setText( Double.toString( current.getYcoord() ) );
            mag.setText( Double.toString( current.getMagnitude() ) );
            magerr.setText( Double.toString( current.getMagnitudeError() ) );
            sum.setText( Double.toString( current.getSignal() ) );
            sky.setText( Double.toString( current.getSky() ) );
            shape.setText( Double.toString( current.getShape() ) );
            status.setText( current.getStatus() );
            semimajor.setText( Double.toString( current.getSemimajor() ) );
            semiminor.setText( Double.toString( current.getSemiminor() ) );
            angle.setText( Double.toString( current.getAngle() ) );

            innerscaleModel.setValue( new Double( current.getInnerscale() ) );
            outerscaleModel.setValue( new Double( current.getOuterscale() ) );
        }
    }

    /**
     * Reset all fields to clear.
     */
    public void reset()
    {
        identifier.setText( "" );
        xcoord.setText( "" );
        ycoord.setText( "" );
        mag.setText( "" );
        magerr.setText( "" );
        sum.setText( "" );
        sky.setText( "" );
        shape.setText( "" );
        status.setText( "?" );
        semimajor.setText( "" );
        semiminor.setText( "" );
        angle.setText( "" );
        innerscaleModel.setValue( new Double( 2.0 ) );
        outerscaleModel.setValue( new Double( 3.0 ) );
    }

    /**
     * Match the current figure of the list to the innerscale
     * displayed value.
     */
    protected void matchInnerscale()
    {
        AnnulusPhotom current = (AnnulusPhotom) photomList.getCurrent();
        if ( current != null ) {
            // Inhibit ChangeListener interface?
            current.setInnerscale(innerscaleModel.getNumber().doubleValue());
        }
    }

    /**
     * Match the current figure of the list to the outerscale
     * displayed value.
     */
    protected void matchOuterscale()
    {
        AnnulusPhotom current = (AnnulusPhotom) photomList.getCurrent();
        if ( current != null ) {
            current.setOuterscale(outerscaleModel.getNumber().doubleValue());
        }
    }

    //
    // Implement the ChangeListener interface.
    //
    public void stateChanged( ChangeEvent e )
    {
        updateView();
    }
}
