package gov.nasa.arc.dert.render;

import gov.nasa.arc.dert.scene.World;

import java.awt.EventQueue;

import com.ardor3d.framework.FrameHandler;
import com.ardor3d.image.util.awt.AWTImageLoader;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.renderer.jogl.JoglTextureRendererProvider;
import com.ardor3d.util.Timer;

/**
 * Provides continuous update of Ardor3D windows (SceneCanvasPanel).
 *
 */
public class SceneFramework {
	
	public static int millisBetweenFrames = 33;

	private static SceneFramework instance;

	// Handles frame update
	private FrameHandler frameHandler;

	// Flag to stop running
	private volatile boolean doit;

	// Count of queued events
	private int count = 0;
	
	// Framework is suspended
	private boolean suspended;
	

	// Execute a single update on the AWT event queue.
	private final Runnable runnable = new Runnable() {
		@Override
		public void run() {
			frameHandler.updateFrame();
			World.markClean();
			count--;
		}
	};

	/**
	 * Create the framework singleton
	 * 
	 * @return
	 */
	public static void createInstance() {
		instance = new SceneFramework();
	}

	/**
	 * Get the framework singleton
	 * 
	 * @return
	 */
	public static SceneFramework getInstance() {
		return (instance);
	}

	/**
	 * Constructor
	 */
	public SceneFramework() {
		frameHandler = new FrameHandler(new Timer());
		// initialize
		TextureRendererFactory.INSTANCE.setProvider(new JoglTextureRendererProvider());
		AWTImageLoader.registerLoader();
		startFrameHandlerUpdate(millisBetweenFrames);
	}

	/**
	 * Start the frame handler update
	 * 
	 * @param sleepTime
	 *            time to sleep between update events
	 */
	public void startFrameHandlerUpdate(int sleepTime) {
		doit = true;
		if (sleepTime <= 0) {
			sleepTime = millisBetweenFrames;
		}
		final int sleepyTime = sleepTime;
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				while (doit) {
					try {
						Thread.sleep(sleepyTime);
						if ((count < 3) && !suspended) {
							// single assignment should be thread safe - could
							// make count atomic
							count++;
							EventQueue.invokeLater(runnable);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
		t.start();
	}

	/**
	 * Stop updating
	 */
	public void stopFrameHandlerUpdate() {
		doit = false;
	}

	/**
	 * Get the frame handler
	 * 
	 * @return
	 */
	public FrameHandler getFrameHandler() {
		return (frameHandler);
	}
	
	public void suspend(boolean val) {
		suspended = val;
	}

}
