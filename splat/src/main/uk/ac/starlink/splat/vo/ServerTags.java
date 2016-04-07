package uk.ac.starlink.splat.vo;
import java.util.ArrayList;



/**
 * ServerTags is a bean class to allow saving and restoring user defined server tags in SPLAT
 *
 * @author Margarida Castro Neves 
 * @version $Id: ServerTags.java 10350 2012-11-15 13:27:36Z mcneves $
 *
 */

public class ServerTags {
      
       private String tagname = null;
       private ArrayList<String> servers;
        
       public ServerTags() {
        // empty constructor
       }
        
       public ServerTags( String name, ArrayList<String> servers ) {
           this.tagname = name;
           this.servers = servers;
       }
       public String getTagname() {
           return this.tagname;
       }
       public void setTagname( String name ) {
            this.tagname = name;
       }
       public ArrayList<String> getServers() {
            return this.servers;
       }
       public void setServers( ArrayList<String> servers ) {
            this.servers = servers;
       }
       
}
