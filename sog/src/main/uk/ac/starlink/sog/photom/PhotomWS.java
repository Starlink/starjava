/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-OCT-2002 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.sog.photom;

import java.io.IOException;
import java.io.File;
import java.net.URL;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.hds.HDSReference;
import uk.ac.starlink.hds.NdfMaker;
import uk.ac.starlink.jaiutil.HDXImage;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.XMLNdxHandler;
import uk.ac.starlink.soap.AppHttpSOAPServer;

import jsky.util.ProxyServerUtil;

/**
 * A test "web-service" wrapper for the PHOTOM application
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PhotomWS
{
    /** The application HTTP/SOAP server */
    private AppHttpSOAPServer server = null;

    /** The default port number for the HTTP server.*/
    private int defaultPortNumber = 8083;

    /** The port number used for the HTTP server. Always re-define this. */
    private int portNumber = defaultPortNumber;

    /** The instance */
    private static PhotomWS instance = null;

    /**
     * Get the instance. Uses lazy instantiation so object does not
     * exist until the first invocation of this method.
     */
    public static PhotomWS getInstance()
    {
        if ( instance == null ) {
            try {
                instance = new PhotomWS();
            }
            catch (IOException e) {
                e.printStackTrace();
                instance = null;
            }
        }

        // Setup the same proxy server as SoG.
        ProxyServerUtil.init();

        return instance;
    }

    /**
     * Listen on either the default port or that defined by the
     * "port.number" property for remote RPC SOAP requests.
     */
    private PhotomWS()
        throws IOException
    {
        establishPortNumber();
    }

    /**
     * Get an initial port number. This will be that defined by the
     * "port.number" property, or the default value. Find out which
     * using the getPortNumber() method.
     */
    protected void establishPortNumber()
    {
        String portString = System.getProperty( "port.number" );
        if ( portString != null ) {
            try {
                portNumber = Integer.parseInt( portString );
            }
            catch ( NumberFormatException e ) {
                // May be a bad number...
                e.printStackTrace();
                portNumber = defaultPortNumber;
            }
        }
        else {
            portNumber = defaultPortNumber;
        }
    }

    /**
     * Set the port number of the HTTP server.
     */
    public void setPortNumber( int value )
    {
        portNumber = value;
    }

    /**
     * Return the port number being used.
     */
    public int getPortNumber()
    {
        return portNumber;
    }

    /**
     * Start the remote control services. Do not forget to use this
     * method as no services are available until after it is invoked.
     */
    public void start()
    {
        //  Create the HTTP/SOAP server. Need our local description to
        //  define the SOAP services offered (by this class).
        URL deployURL = PhotomWS.class.getResource( "deploy.wsdd" );
        System.out.println( deployURL );
        try {
            server = new AppHttpSOAPServer( portNumber );
            server.start();
            server.addSOAPService( deployURL );
            System.out.println( "port = " + portNumber );
        }
        catch ( Exception e ) {
            e.printStackTrace();
            throw new RuntimeException( "Failed to start SOAP services" );
        }
    }

    /**
     * Stop the remote control services.
     */
    public void stop()
    {
        if ( server != null ) {
            try {
                server.stop();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Run autophotom on a NDX, PhotomList and PhotometryGlobals.
     * The results are returned as a PhotomList. Any unspecified
     * program parameters are defaulted.
     */
    public Element autophotom( Element ndxElement,
                               Element photomListElement,
                               Element globalsElement )
        throws IOException
    {
        System.out.println( "Autophotom:" + ndxElement.getTagName() +
                            ", " + photomListElement.getTagName() );

        if ( displayLabel != null ) {
            displayLabel.setText( busyString );
        }
        try {

            //  Convert the NDX into a temporary local NDF.
            Ndx ndx = XMLNdxHandler.getInstance().makeNdx
                ( new DOMSource( ndxElement ), AccessMode.READ );
            NdfMaker maker = new NdfMaker();
            HDSReference tmpNdf = maker.makeTempNDF( ndx );
            
            //  Create a local PhotomList and configure it from the given
            //  Element.
            PhotomList photomList = new PhotomList();
            photomList.decode( photomListElement );
            
            //  Convert the PhotomList into a local positions file.
            File positionsFile = File.createTempFile( "photom_in", ".lis" );
            photomList.write( positionsFile );
            File resultsFile = File.createTempFile( "photom_out", ".lis" );
            
            //  Create a local PhotometryGlobals and configure it from the
            //  given Element.
            PhotometryGlobals globals = new PhotometryGlobals();
            globals.decode( globalsElement );
            
            //  And convert this into a parameter-like string.
            String extras = globals.toApplicationString();
            
            //  Create the command.
            StringBuffer cmd = new StringBuffer();
            cmd.append( "/stardev/bin/photom/autophotom" );
            cmd.append( " in=" + tmpNdf.getContainerName() );
            cmd.append( " infile=" + positionsFile.getPath() );
            cmd.append( " outfile=" + resultsFile.getPath() );
            cmd.append( " " + extras );
            cmd.append( " accept" );
            
            //  And execute it.
            Process process = Runtime.getRuntime().exec( cmd.toString() );
            
            System.out.println( "Starting wait..." );
            int exitValue = 0;
            try {
                exitValue = process.waitFor();
            }
            catch (InterruptedException e) {
                throw new IOException( e.getMessage() );
            }
            
            // Remove temporary files that are no longer needed.
            positionsFile.delete();
            tmpNdf.getContainerFile().delete();
            
            if ( exitValue != 0 ) {
                resultsFile.delete();
                throw new IOException
                    ( "Process exited with non-zero status: " + exitValue );
            }
            System.out.println( "Completed wait..." );
            
            //  Update the PhotomList and return it.
            photomList.read( resultsFile );
            resultsFile.delete();
            
            DocumentBuilderFactory factory = 
                DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = null;
            try {
                builder = factory.newDocumentBuilder();
            }
            catch (Exception e) {
                throw new IOException( "Error converting PhotomList to XML" );
            }
            Document document = builder.newDocument();
            Element newPhotomListElement =
                document.createElement( photomList.getTagName() );
            photomList.encode( newPhotomListElement );

            if ( displayLabel != null ) {
                displayLabel.setText( idleString );
            }
            return newPhotomListElement;
        } 
        finally { 
            if ( displayLabel != null ) {
                displayLabel.setText( idleString );
            }
        }
    }

    /**
     * Run autophotom on a NDX and input file (with default parameters).
     * The results are returned in a new local file resultsFile. All
     * program parameters are defaulted.
     */
    public void autophotom( Element element,
                            String positionsFile,
                            String resultsFile )
        throws IOException
    {
        if ( displayLabel != null ) {
            displayLabel.setText( busyString );
        }

        try {
            System.out.println( "Autophotom:" + element.getTagName() );
            System.out.println( "positions file: " + positionsFile );
            System.out.println( "results file: " + resultsFile );

            //  Convert the NDX into a local NDF.
            Ndx ndx = XMLNdxHandler.getInstance().makeNdx
                ( new DOMSource( element ), AccessMode.READ );
            NdfMaker maker = new NdfMaker();
            HDSReference tmpNdf = maker.makeTempNDF( ndx );
            
            //  Create the command.
            StringBuffer cmd = new StringBuffer();
            cmd.append( "/stardev/bin/photom/autophotom" );
            cmd.append( " in=" + tmpNdf.getContainerName() );
            cmd.append( " infile=" + positionsFile );
            cmd.append( " outfile=" + resultsFile );
            cmd.append( " accept" );
            
            //  And execute it.
            Process process = Runtime.getRuntime().exec( cmd.toString() );
            
            System.out.println( "Starting wait..." );
            int exitValue = 0;
            try {
                exitValue = process.waitFor();
            }
            catch (InterruptedException e) {
                throw new IOException( e.getMessage() );
            }
            
            // Remove temporary file.
            tmpNdf.getContainerFile().delete();
            if ( exitValue != 0 ) {
                throw new IOException
                    ( "Process exited with non-zero status: " + exitValue );
            }
            System.out.println( "Completed wait..." );
        } 
        finally {
            if ( displayLabel != null ) {
                displayLabel.setText( idleString );
            }
        }
    }

    private static JLabel displayLabel = null;
    private static String idleString = 
        ("<html><h2><font color=red><strike>PHOTOM Web Service</strike></font></h2>");
    private static String busyString = 
        ("<html><h2><font color=green><u>PHOTOM Web Service</u></font></h2>");

    /** Run up the webservice as an application */
    public static void main( String[] args )
    {
        JFrame frame = new JFrame( "PHOTOM Web Service (Test)" );
        PhotomWS soapServer = PhotomWS.getInstance();
        soapServer.start();
        displayLabel = new JLabel( idleString );
        frame.getContentPane().add( displayLabel );
        frame.pack();
        frame.setVisible( true );
    }
}
