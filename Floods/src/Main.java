import mesh.Mesh;
import simulation.Simulation;
import utility.ServiceInterface;

public class Main {

    public static void main(String[] args) {
        ServiceInterface si = new ServiceInterface();
        Mesh mesh = new Mesh();
        mesh.start(si);
        Simulation sim = new Simulation();
        sim.start(si);
    }

}
