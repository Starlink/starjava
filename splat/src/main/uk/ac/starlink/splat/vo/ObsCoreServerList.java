package uk.ac.starlink.splat.vo;

import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.table.StarTable;

public class ObsCoreServerList extends AbstractServerList {

    private static String configFile = "ObsCoreServerListV4.xml";
    private static String defaultFile = "obscoreserverlist.xml";

    public ObsCoreServerList() throws SplatException {
        super();
    }
    
    public ObsCoreServerList(StarTable table) {
        super(table);
    }
    
    @Override
    public String getConfigFile() {
        return configFile;
    }
    @Override
    public String getDefaultFile() {
        return defaultFile;
    }


}
