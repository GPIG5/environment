package comms;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import startup.Main;

public class UserInterface extends JFrame {
    private JTable tableDrones;
    private JScrollPane tableScrollPane;
    private DefaultTableModel tableModel;
    private Map<String, Drone> drones;
    private Timer droneTimer;
	
	public UserInterface(Map<String, Drone> drones) {
    	this.drones = drones;
		this.setResizable(true);
		this.setAlwaysOnTop(false);
		this.setFocusable(false);
		createWindow();
	}
	
	private void createTable() {
        tableModel = new DefaultTableModel() {
            String[] columns = {"Drone ID", "Battery Level", "Communication"};

            @Override
            public int getColumnCount() {
                return columns.length;
            }

            @Override
            public String getColumnName(int index) {
                return columns[index];
            }

            @Override
            public Class getColumnClass(int column) {
                switch (column) {
                    case 0:
                        return String.class;
                    case 1:
                        return Integer.class;
                    case 2:
                        return Boolean.class;
                    default:
                        return String.class;
                }
            }
        };
        tableDrones = new JTable(tableModel);
	}
	
	private void createWindow() {
		GridBagConstraints gbc;
		JPanel panelMain = new JPanel(new GridBagLayout());
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        	System.out.println("Could not use native UI.");
        }
        setTitle("Drone Monitor");
        // Table
        createTable();
        // Scroll pane
        tableScrollPane = new JScrollPane();
        tableScrollPane.setViewportView(tableDrones);
        
        // Layout
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panelMain.add(tableScrollPane, gbc);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				Main.close();
			}
		});
        this.getContentPane().add(panelMain);
        pack();
        setLocationRelativeTo(null);
        panelMain.setVisible(true);
        setVisible(true);
        // Start the drone timer
        droneTimer = new Timer(500, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateDrones();				
			}
		});
        droneTimer.setRepeats(true);
        droneTimer.start();
	}
	
	private void updateDrones() {
        Map<String, Boolean> current = new HashMap<String, Boolean>();
        int offset = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
        	int row = i - offset;
        	String uuid = (String)tableModel.getValueAt(row, 0);
        	Drone d = drones.get(uuid);
        	if (d != null) {
        		current.put(uuid, (Boolean)tableModel.getValueAt(row, 2));
        		tableModel.setValueAt(d.getBattery(), row, 1);
        	} else {
        		// Each time we remove a row, update the offset.
        		tableModel.removeRow(row);
        		offset++;
        	}
        }
        // Now check the drones hash map, are there any drones we haven't seen?
        drones.forEach((k, v) -> {
        	// Is this drone in the list of drones we know of?
        	Boolean killed = current.get(k);
        	if (killed == null) {
        		// We don't know about this drone :/
        		tableModel.addRow(new Object[]{k, v.getBattery(), false});
        	}
        	else {
        		// We know about this drone, update the checkbox value.
        		v.setKillComms(killed);
        	}
        });
	}

}
