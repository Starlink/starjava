/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     23-OCT-2002 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.sog.photom;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import jsky.util.Preferences;
import jsky.util.gui.LookAndFeelMenu;

import uk.ac.starlink.sog.SOGNavigatorImageDisplay;

/**
 * Top-level window for an aperture photometry toolbox.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AperturePhotometryFrame
    extends JFrame
{
    //  The panel, deal with this in the JSky standard fashion.
    private AperturePhotometry panel;

    /**
     * Create a top level window.
     */
    public AperturePhotometryFrame( SOGNavigatorImageDisplay imageDisplay )
    {
        super( "Aperture Photometry" );
        panel = new AperturePhotometry( this, imageDisplay );
        getContentPane().add( panel, BorderLayout.CENTER );
        pack();
        Preferences.manageLocation( this );
        setVisible( true );
        setDefaultCloseOperation( HIDE_ON_CLOSE );

        // include this top level window in any future look and feel changes
        LookAndFeelMenu.addWindow( this );
    }

    /** Return the internal panel object */
    public AperturePhotometry getAperturePhotometry()
    {
        return panel;
    }

    public static void main( String[] args )
    {
        JFrame df = new JFrame();
        SOGNavigatorImageDisplay d = 
            new SOGNavigatorImageDisplay( df.getContentPane() );
        df.getContentPane().add( d );
        AperturePhotometryFrame f = new AperturePhotometryFrame( d );

        AperturePhotometry p = f.getAperturePhotometry();
        PhotomList l = p.getPhotomList();
        AnnulusPhotom a1 = new AnnulusPhotom();
        a1.setIdent( 1 );
        l.add( a1 );
        AnnulusPhotom a2 = new AnnulusPhotom();
        a2.setIdent( 2 );
        l.add( a2 );
        AnnulusPhotom a3 = new AnnulusPhotom();
        a3.setIdent( 3 );
        l.add( a3 );

        f.setVisible( true );
        df.setVisible( true );

        f.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

        // Exit when window is closed.
        WindowListener closer = new WindowAdapter() {
                public void windowClosing( WindowEvent e ) {
                    System.exit( 1 );
                }
            };
        f.addWindowListener( closer );
    }
}
