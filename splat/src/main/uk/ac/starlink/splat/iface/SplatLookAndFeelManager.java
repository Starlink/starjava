/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     20-JUL-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.util.prefs.Preferences;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

import uk.ac.starlink.splat.iface.themes.AquaTheme;
import uk.ac.starlink.splat.iface.themes.BigContrastMetalTheme;
import uk.ac.starlink.splat.iface.themes.CharcoalTheme;
import uk.ac.starlink.splat.iface.themes.ContrastTheme;
import uk.ac.starlink.splat.iface.themes.EmeraldTheme;
import uk.ac.starlink.splat.iface.themes.PresentationTheme;
import uk.ac.starlink.splat.iface.themes.RubyTheme;
import uk.ac.starlink.splat.iface.themes.SandStoneTheme;

/**
 * Class that manages the look and feel used in SPLAT. This offers the
 * ability to populate a menu with the available look and feels. It also
 * provides menus that display the available Metal themes.
 * <p>
 * All changes are propagated to the whole application.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SplatLookAndFeelManager implements ActionListener
{
    /**
     *  The default look and feel. Was cross-platform now system.
     */
    protected String defaultLook =
        UIManager.getSystemLookAndFeelClassName();

    protected String crossPlatformLook =
        UIManager.getCrossPlatformLookAndFeelClassName();

    /**
     * GTK look and feel.
     */
    private static final String gtk  =
        "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";

    /**
     * Metal look and feel.
     */
    private static final String metal =
        "javax.swing.plaf.metal.MetalLookAndFeel";

    /**
     * Motif look and feel.
     */
    private static final String motif = 
        "com.sun.java.swing.plaf.motif.MotifLookAndFeel";

    /**
     * UI preferences.
     */
    protected static Preferences prefs =
        Preferences.userNodeForPackage( SplatLookAndFeelManager.class );

    /**
     * The menu to populate with look and feels menu and the
     * available metal themes menu.
     */
    protected JMenu targetMenu = null;

    /**
     * The metal themes menu.
     */
    protected JMenu themeMenu = null;

    /**
     * The menu of look and feels.
     */
    protected JMenu selectLookMenu = null;

    /**
     * Mapping of metal theme names to classes.
     */
    protected Object[][] themeMapping = {
        { "Default" , DefaultMetalTheme.class },
        { "Aqua", AquaTheme.class },
        { "Ruby", RubyTheme.class },
        { "Emerald", EmeraldTheme.class },
        { "Contrast", ContrastTheme.class },
        { "Charcoal", CharcoalTheme.class },
        { "Low vision", BigContrastMetalTheme.class },
        { "Presentation", PresentationTheme.class },
        { "SandStone", SandStoneTheme.class }
    };

    /**
     * The parent of the menu that we're populating.
     */
    protected Window parentWindow = null;

    /**
     * Constructor. Single argument is the menu to populate
     *
     * @param parent some component of the window that contains the
     *               menu.
     * @param targetMenu the menu to populate with the look and feel
     *                   items.
     */
    public SplatLookAndFeelManager( JComponent parent, JMenu targetMenu )
    {
        this.targetMenu = targetMenu;
        this.parentWindow = SwingUtilities.getWindowAncestor( parent );

        if ( defaultLook == null || defaultLook.equals( motif ) ) {
            //  No system look and feel, or just too ugly.
            defaultLook = crossPlatformLook;
        }

        //  Restore users last default look and theme.
        defaultLook = prefs.get( "SplatLookAndFeelManager_look", defaultLook );
        String lastTheme = 
            prefs.get( "SplatLookAndFeelManager_theme", "Default" );

        addLookAndFeels();
        addThemes();

        if ( ! "Default".equals( lastTheme ) ) {
            setThemeFromName( lastTheme  );
        }
        updateLookAndFeel();
    }

    /**
     * Add the available look and feels as a series of radiobuttons in
     * a "Look and Feel" sub-menu.
     */
    protected void addLookAndFeels()
    {
        ButtonGroup lfGroup = new ButtonGroup();
        selectLookMenu = new JMenu( "Look and feel" );

        //  1.4.2 hack, GTK isn't available by default yet. Remove for
        //  Java 1.5.
        UIManager.installLookAndFeel( "GTK", gtk );

        final UIManager.LookAndFeelInfo[] lfInfo =
            UIManager.getInstalledLookAndFeels();

        final JRadioButtonMenuItem lfItems[] =
            new JRadioButtonMenuItem[lfInfo.length];

        for ( int i = 0; i < lfInfo.length; i++ ) {
            final UIManager.LookAndFeelInfo info = lfInfo[i];
            String name = info.getName();
            boolean selected = info.getClassName().equals( defaultLook );
            lfItems[i] = new JRadioButtonMenuItem( name, selected );
            lfItems[i].addActionListener( new ActionListener() {
                    public void actionPerformed( ActionEvent e ) {
                        setLookAndFeel( info.getClassName() );
                    }
                } );

            selectLookMenu.add( lfItems[i] );
            lfGroup.add( lfItems[i] );
            lfItems[i].setEnabled( haveLookAndFeel( info.getClassName() ) );
        }
        targetMenu.add( selectLookMenu );
    }

    /**
     * Set the default look and feel. The related colour and font
     * themes are only available with the Metal look.
     *
     * @param look the look and feel to use.
     */
    public void setLookAndFeel( String look )
    {
        if ( ! defaultLook.equals( look ) ) {
            defaultLook = look;
            themeMenu.setEnabled( look.equals( metal ) );
            updateLookAndFeel();
            prefs.put( "SplatLookAndFeelManager_look", defaultLook );
        }
    }

    /**
     * Check if a named look and feel is really available.
     *
     * @param look the look and feel to check.
     *
     * @return true if the look and feel is available.
     */
    public boolean haveLookAndFeel( String look )
    {
        try {
            Class lookClass = this.getClass().forName( look );
            LookAndFeel newLook = (LookAndFeel) ( lookClass.newInstance() );
            return newLook.isSupportedLookAndFeel();
        }
        catch ( Exception e ) {
            return false;
        }
    }

    /**
     * Add all the known themes to another menu.
     */
    protected void addThemes()
    {
        //  Add colour theme support.
        themeMenu = new JMenu( "Colour theme" );
        targetMenu.add( themeMenu );
        JMenuItem item;
        for ( int i = 0; i < themeMapping.length; i++ ) {
            item = new JMenuItem( (String) themeMapping[i][0] );
            item.addActionListener( this );
            themeMenu.add( item );
        }
    }

    /**
     * Set the colour theme.
     *
     * @param theme the builtin theme to apply (only works with Metal look
     *              and feel).
     */
    protected void setTheme( DefaultMetalTheme theme )
    {
        MetalLookAndFeel.setCurrentTheme( theme );
        updateLookAndFeel();
    }

    /**
     * Set the colour theme using a symbolic name (in themeMapping).
     */
    public void setThemeFromName( String name )
    {
        for ( int i = 0; i < themeMapping.length; i++ ) {
            if ( themeMapping[i][0].equals( name ) ) {
                try {
                    Constructor ct = 
                        ((Class)themeMapping[i][1]).getConstructor(null);
                    setTheme( (DefaultMetalTheme)ct.newInstance(null) );
                    prefs.put( "SplatLookAndFeelManager_theme", name );
                }
                catch (Exception ex) {
                    System.out.println( ex.getMessage() );
                    // It's just a colour scheme!
                }
                break;
            }
        }
    }

    /**
     * Apply the default look and feel to the whole program.
     */
    public void updateLookAndFeel()
    {
        try {
            UIManager.setLookAndFeel( defaultLook );
            SwingUtilities.updateComponentTreeUI( parentWindow );
        }
        catch ( Exception e ) {
            e.printStackTrace();
            JOptionPane.showMessageDialog( parentWindow,
                                           e.getMessage(),
                                           "Error setting look and feel",
                                           JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Update look and feel for all Frames (plots etc.).
        Frame[] frameList = Frame.getFrames();
        for (int i = 0; i < frameList.length; i++) {
            final Frame frame = frameList[i];
            SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        SwingUtilities.updateComponentTreeUI( frame );
                    }
                });
        }
    }

//
//  Implement the ActionListener interface for theme changes.
//
    public void actionPerformed( ActionEvent e )
    {
        JMenuItem item = (JMenuItem) e.getSource();
        setThemeFromName( item.getText() );
    }
}
