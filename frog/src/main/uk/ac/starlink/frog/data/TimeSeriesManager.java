package uk.ac.starlink.frog.data;

import java.util.HashMap;
import java.util.Set;
import java.util.Arrays;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import uk.ac.starlink.frog.Frog;
import uk.ac.starlink.frog.data.TimeSeries;
import uk.ac.starlink.frog.util.FrogDebug;
import uk.ac.starlink.frog.iface.PlotControlFrame;

/**
 * Single instance class to manage the TimeSeriesComp objects
 *
 * @since $Date$
 * @author Alasdair Allan
 * @version $Id$
 * @see TimeSeriesComp
 * @see TimeSeries
 * @see PlotControlFrame
 * @see "The Singleton Design Pattern"
  */
public class TimeSeriesManager
{
   /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();

   /**
     *  Create the single class instance.
     */
    private static final TimeSeriesManager instance = new TimeSeriesManager();

    /**
     *  Hash of TimeSeriesComp objects
     */
    protected HashMap seriesMap = new HashMap();
 
   /**
     *  Hash of PlotControlFrame objects
     */
    protected HashMap frameMap = new HashMap();

    /**
     * Reverse lookup table for PlotControlFrames
     */
    protected HashMap reverseMap = new HashMap();

    /**
     * Reverse lookup table for TimeSeriesComp
     */
    protected HashMap otherMap = new HashMap();
        
    /**
     * Reference to the main Frog object
     */ 
    protected Frog frogMain = null;
     
    
    /**
     * The ID number counter. Increments every time a series is loaded,
     * isn't decremented anywhere, so the number should be unique for
     * each series as it loads.
     */
     protected int seriesCounter = 0;

    /**
     * Do we display meta data automatically?
     */
    protected boolean autoDisplay = true;
         
    /**
     * Default constructor
     */
     private TimeSeriesManager()
     {
        // Do nothing
     }

    /**
     *  Return reference to the only allowed instance of this class.
     *
     *  @return reference to only instance of this class.
     */
    public static TimeSeriesManager getReference()
    {
        return instance;
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
     * Get the number of series indexed by the TimeSeriesManager object
     */
     public int getCount() 
     {
         return seriesMap.size();
     }  
       
    /**
     * Get the number of series (with fits) indexed by the manager
     */
     public int getFitCount() 
     {
         
         debugManager.print("             getFitCount()" );
         
         //
         int fitCount = 0;
         debugManager.print("               fitCount = " + fitCount );
         
         // grab a list of current keys in the manager
         Object [] timeSeriesList = this.getSeriesKeys();
         Arrays.sort(timeSeriesList);
      
         for ( int i=0; i < timeSeriesList.length; i++ ) {

             TimeSeriesComp seriesComp = this.getSeries( 
                                                  (String)timeSeriesList[i] );
             debugManager.print("               Checking " +
                                 (String)timeSeriesList[i]);
      
             // count them
             boolean fitFlag = false;
             for ( int k=0; k < seriesComp.count(); k++ ) {
             
                 debugManager.print("                 Series " + k + " of " +
                                 seriesComp.count() ); 
                 
                 TimeSeries thisSeries = seriesComp.get(k);
                 debugManager.print( "                 Series " + k +
                               " is of type " + thisSeries.getType() ); 
                               
                 if ( thisSeries.getType() == TimeSeries.SINCOSFIT ) {
                     fitFlag = true;
                     debugManager.print("                 Series "+ k + " of " +
                                 (seriesComp.count()-1) + " is a SINCOSFIT" );
                     break;
                 } else {
                     fitFlag = false;
                     debugManager.print("                 Series "+ k + " of " +
                                 (seriesComp.count()-1) + " is not a fit" );
                 }   
             }
             
             if( fitFlag ) {
                fitCount = fitCount + 1;
                fitFlag = false;         
                debugManager.print("               fitCount = " + fitCount );
             }   
         
          }
        
          return fitCount;
         
     } 
      
   /**
    * Return a unique ID for a series (hopefully)
    */
    public int getNextID()
    {
       return (seriesCounter + 1);
    }
    
  /**
    * Return a the last used ID for a series (hopefully)
    */
    public int getCurrentID()
    {
       return (seriesCounter);
    }
           
          
     
    /**
     * Return an array of the seriesMap keys
     */
     public Object [] getSeriesKeys()
     {
         Set keySet = seriesMap.keySet();
         return keySet.toArray(); 
     }
         
    /**
     * Get a TimeSeriesComp object by keyword
     *
     * @param key keyword to search
     * @return series time series referenced by the key
     */
     public TimeSeriesComp getSeries( String key )
     {
      //debugManager.print( "            TimeSeriesManager.getSeries("+key+")");
        return (TimeSeriesComp) seriesMap.get(key);
     }

   /**
     * Get a TimeSeriesComp object by PlotControlFram
     *
     * @param frame PlotControlFrame containing the TimeSeriesComp
     * @return series time series referenced by the key
     */
     public TimeSeriesComp getSeries( PlotControlFrame frame )
     {
        // Grab the shortname from the provided frame keyword
        String key = (String)reverseMap.get(frame);
        return (TimeSeriesComp) seriesMap.get(key);
 
     }   
        
    /**
     * Get a PlotControlFrame object by keyword
     *
     * @param key keyword to search
     * @return frame PlotControlFrame referenced by the key
     */
    public PlotControlFrame getFrame( String key )
    {
      //debugManager.print( "            TimeSeriesManager.getFrame("+key+")"); 
        return (PlotControlFrame) frameMap.get(key);
    } 

    /**
     * Get a PlotControlFrame object by TimeSeriesComp
     *
     * @param series TimeSeriesComp obejct
     * @return frame PlotControlFrame referenced by the key
     */
    public PlotControlFrame getFrame( TimeSeriesComp series )
    {
      //debugManager.print( "            TimeSeriesManager.getFrame("+key+")"); 
        String key = (String)otherMap.get(series);
        return (PlotControlFrame) frameMap.get(key);
    } 

    /**
     * Associate TimeSeriesComp and JInteralFrame objects with a keyword
     *
     * @param key keyword 
     * @param series TimeSeriesComp to associate with keyword
     * @param frame PlotControlFrame to associate with keyword
     */
    public void put( String key, TimeSeriesComp series, PlotControlFrame frame )
    {
      //debugManager.print( "            TimeSeriesManager.put()");     
        
        // build main hash maps
        frameMap.put( key, frame );
        seriesMap.put( key, series );
        
        // build reverse lookup hash
        reverseMap.put( frame, key );
        otherMap.put( series, key );
        
        // increment the seriesCounter
        seriesCounter = seriesCounter + 1;
    }

    /**
     * Return the series key when referenced by a PlotControlFrame 
     *
     * @param f PlotControlFrame containing a TimeSeriesComp object
     * @return key Unique lookup key for this object
     */
     public String getKey( PlotControlFrame frame ) 
     {
        return (String)reverseMap.get(frame);
        
     }

    /**
     * Return the series key when referenced by a PlotControlFrame 
     *
     * @param f a TimeSeriesComp object
     * @return key Unique lookup key for this object
     */
     public String getKey( TimeSeriesComp comp ) 
     {
        return (String)otherMap.get(comp);
        
     }
    
    /**
     * Remove TimeSeries and JInteralFrame objects with a frame
     *
     * @param fram PlotControlFrame
     */
    public void remove( PlotControlFrame frame )
    {
       //debugManager.print( "   TimeSeriesManager.remove()");     
        
        // Grab the shortname from the provided frame keyword
        String key = (String)reverseMap.get(frame);
        TimeSeriesComp comp = (TimeSeriesComp)seriesMap.get( key );
        
        // remove it from the reverse lookup hashs
        reverseMap.remove( frame );
        otherMap.remove( comp );
        
        // remove the series from the main maps
        frameMap.remove( key );
        seriesMap.remove( key );

        // toggle soem menu items
        getFrog().toggleMenuItemsOnSeriesUpdate();
        
        debugManager.print( "   Removing: "+key);     
       
    }    
     
    
    /**
     * Display a PlotControlFrame associated with a keyword
     * on a JDesktopPane
     *
     * @param key keyword with which a TimeSeries has been associated
     * @param desktop JDesktopPane to display the PlotControlFrame onto
     */
     public void display( String key, JDesktopPane desktop )
     {
        //debugManager.print( "            TimeSeriesManager.display()");     

          PlotControlFrame iframe = (PlotControlFrame) frameMap.get(key);
          desktop.add(iframe);
          iframe.setVisible(true);
       
          
     } 
     
     /** 
      * Duimp the current seriesMap to a string
      */
      public String dumpSeriesMap()
      {
           return seriesMap.toString();
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
