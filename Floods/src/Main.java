import com.jme3.app.SettingsDialog.SelectionListener;

import comms.MeshServer;

import simulation.Simulation;
import simulation.SettingsDialog;
import utility.ServiceInterface;

public class Main {
	static Simulation sim = null;
	static MeshServer mesh = null;

    public static void main(String[] args) {
        ServiceInterface si = new ServiceInterface();
        mesh = new MeshServer();
        mesh.start(si);
        Simulation sim = new Simulation();
        // Sim settings
        SettingsDialog sdialog = new SettingsDialog();
        sdialog.setSelectionListener(new SelectionListener() {
			
			@Override
			public void onSelection(int arg0) {
				if (arg0 == SettingsDialog.APPROVE_SELECTION) {
			        sim.setSettings(sdialog.getSettings());
			        sim.setShowSettings(false);
			        sim.start(si);
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
    	System.exit(0);
    }

}
