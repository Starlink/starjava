/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     28-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.frog.util;

import bsh.Interpreter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

import uk.ac.starlink.frog.Frog;

/**
 * This class provides remote control and plugin facilities.<p>
 *
 * <b>Remote Control</b><p>
 *
 * Remote control is performed by issuing
 * <a href="http://www.beanshell.org">Beanshell</a> commands delivered
 * through a socket. The details of the port are stored in the file
 * <code>~/.splat/.remote</code>, which contains the application
 * hostname, the port that a server socket is listening on and an
 * authentification "cookie". The cookie must be passed as the first
 * command string after the connection is made. Connectors not
 * providing this cookie are disconnected.<p>
 *
 * Remote commands can be any valid Java statement, as well as valid
 * Beanshell statements. A couple of special variables are preset to
 * refer to the main <code>Frog</code> object (which displays
 * the global list of spectra that are available) and the
 * GlobalSpecPlotList object. These are called <code>browser</code>
 * and <code>globallist</code> and should be used as entry points for 
 * getting access to internal objects. For instance if the server
 * received the command:
 * <pre>
 *    plot = browser.displaySpectrum( "spectrum.sdf" );
 * </pre>
 * Then the spectrum stored at the top-level of the container file
 * <code>spectrum.sdf</code> would be opened, added to the global list
 * and displayed in a new <code>PlotControlWindow</code> object. See
 * the <code>Frog</code> documentation for the other commands
 * that you could use. Note that the plot reference is a lazy
 * Beanshell variable and now contains the reference to the
 * <code>PlotControlWindow</code>, which can also be controlled.<p>
 *
 * More examples...<p>
 *
 * <b>Plugins</b><p>
 *
 * A plugin is code that is dynamically added after the application
 * starts running. Plugins (like remote control) can have access to the
 * complete set of API's available, but are loaded locally from disk,
 * rather than over a socket.<p>
 *
 * To automatically load a plugin when this class instantiates itself
 * (a method which implies that only one RemoteServer object should be
 * created per-application), you should add the name of a Beanshell
 * script to the property <code>splat.plugins</code>. Names are
 * separated by commas. Your Beanshell script will be sourced.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see Frog
 */
public class RemoteServer extends Thread
{
   /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();

    /**
     * The Beanshell interpreter.
     */
    protected Interpreter interp = null;

    /**
     * The ServerSocket.
     */
    protected ServerSocket serverSocket = null;

    /**
     * The instance of Frog that we're attached to.
     */
    protected Frog browserMain = null;

    /**
     * The hash code that we use as the verification cookie.
     */
    protected String cookie = null;

    /**
     *  Create an instance.
     */
    public RemoteServer( Frog browserMain )
    {
        super( "Remote Beanshell command server" );
        initInterpreter();
        setFrog( browserMain );
        loadStaticPlugins();
    }

    /**
     * Initialise the Beanshell interpreter. Keep hold of input and
     * output streams since we do not want anyone else to see any IO
     * from it.
     */
    protected void initInterpreter()
    {
        //  Create and initialise the interpreter. Allow I/O
        //  to appear on the terminal.
        byte[] dummyBuffer = new byte[1024];
        InputStreamReader in = new InputStreamReader( new
            ByteArrayInputStream( dummyBuffer ) );
        interp = new Interpreter( in, System.out, System.err, false );

        //  Import the "uk.ac.starlink.frog.iface.Frog" and
        //  classes. Need this so we can trivially refer to the
        //  "browser" variables in scripts.
        try {
            interp.eval( "import uk.ac.starlink.frog.Frog" ); 
        } 
        catch (Exception e) {
            // Do nothing, although this failure would be exceptional.
        }

        //  Initialize the socket, let it choose a free user port.
        try {
            serverSocket = new ServerSocket( 0 );
        } 
        catch ( IOException e ) {
            e.printStackTrace();
            return;
        }

        //  Write the contact details to a local file. This must be
        //  used to authenticate a connection.
        writeContactFile();
        debugManager.print( "    Got cookie..." );
        debugManager.print( "    Remote Control Established" );
    }

    /**
     * Execute any Beanshell scripts passed as plugins that should be
     * loaded at startup.
     */
    protected void loadStaticPlugins()
    {
        String plugins = System.getProperty( "frog.plugins", "" );
        if ( ! plugins.equals( "" ) ) {
            debugManager.print( "Beanshell plugins..." );
            StringTokenizer st = new StringTokenizer( plugins, "," );
            while ( st.hasMoreTokens() ) {
                loadPlugin( st.nextToken() );
            }
        }
    }

    /**
     * Load a Beanshell plugin.
     *
     * @param pluginFile the file containing Beanshell script commands.
     */
    public void loadPlugin( String pluginFile )
    {
        debugManager.print( "Loading: " + pluginFile );
        try {
            interp.source( pluginFile );
        } 
        catch ( Exception e ) {
            e.printStackTrace();
            debugManager.print( "Failed to load plugin in file:" + pluginFile);
        }
    }

    /**
     * Set the instance of Frog that we're attached to.
     */
    public void setFrog( Frog browserMain )
    {
        this.browserMain = browserMain;

        //  Establish the current Frog as the variable
        //  browser in the interpreter.
        try {
            interp.set( "browser", getFrog() );
        } 
        catch (Exception e) {
            //  Do nothing.
        }
    }

    /**
     * Get the instance of Frog that we're attached to.
     */
    public Frog getFrog()
    {
        return browserMain;
    }

    /**
     * Write contact details to a file only readable by the owner
     * process and privelegded local users. These details are used to
     * authenticate any requests.
     *
     * hostname port_number cookie
     */
    protected void writeContactFile()
    {
        cookie = RemoteUtilities.writeContactFile(serverSocket.getLocalPort());
    }

    /**
     * Start the process of waiting for remote requests.
     */
    public void run()
    {
        //  This thread loops forever, just accepting single requests
        //  for connections, followed by closure.
        while( true ) {
            Socket clientSocket = null;
            try {
                clientSocket = serverSocket.accept();
            } 
            catch ( IOException e ) {
                e.printStackTrace();
                continue;
            }

            //  Create input and output streams for the client socket.
            PrintWriter out = null;
            BufferedReader in = null;
            try {
                out = new PrintWriter( clientSocket.getOutputStream(), true );
                in = new BufferedReader
                    ( new InputStreamReader( clientSocket.getInputStream() ) );
            } 
            catch (Exception e ) {
                try {
                    clientSocket.close();
                } 
                catch (Exception ee) {
                    //  Do nothing.
                }
                continue;
            }

            //  The first line should be the cookie that we wrote to
            //  the protected contact file.
            String inputLine;
            String outputLine;
            try {
                inputLine = in.readLine();
            } 
            catch (Exception e) {
                e.printStackTrace();
                try {
                    out.close();
                    in.close();
                    clientSocket.close();
                } 
                catch (Exception ee) {
                    // Do nothing.
                }
                continue;
            }

            if ( ! cookie.equals( inputLine ) ) {
                debugManager.print( "Warning: remote access "+
                                    "verification failure (bad cookie)" );
                try {
                    out.close();
                    in.close();
                    clientSocket.close();
                } 
                catch (Exception e) {
                    // Do nothing.
                }
                continue;
            }

            //  Keep reading command-lines until the "bye" word is
            //  seen, or a remote disconnection is made.
            try {
                while ( (inputLine = in.readLine() ) != null ) {
                    if ( inputLine.equals( "bye" ) ) {
                        returnObject( out, "Connection closed" );
                        break;
                    }
                    try {
                        Object result = interp.eval( inputLine );
                        returnObject( out, result );
                    } 
                    catch (Exception e) {
                        returnObject( out, "Execution of your remote "+
                                      "command failed:" + e.getMessage() );
                    }
                }
            } 
            catch (Exception e) {
                e.printStackTrace();
            }

            // Incoming client closes own socket, but we're not
            // listening anymore...
            try {
                clientSocket.shutdownInput();
            } 
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Output an Object to a listening socket stream. This is encoded
     * as two lines, the first has a length specifier, the second the
     * Object converted into a string.
     */
    protected void returnObject( PrintWriter out, Object message )
    {
        if ( message != null ) {
            String encode = message.toString();
            out.println( encode.length() );
            out.println( encode );
        }
        else {
            // No object.
            out.println( 4 );
            out.println( "null" );
        }
    }

    /**
     *  When finalized close the server socket.
     */
    public void finalize()
    {
        try {
            serverSocket.close();
        } 
        catch (Exception e) {
            // Do nothing.
        }
    }
}
