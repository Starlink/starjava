/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     14-FEB-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.ImageWriteParam;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.starlink.ast.gui.DecimalField;
import uk.ac.starlink.ast.gui.ScientificFormat;
import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * This class provides any graphic file utilities used in SPLAT. At present
 * this consists of the ability to print a given JComponent to a JPEG
 * file or a PNG file using the standard ImageIO facilities of Java2D.
 * <p>
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class GraphicFileUtilities
{
    /** Print to JPEG files */
    final public static int JPEG = 1;

    /** Print to PNG files */
    final public static int PNG = 2;

    /**
     * Display a dialog window to choose a file and print either a JPEG or PNG
     * to it. Offers the options to define the width and height and whether
     * to fit to the choosen width and height. The default width and
     * height are defined as the size of the a given component.
     */
    public static void showGraphicChooser( JComponent component )
    {

        GraphicsChooser chooser = new GraphicsChooser( component );
        chooser.setVisible( true );
        if ( chooser.accepted() ) {
            printGraphics( chooser.getFormat(), chooser.getFile(), component,
                           chooser.getWidth(), chooser.getHeight(),
                           chooser.getFit() );
        }
    }

    /**
     * Create a JPEG or PNG file from the contents of a JComponent.
     *
     * @param format the format to print, JPEG or PNG.
     * @param outputFile the output file.
     * @param component the component to print
     * @param width the width of the graphics
     * @param height the height of the graphics
     * @param fit whether to scale the width and height of the component
     *            to fit. Normally you should pass width and height as
     *            the size of the component.
     */
    public static void printGraphics( int format,
                                      File outputFile,
                                      JComponent component,
                                      int width, int height,
                                      boolean fit )
    {
        //  Get a BufferedImage to draw graphics in.
        BufferedImage image = new BufferedImage( width, height,
                                                 BufferedImage.TYPE_INT_RGB );

        //  Get the Graphics2D object needed to render into this.
        Graphics2D g2d = image.createGraphics();

        //  Scale the graphics to fit the width and height if requested.
        if ( fit ) {
            fitToWidthAndHeight( g2d, component, width, height );
        }

        //  Now make the JComponent draw into this.
        component.print( g2d );
        g2d.dispose();

        //  And write this to the file format we need.
        try {
            OutputStream os =  new FileOutputStream( outputFile );
            if ( format == JPEG ) {
                write( image, "JPEG", os );
            }
            else {
                write( image, "PNG", os );
            }
            os.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Write a BufferedImage to an output stream. Format should be a writable
     * format supported by the imageio facilities (currently JPEG and PNG).
     */
    protected static void write( BufferedImage image, String format,
                                 OutputStream os )
        throws SplatException
    {
        try {
            boolean supported = ImageIO.write( image, format, os );
            if ( ! supported ) {
                throw new SplatException( "Failed to write image, " +
                                          "no support available for "
                                          + format + " format" );
            }
        }
        catch (IOException e) {
            throw new SplatException( "Failed to write "+format+" image", e );
        }
    }

    /**
     *  Scale the current graphic so that it fits within the
     *  given width and height.
     */
    static protected void fitToWidthAndHeight( Graphics2D g2,
                                               JComponent component,
                                               int width, int height )
    {
        double compWidth = (double) component.getWidth();
        double compHeight = (double) component.getHeight();
        double xscale = (double)width / compWidth;
        double yscale = (double)height / compHeight;
        if ( xscale < yscale ) {
            yscale = xscale;
        }
        else {
            xscale = yscale;
        }

        // Apply the scale.
        g2.scale( xscale, yscale );
    }

    //
    // Internal class for choosing a JPEG filename and selecting from
    // a set of common options.
    //
    static class GraphicsChooser
        extends JDialog
    {
        /** Whether exit request was OK, otherwise cancel. */
        protected boolean accepted = false;

        /** The dialog contentpane. */
        protected JPanel contentPane;

        /** Accept and exit button. */
        protected JButton okButton = new JButton();

        /** Cancel and exit button. */
        protected JButton cancelButton = new JButton();

        /** Scale graphics to fit the image. */
        protected JCheckBox fitButton = new JCheckBox();

        /** X dimension for output file. */
        protected DecimalField xSize = null;

        /** Y dimension for output file. */
        protected DecimalField ySize = null;

        /** Name of the output file. */
        protected JTextField fileName = new JTextField();

        /** The graphic file format */
        protected JComboBox format = new JComboBox();

        /**
         * Construct an instance with default configuration.
         */
        public GraphicsChooser( JComponent component )
        {
            super();
            setModal( true );
            setTitle( Utilities.getTitle( "Print to graphics file" ) );
            init( component );
        }

        /**
         * Construct an instance, setting the parent, window title and
         * whether the dialog is modal.
         */
        public GraphicsChooser( JComponent component, Frame owner,
                                String title, boolean modal)
        {
            super( owner, title, modal );
            init( component );
        }

        /**
         * Start common initialisation sequence.
         */
        protected void init( JComponent component )
        {
            enableEvents( AWTEvent.WINDOW_EVENT_MASK );
            try {
                initUI( component );
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            this.accepted = false;
        }

        /**
         * Initialise the user interface.
         */
        private void initUI( JComponent component )
        {
            //  Get the dialog content pane and set the layout manager.
            contentPane = (JPanel) this.getContentPane();
            contentPane.setLayout( new BorderLayout() );
            contentPane.setBorder
                ( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );

            //  Configure and populate the controls for selecting
            //  size and name options.
            JPanel centrePanel = new JPanel();
            centrePanel.setBorder(BorderFactory.createTitledBorder
                                  ("Graphics image properties"));

            GridBagLayouter layouter =
                new GridBagLayouter( centrePanel, GridBagLayouter.SCHEME3 );
            layouter.setInsets( new Insets( 5, 5, 5, 5 ) );

            //  Select the graphics format.
            format.addItem( "JPEG" );
            format.addItem( "PNG" );
            layouter.add( "Graphic format:" , false );
            layouter.add( format, true );

            //  Choose whether to scale to fit.
            fitButton.setSelected( false );
            layouter.add( "Scale to fit:", false );
            layouter.add( fitButton, true );
            fitButton.setToolTipText
                ( "Scale plot graphics to fit the width and height" );

            //  Offer size of output image in pixels.
            ScientificFormat scientificFormat = new ScientificFormat();
            xSize = new DecimalField( 0, 10, scientificFormat );
            xSize.setIntValue( component.getWidth() );

            layouter.add( new JLabel( "X size:" ), false );
            layouter.add( xSize, true );
            xSize.setToolTipText( "Width, in pixels, of the "+
                                  "output image (default is actual size" );

            scientificFormat = new ScientificFormat();
            ySize = new DecimalField( 0, 10, scientificFormat );
            ySize.setIntValue( component.getHeight() );

            layouter.add( new JLabel( "Y size:" ), false );
            layouter.add( ySize, true );
            ySize.setToolTipText( "Height, in pixels, of the "+
                                  "output image (default is actual size" );

            //  Get a name for the file.
            fileName.setText( Utilities.getApplicationName() );

            layouter.add( new JLabel( "Output file:" ), false );
            layouter.add( fileName, true );
            fileName.setToolTipText
                ( "Name for the output file (without extension)" );
            layouter.eatSpare();

            //  Configure and place the OK and Cancel buttons.
            JPanel buttonPanel = new JPanel();
            buttonPanel.setBorder( BorderFactory.createEmptyBorder(10,10,0,0));
            buttonPanel.setLayout( new BoxLayout( buttonPanel,
                                                  BoxLayout.X_AXIS ) );
            buttonPanel.add( Box.createHorizontalGlue() );
            buttonPanel.add( okButton );
            buttonPanel.add( Box.createHorizontalGlue() );
            buttonPanel.add( cancelButton );
            buttonPanel.add( Box.createHorizontalGlue() );

            //  Set various close window buttons.
            okButton.setText( "OK" );
            okButton.addActionListener( new ActionListener() {
                    public void actionPerformed( ActionEvent e ) {
                        closeWindow( true );
                    }
                });
            okButton.setToolTipText( "Press to create the graphics file" );

            cancelButton.setText( "Cancel" );
            cancelButton.addActionListener( new ActionListener() {
                    public void actionPerformed( ActionEvent e ) {
                        closeWindow( false );
                    }
                });
            cancelButton.setToolTipText
                ( "Press to cancel creation of graphics file" );

            //  Add main panels to contentPane.
            contentPane.add( buttonPanel, BorderLayout.SOUTH );
            contentPane.add( centrePanel, BorderLayout.CENTER );
            pack();
        }

        /**
         * Return the exit status of the dialog.
         */
        public boolean accepted()
        {
            return accepted;
        }

        /**
         * Return whether user opted for scaling the graphic to fit.
         */
        public boolean getFit()
        {
            return fitButton.isSelected();
        }

        /**
         * Return the width of the output file.
         */
        public int getWidth()
        {
            return xSize.getIntValue();
        }

        /**
         * Return the height of the output file.
         */
        public int getHeight()
        {
            return ySize.getIntValue();
        }

        /**
         * Return the name of the output file.
         */
        public File getFile()
        {
            String extension = ".jpg";
            if ( format.getSelectedIndex() == 1 ) {
                extension = ".png";
            }
            return new File( fileName.getText() + extension );
        }

        /**
         * Get the format.
         */
        public int getFormat()
        {
            return ( format.getSelectedIndex() + 1 );
        }

        /**
         * Close the window. If argument is true then the result is accepted.
         */
        protected void closeWindow( boolean accepted )
        {
            this.accepted = accepted;
            setVisible( false );
        }
    }
}
