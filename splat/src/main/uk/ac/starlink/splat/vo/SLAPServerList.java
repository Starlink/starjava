package uk.ac.starlink.splat.vo;

import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.table.StarTable;

public class SLAPServerList extends AbstractServerList {

    private static String configFile = "SLAPServerListV4.xml";
    private static String defaultFile = "slapserverlist.xml";

    public SLAPServerList() throws SplatException {
        super();
    }
    public SLAPServerList(StarTable table)  
    {    
         super(table);      
    }
    
    @Override
    public String getConfigFile() {
        return configFile;
    }
//    @Override
//    public String getDefaultFile() {
//        return defaultFile;
//    }
}
