import comms.MeshServer;

import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.operation.TransformException;

import simulation.Simulation;
import utility.Location;
import utility.ServiceInterface;

public class Main {

    public static void main(String[] args) {
        ServiceInterface si = new ServiceInterface();
        MeshServer mesh = new MeshServer();
        mesh.start(si);
        Simulation sim = new Simulation();
        sim.start(si);

        mesh.terminate();

        /*
        Location loc = new Location(54.007111f, -1.004663f, 0);
        try {
            loc.getOSGB();
        } catch (MismatchedDimensionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAuthorityCodeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FactoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TransformException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } */
    }

}
