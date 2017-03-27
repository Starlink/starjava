package uk.ac.starlink.splat.vo;

import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.table.StarTable;

public class VAMDCServerList extends AbstractServerList {

    
    private final static String configFile = "VAMDCServerListV4.xml";
    private final static String defaultFile = "VAMDCserverlist.xml";

    public VAMDCServerList( ) throws SplatException  {
        super();
    }
    
    public VAMDCServerList(StarTable table )  {
        super(table); 
    }

    @Override
    public String getConfigFile() {
        // TODO Auto-generated method stub
        return configFile;
    }

}
