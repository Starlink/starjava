/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: NavigatorImageDisplay.java,v 1.34 2002/08/16 22:21:13 brighton Exp $
 */

package jsky.navigator;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.util.HashSet;

import javax.swing.AbstractAction;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;

import jsky.catalog.Catalog;
import jsky.catalog.CatalogDirectory;
import jsky.catalog.TableQueryResult;
import jsky.catalog.gui.CatalogNavigatorOpener;
import jsky.catalog.gui.TablePlotter;
import jsky.image.ImageProcessor;
import jsky.image.fits.codec.FITSImage;
import jsky.image.fits.gui.FITSKeywordsFrame;
import jsky.image.fits.gui.FITSKeywordsInternalFrame;
import jsky.image.gui.DivaMainImageDisplay;
import jsky.image.gui.PickObjectStatistics;
import jsky.util.I18N;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.SwingUtil;

/**
 * Extends the DivaMainImageDisplay class by adding support for
 * browsing catalogs and plotting catalog symbols on the image.
 *
 * @version $Revision: 1.34 $
 * @author Allan Brighton
 * @author Peter W. Draper
 */
public class NavigatorImageDisplay extends DivaMainImageDisplay
        implements CatalogNavigatorOpener {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(NavigatorImageDisplay.class);

    /** The instance of the catalog navigator to use with this image display. */
    private Navigator _navigator;

    /** The Diva pane containing the added catalog symbol layer. */
    private NavigatorPane _navigatorPane;

    /** The catalog navigator frame (or internal frame) */
    private Component _navigatorFrame;

    /** Set of filenames: Used to keep track of the files visited in this session. */
    private HashSet _filesVisited = new HashSet();

    /** Action to use to show the catalog window (Browse catalogs) */
    private AbstractAction _catalogBrowseAction = new AbstractAction(_I18N.getString("browse") + "...") {

        public void actionPerformed(ActionEvent evt) {
            try {
                openCatalogWindow();
            }
            catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };


    /**
     * Construct a NavigatorImageDisplay widget.
     *
     * @param parent the top level parent frame (or internal frame)
     *               used to close the window 
     * @param processor an ImageProcessor to use
     */
    public NavigatorImageDisplay(Component parent, ImageProcessor processor) {
        super(new NavigatorPane(), parent, processor);
        _navigatorPane = (NavigatorPane) getCanvasPane();
    }
    //PWD: need so that sub-classes can provide own ImageProcessor

    /**
     * Construct a NavigatorImageDisplay widget.
     *
     * @param parent the top level parent frame (or internal frame) used to close the window
     */
    public NavigatorImageDisplay(Component parent) {
        super(new NavigatorPane(), parent);
        _navigatorPane = (NavigatorPane) getCanvasPane();
    }

    /** Return the Diva pane containing the added catalog symbol layer. */
    public NavigatorPane getNavigatorPane() {
        return _navigatorPane;
    }

    /**
     * Open up another window like this one and return a reference to it.
     * <p>
     * Note: derived classes should redefine this to return an instance of the
     * correct class, which should be derived JFrame or JInternalFrame.
     */
    public Component newWindow() {
        JDesktopPane desktop = getDesktop();
        if (desktop != null) {
            NavigatorImageDisplayInternalFrame f = new NavigatorImageDisplayInternalFrame(desktop);
            f.getImageDisplayControl().getImageDisplay().setTitle(getTitle());
            f.setVisible(true);
            desktop.add(f, JLayeredPane.DEFAULT_LAYER);
            desktop.moveToFront(f);
            f.setVisible(true);
            return f;
        }
        else {
            NavigatorImageDisplayFrame f = new NavigatorImageDisplayFrame();
            f.getImageDisplayControl().getImageDisplay().setTitle(getTitle());
            f.setVisible(true);
            return f;
        }
    }

    /**
     * Set the instance of the catalog navigator to use with this image display.
     */
    public void setNavigator(Navigator navigator) {
        _navigator = navigator;
        _navigatorFrame = navigator.getRootComponent();
    }

    /**
     * Return the instance of the catalog navigator used with this image display.
     */
    public Navigator getNavigator() {
        return _navigator;
    }

    /**
     * Open the catalog navigator window.
     */
    public void openCatalogWindow() {
        if (_navigatorFrame == null)
            makeNavigatorFrame();
        showNavigatorFrame((Catalog) null);
    }

    /**
     * Display the interface for the given catalog, if not null, otherwise just
     * open the catalog navigator window.
     */
    public void openCatalogWindow(Catalog cat) {
        if (_navigatorFrame == null)
            makeNavigatorFrame();
        showNavigatorFrame(cat);
    }

    /** Open a catalog window for the named catalog, if found. */
    public void openCatalogWindow(String name) {
        CatalogDirectory dir = null;
        try {
	    dir = Navigator.getCatalogDirectory();
        }
        catch (Exception e) {
            DialogUtil.error(e);
            return;
        }
	
	Catalog cat = dir.getCatalog(name);
	if (cat != null)
	    openCatalogWindow(cat);
    }

    /** Pop up a file browser to select a local catalog file to open. */
    public void openLocalCatalog() {
        openCatalogWindow();
        _navigator.open();
    }


    /** Display the FITS table at the given HDU index. */
    public void displayFITSTable(int hdu) {
        try {
            FITSImage fitsImage = getFitsImage();
            NavigatorFITSTable table = new NavigatorFITSTable(getFilename(), fitsImage.getFits(), hdu);
            openCatalogWindow(table.getCatalog());

            // update the FITS header display, if needed
            Component fitsKeywordsFrame = getFitsKeywordsFrame();
            if (fitsKeywordsFrame != null) {
                if (fitsKeywordsFrame instanceof FITSKeywordsFrame) {
                    ((FITSKeywordsFrame) fitsKeywordsFrame).getFITSKeywords().updateDisplay(hdu);
                }
                else if (fitsKeywordsFrame instanceof FITSKeywordsInternalFrame) {
                    ((FITSKeywordsInternalFrame) fitsKeywordsFrame).getFITSKeywords().updateDisplay(hdu);
                }
            }
        }
        catch (Exception e) {
            DialogUtil.error(this, e);
        }
    }

    /**
     * Save (or update) the given table as a FITS table in the current FITS image.
     */
    public void saveFITSTable(TableQueryResult table) {
        FITSImage fitsImage = getFitsImage();
        if (fitsImage == null) {
            DialogUtil.error(this, "This operation is only supported on FITS files.");
            return;
        }

        try {
            NavigatorFITSTable newTable = NavigatorFITSTable.saveWithImage(getFilename(), fitsImage.getFits(), table);
            if (newTable == null)
                return;

            setSaveNeeded(true);
            checkExtensions(true);

            // unplot the original table, since the FITS table will be plotted from now on
            TablePlotter plotter = _navigator.getPlotter();
            if (plotter != null) {
                plotter.unplot(table);
                //plotter.plot(newTable);
                _navigator.setQueryResult(newTable.getCatalog());
            }
        }
        catch (Exception e) {
            DialogUtil.error(this, e);
        }
    }


    /**
     * If the given catalog argument is null, display the catalog window ("Browse" mode),
     * otherwise query the catalog using the default arguments for the current image.
     */
    protected void showNavigatorFrame(Catalog cat) {
        if (cat != null) {
            try {
                _navigator.setAutoQuery(true);
                _navigator.setQueryResult(cat);
            }
            finally {
                //_navigator.setAutoQuery(false);
            }
        }
        else {
	    _navigator.setAutoQuery(false);
            SwingUtil.showFrame(_navigatorFrame);
        }
    }


    /**
     * Make a NavigatorFrame or NavigatorInternalFrame, depending
     * on what type of frames are being used.
     */
    protected void makeNavigatorFrame() {
        CatalogDirectory dir = null;
        try {
	    dir = Navigator.getCatalogDirectory();
        }
        catch (Exception e) {
            DialogUtil.error(e);
            return;
        }

        JDesktopPane desktop = getDesktop();
        Component parent = getParentFrame();

        if (parent instanceof JFrame) {
            _navigatorFrame = new NavigatorFrame(dir, this);
            _navigator = ((NavigatorFrame) _navigatorFrame).getNavigator();
        }
        else if (parent instanceof JInternalFrame) {
            _navigatorFrame = new NavigatorInternalFrame(desktop, dir, this);
            _navigator = ((NavigatorInternalFrame) _navigatorFrame).getNavigator();
            desktop.add(_navigatorFrame, JLayeredPane.DEFAULT_LAYER);
            desktop.moveToFront(_navigatorFrame);
        }
    }


    /**
     * This method is called before and after a new image is loaded, each time
     * with a different argument.
     *
     * @param before set to true before the image is loaded and false afterwards
     */
    protected void newImage(boolean before) {
        super.newImage(before);

        if (!before) {
            if (_navigatorFrame == null)
                makeNavigatorFrame();

            // Replot any previously plotted catalogs (from any image)
            // in coordinate system of the new image
            TablePlotter plotter = _navigator.getPlotter();
            if (plotter != null) {
                plotter.replotAll();
            }

            // If this is the first time this image is being visited this session,
            // plot any catalog tables stored as FITS tables
            String filename = getFilename();
            FITSImage fitsImage = getFitsImage();
            if (fitsImage != null && filename != null) {
                if (!_filesVisited.contains(filename)) {
                    _filesVisited.add(filename);
                    try {
                        NavigatorFITSTable.plotTables(filename, fitsImage.getFits(), _navigator);
                    }
                    catch (Exception e) {
                        DialogUtil.error(this, e);
                    }
                }
            }
        }
    }


    /** Cleanup when the window is no longer needed. */
    public void dispose() {
        super.dispose();

        if (_navigatorFrame != null) {
            if (_navigatorFrame instanceof JFrame)
                ((JFrame) _navigatorFrame).dispose();
            else
                ((JInternalFrame) _navigatorFrame).dispose();
        }
    }

    /**
     * Transform the image graphics using the given AffineTransform.
     */
    protected void transformGraphics(AffineTransform trans) {
        super.transformGraphics(trans);
        if (_navigator != null) {
            TablePlotter plotter = _navigator.getPlotter();
            if (plotter != null) {
                plotter.transformGraphics(trans);
            }
        }
    }


    /** Save any current catalog overlays as a FITS table in the image file. */
    public void saveCatalogOverlaysWithImage() {
        if (_navigator != null) {
            TablePlotter plotter = _navigator.getPlotter();
            if (plotter != null) {
                TableQueryResult[] tables = plotter.getTables();
                if (tables != null) {
                    for (int i = 0; i < tables.length; i++)
                        saveFITSTable(tables[i]);
                }
            }
        }
    }

    /**
     * Called when an object is selected in the Pick Object window.
     * <p>
     * Add the currently selected object in the "Pick Object" window to the currently
     * displayed table, or create a new table if none is being displayed.
     */
    protected void pickedObject() {
        if (_navigatorFrame == null)
            makeNavigatorFrame();
        if (_navigator == null)
            return;
        PickObjectStatistics stats = getPickObjectPanel().getStatistics();
        if (stats == null) {
            DialogUtil.error("No object was selected");
            return;
        }
        _navigator.addPickedObjectToTable(stats, getPickObjectPanel().isUpdate());
    }


    // Other menu and toolbar actions
    public AbstractAction getCatalogBrowseAction() {
        return _catalogBrowseAction;
    }
}
