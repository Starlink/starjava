package uk.ac.starlink.frog.iface;

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;

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

import uk.ac.starlink.frog.util.FrogDebug;

import uk.ac.starlink.frog.iface.themes.AquaTheme;
import uk.ac.starlink.frog.iface.themes.BigContrastMetalTheme;
import uk.ac.starlink.frog.iface.themes.CharcoalTheme;
import uk.ac.starlink.frog.iface.themes.ContrastTheme;
import uk.ac.starlink.frog.iface.themes.EmeraldTheme;
import uk.ac.starlink.frog.iface.themes.PresentationTheme;
import uk.ac.starlink.frog.iface.themes.RubyTheme;
import uk.ac.starlink.frog.iface.themes.SandStoneTheme;

/**
 * Class that manages the look and feel used in FROG. This offers the
 * ability to populate a menu with the available look and feels. It also
 * provides menus that display the available Metal themes. Based heavily
 * on the SPLAT LookAndFeelManger, only real modification is to include
 * FROG specific debug messages.
 * <p>
 * All changes are propagated to the whole application.
 *
 * @since $Date$
 * @since 20-JUL-2001
 * @author Peter W. Draper & Alasdair Allan
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 */
public class LookAndFeelManager implements ActionListener
{

   /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();
    
    /**
     *  The default look and feel.
     */
    protected String defaultLook =
       UIManager.getCrossPlatformLookAndFeelClassName();

    /**
     * The menu to populate with look and feels options and the
     * available metal themes.
     */
    protected JMenu targetMenu = null;

    /**
     * The metal themes menu.
     */
    protected JMenu themeMenu = null;

    /**
     * Mapping of metal theme names to classes.
     */
    protected Object[][] themeMapping = {
        {"Default" , DefaultMetalTheme.class},
        {"Aqua", AquaTheme.class},
        {"Ruby", RubyTheme.class},
        {"Emerald", EmeraldTheme.class},
        {"Contrast", ContrastTheme.class},
        {"Charcoal", CharcoalTheme.class},
        {"Low vision", BigContrastMetalTheme.class},
        {"Presentation", PresentationTheme.class},
        {"SandStone", SandStoneTheme.class}
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
     * @param debugManager manager object for debugging information
     */
    public LookAndFeelManager( JComponent parent, JMenu targetMenu )
    {
        debugManager.print( "      Calling LookAndFeelManger()...");

        this.targetMenu = targetMenu;
        this.parentWindow = SwingUtilities.getWindowAncestor( parent );
        addLookAndFeels( );
        addThemes( );
    }

    /**
     * Add the available look and feels as a series of radiobuttons in
     * a "Look and Feel" sub-menu.
     */
    protected void addLookAndFeels()
    {
        debugManager.print( "        Calling addLookAndFeels()...");

        ButtonGroup lfGroup = new ButtonGroup();
        JMenu selectLookMenu = new JMenu( "Look and Feel" );

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
       debugManager.print( "setLookAndFeel( " + look + " )");  
       if( defaultLook != look ) {
            defaultLook = look;
            themeMenu.setEnabled(
               look == "javax.swing.plaf.metal.MetalLookAndFeel" );
            updateLookAndFeel();
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
    protected void addThemes( )
    {
        debugManager.print( "        Calling addThemes()...");

        //  Add colour theme support.
        themeMenu = new JMenu( "Colour Theme" );
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
    public void setTheme( DefaultMetalTheme theme )
    {
        MetalLookAndFeel.setCurrentTheme( theme );
        updateLookAndFeel();
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
        String name = item.getText();
        for ( int i = 0; i < themeMapping.length; i++ ) {
            if ( name.equals( themeMapping[i][0] ) ) {
                try {
                    debugManager.print( "setTheme( " + name + " )");

                    Constructor ct =
                         ((Class)themeMapping[i][1]).getConstructor(null);
                    setTheme( (DefaultMetalTheme)ct.newInstance(null) );
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    // Do nothing, it's just a colour!
                }
                break;
            }
        }
    }
}
