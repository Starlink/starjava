/*
 * ESO Archive
 *
 * $Id: Navigator.java,v 1.29 2002/08/16 22:21:13 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/06/02  Created
 */

package jsky.navigator;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Stack;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import jsky.catalog.Catalog;
import jsky.catalog.CatalogDirectory;
import jsky.catalog.FieldDesc;
import jsky.catalog.TableQueryResult;
import jsky.catalog.astrocat.AstroCatConfig;
import jsky.catalog.gui.CatalogHistoryItem;
import jsky.catalog.gui.CatalogNavigator;
import jsky.catalog.gui.CatalogNavigatorOpener;
import jsky.catalog.gui.CatalogQueryTool;
import jsky.catalog.gui.CatalogTree;
import jsky.catalog.gui.TableDisplayTool;
import jsky.catalog.gui.TablePlotter;
import jsky.catalog.skycat.SkycatCatalog;
import jsky.catalog.skycat.SkycatConfigEntry;
import jsky.catalog.skycat.SkycatConfigFile;
import jsky.catalog.skycat.SkycatTable;
import jsky.image.gui.ImageDisplayControlFrame;
import jsky.image.gui.MainImageDisplay;
import jsky.image.gui.PickObjectStatistics;
import jsky.util.Logger;
import jsky.util.Preferences;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.ExampleFileFilter;
import jsky.util.gui.ProgressBarFilterInputStream;
import jsky.util.gui.ProgressException;
import jsky.util.gui.ProgressPanel;
import jsky.util.gui.SwingUtil;


/**
 * Extends CatalogNavigator to include support for displaying images
 * and plotting catalog data in images.
 */
public class Navigator extends CatalogNavigator implements CatalogNavigatorOpener {

    /** Top level image window (frame or internal frame version) */
    private Component _imageDisplayControlFrame;

    /** Used to display images */
    private NavigatorImageDisplay _imageDisplay;

    /** Action to use to show the image window. */
    private AbstractAction _imageDisplayAction = new AbstractAction("Image") {

        public void actionPerformed(ActionEvent evt) {
            showImageDisplay();
        }
    };

    // The root catalog directory to use
    private static CatalogDirectory _catDir;

    /** 
     * Return the top level catalog directory to use based on the value of the 
     * <em>jsky.catalog.directory</em> system property, which may be set to the name of
     * the class implementing the CatalogDirectory interface. The default is
     * to use the {@link jsky.catalog.astrocat.AstroCatConfig} class (The 
     * {@link jsky.catalog.skycat.SkycatConfigFile} class is another alternative).
     * The static method in the given class named <em>getDirectory()</em> is called to 
     * return a reference to the top level CatalogDirectory.
     */
    public static CatalogDirectory getCatalogDirectory() {
	if (_catDir == null) {
	    String className = System.getProperty("jsky.catalog.directory", "jsky.catalog.astrocat.AstroCatConfig");
	    try {
		Class c = Class.forName(className,true,
                            Thread.currentThread().getContextClassLoader());
		Object o = c.getMethod("getDirectory", null).invoke(null, null);
		if (o instanceof CatalogDirectory) {
		    _catDir = (CatalogDirectory)o;
		    _catDir.setName("My Catalogs");
		}
		else {
		    throw new RuntimeException("Error: call to " + className + ".getDirectory() returned: " + o);
		}
	    }
	    catch (InvocationTargetException e1) {
		Throwable t = e1.getTargetException();
		t.printStackTrace();
		throw new RuntimeException("Error calling " + className + ".getDirectory(): " + t);
	    }
	    catch (Exception e2) {
		e2.printStackTrace();
		throw new RuntimeException("Error calling " + className + ".getDirectory(): " + e2);
	    }
	}

	return _catDir;
    }


    /**
     * Construct a Navigator using the given CatalogTree widget
     * (call setQueryResult to set the catalog or data to display).
     *
     * @param parent the parent component
     *
     * @param catalogTree a CatalogTree (normally a subclass of CatalogTree
     *                    that knows about certain types of catalogs)
     *
     * @param plotter the object to use to plot catalog table data
     *                (when the plot button is pressed)
     *
     * @param imageDisplay optional widget to use to display images (if not specified,
     *                     or null, a new window will be created)
     *
     */
    public Navigator(Component parent, CatalogTree catalogTree,
                     TablePlotter plotter, MainImageDisplay imageDisplay) {
        super(parent, catalogTree, plotter);

        if (imageDisplay != null) {
            _imageDisplay = (NavigatorImageDisplay) imageDisplay;
            _imageDisplayControlFrame = imageDisplay.getRootComponent();
            initSymbolPlotter();
        }
    }

    /**
     * Construct a Navigator using the given CatalogTree widget
     * (call setQueryResult to set the catalog or data to display).
     *
     * @param parent the parent component
     *
     * @param catalogTree a CatalogTree (normally a subclass of CatalogTree
     *                    that knows about certain types of catalogs)
     *
     * @param plotter the object to use to plot catalog table data
     *                (when the plot button is pressed)
     *
     */
    public Navigator(Component parent, CatalogTree catalogTree, TablePlotter plotter) {
        this(parent, catalogTree, plotter, null);
    }

    /** Used to display images */
    public MainImageDisplay getImageDisplay() {
        return _imageDisplay;
    }

    /** Return the action to use to show the image window. */
    public Action getImageDisplayAction() {
        return _imageDisplayAction;
    }


    /**
     * Make a panel for querying a catalog
     * (Redefined from the parent class to use a CatalogQueryTool subclass).
     */
    protected CatalogQueryTool makeCatalogQueryTool(Catalog catalog) {
        return new NavigatorQueryTool(catalog, this, _imageDisplay);
    }

    /**
     * Make an ImageDisplayControlFrame or ...InternalFrame, depending
     * on what type of frames are being used.
     */
    protected void makeImageDisplayControlFrame() {
        Component parent = getParentFrame();
        if (parent instanceof JFrame) {
            _imageDisplayControlFrame = new NavigatorImageDisplayFrame();
            _imageDisplayControlFrame.setVisible(true);
            _imageDisplay = (NavigatorImageDisplay)
                    ((NavigatorImageDisplayFrame) _imageDisplayControlFrame).getImageDisplayControl().getImageDisplay();
        }
        else if (parent instanceof JInternalFrame) {
            JDesktopPane desktop = getDesktop();
            _imageDisplayControlFrame = new NavigatorImageDisplayInternalFrame(desktop);
            _imageDisplayControlFrame.setVisible(true);
            _imageDisplay = (NavigatorImageDisplay)
                    ((NavigatorImageDisplayInternalFrame) _imageDisplayControlFrame).getImageDisplayControl().getImageDisplay();
            desktop.add(_imageDisplayControlFrame, JLayeredPane.DEFAULT_LAYER);
            desktop.moveToFront(_imageDisplayControlFrame);
        }
        _imageDisplay.setNavigator(this);
    }

    /**
     * Load and display the given image file.
     * The URL is only needed for the image history, in case the f/ile is deleted.
     */
    protected void loadImage(String filename, URL url) {
        showImageDisplay();
        _imageDisplay.setFilename(filename, url);
    }


    /**
     * Show the image display window.
     */
    public void showImageDisplay() {
        if (_imageDisplay == null) {
            // create the image display frame
            makeImageDisplayControlFrame();
            notifyNewImageDisplay();
            initSymbolPlotter();
        }
        else {
            SwingUtil.showFrame(_imageDisplayControlFrame);
        }
    }

    /**
     * Download the given image URL to a temporary file and then
     * display the image file when done.
     * This method is called in a background thread.
     */
    protected void loadImage(URL url, String contentType) throws IOException {
        if (url.getProtocol().equals("file")) {
            SwingUtilities.invokeLater(new NavigatorImageLoader(url.getPath(), url));
        }
        else {
            String dir = Preferences.getPreferences().getCacheDir().getPath();

            // Try to determine the correct suffix for the file, for later reference
            String suffix;
            if (contentType.endsWith("hfits"))
                suffix = ".hfits"; // H-compressed FITS
            else if (contentType.endsWith("zfits") 
		     || contentType.equals("image/x-fits")) // XX hack: caltech/oasis returns this with gzipped FITS!
                suffix = ".fits.gz"; // gzipped FITS (other contentTypes?)
            else if (contentType.endsWith("fits"))
                suffix = ".fits"; // plain FITS
            else
                suffix = ".tmp";  // other image formats...

            File file = File.createTempFile("jsky", suffix, new File(dir));
	    
	    //System.out.println("XXX loadImage: file = " + file);

            ProgressPanel progressPanel = getProgressPanel();
            ProgressBarFilterInputStream in = progressPanel.getLoggedInputStream(url);
            FileOutputStream out = new FileOutputStream(file);

            // copy the data
            synchronized (in) {
                synchronized (out) {
                    byte[] buffer = new byte[8 * 1024];
                    while (true) {
                        int bytesRead = in.read(buffer);
                        if (bytesRead == -1) {
                            break;
                        }
                        if (progressPanel.isInterrupted()) {
                            throw new ProgressException("Interrupted");
                        }
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }

            in.close();
            out.flush();
            out.close();

            if (!progressPanel.isInterrupted())
                SwingUtilities.invokeLater(new NavigatorImageLoader(file.toString(), url));
        }
    }


    /**
     *  Notify any panels that need to know about the new image display window.
     */
    protected void notifyNewImageDisplay() {
        notifyNewImageDisplay(getBackStack());
        notifyNewImageDisplay(getForwStack());

        JComponent queryComponent = getQueryComponent();
        if (queryComponent instanceof NavigatorQueryTool) {
            ((NavigatorQueryTool) queryComponent).setImageDisplay(_imageDisplay);
        }
    }

    /**
     * Notify any panels in the given stack that need to know about
     * the new image display window.
     */
    protected void notifyNewImageDisplay(Stack stack) {
        int n = stack.size();
        for (int i = 0; i < n; i++) {
            JComponent comp = (JComponent) (stack.get(i));
            if (comp instanceof NavigatorQueryTool) {
                ((NavigatorQueryTool) comp).setImageDisplay(_imageDisplay);
            }
        }
    }


    /**
     * initialize the symbol plotter
     */
    protected void initSymbolPlotter() {
        // initialize the symbol plotter
        TablePlotter plotter = getPlotter();
        if (plotter != null) {
            plotter.setCanvasGraphics(_imageDisplay.getCanvasGraphics());
            plotter.setCoordinateConverter(_imageDisplay.getCoordinateConverter());
            _imageDisplay.getNavigatorPane().setPlotter(plotter);
        }
    }


    /**
     * Return a new JComponent displaying the contents of the given URL.
     */
    protected JComponent makeURLComponent(URL url, String contentType) throws IOException {
        if (Logger.isDebugEnabled(this))
            Logger.debug(this, "Display URL: ContentType=" + contentType + ", URL=" + url);

        if (contentType.equals("text/html"))
            return super.makeURLComponent(url, contentType);

        String filename = url.getFile();
	String protocol = url.getProtocol();

        // Check for a Skycat style catalog config file?
        if (protocol.equals("file")) {
            if (filename.endsWith(".cfg")) {
                // skycat config file?
                String basename = new File(filename).getName();
                SkycatConfigFile cf = new SkycatConfigFile(basename, url);
                cf.setHTMLQueryResultHandler(this);
                return makeCatalogDirectoryComponent(cf);
            }
            else if (filename.endsWith(".table") || filename.endsWith(".scat") || filename.endsWith(".cat")) {
                // skycat local catalog file?
                SkycatTable table = new SkycatTable(filename);
                return makeCatalogComponent(table.getCatalog());
            }
        }
        if (protocol.equals("file") || protocol.equals("xml") && filename.endsWith(".xml")) {
	    // AstroCat XML file?
	    String basename = new File(filename).getName();
	    AstroCatConfig cf = new AstroCatConfig(basename, url);
	    cf.setHTMLQueryResultHandler(this);
	    return makeCatalogDirectoryComponent(cf);
	}

        if (contentType.equals("text/plain"))
            return super.makeURLComponent(url, contentType);

        // assume it is an image and display in a separate window
        loadImage(url, contentType);
        return getResultComponent();
    }

    /**
     * Open the catalog navigator window (in this case, it is already open).
     * @see CatalogNavigatorOpener
     */
    public void openCatalogWindow() {
    }


    /**
     * Open the catalog navigator window and display the interface for the given catalog,
     * if not null (in this case, the window is already open).
     * @see CatalogNavigatorOpener
     */
    public void openCatalogWindow(Catalog cat) {
        if (cat != null) {
            setQueryResult(cat);
        }
    }

    /** 
     * Open a catalog window for the named catalog, if found. 
     * @see CatalogNavigatorOpener
     */
    public void openCatalogWindow(String name) {
	Catalog cat = getCatalogDirectory().getCatalog(name);
	if (cat != null)
	    openCatalogWindow(cat);
    }


    /** 
     * Pop up a file browser to select a local catalog file to open. 
     * @see CatalogNavigatorOpener
     */
    public void openLocalCatalog() {
        open();
    }

    /**
     * Save the current table as a FITS table in the current FITS image.
     */
    public void saveWithImage() {
        JComponent resultComponent = getResultComponent();
        if (!(resultComponent instanceof TableDisplayTool)) {
            DialogUtil.error("This operation is only supported for tables");
        }
        else if (_imageDisplay == null) {
            DialogUtil.error("No current FITS image.");
        }
        else {
            TableQueryResult table = ((TableDisplayTool) resultComponent).getTable();
            _imageDisplay.saveFITSTable(table);
        }
    }


    /**
     * This method is called after the history list is deserialized to remove any
     * items in the list that can't be accessed.
     */
    protected void cleanupHistoryList() {
        ListIterator it = getHistoryList().listIterator(0);
        while (it.hasNext()) {
            CatalogHistoryItem item = (CatalogHistoryItem) it.next();
            if (item.getURLStr() == null && _catDir.getCatalog(item.getName()) == null)
                it.remove();
        }
    }


    /**
     * Create and return a new file chooser to be used to select a local catalog file
     * to open.
     */
    public JFileChooser makeFileChooser() {
        JFileChooser fileChooser = new JFileChooser(new File("."));

        ExampleFileFilter configFileFilter = new ExampleFileFilter(new String[]{"cfg"},
                "Catalog config Files (Skycat style)");
        fileChooser.addChoosableFileFilter(configFileFilter);

        ExampleFileFilter fitsFilter = new ExampleFileFilter(new String[]{
            "fit", "fits", "fts"}, "FITS File with Table Extensions");
        fileChooser.addChoosableFileFilter(fitsFilter);

        ExampleFileFilter skycatLocalCatalogFilter = new ExampleFileFilter(new String[]{
            "table", "scat", "cat"}, "Local catalog Files (Skycat style)");
        fileChooser.addChoosableFileFilter(skycatLocalCatalogFilter);

        ExampleFileFilter htmlFilter = new ExampleFileFilter(new String[]{
            "html", "htm"}, "HTML File");
        fileChooser.addChoosableFileFilter(htmlFilter);

        fileChooser.setFileFilter(skycatLocalCatalogFilter);

        return fileChooser;
    }


    /**
     * This local class is used to load an image in the event dispatching thread.
     * Doing it in the calling thread could cause the window to hang, since
     * it needs to create and show a top level window and the calling thread
     * (from CatalogNavigator) is already a background thread.
     * The url is only needed for the image history, in case the file is deleted.
     */
    protected class NavigatorImageLoader implements Runnable {

        String filename;
        URL url;

        public NavigatorImageLoader(String filename, URL url) {
            this.filename = filename;
            this.url = url;
        }

        public void run() {
            loadImage(filename, url);
        }
    }

    /**
     * Add the object described by stats to the currently
     * displayed table, or create a new table if none is being displayed.
     *
     * @param stats describes the selected object
     * @param isUpdate set to true if this is just an update of the previously selected position
     */
    public void addPickedObjectToTable(PickObjectStatistics stats, boolean isUpdate) {
        TableQueryResult table = null;
        TableDisplayTool tableDisplayTool = null;

        // see if a table is already being displayed, and if so, use it

        JComponent resultComponent = getResultComponent();
        if (resultComponent instanceof TableDisplayTool) {
            tableDisplayTool = (TableDisplayTool) resultComponent;
            table = tableDisplayTool.getTableDisplay().getTableQueryResult();
        }

        if (table == null) {
            // no table being displayed: make a new table
            table = makePickObjectTable(stats);

            // Make a dummy catalog for the table, so we have something to display in the top window
            SkycatCatalog cat = new SkycatCatalog((SkycatTable) table);
            setQueryResult(cat);

            JComponent queryComponent = getQueryComponent();
            if (queryComponent instanceof CatalogQueryTool) {
                // This is like pressing the Go button to show the contents of the table
                ((CatalogQueryTool) queryComponent).search();
            }
            getParentFrame().setVisible(true);
        }
        else {
            // just add the row to the existing table
            addRowForPickedObject(table, tableDisplayTool, stats, isUpdate);
        }
    }


    /**
     * Make a catalog table to use to hold the objects picked by the user and
     * add the first row based on the given stats object.
     */
    protected TableQueryResult makePickObjectTable(PickObjectStatistics stats) {
        Properties properties = new Properties();
        properties.setProperty(SkycatConfigFile.SERV_TYPE, "local");
        properties.setProperty(SkycatConfigFile.LONG_NAME, "Picked Objects");
        properties.setProperty(SkycatConfigFile.SHORT_NAME, "PickedObjects");
        properties.setProperty(SkycatConfigFile.SYMBOL,
                "{{FWHM_X} {FWHM_Y} {Angle}} "
                + "{{plus} {green} {$FWHM_X/$FWHM_Y} {$Angle} {} {1}} "
                + "{{($FWHM_X+$FWHM_Y)*0.5} {image}}");
        properties.setProperty(SkycatConfigFile.URL, "none");
        SkycatConfigEntry configEntry = new SkycatConfigEntry(properties);
        FieldDesc[] fields = PickObjectStatistics.getFields();

        Vector dataRows = new Vector();
        dataRows.add(stats.getRow());
        SkycatTable table = new SkycatTable(configEntry, dataRows, fields);
        table.setProperties(properties);
        return table;
    }


    /**
     * Add a row to the given table with information from the given stats object.
     * The target table may or may not be compatible, so column names and types
     * are checked.
     */
    protected void addRowForPickedObject(TableQueryResult table, TableDisplayTool tableDisplayTool,
                                         PickObjectStatistics stats, boolean isUpdate) {
        if (!table.hasCoordinates()) {
            DialogUtil.error("The current table does not support coordinates");
            return;
        }

        int numCols = table.getColumnCount();
        Vector v = stats.getRow();
        Vector rowVec = new Vector(numCols);

        for (int col = 0; col < numCols; col++) {
            FieldDesc field = table.getColumnDesc(col);
            String name = field.getName();
            if (field.isId()) {
                rowVec.add(v.get(stats.ID));
            }
            else if (field.isRA()) {
                rowVec.add(stats.getCenterPos().getRA().toString());
            }
            else if (field.isDec()) {
                rowVec.add(stats.getCenterPos().getDec().toString());
            }
            else {
                Object o = null;
                for (int i = 0; i < stats.NUM_FIELDS; i++) {
                    if (name.equals(stats.FIELD_NAMES[i])) {
                        o = v.get(i);
                        break;
                    }
                }
                rowVec.add(o);
            }
        }
        if (isUpdate) {
            // if the last row added is for the same point, update it
            int rowIndex = tableDisplayTool.getRowCount() - 1;
	    if (rowIndex >= 0)
		tableDisplayTool.updateRow(rowIndex, rowVec);
	    else 
		tableDisplayTool.addRow(rowVec);
        }
        else {
            tableDisplayTool.addRow(rowVec);
        }
        tableDisplayTool.replot();
    }
}

