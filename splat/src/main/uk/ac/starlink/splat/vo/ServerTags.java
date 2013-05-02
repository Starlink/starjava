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
      
       private String name = null;
       private ArrayList<String> tags;
        
       public ServerTags() {
        // empty constructor
       }
        
       public ServerTags( String name, ArrayList<String> tags ) {
           this.name = name;
           this.tags = tags;
       }
       public String getName() {
           return this.name;
       }
       public void setName( String name ) {
            this.name = name;
       }
       public ArrayList<String> getTags() {
            return this.tags;
       }
       public void setTags( ArrayList<String> tags ) {
            this.tags = tags;
       }
       
}
