// Copyright (C) 2002 Central Laboratory of the Research Councils

// History:
//    17-JUN-2002 (Peter W. Draper):
//       Original version.

package uk.ac.starlink.frog.util;

import java.net.URL;
import java.io.IOException;
import java.net.Socket;
import org.w3c.dom.Element;

import uk.ac.starlink.soap.AppHttpSOAPServer;

import uk.ac.starlink.frog.Frog;
import uk.ac.starlink.frog.iface.PlotControlFrame;
import uk.ac.starlink.frog.data.TimeSeries;
import uk.ac.starlink.frog.data.TimeSeriesImpl;
import uk.ac.starlink.frog.data.TXTTimeSeriesImpl;
import uk.ac.starlink.frog.data.Gram;
import uk.ac.starlink.frog.data.GramFactory;
import uk.ac.starlink.frog.util.FrogDebug;

/**
 * Implements the SOAP web services offered by the FROG
 * application. There is only one instance of this class for the FROG
 * application, but it does not come into existance until the
 * {@link #getInstance()} method is invoked. 
 * <p>
 * Current (unlike the IPC socket remote services) you should always
 * set the port number in use (the default of 8084 isn't suitable for
 * applications on a general user machine).
 *
 * @author Peter W. Draper, Alasdair Allan
 * @version $Id$
 */
public class FrogSOAPServer
{
   /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();

    /** 
     * The application HTTP/SOAP server 
     */
    private AppHttpSOAPServer server = null;

    /**
     * The instance of FrogBrowser that we're attached to.
     */
    private Frog browserMain = null;

    /** 
     * The port number for the HTTP server. Always re-define this. 
     */
    private int portNumber = 8084;

    /**
     * The instance.
     */
    private static FrogSOAPServer instance = null;

    /**
     * Get the instance. Uses lazy instantiation so object does not
     * exist until the first invocation of this method. Make sure 
     * that the FrogBrowser to be used is set before making any of
     * of this reference {@link #setFrogBrowser}.
     */
    public static FrogSOAPServer getInstance()
    {
        if ( instance == null ) {
            try {
                instance = new FrogSOAPServer();
            }
            catch (IOException e) {
                e.printStackTrace();
                instance = null;
            }
        }
        return instance;
    }

    /**
     * Listen for remote RPC SOAP requests.
     */
    private FrogSOAPServer()
        throws IOException
    {
        //  Do nothing.
    }

    /**
     * Set the instance of FrogBrowser that we're attached to.
     */
    public void setFrog( Frog browserMain )
    {
        this.browserMain = browserMain;
    }

    /**
     * Get the instance of FrogBrowser that we're attached to.
     */
    public Frog getFrog()
    {
        return browserMain;
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
     * Start the remote services. Do not forget to use this method as
     * no services are available until after it is invoked. 
     */
    public void start()
    {
        //  Create the HTTP/SOAP server. Need our local description to
        //  define the SOAP services offered (by this class). 
        URL deployURL = FrogSOAPServer.class.getResource( "deploy.wsdd" );
        debugManager.print( "    url = " + deployURL.toString() );

        //  Check if this port is already bound. In which case give up
        //  now!
        boolean open = true;
        try {
            Socket tempSocket = new Socket( "localhost", portNumber );
            tempSocket.close();
        }
        catch (Exception any) {
            // Fails if not already in use, which is good.
            open = false;
        }
        if ( open ) {
            throw new RuntimeException( "Failed to start FROG " +
                                        "SOAP services, port already in use" );
        }
        try {
            server = new AppHttpSOAPServer( portNumber );
            server.start();
            server.addSOAPService( deployURL );
            debugManager.print( "    port = " + portNumber );
        }
        catch ( Exception e ) {
            e.printStackTrace();
            throw new RuntimeException("Failed to start FROG SOAP services");
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

//
// Define the actual services, these are mediated through a static
// class SOAPServices.
//
    /**
     * Display a TimeSeries by name.
     *
     * @param series the spectrum specification
     */
    public boolean displaySeries( String series )
    {
       debugManager.print( "Called displaySeries() via SOAP Service");
       debugManager.print( "Passed:\n" + series );
       browserMain.setStatus("Recieved SOAP message...");
       
       // break the String into lines
       String[] lines = series.split("\\n");
       
       // Create a TimeSeriesImpl
       TimeSeriesImpl impl = null;
       try {
          impl = new TXTTimeSeriesImpl( lines );
       
          // Wrap the implementation in a time series object
          TimeSeries newSeries = new TimeSeries( impl );
          newSeries.setOrigin( "a SOAP message" );
          newSeries.setType( TimeSeries.TIMESERIES );
       
          // add it to the Frog Main Window
          browserMain.addSeries( newSeries );
       } catch (FrogException fe) {
           //fe.printStackTrace();
           debugManager.print( "Unable to parse message...");
           browserMain.setStatus("ERROR: Unable to parse SOAP message");
           return false;
       } catch (NumberFormatException ne) {
           debugManager.print( "Unable to parse message...");
           debugManager.print( "Doesn't look like it's even numeric input?");
           browserMain.setStatus("ERROR: Unable to parse SOAP message");
           return false;
       } catch (Exception e ){
           debugManager.print( "Unable to parse message...");
           debugManager.print( "Generic Exception, who knows...");
           browserMain.setStatus("ERROR: Unable to parse SOAP message");
           return false;
       } 
       return true;       
    }

//
// Define the actual services, these are mediated through a static
// class SOAPServices.
//
    /**
     * Get a time series and perform a fourier transform on the
     *
     * @param series the spectrum specification
     */
    public String getFourierTransform( 
     String series, double minFreq, double maxFreq, double freqInterval) {
       debugManager.print( "Called getFourierTransform() via SOAP Service");
       debugManager.print( "Passed:\n" + series );
       browserMain.setStatus("Recieved SOAP message...");
       
       // break the String into lines
       String[] lines = series.split("\\n");
       
       // Create a TimeSeries
       TimeSeries newSeries = null;
       try {
          TimeSeriesImpl impl = new TXTTimeSeriesImpl( lines );
       
          // Wrap the implementation in a time series object
          newSeries = new TimeSeries( impl );
          newSeries.setOrigin( "a SOAP message" );
          newSeries.setType( TimeSeries.TIMESERIES );
       
       } catch (FrogException fe) {
           //fe.printStackTrace();
           debugManager.print( "Unable to parse message...");
           browserMain.setStatus("ERROR: Unable to parse SOAP message");
           return "Error: Unable to parse message";
       } catch (NumberFormatException ne) {
           debugManager.print( "Unable to parse message...");
           debugManager.print( "Doesn't look like it's even numeric input?");
           browserMain.setStatus("ERROR: Unable to parse SOAP message");
           return "Error: Unable to parse message, doesn't look numeric";
       } catch (Exception e ){
           debugManager.print( "Unable to parse message...");
           debugManager.print( "Generic Exception, who knows...");
           browserMain.setStatus("ERROR: Unable to parse SOAP message");
           return "Error: Unknown error parising message, giving up";
       }
       
       // Perform the fourier transform
       // -----------------------------
       browserMain.setStatus("Creating a Fourier Transform...");
       
       // Periodogram Factory
       Gram periodogram = null;
       GramFactory gramFactory = GramFactory.getReference();       
       debugManager.print( "  Building gramFactory..." );
       try {
          debugManager.print( "    Performing a FOURIER TRANSFORM...");
          periodogram = gramFactory.get( 
               newSeries, false, minFreq, maxFreq, freqInterval, "FOURIER" );
       } catch ( Exception e ) {         
         // do nothing
         //e.printStackTrace();
         debugManager.print( "    Unable to generate an Fourier Transform");
         return "Error: Unable to sucessfully generate a Fourier Transform";
       }
         
       // verbose for debugging  
       double[] xData = periodogram.getXData();
       double[] yData = periodogram.getYData(); 
       String returnMessage = null;     
       for ( int i = 0; i < xData.length; i++ ) {
          returnMessage = returnMessage + xData[i] + " " + yData[i] + "\n";
       }   
       
       browserMain.setStatus("Returned message to SOAP client...");
       debugManager.print( "     Returning FOURIER TRANSFORM" );
       return returnMessage;
              
    }


}
