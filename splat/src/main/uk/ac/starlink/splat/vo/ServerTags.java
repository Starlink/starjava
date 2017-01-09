package uk.ac.starlink.splat.vo;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;



/**
 * ServerTagsis a bean class to allow saving and restoring user defined server tags in SPLAT 
 *
 * @author Margarida Castro Neves 
 * @version $Id: ServerTags.java 10350 2012-11-15 13:27:36Z mcneves $
 *
 */

public class ServerTags {
      
       private String tagsFile = "";

       
       /** the tags: one tag -> many servers **/
       
       Map<String,ArrayList<String>> tagsMap;
       
       /** the tags: one server -> many tags **/
       Map<String,ArrayList<String>> serverTagsMap;

    //  private ArrayList<String> servers;
        
       public ServerTags(String filename) {
           
           tagsFile = filename;
           
           tagsMap = new HashMap<String,ArrayList<String>>();
           serverTagsMap = new HashMap<String,ArrayList<String>>();     
                  
       }
        
      
    // returns a list  servers tagged by tagstr.
    public ArrayList<String> getList(String tagstr) {
        
        return tagsMap.get(tagstr);
    }
    
    /**
     * read saved tags from file
     */
    public void restoreTags()
        //throws SplatException
    {
        //  Locate the description file. This may exist in the user's
        //  application specific directory or, the first time, as part of the
        //  application resources.
        //  User file first.
        File backingStore = Utilities.getConfigFile( tagsFile );
        InputStream inputStream = null;
        if ( backingStore.canRead() ) {
            try {
                inputStream = new FileInputStream( backingStore );
            }
            catch (FileNotFoundException e) {  // should never happen because of canRead()
              // throw new SplatException("Tags file not found:"+backingStore);
            }
        }
     //   boolean restored = false;
        if ( inputStream != null ) {
            restoreTags( inputStream );
            try {
                inputStream.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        } 
    }  

  
    /**
     * Read an InputStream that contains a list of servers to restore.
     */
    protected void restoreTags( InputStream inputStream )
      //  throws SplatException
    {
        XMLDecoder decoder = new XMLDecoder( inputStream );
     //   boolean ok = true;
        //ServerTags st = null;           
    
        while ( true ) {
            try {
                Object ob =  decoder.readObject();
                //st = (ServerTags)  ob;
                tagsMap = (Map<String, ArrayList<String>>) ob;
            }
            catch( ArrayIndexOutOfBoundsException e ) {
                break; // End of list.
            }
            catch ( NoSuchElementException e ) {
                System.out.println( "Failed to read server list " +
                                    " (old-style or invalid):  '" +
                                    e.getMessage() + "'"  );
    //            ok = false;
                break;
            }
            serverTagsMap.clear();
            
            Set <String> tags = tagsMap.keySet();
          
           // tagCombo.addItem("");
            Iterator <String> it = tags.iterator();
            while (it.hasNext()) {
                String tag = it.next();
              //  tagCombo.addItem(tag);
                ArrayList<String> servers = tagsMap.get(tag);
                for (int i=0;i<servers.size();i++) {
                    String server = servers.get(i);
                    ArrayList<String> tagnames;
                    if (serverTagsMap.containsKey(server))
                        tagnames = serverTagsMap.get(server);
                    else 
                        tagnames = new ArrayList<String>();
                    tagnames.add(tag);
                    serverTagsMap.put(server, tagnames);
                }
                
            }
                
        }  
      
        decoder.close();
 
    }
   
    
    /**
     *  Save tag information to a file
     */
    public void save() throws SplatException { 
        
        File tagfile = Utilities.getConfigFile(tagsFile);
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream( tagfile );
        }
        catch (FileNotFoundException e) {
            throw new SplatException( e );
        }
        XMLEncoder encoder = new XMLEncoder( outputStream );
        encoder.writeObject(tagsMap);
        encoder.close();  
     
    }
    
    public void addTag(String shortname, String tagname) {
        
        ArrayList<String> servers;
        if (tagsMap.containsKey(tagname))  // add tag to reverse tags map
            servers =tagsMap.get(tagname);
        else
            servers = new ArrayList<String>();

        if (! servers.contains(shortname)) {
            servers.add(shortname);
        }
        tagsMap.put(tagname, servers);
        
        ArrayList<String> smap;
        if (serverTagsMap.containsKey(shortname))  // add tag to reverse tags map
            smap =serverTagsMap.get(shortname);
        else
            smap = new ArrayList<String>(); // create new entry in reverse tags map
        
        smap.add(tagname);
        serverTagsMap.put(shortname, smap);          
        
    }
    
    /**
     *  remove all entries of shortname from the map;
     */
    public void removeFromTags(String shortname) {
     
        ArrayList<String> tags= serverTagsMap.get(shortname);
        if (tags==null)
            return;
        for (int i=0;i<tags.size();i++) {
            ArrayList<String> servers = tagsMap.get(tags.get(i));
            servers.remove(shortname);
        }
    }
    
    /**
     *  Remove  tag
     */
    public void removeTag(String tagname)
    {
    //    tagsListModel.removeElement(tagname);
        ArrayList<String> st = tagsMap.get(tagname);
        tagsMap.remove(tagname);
        
        for (int i=0;i<st.size();i++) { //remove from reverse tags map
            ArrayList<String> ts = serverTagsMap.get(st.get(i));
            if (ts.contains(tagname) )
                    ts.remove(tagname);
            serverTagsMap.put(st.get(i), ts);
        }
        
    }
    
    public String [] getTags() {
        Set<String> keys = tagsMap.keySet();
        return (String[]) keys.toArray(new String[keys.size()]);    
    }
    
    public String [] getTags(String server) {

        ArrayList<String> tagsList = serverTagsMap.get(server);
        if (tagsList == null)
            return null;
        String[] tagsArray = new String[tagsList.size()];
        return tagsList.toArray(tagsArray);      
    }

       
}
