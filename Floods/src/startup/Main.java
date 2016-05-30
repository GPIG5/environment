package startup;
import com.jme3.app.SettingsDialog.SelectionListener;

import comms.MeshServer;
import comms.UserInterface;
import simulation.Simulation;
import simulation.SettingsDialog;
import utility.ServiceInterface;

public final class Main {
	static Simulation sim = null;
	static MeshServer mesh = null;
	static UserInterface droneWindow = null;
	static SettingsDialog settings = null;
	
    public static void main(String[] args) {
        ServiceInterface si = new ServiceInterface();
        mesh = new MeshServer();
		if (args.length > 0) {
			mesh.start(si, args[0]);
		} else {
			mesh.start(si, null);
		}
        Simulation sim = new Simulation();
        // Sim settings
        settings = new SettingsDialog();
        settings.setSelectionListener(new SelectionListener() {
			@Override
			public void onSelection(int arg0) {
				if (arg0 == SettingsDialog.APPROVE_SELECTION) {
					droneWindow = new UserInterface(mesh.getDrones());
			        sim.setSettings(settings.getSettings());
			        sim.setShowSettings(false);
			        sim.start(si, mesh.getC2().getLocations().get(0));
				}
				else {
					close();
				}
				
			}
		});
    }
    
    public static void close() {
    	if (mesh != null) {
    		mesh.terminate();
    	}
    	if (sim != null) {
    		sim.requestClose(true);
    	}
    	if (settings != null) {
    		settings.dispose();
    	}
    	if (droneWindow != null) {
    		droneWindow.dispose();
    	}
    	System.exit(0);
    }
}
