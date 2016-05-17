constant float FFLOW =  0.0005f * 9.81f / 1.0f;
// https://software.intel.com/sites/default/files/managed/72/2c/gfxOptimizationGuide.pdf
// https://www.khronos.org/registry/cl/specs/opencl-1.x-latest.pdf#page=168

/* The flow kernel. Processes one row.
float t -  time since last frame.
float* heights - heights for every cell in water.
float* theights - heights for terrain.
float* flows - flows for each cell, 4 for each. (N,E,S,W)
*/
kernel void flow(const float t, 
				global const float* heights, 
				global const float* theights,
				global float* flows) {
	int r = get_global_id(0);
	int offset = r * (NCOLS);
	float flowf = t * (FFLOW);
	// NOTE: number of columns MUST be divisible by 4.
	for (int c = 0; c < (NCOLS); c++, offset++) {
		float waterh = heights[offset];
		float h = waterh + theights[offset];
		// Load vectors flows.
		float4 vflows = vload4(offset, flows);
		// N
		if (r != 0) {
			vflows.x += flowf * (h - (theights[offset - (NCOLS)] + heights[offset - (NCOLS)]));
			vflows.x = fmax(0.0f, vflows.x);
		}
		// E
		if (c != (NCOLS-1)) {
			vflows.y += flowf * (h - (theights[offset + 1] + heights[offset + 1]));
			vflows.y = fmax(0.0f, vflows.y);
		}
		// S
		if (r != (NROWS-1)) {
			vflows.z += flowf * (h - (theights[offset + (NCOLS)] + heights[offset + (NCOLS)]));
			vflows.z = fmax(0.0f, vflows.z);
		}
		// W
		if (c != 0) {
			vflows.w += flowf * (h - (theights[offset - 1] + heights[offset - 1]));
			vflows.w = fmax(0.0f, vflows.w);
		}
		// scaling
		float sum = vflows.x + vflows.y + vflows.z + vflows.w;
	    float k = fmin(1.0f, (float)((waterh*(CSIZE))/(sum*t)));
	    vflows *= k;
		// Store results.
		vstore4(vflows, offset, flows);
	}
}

/* Height calculation kernel. Processes one row.
float t - time since last frame.
float* heights - heights for every cell in water.
float* flows - flows for each cell, 4 for each. (N,E,S,W)
int* pipes - pipes id for each cell, 4 for each. (N,E,S,W)
flaot vertices - the vertices that make up the scene.
*/
kernel void height(const float t,
				global float* heights, 
				global const float* flows, 
				global const float* theights,
				global float* vertices) {
	int r = get_global_id(0);
	int offset = r * (NCOLS);
	for (int c = 0; c < (NCOLS); c++, offset++) {
		float inflow = 0;
		float outflow = 0;
		// Load vector for flows.
		float4 vflows = vload4(offset, flows);
		// N
		if (r != 0) {
			// Flow from north neighbour in south direction.
			inflow += flows[((offset - (NCOLS))<<2)+2];
			outflow += vflows.x;
		}
		// E
		if (c != (NCOLS-1)) {
			// Flow from east neighbour in west direction.
			inflow += flows[((offset + 1)<<2)+3];
			outflow += vflows.y;
		}
		// S
		if (r != (NROWS-1)) {
			// Flow from south neighbour in north direction.
			inflow += flows[(offset + (NCOLS))<<2];
			outflow += vflows.z;
		}
		// W
		if (c != 0) {
			// Flow from west neighbour in east direction.
			inflow += flows[((offset-1)<<2)+1];
			outflow += vflows.w;
		}
		float dv = t * (inflow-outflow);
		float waterh = heights[offset] + dv/(CSIZE);
		float nh = fmax(0.0f, waterh);
		heights[offset] = nh;
		// Update vertex buffer.
		float nheight = theights[offset] + nh;
		vertices[(offset<<1)+offset+1] = nheight;
	}
}


