package water;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Scanner;

import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.*;
import org.lwjgl.opencl.api.CLBufferRegion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.CL11.*;

import terrain.ASCTerrain;
import terrain.Terrain;

public class Water extends Node {
	long ticks_last;
	// Group particles by the row they occupy.
	Cell[] grid;
	Terrain terrain;
	int ncells;
	// OpenCL variables
	public static CLContext context;
	public static CLPlatform platform;
	public static List<CLDevice> devices;
	public static CLCommandQueue queue;
	public static CLKernel calcFlow;
	public static CLKernel calcHeight;
	
	// https://github.com/LWJGL/lwjgl/blob/master/src/java/org/lwjgl/test/opencl/HelloOpenCL.java
	public Water(Terrain t, Material mat) {
		super.setName("Water Body");
		// Params
		ticks_last = System.currentTimeMillis();
		terrain = t;
		grid = terrain.makeCells();
		ncells = grid.length;
		System.out.println("Adding water cells to scene...");
		for (int c = 0; c < ncells; c++) {
			grid[c].setMaterial(mat);
			attachChild(grid[c]);
		}
		// OpenCL
		try {
			CL.create();
			IntBuffer errorBuf = BufferUtils.createIntBuffer(1);
			final List<CLPlatform> platforms = CLPlatform.getPlatforms();
			if (platforms != null) {
				platform = CLPlatform.getPlatforms().get(0); 
				// Run our program on the GPU
				devices = platform.getDevices(CL10.CL_DEVICE_TYPE_GPU);
				// Create an OpenCL context, this is where we could create an OpenCL-OpenGL compatible context
				context = CLContext.create(platform, devices, errorBuf);
				// Create a command queue
				queue = CL10.clCreateCommandQueue(context, devices.get(0), CL10.CL_QUEUE_PROFILING_ENABLE, errorBuf);
				// Check for any errors
				Util.checkCLError(errorBuf.get(0)); 
				CLProgram prog = CL10.clCreateProgramWithSource(context, loadCLProgram(), null);
				// Build the OpenCL program, store it on the specified device
				int error = CL10.clBuildProgram(prog, devices.get(0), "", null);
				System.out.println(prog.getBuildInfoString(devices.get(0), CL_PROGRAM_BUILD_LOG));
				// Check for any OpenCL errors
				Util.checkCLError(error);
			}
		} catch (LWJGLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//grid[20][20].setVoume(1);
	}
	
	private String loadCLProgram() {
		InputStream is = Water.class.getResourceAsStream("/water/pipes.cls");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		try {
		    String line = br.readLine();
		    while (line != null) {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
		    br.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
	
	public void process() {
		grid[20000].add(0.01f);
		long ticks_now = System.currentTimeMillis();
		// How much time this frame represents.
		float t = (ticks_now - ticks_last)/1000.0f;
		ticks_last = ticks_now;
		for (int c = 0; c < ncells; c++) {
				grid[c].flow(t*20);
		}
		for (int c = 0; c < ncells; c++) {
			grid[c].redraw(t*20);
		}
	}
}
