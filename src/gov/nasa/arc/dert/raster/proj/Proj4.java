package gov.nasa.arc.dert.raster.proj;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Provides an interface to the Proj4 library.
 *
 */
public class Proj4 {

	// Load the native Proj4 library
	static {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("mac")) {
			loadNativeLibrary("/libgeo.jnilib");
		} else if (os.contains("lin")) {
			loadNativeLibrary("/libgeo.so");
		}
	}

	/**
	 * Load a native library.
	 * 
	 * @param libName
	 */
	protected static void loadNativeLibrary(String libName) {
		try {
			final InputStream in = Proj4.class.getResource(libName).openStream();
			int p0 = libName.lastIndexOf('/');
			int p1 = libName.lastIndexOf('.');
			String tempName = libName.substring(p0, p1) + '_' + System.currentTimeMillis();
			final File libFile = File.createTempFile(tempName, ".jni");
			libFile.deleteOnExit();
			final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(libFile));
			int len = 0;
			byte[] buffer = new byte[32768];
			while ((len = in.read(buffer)) > -1) {
				out.write(buffer, 0, len);
			}
			out.close();
			in.close();
			System.load(libFile.getAbsolutePath());
			libFile.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Proj4 native instance
	private final long handle;

	// The Proj4 definition for this instance
	private String projDef;

	// arrays for passing arguments to the native code
	private double[] xCoord = new double[1];
	private double[] yCoord = new double[1];
	private double[] zCoord = new double[1];

	/**
	 * Native method to create a native Proj4 instance with a definition
	 * 
	 * @param projDef
	 * @return
	 */
	protected static native long createProj4(String projDef);

	/**
	 * Native method to destroy a native Proj4 instance
	 * 
	 * @param address
	 */
	protected static native void destroyProj4(long address);

	/**
	 * Set the PROJ_LIB environment variable
	 * 
	 * @param path
	 */
	public static native void setProjPath(String path);

	/**
	 * Native method to transform a coordinate from one projection to another.
	 * 
	 * @param src
	 * @param dest
	 * @param pointCnt
	 * @param offset
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	protected native String transform(long src, long dest, long pointCnt, int offset, double[] x, double[] y, double[] z);

	/**
	 * Create a new instance of a Proj4 with a projection definition string
	 * 
	 * @param projDef
	 * @return
	 */
	public static Proj4 newInstance(String projDef) {
		return (new Proj4(projDef));
	}

	/**
	 * Constructor
	 * 
	 * @param projDef
	 */
	protected Proj4(String projDef) {
		handle = createProj4(projDef);
		if (handle == 0) {
			throw new IllegalArgumentException(projDef);
		}
		this.projDef = projDef;
	}

	/**
	 * Destroy the native resources
	 */
	@Override
	public void finalize() {
		destroyProj4(handle);
	}

	/**
	 * Calls pj_transform for the arguments. Lon/lat must be in radians.
	 * 
	 * @param dest
	 * @param pointCnt
	 * @param offset
	 * @param x
	 * @param y
	 * @param z
	 *            may be null
	 */
	public void transform(Proj4 dest, long pointCnt, int offset, double[] x, double[] y, double[] z) {
		if (dest == null) {
			throw new NullPointerException("Null destination");
		}
		if ((pointCnt < 1) || (offset < 0)) {
			throw new IllegalArgumentException("Invalid point count or offset");
		}
		if ((x == null) || (x.length < pointCnt) || (x.length < offset)) {
			throw new IllegalArgumentException("Invalid X coordinate array");
		}
		if ((y == null) || (y.length < pointCnt) || (y.length < offset)) {
			throw new IllegalArgumentException("Invalid Y coordinate array");
		}
		if ((z != null) && ((z.length < pointCnt) || (z.length < offset))) {
			throw new IllegalArgumentException("Invalid Z coordinate array");
		}

		String errStr = transform(handle, dest.handle, pointCnt, offset, x, y, z);
		if (errStr != null) {
			throw new IllegalStateException(errStr);
		}
	}

	/**
	 * Calls pj_transform for the coordinate triple. Lon/lat must be in radians.
	 * 
	 * @param dest
	 * @param coords
	 */
	public void transform(Proj4 dest, double[] coords) {
		if (dest == null) {
			throw new NullPointerException("Null destination");
		}
		if ((coords == null) || (coords.length < 3)) {
			throw new IllegalArgumentException("Null or too few elements for coordinates.");
		}
		xCoord[0] = coords[0];
		yCoord[0] = coords[1];
		zCoord[0] = coords[2];
		String errStr = transform(handle, dest.handle, 1, 0, xCoord, yCoord, zCoord);
		if (errStr == null) {
			coords[0] = xCoord[0];
			coords[1] = yCoord[0];
			coords[2] = zCoord[0];
		} else {
			throw new IllegalStateException(errStr);
		}
	}

}
