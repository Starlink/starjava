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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

import jsky.util.Logger;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.table.BeanStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.vo.RegResource;

/**
 * AbstractServer list contains the common methods for handling a list of services 
 * (SSAP, ObsCore, SLAP, VAMDC)
 * 
 * @author Margarida Castro Neves
 *
 */
/**
 * @author mm
 *
 */
public abstract class AbstractServerList {
    
    private HashMap<String, SSAPRegResource> serverList = new HashMap<String, SSAPRegResource>();
    
    /**
     * AbstractServerList
     * read the list from the configured file
     * 
     * @throws SplatException
     */
    protected AbstractServerList()
            throws SplatException
    {
        restoreKnownServers();
        
    }


    /**
     * AbstractServerList
     * create a list from a given StarTable
     * 
     * @throws SplatException
     */
    public AbstractServerList(StarTable table)  //throws SplatException
    {    
        addNewServers(table);      
    }

    
    
    abstract public String getConfigFile();
    //abstract public String getDefaultFile();

    
    /*
     * addNewServers
     * add contents of a startable to serverList
     *
     * @param table star table containing service resource information
     */

    public void addNewServers(StarTable table ) {

        if ( table == null ) 
            return;

        // now add  the new ones
        if ( table instanceof BeanStarTable ) {
            Object[] resources = ((BeanStarTable)table).getData();
            for ( int i = 0; i < resources.length; i++ ) {

                SSAPRegResource server = (SSAPRegResource)resources[i];
                String shortname = server.getShortName();
                if (shortname == null || shortname.length()==0)
                    shortname = server.getTitle(); // avoid problems if server has no name (should not happen, but it does!!!)
                SSAPRegCapability caps[] = server.getCapabilities();
                int nrcaps = server.getCapabilities().length;
                int nrssacaps=0;
                // create one serverlist entry for each ssap capability  !!!! 
                // TODO:  DO THIS ALREADY ON SSAPREGISTRYQUERY!?
                for (int c=0; c< nrcaps; c++) {

                    SSAPRegResource ssapserver = new SSAPRegResource(server);
                    SSAPRegCapability onecap[] = new SSAPRegCapability[1];
                    onecap[0] = caps[c];  
                    String name = shortname;
                    ssapserver.setCapabilities(onecap);
                    if (nrssacaps > 0) 
                        name =  name + "(" + nrssacaps + ")";
                    ssapserver.setShortName(name);
                    addServer( ssapserver, false );
                    nrssacaps++;
                }                    
            }
            try {
                saveServers();
            } catch (SplatException e) {
                // do nothing
            } 
        }
    }

    // add new servers - Before adding, remove all old entries except the manually added ones

    /**
     * AddNewServers
     * creates a new server list from table, but keeping the manually added services from the old list
     * @param table  contains services to be added to the list 
     * @param manuallyAddedServices the list of shortnames of the services that should be kept in the list
     */
    public void addNewServers(StarTable table, ArrayList<String> manuallyAddedServices) {


        HashMap<String, SSAPRegResource> newServerList = new HashMap<String, SSAPRegResource>();
        if (manuallyAddedServices != null) {

            for (int i=0;i<manuallyAddedServices.size(); i++) {
                String key=manuallyAddedServices.get(i);
                newServerList.put(key, serverList.get(key));
            }
            serverList.clear();
            serverList = newServerList;
        } else {
            serverList.clear();
        }
        addNewServers(table);

    }


    /**
     * Add a server resource to the known list, saving after adding.
     *
     * @param server an instance of RegResource.
     */
    public void addServer( SSAPRegResource server )
    {
        addServer( server, true );
    }

    /**
     * Add a server resource to the known list.
     *
     * @param server an instance of RegResource.
     * @param save if true then the backing store of servers should be updated.
     */
    protected void addServer( SSAPRegResource server, boolean save )
    {


        String shortname = server.getShortName();
        if (shortname != null)
            shortname = shortname.trim();
        else shortname = server.getTitle();
        SSAPRegResource resource = serverList.get(shortname);
        if (resource != null && ! resource.getIdentifier().equals( server.getIdentifier()) ) { // there could be more than one service with same shortname
            shortname=shortname+"+";
            server.setShortName(shortname);
        }
        serverList.put( shortname, server );
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
     * Remove a server from the known list, if already present.
     *
     * @param server an instance of RegResource.
     */
    public void removeServer( SSAPRegResource server )
    {
        String shortname = server.getShortName();
        if (shortname != null)
            shortname = shortname.trim();
        removeServer( shortname );

    }
    
    /**
     * Remove a server from the known list, if already present.
     *
     * @param the shortname of the server.
     */
    public void removeServer( String shortName )
    {
        if (shortName != null)
            shortName = shortName.trim();
        serverList.remove( shortName );
    }

    /**
     * Return an Iterator over the known servers. The objects iterated over
     * are instances of {@link RegResource}.
     */
    public Iterator<SSAPRegResource> getIterator()
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
     * Return the list as an array of {@link RegResource} in instance of
     * {@link BeanStarTable}. This can be used to populate a
     * {@link StarJTable}. Note once obtained no further modifications of the
     * table will be made, so the caller should arrange to synchronize as
     * necessary.
     */
    public BeanStarTable getBeanStarTable()
    {
        BeanStarTable table = null;
        try {
            table = new BeanStarTable( RegResource.class );
            table.setData( getData() );
        }
        catch ( java.beans.IntrospectionException e ) {
            //  Do nothing...
        }
        return table;
    }

    /**
     * Return an array of {@link RegResource} instances that describe the
     * current list of servers.
     */
    public SSAPRegResource[] getData()
    {
        SSAPRegResource[] rra = new SSAPRegResource[serverList.size()];
        rra = (SSAPRegResource[]) serverList.values().toArray( rra );
        return rra;
    }

    /**
     * Return a {@link SSAPRegResource} instance matching the short name given
     * current list of servers.
     * @param shortname
     */
    public SSAPRegResource getResource(String shortname)
    {
        if (shortname != null)
            shortname = shortname.trim();
        return (SSAPRegResource) serverList.get(shortname);
    }

    /**
     * Return a {@link SSAPRegResource} instance matching the short name given
     * current list of servers.
     * @param shortname
     */
    public String getBaseURL(String shortname)
    {
        if (shortname != null)
            shortname = shortname.trim();
        SSAPRegResource res = (SSAPRegResource) serverList.get(shortname);
        SSAPRegCapability[] cap = res.getCapabilities();
        return cap[0].getAccessUrl();
    } 

   

    /**
     * Initialize the known servers which are kept in a resource file along
     * with SPLAT. The format of this file is determined by the
     * {@link XMLEncode} form produced for an {@link SSAPRegResource}.
     */
    protected void restoreKnownServers()
            throws SplatException
    {
        //  Locate the description file. This may exist in the user's
        //  application specific directory or, the first time, as part of the
        //  application resources.
        //  User file first.
        File backingStore = Utilities.getConfigFile( getConfigFile() );
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
               // e.printStackTrace();
            }
        }

        //  If the restore of the user file failed, or it doesn't exist use
        //  the system default version. (only for ssa)
        if ( ! restored ) {
         //   inputStream = SSAServerList.class.getResourceAsStream(getDefaultFile());
        //    if ( inputStream == null ) {
                // That's bad. Need to complain, unless this is an update
                /// of the format. In which case skip this section.
               throw new SplatException( "Failed to find" +
                                         " a server listing file" );
               
        //    }  
        //    needSave = true;
         //   restoreServers( inputStream );
        //    try {
        //        inputStream.close();
        //    }
        //    catch (Exception e) {
        //        e.printStackTrace();
        //    }
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
        boolean ok = true;
        SSAPRegResource server = null;
        while ( true ) {
            try {
                server = (SSAPRegResource) decoder.readObject();
                addServer(server, false);

                // serverList.put( name, server );
                //selectionList.put(name, true );
            }
            catch( ArrayIndexOutOfBoundsException e ) {
                break; // End of list.
            }
            catch ( NoSuchElementException e ) {
                Logger.info(this, "Failed to read server list " +
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
    public void saveServers()
            throws SplatException
    {
        saveServers( Utilities.getConfigFile( getConfigFile() ) );
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
        Iterator<SSAPRegResource> i = serverList.values().iterator();

        //  Note these have to be SSAPRegResource instances, not RegResource.
        //  So that they can be serialised as beans.
        SSAPRegResource server = null;

        while ( i.hasNext() ) {
            server = (SSAPRegResource) i.next();
            try {
                SSAPRegResource resource = new SSAPRegResource( server );
                encoder.writeObject( resource );
                //   encoder.writeObject( resource.getMetadata());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        encoder.close();
    }

    
    /**
     * get the number of list elements
     * @return the list size
     */
    
    public int getSize() {
        return serverList.size();
    }

    /**
     * add new value to metadata list in servers
     * @param mip - the metadata parameter
     */
    public void addMetadata(MetadataInputParameter mip) {


        ArrayList<String> servers = (ArrayList<String>) mip.getServers(); // list of shortnames 
        for (int i=0;i<servers.size();i++) {
            SSAPRegResource srv = getResource(servers.get(i)); 
            ArrayList<MetadataInputParameter> srvmeta = (ArrayList<MetadataInputParameter>) srv.getMetadata();
            for (int j=0;j<srvmeta.size();j++) {
                MetadataInputParameter srvmip = srvmeta.get(j);
                if (srvmip.getName().equals(mip.getName())) {
                    srvmeta.set(j, mip);
                }
            }
            srv.setMetadata(srvmeta);
            serverList.put(servers.get(i), srv);
        }
    }

}
