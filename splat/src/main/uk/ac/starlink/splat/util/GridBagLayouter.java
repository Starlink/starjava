/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     27-MAY-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import java.awt.Container;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.Box;
import javax.swing.JLabel;

/**
 * Utility class for laying out UI components using a GridBagLayout. 
 * See the various schemes (SCHEME[1-]) for what layouts are supported.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class GridBagLayouter
{
    // Enumerations of schemes that are available.

    /**
     * Simple row by row layout. Everything packed to the left,
     * labels right justified, no fills. Typical usage:
     * <pre>
     *    label: [input component]
     *    label: [input component]
     *    ....
     * </pre>
     */
    public final static int SCHEME1 = 1;

    /**
     * Simple row by row layout, but this time with three components,
     * weighted 0.15 to 0 to 0.85, so that most space is kept by right
     * component. Typical usage:
     * <pre>
     *    [minor input component] label [main input component]
     *    [minor input component] label [main input component]
     *    ....
     * </pre>
     * 
     */
    public final static int SCHEME2 = 2;

    /**
     * Simple row by row layout. Everything packed to the left,
     * labels right justified, last component uses all remaining
     * space. Typical usage:
     * <pre>
     *    label: [long text input component            ]
     *    label: [long text input component            ]
     *    ....
     * </pre>
     */
    public final static int SCHEME3 = 3;

    /**
     * Simple row by row layout, as in SCHEME3, but with the occasion
     * component that fills vertically too. Typical usage:
     * <pre>
     *    label: [long text input component            ]
     *    label: [long text input component            ]
     *    [text/display area                           ]
     *    [....                                        ]
     *    [....                                        ]
     *    ....
     * </pre>
     * a filled area is indicated by a single item on a line.
     */
    public final static int SCHEME4 = 4;

    /** The container we're packing */
    private Container container = null;

    /** Our GridBagConstraints */
    private GridBagConstraints gbc = new GridBagConstraints();

    /** Scheme being used */
    private int scheme = SCHEME1;

    /** Entry number for exact row count schemes. */
    private int entryCount = 0;

    /** Insets used for padding */
    private Insets insets = new Insets( 2, 0, 0, 2 );

    /**
     * Create an instance with the default layout scheme (SCHEME1).
     * Note that container gains a GridBagLayout.
     */
    public GridBagLayouter( Container container )
    {
        this( container, SCHEME1 );
    }

    /**
     * Create an instance with a given layout scheme.
     * Note that container gains a GridBagLayout.
     */
    public GridBagLayouter( Container container, int scheme )
    {
        this.container = container;
        container.setLayout( new GridBagLayout() );
        this.scheme = scheme;
        resetGbc();
    }

    /**
     * Add a control.
     */
    public void add( Component component, boolean complete )
    {
        switch (scheme) {
            case SCHEME1: {
                addScheme1( component, complete );
            }
            break;
            case SCHEME2: {
                addScheme2( component, complete );
            }
            break;
            case SCHEME3: {
                addScheme3( component, complete );
            }
            break;
            case SCHEME4: {
                addScheme4( component, complete );
            }
            break;
        }
    }

    /**
     * Set the Insets. Overrides the default.
     */
    public void setInsets( Insets insets )
    {
        this.insets = insets;
        gbc.insets = insets;
    }

    private void addScheme1( Component component, boolean complete )
    {
        if ( component instanceof JLabel ) {
            setForLabel();
        }
        else {
            setForNonLabel();
        }
        container.add( component, gbc );
        if ( complete ) {
            eatLine();
        }
    }

    private void addScheme2( Component component, boolean complete )
    {
        if ( component instanceof JLabel ) {
            setForLabel();
        }
        else {
            setForNonLabel();
        }
        double weight = 0.0;
        switch ( entryCount ) {
            case 0: {
                weight = 0.15;
            }
            break;
            case 2: {
                weight = 0.85;
            }
            break;
        }
        gbc.weightx = weight;

        if ( complete ) {
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            entryCount = 0;
        }
        else {
            entryCount++;
        }
        container.add( component, gbc );
    }

    private void addScheme3( Component component, boolean complete )
    {
        if ( component instanceof JLabel ) {
            setForLabel();
        }
        else {
            setForNonLabel();
        }
        if ( complete ) {
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
        }
        container.add( component, gbc );
    }

    private void addScheme4( Component component, boolean complete )
    {
        if ( component instanceof JLabel ) {
            setForLabel();
        }
        else {
            setForNonLabel();
        }
        if ( complete ) {
            gbc.weightx = 1.0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            if ( entryCount == 0 ) {
                gbc.fill = GridBagConstraints.BOTH;
                gbc.weighty = 1.0;
            }
            else {
                gbc.fill = GridBagConstraints.HORIZONTAL;
            }
            entryCount = 0;
        }
        else {
            entryCount++;
        }
        container.add( component, gbc );
        gbc.weighty = 0.0;
    }

    /**
     * Add a control using given GridBagContraints.
     */
    public void add( Component component, GridBagConstraints gbc )
    {
        container.add( component, gbc );
    }

    /**
     * Reset GridBagConstraints to default values.
     */
    private void resetGbc()
    {
        gbc.insets = insets;
        switch (scheme) {
            case SCHEME1:
            case SCHEME3: 
            case SCHEME4: {
                gbc.anchor = GridBagConstraints.WEST;
                gbc.fill = GridBagConstraints.NONE;
                gbc.gridwidth = 1;
                gbc.weightx = 0.0;
            }
            break;
            case SCHEME2: {
                gbc.anchor = GridBagConstraints.EAST;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = 1;
                gbc.weightx = 0.0;
            }
            break;
        }
    }

    /**
     * Set GridBagConstraints to values for adding a label.
     */
    private void setForLabel()
    {
        resetGbc();
        switch (scheme) {
            case SCHEME1: 
            case SCHEME3: 
            case SCHEME4: {
               gbc.anchor = GridBagConstraints.EAST;
            }
            break;
            case SCHEME2: {
                gbc.anchor = GridBagConstraints.WEST;
            }
            break;
        }
    }

    /**
     * Set GridBagConstraints to values for adding anything but a label.
     */
    private void setForNonLabel()
    {
        resetGbc();
    }

    /**
     * Eat to end of current line using GridBagLayout.
     */
    private void eatLine()
    {
        resetGbc();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        container.add( Box.createHorizontalGlue(), gbc );
    }

    /**
     * Finish adding by "eating" all remaining space (makes components
     * move to top of component).
     */
    public void eatSpare()
    {
        resetGbc();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        container.add( Box.createVerticalGlue(), gbc );
    }

}
