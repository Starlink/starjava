/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 * History:
 *    02-DEC-2003 (PWD):
 *       Converted from JSky for SPLAT.
 */
package uk.ac.starlink.diva;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.diva.interp.InterpolatorFactory;
import uk.ac.starlink.util.gui.BasicFontChooser;

/**
 * A menu with graphics related actions from a {@link DrawActions} instance
 * for drawing and manipulating figures on a {@link Draw} implementation.
 *
 * @author Allan Brighton
 * @author Peter W. Draper
 * @version $Id$
 */
public class DrawGraphicsMenu
    extends JMenu
{
    /** Object managing the drawing */
    protected DrawActions drawActions;

    /** Array of menu items. */
    protected JRadioButtonMenuItem[] drawingModeMenuItems =
        new JRadioButtonMenuItem[DrawActions.NUM_DRAWING_MODES];

    /** Save and restore graphics menu item */
    protected JMenuItem saveRestoreGraphicsMenuItem;

    /** The FigureStore used to save and restore figures. */
    protected FigureStore store = null;

    /** Create a menu with graphics related items */
    public DrawGraphicsMenu( DrawActions drawActions )
    {
        super( "Graphics" );
        this.drawActions = drawActions;
        add( createDrawingModeMenu() );
        add( createLineWidthMenu() );
        add( updateOutlineMenu( false ) );
        add( updateFillMenu( true ) );
        add( createCompositeMenu() );
        add( updateFontMenu() );
        add( createInterpMenu() );
        addSeparator();
        add( drawActions.deleteSelectedAction );
        add( drawActions.clearAction );
        addSeparator();
        add( drawActions.raiseSelectedAction );
        add( drawActions.lowerSelectedAction );
        addSeparator();
        add( new JCheckBoxMenuItem( drawActions.hideGraphicsAction ) );
        addSeparator();
        add( drawActions.saveRestoreAction );
    }

    /** Create the "Drawing Mode" menu */
    protected JMenu createDrawingModeMenu()
    {
        JMenu menu = new JMenu( "Drawing mode" );
        ButtonGroup group = new ButtonGroup();

        for ( int i = 0; i < drawActions.NUM_DRAWING_MODES; i++ ) {
            createDrawingModeMenuItem( i, menu, group );
        }
        drawingModeMenuItems[0].setSelected( true );

        // arrange to select the menu item when the mode is changed
        drawActions.addChangeListener( new ChangeListener()
        {
            public void stateChanged( ChangeEvent e )
            {
                int drawingMode = drawActions.getDrawingMode();
                try {
                    boolean selected =
                        drawingModeMenuItems[drawingMode].isSelected();
                    if ( ! selected ) {
                        drawingModeMenuItems[drawingMode].setSelected( true );
                    }
                }
                catch ( Exception ee ) {
                    // Do nothing
                }
            }
        });

        return menu;
    }

    /** Create the menu item for the given mode */
    protected void createDrawingModeMenuItem( int drawingMode, JMenu menu,
                                              ButtonGroup group )
    {
        JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem
            ( drawActions.getDrawingModeAction( drawingMode ) );
        menu.add( menuItem );
        group.add( menuItem );
        drawingModeMenuItems[drawingMode] = menuItem;
    }

    /** Create the "Line Width" menu */
    protected JMenu createLineWidthMenu()
    {
        JMenu menu = new JMenu( "Line width" );
        ButtonGroup group = new ButtonGroup();
        int n = drawActions.NUM_LINE_WIDTHS;

        for ( int i = 0; i < n; i++ ) {
            JRadioButtonMenuItem menuItem =
                new JRadioButtonMenuItem( drawActions.getLineWidthAction( i ) );
            menu.add( menuItem );
            group.add( menuItem );
        }
        menu.getItem( 0 ).setSelected( true );
        return menu;
    }

    JMenu outlineMenu = null;

    /**
     * Create or update the "Outline" menu. If set is true assume last
     * entry has just been added and make it current.
     */
    protected JMenu updateOutlineMenu( boolean set )
    {
        // This consists of the fixed set, plus an additional field
        // for user to choose their own.
        if ( outlineMenu == null ) {
            outlineMenu = new JMenu( "Outline" );
        }
        else {
            outlineMenu.removeAll();
        }
        ButtonGroup group = new ButtonGroup();
        int n = drawActions.getColorCount();
        for (int i = 0; i < n; i++) {
            JRadioButtonMenuItem menuItem =
                new JRadioButtonMenuItem( drawActions.getOutlineAction( i ) );
            menuItem.setBackground( drawActions.getColor( i ) );
            outlineMenu.add( menuItem );
            group.add( menuItem );
        }

        JMenuItem bespokeItem =
            new JMenuItem( new AbstractAction( "Choose colour..." )
            {
                public void actionPerformed( ActionEvent e )
                {
                    selectColor( true );
                }
            } );
        outlineMenu.add( bespokeItem );
        group.add( bespokeItem );
        if ( set ) {
            outlineMenu.getItem( n - 1 ).setSelected( true );
            drawActions.setOutline( drawActions.getColor( n - 1 ) );
        }
        return outlineMenu;
    }

    JMenu fillMenu = null;

    /**
     * Create or update the "Fill" menu, if set is true assume last
     * entry has just been added and make it current.
     */
    protected JMenu updateFillMenu( boolean set )
    {
        // This consists of the fixed set, plus an additional field
        // for user to choose their own.
        if ( fillMenu == null ) {
            fillMenu = new JMenu( "Fill" );
        }
        else {
            fillMenu.removeAll();
        }
        ButtonGroup group = new ButtonGroup();
        int n = drawActions.getColorCount();
        for ( int i = 0; i < n; i++ ) {
            JRadioButtonMenuItem menuItem =
                new JRadioButtonMenuItem( drawActions.getFillAction( i ) );
            menuItem.setBackground( drawActions.getColor( i ) );
            fillMenu.add( menuItem );
            group.add( menuItem );
        }

        JMenuItem bespokeItem =
            new JMenuItem( new AbstractAction( "Choose colour..." )
            {
                public void actionPerformed( ActionEvent e )
                {
                    selectColor( false );
                }
            } );
        fillMenu.add( bespokeItem );
        group.add( bespokeItem );
        if ( set ) {
            fillMenu.getItem( n - 1 ).setSelected( true );
            drawActions.setFill( drawActions.getColor( n - 1 ) );
        }
        return fillMenu;
    }

    public void selectColor( boolean flag )
    {
        Color newColor = JColorChooser.showDialog( this, "Choose Colour",
                                                   Color.white );
        if ( newColor != null ) {
            drawActions.addColor( newColor );
            updateOutlineMenu( flag );
            updateFillMenu( ! flag );
        }
    }

    /** Create the "Composite" menu */
    protected JMenu createCompositeMenu()
    {
        JMenu menu = new JMenu( "Composite" );
        ButtonGroup group = new ButtonGroup();
        int n = drawActions.NUM_COMPOSITES;
        for ( int i = 0; i < n; i++ ) {
            JRadioButtonMenuItem menuItem =
                new JRadioButtonMenuItem( drawActions.getCompositeAction(i) );
            menu.add( menuItem );
            group.add( menuItem );
        }
        menu.getItem( n - 1 ).setSelected( true );
        return menu;
    }


    JMenu fontMenu = null;

    /** Update or create the "Font" menu */
    protected JMenu updateFontMenu()
    {
        // This consists of the fixed set, plus an additional field
        // for user to choose their own.
        if ( fontMenu == null ) {
            fontMenu = new JMenu( "Font" );
        }
        else {
            fontMenu.removeAll();
        }
        ButtonGroup fontGroup = new ButtonGroup();

        int n = drawActions.fontCount();
        JRadioButtonMenuItem menuItem = null;

        for ( int i = 0; i < n; i++ ) {
            menuItem = new JRadioButtonMenuItem( drawActions.getFontAction(i) );
            menuItem.setFont( drawActions.getFont( i ) );
            fontMenu.add( menuItem );
            fontGroup.add( menuItem );
        }

        JMenuItem bespokeItem =
            new JMenuItem( new AbstractAction( "Choose font..." )
            {
                public void actionPerformed( ActionEvent e )
                {
                    selectFont();
                }
            } );
        fontMenu.add( bespokeItem );
        fontGroup.add( menuItem );
        fontMenu.getItem( n - 1 ).setSelected( true );
        drawActions.setFont( drawActions.getFont( n - 1 ) );
        return fontMenu;
    }

    BasicFontChooser fontChooser = new BasicFontChooser( null, "Choose font",
                                                         true );
    public void selectFont()
    {
        fontChooser.show();
        if ( fontChooser.accepted() ) {
            Font newFont = fontChooser.getSelectedFont();
            if ( newFont != null ) {
                drawActions.addFont( newFont );
                updateFontMenu();
            }
        }
    }

    /** Create the interpolated curves menu */
    protected JMenu createInterpMenu()
    {
        JMenu menu = new JMenu( "Curve type" );

        ButtonGroup group = new ButtonGroup();
        int n = drawActions.getInterpolatorFactory().getInterpolatorCount();
        for ( int i = 0; i < n; i++ ) {
            JRadioButtonMenuItem menuItem =
                new JRadioButtonMenuItem( drawActions.getCurveAction( i ) );
            menu.add( menuItem );
            group.add( menuItem );
        }
        menu.getItem( 0 ).setSelected( true );
        return menu;
    }
}
