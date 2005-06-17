/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     17-JUN-2005 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.iface;

import java.awt.event.ActionEvent;
import java.awt.Dimension;
import java.awt.BorderLayout;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.iface.images.ImageHolder;

/**
 * Interactively display the statistics of a region of the current
 * spectrum. The statistics are, sum, mean, standard deviation, median, mode,
 * min, max and number of pixels used.
 *
 * @author Peter W. Draper
 * @version $Id$
 */      
public class StatsFrame
    extends JFrame
{
    /** Content pane of frame */
    protected JPanel contentPane = null;

    /** The PlotControl that is displaying the current spectrum */
    protected PlotControl plot = null;

    /** The global list of spectra and plots */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * Create an instance.
     */
    public StatsFrame( PlotControl plot )
    {
        contentPane = (JPanel) getContentPane();
        contentPane.setLayout( new BorderLayout() );
        setPlot( plot );
        initUI();
        initFrame();
    }

    /**
     * Get the PlotControl that we are using.
     *
     * @return the PlotControl
     */
    public PlotControl getPlot()
    {
        return plot;
    }

    /**
     * Set the PlotControlFrame that has the spectrum that we are to
     * process.
     *
     * @param plot the PlotControl reference.
     */
    public void setPlot( PlotControl plot )
    {
        this.plot = plot;
    }

    /**
     * Initialise the main part of the user interface.
     */
    protected void initUI()
    {
        //  Menubar.
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar( menuBar );
        
        //  File menu.
        JMenu fileMenu = new JMenu( "File" );
        menuBar.add( fileMenu );

        //  Action bar for buttons.
        JPanel actionBar = new JPanel();

        //  Images.
        ImageIcon closeImage =
            new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );

        //  Add an action to close the window.
        LocalAction closeAction = new LocalAction( LocalAction.CLOSE, 
                                                   "Close", closeImage,
                                                   "Close window" );
        fileMenu.add( closeAction );
        JButton closeButton = new JButton( closeAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( closeButton );
        
        actionBar.add( Box.createGlue() );
        contentPane.add( actionBar, BorderLayout.SOUTH );

        //  Add the help menu.
        HelpFrame.createHelpMenu( "stats-window", "Help on window",
                                  menuBar, null );
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Region statistics" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        setSize( new Dimension( 400, 300 ) );
        setVisible( true );
    }

    /**
     * Close the window. Delete any local resources.
     */
    protected void closeWindowEvent()
    {
        this.dispose();
    }

    /**
     * Inner class defining all local Actions.
     */
    protected class LocalAction
        extends AbstractAction
    {
        
        //  Types of action.
        public static final int CLOSE = 0;

        //  The type of this instance.
        private int actionType = CLOSE;

        public LocalAction( int actionType, String name, Icon icon )
        {
            super( name, icon );
            this.actionType = actionType;
        }

        public LocalAction( int actionType, String name, Icon icon, 
                            String help )
        {
            this( actionType, name, icon );
            putValue( SHORT_DESCRIPTION, help );
        }

        public void actionPerformed( ActionEvent ae )
        {
            switch ( actionType )
            {
               case CLOSE: {
                   closeWindowEvent();
                   break;
               }
            }
        }
    }
}
