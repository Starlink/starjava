/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * (Modified from NASA/SEA classes.)
 *
 * $Id: ImagePrintDialog.java,v 1.8 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.image.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JComponent;

import jsky.coords.WorldCoordinateConverter;
import jsky.coords.WorldCoords;
import jsky.util.I18N;
import jsky.util.SwingWorker;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.PrintPreview;
import jsky.util.gui.ProgressPanel;


/**
 * Displays a print dialog box for printing the current image display
 * and handles the details of printing the image and graphics.
 */
public class ImagePrintDialog implements Printable, ActionListener {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(ImagePrintDialog.class);

    /** The target image display */
    protected MainImageDisplay imageDisplay;

    /** Panel used to display print progress */
    protected ProgressPanel progressPanel;

    /**
     * Font used for printing text (headers and footers)
     **/
    private static final Font PRINTING_FONT = Font.decode("SansSerif-8");

    private boolean fNewPrint;
    private double fPrintOffsetX;
    private double fPrintOffsetY;
    private final SimpleDateFormat fDateFormatter = new SimpleDateFormat("MM/dd/yy HH:mm:ss");


    /** Initialize with the target image display object. */
    public ImagePrintDialog(MainImageDisplay imageDisplay) {
        this.imageDisplay = imageDisplay;
    }


    /**
     * Display a preview of the image to be printed in a popup window.
     **/
    public void preview() {
        SwingWorker worker = new SwingWorker() {

            public Object construct() {
                try {
                    String title = imageDisplay.getObjectName();
                    if (title == null)
                        title = imageDisplay.getFilename();
                    if (title == null)
                        title = _I18N.getString("printPreview");
                    startPrint(_I18N.getString("preparingImage"));
                    return new PrintPreview(ImagePrintDialog.this, ImagePrintDialog.this, title);
                }
                catch (Exception e) {
                    return e;
                }
            }

            public void finished() {
                progressPanel.stop();
                progressPanel.setTitle(_I18N.getString("printingImage"));
                Object o = getValue();
                if (o instanceof Exception) {
                    DialogUtil.error((Exception) o);
                }
                //else if (o instanceof PrintPreview) {
                //PrintPreview pp = (PrintPreview)o;
                //}
            }
        };
        worker.start();
    }

    /** Called for the Print button in the preview window */
    public void actionPerformed(ActionEvent e) {
        print();
    }


    /**
     * Prints the contents of the current image display image area.
     * Prompts user with standard print dialog boxes first.
     **/
    public void print() {
        // Get a PrinterJob
        final PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(_I18N.getString("imageDisplay"));

        // Get the page format from the user
        PageFormat format = job.pageDialog(job.defaultPage());

        // Create a Book to contain all the page info
        // Pass the canvas to the print job, since canvas is a Printable
        Book document = new Book();
        document.append(this, format);
        job.setPageable(document);

        // Put up the dialog box
        if (job.printDialog()) {
            startPrint(_I18N.getString("printing"));
            new PrintWorker(job).start();
        }
    }

    /**
     * For the Printable interface: Render the image contents onto a
     * printable graphics context.  Provides the ability to print the
     * image canvas contents.
     */
    public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
        Graphics2D g2d = (Graphics2D) g;
        JComponent canvas = imageDisplay.getCanvas();
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        // Indicate end of page if printing task is interrupted
        if (pageIndex > 0) {
            return Printable.NO_SUCH_PAGE;
        }

        boolean progress = true;
        if (fNewPrint) {
            // Remember the original clip offset
            fNewPrint = false;
            progress = false; // No progress event first time because of irregular clip bounds
            Rectangle r = g2d.getClipBounds();
            if (r != null) {
                fPrintOffsetX = r.x;
                fPrintOffsetY = r.y;
            }
        }

        // Compute the scale
        double scale = Math.min((pf.getImageableWidth() - 20) / (double) canvasWidth,
                (pf.getImageableHeight() - 20) / (double) canvasHeight);

        // Draw the footer text
        // Just draws name of first image.
        // Probably should rethink how this works for multiple images.
        // Determine default file name
        String footer = imageDisplay.getObjectName();
        if (footer == null)
            footer = imageDisplay.getFilename();
        if (footer == null) {
            if (imageDisplay.isWCS()) {
                WorldCoordinateConverter wcc = imageDisplay.getWCS();
                WorldCoords center = new WorldCoords(wcc.getWCSCenter(), wcc.getEquinox());
                footer = center.toString();
            }
            else {
                footer = _I18N.getString("blankImage");
            }
        }

        FontMetrics metrics = canvas.getFontMetrics(PRINTING_FONT);
        int width = metrics.stringWidth(footer) + 6;
        int height = metrics.getHeight() + 4;
        g2d.setColor(Color.black);
        g2d.setFont(PRINTING_FONT);
        g2d.drawString(footer,
                (float) fPrintOffsetX,
                (float) (((canvasHeight + height) * scale) + pf.getImageableY()));
        footer = fDateFormatter.format(new Date());
        width = metrics.stringWidth(footer) + 6;
        g2d.drawString(footer,
                (float) (fPrintOffsetX + ((canvasWidth - width) * scale) - 15),
                (float) (((canvasHeight + height) * scale) + pf.getImageableY()));

        // Translate and scale the graphics to fit on the page
        g2d.translate(fPrintOffsetX, fPrintOffsetY);
        g2d.scale(scale, scale);

        // Clip the canvas drawing so that none of the Viewable objects are drawn
        // outside of the image area.
        int y = 0;
        int x = 0;
        int h = canvasHeight;
        int w = canvasWidth;

        if (g2d.getClipBounds() != null) {
            x = g2d.getClipBounds().x;
            y = g2d.getClipBounds().y;
            w = g2d.getClipBounds().width;
            h = g2d.getClipBounds().height;

            if (x + w > canvasWidth) {
                w = (int) (canvasWidth);
            }
            if (y + h > canvasHeight) {
                h = (int) Math.max(0, (int) (canvasHeight) - y);
            }
        }
        g2d.setClip(x, y, w, h);

        // Paint canvas objects onto the image.
        imageDisplay.paintImageAndGraphics(g2d);

        if (progress) {
            int percent = (int) Math.min(100, Math.floor(((double) (y + h) / (double) canvasHeight) * 100.0));
            progressPanel.setProgress(percent);
        }

        return Printable.PAGE_EXISTS;
    }

    /**
     * Initialize printing.  This method must be called at the beginning of any
     * print operation because the print() method will be called multiple times.
     *
     * @param msg the message for the progress dialog
     **/
    public void startPrint(String msg) {
        fNewPrint = true;
        fPrintOffsetX = 0.0;
        fPrintOffsetY = 0.0;

        if (progressPanel == null)
            progressPanel = ProgressPanel.makeProgressPanel(msg);
        else
            progressPanel.setTitle(msg);
        progressPanel.start();
    }


    /**
     * Performs all the print calculations in a separate thread.
     * A progress bar is shown to the user while the printing occurs.
     **/
    protected class PrintWorker extends SwingWorker {

        private PrinterJob fJob;

        public PrintWorker(PrinterJob job) {
            fJob = job;
        }

        public Object construct() {
            try {
                progressPanel.setProgress(5);
                fJob.print();
                progressPanel.setProgress(99);

            }
            catch (Exception ex) {
                DialogUtil.error(_I18N.getString("printError") + ": " + ex.toString());
            }
            return null;
        }

        public void finished() {
            progressPanel.stop();
        }
    }
}


