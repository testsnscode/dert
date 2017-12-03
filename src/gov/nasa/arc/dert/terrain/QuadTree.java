package gov.nasa.arc.dert.terrain;

import gov.nasa.arc.dert.landscape.Landscape;
import gov.nasa.arc.dert.scene.World;
import gov.nasa.arc.dert.viewpoint.BasicCamera;

import java.nio.FloatBuffer;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Node;

/**
 * A tile in the quad tree structure representing the landscape. This object
 * contains a mesh that is displayed in the landscape, and a pointer to 4
 * children QuadTrees. If the children are in use, the mesh is not displayed. It
 * maintains copies of the edge vertices of the mesh to use for stitching, and a
 * timestamp. A set of test points is used to determine if a split or merge is
 * needed. It also keeps track of it neighbors for stitching purposes.
 *
 */
public class QuadTree extends Node {

	// The minimum number of pixels for a mesh cell size before loading a higher
	// resolution quad tree
	public static int CELL_SIZE = 4;

	// Side of a quad tree
	protected static enum Side {
		Left, Right, Top, Bottom
	}

	// This quad tree is in use
	protected boolean inUse;

	// Last time this quad tree was pulled from the cache
	protected long timestamp;

	// The mesh that will be rendered
	protected QuadTreeMesh mesh;

	// The level in the pyramid and the quadrant in the parent quad tree
	protected int level, quadrant;

	// This quad tree is at the highest resolution
	protected boolean highestLevel;

	// The next level of quad trees
	protected QuadTree[] child;

	// Sides of the quad tree that need stitching
	private boolean leftDirty, rightDirty, bottomDirty, topDirty;

	// The quad tree's neighbors
	private QuadTree left, top, right, bottom;

	// Fields used for determining if quad tree should be merged or split
	private Vector3 camLoc, lookAt, closest;

	// Points in the quad tree used to determine if it should be merged or split
	// Coordinates are relative to the landscape center
	private Vector3[] cornerPoint;
	private Vector3 centerPoint;
//
//	// Copy of edge vertices for stitching
//	private float[][] edge;
//
//	// Copy of edge normals for stitching
//	private Vector3[][] nrml;

	// Fields used for interpolation of edge normals.
	private Vector3 n0, n1, n2;

	// Pixel dimensions
	protected double pixelWidth, pixelLength;
	
	// Size of this quad tree object in bytes
	protected int sizeInBytes;

	/**
	 * Constructor
	 * 
	 * @param name
	 * @param p
	 *            Translation point relative to the parent quad tree
	 * @param pixelWidth
	 * @param tileWidth
	 * @param pixelLength
	 * @param tileLength
	 * @param level
	 * @param quadrant
	 */
	public QuadTree(String name, ReadOnlyVector3 p, int level, int quadrant, double pixelWidth, double pixelLength, int sizeInBytes) {
		super(name);
		camLoc = new Vector3();
		lookAt = new Vector3();
		closest = new Vector3();
		n0 = new Vector3();
		n1 = new Vector3();
		n2 = new Vector3();

		this.level = level;
		this.quadrant = quadrant;
		this.pixelWidth = pixelWidth;
		this.pixelLength = pixelLength;
		this.sizeInBytes = sizeInBytes;
		setTranslation(p);
	}

	/**
	 * Create the test points for this quad tree. They will include the 4
	 * corners and p. The coordinates are relative to the center of the
	 * landscape.
	 * 
	 * @param p
	 */
	public synchronized void createCornerPoints(ReadOnlyVector3 p, double tileWidth, double tileLength) {
		double width = tileWidth * pixelWidth / 2;
		double length = tileLength * pixelLength / 2;
		// create test points
		cornerPoint = new Vector3[4];
		cornerPoint[0] = new Vector3(p.getX() - width, p.getY() - length, p.getZ());
		cornerPoint[1] = new Vector3(p.getX() + width, p.getY() - length, p.getZ());
		cornerPoint[2] = new Vector3(p.getX() + width, p.getY() + length, p.getZ());
		cornerPoint[3] = new Vector3(p.getX() - width, p.getY() + length, p.getZ());
		centerPoint = new Vector3(p);
	}

	/**
	 * Set the mesh for this QuadTree
	 * 
	 * @param mesh
	 */
	public synchronized void setMesh(QuadTreeMesh mesh) {
		this.mesh = mesh;
		attachChild(mesh);
		updateGeometricState(0);
		int tileWidth = mesh.getTileWidth();
		int tileLength = mesh.getTileLength();
		int tWidth = tileWidth + 1;

		// update test point Z values
		FloatBuffer vertexBuffer = mesh.getMeshData().getVertexBuffer();
		if (mesh.isEmpty()) {
			for (int i = 0; i < cornerPoint.length; ++i)
				cornerPoint[i].setZ(vertexBuffer.get(2));
			centerPoint.setZ(vertexBuffer.get(2));
		} else {
			double minZ = Landscape.getInstance().getMinimumElevation();
			// lower left
			cornerPoint[0].setZ(vertexBuffer.get(tileLength * tWidth * 3 + 2) - minZ);
			// lower right
			cornerPoint[1].setZ(vertexBuffer.get((tileLength * tWidth + tileWidth) * 3 + 2) - minZ);
			// upper right
			cornerPoint[2].setZ(vertexBuffer.get(tileWidth * 3 + 2) - minZ);
			// upper left
			cornerPoint[3].setZ(vertexBuffer.get(2) - minZ);
			// center
			centerPoint.setZ(vertexBuffer.get((tWidth * tileLength / 2 + tileWidth / 2) * 3 + 2) - minZ);
		}
	}

	/**
	 * Get the mesh
	 * 
	 * @return
	 */
	public synchronized QuadTreeMesh getMesh() {
		return (mesh);
	}
	
	public int getSize() {
		return(sizeInBytes);
	}

	/**
	 * Does this quad tree contain the coordinate X,Y?
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public final boolean contains(double x, double y) {
		if (cornerPoint == null) {
			return (false);
		}
		if (x < cornerPoint[0].getX()) {
			return (false);
		}
		if (x > cornerPoint[2].getX()) {
			return (false);
		}
		if (y < cornerPoint[0].getY()) {
			return (false);
		}
		if (y > cornerPoint[2].getY()) {
			return (false);
		}
		return (true);
	}

	/**
	 * Set the Quad Tree neighbors
	 * 
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 */
	public void setNeighbors(QuadTree left, QuadTree top, QuadTree right, QuadTree bottom) {
		this.left = left;
		this.top = top;
		this.right = right;
		this.bottom = bottom;
	}

	/**
	 * Stitch a quadtree to the given side.
	 * 
	 * @param side
	 *            the side where the quadtree is located
	 * @param that
	 *            the quadtree
	 */
	private void doStitch(Side side, QuadTree that) {
		// this and that are on the same level, fill in their own elevation
		// values
		if (this.level == that.level) {
			int tileWidth = mesh.getTileWidth();
			int tileLength = mesh.getTileLength();
			switch (side) {
			case Left:
				mesh.setElevationColumn(0, mesh.getEdge(0));
				mesh.setNormalsColumn(0, mesh.getNrml(0));
				that.mesh.setElevationColumn(tileWidth, that.mesh.getEdge(2));
				that.mesh.setNormalsColumn(tileWidth, mesh.getNrml(0));
				break;
			case Top:
				mesh.setElevationRow(0, mesh.getEdge(1));
				mesh.setNormalsRow(0, mesh.getNrml(1));
				that.mesh.setElevationRow(tileLength, that.mesh.getEdge(3));
				that.mesh.setNormalsRow(tileLength, mesh.getNrml(1));
				break;
			case Right:
				mesh.setElevationColumn(tileWidth, mesh.getEdge(2));
				mesh.setNormalsColumn(tileWidth, that.mesh.getNrml(0));
				that.mesh.setElevationColumn(0, that.mesh.getEdge(0));
				that.mesh.setNormalsColumn(0, that.mesh.getNrml(0));
				break;
			case Bottom:
				mesh.setElevationRow(tileLength, mesh.getEdge(3));
				mesh.setNormalsRow(tileLength, that.mesh.getNrml(1));
				that.mesh.setElevationRow(0, that.mesh.getEdge(1));
				that.mesh.setNormalsRow(0, that.mesh.getNrml(1));
				break;
			}
		}
		// this is higher resolution than that
		// find the ends and do the stitching
		else if (this.level > that.level) {
			double[] e = findStitchEnds(this, side, that);
			fillEdge(side, that, e);
		}
		// this is lower or equal resolution to that,
		// shouldn't happen
		else {
			throw new IllegalStateException("Passed in quadtree is higher resolution than stitching quadtree.");
		}
	}

	private void stitch(Side side, QuadTree that) {
		// drill down
		if (child != null) {
			switch (side) {
			case Left:
				if (isStitchable(child[0])) {
					child[0].stitch(Side.Left, that);
				}
				if (isStitchable(child[2])) {
					child[2].stitch(Side.Left, that);
				}
				break;
			case Top:
				if (isStitchable(child[0])) {
					child[0].stitch(Side.Top, that);
				}
				if (isStitchable(child[1])) {
					child[1].stitch(Side.Top, that);
				}
				break;
			case Right:
				if (isStitchable(child[1])) {
					child[1].stitch(Side.Right, that);
				}
				if (isStitchable(child[3])) {
					child[3].stitch(Side.Right, that);
				}
				break;
			case Bottom:
				if (isStitchable(child[2])) {
					child[2].stitch(Side.Bottom, that);
				}
				if (isStitchable(child[3])) {
					child[3].stitch(Side.Bottom, that);
				}
				break;
			}
		}
		// Stop here and actually do the stitching.
		else {
			switch (side) {
			case Left:
				if (leftDirty || that.rightDirty) {
					doStitch(side, that);
					leftDirty = false;
				}
				break;
			case Top:
				if (topDirty || that.bottomDirty) {
					doStitch(side, that);
					topDirty = false;
				}
				break;
			case Right:
				if (rightDirty || that.leftDirty) {
					doStitch(side, that);
					rightDirty = false;
				}
				break;
			case Bottom:
				if (bottomDirty || that.topDirty) {
					doStitch(side, that);
					bottomDirty = false;
				}
				break;
			}
		}
	}

	private void split() {
		// we are not at the highest resolution
		if (!highestLevel) {
			final QuadTree[] qt = new QuadTree[4];
			QuadTreeFactory factory = Landscape.getInstance().getFactory();
			// get the children
			int count = factory.loadQuadTrees(getName(), this, qt, false);
			// now at the highest resolution
			if (count < 0) {
				highestLevel = true;
			} else if (count == 4) {
				setChildren(qt);
				for (int i = 0; i < 4; ++i) {
					World.getInstance().getMarble().landscapeChanged(child[i]);
					World.getInstance().getLandmarks().landscapeChanged(child[i]);
					World.getInstance().getFeatureSets().landscapeChanged(child[i]);
				}

				updateGeometricState(0, true);
			}
		}
	}

	private synchronized void setChildren(QuadTree[] qt) {
		child = qt;
		detachChild(mesh);
		for (int i = 0; i < child.length; ++i) {
			child[i].leftDirty = true;
			child[i].rightDirty = true;
			child[i].bottomDirty = true;
			child[i].topDirty = true;
			attachChild(child[i]);
		}
	}

	/**
	 * Remove all children from this QuadTree and re-attach its mesh.
	 */
	private synchronized void clearChildren() {
		if (child == null) {
			return;
		}
		for (int i = 0; i < child.length; ++i) {
			child[i].clearChildren();
			detachChild(child[i]);
			child[i].inUse = false;
			child[i].leftDirty = false;
			child[i].rightDirty = false;
			child[i].bottomDirty = false;
			child[i].topDirty = false;
			child[i] = null;
		}
		child = null;
		attachChild(mesh);
		leftDirty = true;
		rightDirty = true;
		bottomDirty = true;
		topDirty = true;
	}

	/**
	 * Stitch this QuadTree to its neighbors at the given level
	 * 
	 * @param stitchLevel
	 */
	public void stitch(int stitchLevel) {
		if (child == null) {
			if (stitchLevel == level) {
				stitchSides();
			}
		} else if (stitchLevel > level) {
			for (int i = 0; i < child.length; ++i) {
				child[i].stitch(stitchLevel);
			}
		}
	}

	/**
	 * Get the neighbor on each side of this quadtree and stitch it.
	 */
	private void stitchSides() {
		if (!isStitchable(this)) {
			return;
		}
		QuadTree qt = null;
		qt = getNeighbor(Side.Left, level, quadrant);
		if (isStitchable(qt) && (qt.level == level)) {
			qt.stitch(Side.Right, this);
			leftDirty = false;
		}
		qt = getNeighbor(Side.Top, level, quadrant);
		if (isStitchable(qt) && (qt.level == level)) {
			qt.stitch(Side.Bottom, this);
			topDirty = false;
		}
		qt = getNeighbor(Side.Right, level, quadrant);
		if (isStitchable(qt) && (qt.level == level)) {
			qt.stitch(Side.Left, this);
			rightDirty = false;
		}
		qt = getNeighbor(Side.Bottom, level, quadrant);
		if (isStitchable(qt) && (qt.level == level)) {
			qt.stitch(Side.Top, this);
			bottomDirty = false;
		}
	}

	private boolean isStitchable(QuadTree qt) {
		if (qt == null) {
			return (false);
		}
		if (qt.mesh == null) {
			return (false);
		}
		if (qt.mesh.isEmpty()) {
			return (false);
		}
		return (true);
	}

	private void merge() {
		clearChildren();
		World.getInstance().getMarble().landscapeChanged(this);
		World.getInstance().getLandmarks().landscapeChanged(this);
		World.getInstance().getFeatureSets().landscapeChanged(this);
	}

	/**
	 * Determine if this QuadTree needs to be merged or split
	 * 
	 * @param camera
	 * @param return true if changed
	 */
	public boolean update(final BasicCamera camera) {
		if (!inUse) {
			return(false);
		}
		
		boolean isCulled = camera.isCulled(this);

		Vector3[] tPoint = getCornerPoints();

		if (tPoint == null) {
			return(false);
		}

		// find test point or camera lookAt point that is closest to the camera
		double minDist = Double.MAX_VALUE;
		camLoc.set(camera.getLocation());
		lookAt.set(camera.getLookAt());
		if (contains(lookAt.getX(), lookAt.getY())) {
			minDist = camLoc.distance(lookAt);
			closest.set(lookAt);
		}
		if (camLoc.distance(centerPoint) < minDist)
			closest.set(centerPoint);

		// get the pixel size at the closest point
		double pixSize = camera.getPixelSizeAt(closest, true);
		if (pixSize <= 0)
			return(false);

		// Mesh cells should be larger than a single pixel.
		pixSize *= CELL_SIZE;
		
		boolean changed = false;

		// greater than the pixel size of this tile, we can go to a coarser
		// resolution by merging
		if ((pixSize >= pixelWidth) || isCulled) {
			// only merge if we have split
			if (child != null) {
				merge();
				changed = true;
			}
		}

		// less than the pixel size of this tile, we need to go to a higher
		// resolution by splitting
		else if (pixSize <= pixelWidth / 2) {
			// only split if we haven't already
			if (child == null) {
				split();
				changed = true;
			} else {
				for (int i = 0; i < child.length; ++i) {
					changed |= child[i].update(camera);
				}
			}
		}

		// let children update
		else if (child != null) {
			for (int i = 0; i < child.length; ++i) {
				changed |= child[i].update(camera);
			}
		}
		return(changed);
	}

	private final QuadTree getNeighbor(Side side) {
		switch (side) {
		case Left:
			return (left);
		case Top:
			return (top);
		case Right:
			return (right);
		case Bottom:
			return (bottom);
		}
		throw new IllegalStateException("Unknown side " + side);
	}

	/**
	 * Given the side, level, and quadrant, get the neighboring QuadTree.
	 * 
	 * @param side
	 *            side
	 * @param l
	 *            level
	 * @param q
	 *            quadrant
	 * @return
	 */
	private QuadTree getNeighbor(Side side, int l, int q) {
		// get sibling
		QuadTree n = getNeighbor(side);
		// no sibling on that side, get parent's sibling
		if (n == null) {
			Node p = getParent();
			if (!(p instanceof QuadTree)) {
				return (null);
			}
			n = ((QuadTree) p).getNeighbor(side, l, quadrant);
		}
		if (n == null) {
			return (n);
		}
		if (n.level == l) {
			return (n);
		}
		if (n.level < l) { // lower res, see if any children are higher level
			if (n.child == null) {
				return (n);
			} else { // look at the quadrant passed in to determine the child of
						// n
				switch (side) {
				case Left:
					if (q == 0) {
						return (n.child[1]);
					} else if (q == 2) {
						return (n.child[3]);
					} else {
						throw new IllegalStateException("Quadrant = " + q + " for left");
					}
				case Top:
					if (q == 0) {
						return (n.child[2]);
					} else if (q == 1) {
						return (n.child[3]);
					} else {
						throw new IllegalStateException("Quadrant = " + q + " for top");
					}
				case Right:
					if (q == 1) {
						return (n.child[0]);
					} else if (q == 3) {
						return (n.child[2]);
					} else {
						throw new IllegalStateException("Quadrant = " + q + " for right");
					}
				case Bottom:
					if (q == 2) {
						return (n.child[0]);
					} else if (q == 3) {
						return (n.child[1]);
					} else {
						throw new IllegalStateException("Quadrant = " + q + " for bottom");
					}
				}
			}
		}
		throw new IllegalStateException("Level " + n.level + " is higher than " + l);
	}

	/**
	 * Dispose of any resources
	 */
	public void dispose() {
//		System.err.println("QuadTree.dispose "+getName());
		inUse = false;
		if (mesh != null)
			mesh.dispose();
		mesh = null;
	}

	/**
	 * Get the center of the QuadTree
	 * 
	 * @return
	 */
	public ReadOnlyVector3 getCenter() {
		return (getWorldTranslation());
	}

	/**
	 * Get the test points
	 * 
	 * @return
	 */
	public Vector3[] getCornerPoints() {
		return (cornerPoint);
	}

	/**
	 * Get the elevation at the given coordinates using bilinear interpolation.
	 * 
	 * @param x
	 * @param y
	 * @return NaN if outside this QuadTree
	 */
	public synchronized float getElevation(double x, double y) {
		if (child != null) {
			for (int i = 0; i < child.length; ++i) {
				if (child[i].contains(x, y)) {
					return (child[i].getElevation(x, y));
				}
			}
		} else {
			return (mesh.getElevationBilinear(x - cornerPoint[0].getX(), y - cornerPoint[0].getY()));
		}
		return (Float.NaN);
	}

	/**
	 * Get the elevation using nearest neighbor interpolation.
	 * 
	 * @param x
	 * @param y
	 * @return NaN, if outside this QuadTree
	 */
	public synchronized float getElevationNearestNeighbor(double x, double y) {
		if (child != null) {
			for (int i = 0; i < child.length; ++i) {
				if (child[i].contains(x, y)) {
					return (child[i].getElevationNearestNeighbor(x, y));
				}
			}
		} else {
			return (mesh.getElevationNearestNeighbor(x - cornerPoint[0].getX(), y - cornerPoint[0].getY()));
		}
		return (Float.NaN);
	}

	/**
	 * Get the normal at the given X,Y coordinate
	 * 
	 * @param x
	 * @param y
	 * @param store
	 * @return
	 */
	public synchronized boolean getNormal(double x, double y, Vector3 store) {
		if (child != null) {
			for (int i = 0; i < child.length; ++i) {
				if (child[i].contains(x, y)) {
					return (child[i].getNormal(x, y, store));
				}
			}
		} else {
			return (mesh.getNormal((int) Math.floor((x - cornerPoint[0].getX()) / pixelWidth),
					mesh.getTileLength()-(int)Math.floor((y - cornerPoint[0].getY()) / pixelLength), store));
		}
		return (false);
	}

	private void fillEdge(Side side, QuadTree that, double[] e) {
		if (mesh == null) {
			return;
		}
		int ib = (int) e[0];
		int ie = (int) e[1];
		if (ie == 0) {
			System.err.println("QuadTreeMesh.fillEdge fill count = 0 for " + getName());
			return;
		}
		int tileWidth = mesh.getTileWidth();
		int tileLength = mesh.getTileLength();
		int tWidth = tileWidth + 1;
		int tLength = tileLength + 1;
		int nw = tileWidth / ie;
		int nh = tileLength / ie;
		int i = ib;
		float ww = 1.0f / nw;
		float wh = 1.0f / nh;
		float[] dataEdge;
		Vector3[] dataNrml;
		switch (side) {
		case Left:
			dataEdge = that.mesh.getEdge(2);
			dataNrml = mesh.getNrml(0);
			for (int j = 0; j < tLength; j += nh) {
				mesh.setElevation(0, j, dataEdge[i]);
				that.mesh.setNormal(tileWidth, i, dataNrml[j]);
				i++;
			}
			for (int j = 0; j < tileLength; j += nh) {
				float el0 = mesh.getElevation(0, j);
				mesh.getNormal(0, j, n0);
				float el1 = mesh.getElevation(0, j + nh) - el0;
				mesh.getNormal(0, j + nh, n1);
				for (int k = 1; k < nh; ++k) {
					float el = el0 + k * wh * el1;
					interpolateNormal(n0, n1, n2, k * wh);
					mesh.setElevation(0, j + k, el);
					mesh.setNormal(0, j + k, n2);
				}
			}
			break;
		case Top:
			dataEdge = that.mesh.getEdge(3);
			dataNrml = mesh.getNrml(1);
			for (int j = 0; j < tWidth; j += nw) {
				mesh.setElevation(j, 0, dataEdge[i]);
				that.mesh.setNormal(i, tileLength, dataNrml[j]);
				i++;
			}
			for (int j = 0; j < tileWidth; j += nw) {
				float el0 = mesh.getElevation(j, 0);
				mesh.getNormal(j, 0, n0);
				float el1 = mesh.getElevation(j + nw, 0) - el0;
				mesh.getNormal(j + nw, 0, n1);
				for (int k = 1; k < nw; ++k) {
					float el = el0 + k * ww * el1;
					interpolateNormal(n0, n1, n2, k * ww);
					mesh.setElevation(j + k, 0, el);
					mesh.setNormal(j + k, 0, n2);
				}
			}
			break;
		case Right:
			dataEdge = that.mesh.getEdge(0);
			dataNrml = that.mesh.getNrml(0);
			for (int j = 0; j < tLength; j += nh) {
				mesh.setElevation(tileWidth, j, dataEdge[i]);
				mesh.setNormal(tileWidth, j, dataNrml[i]);
				i++;
			}
			for (int j = 0; j < tileLength; j += nh) {
				float el0 = mesh.getElevation(tileWidth, j);
				mesh.getNormal(tileWidth, j, n0);
				float el1 = mesh.getElevation(tileWidth, j + nh) - el0;
				mesh.getNormal(tileWidth, j + nh, n1);
				for (int k = 1; k < nh; ++k) {
					float el = el0 + k * wh * el1;
					interpolateNormal(n0, n1, n2, k * wh);
					mesh.setElevation(tileWidth, j + k, el);
					mesh.setNormal(tileWidth, j + k, n2);
				}
			}
			break;
		case Bottom:
			dataEdge = that.mesh.getEdge(1);
			dataNrml = that.mesh.getNrml(1);
			for (int j = 0; j < tWidth; j += nw) {
				mesh.setElevation(j, tileLength, dataEdge[i]);
				mesh.setNormal(j, tileLength, dataNrml[i]);
				i++;
			}
			for (int j = 0; j < tileWidth; j += nw) {
				float el0 = mesh.getElevation(j, tileLength);
				mesh.getNormal(j, tileLength, n0);
				float el1 = mesh.getElevation(j + nw, tileLength) - el0;
				mesh.getNormal(j + nw, tileLength, n1);
				for (int k = 1; k < nw; ++k) {
					float el = el0 + k * ww * el1;
					interpolateNormal(n0, n1, n2, k * ww);
					mesh.setElevation(j + k, tileLength, el);
					mesh.setNormal(j + k, tileLength, n2);
				}
			}
			break;
		}
	}

	private void interpolateNormal(Vector3 n0, Vector3 n1, Vector3 result, float weight) {
		result.set(n0);
		result.subtractLocal(n1);
		result.multiplyLocal(weight);
		result.addLocal(n1);
	}

	private double[] findStitchEnds(QuadTree qt0, Side side, QuadTree qt1) {
		QuadTree qt = qt0;
		int tileWidth = mesh.getTileWidth();
		int tileLength = mesh.getTileLength();

		// pixel step
		double n = Math.pow(2, qt.level - qt1.level);
		// start point
		double j = 0;
		// end point
		double k = 0;
		switch (side) {
		case Left:
		case Right:
			k = tileLength / n;
			break;
		case Top:
		case Bottom:
			k = tileWidth / n;
			break;
		}
		while (qt.level > qt1.level) {
			n = Math.pow(2, qt.level - qt1.level);
			switch (side) {
			case Left:
				if (qt.quadrant == 2) {
					j += tileLength / n;
				}
				break;
			case Top:
				if (qt.quadrant == 1) {
					j += tileWidth / n;
				}
				break;
			case Right:
				if (qt.quadrant == 3) {
					j += tileLength / n;
				}
				break;
			case Bottom:
				if (qt.quadrant == 3) {
					j += tileWidth / n;
				}
				break;
			}
			qt = (QuadTree) qt.getParent();
		}
		return (new double[] { j, k });
	}

}
