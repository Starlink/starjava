/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     04-FEB-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * SplatName is a composite widget for displaying and modifying the
 * various names associated with a spectrum. These are the short name,
 * full name (usually a disk file) and format (disk-file type or some
 * memory type). The short name can be modified, in which case this
 * will be reflected in the global list.
 *
 * @version $Id$
 * @author Peter W. Draper
 */
public class SplatName
    extends JPanel
{
    // Note this class is not used in {@link #SplatSelectedProperties} by
    // design (the widget layout and interaction with a JList are
    // complicating factors).

    /**
     *  Reference to GlobalSpecPlotList object.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    // UI components
    protected JLabel shortNameLabel = new JLabel();
    protected JLabel fullNameLabel = new JLabel();
    protected JLabel formatLabel = new JLabel();
    protected JTextField shortName = new JTextField();
    protected JLabel fullName = new JLabel();
    protected JLabel format = new JLabel();

    /**
     * The spectrum.
     */
    protected SpecData specData = null;

    /**
     *  Creates an instance with no spectrum.
     */
    public SplatName()
    {
        initUI();
    }

    /**
     *  Creates an instance with an initial spectrum.
     */
    public SplatName( SpecData specData )
    {
        initUI();
        setSpecData( specData );
    }

    /**
     *  Add all the components for display the spectrum names.
     */
    protected void initUI()
    {
        GridBagLayouter layouter =
            new GridBagLayouter( this, GridBagLayouter.SCHEME3 );

        // Short name label
        shortNameLabel.setAlignmentY( (float) 0.0 );
        shortNameLabel.setText( "Short name:" );
        layouter.add( shortNameLabel, false );

        layouter.add( Box.createHorizontalStrut( 5 ), false );

        // Short name field
        layouter.add( shortName, true );
        shortName.setToolTipText
            ( "Symbolic name of spectrum, press return to accept edits" );

        //  The short name field can be editted.
        shortName.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    updateShortName();
                }
            });

        // Full name label
        fullNameLabel.setAlignmentY( (float) 0.0 );
        fullNameLabel.setText( "Full name:" );
        layouter.add( fullNameLabel, false );
        layouter.add( Box.createHorizontalStrut( 5 ), false );

        // Full name field
        layouter.add( fullName, true );
        fullName.setToolTipText( "Full name of spectrum (usually filename)" );

        // Data format label
        formatLabel.setText( "Format:" );
        layouter.add( formatLabel, false );
        layouter.add( Box.createHorizontalStrut( 5 ), false );

        // Data format field
        layouter.add( format, true );
        format.setToolTipText( "Data type used for storage of spectrum" );
    }

    /**
     * Set the spectrum that we're showing the names of.
     */
    public void setSpecData( SpecData specData )
    {
        this.specData = specData;
        update();
    }

    /**
     * Update all the widgets to show the spectrum names.
     */
    public void update()
    {
        if ( specData != null ) {
            shortName.setText( specData.getShortName() );
            fullName.setText( specData.getFullName() );
            format.setText( specData.getDataFormat() );
        }
        else {
            shortName.setText( "" );
            fullName.setText( "" );
            format.setText( "" );
        }
    }

    /**
     *  Change the short name used for this spectrum.
     */
    public void updateShortName()
    {
        if ( specData != null ) {
            String name = shortName.getText();
            int index = globalList.getSpectrumIndex( specData );
            if ( name != null && ! "".equals( name.trim() ) ) {
                globalList.setShortName( index, name );
            }
            else {
                //  Blank name, so reset to previous name.
                globalList.setShortName( index,
                                         globalList.getShortName( index ) );
            }
        }
    }
}
