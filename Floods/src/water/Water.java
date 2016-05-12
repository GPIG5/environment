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
import java.nio.FloatBuffer;
import java.util.List;

import static org.lwjgl.opencl.CL10.*;

import terrain.Terrain;

public class Water extends Node {
	long ticks_last;
	// Group particles by the row they occupy.
	Cells cells;
	Terrain terrain;
	int size;
	float csize2;
	Geometry[] planes;
	Vector3f[] points;
	// OpenCL variables
	public static CLContext context;
	public static CLPlatform platform;
	public static List<CLDevice> devices;
	public static CLCommandQueue queue;
	public static CLKernel calcFlow;
	public static CLKernel calcHeight;
	private static CLMem hMem;
	private static CLMem pMem;
	private static CLMem fMem;
	private static CLMem tMem;
	
	// Work memory
	// Height buffer
	FloatBuffer hBuff;
	// Terrain height buffer
	FloatBuffer tBuff;
	// Pipes buffer
	IntBuffer pBuff;
	// Flows buffer
	FloatBuffer fBuff;
	// Result buffer
	FloatBuffer rBuff;
	// Pointer work buffer
	PointerBuffer sBuff;
	
	// https://github.com/LWJGL/lwjgl/blob/master/src/java/org/lwjgl/test/opencl/HelloOpenCL.java
	public Water(Terrain t, Material mat) {
		super.setName("Water Body");
		// Params
		ticks_last = System.currentTimeMillis();
		terrain = t;
		cells = terrain.makeCells();
		size = cells.getSize();
		csize2 = cells.getCsize2();
		planes = cells.getPlanes();
		points = cells.getPoints();
		System.out.println("Adding water cells to scene...");
		for (Geometry g : cells.getPlanes()) {
			g.setMaterial(mat);
			// Cull initially
			attachChild(g);
			g.setCullHint(cullHint.Always);
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
				Util.checkCLError(errorBuf.get(0)); 
				// Build the OpenCL program, store it on the specified device
				CLProgram prog = CL10.clCreateProgramWithSource(context, loadCLProgram(), null);
				int error = CL10.clBuildProgram(prog, devices.get(0), "", null);
				System.out.println(prog.getBuildInfoString(devices.get(0), CL_PROGRAM_BUILD_LOG));
				// Check for any OpenCL errors
				Util.checkCLError(error);
				// Kernels
				calcFlow = CL10.clCreateKernel(prog, "flow", null);
				calcHeight = CL10.clCreateKernel(prog, "height", null);
				// Memory
				hBuff = BufferUtils.createFloatBuffer(size);
				hBuff.put(cells.getHeights());
				hBuff.rewind();
				hMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR, hBuff, errorBuf);
				
				tBuff = BufferUtils.createFloatBuffer(size);
				tBuff.put(cells.getTerHeights());
				tBuff.rewind();
				tMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, tBuff, errorBuf);
				
				pBuff = BufferUtils.createIntBuffer(size<<2);
				pBuff.put(cells.getPipes());
				pBuff.rewind();
				pMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, pBuff, errorBuf);
				
				fBuff = BufferUtils.createFloatBuffer(size<<2);
				fBuff.put(cells.getFlows());
				fBuff.rewind();
				fMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR, fBuff, errorBuf);
				
				rBuff = BufferUtils.createFloatBuffer(size);
				
				// Work size
				sBuff = BufferUtils.createPointerBuffer(1);
				sBuff.put(0, size);
				
				// Args
				calcFlow.setArg(2, hMem);
				calcFlow.setArg(3, tMem);
				calcFlow.setArg(4, fMem);
				calcFlow.setArg(5, pMem);
				calcHeight.setArg(2, hMem);
				calcHeight.setArg(3, fMem);
				calcHeight.setArg(4, pMem);
			}
		} catch (LWJGLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		long ticks_now = System.currentTimeMillis();
		// How much time this frame represents.
		float t = (ticks_now - ticks_last)/1000.0f;
		ticks_last = ticks_now;
		calcFlow.setArg(0, t);
		calcFlow.setArg(1, csize2);
		// Run the specified number of work units using our OpenCL program kernel
		CL10.clEnqueueNDRangeKernel(queue, calcFlow, 1, null, sBuff, null, null, null);
		CL10.clFinish(queue);
		calcHeight.setArg(0, t);
		calcHeight.setArg(1, csize2);
		CL10.clEnqueueNDRangeKernel(queue, calcHeight, 1, null, sBuff, null, null, null);
		CL10.clFinish(queue);
		// Read the new heights
		rBuff.rewind();
		CL10.clEnqueueReadBuffer(queue, hMem, CL10.CL_TRUE, 0, rBuff, null, null);
		for (int i = 0; i < rBuff.capacity(); i++) {
			planes[i].setLocalTranslation(points[i].x, rBuff.get(i) + tBuff.get(i), points[i].z);
			// Cull until 0.001 surpassed.
			if (rBuff.get(i) > 0.001) {
				planes[i].setCullHint(cullHint.Dynamic);
			}
		}
		/*
		for (int c = 0; c < ncells; c++) {
				grid[c].flow(t*20);
		}
		for (int c = 0; c < ncells; c++) {
			grid[c].redraw(t*20);
		}
		*/
	}
}
