/*
 * Copyright (C) 2006 Particle Physics and Astronomy Research Council
 *
 *  History:
 *     09-JAN-2006 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.splat.data.AssociatedLineIDSpecData;
import uk.ac.starlink.splat.data.AssociatedLineIDTXTSpecDataImpl;
import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.splat.data.LineIDSpecData;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * A simple class for controlling a {@link LineProvider}, so that
 * a list of spectral lines and optionally related spectra can be viewed,
 * one-by-one.
 * <p>
 * This class provides controls for stepping through a list of lines and,
 * optionally, their related spectra. If no related spectra are known then the
 * current spectrum of the {@link LineProvider} will be used.
 * <p>
 * The line coordinates are stored in a simple text file (the text files can
 * be line identifier files, with just one column, or files like those, except
 * with three columns, coordinates, label and associated spectrum).  When a
 * line is "visited" it can have a state object associated with it. This
 * allows any known information associated with the line to be restored (like
 * analysis and UI state).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class LineVisitor
    extends JPanel
    implements ActionListener
{
    /** The LineProvider instance */
    private LineProvider provider = null;

    /** Current position, in list of lines, start at off-list */
    private int position = -1;

    /** The list of associated states */
    private Object[] states = null;

    /** Extracted 1D Frame describing the coordinate system and units */
    private Frame coordFrame = null;

    /** The EditableSpecData instance*/
    private EditableSpecData specData = null;

    /** Storage for labels. These need to be unique Strings so include
     *  an index */
    private ArrayList labels = new ArrayList();

    /** Storage for associated spectra. */
    private SpecData[] spectra = null;

    /** Storage for associated spectra names as stored in the line list. */
    private String[] spectraNames = null;

    /** Local constants for various actions. */
    public static final int FIRST = -2;
    public static final int PREV = -1;
    public static final int NEXT = 1;
    public static final int LAST = 2;

    /** JComboBox for selecting a coordinate directly and displaying the
     *  current value */
    private JComboBox lineBox = null;
    private LineListModel lineBoxModel = null;

    /**  The global list of spectra */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**  The spectrum factory */
    protected SpecDataFactory specDataFactory = SpecDataFactory.getInstance();

   /**
     * Create an instance.
     *
     * @param provider an instance of {@link LineProvider}.
     */
    public LineVisitor( LineProvider provider )
    {
        setLineProvider( provider );
        initUI();
    }

    /**
     * Initialise the various user interface components.
     */
    protected void initUI()
    {
        GridBagLayouter layouter =
            new GridBagLayouter( this, GridBagLayouter.SCHEME5 );

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;

        //  Current line, is displayed and selectable by this control.
        lineBoxModel = new LineListModel();
        lineBox = new JComboBox();
        lineBox.addActionListener( this );
        gbc.gridwidth = 1;
        layouter.add( Box.createGlue(), gbc );
        gbc.gridwidth = 7;
        layouter.add( lineBox, gbc );
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        layouter.add( Box.createGlue(), gbc );

        //  Back to start.
        FirstAction firstAction = new FirstAction();
        JButton firstButton = new JButton( firstAction );
        layouter.add( Box.createGlue(), false );
        layouter.add( firstButton, false );

        //  Back one line.
        PrevAction prevAction = new PrevAction();
        JButton prevButton = new JButton( prevAction );
        layouter.add( Box.createGlue(), false );
        layouter.add( prevButton, false );

        //  Next line.
        NextAction nextAction = new NextAction();
        JButton nextButton = new JButton( nextAction );
        layouter.add( Box.createGlue(), false );
        layouter.add( nextButton, false );

        //  Forward to last.
        LastAction lastAction = new LastAction();
        JButton lastButton = new JButton( lastAction );
        layouter.add( Box.createGlue(), false );
        layouter.add( lastButton, false );
        layouter.add( Box.createGlue(), false );
    }

    /** Set the LineProvider */
    public void setLineProvider( LineProvider provider )
    {
        this.provider = provider;
    }

    /** Get the LineProvider */
    public LineProvider getLineProvider()
    {
        return provider;
    }

    /**
     * Step forwards/backwards along the list.
     *
     * @param action one of the local constants FIRST, PREV, NEXT and LAST.
     */
    public void step( int action )
    {
        //  Check for nothing read yet.
        if ( specData == null ) return;
        double[] coords = specData.getXData();
        if ( coords == null || coords.length == 0 ) return;

        //  Determine position within bounds of list, following the given
        //  action.
        int newPosition = position;
        switch (action) {
           case FIRST: {
               newPosition = 0;
           }
           break;
           case PREV: {
               newPosition--;
           }
           break;
           case NEXT: {
               newPosition++;
           }
           break;
           case LAST: {
               newPosition = coords.length - 1;
           }
           break;
        }
        newPosition = Math.max( 0, Math.min( newPosition, coords.length-1 ) );
        moveto( newPosition, true );
    }

    /**
     * Move to a position in the list.
     */
    protected void moveto( int newPosition, boolean updateLineBox )
    {
        //  Don't do anything unless needed (stops lineBox from looping).
        if ( specData == null ) return;
        if ( newPosition != position ) {
            double[] coords = specData.getXData();
            if ( coords != null && coords.length > 0 ) {

                //  Get current state and store for next time.
                if ( position != -1 ) {
                    states[position] = provider.getLineState();
                }

                //  If we have a spectrum and it's not realized yet, do that
                //  now.
                position = newPosition;
                if ( spectraNames != null &&
                     spectraNames[position] != null &&
                     spectra[position] == null ) {
                    try {
                        SpecData newSpec = null;
                        int index =
                            globalList.specKnown( spectraNames[position] );
                        if ( index != -1 ) {
                            //  Already in global list, so reuse.
                            newSpec = globalList.getSpectrum( index );
                        }
                        else {
                            //  Create by reading file and enter into global
                            //  list.
                            newSpec =
                                specDataFactory.get( spectraNames[position] );
                            globalList.add( newSpec );
                        }
                        spectra[position] = newSpec;
                    }
                    catch (SplatException e) {
                        //  Complain and stop a future attempt.
                        e.printStackTrace();
                        spectraNames[position] = null;
                    }
                }

                //  Move to new line. Ask for the spectrum to be displayed, if
                //  available and also pass stored state so it can be restored.
                if ( spectra[position] != null ) {
                    provider.viewSpectrum( spectra[position] );
                }
                provider.viewLine( coords[position], coordFrame,
                                   states[position] );

                //  Set the lineBox to show this value.
                if ( updateLineBox ) {
                    lineBox.setSelectedIndex( position );
                }

            }
        }
    }

    /**
     * Read a simple text file of line positions. This creates a
     * {@link LineIDSpecData} that should be added to the global list and
     * plotted, if appropriate.
     *
     * @param file the text file containing the positions to visit. This can
     *             be a fully specified line identifier file and include
     *             labels and coordinate system information.
     *
     * @return the {@link LineIDSpecData} representing the positions, null if
     *         anything fails.
     */
    public AssociatedLineIDSpecData readLines( File file )
        throws SplatException
    {
        if ( ! file.exists() ) return null;

        AssociatedLineIDTXTSpecDataImpl impl =
            new AssociatedLineIDTXTSpecDataImpl( file );
        AssociatedLineIDSpecData specData =
            new AssociatedLineIDSpecData( impl );

        setLineList( specData );
        return specData;
    }

    /**
     * Set a {@link EditableSpecData} to use as the visitor list. If this is
     * an instance of {@link LineIDSpecData}, then the labels will be used in
     * the combobox of selectable positions. Otherwise it will contain the
     * coordinates.
     */
    public void setLineList( EditableSpecData specData )
    {
        //  Release existing states and spectra and get new coordinates.
        this.specData = specData;
        double[] coords = specData.getXData();
        states = new Object[coords.length];
        spectra = new SpecData[coords.length];
        spectraNames = null;

        //  Generate labels for JComboBox. Must be unique, so add index.
        labels.clear();
        if ( specData instanceof AssociatedLineIDSpecData ) {
            //  may have associated spectra.
            String[] specLabels =
                ((AssociatedLineIDSpecData)specData).getLabels();
            for ( int i = 0; i < specLabels.length; i++ ) {
                labels.add( i + ": " + specLabels[i] );
            }
            spectraNames =
                ((AssociatedLineIDSpecData)specData).getAssociations();
        }
        else if ( specData instanceof LineIDSpecData ) {
            //  No associated spectra, just positions and labels.
            String[] specLabels = ((LineIDSpecData)specData).getLabels();
            for ( int i = 0; i < specLabels.length; i++ ) {
                labels.add( i + ": " + specLabels[i] );
            }
        }
        else {
            //  Any old spectrum, just use the positions and make up some
            //  labels.
            for ( int i = 0; i < coords.length; i++ ) {
                labels.add( i + ": " + coords[i] );
            }
        }

        //  Extract the coordinate Frame describing the units (first axis of
        //  the plot FrameSet associated with the spectrum).
        coordFrame =
            specData.getAst().getRef().pickAxes( 1, new int[]{ 1 }, null );

        //  Move to first line. Reset model to force update of lineBox.
        position = -1;
        lineBox.setModel( lineBoxModel );
        step( FIRST );
    }

    /**
     * Clear all states so that they can be refreshed on next visits.
     */
    public void clearStates()
    {
        if ( specData != null ) {
            double[] coords = specData.getXData();
            states = new Object[coords.length];
        }
    }

    //  Provide enabled so that children are affected.
    public void setEnabled( boolean enabled )
    {
        super.setEnabled( enabled );
        Component[] children = getComponents();
        for ( int i = 0; i < children.length; i++ ) {
            children[i].setEnabled( enabled );
        }
    }

    /**
     * Inner class defining Action for stepping to start of list.
     */
    protected class FirstAction
        extends AbstractAction
    {
        public FirstAction()
        {
            super( "First", firstImage );
            putValue( SHORT_DESCRIPTION, "Backwards to first line" );
        }
        public void actionPerformed( ActionEvent ae )
        {
            step( FIRST );
        }
    }
    private final static ImageIcon firstImage =
        new ImageIcon( ImageHolder.class.getResource( "first.gif" ) );

    /**
     * Inner class defining Action for stepping to previous line.
     */
    protected class PrevAction
        extends AbstractAction
    {
        public PrevAction()
        {
            super( "Prev", prevImage );
            putValue( SHORT_DESCRIPTION, "Backward to previous line" );
        }
        public void actionPerformed( ActionEvent ae )
        {
            step( PREV );
        }
    }
    private final static ImageIcon prevImage =
        new ImageIcon( ImageHolder.class.getResource( "prev.gif" ) );

    /**
     * Inner class defining Action for stepping to next line.
     */
    protected class NextAction
        extends AbstractAction
    {
        public NextAction()
        {
            super( "Next", forwardImage );
            putValue( SHORT_DESCRIPTION, "Forward to next line" );
        }
        public void actionPerformed( ActionEvent ae )
        {
            step( NEXT );
        }
    }
    private final static ImageIcon forwardImage =
        new ImageIcon( ImageHolder.class.getResource( "next.gif" ) );


    /**
     * Inner class defining Action for stepping to head of list.
     */
    protected class LastAction
        extends AbstractAction
    {
        public LastAction()
        {
            super( "Last", lastImage );
            putValue( SHORT_DESCRIPTION, "Forward to last line" );
        }
        public void actionPerformed( ActionEvent ae )
        {
            step( LAST );
        }
    }
    private final static ImageIcon lastImage =
        new ImageIcon( ImageHolder.class.getResource( "last.gif" ) );

    //
    // Implement ActionListener interface.
    //
    public void actionPerformed( ActionEvent e )
    {
        //  Move to the selected line.
        moveto( lineBox.getSelectedIndex(), false );
    }

    //
    // Implement a simple ComboBoxModel that uses the labels of the enclosing
    // class instance.
    //
    protected class LineListModel
        extends AbstractListModel
        implements ComboBoxModel
    {
        public LineListModel()
        {
            // Do nothing.
        }
        public Object getSelectedItem()
        {
            if ( specData != null ) {
                double[] coords = specData.getXData();
                if ( coords != null && position != -1 ) {
                    return labels.get( position );
                }
            }
            return null;
        }
        public void setSelectedItem( Object anItem )
        {
            if ( specData == null ) return;
            int index = labels.indexOf( anItem );
            if ( index > -1 ) {
                moveto( index, false );
                fireContentsChanged( this, -1, -1 );
            }
        }
        public Object getElementAt( int index )
        {
            if ( specData != null ) {
                return labels.get( index );
            }
            return null;
        }
        public int getSize()
        {
            if ( specData != null ) {
                return labels.size();
            }
            return 0;
        }
    }
}
