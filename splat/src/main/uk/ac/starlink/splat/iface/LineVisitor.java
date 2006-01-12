/*
 * Copyright (C) 2006 Particle Physics and Astronomy Research Council
 *
 *  History:
 *     09-JAN-2006 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import uk.ac.starlink.splat.data.LineIDSpecData;
import uk.ac.starlink.splat.data.LineIDTXTSpecDataImpl;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.SplatException;

/**
 * A simple class for controlling a {@link LineProvider}, so that
 * a list of spectral lines can be viewed and analysed, one-by-one.
 * <p>
 * This class provides controls for stepping through a list of lines that are
 * stored in a simple text file as coordinates (the text files are line
 * identifier files, with the additional capability of containing just one
 * column). When a line is "visited" it can have a state object associated
 * with it. This allows any known information associated with the line to be
 * restored.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class LineVisitor
    extends JPanel
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

    /** Local constants for various actions. */
    private static final int FIRST = -2;
    private static final int PREV = -1;
    private static final int NEXT = 1;
    private static final int LAST = 2;

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
    protected void step( int action )
    {
        //  Check for nothing read yet.
        if ( specData == null ) return;
        double[] coords = specData.getXData();
        if ( coords == null || coords.length == 0 ) return;

        //  Determine position within bounds of list, following the given
        //  action.
        int oldPosition = position;
        switch (action) {
           case FIRST: {
               position = 0;
           }
           break;
           case PREV: {
               position--;
           }
           break;
           case NEXT: {
               position++;
           }
           break;
           case LAST: {
               position = coords.length - 1;
           }
           break;
        }
        position = Math.max( 0, Math.min( position, coords.length - 1 ) );
        if ( position != oldPosition ) {

            //  Get current state and store.
            if ( oldPosition != -1 ) {
                states[oldPosition]= provider.getLineState();
            }

            //  Move to new line restoring state.
            provider.viewLine( coords[position], states[position] );
        }
    }

    /**
     * Initialise the various user interface components.
     */
    protected void initUI()
    {
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );

        //  Back to start.
        FirstAction firstAction = new FirstAction();
        JButton firstButton = new JButton( firstAction );
        add( Box.createGlue() );
        add( firstButton );

        //  Back one line.
        PrevAction prevAction = new PrevAction();
        JButton prevButton = new JButton( prevAction );
        add( Box.createGlue() );
        add( prevButton );

        //  Next line.
        NextAction nextAction = new NextAction();
        JButton nextButton = new JButton( nextAction );
        add( Box.createGlue() );
        add( nextButton );

        //  Forward to last.
        LastAction lastAction = new LastAction();
        JButton lastButton = new JButton( lastAction );
        add( Box.createGlue() );
        add( lastButton );
        add( Box.createGlue() );
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

        //  Move to first line.
        position = -1;
        step( FIRST );
    }

    //  Provide enabled so that children are affected.
    public void setEnabled( boolean enabled )
    {
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

}
