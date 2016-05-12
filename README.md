# GPIG Environment Simulator

Environment server for GPIG module, uses LiDAR data of York (from the Environment Agency) and jMonkeyEngine

## Run Configuration
Requires considerable memory, set '-Xms4096m' and '-Xmx4096m' in run configuration.

## Nvidia OpenCL Profiling
Water simulation (hydrostratic pipe model) uses OpenCL for many performances, you can profile this [see here.](http://uob-hpc.github.io/2015/05/27/nvvp-import-opencl/)
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
'''