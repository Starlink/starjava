/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ImageDisplayMenuBar.java,v 1.31 2002/08/16 22:21:13 brighton Exp $
 */

package jsky.image.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.NumberFormat;
import java.util.Locale;

import javax.media.jai.Interpolation;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import jsky.image.ImageChangeEvent;
import jsky.image.ImageProcessor;
import jsky.image.graphics.gui.ImageGraphicsMenu;
import jsky.util.I18N;
import jsky.util.Preferences;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.GenericToolBar;
import jsky.util.gui.LookAndFeelMenu;
import jsky.util.Resources;


/**
 * Implements a menubar for an ImageDisplayControl.
 *
 * @version $Revision: 1.31 $
 * @author Allan Brighton
 */
public class ImageDisplayMenuBar extends JMenuBar {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(ImageDisplayMenuBar.class);

    /** Maximum scale (zoom) factor for menu */
    public static final float MAX_SCALE = 20.0F;

    /** Minimum scale (zoom) factor for menu */
    public static final float MIN_SCALE = 1.0F/MAX_SCALE;

    // Used to format magnification settings < 1. 
    private static NumberFormat _scaleFormat = NumberFormat.getInstance(Locale.US);

    static {
        _scaleFormat.setMaximumFractionDigits(1);
    }

    // Target image window 
    private DivaMainImageDisplay _imageDisplay;

    // The current image window (for the Go/history menu, which may be shared by
    // multiple image displays)
    private static DivaMainImageDisplay _currentImageDisplay;

    // The toolbar associated with the image display 
    private GenericToolBar _toolBar;

    // Handle for the File menu 
    private JMenu _fileMenu;

    // Handle for the Edit menu 
    private JMenu _editMenu;

    // Handle for the View menu 
    private JMenu _viewMenu;

    // Handle for the Go menu 
    private JMenu _goMenu;

    // Handle for the Graphics menu 
    private JMenu _graphicsMenu;

    // The scale and zoom submenus 
    private JMenu _scaleMenu;
    private JMenu _zoomInMenu;
    private JMenu _zoomOutMenu;
    private ButtonGroup _zoomInOutGroup = new ButtonGroup();

    // The Exit menu item 
    private JMenuItem _newWindowMenuItem;

    // The Image Properties menu item 
    private JMenuItem _imagePropertiesMenuItem;

    // The FITS Keywords menu item 
    private JMenuItem _fitsKeywordsMenuItem;

    // The FITS Extensions menu item 
    private JMenuItem _fitsExtensionsMenuItem;

    // The Pick Object menu item 
    private JMenuItem _pickObjectMenuItem;


    /**
     * Create the menubar for the given main image display.
     *
     * @param imageDisplay the target image display
     * @param toolBar the toolbar associated with this menubar (shares some actions)
     */
    public ImageDisplayMenuBar(final DivaMainImageDisplay imageDisplay, GenericToolBar toolBar) {
        super();
        _imageDisplay = imageDisplay;
        _toolBar = toolBar;

        add(_fileMenu = createFileMenu());
        //add(_editMenu = createEditMenu());
        add(_viewMenu = createViewMenu());
        add(_goMenu = createGoMenu(null));
        add(_graphicsMenu = new ImageGraphicsMenu(imageDisplay.getCanvasDraw()));

        // Arrange to always set the current image display for use by the ImageHistoryItem class,
        // since the same items may be in the menus of multiple image displays
        _goMenu.addMenuListener(new MenuListener() {

            public void menuSelected(MenuEvent e) {
                _currentImageDisplay = imageDisplay;
            }

            public void menuDeselected(MenuEvent e) {
            }

            public void menuCanceled(MenuEvent e) {
            }
        });

        // keep the Go history menu up to date
        imageDisplay.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent ce) {
                ImageChangeEvent e = (ImageChangeEvent) ce;
                if (e.isNewImage() && !e.isBefore()) {
                    _goMenu.removeAll();
                    createGoMenu(_goMenu);

                    // enable/disable some items
                    if (imageDisplay.getFitsImage() != null) {
                        _fitsExtensionsMenuItem.setEnabled(true);
                        _fitsKeywordsMenuItem.setEnabled(true);
                        _pickObjectMenuItem.setEnabled(true);
                        _imagePropertiesMenuItem.setEnabled(false);
                    }
                    else {
                        _fitsExtensionsMenuItem.setEnabled(false);
                        _fitsKeywordsMenuItem.setEnabled(false);
                        _pickObjectMenuItem.setEnabled(false);
                        _imagePropertiesMenuItem.setEnabled(true);
                    }
                }
            }
        });
    }

    /**
     * Return the current image window (for the Go/history menu, which may be shared by
     * multiple image displays);
     */
    public static DivaMainImageDisplay getCurrentImageDisplay() {
        return _currentImageDisplay;
    }

    /**
     * Set the current image window (for the Go/history menu, which may be shared by
     * multiple image displays);
     */
    public static void setCurrentImageDisplay(DivaMainImageDisplay imageDisplay) {
        _currentImageDisplay = imageDisplay;
    }


    /**
     * Create the File menu.
     */
    protected JMenu createFileMenu() {
        JMenu menu = new JMenu(_I18N.getString("file"));
        menu.add(_imageDisplay.getOpenAction());
        menu.add(createFileOpenURLMenuItem());
        menu.addSeparator();
        menu.add(createFileClearImageMenuItem());
        menu.addSeparator();
        menu.add(_imageDisplay.getSaveAction());
        menu.add(_imageDisplay.getSaveAsAction());
        menu.addSeparator();
        menu.add(_imageDisplay.getPrintPreviewAction());
        menu.add(_imageDisplay.getPrintAction());

        menu.addSeparator();

        menu.add(_newWindowMenuItem = createFileNewWindowMenuItem());

        menu.add(createFileCloseMenuItem());

        // check if using internal frames before adding exit item
        JDesktopPane desktop = _imageDisplay.getDesktop();
        if (desktop == null && _imageDisplay.isMainWindow())
            menu.add(createFileExitMenuItem());

        return menu;
    }


    /**
     * Create the File => "Open URL" menu item
     */
    protected JMenuItem createFileOpenURLMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("openURL"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.openURL();
            }
        });
        return menuItem;
    }

    /**
     * Create the File => Clear Image menu item
     */
    protected JMenuItem createFileClearImageMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("clearImage"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.clear();
            }
        });
        return menuItem;
    }


    /**
     * Create the File => "New Window" menu item
     */
    protected JMenuItem createFileNewWindowMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("newWindow"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.newWindow();
            }
        });
        return menuItem;
    }


    /**
     * Create the File => Exit menu item
     */
    protected JMenuItem createFileExitMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("exit"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.exit();
            }
        });
        return menuItem;
    }


    /**
     * Create the File => Close menu item
     */
    protected JMenuItem createFileCloseMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("close"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.close();
            }
        });
        return menuItem;
    }

    /**
     * Create the Edit menu.
     */
    protected JMenu createEditMenu() {
        JMenu menu = new JMenu(_I18N.getString("edit"));
        menu.add(createEditPreferencesMenuItem());
        return menu;
    }

    /**
     * Create the Edit => "Preferences" menu item
     */
    protected JMenuItem createEditPreferencesMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("preferences"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                DialogUtil.error("Sorry, not implemented...");
                //_imageDisplay.editPreferences();
            }
        });
        return menuItem;
    }

    /**
     * Create the View menu.
     */
    protected JMenu createViewMenu() {
        JMenu menu = new JMenu(_I18N.getString("view"));
        menu.add(createViewToolBarMenuItem());
        menu.add(createViewShowToolBarAsMenu());
        menu.addSeparator();

        menu.add(_imageDisplay.getColorsAction());
        menu.add(_imageDisplay.getCutLevelsAction());

        menu.add(_pickObjectMenuItem = createViewPickObjectMenuItem());
        menu.add(_fitsExtensionsMenuItem = createViewFitsExtensionsMenuItem());
        menu.add(_fitsKeywordsMenuItem = createViewFitsKeywordsMenuItem());
        menu.add(_imagePropertiesMenuItem = createViewImagePropertiesMenuItem());
        menu.addSeparator();

        menu.add(createViewScaleMenu());
        menu.add(createViewInterpolationMenu());

        // XXX doesn't currently work well with the non-square images pan window
        //menu.add(createViewRotateMenu());

        // XXX Works okay for jskycat, but not supported by OT yet
        // PWD: coordinates are not correct when flipped.
        //menu.add(createViewFlipXMenuItem());
        //menu.add(createViewFlipYMenuItem());

        menu.add(createViewSmoothScrollingMenuItem());

        // XXX Switching back and forth changes the GUI colors and does not reset them again
        //
        // Only add Look and Feel item if not using internal frames
        // (otherwise its in the main Image menu)
        if (_imageDisplay.getRootComponent() instanceof JFrame) {
            menu.addSeparator();
            menu.add(new LookAndFeelMenu());
        }

        return menu;
    }

    /**
     * Create the View => "Toolbar" menu item
     */
    protected JCheckBoxMenuItem createViewToolBarMenuItem() {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(_I18N.getString("toolbar"));

        // name used to store setting in user preferences
        final String prefName = getClass().getName() + ".ShowToolBar";

        menuItem.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                JCheckBoxMenuItem rb = (JCheckBoxMenuItem) e.getSource();
                _toolBar.setVisible(rb.getState());
                if (rb.getState())
                    Preferences.set(prefName, "true");
                else
                    Preferences.set(prefName, "false");
            }
        });

        // check for a previous preference setting
        String pref = Preferences.get(prefName);
        if (pref != null)
            menuItem.setState(pref.equals("true"));
        else
            menuItem.setState(true);

        return menuItem;
    }

    /**
     * Create the View => "Show Toolbar As" menu
     */
    protected JMenu createViewShowToolBarAsMenu() {
        JMenu menu = new JMenu(_I18N.getString("showToolBarAs"));

        JRadioButtonMenuItem b1 = new JRadioButtonMenuItem(_I18N.getString("picAndText"));
        JRadioButtonMenuItem b2 = new JRadioButtonMenuItem(_I18N.getString("picOnly"));
        JRadioButtonMenuItem b3 = new JRadioButtonMenuItem(_I18N.getString("textOnly"));

        b1.setSelected(true);
        _toolBar.setShowPictures(true);
        _toolBar.setShowText(true);

        menu.add(b1);
        menu.add(b2);
        menu.add(b3);

        ButtonGroup group = new ButtonGroup();
        group.add(b1);
        group.add(b2);
        group.add(b3);

        // name used to store setting in user preferences
        final String prefName = getClass().getName() + ".ShowToolBarAs";

        ItemListener itemListener = new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                JRadioButtonMenuItem rb = (JRadioButtonMenuItem) e.getSource();
                if (rb.isSelected()) {
                    if (rb.getText().equals(_I18N.getString("picAndText"))) {
                        _toolBar.setShowPictures(true);
                        _toolBar.setShowText(true);
                        Preferences.set(prefName, "1");
                    }
                    else if (rb.getText().equals(_I18N.getString("picOnly"))) {
                        _toolBar.setShowPictures(true);
                        _toolBar.setShowText(false);
                        Preferences.set(prefName, "2");
                    }
                    else if (rb.getText().equals(_I18N.getString("textOnly"))) {
                        _toolBar.setShowPictures(false);
                        _toolBar.setShowText(true);
                        Preferences.set(prefName, "3");
                    }
                }
            }
        };

        b1.addItemListener(itemListener);
        b2.addItemListener(itemListener);
        b3.addItemListener(itemListener);

        // check for a previous preference setting
        String pref = Preferences.get(prefName);
        if (pref != null) {
            JRadioButtonMenuItem[] ar = new JRadioButtonMenuItem[]{null, b1, b2, b3};
            try {
                ar[Integer.parseInt(pref)].setSelected(true);
            }
            catch (Exception e) {
            }
        }

        return menu;
    }

    /**
     * Create the View => "Cut Levels" menu item
     */
    protected JMenuItem createViewCutLevelsMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("cutLevels") + "...");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.editCutLevels();
            }
        });
        return menuItem;
    }


    /**
     * Create the View => "Colors" menu item
     */
    protected JMenuItem createViewColorsMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("colors") + "...");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.editColors();
            }
        });
        return menuItem;
    }

    /**
     * Create the View => "Pick Object" menu item
     */
    protected JMenuItem createViewPickObjectMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("pickObjects"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.pickObject();
            }
        });
        return menuItem;
    }

    /**
     * Create the View => "FITS Extensions"  menu item
     */
    protected JMenuItem createViewFitsExtensionsMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("fitsExt"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.viewFitsExtensions();
            }
        });
        return menuItem;
    }

    /**
     * Create the View => "FITS Keywords"  menu item
     */
    protected JMenuItem createViewFitsKeywordsMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("fitsKeywords"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.viewFitsKeywords();
            }
        });
        return menuItem;
    }

    /**
     * Create the View => "Image Properties"  menu item
     */
    protected JMenuItem createViewImagePropertiesMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("imageProps"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.viewImageProperties();
            }
        });
        return menuItem;
    }

    /**
     * Get the scale menu label for the given float scale factor.
     */
    public static String getScaleLabel(float f) {
        if (f < 1.0) {
	    int i = Math.round(1.0F/f);
	    return "1/" + i + "x";
	}
        return Integer.toString(Math.round(f)) + "x";
    }

    /**
     * Create the View => "Scale"  menu item
     */
    protected JMenu createViewScaleMenu() {
        _scaleMenu = new JMenu(_I18N.getString("scale"));
        _scaleMenu.add(createViewScaleZoomOutMenu());
        _scaleMenu.add(createViewScaleZoomInMenu());
        _scaleMenu.add(createViewScaleFitToWindowMenuItem());

        _imageDisplay.addChangeListener(new ChangeListener() {
		public void stateChanged(ChangeEvent ce) {
		    ImageChangeEvent e = (ImageChangeEvent) ce;
		    if (e.isNewScale()) {
			float scale = _imageDisplay.getScale();
			String s = getScaleLabel(scale);
			JMenu menu;
			if (scale < 1) 
			    menu = _zoomOutMenu;
			else
			    menu = _zoomInMenu;
			int n = menu.getItemCount();
			for (int i = 0; i < n; i++) {
			    JRadioButtonMenuItem b = (JRadioButtonMenuItem) menu.getItem(i);
			    if (b.getText().equals(s))
				b.setSelected(true);
			}
		    }
		}
	    });
	return _scaleMenu;
    }

    /**
     * Create the View => "Scale" => "Zoom Out"  menu item
     */
    protected JMenu createViewScaleZoomOutMenu() {
        _zoomOutMenu = new JMenu(_I18N.getString("zoomOut"));
        for (int i = 1; i <= MAX_SCALE; i++) {
            addScaleMenuItem(_zoomOutMenu, _zoomInOutGroup, "1/" + i + "x", 1.0F/(float)i);
        }

        return _zoomOutMenu;
    }

    /**
     * Create the View => "Scale" => "Zoom In"  menu item
     */
    protected JMenu createViewScaleZoomInMenu() {
        _zoomInMenu = new JMenu(_I18N.getString("zoomIn"));
        for (int i = 1; i <= MAX_SCALE; i++) {
            addScaleMenuItem(_zoomInMenu, _zoomInOutGroup, Integer.toString(i) + "x", (float)i);
        }

        return _zoomInMenu;
    }


    /**
     * Create the View => "Scale" => "Fit Image in Window"  menu item
     */
    protected JMenuItem createViewScaleFitToWindowMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("fitImageInWindow"));

        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.scaleToFit();
                _imageDisplay.updateImage();
            }
        });
        return menuItem;
    }



    /**
     * Add a radio button menu item to the scale menu and given group
     * with the given label and scale value.
     */
    protected void addScaleMenuItem(JMenu menu, ButtonGroup group, String label, float value) {
        JRadioButtonMenuItem b = new JRadioButtonMenuItem(label);
        b.setActionCommand(Float.toString(value));
        b.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setScale(Float.parseFloat(e.getActionCommand()));
            }
        });
        group.add(b);
        menu.add(b);
    }


    /**
     * Set the scale for the image to the given value and update the menu
     * label.
     */
    public void setScale(float value) {
        _imageDisplay.setScale(value);
        _imageDisplay.updateImage();
    }


    /**
     * Create the View => "Scale Interpolation"  menu item
     */
    protected JMenu createViewInterpolationMenu() {
        JMenu menu = new JMenu(_I18N.getString("scaleInt"));

        JRadioButtonMenuItem b1 = new JRadioButtonMenuItem("Nearest");
        JRadioButtonMenuItem b2 = new JRadioButtonMenuItem("Bilinear");
        JRadioButtonMenuItem b3 = new JRadioButtonMenuItem("Bicubic");
        JRadioButtonMenuItem b4 = new JRadioButtonMenuItem("Bicubic2");

        b1.setSelected(true);
        menu.add(b1);
        menu.add(b2);
        menu.add(b3);
        menu.add(b4);

        ButtonGroup group = new ButtonGroup();
        group.add(b1);
        group.add(b2);
        group.add(b3);
        group.add(b4);

        ItemListener itemListener = new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                JRadioButtonMenuItem rb = (JRadioButtonMenuItem) e.getSource();
                if (rb.isSelected()) {
                    if (rb.getText().equals("Nearest")) {
                        _imageDisplay.setInterpolation(Interpolation.getInstance(Interpolation.INTERP_NEAREST));
                    }
                    else if (rb.getText().equals("Bilinear")) {
                        _imageDisplay.setInterpolation(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
                    }
                    else if (rb.getText().equals("Bicubic")) {
                        _imageDisplay.setInterpolation(Interpolation.getInstance(Interpolation.INTERP_BICUBIC));
                    }
                    else if (rb.getText().equals("Bicubic2")) {
                        _imageDisplay.setInterpolation(Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2));
                    }
                    _imageDisplay.updateImage();
                }
            }
        };

        b1.addItemListener(itemListener);
        b2.addItemListener(itemListener);
        b3.addItemListener(itemListener);
        b4.addItemListener(itemListener);

        return menu;
    }

    /**
     * Create the View => "Rotate"  menu item
     */
    protected JMenu createViewRotateMenu() {
        JMenu menu = new JMenu("Rotate");

        JRadioButtonMenuItem b1 = new JRadioButtonMenuItem("No Rotation");
        JRadioButtonMenuItem b2 = new JRadioButtonMenuItem("  90 deg");
        JRadioButtonMenuItem b3 = new JRadioButtonMenuItem(" 180 deg");
        JRadioButtonMenuItem b4 = new JRadioButtonMenuItem(" -90 deg");
        //JRadioButtonMenuItem b5 = new JRadioButtonMenuItem("  45 deg (XXX not impl)");

        b1.setSelected(true);
        menu.add(b1);
        menu.add(b2);
        menu.add(b3);
        menu.add(b4);
        //menu.add(b5);

        ButtonGroup group = new ButtonGroup();
        group.add(b1);
        group.add(b2);
        group.add(b3);
        group.add(b4);
        //group.add(b5);

        ItemListener itemListener = new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                JRadioButtonMenuItem rb = (JRadioButtonMenuItem) e.getSource();
                double rad = Math.PI / 180.;
                ImageProcessor imageProcessor = _imageDisplay.getImageProcessor();
                if (rb.isSelected()) {
                    if (rb.getText().equals("No Rotation")) {
                        imageProcessor.setAngle(0.0);
                    }
                    else if (rb.getText().equals("  90 deg")) {
                        imageProcessor.setAngle(90.0 * rad);
                    }
                    else if (rb.getText().equals(" 180 deg")) {
                        imageProcessor.setAngle(180.0 * rad);
                    }
                    else if (rb.getText().equals(" -90 deg")) {
                        imageProcessor.setAngle(-90.0 * rad);
                    }
                    //else if (rb.getText().equals("  45 deg (XXX not impl)")) {
                    //    imageProcessor.setAngle(45.0*rad);
                    //}
                    imageProcessor.update();
                }
            }
        };

        b1.addItemListener(itemListener);
        b2.addItemListener(itemListener);
        b3.addItemListener(itemListener);
        b4.addItemListener(itemListener);
        //b5.addItemListener(itemListener);

        return menu;
    }


    /**
     * Create the View => "Flip X"  menu item
     */
    protected JCheckBoxMenuItem createViewFlipXMenuItem() {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem("Flip X");
        menuItem.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                JCheckBoxMenuItem rb = (JCheckBoxMenuItem) e.getSource();
                ImageProcessor imageProcessor = _imageDisplay.getImageProcessor();
                imageProcessor.setFlipX(rb.getState());
                imageProcessor.update();
            }
        });

        return menuItem;
    }

    /**
     * Create the View => "Flip Y"  menu item
     */
    protected JCheckBoxMenuItem createViewFlipYMenuItem() {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem("Flip Y");
        menuItem.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                JCheckBoxMenuItem rb = (JCheckBoxMenuItem) e.getSource();
                ImageProcessor imageProcessor = _imageDisplay.getImageProcessor();
                imageProcessor.setFlipY(rb.getState());
                imageProcessor.update();
            }
        });

        return menuItem;
    }


    /**
     * Create the View => "Smooth Scrolling"  menu item
     */
    protected JCheckBoxMenuItem createViewSmoothScrollingMenuItem() {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(_I18N.getString("smoothScrolling"));
        menuItem.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                JCheckBoxMenuItem rb = (JCheckBoxMenuItem) e.getSource();
                _imageDisplay.setImmediateMode(rb.getState());
                _imageDisplay.updateImage();
            }
        });

        return menuItem;
    }


    /**
     * Create or update the Go (history) menu.
     */
    protected JMenu createGoMenu(JMenu menu) {
        if (menu == null)
            menu = new JMenu(_I18N.getString("go"));

        menu.add(_imageDisplay.getBackAction());
        menu.add(_imageDisplay.getForwAction());
        menu.addSeparator();
        _imageDisplay.addHistoryMenuItems(menu);
        menu.addSeparator();
        menu.add(createGoClearHistoryMenuItem());

        return menu;
    }


    /**
     * Create the Go => "Clear History" menu item.
     */
    protected JMenuItem createGoClearHistoryMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("clearHistory"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _imageDisplay.clearHistory();
                _goMenu.removeAll();
                createGoMenu(_goMenu);
            }
        });
        return menuItem;
    }


    /** Return the target image window */
    public DivaMainImageDisplay getImageDisplay() {
        return _imageDisplay;
    }

    /** Return the handle for the File menu */
    public JMenu getFileMenu() {
        return _fileMenu;
    }

    /** Return the handle for the Edit menu */
    public JMenu getEditMenu() {
        return _editMenu;
    }

    /** Return the handle for the View menu */
    public JMenu getViewMenu() {
        return _viewMenu;
    }

    /** Return the handle for the Go menu */
    public JMenu getGoMenu() {
        return _goMenu;
    }

    /** Return the handle for the Graphics menu */
    public JMenu getGraphicsMenu() {
        return _graphicsMenu;
    }

    /** Return the File => Exit menu item */
    public JMenuItem getNewWindowMenuItem() {
        return _newWindowMenuItem;
    }

    /** Return the Pick Object menu item */
    public JMenuItem getPickObjectMenuItem() {
        return _pickObjectMenuItem;
    }
}





