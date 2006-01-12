/*
 * Copyright (C) 2006 Particle Physics and Astronomy Research Council
 *
 *  History:
 *     09-JAN-2006 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.event.ListDataListener;

import uk.ac.starlink.splat.data.LineIDSpecData;
import uk.ac.starlink.splat.data.LineIDTXTSpecDataImpl;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * A simple class for controlling a {@link LineProvider}, so that
 * a list of spectral lines can be viewed and analysed, one-by-one.
 * <p>
 * This class provides controls for selecting a line and for stepping through
 * a list of lines. The line coordinate are stored in a simple text file (the
 * text files are line identifier files, with the additional capability of
 * containing just one column). When a line is "visited" it can have a state
 * object associated with it. This allows any known information associated
 * with the line to be restored.
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

    /** Backing spectrum for reading coordinates and providing transformation
      * facilities */
    private LineIDSpecData specData = null;

    /** Storage for labels. These need to be unique so include coordinate */
    private ArrayList labels = new ArrayList();

    /** Local constants for various actions. */
    public static final int FIRST = -2;
    public static final int PREV = -1;
    public static final int NEXT = 1;
    public static final int LAST = 2;

    /** JComboBox for selecting a coordinate directly and displaying the
     *  current value */
    private JComboBox lineBox = null;
    private LineListModel lineBoxModel = null;

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
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;

        //  Current line, is displayed and selectable by this control.
        lineBoxModel = new LineListModel();
        lineBox = new JComboBox();
        lineBox.addActionListener( this );
        layouter.add( lineBox, gbc );

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
        if ( newPosition != position ) {
            double[] coords = specData.getXData();
            if ( coords != null && coords.length > 0 ) {

                //  Get current state and store for next time.
                if ( position != -1 ) {
                    states[position] = provider.getLineState();
                }

                //  Move to new line. Pass stored state so it can be restored.
                position = newPosition;
                provider.viewLine( coords[position], states[position] );

                //  Set the lineBox to show this value.
                if ( updateLineBox ) {
                    lineBox.setSelectedIndex( position );
                }

            }
        }
    }

    /**
     * Read a simple text file of line positions. XXX deal with coordinate
     * system.
     */
    public void readLines( File file )
        throws SplatException
    {
        if ( ! file.exists() ) return;

        LineIDTXTSpecDataImpl impl = new LineIDTXTSpecDataImpl( file );
        LineIDSpecData specData = new LineIDSpecData( impl );

        //  Success, so clear existing lists and keep this SpecData;
        this.specData = specData;
        double[] coords = specData.getXData();
        states = new Object[coords.length];

        //  Generate labels for JComboBox. Must be unique, so add index.
        labels.clear();
        String[] specLabels = specData.getLabels();
        for ( int i = 0; i < specLabels.length; i++ ) {
            labels.add( i + ": " + specLabels[i] );
        }

        //  Move to first line. Reset model to force update of lineBox.
        position = -1;
        lineBox.setModel( lineBoxModel );
        step( FIRST );
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
            if ( specData != null && position != -1 ) {
                return labels.get( position );
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
