package uk.ac.starlink.jaiutil;

/*
 * ESO Archive
 * $Id$
 * 
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/11/16  created
 */

import java.awt.image.Raster;
import java.util.LinkedList;
import java.util.Iterator;
import java.awt.Point;

/**
 * Implements the JAI TileCache interface.
 *
 * @version $Revision$
 * @author Allan Brighton
 */
public class MyTileCache {

    /** Max amout of memory to cache */
    private int memoryCapacity;

    /** Max number of tiles to cache */
    private int tileCapacity;

    /** Two dimensional array of tiles, some of which may be null */
    private Raster[][] tiles;

    /** Number of tiles in the X axis */
    private int numXTiles;

    /** Number of tiles in the Y axis */
    private int numYTiles;

    /** List of tile indexes (tileX,tileY), in order of age */
    private LinkedList tileIndexes = new LinkedList();

    /** Number of tiles cached so far */
    private int numTiles = 0;

  
    /**
     * Create a new tile cache for an image with the given size.
     *
     * @param imageWidth The width of the image in pixels
     * @param imageHeight The height of the image in pixels
     * @param tileWidth The width of a tile in pixels
     * @param tileHeight The height of a tile in pixels
     * @param bytesPerPixel The number of bytes required for one pixel
     * @param memoryCapacity The maximum number of bytes to allocate before discarding old tiles
     */
    public MyTileCache(int imageWidth, int imageHeight, int tileWidth, int tileHeight, 
		       int bytesPerPixel, int memoryCapacity) {
	this.memoryCapacity = memoryCapacity;

	// number of tiles we can keep in cache
	tileCapacity = memoryCapacity/(tileWidth*tileHeight*bytesPerPixel);
	if (tileCapacity == 0)
	    tileCapacity = 1;

	// Create slots for all tiles (a slot may be empty)
	numXTiles = imageWidth/tileWidth + 1;
	numYTiles = imageHeight/tileHeight + 1;
	tiles = new Raster[numXTiles][numYTiles];
    }


    /**
     * Create a new tile cache for an image with the given size.
     * The maximum memory usage is set to 64MB.
     *
     * @param imageWidth The width of the image in pixels
     * @param imageHeight The height of the image in pixels
     * @param tileWidth The width of a tile in pixels
     * @param tileHeight The height of a tile in pixels
     * @param bytesPerPixel The number of bytes required for one pixel
     */
    public MyTileCache(int imageWidth, int imageHeight, int tileWidth, int tileHeight, 
		       int bytesPerPixel) {
	this(imageWidth, imageHeight, tileWidth, tileHeight, bytesPerPixel, 1024*1024*20);
    }


    /**
     * Adds a tile to the cache.
     *
     * @param tileX The X index of the tile in the tile grid.
     * @param tileY The Y index of the tile in the tile grid.
     * @param tile A <code>Raster</code> containging the tile data.
     */
    public void add(int tileX, int tileY, Raster tile) {
	if (numTiles >= tileCapacity) {
	    // remove oldest tile
	    Point p = (Point)tileIndexes.removeFirst();
	    tiles[(int)p.getX()][(int)p.getY()] = null;
	    numTiles--;
	}
	tiles[tileX][tileY] = tile;
	tileIndexes.add(new Point(tileX, tileY));
	numTiles++;
    }
    

    /**
     * Advises the cache that a tile is no longer needed.  It is legal
     * to implement this method as a no-op.
     *
     * @param tileX The X index of the tile in the tile grid.
     * @param tileY The Y index of the tile in the tile grid.
     */
    public void remove(int tileX, int tileY) {
	tiles[tileX][tileY] = null;
	if (tileIndexes.remove(new Point(tileX, tileY)))
	    numTiles--;
    }

    
    /**
     * Clear the tile cache, so that the memory may be reclaimed
     */
    public void clear() {
	Iterator it = ((LinkedList)tileIndexes.clone()).iterator();
	while(it.hasNext()) {
	    Point p = (Point)it.next();
	    remove((int)p.getX(), (int)p.getY());
	}
	System.gc();
    }
    
    
    /**
     * Retrieves a tile.  Returns <code>null</code> if the tile is not
     * present in the cache.
     *
     * @param tileX The X index of the tile in the tile grid.
     * @param tileY The Y index of the tile in the tile grid.
     */
    public Raster getTile(int tileX, int tileY) {
	return tiles[tileX][tileY];
    }
    
    
    /**
     * Returns the tile capacity in tiles.
     */
    public int getTileCapacity() {
	return tileCapacity;
    }
    

    /**
     * Returns the memory capacity in bytes.
     */
    public long getMemoryCapacity() {
	return memoryCapacity;
    }
    
}
