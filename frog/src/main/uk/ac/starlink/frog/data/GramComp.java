package uk.ac.starlink.frog.data;

import java.util.ArrayList;

import uk.ac.starlink.frog.ast.AstUtilities;
import uk.ac.starlink.ast.Grf;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.frog.util.FrogException;
import uk.ac.starlink.frog.util.FrogDebug;

/**
 * This class is designed to handle multiple instances of Gram
 * objects. Thus creating an apparent "composite" gram, from
 * possibly several others.
 *
 * @since $Date$
 * @author Alasdair Allan
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 * @see Gram
 */
public class GramComp implements GramAccess 
{
   /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();


    /** 
     *  List of references to the gram. Note that indexing this
     *  list isn't fixed, i.e. removing Gram objects reshuffles
     *  the indices.  
     */
    protected ArrayList gram = new ArrayList();

    /**
     *
     */
    protected int currentGram;

    /**
     *  Create a GramComp instance.
     */
    public GramComp() 
    {
        //  Do nothing.
    }

    /**
     *  Create a GramComp adding in the first gram.
     */
    public GramComp( Gram ingram ) 
    {
        add( ingram );
        currentGram = gram.size() - 1;
        debugManager.print( "            GramComp( Gram )");
        debugManager.print( "              currentGram = " + currentGram );
    }

    /**
     *  Create a GramComp adding in the first from a concrete
     *  implementation.
     */
    public GramComp( GramImpl ingram ) throws FrogException 
    {
        add( new Gram( ingram ) );
        currentGram = gram.size() - 1;
        
        debugManager.print( "            GramComp( GramImpl )");
        debugManager.print( "              currentGram = " + currentGram );
    }

    /**
     *  Add a gram to the managed list.
     * 
     *  @param ingram reference to a Gram object that is to be
     *                added to the composite
     */
    public void add( Gram ingram ) 
    {
        gram.add( ingram );
    }

    /**
     *  Remove a gram.
     *
     *  @param ingram reference to the gram to remove.
     */
    public void remove( Gram ingram ) 
    {
        gram.remove( ingram );
        if ( currentGram >= gram.size() ) {
           currentGram = gram.size() - 1;
        }
    }

    /**
     *  Remove a gram.
     *
     *  @param index the index of the gram.
     */
    public void remove( int index ) 
    {
        gram.remove( index );
        if ( currentGram >= gram.size() ) {
           currentGram = gram.size() - 1;
        }
    }
    
    /**
     * Set gram which is the "current" gram
     *
     * @param index the index of the gram
     */
     public void setCurrentGram( int index )
     {
        currentGram = index;
     }
     
   /**
     * Get the "current" gram
     *
     */
     public int getCurrentGram( )
     {
        return currentGram;
     }     
     
   /**
     * Get the "current" gram
     *
     * @return gra, The current gram object
     */
     public Gram getGram( )
     {
        return (Gram) gram.get( currentGram );
     } 
          

    /**
     *  Get a reference to a gram.
     *
     *  @param index the index of the gram.
     */
    public Gram get( int index ) 
    {
        return (Gram) gram.get( index );
    }

    /**
     *  Get the index of a gram.
     *
     *  @param index the index of the gram.
     */
    public int indexOf( Gram ingram ) 
    {
        return gram.indexOf( ingram );
    }

    /**
     *  Get the number of gram currently being handled.
     */
    public int count() 
    {
        return gram.size();
    }

    /**
     *  Return if we already have a reference to a gram.
     */
    public boolean have( Gram s ) 
    {
        if ( gram.indexOf( s ) > -1 ) {
            return true;
        } else {
            return false;
        }
    }

   /**
     *  Set the plotting style of the current gram. The value should
     *  be one of the symbolic constants "POLYLINE" and "HISTOGRAM" or 
     *  "POINTS".
     *
     * @param style one of the symbolic contants Gram.POLYLINE or
     *      Gram.HISTOGRAM.
     */
     public void setPlotStyle( int style ) 
     {
          Gram tmp = (Gram)gram.get(currentGram);
          tmp.setPlotStyle( style );
     }     
          
   /**
     *  Set the marker style of the current gram. The value should
     *  be one of the symbolic constants faound in DefaultGrfMarker
     *
     * @param marker e.g. DefaultGrfMarker.FILLEDSQUARE
     */
     public void setMarkerStyle( int style ) 
     {
          Gram tmp = (Gram)gram.get(currentGram);
          tmp.setMarkStyle( style );
     }   
       
   /**
     *  Set the marker size of the current gram. 
     *
     * @param size default is 5.0
     */
     public void setMarkerStyle( double size ) 
     {
          Gram tmp = (Gram)gram.get(currentGram);
          tmp.setMarkSize( size );
     }   

     /**
     *  Find out wether the curretn plot has its error bars drawn
     *
     * @param state true to drawing errors, if possible
     */
     public boolean isDrawErrorBars( ) 
     {
          Gram tmp = (Gram)gram.get(currentGram);
          return tmp.isDrawErrorBars( );
     }   
          
     /**
     *  Set the current Gram to display error bars if available
     *
     * @param state true to draw errors, if possible
     */
     public void setDrawErrorBars( boolean state ) 
     {
          Gram tmp = (Gram)gram.get(currentGram);
          tmp.setDrawErrorBars( state );
     }   
        
       
    /** 
     *  Get reference to AstUtilities object set up to specify the coordinate
     *  system. This always returns the AstUtilities object of the first
     *  gram, so all other gram must have a context that is
     *  valid within the coordinate system defined by it.  
     */
    public AstUtilities getAst() 
    {
        return ((Gram)gram.get(0)).getAst();
    }

    /**
     *  Get a symbolic name for all gram.
     */
    public String getShortName() 
    {
        StringBuffer name = new StringBuffer( ((Gram)gram.get(0)).getShortName() );
        if ( gram.size() > 1 ) {
            name.append("(+").append(gram.size()-1).append(" others)");
        }
        return name.toString();
    }

    /**
     *  Get a full name for all gram. Blank.
     */
    public String getFullName() 
    {
        return "";
    }

    /**
     *  Get the symbolic name of a gram.
     */
    public String getShortName( int index ) 
    {
        return ((Gram)gram.get(index)).getShortName();
    }

    /**
     *  Get the full name of a gram.
     */
    public String getFullName( int index ) 
    {
        return ((Gram)gram.get(index)).getFullName();
    }

    /**
     *  Get the data range of all the gram
     */
    public double[] getRange() 
    {
        double[] range = new double[4];
        range[0] = Double.MAX_VALUE;
        range[1] = Double.MIN_VALUE;
        range[2] = Double.MAX_VALUE;
        range[3] = Double.MIN_VALUE;
        for ( int i = 0; i < gram.size(); i++ ) {
            double[] newrange = ((Gram)gram.get(i)).getRange();
            range[0] = Math.min( range[0], newrange[0] );
            range[1] = Math.max( range[1], newrange[1] );
            range[2] = Math.min( range[2], newrange[2] );
            range[3] = Math.max( range[3], newrange[3] );
        }
        return range;
    }

    /**
     *  Get the full data range of all the gram.
     */
    public double[] getFullRange() 
    {
        double[] range = new double[4];
        range[0] = Double.MAX_VALUE;
        range[1] = Double.MIN_VALUE;
        range[2] = Double.MAX_VALUE;
        range[3] = Double.MIN_VALUE;
        for ( int i = 0; i < gram.size(); i++ ) {
            double[] newrange = ((Gram)gram.get(i)).getFullRange();
            range[0] = Math.min( range[0], newrange[0] );
            range[1] = Math.max( range[1], newrange[1] );
            range[2] = Math.min( range[2], newrange[2] );
            range[3] = Math.max( range[3], newrange[3] );
        }
        return range;
    }

    /**
     * Get the data range of the gram, that should be used when
     * auto-ranging. Autoranging only uses gram marked for this
     * purpose, unless there are no allowable gram (in which case 
     * it would be bad to have no autorange). If errorbars are in use
     * then their range is also accomodated.
     */
    public double[] getAutoRange() 
    {
        double[] range = new double[4];
        range[0] = Double.MAX_VALUE;
        range[1] = Double.MIN_VALUE;
        range[2] = Double.MAX_VALUE;
        range[3] = Double.MIN_VALUE;
        Gram spec = null;
        int count = gram.size();
        int used = 0;
        double newrange[];
        for ( int i = 0; i < count; i++ ) {
            spec = (Gram)gram.get(i);
            if ( spec.isUseInAutoRanging() || count == 1 ) {
                if ( spec.isDrawErrorBars() ) {
                    newrange = spec.getFullRange();
                } 
                else {
                    newrange = spec.getRange();
                }
                range[0] = Math.min( range[0], newrange[0] );
                range[1] = Math.max( range[1], newrange[1] );
                range[2] = Math.min( range[2], newrange[2] );
                range[3] = Math.max( range[3], newrange[3] );
                used++;
            }
        }
        if ( used == 0 ) {
            range = getFullRange();
        }
        return range;
    }

    /**
     *  Draw all gram using the graphics context provided.
     */
    public void drawGram( Grf grf, Plot plot, double[] limits ) 
    {
        for ( int i = 0; i < gram.size(); i++ ) {
            ((Gram)gram.get(i)).drawGram( grf, plot, limits );
        }
    }

    /**
     *  Lookup the physical values (i.e. timestamp and data value)
     *  that correspond to a graphics X coordinate.
     *  <p>
     *  Note that this only works for first gram.
     *
     *  @param xg X graphics coordinate
     *  @param plot AST plot needed to transform graphics position
     *              into physical coordinates
     *  
     */
    public double[] lookup( int xg, Plot plot ) 
    {
        return ((Gram)gram.get(0)).lookup( xg, plot );
    }

    /**
     *  Lookup the physical values (i.e. timestamp and data value)
     *  that correspond to a graphics X coordinate, returned in
     *  formatted strings (could be hh:mm:ss.ss for instance).
     *  <p>
     *  Note that this only works for first gram.
     *
     *  @param xg X graphics coordinate
     *  @param plot AST plot needed to transform graphics position
     *              into physical coordinates
     *  
     */
    public String[] formatLookup( int xg, Plot plot ) 
    {
        return ((Gram)gram.get(0)).formatLookup( xg, plot );
    }

    /**
     *  Lookup interpolated physical values (i.e. timestamp and data value)
     *  that correspond to a graphics X coordinate, returned in
     *  formatted strings (could be hh:mm:ss.ss for instance).
     *  <p>
     *  Note that this only works for first gram.
     *
     *  @param xg X graphics coordinate
     *  @param plot AST plot needed to transform graphics position
     *              into physical coordinates
     *  
     */
    public String[] formatInterpolatedLookup( int xg, Plot plot ) 
    {
        return ((Gram)gram.get(0)).formatInterpolatedLookup( xg, plot );
    }

    /**
     * Convert a formatted value into a floating value coordinates
     * (the input could be hh:mm:ss.s, in which case we get back
     * suitable radians). 
     *
     *  @param axis the axis to use for formatting rules.
     *  @param plot AST plot that defines the coordinate formats.
     *  @param value the formatted value.
     *  @return the unformatted value.
     */
    public double unFormat( int axis, Plot plot, String value ) 
    {
        return ((Gram)gram.get(0)).unFormat( axis, plot, value );
    }

    /**
     *  Convert a floating point coordinate into a value formatted for
     *  a given axis.
     *
     *  @param axis the axis to use for formatting rules.
     *  @param plot AST plot that defines the coordinate formats.
     *  @param value the value.
     *  @return the formatted value.
     */
    public String format( int axis, Plot plot, double value ) 
    {
        return ((Gram)gram.get(0)).format( axis, plot, value );
    }

    /**
     *  Get the size of the gram (first only).
     */
    public int size() 
    {
        return ((Gram)gram.get(0)).size();
    }
    
    /**
     * Return a reference to this object
     *
     * @return refernce to this object
     */
     public GramComp getContainer()
     {
        return this;
     }
     
}
