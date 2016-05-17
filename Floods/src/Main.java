import comms.MeshServer;
import utility.ServiceInterface;

public class Main {

    public static void main(String[] args) {
        ServiceInterface si = new ServiceInterface();
        MeshServer mesh = new MeshServer();
        mesh.start(si);
        Simulation sim = new Simulation();
        sim.start(si);
        mesh.terminate();
    }

}
