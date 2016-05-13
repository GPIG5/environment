# GPIG Environment Simulator

Environment server for GPIG module, uses LiDAR data of York from the [Environment Agency](http://www.geostore.com/environment-agency/survey.html) and [jMonkeyEngine](https://jmonkeyengine.org/). Contains water simulation derived from:

* Dynamic Simulation of Splashing Fluids - O'Brien and Hodgins (1995)
* Interactive Terrain Modeling Using Hydraulic Erosion -  Št’ava et al. (2008)
* Efficient Animation of Water Flow on Irregular Terrains  - Maes et al. (2006)

## Run Configuration
Requires considerable memory, set `-Xms4096m` and `-Xmx4096m` in run configuration.

## Nvidia OpenCL Profiling
Water simulation uses OpenCL for many performances, you can profile this [see here.](http://uob-hpc.github.io/2015/05/27/nvvp-import-opencl/)
Set (in run configuration)
`COMPUTE_PROFILE=1`
`COMPUTE_PROFILE_CONFIG=nvvp.cfg`

Where nvvp.cfg is in the local directory:
```
	profilelogformat CSV
	streamid
	gpustarttimestamp
	gpuendtimestamp
	gridsize
	threadblocksize
	dynsmemperblock
	stasmemperblock
	regperthread
	memtransfersize
```