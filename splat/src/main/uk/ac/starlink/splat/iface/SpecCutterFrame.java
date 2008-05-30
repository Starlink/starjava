/*
 * Copyright (C) 2001-2003 Central Laboratory of the Research Councils
 * Copyright (C) 2008 Science and Technology Facilities Council.
 *
 *  History:
 *     15-JUN-2001 (Peter W. Draper):
 *       Original version.
 *     28-JAN-2003 (Peter W. Draper):
 *       Added remove facilities.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;

/**
 * Provides a toolbox with number of ways to cut out or remove parts
 * of a spectrum. The result becomes a new memory spectrum on the global
 * list.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see SpecCutter
 */
public class SpecCutterFrame
    extends JFrame
{
    /**
     * List of spectra that we have created.
     */
    protected ArrayList localList = new ArrayList( 5 );

    /**
     * Content pane of frame.
     */
    protected JPanel contentPane = null;

    /**
     *  Menubar and various menus and items that it contains.
     */
    protected JMenuBar menuBar = new JMenuBar();
    protected JMenu fileMenu = new JMenu();
    protected JMenu rangeMenu = new JMenu();
    protected JMenuItem closeFileMenu = new JMenuItem();

    /**
     *  The PlotControl that is displaying the current spectrum.
     */
    protected PlotControl plot = null;

    /**
     *  Ranges of the spectrum.
     */
    protected XGraphicsRangesView rangeList = null;

    /**
     *  Reference to GlobalSpecPlotList object.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * The SpecCutter instance.
     */
    protected SpecCutter specCutter = SpecCutter.getInstance();

    /**
     *  The current spectrum last removed, if done.
     */
    protected SpecData removedCurrentSpectrum = null;

    /**
     * Create an instance.
     */
    public SpecCutterFrame( PlotControl plot )
    {
        contentPane = (JPanel) getContentPane();
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
        //  The layout is a BorderLayout with the list of regions in
        //  the center and the toolbox below this.
        contentPane.setLayout( new BorderLayout() );

        //  Add the menuBar.
        setJMenuBar( menuBar );

        //  Add the list of regions.
        rangeList = new XGraphicsRangesView( plot.getPlot(), rangeMenu );
        contentPane.add( rangeList, BorderLayout.CENTER );

        //  Three action bars use BoxLayout and are placed at the south.
        JPanel actionBar = new JPanel( new BorderLayout() );
        JPanel actionBar12 = new JPanel( new BorderLayout() );
        JPanel actionBar1 = new JPanel();
        JPanel actionBar2 = new JPanel();
        JPanel actionBar3 = new JPanel();
        actionBar.add( actionBar12, BorderLayout.NORTH );
        actionBar.add( actionBar3, BorderLayout.SOUTH );
        actionBar12.add( actionBar1, BorderLayout.NORTH );
        actionBar12.add( actionBar2, BorderLayout.SOUTH );
        actionBar1.setLayout( new BoxLayout( actionBar1, BoxLayout.X_AXIS ) );
        actionBar2.setLayout( new BoxLayout( actionBar2, BoxLayout.X_AXIS ) );
        actionBar3.setLayout( new BoxLayout( actionBar3, BoxLayout.X_AXIS ) );
        actionBar1.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );
        actionBar2.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );
        actionBar3.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );
        contentPane.add( actionBar, BorderLayout.SOUTH );

        //  Get icons.
        ImageIcon closeImage = new ImageIcon(
            ImageHolder.class.getResource( "close.gif" ) );
        ImageIcon readImage = new ImageIcon(
            ImageHolder.class.getResource( "read.gif" ) );
        ImageIcon resetImage = new ImageIcon(
            ImageHolder.class.getResource( "reset.gif" ) );
        ImageIcon cutterImage = new ImageIcon(
            ImageHolder.class.getResource( "cutter.gif" ) );
        ImageIcon removeImage = new ImageIcon(
            ImageHolder.class.getResource( "erase.gif" ) );
        ImageIcon interpImage = new ImageIcon(
            ImageHolder.class.getResource( "lininterp.gif" ) );

        //  Create the File menu.
        fileMenu.setText( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );

        //  Add action to do read a list of ranges from disk file.
        Action readAction = rangeList.getReadAction("Read ranges", readImage);
        fileMenu.add( readAction );

        //  Add action to cut out all regions.
        CutAction cutAction =
            new CutAction( "Cut", cutterImage );
        fileMenu.add( cutAction ).setMnemonic( KeyEvent.VK_U );

        JButton cutButton = new JButton( cutAction );
        actionBar1.add( Box.createGlue() );
        actionBar1.add( cutButton );
        cutButton.setToolTipText
            ( "Cut out all regions from the current spectrum" );

        //  Add action to cut out the selected regions.
        CutSelectedAction cutSelectedAction =
            new CutSelectedAction( "Cut Selected", cutterImage );
        fileMenu.add( cutSelectedAction ).setMnemonic( KeyEvent.VK_S );

        JButton cutSelectedButton = new JButton( cutSelectedAction );
        actionBar1.add( Box.createGlue() );
        actionBar1.add( cutSelectedButton );
        cutSelectedButton.setToolTipText
            ( "Cut out the selected regions from the current spectrum" );

        //  Add action to remove all regions.
        RemoveAction removeAction =
            new RemoveAction( "Remove", removeImage );
        fileMenu.add( removeAction ).setMnemonic( KeyEvent.VK_M );

        JButton removeButton = new JButton( removeAction );
        actionBar1.add( Box.createGlue() );
        actionBar1.add( removeButton );
        removeButton.setToolTipText
            ( "Remove all regions from the current spectrum" );

        //  Add action to just remove the selected regions.
        RemoveSelectedAction removeSelectedAction =
            new RemoveSelectedAction( "Remove Selected", removeImage );
        fileMenu.add( removeSelectedAction ).setMnemonic( KeyEvent.VK_O );

        JButton removeSelectedButton = new JButton( removeSelectedAction );
        actionBar1.add( Box.createGlue() );
        actionBar1.add( removeSelectedButton );
        removeSelectedButton.setToolTipText
            ( "Remove the selected regions from the current spectrum" );

        actionBar1.add( Box.createGlue() );

        //  Add action to interpolate all regions.
        InterpAction interpAction =
            new InterpAction( "Interpolate", interpImage );
        fileMenu.add( interpAction ).setMnemonic( KeyEvent.VK_J );

        JButton interpButton = new JButton( interpAction );
        actionBar2.add( Box.createGlue() );
        actionBar2.add( interpButton );
        interpButton.setToolTipText
            ( "Interpolate all regions from the current spectrum" );

        //  Add action to interpolate all regions and replace existing
        //  spectrum.
        InterpReplaceAction interpReplaceAction =
            new InterpReplaceAction( "Interpolate (Replace)", interpImage );
        fileMenu.add( interpReplaceAction ).setMnemonic( KeyEvent.VK_K );

        JButton interpReplaceButton = new JButton( interpReplaceAction );
        actionBar2.add( Box.createGlue() );
        actionBar2.add( interpReplaceButton );
        interpReplaceButton.setToolTipText
            ( "Interpolate all regions from the current spectrum and replace"
              + " current spectrum" );

        //  Add action to remove and interpolate the selection regions.
        InterpSelectedAction interpSelectedAction =
            new InterpSelectedAction( "Interpolate Selected", interpImage );
        fileMenu.add( interpSelectedAction ).setMnemonic( KeyEvent.VK_I );

        JButton interpSelectedButton = new JButton( interpSelectedAction );
        actionBar2.add( Box.createGlue() );
        actionBar2.add( interpSelectedButton );
        interpSelectedButton.setToolTipText
            ( "Interpolate the selected regions from the current spectrum" );

        actionBar2.add( Box.createGlue() );

        //  Add action to reset undo the last replace.
        ResetReplaceAction resetReplaceAction = 
            new ResetReplaceAction( "Reset (Replace)", resetImage );
        fileMenu.add( resetReplaceAction ).setMnemonic( KeyEvent.VK_P );

        JButton resetReplaceButton = new JButton( resetReplaceAction );
        actionBar3.add( Box.createGlue() );
        actionBar3.add( resetReplaceButton );
        resetReplaceButton.setToolTipText( "Reset last (Replace) action" );

        //  Add action to reset all values.
        ResetAction resetAction = new ResetAction( "Reset", resetImage );
        fileMenu.add( resetAction ).setMnemonic( KeyEvent.VK_R );

        JButton resetButton = new JButton( resetAction );
        actionBar3.add( Box.createGlue() );
        actionBar3.add( resetButton );
        resetButton.setToolTipText( "Clear all produced spectra and ranges" );

        //  Add an action to close the window.
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );

        JButton closeButton = new JButton( closeAction );
        actionBar3.add( Box.createGlue() );
        actionBar3.add( closeButton );
        closeButton.setToolTipText( "Close window" );

        actionBar3.add( Box.createGlue() );

        // Now add the Ranges menu.
        rangeMenu.setText( "Ranges" );
        rangeMenu.setMnemonic( KeyEvent.VK_R );
        menuBar.add( rangeMenu );

        //  Add the help menu.
        HelpFrame.createHelpMenu( "cutter-window", "Help on window",
                                  menuBar, null );
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Cut/Remove regions from a spectrum" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        setSize( new Dimension( 550, 400 ) );
        setVisible( true );
    }

    /**
     *  Do the cut, to either all the ranges or just the selected
     *  ones.
     *
     *  @param selected true if we should just cut to selected ranges.
     */
    public void cut( boolean selected )
    {
        //  Extract all ranges and obtain current spectrum.
        SpecData currentSpectrum = plot.getCurrentSpectrum();
        if ( currentSpectrum == null ) {
            return; // No spectrum available, so do nothing.
        }

        double[] ranges = rangeList.getRanges( selected );
        if ( ranges.length == 0 ) {
            return; // No ranges, so nothing to do.
        }

        //  Perform the cut operation and add the spectrum to the
        //  global list.
        SpecData newSpec =
            specCutter.cutRanges( currentSpectrum, ranges );

        display( newSpec, false );
    }

    /**
     *  Do the remove, to either all the ranges or just the selected
     *  ones.
     *
     *  @param selected true if we should just remove selected ranges.
     */
    public void remove( boolean selected )
    {
        //  Extract all ranges and obtain current spectrum.
        SpecData currentSpectrum = plot.getCurrentSpectrum();
        if ( currentSpectrum == null ) {
            return; // No spectrum available, so do nothing.
        }

        double[] ranges = rangeList.getRanges( selected );
        if ( ranges.length == 0 ) {
            return; // No ranges, so nothing to do.
        }

        //  Perform the cut operation and add the spectrum to the
        //  global list.
        SpecData newSpec =
            specCutter.deleteRanges( currentSpectrum, ranges );

        display( newSpec, false );
    }

    /**
     *  Do the interpolation, to either all the ranges or just the selected
     *  ones.
     *
     *  @param selected if we should just interpolate selected ranges.
     *  @param replace if true remove current spectrum and replace with new
     *                 one.
     */
    public void interpolate( boolean selected, boolean replace )
    {
        //  Extract all ranges and obtain current spectrum.
        SpecData currentSpectrum = plot.getCurrentSpectrum();
        if ( currentSpectrum == null ) {
            return; // No spectrum available, so do nothing.
        }

        double[] ranges = rangeList.getRanges( selected );
        if ( ranges.length == 0 ) {
            return; // No ranges, so nothing to do.
        }

        //  Perform the interpolation operation and add the spectrum to the
        //  global list.
        SpecData newSpec =
            specCutter.interpRanges( currentSpectrum, ranges );

        display( newSpec, replace );
    }

    /**
     *  Display a new spectrum and add it to the local list, replacing current
     *  spectrum if requested.
     */
    protected void display( SpecData newSpec, boolean replace )
    {
        localList.add( newSpec );
        try {
            SpecData currentSpectrum = null;
            if ( replace ) {
                currentSpectrum = plot.getCurrentSpectrum();
            }
            plot.addSpectrum( newSpec );
            if ( replace ) {
                plot.removeSpectrum( currentSpectrum );

                //  This becomes spectrum we back out to.
                removedCurrentSpectrum = currentSpectrum;
            }

            //  Default line is gray.
            globalList.setKnownNumberProperty
                ( newSpec, SpecData.LINE_COLOUR,
                  new Integer( Color.darkGray.getRGB() ) );
        }
        catch (SplatException e) {
            System.out.println( e.getMessage() );
        }
    }

    /**
     *  Close the window. Delete any ranges that are shown.
     */
    protected void closeWindowEvent()
    {
        rangeList.deleteAllRanges();
        this.dispose();
    }

    /**
     *  Delete all known cuts and deletes if all is true, otherwise do not
     *  delete the removedCurrentSpectrum.
     */
    protected void deleteCuts( boolean all )
    {
        if ( all ) {
            for ( int i = 0; i < localList.size(); i++ ) {
                globalList.removeSpectrum( (SpecData)localList.get( i ) );
            }
            localList.clear();
        } 
        else {
            SpecData spec = null;
            Iterator i = localList.iterator();
            while ( i.hasNext() ) {
                spec = (SpecData) i.next();
                if ( ! spec.equals( removedCurrentSpectrum ) ) {
                    globalList.removeSpectrum( spec );
                    i.remove();
                }
            }
        }
    }

    /**
     *  Reset all controls and dispose of all associated cuts.
     */
    protected void resetActionEvent()
    {
        //  Remove any spectra.
        deleteCuts( true );

        //  Remove any graphics and ranges.
        rangeList.deleteAllRanges();
    }

    /**
     *  Reset last Replace action. Disposes of all generated spectra and
     *  restores the last current spectrum (keeping that if it is a generated
     *  spectrum).
     */
    protected void resetReplaceActionEvent()
    {
        //  Remove any generated spectra.
        deleteCuts( false );

        //  Restore the current spectrum, if needed.
        if ( removedCurrentSpectrum != null ) {
            if ( ! plot.isDisplayed( removedCurrentSpectrum )  ) {
                try {
                    plot.addSpectrum( removedCurrentSpectrum );
                }
                catch (SplatException e) {
                    //  Do nothing, not important.
                    e.printStackTrace();
                }
            }
        }

    }
    /**
     * Cut action. Cuts out all ranges.
     */
    protected class CutAction
        extends AbstractAction
    {
        public CutAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control C" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            cut( false );
        }
    }

    /**
     * Cut selected action. Performs cut of only selected ranges.
     */
    protected class CutSelectedAction
        extends AbstractAction
    {
        public CutSelectedAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control S" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            cut( true );
        }
    }

    /**
     * Remove action. Removes all ranges.
     */
    protected class RemoveAction
        extends AbstractAction
    {
        public RemoveAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control M" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            remove( false );
        }
    }

    /**
     * Remove selected action. Performs remove of selected ranges.
     */
    protected class RemoveSelectedAction
        extends AbstractAction
    {
        public RemoveSelectedAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control O" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            remove( true );
        }
    }

    /**
     * Interp action. Interpolate all ranges.
     */
    protected class InterpAction
        extends AbstractAction
    {
        public InterpAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control J" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            interpolate( false, false );
        }
    }

    /**
     * Interp action. Interpolate all ranges and replace current spectrum.
     */
    protected class InterpReplaceAction
        extends AbstractAction
    {
        public InterpReplaceAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control K" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            interpolate( false, true );
        }
    }

    /**
     * Interp selected action. Performs interpolation of selected ranges.
     */
    protected class InterpSelectedAction
        extends AbstractAction
    {
        public InterpSelectedAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control I" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            interpolate( true, false );
        }
    }

    /**
     * Inner class defining Action for closing window and keeping the
     * cuts.
     */
    protected class CloseAction
        extends AbstractAction
    {
        public CloseAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control W" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            closeWindowEvent();
        }
    }

    /**
     * Inner class defining action for resetting a Replace.
     */
    protected class ResetReplaceAction
        extends AbstractAction
    {
        public ResetReplaceAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control P" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            resetReplaceActionEvent();
        }
    }

    /**
     * Inner class defining action for resetting all values.
     */
    protected class ResetAction
        extends AbstractAction
    {
        public ResetAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control R" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            resetActionEvent();
        }
    }
}
