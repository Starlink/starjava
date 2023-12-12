package uk.ac.starlink.splat.vo;

import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.table.StarTable;

public class LinetapServerList extends AbstractServerList {
	
	private  static final String configFile = "LINETAPServerListV4.xml";
    private final static String defaultFile = "LINETAPServerlist.xml";

	protected LinetapServerList() throws SplatException {
		super();
		restoreKnownServers();
	}

	public LinetapServerList(StarTable table) {
		super(table);
		
	}

	@Override
	public String getConfigFile() {
	
		return configFile;
	}
	
	 /**
     * Add a server resource to the known list, saving after adding.
     *
     * @param server an instance of RegResource.
     *
    public void addServer( SSAPRegResource server )
    {
    //	SSAPRegCapability[] caps = server.getCapabilities();
    	
    //	for (int i=0;i<caps.length;i++) {
    //		String accessUrl=caps[i].getAccessUrl();
    //		String table = server.getTableName(); 
    		
    //		
    //	}
    	
        addServer( server, true );
    }
    */
    /**
     * Add a server resource to the known list.
     *
     * @param server an instance of RegResource.
     * @param save if true then the backing store of servers should be updated.
     */
    protected void addServer( SSAPRegResource server, boolean save )
    {


        String shortname = server.getShortName();
        if (shortname != null && shortname != "")
            shortname = shortname.trim();
        else shortname = server.getTableName();
        SSAPRegResource resource = serverList.get(shortname); 
        if (resource != null ) { // check if there is already a resource with same shortname
            String ident = resource.getIdentifier();    
            if (ident != null && ident.equals( server.getIdentifier()) ) { // same identifier (other capability)     
                shortname=shortname+"+";
                server.setShortName(shortname);
            }
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


}
