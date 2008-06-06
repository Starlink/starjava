/*
 * Copyright (C) 2007 Particle Physics and Astronomy Research Council
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     27-MAR-2007 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import java.awt.Rectangle;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.awt.print.PageFormat;
import javax.print.PrintService;
import javax.print.StreamPrintService;
import javax.print.StreamPrintServiceFactory;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;

import org.jibble.epsgraphics.EpsGraphics2D;

/**
 * Utility class to contain various functions related to printing.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PrintUtilities
{
    /**
     * Constructor, private so it cannot be used.
     */
    private PrintUtilities()
    {
        //  Do nothing.
    }

    /**
     * Print a Printable object using the Java printing system to open a
     * dialog. If no local printers are available try printing to a postscript
     * file instead.
     *
     * @param printable an object that supports the {@link Printable}
     *                  interface (Java2D).
     * @param pageSet the default page setup.
     * @param bounds bounds of the region printed, if EPS.
     * @param postscriptFile name of a file to use if the postscript
     *                       option is only one available.
     */
    public static void print( Printable printable,
                              PrintRequestAttributeSet pageSet,
                              Rectangle bounds, String postscriptFile,
                              boolean landscape )
        throws SplatException
    {
        try {
            PrintService[] services = PrinterJob.lookupPrintServices();
            if ( services.length == 0 ) {

                // No actual print services are available (i.e. no valid local
                // printers), then fall back to the postscript option.
                System.out.println( "Falling back to postscript" );
                PrintUtilities.printPostscript( printable, false, pageSet,
                                                bounds, postscriptFile );
                return;
            }
            PrinterJob pj = PrinterJob.getPrinterJob();
            pj.setPrintService( services[0] );
            pj.setPrintable( printable );
            if ( pj.printDialog( pageSet ) ) {
                pj.print( pageSet );
            }
        }
        catch (PrinterException e) {
            throw new SplatException( e );
        }
    }

    /**
     * Print to a postscript file, EPS if requested.
     *
     * @param printable the object to print.
     * @param eps whether an EPS file is required
     * @param pageSet page description, if not EPS.
     * @param bounds bounds of the region printed, if EPS.
     * @param fileName name of the destination file, if EPS.
     *
     * @throws SplatException if there are problems or an printer
     *                        cannot be found
     */
    public static void printPostscript( Printable printable, boolean eps,
                                        PrintRequestAttributeSet pageSet,
                                        Rectangle bounds, String fileName )
        throws SplatException
    {
        if ( eps ) {
            try {
                BufferedOutputStream ostrm =
                    new BufferedOutputStream( new FileOutputStream(fileName) );
                EpsGraphics2D g2 =
                    new EpsGraphics2D( fileName, ostrm,
                                       bounds.x, bounds.y,
                                       bounds.x + bounds.width,
                                       bounds.y + bounds.height );
                printable.print( g2, null, 0 );
                g2.close();
            }
            catch (Exception e) {
                throw new SplatException( e );
            }
        }
        else {
            PrintService service =
                PrintUtilities.getPostscriptPrintService( fileName );
            if ( service != null ) {
                try {
                    PrinterJob pj = PrinterJob.getPrinterJob();
                    pj.setPrintService( service );
                    pj.setPrintable( printable );
                    if ( pj.printDialog( pageSet ) ) {
                        pj.print( pageSet );
                        StreamPrintService sps = (StreamPrintService) service;
                        sps.dispose();
                        sps.getOutputStream().close();
                    }
                }
                catch ( PrinterException e ) {
                    throw new SplatException( e );
                }
                catch ( IOException e ) {
                    throw new SplatException( e );
                }
            }
            else {
                // Report there are no printers available.
                throw new SplatException( "Sorry no printers are available" );
            }
        }
    }

    /**
     * Look for a postscript printing service and it configured
     * to write to the given file.
     *
     * @param fileName the name of a file that the services should write too.
     * @return a {@link PrintService}s that produces postscript or null
     *         if none found.
     */
    public static PrintService getPostscriptPrintService( String fileName )
        throws SplatException
    {
        PrintService service = null;
        FileOutputStream outstream;
        StreamPrintService psPrinter;
        String psMimeType = "application/postscript";

        StreamPrintServiceFactory[] factories =
            PrinterJob.lookupStreamPrintServices( psMimeType );
        if ( factories.length > 0 ) {
            try {
                outstream = new FileOutputStream( new File( fileName ) );
                service =  factories[0].getPrintService( outstream );
            }
            catch ( FileNotFoundException e ) {
                throw new SplatException( e );
            }
        }
        return service;
    }

    /**
     * Create a default {@link PrintRequestAttributeSet} for printing.
     * This is A4.
     *
     * @param landscape if true the PageSet is landscape, otherwise portrait.
     */
    public static PrintRequestAttributeSet makePageSet( boolean landscape )
    {
        PrintRequestAttributeSet pageSet = null;
        pageSet = new HashPrintRequestAttributeSet();
        if ( landscape ) {
            pageSet.add( OrientationRequested.LANDSCAPE );
        }
        else {
            pageSet.add( OrientationRequested.PORTRAIT );
        }
        pageSet.add( MediaSizeName.ISO_A4 );
        pageSet.add(new JobName( Utilities.getTitle( "printer job" ), null ));
        return pageSet;
    }
}
