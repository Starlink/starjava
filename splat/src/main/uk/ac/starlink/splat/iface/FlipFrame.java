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
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.SpecFrame;
import uk.ac.starlink.ast.WinMap;
import uk.ac.starlink.ast.gui.DecimalField;
import uk.ac.starlink.ast.gui.ScientificFormat;
import uk.ac.starlink.ast.gui.ScientificSpinner;
import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.splat.data.LineIDSpecData;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.MathUtils;
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
     * Is translation to be interpreted as a redshift?
     */
    protected JCheckBox redshiftBox = null;

    /**
     * User translation.
     */
    protected ScientificSpinner spinnerTran = null;

    /**
     * And the model.
     */
    protected SpinnerNumberModel spinnerModel = null;

    /**
     * Spinner increment.
     */
    protected DecimalField spinnerIncr = null;

    /**
     * Flip coordinate.
     */
    protected DecimalField flipCentre = null;

    /**
     * The global list of spectra and plots.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * Content pane of frame.
     */
    protected JPanel contentPane = null;

    /**
     * Menubar and various menus and items that it contains.
     */
    protected JMenuBar menuBar = new JMenuBar();
    protected JMenu fileMenu = new JMenu();
    protected JMenuItem closeFileMenu = new JMenuItem();
    protected JMenu helpMenu = new JMenu();
    protected JMenuItem helpMenuAbout = new JMenuItem();

    /**
     * The PlotControl that is displaying the current spectrum.
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
        flipBox.setToolTipText( "Flip copy left to right" );
        flipBox.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    flipCentre.setEnabled( flipBox.isSelected() );
                }
            });
        gbl1.add( flipBox, false );

        //  Coordinate of flip.
        JLabel flipCentreLabel = new JLabel( "Flip centre:" );
        gbl1.add( flipCentreLabel, false );

        ScientificFormat scientificFormat = new ScientificFormat();
        flipCentre = new DecimalField( 0.0, 5, scientificFormat );
        flipCentre.setToolTipText( "Coordinate of flip centre, " + 
                                   "zero for visible centre of plot, " +
                                   "units of current spectrum" );
        gbl1.add( flipCentre, true );

        //  Grab button. This creates the copy, which may be flipped.
        CopyAction copyAction = new CopyAction( "Create copy" );
        JButton copyButton = new JButton( copyAction );
        copyButton.setToolTipText( "Press to create a copy of the current " +
                                   "spectrum in the associated plot" );
        gbl1.add( Box.createGlue(), false );
        gbl1.add( copyButton, false );
        gbl1.add( Box.createGlue(), true );

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

        //  Offset, increment and redshift.
        redshiftBox = new JCheckBox( "Redshift" );
        redshiftBox.setSelected( false );
        redshiftBox.setToolTipText( "Interpret offset as a redshift" );
        gbl2.add( redshiftBox, true );

        JLabel incrLabel = new JLabel( "Increment:" );
        gbl2.add( incrLabel, false );

        scientificFormat = new ScientificFormat();
        spinnerIncr = new DecimalField( 10.0, 5, scientificFormat );
        spinnerIncr.addActionListener( this );
        spinnerIncr.setToolTipText( "Increment used for spinner controls" );
        gbl2.add( spinnerIncr, true );

        JLabel spinnerLabel = new JLabel( "Offset:" );
        gbl2.add( spinnerLabel, false );

        spinnerModel = new SpinnerNumberModel( 0.0, -Double.MAX_VALUE,
                                               Double.MAX_VALUE, 10.0 );
        spinnerTran = new ScientificSpinner( spinnerModel );

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
        setSize( new Dimension( 400, 300 ) );
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
            centre = flipCentre.getDoubleValue();
            if ( centre == 0.0 ) {
                //  Use centre of visible plot.
                double[] view = plot.getViewCoordinates();
                centre = view[0] + 0.5 * ( view[2] - view[0] );
            }
            offset = 2.0 * centre;
        }
        flipTransform( scale, offset, comparisonSpectrum, false );

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
     * current spectrum if the given spectrum is null. If redshift is true
     * then the offset is interpreted as a redshift to be applied (and the
     * scale is ignored).
     */
    private void flipTransform( double scale, double offset,
                                EditableSpecData spectrum, boolean redshift  )
    {
        //  If no spectrum has been given then we need to access the currently
        //  selected one.
        if ( spectrum == null ) {
            spectrum = getSelectedSpectrum();
        }

        //  Recover the stored properties, such as the original FrameSet
        //  (need this to avoid adding redundant frames and mappings) and
        //  Frame (defines coordinate system).
        StoredProperties storedProperties = getStoredProperties( spectrum );
        FrameSet frameSet = storedProperties.getFrameSet();
        Frame frame = storedProperties.getFrame();

        //  Store the offset so we can set the offset widget to the right
        //  value for this spectrum next time it is selected.
        storedProperties.setOffset( offset );

        //  Create a WinMap for the linear transformation.
        double[] ina = new double[1];
        double[] inb = new double[1];
        double[] outa = new double[1];
        double[] outb = new double[1];
        boolean simple = true;
        if ( redshift ) {
            //  Pick out first axis from FrameSet, this should be a SpecFrame,
            //  otherwise we don't care about units, systems etc. and will
            //  assume everything just works as a wavelength.
            int iaxes[] = { 1 };
            Frame picked = frameSet.pickAxes( 1, iaxes, null );
            if ( picked instanceof SpecFrame ) {
                boolean islineid = ( spectrum instanceof LineIDSpecData );
                if ( redshiftSpecFrameSet( frameSet, islineid, offset ) ) {
                    simple = false;
                }
                else {
                    //  Failed.
                    return;
                }
            }
            else {
                //  Redshift is offset value so times wavelength by (z+1).
                ina[0] = 0.0;
                inb[0] = 1.0;
                outa[0] = ina[0] * ( offset + 1 );
                outb[0] = inb[0] * ( offset + 1 );
            }
        }
        else {
            ina[0] = 0.0;
            inb[0] = 1.0;
            outa[0] = ina[0] * scale + offset;
            outb[0] = inb[0] * scale + offset;
        }
        if ( simple ) {
            WinMap winMap = new WinMap( 1, ina, inb, outa, outb );

            //  Add mapping and original Frame to FrameSet.
            frameSet.addFrame( FrameSet.AST__CURRENT, winMap, frame );
        }

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
     * Transform the spectral coordinates of a FrameSet by a redshift factor.
     *
     * To deal with all occasions (nearly) we must let AST work this out. The
     * way to do this is by setting the observer reference frame to be the
     * Source and add a source velocity equivalent to the redshift. We then
     * move the reference frame back to some solar system frame...
     * The problems with this approach are when the reference frame is
     * already at the Source... In that case I think we would need to
     * construct a mapping for all possible spectral coordinate systems and
     * units, a non-trivial task. Assume a fix for line identifiers, which we
     * know are at "source.
     */
    protected boolean redshiftSpecFrameSet( FrameSet frameSet, 
                                            boolean islineid,
                                            double redshift )
    {
        Frame current = frameSet.getFrame( FrameSet.AST__CURRENT );

        String stdOfRest = current.getC( "StdOfRest" );
        boolean stdOfRestFudged = false;
        if ( stdOfRest.equalsIgnoreCase( "Source" ) ) {
            //  We're stuffed, unless this is a line identifier.
            if ( islineid ) {
                //  Line identifier.
                stdOfRestFudged = true;
                stdOfRest = "Heliocentric";
            }
            else {
                JOptionPane.showMessageDialog
                    ( this, "Cannot redshift when the" +
                      " spectral standard of rest is already set to 'Source'",
                      "Cannot redshift", JOptionPane.ERROR_MESSAGE );
                return false;
            }
        }

        //  Change standard of rest to "Source" and set the velocity, without
        //  causing a remap to the FrameSet.
        double initialVelocity = current.getD( "SourceVel" );
        current.setC( "StdOfRest", "Source" );
        double redshiftVelocity = 
            0.001 * MathUtils.redshiftToVelocity( redshift ); // Km/s
        current.setD( "SourceVel", redshiftVelocity );

        //  Now apply the redshift by moving the frameSet back to the original
        //  reference frame, this should cause a remapping.
        frameSet.setC( "StdOfRest", stdOfRest );

        //  Restore any original source velocity, without causing a remap.
        current.setD( "SourceVel", initialVelocity );
        
        //  If line identifier, we were always at "Source".
        if ( stdOfRestFudged ) {
            frameSet.setC( "StdOfRest", "Source" );
        }

        return true;
    }

    /**
     * Update the spectra available for translation. Only EditableSpecData
     * instances are allowed and only those in the plot.
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
        flipTransform( 1.0, offset, null, redshiftBox.isSelected() );
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
     * Close the window. Delete any ranges that are shown.
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
    // Sent when a plot property is changed, assume this means that a
    // spectrum has been added or removed, so update the list.
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
    // ItemListener for changes to the selected spectrum.
    //
    public void itemStateChanged( ItemEvent e )
    {
        EditableSpecData spectrum = getSelectedSpectrum();
        StoredProperties storedProperties = getStoredProperties( spectrum );

        // Need to restore the offset last used for this spectrum.
        spinnerTran.setValue( new Double( storedProperties.getOffset() ) );
    }

    //
    // ActionListener for changes to spinner increment.
    //
    public void actionPerformed( ActionEvent e )
    {
        spinnerModel.setStepSize( new Double( spinnerIncr.getDoubleValue() ) );
    }

    //
    // Internal class for storing original properties of any spectra that we
    // change.
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
