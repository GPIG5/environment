__constant float FFLOW =  0.0005f * 9.81f / 1.0f;
// https://software.intel.com/sites/default/files/managed/72/2c/gfxOptimizationGuide.pdf
// https://www.khronos.org/registry/cl/specs/opencl-1.x-latest.pdf#page=168

/* The flow kernel. Processes one row.
int n - the number of columns in a row.
float t -  time since last frame.
float csize2 - cellsize * cellsize (area).
float* heights - heights for every cell in water.
float* theights - heights for terrain.
float* flows - flows for each cell, 4 for each. (N,E,S,W)
int* pipes - pipes id for each cell, 4 for each. (N,E,S,W)
*/
kernel void flow(const int n,
				const float t, 
				const float csize2,
				global const float* heights, 
				global const float* theights,
				global float* const flows, 
				global const int* pipes) {
	int id = get_global_id(0) * n;
	float flowf = t * FFLOW;
	int c = 0;
	int i;
	for (; c < n; c++) {
		i = id + c;
		float waterh = heights[i];
		float h = waterh + theights[i];
		// Load vectors for pipes and flows.
		int4 vpipes = vload4(i, pipes);
		float4 vflows = vload4(i, flows);
		if (vpipes.x != -1) {
			vflows.x += flowf * (h - (theights[vpipes.x] + heights[vpipes.x]));
			vflows.x = fmax(0.0f, vflows.x);
		}
		if (vpipes.y != -1) {
			vflows.y += flowf * (h - (theights[vpipes.y] + heights[vpipes.y]));
			vflows.y = fmax(0.0f, vflows.y);
		}
		if (vpipes.z != -1) {
			vflows.z += flowf * (h - (theights[vpipes.z] + heights[vpipes.z]));
			vflows.z = fmax(0.0f, vflows.z);
		}
		if (vpipes.w != -1) {
			vflows.w += flowf * (h - (theights[vpipes.w] + heights[vpipes.w]));
			vflows.w = fmax(0.0f, vflows.w);
		}
		// scaling
		float sum = vflows.x + vflows.y + vflows.z + vflows.w;
	    float k = fmin((float)1.0, (float)((waterh*csize2)/(sum*t)));
	    vflows.x *= k;
	   	vflows.y *= k;
	    vflows.z *= k;
	    vflows.w *= k;
		// Store results.
		vstore4(vflows, i, flows);
	}
}    

/* Height calculation kernel. Processes one row.
int n - the number of columns in a row.
float t - time since last frame.
float csize2 - cellsize * cellsize (area).
float* heights - heights for every cell in water.
float* flows - flows for each cell, 4 for each. (N,E,S,W)
int* pipes - pipes id for each cell, 4 for each. (N,E,S,W)
*/
kernel void height(const int n,
				const float t,
				const float csize2,
				global float* const heights, 
				global const float* flows, 
				global const int* pipes) {
	int id = get_global_id(0) * n;
	int i;
	int c = 0;
	for (;c < n; c++) {
		i = id + c;
		float inflow = 0;
		float outflow = 0;
		// Load vectors for pipes and flows.
		int4 vpipes = vload4(i, pipes);
		float4 vflows = vload4(i, flows);
		if (vpipes.x != -1) {
			// Flow from north neighbour in south direction.
			inflow += flows[(vpipes.x<<2)+2];
			outflow += vflows.x;
		}
		if (vpipes.y != -1) {
			// Flow from east neighbour in west direction.
			inflow += flows[(vpipes.y<<2)+3];
			outflow += vflows.y;
		}
		if (vpipes.z != -1) {
			// Flow from south neighbour in north direction.
			inflow += flows[vpipes.z<<2];
			outflow += vflows.z;
		}
		if (vpipes.w != -1) {
			// Flow from west neighbour in east direction.
			inflow += flows[(vpipes.w<<2)+1];
			outflow += vflows.w;
		}
		float dv = t * (inflow-outflow);
		float waterh = heights[i] + dv/(csize2);
		heights[i] = fmax(0.0f, waterh);
	}
}

