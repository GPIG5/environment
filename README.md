# GPIG Environment Simulator

Some notes here.

## Run Configuration
Set

## Nvidia OpenCL Profiling
Water simulation (hydrostratic pipe model) uses OpenCL for many performances, you can profile [this](http://uob-hpc.github.io/2015/05/27/nvvp-import-opencl/).
Set (in run configuration)
`COMPUTE_PROFILE=1`
`COMPUTE_PROFILE_CONFIG=nvvp.cfg`

Where nvvp.cfg is in the local directory:
	profilelogformat CSV
	streamid
	gpustarttimestamp
	gpuendtimestamp