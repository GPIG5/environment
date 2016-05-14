package water;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Scanner;

import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.opengl.GL;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.*;
import org.lwjgl.opencl.api.CLBufferRegion;
import org.lwjgl.opengl.Drawable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import static org.lwjgl.opencl.CL10.*;

import terrain.Terrain;

public class Water extends Mesh {
	boolean init = false;
	long ticks_last;
	// Group particles by the row they occupy.
	Cells cells;
	Terrain terrain;
	int size;
	int cols;
	int rows;
	boolean cl_initd = false;
	// OpenCL variables
	public static CLContext context;
	public static CLPlatform platform;
	public static List<CLDevice> devices;
	public static CLCommandQueue queue;
	public static CLKernel calcFlow;
	public static CLKernel calcHeight;
	private static CLMem hMem;
	private static CLMem fMem;
	private static CLMem tMem;
	private static CLMem vMem;
	
	// Work memory
	// Height buffer
	FloatBuffer hBuff;
	// Terrain height buffer
	FloatBuffer tBuff;
	// Error buffer
	IntBuffer eBuff;
	// Flows buffer
	FloatBuffer fBuff;
	// Result buffer
	FloatBuffer rBuff;
	// Pointer work buffer
	PointerBuffer sBuff;
	// Vertex buffer
	FloatBuffer vertBuff;
	
	// https://github.com/LWJGL/lwjgl/blob/master/src/java/org/lwjgl/test/opencl/HelloOpenCL.java
	// https://github.com/riccardobl/JMEOpenCL
	public Water(Terrain t, Drawable d) {
		// First step is to init OpenCL context.
		try {
			CL.create();
			eBuff = BufferUtils.createIntBuffer(1);
			final List<CLPlatform> platforms = CLPlatform.getPlatforms();
			if (platforms != null) {
				platform = CLPlatform.getPlatforms().get(0); 
				// Run our program on the GPU
				devices = platform.getDevices(CL10.CL_DEVICE_TYPE_GPU);
				// Create an OpenCL context, this is where we could create an OpenCL-OpenGL compatible context
				context = CLContext.create(platform, devices, null, d, eBuff);
			}
		} catch (LWJGLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//
		ticks_last = System.currentTimeMillis();
		terrain = t;
		cells = terrain.makeCells();
		size = cells.getSize();
		cols = cells.getCols();
		rows = cells.getRows();
		System.out.println("Adding water cells to scene...");
		this.setStreamed();
		setBuffer(Type.Position, 3, cells.getVertices());
		setBuffer(Type.Index, 3, cells.getEdges());
		updateBound();
	}
	
	private void setupOpenCL(int vid) {
		if (context != null) {
			// Create a command queue
			queue = CL10.clCreateCommandQueue(context, devices.get(0), CL10.CL_QUEUE_PROFILING_ENABLE, eBuff);
			Util.checkCLError(eBuff.get(0)); 
			// Build the OpenCL program, store it on the specified device
			CLProgram prog = CL10.clCreateProgramWithSource(context, loadCLProgram(), null);
			// Each kernel will do HALF a row.
			String args = "-cl-single-precision-constant -cl-no-signed-zeros -cl-finite-math-only";
			args += " -DNROWS="+rows+" -DNCOLS="+cols+" -DCSIZE="+cells.getCsize2()+"f";
			System.out.println("OpenCL build args: " + args);
			int error = CL10.clBuildProgram(prog, devices.get(0), args, null);
			System.out.println(prog.getBuildInfoString(devices.get(0), CL_PROGRAM_BUILD_LOG));
			// Check for any OpenCL errors
			Util.checkCLError(error);
			// Kernels
			calcFlow = CL10.clCreateKernel(prog, "flow", null);
			calcHeight = CL10.clCreateKernel(prog, "height", eBuff);
			System.out.println(eBuff.get(0));
			// Memory
			hBuff = cells.getHeights();
			hBuff.rewind();
			hMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR, hBuff, eBuff);
			
			tBuff = cells.getTerHeights();
			tBuff.rewind();
			tMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, tBuff, eBuff);
			
			fBuff = cells.getFlows();
			fBuff.rewind();
			fMem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_COPY_HOST_PTR, fBuff, eBuff);
			// Vertex memory
			vMem = CL10GL.clCreateFromGLBuffer(context, CL10.CL_MEM_READ_WRITE, vid, eBuff);
			
			rBuff = BufferUtils.createFloatBuffer(size);
			
			// Work size
			sBuff = BufferUtils.createPointerBuffer(1);
			sBuff.put(0, rows);
			
			// Args
			calcFlow.setArg(1, hMem);
			calcFlow.setArg(2, tMem);
			calcFlow.setArg(3, fMem);
			calcHeight.setArg(1, hMem);
			calcHeight.setArg(2, fMem);
			calcHeight.setArg(3, tMem);
			calcHeight.setArg(4, vMem);
			cl_initd = true;
		}
	}
	
	private String loadCLProgram() {
		InputStream is = Water.class.getResourceAsStream("/water/pipes.cl");
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
		// For some reason buffer ids don't get updated for a few frames.
		if (!cl_initd) {
			int vid = this.getBuffer(Type.Position).getId();
			if (vid != -1) {
				setupOpenCL(vid);
			}
			
		}
		else {
			long ticks_now = System.currentTimeMillis();
			// How much time this frame represents.
			float t = (ticks_now - ticks_last)/1000.0f;
			ticks_last = ticks_now;
			calcFlow.setArg(0, t);
			CL10.clEnqueueNDRangeKernel(queue, calcFlow, 1, null, sBuff, null, null, null);
			CL10.clFinish(queue);
			calcHeight.setArg(0, t);
			// We need the vertex buffer now.
			CL10GL.clEnqueueAcquireGLObjects(queue, vMem, null, null);
			CL10.clEnqueueNDRangeKernel(queue, calcHeight, 1, null, sBuff, null, null, null);
			CL10GL.clEnqueueReleaseGLObjects(queue, vMem, null, null);
			CL10.clFinish(queue);
			// Read the new heights
			//rBuff.rewind();
			//CL10.clEnqueueReadBuffer(queue, hMem, CL10.CL_TRUE, 0, rBuff, null, null);
		}
	}
}
