package uk.ac.starlink.frog.data;

import java.util.HashMap;
import java.util.Set;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import uk.ac.starlink.frog.Frog;
import uk.ac.starlink.frog.data.Gram;
import uk.ac.starlink.frog.util.FrogDebug;
import uk.ac.starlink.frog.iface.GramControlFrame;

/**
 * Single instance class to manage the GramComp objects
 *
 * @since $Date$
 * @author Alasdair Allan
 * @version $Id$
 * @see GramComp
 * @see Gram
 * @see GramControlFrame
 * @see "The Singleton Design Pattern"
  */
public class GramManager
{
   /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();

    
    /**
     * Reference to the main Frog object
     */ 
    protected Frog frogMain = null;
     
     
   /**
     *  Create the single class instance.
     */
    private static final GramManager instance = new GramManager();

    /**
     *  Hash of GramComp objects
     */
    protected HashMap gramMap = new HashMap();
 
   /**
     *  Hash of GramControlFrame objects
     */
    protected HashMap frameMap = new HashMap();

    /**
     * Reverse lookup table
     */
    protected HashMap reverseMap = new HashMap();
    
    /**
     * The ID number counter. Increments every time a periodogram
     * is loaded, isn't decremented anywhere, so the number should 
     * be unique for each periodogram as it loads.
     */
     protected int gramCounter = 0;
 
    /**
     * Do we display meta data automatically?
     */
    protected boolean autoDisplay = true;
        
    /**
     * Default constructor
     */
     private GramManager()
     {
        // Do nothing
     }

    /**
     *  Set the automatic display of meta-data popups
     */
     public void setAuto(boolean b) 
     {
         autoDisplay = b;
     } 
        
    /**
     *  Get the automatic display of meta-data popups
     */
     public boolean getAuto() 
     {
         return autoDisplay;
     }       
     
    /**
     * Get a reference to the instance of the main Frog class.
     */
     public Frog getFrog() 
     {
         return frogMain;
     }   
 
    /**
     * Set a reference to the instance of the main Frog class.
     */
     public void setFrog( Frog f) 
     {
         frogMain = f;
     }       
         
    /**
     *  Return reference to the only allowed instance of this class.
     *
     *  @return reference to only instance of this class.
     */
    public static GramManager getReference()
    {
        return instance;
    } 
     
    /**
     * Get the number of series indexed by the GramManager object
     */
     public int getCount() 
     {
         return gramMap.size();
     }  
     
   /**
    * Return a unique ID for a periodogram
    */
    public int getNextID()
    {
       return (gramCounter + 1);
    }   
  
  /**
    * Return a the last used ID for a series (hopefully)
    */
    public int getCurrentID()
    {
       return (gramCounter);
    }
                     
     
    /**
     * Return an array of the gramMap keys
     */
     public Object [] getGramKeys()
     {
         Set keySet = gramMap.keySet();
         return keySet.toArray(); 
     }
         
    /**
     * Get a GramComp object by keyword
     *
     * @param key Keyword to search
     * @return gram Periodogram referenced by the key
     */
     public GramComp getGram( String key )
     {
        return (GramComp) gramMap.get(key);
     }

   /**
     * Get a GramComp object by frame
     *
     * @param frame GramFrame containing the GramComp object
     * @return gram Periodogram referenced by the key
     */
     public GramComp getGram( GramControlFrame frame )
     {
        // Grab the shortname from the provided frame keyword
        String key = (String)reverseMap.get(frame);
        return (GramComp) gramMap.get(key);
 
     }   
        
    /**
     * Get a GramFrame object by keyword
     *
     * @param key keyword to search
     * @return frame GramControlFrame referenced by the key
     */
    public GramControlFrame getFrame( String key )
    {
        return (GramControlFrame) frameMap.get(key);
    } 

    /**
     * Associate GramComp and JInteralFrame objects with a keyword
     *
     * @param key keyword 
     * @param gram GramComp to associate with keyword
     * @param frame GramControlFrame to associate with keyword
     */
    public void put( String key, GramComp gram, GramControlFrame frame )
    {        
        // build main hash maps
        frameMap.put( key, frame );
        gramMap.put( key, gram );
        
        // build reverse lookup hash
        reverseMap.put( frame, key );
        
        // increment the gramCounter
        gramCounter = gramCounter + 1;
    }

    /**
     * Return the gram key when referenced by a GramControlFrame 
     *
     * @param f GramControlFrame containing a GramComp object
     * @return key Unique lookup key for this object
     */
     public String getKey( GramControlFrame frame ) 
     {
        return (String)reverseMap.get(frame);
        
     }  
       
    /**
     * Remove Gram and JInteralFrame objects with a keyword
     *
     * @param key keyword 
     */
    public void remove( GramControlFrame frame )
    {        
        // Grab the shortname from the provided frame keyword
        String key = (String)reverseMap.get(frame);
 
        // remove it from the reverse lookup hash
        reverseMap.remove( frame );       
        
        // remove the series from the main maps
        frameMap.remove( key );
        gramMap.remove( key );

        debugManager.print( "   Removing: "+key);     
       
    }    
     
    
    /**
     * Display a GramControlFrame associated with a keyword
     * on a JDesktopPane
     *
     * @param key keyword with which a Gram has been associated
     * @param desktop JDesktopPane to display the GramControlFrame onto
     */
     public void display( String key, JDesktopPane desktop )
     {
        //debugManager.print( "            TimeSeriesManager.display()");     

          GramControlFrame iframe = (GramControlFrame) frameMap.get(key);
          desktop.add(iframe);
          iframe.setVisible(true);
          
     } 
     
     /** 
      * Duimp the current gramMap to a string
      */
      public String dumpGramMap()
      {
           return gramMap.toString();
      }    
     
     /** 
      * Duimp the current frameMap to a string
      */
      public String dumpFrameMap()
      {
           return frameMap.toString();
      }    
      
     
     /** 
      * Duimp the current reverseMap to a string
      */
      public String dumpReverseMap()
      {
           return reverseMap.toString();
      }    
            
}
