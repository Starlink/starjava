/*
 * $Id: ManhattanRouter.java,v 1.7 2002/09/26 12:18:30 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.canvas.connector;

import diva.util.java2d.Polyline2D;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import javax.swing.SwingConstants;

/** An object that is used to route a single Manhattan connector.
 * This code is abstracted out into a separate class so that the
 * routing algorithm can be used in other applications. A route
 * consists of a series of <i>segments</i>, which can be created
 * based on the positions and direction of the start and endpoints,
 * and then manipulated to make a more complex route. For example,
 * a segment can be split, adding two new segments to the route.
 * Once the route is complete, a Polyline2D can be extracted from
 * it for the purpose of drawing a connector.
 *
 * <P> Each segment has a "direction." This is specified as a constant
 * from SwingUtilities, such as SwingUtilities.SOUTH, and indicates
 * the direction of the segment when proceeding from the start to the
 * end of the route.
 *
 * <p>Each segment in a route can be "pinned." This means that the
 * position of the segment (perpendicular to its direction) will not
 * be changed by any of the routing methods. Typically, if an interactor
 * moves a segment, it will then pin it so that it appears to "stay put."
 * The result is a reasonable compromise between automatic routing
 * and manual control.
 *
 * @version $Revision: 1.7 $
 * @author  Michael Shilman
 * @author John Reekie
 */
public class ManhattanRouter {

    private double TOL = .1;
    private double MINDIST = 12;

    /** The list of segments
     */
    private SegmentList _segments;

    /** The starting point
     */
    private Point2D _start;

    /** Create a new empty router.
     */
    public ManhattanRouter () {
	;
    }

    /** Add a new segment to the route. The initial
     * length of the segment is zero.
     */
    public void addSegment (int direction) {
	int ix = getSegmentCount();
	double x = getSegment(ix-1).x;
	double y = getSegment(ix-1).y;

	_segments.add(new Segment(direction, x, y, false));
    }

    /** Create a route based on the given start and end points, and the direction
     * leaving and arriving at those points. Return a list of segments.
     */
    SegmentList createRoute (Point2D start, int startDir, Point2D end, int endDir) {
	SegmentList segments = new SegmentList();

	Point2D point = start;
	int dir = startDir;
	boolean done = false;

	while (!done) {
	    double xDiff = end.getX() - point.getX();
	    double yDiff = end.getY() - point.getY();

	    // System.out.println("routing, diff=(" + xDiff + ", " + yDiff + ")");
	    //System.out.println("dir = " + dir);
	    //System.out.println("point = " + point);

	    int thisdir = dir;
	    switch (dir) {
	    case SwingConstants.EAST:
		//System.out.println("point is west");
		if(xDiff > 0 && yDiff * yDiff < TOL && endDir == SwingConstants.EAST) {
		    //System.out.println("completing straight");
		    point = end;
		    dir = endDir;
		    done = true;

		} else {
		    if (xDiff < 0) {
			//System.out.println("routing backwards");
			point = new Point2D.Double(point.getX() + MINDIST, point.getY());

		    } else if (yDiff > 0 && endDir == SwingConstants.SOUTH ||
			       yDiff < 0 && endDir == SwingConstants.NORTH) {
			//System.out.println("completing 90");
			point = new Point2D.Double(end.getX(), point.getY());

		    } else if (endDir == SwingConstants.WEST) {
			point = new Point2D.Double(end.getX() + MINDIST, point.getY());

		    } else {
			point = new Point2D.Double(point.getX() + xDiff/2, 
						   point.getY());
		    } 
		    if (yDiff > 0) {
			dir = SwingConstants.SOUTH;
		    } else {
			dir = SwingConstants.NORTH;
		    }
		}
		break;

	    case SwingConstants.WEST:
		//System.out.println("point is west");
		if (xDiff < 0 && yDiff * yDiff < TOL && endDir == SwingConstants.WEST) {
		    //System.out.println("completing");
		    point = end;
		    dir = endDir;
		    done = true;

		} else {
		    if (xDiff > 0) {
			//System.out.println("routing backwards");
			point = new Point2D.Double(point.getX() - MINDIST, point.getY());

		    } else if (yDiff > 0 && endDir == SwingConstants.SOUTH ||
			       yDiff < 0 && endDir == SwingConstants.NORTH) {
			//System.out.println("completing 90");
			point = new Point2D.Double(end.getX(), point.getY());

		    } else if (endDir == SwingConstants.EAST) {
			point = new Point2D.Double(end.getX() - MINDIST, point.getY());

		    } else {
			point = new Point2D.Double(point.getX() + xDiff/2, point.getY());
		    }

		    if (yDiff > 0) {
			dir = SwingConstants.SOUTH;
		    } else {
			dir = SwingConstants.NORTH;
		    }
		}
		break;

	    case SwingConstants.SOUTH:
		//System.out.println("SOUTH");
		if(xDiff * xDiff < TOL && yDiff > 0 && endDir == SwingConstants.SOUTH) {
		    //System.out.println("completing");
		    point = end;
		    dir = endDir;
		    done = true;

		} else {
		    if (yDiff < 0) {
			//System.out.println("routing backwards");
			point = new Point2D.Double(point.getX(),point.getY() + MINDIST);
			
		    } else if (xDiff > 0 && endDir == SwingConstants.EAST ||
			       xDiff < 0 && endDir == SwingConstants.WEST) {
			//System.out.println("completing 90");
			point = new Point2D.Double(point.getX(), end.getY());
			
		    } else if (endDir == SwingConstants.NORTH) {
			point = new Point2D.Double(point.getX(), end.getY() + MINDIST);
			
		    } else {
			point = new Point2D.Double(point.getX(), point.getY() + yDiff / 2);
		    }
		    
		    if (xDiff > 0) {
			dir = SwingConstants.EAST;
		    } else {
			dir = SwingConstants.WEST;
		    }
		}
		break;

	    case SwingConstants.NORTH:
		//System.out.println("point is south");
		if(xDiff * xDiff < TOL && yDiff < 0 && endDir == SwingConstants.NORTH) {
		    //System.out.println("completing");
		    point = end;
		    dir = endDir;
		    done = true;

		} else {
		    if (yDiff > 0) {
			//System.out.println("routing backwards");
			point = new Point2D.Double(point.getX(), point.getY() - MINDIST);
			
		    } else if (xDiff > 0 && endDir == SwingConstants.EAST ||
			       xDiff < 0 && endDir == SwingConstants.WEST) {
			//System.out.println("completing 90");
			point = new Point2D.Double(point.getX(), end.getY());
			
		    } else if (endDir == SwingConstants.SOUTH) {
			point = new Point2D.Double(point.getX(), end.getY() - MINDIST);
			
		    } else {
			point = new Point2D.Double(point.getX(), point.getY() + yDiff / 2);
		    } 
		    if (xDiff > 0) {
			dir = SwingConstants.EAST;
		    } else {
			dir = SwingConstants.WEST;
		    }
		}
	    }
	    Segment segment = new Segment (thisdir, point.getX(), point.getY(), false);
	    segments.add(segment);
	}
	return segments;
    }

    /** Create a Polyline2D that represents the shape of the current
     * route.
     */
    public Polyline2D createShape () {
	Polyline2D path = new Polyline2D.Double();
	double x = _start.getX();
	double y = _start.getY();
	path.moveTo(x, y);

	int count = getSegmentCount();
	for (int i = 0; i < count; i++) {
	    Segment seg = getSegment(i);
	    path.lineTo(seg.x, seg.y);
	}
	return path;
    }

    /** Delete a segment. The segment *and* it's following segment
     * will be deleted. In addition, the lengths of the segments
     * before and after the two deleted segments will be adjusted
     * accordingly. This method cannot be called on the first, last, or
     * next to last segment. It can also only be called if the preceding
     * and following segments have the same direction.
     */
    public void deleteSegment (int segment) {
	// Check here, cause this is a nasty one if not caught
	if (segment <= 0 || segment >= getSegmentCount()-2) {
	    throw new IllegalArgumentException("Cannot delete first, last, or next to last segment (" + segment + ")");
	}

	Segment here = getSegment(segment);
	Segment prev = getSegment(segment-1);
	Segment next = getSegment(segment+1);
	// Check this too
	if (prev.direction != next.direction) {
	    throw new IllegalArgumentException("Cannot delete segments if previous and next segments are not in the same direction");
	}

	if (here.direction == SwingConstants.NORTH
	    || here.direction == SwingConstants.SOUTH) {
	    prev.x = next.x;
	} else {
	    prev.y = next.y;
	}
	_segments.remove(segment);
	_segments.remove(segment+1);
    }

    /** Get the region covered by some number of segments. The returned
     * rectangle covers all segments between and including the given
     * first and last segments. It doesn't include the pixels used to
     * actually draw any graphical objects, so clients will want to
     * enlarge the returned rectangle if using it for damage regions.
     */
    public Rectangle2D getRegion (int first, int last) {
	return null; // FIXME
    }

    /** Get the segment with the given index
     */
    public Segment getSegment (int segment) {
	return (Segment) _segments.get(segment);
    }

    /** Get the number of segments
     */
    public int getSegmentCount () {
	return _segments.size();
    }

    /** Get the starting point
     */
    public Point2D getStart () {
	return _start;
    }

    /** Reroute the end section of the route. Specifically, segments
     * from the last pinned segment up to the end point may be rerouted.
     * New segments may be created if necessary.
     */
    public void translateEnd (double dx, double dy) {
	// Figure out where the last pinned segment is
	int i = getSegmentCount() - 2;
	if (i < 0) {
	    i = 0;
	} else {
	    while (!getSegment(i).pinned && i > 0) {
		i--;
	    }
	}
	//System.out.println("Last pinned = " + i);
	Point2D start, end;
	if (i == 0) {
	    start = _start;
	} else {
	    start = new Point2D.Double(getSegment(i-1).x, getSegment(i-1).y);
	}
	Segment lastseg = getSegment(getSegmentCount()-1);
	end = new Point2D.Double(lastseg.x + dx, lastseg.y + dy);

	// Call routing method to do the work
	SegmentList segs = createRoute(start, getSegment(i).direction,
				       end, lastseg.direction);
	_segments.removeSegments(i, getSegmentCount());
	_segments.addAll(segs);
	//System.out.println(this);
    }

    /** Translate and reroute the start section of the
     * route. Specifically, segments from the start up to the first
     * pinned segment may be rerouted.  New segments may be created if
     * necessary.
     */
    public void translateStart (double dx, double dy) {
	// Update start location
	_start = new Point2D.Double(_start.getX() + dx,
				    _start.getY() + dy);

	// Figure out where the first pinned segment is
	int i = 0;
	while (!getSegment(i).pinned && i < getSegmentCount()-1) {
	    i++;
	}
	Segment lastseg = getSegment(i);
	Point2D end = new Point2D.Double(lastseg.x, lastseg.y);

	// Call routing method to do the work
	SegmentList segs = createRoute(_start, getSegment(0).direction,
				       end, lastseg.direction);
	_segments.removeSegments(0, i+1);
	_segments.addAll(0, segs);
	//System.out.println(this);
     }

    /** Create a route based on the given start and end points, and the direction
     * leaving and arriving at those points.
     */
    public void route (Point2D start, int startDir, Point2D end, int endDir) {
	_start = start;
	_segments = createRoute(start, startDir, end, endDir);
	//System.out.println(this);
    }
    
    /** Set all segments of the route. This method is intended for
     * use by clients that are reconstructing a route from a
     * saved representation.
     */
    public void setSegments (SegmentList segments) {
	_segments = segments;
    }

    /** Split a segment. The referenced segment will be split in
     * half, thereby inserting two segments into the route: one
     * perpendicular to it of zero length, and another in the same
     * direction of half the length. A client will typically translate
     * one of the two half-segments immediately after this call.
     */
    public void splitSegment (int segment) {
	Segment here = getSegment(segment);
	double x = here.x;
	double y = here.y;
	double prevx, prevy;
	if (segment == 0) {
	    prevx = _start.getX();
	    prevy = _start.getY();
	} else {
	    prevx = getSegment(segment-1).x;
	    prevy = getSegment(segment-1).y;
	}

	int perp;
	if (here.direction == SwingConstants.NORTH
	    || here.direction == SwingConstants.SOUTH) {
	    perp = SwingConstants.EAST;
	    y = (prevy + y) / 2;
	} else {
	    perp = SwingConstants.NORTH;
	    x = (prevx + x) / 2;
	}
	Segment next = new Segment (perp, x, y, false);
	Segment nextnext = new Segment (here.direction, here.x, here.y, false);
	here.x = x;
	here.y = y;

	_segments.add(segment+1, next);
	_segments.add(segment+2, nextnext);
    }

    /** Print the route
     */
    public String toString () {
	StringBuffer str = new StringBuffer(super.toString());
	str.append(" (" + _start.getX() + "," + _start.getY() + ")");
	for (int i = 0; i < getSegmentCount(); i++) {
	    String d = "";
	    Segment s = getSegment(i);
	    switch (s.direction) {
	    case SwingConstants.NORTH:
		d = "n";
		break;
	    case SwingConstants.SOUTH:
		d = "s";
		break;
	    case SwingConstants.EAST:
		d = "e";
		break;
	    case SwingConstants.WEST:
		d = "w";
		break;
	    }
	    str.append(" ->" + d + " (" + s.x + "," + s.y + ")");
	}
	return str.toString();
    }

    /** Translate a segment by the given distance. Segments may be created
     * on either side if needed; in addition, segments on either side
     * may be deleted if this segment "lines up" with a free segment.
     * This method cannot be called on the first or last segments. One
     * of the x or y parameters will be ignored, depending on the
     * direction of the segment. Note that, as a result of calling this
     * method, the index of the segment being moved will change; not only
     * that, the Segment object will be replaced as well. So this method
     * returns the index of the "same" segment after it's been translated.
     *
     * <p> Often, an interactor that calls this method will also
     * pin the segment so that further calls to translateStart() or
     * translateEnd() do not overwrite the new segment location.
     */
    public int translateSegment (int segment, double x, double y) {
	Segment here = getSegment(segment);

	// Figure out where the last previous pinned segment is
	int lastpinned = segment-1;
	while (!getSegment(lastpinned).pinned && lastpinned > 0) {
	    lastpinned--;
	}
	// Figure out where the first later pinned segment is
	int firstpinned = segment+1;
	while (!getSegment(firstpinned).pinned && firstpinned < getSegmentCount()) {
	    firstpinned++;
	}
	// FIXME
	return -1;
    }

    /** Inner class representing a segment of the route. There is
     * a certain amount of redundancy in the representation (successive
     * segments have the same x or y value), doing it this way makes
     * the code a lot more understandable.
     */
    public static class Segment {
	/** The direction of this segment */
	public int direction;

	/** The X location of the end of the segment */
	public double x;

	/** The Y location of the end of the segment */
	public double y;

	/** The flag saying whether this segment is pinned */
	public boolean pinned;

	public Segment (int direction, double x, double y, boolean pinned) {
	    this.direction = direction;
	    this.x = x;
	    this.y = y;
	    this.pinned = pinned;
	}
    }

    /** Inner class for a list of segments.
     */
    public static class SegmentList extends ArrayList {
	public void removeSegments (int first, int last) {
	    removeRange(first, last);
	}
    }

    /** Test this thing...
     */
    public static void main (String argv[]) {
	Point2D start = new Point2D.Double(0,0);
	int startDir = SwingConstants.SOUTH;
	Point2D end = new Point2D.Double(100,100);
	int endDir = SwingConstants.SOUTH;
	
	ManhattanRouter mr = new ManhattanRouter();
	mr.route(start, startDir, end, endDir);
	System.out.println(mr);
    }
}
