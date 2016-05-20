package simulation;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.*;


import com.jme3.system.AppSettings;

public final class SimulationSettings extends JFrame {
	AppSettings settings;
	DisplayMode[] modes;
	
	public SimulationSettings() {
		settings = new AppSettings(true);
		settings.setTitle("Environment Monitor");
		GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		modes = device.getDisplayModes();
		// Go
		createDialog();
	} 
	
	
	private void createDialog() {
		GridBagConstraints gbc;
		JPanel panelMain = new JPanel(new GridBagLayout());
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        	System.out.println("Could not use native UI.");
        }
        setTitle("Simulation Settings");
        // icon
        JLabel icon = new JLabel(new ImageIcon(SimulationSettings.class.getResource("/assets/gpig.jpg")));
        // Screen resolution
        JComboBox comboRes = new JComboBox();
        // File chooser
        JTextField txtSelected = new JTextField(10);
        txtSelected.setText("None");
        JFileChooser chooser = new JFileChooser();
        // Vsync
        JCheckBox boxVsync = new JCheckBox("Enable Vsync?");
        boxVsync.setSelected(false);
        // Full screen
        JCheckBox boxFscreen = new JCheckBox("Enable Fullscreen?");
        boxFscreen.setSelected(false);
        // Buttons
        JButton btnAccept = new JButton("Accept");               
        JButton btnCancel = new JButton("Cancel");
        
        // Layout components
        gbc =  new GridBagConstraints();
        gbc.gridwidth = 4;
        panelMain.add(icon, gbc);
        
        // Checkboxes
        gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.weightx = 0.5;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        panelMain.add(boxFscreen, gbc);
        gbc.gridx = 1;
        panelMain.add(boxVsync, gbc);
        
        // Res selector.
        gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        panelMain.add(new JLabel("Resolution:"), gbc);
        gbc.gridx = 1;
        panelMain.add(comboRes, gbc);
        
        // Height map chooser.
        gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        panelMain.add(new JLabel("Water heightmap:"), gbc);
        gbc.gridx = 1;
        panelMain.add(txtSelected, gbc);

        // Buttons
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 0.5;
        panelMain.add(btnCancel, gbc);
        gbc.gridx = 1;
        panelMain.add(btnAccept, gbc);        
        gbc = new GridBagConstraints();
        
        // Start
        this.getContentPane().add(panelMain);
        panelMain.getRootPane().setDefaultButton(btnAccept);
        pack();
        panelMain.setVisible(true);
        setVisible(true);
	}
	
	public AppSettings getSettings() {
		return settings;
	}
}
