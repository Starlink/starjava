/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     17-MAY-2005 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.text.DecimalFormat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.WinMap;
import uk.ac.starlink.ast.gui.DecimalField;
import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * Provides a toolbox for optionally flipping and translating a modifiable
 * spectrum. Flipping allows the detailed line shape to be compared.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class FlipFrame
    extends JFrame
    implements ActionListener, ChangeListener, PlotListener, ItemListener
{
    /**
     * List of spectra and properties that we have modified.
     */
    protected Map storedPropertiesMap = new HashMap();

    /**
     * JComboBox that displays the list of available spectra.
     */
    protected JComboBox availableSpectra = new JComboBox();

    /**
     * Flip state.
     */
    protected JCheckBox flipBox = null;

    /**
     * User translation.
     */
    protected JSpinner spinnerTran = null;

    /**
     * And the model.
     */
    protected SpinnerNumberModel spinnerModel = null;

    /**
     * Spinner increment.
     */
    protected DecimalField spinnerIncr = null;

    /**
     *  The global list of spectra and plots.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * Content pane of frame.
     */
    protected JPanel contentPane = null;

    /**
     *  Menubar and various menus and items that it contains.
     */
    protected JMenuBar menuBar = new JMenuBar();
    protected JMenu fileMenu = new JMenu();
    protected JMenuItem closeFileMenu = new JMenuItem();
    protected JMenu helpMenu = new JMenu();
    protected JMenuItem helpMenuAbout = new JMenuItem();

    /**
     *  The PlotControl that is displaying the current spectrum.
     */
    protected PlotControl plot = null;

    /**
     * Create an instance.
     */
    public FlipFrame( PlotControl plot )
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

        //  We are listening to changes to the available spectra.
        globalList.addPlotListener( this );
        updateNames();
    }

    /**
     * Initialise the main part of the user interface.
     */
    protected void initUI()
    {
        //  Panel for centre of contentPane.
        JPanel topPanel = new JPanel( new BorderLayout() );
        contentPane.add( topPanel, BorderLayout.NORTH );
        GridBagLayouter gbl0 = new GridBagLayouter( topPanel,
                                                    GridBagLayouter.SCHEME3 );

        //  Panel for spectrum choice, includes copy of current spectrum.
        JPanel spectrumPanel = new JPanel();
        spectrumPanel.setBorder(BorderFactory.createTitledBorder("Spectrum:"));
        GridBagLayouter gbl1 = new GridBagLayouter( spectrumPanel,
                                                    GridBagLayouter.SCHEME3 );

        //  Controls for copying current spectrum
        JLabel copyLabel = new JLabel( "Copy current: " );
        gbl1.add( copyLabel, false );

        //  If current spectrum should be flipped.
        flipBox = new JCheckBox( "Flip" );
        flipBox.setSelected( true );
        gbl1.add( flipBox, false );

        //  Grab button. This creates the copy, which may be flipped.
        CopyAction copyAction = new CopyAction( "Copy" );
        JButton copyButton = new JButton( copyAction );
        copyButton.setToolTipText( "Press to create a copy of the current " +
                                   "spectrum in the associated plot" );
        gbl1.add( copyButton, false );
        gbl1.eatLine();

        //  Add controls for choosing the spectrum.
        JLabel availableSpectraLabel = new JLabel( "Spectrum:" );
        gbl1.add( availableSpectraLabel, false );
        String tipText = "Select a spectrum for translating ";
        availableSpectraLabel.setToolTipText( tipText );

        availableSpectra.setRenderer( new LineRenderer( availableSpectra ) );
        gbl1.add( availableSpectra, true );
        availableSpectra.setToolTipText( tipText );

        //  Respond to changes in selection.
        availableSpectra.addItemListener( this );

        //  Translation controls.
        JPanel transPanel = new JPanel();
        transPanel.setBorder(BorderFactory.createTitledBorder("Translation:"));
        GridBagLayouter gbl2 = new GridBagLayouter( transPanel,
                                                    GridBagLayouter.SCHEME3 );

        //  Just offset and increment for now (redshift...).
        JLabel incrLabel = new JLabel( "Increment:" );
        gbl2.add( incrLabel, false );

        DecimalFormat decimalFormat = new DecimalFormat();
        spinnerIncr = new DecimalField( 10.0, 5, decimalFormat );
        spinnerIncr.addActionListener( this );
        spinnerIncr.setToolTipText( "Increment used for spinner controls" );
        gbl2.add( spinnerIncr, true );

        JLabel spinnerLabel = new JLabel( "Offset:" );
        gbl2.add( spinnerLabel, false );

        spinnerModel = new SpinnerNumberModel( 0.0,
                                               -Double.MAX_VALUE,
                                               Double.MAX_VALUE,
                                               10.0 );
        spinnerTran = new JSpinner( spinnerModel );
        spinnerTran.addChangeListener( this );
        spinnerTran.setToolTipText("Offset of spectrum from initial position");
        gbl2.add( spinnerTran, true );

        gbl0.add( spectrumPanel, true );
        gbl0.eatLine();
        gbl0.add( transPanel, true );

        //  Add the menuBar.
        setJMenuBar( menuBar );

        //  Get icons for menus and action bar.
        ImageIcon closeImage =
            new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );
        ImageIcon resetImage =
            new ImageIcon( ImageHolder.class.getResource( "reset.gif" ) );

        //  Menubar.
        fileMenu.setText( "File" );
        menuBar.add( fileMenu );

        //  Action bar for buttons.
        JPanel actionBar = new JPanel();

        //  Add an action to reset the spectrum to the default transform.
        ResetAction resetAction = new ResetAction( "Reset", resetImage );
        fileMenu.add( resetAction );
        JButton resetButton = new JButton( resetAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( resetButton );
        resetButton.setToolTipText( "Reset spectrum to default offset" );

        //  Add an action to close the window.
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction );
        JButton closeButton = new JButton( closeAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( closeButton );
        closeButton.setToolTipText( "Close window" );

        actionBar.add( Box.createGlue() );
        contentPane.add( actionBar, BorderLayout.SOUTH );

        //  Add the help menu.
        HelpFrame.createHelpMenu( "flipper-window", "Help on window",
                                  menuBar, null );
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Flip/translate spectrum" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        setSize( new Dimension( 400, 250 ) );
        setVisible( true );
    }

    /**
     * Make a copy of the current spectrum and apply a flip, if requested. The
     * initial translation is set so that the feature in the middle of the
     * Plot is shown in the middle.
     */
    protected void copyFlipCurrentSpectrum()
    {
        boolean flipped = flipBox.isSelected();

        //  Create the copy.
        SpecData spec = plot.getCurrentSpectrum();
        String name = null;
        if ( flipped ) {
            name = "Flip of: " + spec.getShortName();
        }
        else {
            name = "Copy of: " + spec.getShortName();
        }
        EditableSpecData comparisonSpectrum = null;
        try {
            comparisonSpectrum =
                SpecDataFactory.getInstance().createEditable( name, spec,
                                                              false );
            globalList.add( comparisonSpectrum );
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog( this, e.getMessage(),
                                           "Failed to create copy",
                                           JOptionPane.ERROR_MESSAGE );
            comparisonSpectrum = null;
            return;
        }

        //  Decide how to flip and transform the spectrum. If not flipping no
        //  need to offset, otherwise reflection about zero needs an offset to
        //  get back to same part of display.
        double offset = 0.0;
        double scale = 1.0;
        double centre = 0.0;
        if ( flipped ) {
            scale = -1.0;
            double[] view = plot.getViewCoordinates();
            centre = view[0] + 0.5 * ( view[2] - view[0] );
            offset = 2.0 * centre;
        }
        flipTransform( scale, offset, comparisonSpectrum );

        //  Add this to our plot and make it the selected spectrum.
        try {
            globalList.addSpectrum( plot, comparisonSpectrum );
            if ( flipped ) {
                //  Reposition plot window, may have zoomed on feature.
                plot.centreOnXCoordinate( Double.toString( centre ) );
            }
            addSpectrum( comparisonSpectrum, true );

            //  Do this last so that the shift to 0.0 works on the final
            //  version of comparison spectrum that is cached.
            availableSpectra.setSelectedItem( comparisonSpectrum );
        }
        catch (SplatException e) {
            JOptionPane.showMessageDialog( this, e.getMessage(),
                                           "Failed to add spectrum to plot",
                                           JOptionPane.ERROR_MESSAGE );
        }
    }

    /**
     * Apply a scale and offset transformation to a given spectrum, or to the
     * current spectrum if the given spectrum is null.
     */
    private void flipTransform( double scale, double offset, 
                                EditableSpecData spectrum  )
    {
        //  Create a WinMap for the linear transformation.
        double[] ina = new double[1];
        double[] inb = new double[1];
        double[] outa = new double[1];
        double[] outb = new double[1];
        ina[0] = 0.0;
        inb[0] = 1.0;
        outa[0] = ina[0] * scale + offset;
        outb[0] = inb[0] * scale + offset;
        WinMap winMap = new WinMap( 1, ina, inb, outa, outb );

        //  Modify the original comparison spectrum FrameSet (possibly
        //  modified for any initial transform) to our new values by adding
        //  the WinMap.
        if ( spectrum == null ) {
            spectrum = getSelectedSpectrum();
        }
        StoredProperties storedProperties = getStoredProperties( spectrum );
        storedProperties.setOffset( offset );
        FrameSet frameSet = storedProperties.getFrameSet();
        Frame frame = storedProperties.getFrame();
        frameSet.addFrame( FrameSet.AST__CURRENT, winMap, frame );

        //  Make changes propagate.
        try {
            spectrum.setFrameSet( frameSet );
            globalList.notifySpecListenersModified( spectrum );
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog( this, e.getMessage(),
                        "Failed to update spectral coordinates",
                        JOptionPane.ERROR_MESSAGE );
        }
    }

    /**
     *  Update the spectra available for translation. Only EditableSpecData
     *  instances are allowed and only those in the plot.
     */
    protected void updateNames()
    {
        //  Keep reference to currently selected spectrum.
        EditableSpecData currentSpectrum = getSelectedSpectrum();
        if ( globalList.getSpectrumIndex( currentSpectrum ) == -1 ) {
            //  This has been removed.
            currentSpectrum = null;
        }

        //  Re-create a vector/model for this. Faster as avoids
        //  listener updates also gets resize right.
        Vector v = new Vector();
        SpecData spec = null;
        for ( int i = 0; i < globalList.specCount(); i++ ) {
            spec = globalList.getSpectrum( i );
            if ( spec instanceof EditableSpecData ) {
                if ( plot.isDisplayed( spec ) ) {
                    v.add( globalList.getSpectrum( i ) );
                }
            }
        }
        ComboBoxModel model = new DefaultComboBoxModel( v );
        availableSpectra.setModel( model );

        //  Restore selected spectrum.
        if ( currentSpectrum != null && globalList.specCount() > 1 ) {
            model.setSelectedItem( currentSpectrum );
        }
    }

    /**
     * Access the selected spectrum.
     */
    protected EditableSpecData getSelectedSpectrum()
    {
        EditableSpecData spectrum =
            (EditableSpecData) availableSpectra.getSelectedItem();
        if ( spectrum == null ) {
            //  No selected spectra, look for any on the changed list.
            if ( storedPropertiesMap.size() > 0 ) {
                Iterator i = storedPropertiesMap.keySet().iterator();
                spectrum = (EditableSpecData) i.next();
            }
        }
        return spectrum;
    }

    /**
     * Get the StoredProperties instance for a spectrum.
     */
    protected StoredProperties getStoredProperties( EditableSpecData spectrum )
    {
        if ( ! storedPropertiesMap.containsKey( spectrum ) ) {
            //  Unknown spectrum, so add it.
            addSpectrum( spectrum, false );
        }
        return (StoredProperties) storedPropertiesMap.get( spectrum );
    }

    /**
     * Apply the current offset to the selected spectrum.
     */
    protected void applyOffset()
    {
        double offset = ((Double)spinnerTran.getValue()).doubleValue();
        flipTransform( 1.0, offset, null );
    }

    /**
     * Add a spectrum to the changed list. Replaces an existing spectrum, if
     * replace if true.
     */
    protected void addSpectrum( EditableSpecData spectrum, boolean replace )
    {
        if ( storedPropertiesMap.containsKey( spectrum ) ) {
            if ( replace ) {
                storedPropertiesMap.put( spectrum,
                                         new StoredProperties( spectrum ) );
            }
        }
        else {
            storedPropertiesMap.put( spectrum,
                                     new StoredProperties( spectrum ) );
        }
    }

    /**
     * Reset selected spectrum to it default offset.
     */
    protected void resetSelectedSpectrum()
    {
        spinnerTran.setValue( new Double( 0.0 ) );
    }

    /**
     *  Close the window. Delete any ranges that are shown.
     */
    protected void closeWindowEvent()
    {
        globalList.removePlotListener( this );
        this.dispose();
    }

    /**
     * Inner class defining Action for closing window.
     */
    protected class CloseAction
        extends AbstractAction
    {
        public CloseAction( String name, Icon icon )
        {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae )
        {
            closeWindowEvent();
        }
    }

    /**
     * Inner class defining Action for reseting offset to default.
     */
    protected class ResetAction
        extends AbstractAction
    {
        public ResetAction( String name, Icon icon )
        {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae )
        {
            resetSelectedSpectrum();
        }
    }

    /**
     * Make a copy of the current spectrum and apply flip.
     */
    protected class CopyAction
        extends AbstractAction
    {
        public CopyAction( String name )
        {
            super( name, null );
        }
        public void actionPerformed( ActionEvent ae )
        {
            copyFlipCurrentSpectrum();
        }
    }

    //
    // Implement ChangeListener so that we update translation.
    //
    public void stateChanged( ChangeEvent e )
    {
        applyOffset();
    }

    //
    // Implement the PlotListener interface so we can keep the list of
    // available spectra up to date.
    //

    public void plotCreated( PlotChangedEvent e )
    {
        //  Do nothing.
    }
    public void plotRemoved( PlotChangedEvent e )
    {
        // Do nothing.
    }

    //
    //  Sent when a plot property is changed, assume this means that a
    //  spectrum has been added or removed, so update the list.
    //
    public void plotChanged( PlotChangedEvent e )
    {
        updateNames();
        if ( e.getType() == PlotChangedEvent.REMOVED ) {
            //  Spectrum may need removing from our lists.
            SpecData spectrum = globalList.getSpectrum( e.getIndex() );
            storedPropertiesMap.remove( spectrum );
        }

        //  XXX there is a problem here. When spectra are removed from the
        //  GlobalSpecPlotList and are displayed in our Plot, we don't see the
        //  event. Clearly the GlobalSpecPlotList needs to inform all
        //  PlotListeners, but doesn't. May have some other consequences...
    }

    //
    //  ItemListener for changes to the selected spectrum.
    //
    public void itemStateChanged( ItemEvent e )
    {
        EditableSpecData spectrum = getSelectedSpectrum();
        StoredProperties storedProperties = getStoredProperties( spectrum );

        // Need to restore the offset last used for this spectrum.
        spinnerTran.setValue( new Double( storedProperties.getOffset() ) );
    }

    //
    //  ActionListener for changes to spinner increment.
    //
    public void actionPerformed( ActionEvent e )
    {
        spinnerModel.setStepSize( new Double( spinnerIncr.getDoubleValue() ) );
    }

    //
    //  Internal class for storing original properties of any spectra that we
    //  change.
    //
    protected class StoredProperties
    {
        private FrameSet originalFrameSet = null;
        private Frame originalFrame = null;
        private double offset = 0.0;

        public StoredProperties( EditableSpecData spectrum )
        {
            originalFrameSet = (FrameSet) spectrum.getFrameSet().copy();
            originalFrame = (Frame)
                originalFrameSet.getFrame(FrameSet.AST__CURRENT).copy();
        }
        public FrameSet getFrameSet()
        {
            return (FrameSet) originalFrameSet.copy();
        }
        public Frame getFrame()
        {
            return (Frame) originalFrame.copy();
        }

        public void setOffset( double offset )
        {
            this.offset = offset;
        }
        public double getOffset()
        {
            return offset;
        }
    }
}
