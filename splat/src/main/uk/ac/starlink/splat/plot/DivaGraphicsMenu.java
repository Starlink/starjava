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

package uk.ac.starlink.splat.plot;

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

import uk.ac.starlink.splat.data.SpecDataComp;
import uk.ac.starlink.util.gui.BasicFontChooser;

/**
 * A menu with graphics related items, for drawing and manipulating
 * figures on a DivaPlot.
 *
 * @author Allan Brighton
 * @author Peter W. Draper
 * @version $Id$
 */
public class DivaGraphicsMenu
    extends JMenu
{
    /** Object managing the drawing */
    protected DivaPlotCanvasDraw canvasDraw;

    /** Array of menu items. */
    protected JRadioButtonMenuItem[] drawingModeMenuItems =
        new JRadioButtonMenuItem[DivaPlotCanvasDraw.NUM_DRAWING_MODES];

    /** Save Graphics Menu item */
    protected JMenuItem saveGraphicsMenuItem;

    /** Create a menu with graphics related items */
    public DivaGraphicsMenu( DivaPlotCanvasDraw canvasDraw )
    {
        super( "Graphics" );
        this.canvasDraw = canvasDraw;
        add( createDrawingModeMenu() );
        add( createLineWidthMenu() );
        add( updateOutlineMenu( true ) );
        add( updateFillMenu( true ) );
        add( createCompositeMenu() );
        add( updateFontMenu() );
        add( createInterpMenu() );
        add( createPolyDegreeMenu() );
        addSeparator();
        add( canvasDraw.deleteSelectedAction );
        add( canvasDraw.clearAction );
        addSeparator();
        add( canvasDraw.raiseSelectedAction );
        add( canvasDraw.lowerSelectedAction );
        addSeparator();
        add( new JCheckBoxMenuItem( canvasDraw.hideGraphicsAction ) );
        addSeparator();
        saveGraphicsMenuItem = createSaveGraphicsWithImageMenuItem();
        add( saveGraphicsMenuItem );
    }

    /** Create the "Drawing Mode" menu */
    protected JMenu createDrawingModeMenu()
    {
        JMenu menu = new JMenu( "Drawing mode" );
        ButtonGroup group = new ButtonGroup();

        for ( int i = 0; i < canvasDraw.NUM_DRAWING_MODES; i++ ) {
            createDrawingModeMenuItem(i, menu, group);
        }
        drawingModeMenuItems[0].setSelected(true);

        // arrange to select the menu item when the mode is changed
        canvasDraw.addChangeListener( new ChangeListener()
        {
            public void stateChanged( ChangeEvent e )
            {
                int drawingMode = canvasDraw.getDrawingMode();
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
            ( canvasDraw.getDrawingModeAction( drawingMode ) );
        menu.add( menuItem );
        group.add( menuItem );
        drawingModeMenuItems[drawingMode] = menuItem;
    }

    /** Create the "Line Width" menu */
    protected JMenu createLineWidthMenu()
    {
        JMenu menu = new JMenu( "Line width" );
        ButtonGroup group = new ButtonGroup();
        int n = canvasDraw.NUM_LINE_WIDTHS;

        for ( int i = 0; i < n; i++ ) {
            JRadioButtonMenuItem menuItem =
                new JRadioButtonMenuItem( canvasDraw.getLineWidthAction( i ) );
            menu.add( menuItem );
            group.add( menuItem );
        }
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
        int n = canvasDraw.getColorCount();
        for (int i = 0; i < n; i++) {
            JRadioButtonMenuItem menuItem =
                new JRadioButtonMenuItem( canvasDraw.getOutlineAction( i ) );
            menuItem.setBackground( canvasDraw.getColor( i ) );
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
            canvasDraw.setOutline( canvasDraw.getColor( n - 1 ) );
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
        int n = canvasDraw.getColorCount();
        for ( int i = 0; i < n; i++ ) {
            JRadioButtonMenuItem menuItem =
                new JRadioButtonMenuItem( canvasDraw.getFillAction( i ) );
            menuItem.setBackground( canvasDraw.getColor( i ) );
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
            canvasDraw.setFill( canvasDraw.getColor( n - 1 ) );
        }
        return fillMenu;
    }

    public void selectColor( boolean flag )
    {
        Color newColor = JColorChooser.showDialog( this, "Choose Colour",
                                                   Color.white );
        if ( newColor != null ) {
            canvasDraw.addColor( newColor );
            updateOutlineMenu( flag );
            updateFillMenu( ! flag );
        }
    }

    /** Create the "Composite" menu */
    protected JMenu createCompositeMenu()
    {
        JMenu menu = new JMenu( "Composite" );
        ButtonGroup group = new ButtonGroup();
        int n = canvasDraw.NUM_COMPOSITES;
        for ( int i = 0; i < n; i++ ) {
            JRadioButtonMenuItem menuItem =
                new JRadioButtonMenuItem( canvasDraw.getCompositeAction(i) );
            menu.add( menuItem );
            group.add( menuItem );
        }
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

        int n = canvasDraw.fontCount();
        JRadioButtonMenuItem menuItem = null;

        for ( int i = 0; i < n; i++ ) {
            menuItem = new JRadioButtonMenuItem( canvasDraw.getFontAction(i) );
            menuItem.setFont( canvasDraw.getFont( i ) );
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
        canvasDraw.setFont( canvasDraw.getFont( n - 1 ) );
        return fontMenu;
    }

    BasicFontChooser fontChooser = new BasicFontChooser( null, "Choose font",
                                                         true );
    public void selectFont()
    {
        fontChooser.show();
        if ( fontChooser.accepted() ) {
            System.out.println( "font accepted" );
            Font newFont = fontChooser.getSelectedFont();
            if ( newFont != null ) {
                System.out.println( newFont );
                canvasDraw.addFont( newFont );
                updateFontMenu();
            }
        }
    }

    /** Create the interpolated curves menu */
    protected JMenu createInterpMenu()
    {
        JMenu menu = new JMenu( "Curve type" );

        ButtonGroup group = new ButtonGroup();
        int n = canvasDraw.NUM_CURVES;
        for ( int i = 0; i < n; i++ ) {
            JRadioButtonMenuItem menuItem =
                new JRadioButtonMenuItem( canvasDraw.getCurveAction(i) );
            menu.add( menuItem );
            group.add( menuItem );
        }
        return menu;
    }

    /** Create the interpolated curves polynomial degree menu */
    protected JMenu createPolyDegreeMenu()
    {
        JMenu menu = new JMenu( "Polynomial degree" );

        ButtonGroup group = new ButtonGroup();
        int n = canvasDraw.MAX_POLYDEGREE;
        for ( int i = 0; i < n; i++ ) {
            JRadioButtonMenuItem menuItem =
                new JRadioButtonMenuItem( canvasDraw.getPolyDegreeAction(i) );
            menu.add( menuItem );
            group.add( menuItem );
        }
        return menu;
    }

    /** Create and return the "Save Graphics With Image" menu item. */
    protected JMenuItem createSaveGraphicsWithImageMenuItem()
    {
        JMenuItem menuItem = new JMenuItem( "Save graphics" );
        menuItem.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent ae )
            {
                System.out.println( "Save graphics somehow!!" );
            }
        } );
        return menuItem;
    }

    /**
     * test main: usage: java GraphicsImageDisplay <filename>.
     */
    public static void main( String[] args ) {
        try {
            JFrame frame = new JFrame( "DivaPlotGraphicsMenu" );
            DivaPlot plot = new DivaPlot( new SpecDataComp() );

            DivaPlotCanvasDraw canvasDraw = new DivaPlotCanvasDraw( plot );
            JMenuBar menubar = new JMenuBar();
            menubar.add( new DivaGraphicsMenu( canvasDraw ) );

            frame.getContentPane().add( menubar, BorderLayout.NORTH );
            frame.getContentPane().add( plot, BorderLayout.CENTER );
            frame.pack();
            frame.setVisible( true );
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
