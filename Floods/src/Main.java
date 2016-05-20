import comms.MeshServer;

import simulation.Simulation;
import simulation.SimulationSettings;
import utility.ServiceInterface;

public class Main {

    public static void main(String[] args) {
        ServiceInterface si = new ServiceInterface();
        MeshServer mesh = new MeshServer();
        mesh.start(si);
        // Sim settings
        SimulationSettings sdialog = new SimulationSettings();
        Simulation sim = new Simulation();
        sim.setSettings(sdialog.getSettings());
        sim.setShowSettings(false);
        sim.start(si);
        // Terminate the mesh when the sim stops.
        //mesh.terminate();
    }

}
