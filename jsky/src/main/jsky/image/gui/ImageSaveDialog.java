/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * (Modified from NASA/SEA classes.)
 *
 * $Id: ImageSaveDialog.java,v 1.7 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.image.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.OutOfMemoryError;
import java.lang.reflect.Method;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import jsky.image.GifEncoder;
import jsky.image.GreyscaleFilter;
import jsky.util.I18N;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.GridBagUtil;


/**
 * A Dialog box for saving wither the original image file, or the
 * image view, with graphics.
 */
public class ImageSaveDialog extends JFileChooser {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(ImageSaveDialog.class);

    /** The target image display */
    protected MainImageDisplay imageDisplay;

    /** "Save entire image" button. */
    JRadioButton useAll;

    /** "Save current view" button. */
    JRadioButton useView;

    /** Choice of file formats. */
    JComboBox formatBox;

    /** The JPEG file type. **/
    protected static final String JPEG_TYPE = "JPEG";

    /** The GIF file type. **/
    protected static final String GIF_TYPE = "GIF";

    /** The FITS file type. **/
    protected static final String FITS_TYPE = "FITS";

    /** The TIFF file type. **/
    protected static final String TIFF_TYPE = "TIFF";

    /** The PNG file type. **/
    protected static final String PNG_TYPE = "PNG";

    /** The PNM file type. **/
    protected static final String PNM_TYPE = "PNM";

    /** The BMP file type. **/
    protected static final String BMP_TYPE = "BMP";

    /** The set of possible file types for saving the current view.**/
    protected static final String[] VIEW_FILE_TYPES = {
        JPEG_TYPE, GIF_TYPE
    };

    /** The set of possible file types for saving the image to a file.**/
    protected static final String[] SAVE_FILE_TYPES = {
        FITS_TYPE, JPEG_TYPE, TIFF_TYPE, PNG_TYPE, PNM_TYPE, BMP_TYPE
    };

    /** "Save Image File" option to save only the image area currently in the viewer. **/
    protected static final int SAVE_CURRENT_VIEW = 1;

    /** "Save Image File" option to save the entire image area at normal magnification. **/
    protected static final int SAVE_ENTIRE_IMAGE = 2;


    /** Initialize with the target image display object. */
    public ImageSaveDialog(MainImageDisplay imageDisplay) {
        super(new File("."));
        this.imageDisplay = imageDisplay;
        createAccessoryPanel();
    }


    /** Create the accessory panel. */
    protected void createAccessoryPanel() {
        JPanel accessoryPanel = new JPanel();
        accessoryPanel.setLayout(new GridBagLayout());
        GridBagUtil layout = new GridBagUtil(accessoryPanel, (GridBagLayout) accessoryPanel.getLayout());
        accessoryPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        formatBox = new JComboBox();
        formatBox.setSelectedItem(JPEG_TYPE);
        formatBox.setToolTipText(_I18N.getString("selectFileFormat"));
        formatBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateFileSuffix();
            }
        });
        int r = 0;
        int none = GridBagConstraints.NONE;
        int west = GridBagConstraints.WEST;
        layout.add(new JLabel(_I18N.getString("saveAsType") + ":"), 0, r++, 1, 1, 0.0, 0.0, none, west);
        layout.add(formatBox, 0, r++, 1, 1, 0.0, 0.0, none, west);
        layout.add(new JLabel(" "), 0, r++, 1, 1, 0.0, 0.0, none, west); // space
        layout.add(new JLabel(_I18N.getString("options") + ":"), 0, r++, 1, 1, 0.0, 0.0, none, west);

        useAll = new JRadioButton(_I18N.getString("saveImage"));
        useAll.setToolTipText(_I18N.getString("saveImageAreaNoGraphics"));
        useAll.setSelected(true);
        useAll.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateFormatBox(false);
            }
        });

        useView = new JRadioButton(_I18N.getString("saveCurrentView"));
        useView.setToolTipText(_I18N.getString("saveImageAreaWithGraphics"));
        useView.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateFormatBox(true);
            }
        });

        ButtonGroup bg = new ButtonGroup();
        bg.add(useAll);
        bg.add(useView);
        layout.add(useAll, 0, r++, 1, 1, 0.0, 0.0, none, west);
        layout.add(useView, 0, r++, 1, 1, 0.0, 0.0, none, west);

        setAccessory(accessoryPanel);
    }

    /** Update the suffix of the displayed filename based on the selected file format. */
    protected void updateFileSuffix() {
        File file = getSelectedFile();
        if (file == null)
            return;
        String filename = file.getName();
        String s = filename.toLowerCase();
        if (formatBox.getSelectedItem() == JPEG_TYPE) {
            if (!s.endsWith(".jpg") && !s.endsWith(".jpeg")) {
                setSelectedFile(new File(addSuffix(filename, getTypeSuffix(JPEG_TYPE))));
            }
        }
        else if (formatBox.getSelectedItem() == GIF_TYPE) {
            if (!s.endsWith(".gif")) {
                setSelectedFile(new File(addSuffix(filename, getTypeSuffix(GIF_TYPE))));
            }
        }
        else if (formatBox.getSelectedItem() == FITS_TYPE) {
            if (!s.endsWith(".fits") && !s.endsWith(".fts")) {
                setSelectedFile(new File(addSuffix(filename, getTypeSuffix(FITS_TYPE))));
            }
        }
        else if (formatBox.getSelectedItem() == PNG_TYPE) {
            if (!s.endsWith(".png")) {
                setSelectedFile(new File(addSuffix(filename, getTypeSuffix(PNG_TYPE))));
            }
        }
        else if (formatBox.getSelectedItem() == PNM_TYPE) {
            if (!s.endsWith(".pnm")) {
                setSelectedFile(new File(addSuffix(filename, getTypeSuffix(PNM_TYPE))));
            }
        }
        else if (formatBox.getSelectedItem() == TIFF_TYPE) {
            if (!s.endsWith(".tiff") && !s.endsWith(".tif")) {
                setSelectedFile(new File(addSuffix(filename, getTypeSuffix(TIFF_TYPE))));
            }
        }
        else if (formatBox.getSelectedItem() == BMP_TYPE) {
            if (!s.endsWith(".bmp")) {
                setSelectedFile(new File(addSuffix(filename, getTypeSuffix(BMP_TYPE))));
            }
        }
    }


    /**
     * Update the list of supported output image formats based on the option chosen
     *
     * @param useView if true, save the current view, with graphics, otherwise just the
     *                image without graphics.
     */
    protected void updateFormatBox(boolean useView) {
        if (useView) {
            formatBox.setModel(new DefaultComboBoxModel(VIEW_FILE_TYPES));
        }
        else {
            formatBox.setModel(new DefaultComboBoxModel(SAVE_FILE_TYPES));
        }
    }


    /** Display the dialog */
    public void save() {
        updateFormatBox(useView.isSelected());

        // Determine default file name
        String initialFilename = imageDisplay.getFilename();
        if (initialFilename == null)
            initialFilename = imageDisplay.getObjectName();
        if (initialFilename == null)
            initialFilename = "unknown";

        // Get the location from the user
        String location = promptUserForSaveLocation(initialFilename);

        if (location == null) {
            return;
        }

        // Get selected file type
        String type = (String) formatBox.getSelectedItem();

        // Automatically append suffix if none entered
        if (!location.toLowerCase().endsWith(getTypeSuffix(type))) {
            location += getTypeSuffix(type);
        }

        // And finally perform the save
        try {
            if (useView.isSelected()) {
                if (type == JPEG_TYPE) {
                    saveJpegImage(location);
                }
                else if (type == GIF_TYPE) {
                    saveGifImage(location);
                }
            }
            else {
                imageDisplay.saveAs(location);
            }
        }
        catch (Exception ex) {
            DialogUtil.error(_I18N.getString("unableToSaveImage") + ": " + ex.toString());
            ex.printStackTrace();
        }
    }


    /**
     * Replace the current suffix, if any, with the given one, and return the
     * result.
     */
    protected String addSuffix(String filename, String suffix) {
        int i = filename.lastIndexOf('.');
        if (i > 0)
            return filename.substring(0, i) + suffix;
        return filename + suffix;
    }


    /**
     * Saves the current canvas image as a local GIF image file.  The saved
     * image contains the current canvas image, including all overlayed objects
     * and any filters applied to the image.
     *
     * @param	filename	name of new image file on local disk
     **/
    protected void saveGifImage(String filename)
            throws IOException, OutOfMemoryError {

        // Create an output stream for the filename
        File fd = new File(filename);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(fd));

        // Create the actual Image for output
        Image paintedImage = createOutputImage();

        // Pass old image through grayscale filter, creating new image
        // This is necessary since GIFs are limited to 255 colors.
        // The easiest way to guarantee this is to convert to greyscale.
        Image oldImage = paintedImage;
        ImageFilter filter = new GreyscaleFilter();
        Image tempImage = createImage(new FilteredImageSource(oldImage.getSource(), filter));

        // Use MediaTracker to wait for new image to be created
        try {
            MediaTracker tracker = new MediaTracker(this);
            tracker.addImage(tempImage, 0);
            tracker.waitForID(0);
        }
        catch (InterruptedException e) {
            DialogUtil.error(e);
        }
        Image newImage = tempImage;

        // Encode the painted image as a GIF, sending to output stream
        GifEncoder encoder = new GifEncoder(newImage, out);
        encoder.encode();

        // Close the output stream
        out.close();
    }

    /**
     * Saves the current canvas image as a local JPEG image file.  The saved
     * image contains the current canvas image, including all overlayed objects
     * and any filters applied to the image.
     *
     * @param	filename	name of new image file on local disk
     **/
    public void saveJpegImage(String filename) throws IOException, OutOfMemoryError {

        // Create an output stream for the filename
        File fd = new File(filename);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(fd));

        // Create the actual Image for output
        Image paintedImage = createOutputImage();

        // Encode the image as a JPEG, sending to output stream
        try {
            // Do everything dynamically because the com.sun.image.codec.jpeg classes
            // are not part of the core standard classes.  They are included in the
            // Sun Java 2 releases, but may not be in other VMs.
            // This way, if they don't exist, the rest of the application still functions.

            // Get the Codec
            Class c = this.getClass().forName("com.sun.image.codec.jpeg.JPEGCodec");

            // Get the Encoder
            Method m = c.getMethod("createJPEGEncoder", new Class[]{OutputStream.class});
            Object encoder = m.invoke(null, new Object[]{out});

            // Get the EncoderParams
            Method m2 = c.getMethod("getDefaultJPEGEncodeParam", new Class[]{BufferedImage.class});
            Object encodeParam = m2.invoke(null, new Object[]{paintedImage});

            // Note: tried this - didn't help like I thought it would, so disabled.
            // Change to use the highest quality
            // Method quality = encodeParam.getClass().getMethod("setQuality", new Class[] { Float.TYPE, Boolean.TYPE });
            // quality.invoke(encodeParam, new Object[] { new Float(1.0), new Boolean(true) });

            // Do the encoding
            Method em = encoder.getClass().getMethod("encode",
                    new Class[]{
                        BufferedImage.class,
                        this.getClass().forName("com.sun.image.codec.jpeg.JPEGEncodeParam")
                    });
            em.invoke(encoder, new Object[]{paintedImage, encodeParam});
        }
        catch (Exception ex) {
            DialogUtil.error(_I18N.getString("jpegEncodeError") + ": " + ex.toString());
            out.close();
            return;
        }

        // Close the output stream
        out.close();
    }

    /**
     * Returns the standard filename suffix for the specified file type.
     **/
    protected String getTypeSuffix(String type) {
        if (type == GIF_TYPE) {
            return ".gif";
        }
        else if (type == JPEG_TYPE) {
            return ".jpg";
        }
        else if (type == FITS_TYPE) {
            return ".fits";
        }
        else if (type == TIFF_TYPE) {
            return ".tif";
        }
        else if (type == PNG_TYPE) {
            return ".png";
        }
        else if (type == PNM_TYPE) {
            return ".pnm";
        }
        else if (type == BMP_TYPE) {
            return ".bmp";
        }
        else {
            return null;
        }
    }

    /**
     * Creates an Image for output to a file or some other output device (printer).
     **/
    protected Image createOutputImage() {
        Image paintedImage = null;

        // Get the size of the viewport
        JComponent canvas = imageDisplay.getCanvas();
        Dimension size = canvas.getSize();

        // Create a blank image the size of the original image
        paintedImage = canvas.createImage((int) size.getWidth(), (int) size.getHeight());

        // Get a Graphics object for the blank image
        Graphics2D g = (Graphics2D) paintedImage.getGraphics();
        g.setClip(0, 0, (int) size.getWidth(), (int) size.getHeight());

        // Paint canvas objects onto the image.
        imageDisplay.paintImageAndGraphics(g);

        return paintedImage;
    }

    /**
     * Prompts the user for the location of a file for saving.
     *
     * @param	initialFilename default name of the file to save
     * @param	acceptableExtensions array of acceptable file extensions for when newExtension is not null
     * @param	newExtension required file extension to append to filename if not acceptable, or null
     *
     * @return  the full file path chosen, or null if user cancelled.
     **/
    protected String promptUserForSaveLocation(String initialFilename) {
        // Set dialog title
        setDialogTitle(_I18N.getString("saveImageFile"));

        // Set initial filename
        if (initialFilename != null) {
            setSelectedFile(new File(initialFilename));
            updateFileSuffix();
        }

        // Show the dialog and wait for user choice
        int result = showSaveDialog(imageDisplay.getCanvas());

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = getSelectedFile();
            String location = file.getAbsolutePath();
            String filename = file.getName();

            // Automatically append suffix if none entered
            String type = (String) formatBox.getSelectedItem();
            if (!filename.toLowerCase().endsWith(getTypeSuffix(type))) {
                String s = getTypeSuffix(type);
                location += s;
                file = new File(location);
                filename += s;
            }

            if (file.exists()) {
                // File already exists.  Prompt for overwrite, if the new and old filenames are not the same
                String origFilename = imageDisplay.getFilename();
                if (origFilename != null) {
                    if (file.equals(new File(origFilename)))
                        return location;
                }

                int ans = DialogUtil.confirm(_I18N.getString("fileOverwriteQuestion", filename));
                if (ans != JOptionPane.YES_OPTION) {
                    return null;
                }
            }
            return location;
        }
        return null;
    }
}
