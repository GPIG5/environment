__constant float FFLOW =  0.00005f * 9.81f / 1.0f;
// https://software.intel.com/sites/default/files/managed/72/2c/gfxOptimizationGuide.pdf

/* The flow kernel.
float t -  time since last frame.
float* heights - heights for every cell in water.
float* flows - flows for each cell, 4 for each. (N,E,S,W)
int* pipes - pipes id for each cell, 4 for each. (N,E,S,W)
*/
kernel void flow(const float t, 
				global const float* heights, 
				global float* const flows, 
				global const int* pipes) {
	const int id = get_global_id(0);
	float flowf = t * FFLOW;
	float nflow;
	float h = heights[id];
	int idx = id << 2;
	if (pipes[idx] != -1) {
		nflow = flows[idx] + flowf * (h - heights[pipes[idx]]);
		flows[idx] = fmax(0.0f, nflow);
	}
	idx++;
	if (pipes[idx] != -1) {
		nflow = flows[idx] + flowf * (h - heights[pipes[idx]]);
		flows[idx] = fmax(0.0f, nflow);
	}
	idx++;
	if (pipes[idx] != -1) {
		nflow = flows[idx] + flowf * (h - heights[pipes[idx]]);
		flows[idx] = fmax(0.0f, nflow);
	}
	idx++;
	if (pipes[idx] != -1) {
		nflow = flows[idx] + flowf * (h - heights[pipes[idx]]);
		flows[idx] = fmax(0.0f, nflow);
	}
}    

/* Height calculation kernel.
float t - time since last frame.
float cellsize2 - cellsize * cellsize (area).
float* heights - heights for every cell in water.
float* flows - flows for each cell, 4 for each. (N,E,S,W)
int* pipes - pipes id for each cell, 4 for each. (N,E,S,W)
*/
kernel void height(const float t,
				const float cellsize2,
				global float* const heights, 
				global const float* flows, 
				global const int* pipes) {
	const int id = get_global_id(0);
	float inflow = 0;
	float outflow = 0;
	int idx = id << 2;
	if (pipes[idx] != -1) {
		// Flow from north neighbour in south direction.
		inflow += flows[(pipes[idx]<<2)+2];
		outflow += flows[idx];
	}
	idx++;
	if (pipes[idx] != -1) {
		// Flow from east neighbour in west direction.
		inflow += flows[(pipes[idx]<<2)+3];
		outflow += flows[idx];
	}
	idx++;
	if (pipes[idx] != -1) {
		// Flow from south neighbour in north direction.
		inflow += flows[pipes[idx]<<2];
		outflow += flows[idx];
	}
	idx++;
	if (pipes[idx] != -1) {
		// Flow from west neighbour in east direction.
		inflow += flows[(pipes[idx]<<2)+1];
		outflow += flows[idx];
	}
	float dv = t * (inflow-outflow);
	float waterh = heights[id] + dv/(cellsize2);
	heights[id] = fmax(0.0f, waterh);
}

