/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     11-NOV-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.vo;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.us_vo.www.SimpleResource;

import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;

import uk.ac.starlink.table.BeanStarTable;

/**
 * Container for a list of possible Simple Spectral Access Protocol (SSAP)
 * servers that can be used. Each server is represented by a
 * {@link SimpleResource}. The primary source of these should be a query to a
 * VO registry.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SSAServerList
{
    private HashMap serverList = new HashMap();
    private static String configFile = "SSAPServerListV2.xml";
    private static String defaultFile = "serverlist.xml";

    public SSAServerList()
        throws SplatException
    {
        restoreKnownServers();
    }

    /**
     * Add an SSA server to the known list.
     *
     * @param description a human readable description of the service.
     * @param baseURL the URL for accessing the service (including trailing ?).
     *
     * @throws MalFormedURLException is the URL is invalid.
     */
    public void addServer( String description, String baseURL )
    {
        addServer( description, baseURL, true );
    }

    /**
     * Add an SSA server to the known list.
     *
     * @param description a human readable description of the service.
     * @param baseURL the URL for accessing the service (including trailing ?).
     * @param save if true then the backing store of servers should be updated.
     *
     * @throws MalFormedURLException is the URL is invalid.
     */
    protected void addServer( String description, String baseURL,
                              boolean save )
    {
        SimpleResource sr = new SimpleResource();
        sr.setShortName( description );
        sr.setServiceURL( baseURL );
        addServer( sr, save );
    }

    /**
     * Add an SSA server to the known list.
     *
     * @param server an instance of SimpleResource.
     */
    public void addServer( SimpleResource server )
    {
        addServer( server, true );
    }

    /**
     * Add an SSA server resource to the known list.
     *
     * @param server an instance of SimpleResource.
     * @param save if true then the backing store of servers should be updated.
     */
    protected void addServer( SimpleResource server, boolean save )
    {
        serverList.put( server.getShortName(), server );
        if ( save ) {
            try {
                saveServers();
            }
            catch (SplatException e) {
                //  Do nothing, it's not fatal.
            }
        }
    }

    /**
     * Remove an SSA server from the known list, if already present.
     *
     * @param server an instance of SimpleResource.
     */
    public void removeServer( SimpleResource server )
    {
        serverList.remove( server.getShortName() );
    }



    /**
     * Return an Iterator over the known servers. The objects iterated over
     * are instances of {@link SSAServer}.
     */
    public Iterator getIterator()
    {
        return serverList.values().iterator();
    }

    /**
     * Clear the server list.
     */
    public void clear()
    {
        serverList.clear();
    }

    /**
     * Return the list as an array of {@link SimpleResource} in instance of
     * {@link BeanStarTable}. This can be used to populate a
     * {@link StarJTable}. Note once obtained no further modifications of the
     * table will be made, so the caller should arrange to synchronize as
     * necessary.
     */
    public BeanStarTable getBeanStarTable()
    {
        BeanStarTable table = null;
        try {
            table = new BeanStarTable( SimpleResource.class );
            table.setData( getData() );
        }
        catch ( java.beans.IntrospectionException e ) {
            //  Do nothing...
        }
        return table;
    }

    /**
     * Return an array of {@link SimpleResource} instances that describe the
     * current list of servers.
     */
    public SimpleResource[] getData()
    {
        SimpleResource[] sra = new SimpleResource[serverList.size()];
        sra = (SimpleResource[]) serverList.values().toArray( sra );
        return sra;
    }

    /**
     * Initialise the known servers which are kept in a resource file along
     * with SPLAT. The format of this file is determined by the
     * {@link XMLEncode} form produced for a {@link SimpleResource}.
     */
    protected void restoreKnownServers()
        throws SplatException
    {
        //  Locate the description file. This may exist in the user's
        //  application specific directory or, the first time, as part of the
        //  application resources. 
        //  User file first.
        File backingStore = Utilities.getConfigFile( configFile );
        InputStream inputStream = null;
        boolean needSave = false;
        if ( backingStore.canRead() ) {
            try {
                inputStream = new FileInputStream( backingStore );
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        boolean restored = false;
        if ( inputStream != null ) {
            restored = restoreServers( inputStream );
            try {
                inputStream.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        //  If the restore of the user file failed, or it doesn't exist use
        //  the system default version.
        if ( ! restored ) {
            inputStream = SSAServerList.class.getResourceAsStream(defaultFile);
            if ( inputStream == null ) {
                // That's bad. Need to complain.
                throw new SplatException( "Internal error: Failed to find" +
                                          " the builtin SSAP server listing" );
            }
            needSave = true;
            restoreServers( inputStream );
            try {
                inputStream.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Save the current state back to disk if we're using the default list.
        if ( needSave ) {
            saveServers();
        }
    }

    /**
     * Add a series of servers stored in an XML file. The format of this file
     * is the same as that of the backing store version.
     */
    public void restoreServers( File inputFile )
        throws SplatException
    {
        InputStream inputStream = null;
        boolean needSave = false;
        if ( inputFile.canRead() ) {
            try {
                inputStream = new FileInputStream( inputFile );
            }
            catch (FileNotFoundException e) {
                throw new SplatException( e );
            }
        }
        else {
            throw new SplatException( "Server listing file '" + inputFile +
                                      "' does not exist" );
        }

        // Read the stream.
        try {
            restoreServers( inputStream );
            inputStream.close();
        }
        catch (Exception e) {
            throw new SplatException( e );
        }

        // Save the current state to backing store.
        saveServers();
    }


    /**
     * Read an InputStream that contains a list of servers to restore.
     */
    protected boolean restoreServers( InputStream inputStream )
        throws SplatException
    {
        XMLDecoder decoder = new XMLDecoder( inputStream );
        SimpleResource server = null;
        boolean ok = true;
        while ( true ) {
            try {
                server = (SimpleResource) decoder.readObject();
                serverList.put( server.getShortName(), server );
            }
            catch( ArrayIndexOutOfBoundsException e ) {
                break; // End of list.
            }
            catch ( NoSuchElementException e ) {
                System.out.println( "Failed to read server list " +
                                    " (old-style or invalid):  '" +
                                    e.getMessage() + "'"  );
                ok = false;
                break;
            }
        }
        decoder.close();
        return ok;
    }


    /**
     * Save the current list of servers to the backing store configuration
     * file.
     */
    protected void saveServers()
        throws SplatException
    {
        saveServers( Utilities.getConfigFile( configFile ) );
    }

    /**
     * Save the current list of servers to a named file.
     */
    public void saveServers( File file )
        throws SplatException
    {
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream( file );
        }
        catch (FileNotFoundException e) {
            throw new SplatException( e );
        }
        saveServers( outputStream );
        try {
            outputStream.close();
        }
        catch (IOException e) {
            throw new SplatException( e );
        }
    }

    /**
     * Write the current server list to an OutputStream.
     */
    protected void saveServers( OutputStream outputStream )
        throws SplatException
    {
        XMLEncoder encoder = new XMLEncoder( outputStream );
        Iterator i = serverList.values().iterator();
        SimpleResource server = null;
        while ( i.hasNext() ) {
            server = (SimpleResource) i.next();
            encoder.writeObject( server );
        }
        encoder.close();
    }
}
