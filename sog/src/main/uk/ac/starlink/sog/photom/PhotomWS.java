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

    /** The port number for the HTTP server. Always re-define this. */
    private int portNumber = 8083;

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
        return instance;
    }

    /**
     * Listen on the given port for remote RPC SOAP requests.
     */
    private PhotomWS()
        throws IOException
    {
        // Do nothing? Look for PHOTOM?
    }

    /**
     * Set the port number of the HTTP server.
     */
    public void setPortNumber( int portNum ) 
    {
        portNumber = portNum;
    }

    /**
     * Return the port number being used by this application.
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
     * Run autophotom on a NDX and PhotomList. The results are
     * returned as a PhotomList. All program parameters are defaulted.
     */
    public Element autophotom( Element ndxElement, 
                               Element photomListElement )
        throws IOException
    {
        System.out.println( "Autophotom:" + ndxElement.getTagName() + 
                            ", " + photomListElement.getTagName() );
        
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

        //System.out.println( "Exec: \n" + 
        //                    "/stardev/bin/photom/autophotom in=" +
        //                    tmpNdf.getContainerName() +
        //                    " infile=" + positionsFile.getPath() + 
        //                    " outfile=" + resultsFile.getPath() + 
        //                    " centro=true accept" );

        Process process = Runtime.getRuntime().exec
            ( "/stardev/bin/photom/autophotom in=" +
              tmpNdf.getContainerName() +
              " infile=" + positionsFile.getPath() + 
              " outfile=" + resultsFile.getPath() + 
              " centro=true accept" );

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

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
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
        return newPhotomListElement;
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
        System.out.println( "Autophotom:" + element.getTagName() );
        System.out.println( "positions file: " + positionsFile );
        System.out.println( "results file: " + resultsFile );
        
        //  Convert the NDX into a local NDF.
        Ndx ndx = XMLNdxHandler.getInstance().makeNdx
            ( new DOMSource( element ), AccessMode.READ );
        NdfMaker maker = new NdfMaker();
        HDSReference tmpNdf = maker.makeTempNDF( ndx );

        //System.out.println( "Exec: \n" + 
        //                    "/stardev/bin/photom/autophotom in=" +
        //                    tmpNdf.getContainerName() +
        //                    " infile=" + positionsFile + 
        //                    " outfile=" + resultsFile + 
        //                    " centro=true accept" );

        Process process = Runtime.getRuntime().exec
            ( "/stardev/bin/photom/autophotom in=" +
              tmpNdf.getContainerName() +
              " infile=" + positionsFile + 
              " outfile=" + resultsFile + 
              " centro=true accept" );

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

    public static void main( String[] args )
    {
        JFrame frame = new JFrame( "PHOTOM Web Service (Test)" );
        PhotomWS soapServer = PhotomWS.getInstance();
        soapServer.start();
        JLabel label = new JLabel
        ("<html><h1><font color=red>PHOTOM Web Service</font></h1>");
        frame.getContentPane().add( label );
        frame.pack();
        frame.setVisible( true );
    }
}
