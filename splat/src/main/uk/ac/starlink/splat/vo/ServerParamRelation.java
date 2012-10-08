package uk.ac.starlink.splat.vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;


/**
 * Class for relating the list of SSAP servers with the parameters list.
 * 
 *
 * @author Margarida Castro Neves
 * @version $Id: ServerParameterRelation.java  2012-08-14 10:00:40Z mcneves $
 */
public class ServerParamRelation {

    static HashMap<String, ArrayList> server2param;
    static HashMap<String, ArrayList> param2server;
    private static Logger logger =
            Logger.getLogger( "uk.ac.starlink.splat.vo.ServerParamRelation" );
    
    /**
     * Constructor
     */    
    public ServerParamRelation() {
       
        server2param = new HashMap<String, ArrayList>();
        param2server = new HashMap<String, ArrayList>();
    }
    
    /**
     * adds one relation
     * @param   server  a ssap server
     * @param   parameter  a metadata parameter that the server understands
     */    
    public static synchronized void addRelation( String server, String param) {
       
        logger.info( "Add Relation " + server + ", "+param );
        if ( ! server2param.containsKey(server)) {
            ArrayList<String> list =  new ArrayList<String>();
            server2param.put(server, list);
        } 
        server2param.get(server).add(param);
        if ( ! param2server.containsKey(param)) {
            ArrayList<String> list =  new ArrayList<String>();
            param2server.put(param, list);
        } 
        param2server.get(param).add(server);
    } //addRelation
    
    /**
     * gets all servers using  parameter 
     * @param   param the parameter being consulted
     * @return  the list of servers that understand the parameter
     */    
    public ArrayList<String> getServers( String param ) {
        return (ArrayList<String>) param2server.get(param);
        
    }
    
    /**
     * gets all parameters understood by server
     * @param   server the SSAP server
     * @return  the list of parameters 
     */   
    public ArrayList<String> getParams( String server ) {
        return (ArrayList<String>) server2param.get(server);       
    }
    
}
