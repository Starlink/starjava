package uk.ac.starlink.splat.vo;

import java.io.IOException;

import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.table.BeanStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

public class SLAPServerList extends AbstractServerList<SlapRegResource> {

    private static String configFile = "SLAPServerListV5.xml";
    private static String defaultFile = "slapserverlist.xml";

    public SLAPServerList() throws SplatException {
        super();
        restoreKnownServers();
    }
    public SLAPServerList(StarTable table)  
    {    
         super(table);      
    }
    
    protected void addNewServersToServerList(StarTable table ) {
    	
    	

        if ( table == null ) 
            return;

        // now add  the new ones
        if ( table instanceof BeanStarTable ) {
        	
            int nrserv = 0;
            Object[] resources = ( (BeanStarTable) table ).getData();
            for ( int i = 0; i < resources.length; i++ ) {

                SSAPRegResource auxsrv = (SSAPRegResource) resources[i];
                SlapRegResource server = new SlapRegResource( auxsrv );  
              
                String shortname = server.getShortName();
                if ( shortname == null || shortname.length() == 0 )
                    shortname = server.getTitle();

                SLAPRegCapability caps[] = server.getCapabilities();
                int nrcaps = caps.length;
                int nrssacaps = 0;
                
             
                for ( int c = 0; c < nrcaps; c++ ) {

                    SlapRegResource slapserver = new SlapRegResource( server );
                    SLAPRegCapability onecap[] = new SLAPRegCapability[1];
                    onecap[0] = caps[c];   // 
                    String name = shortname;
                    slapserver.setCapabilities( onecap );
                    if ( nrssacaps > 0 )
                        name = name + "(" + nrssacaps + ")";
                    slapserver.setShortName( name );
                    addServer( slapserver, false );
                    nrserv++;
                    nrssacaps++;
                }
            }
      
        }
        try {
            saveServers();
        } catch (SplatException e) {
            // do nothing
        }
        
    }
    
    @Override
    public String getConfigFile() {
        return configFile;
    }
    
    private static int findColumnIndex( StarTable table, String columnName )
    {
        for ( int i = 0; i < table.getColumnCount(); i++ ) {
            String name = table.getColumnInfo( i ).getName();
            if ( name.equalsIgnoreCase( columnName ) ) {
                return i;
            }
        }
        return -1;
    }
//    @Override
//    public String getDefaultFile() {
//        return defaultFile;
//    }
}
