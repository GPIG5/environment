package simulation;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.jme3.app.SettingsDialog.SelectionListener;
import com.jme3.system.AppSettings;

public final class SettingsDialog extends JFrame {
    public static final int APPROVE_SELECTION = 0, EXIT_SELECTION = 1;
    
	private AppSettings settings;
	private DisplayMode[] modes;
	private String[] strModes;
	
	private JCheckBox boxVsync;
	private JCheckBox boxFscreen;
	private JComboBox<String> comboRes;
	private JFileChooser chooser;
	private SelectionListener selectionListener = null;
	
	private String heightmap = "";
	private boolean vsync = false;
	private boolean fscreen = false;
	
	private DisplayMode[] defaultModes = {
	        new DisplayMode(1024, 768, 24, 60),
	        new DisplayMode(1280, 720, 24, 60),
	        new DisplayMode(1280, 1024, 24, 60),
	        new DisplayMode(1440, 900, 24, 60),
	        new DisplayMode(1680, 1050, 24, 60),
	};
	
	public SettingsDialog() {
		settings = new AppSettings(true);
		settings.setTitle("Environment Monitor");
		GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		ArrayList<DisplayMode> tmpModes = new ArrayList<DisplayMode>();
		DisplayMode[] devModes = device.getDisplayModes();
		for (DisplayMode d : devModes) {
			tmpModes.add(d);
		}
		for (DisplayMode d : defaultModes) {
			boolean found = false;
			for (DisplayMode e : devModes) {
				if (e.getWidth() == d.getWidth() 
						&& e.getHeight() == d.getHeight()) {
					found = true;
					break;
				}
			}
			if (!found) {
				tmpModes.add(d);
			}
		}
		// Populate str modes.
		tmpModes.sort(new DisplayModeSorter());
		modes = tmpModes.toArray(devModes);
		strModes = new String[modes.length];
		for (int i = 0; i < modes.length; i++) {
			StringBuilder strb =  new StringBuilder(modes[i].getWidth()+"x"+modes[i].getHeight());
			strb.append(" @ "+modes[i].getRefreshRate()+"Hz, "+modes[i].getBitDepth()+"bit");
			strModes[i] = strb.toString();
		}
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				setUserSelection(EXIT_SELECTION);
			}
		});
		// Go
		this.setResizable(false);
		this.setAlwaysOnTop(true);
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
        JLabel icon = new JLabel(new ImageIcon(SettingsDialog.class.getResource("/assets/gpig.jpg")));
        // Screen resolution
        comboRes = new JComboBox<>();
        comboRes.setModel(new DefaultComboBoxModel<>(strModes));
        comboRes.setSelectedItem(0);
        // File chooser
        JTextField txtSelected = new JTextField(15);
        txtSelected.setEditable(false);
        txtSelected.setText("Select file");
        chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Image files", "jpeg", "jpg", "png", "bmp"));
        txtSelected.addMouseListener(new MouseAdapter() {
        	@Override
        	public void mouseClicked(MouseEvent e) {
        		if (chooser.showOpenDialog(panelMain) == JFileChooser.APPROVE_OPTION) {
        			txtSelected.setText(chooser.getSelectedFile().getName());
        			heightmap = chooser.getSelectedFile().getPath();
        		}
        	}
		});
        // Vsync
        boxVsync = new JCheckBox("Vsync");
        boxVsync.setSelected(false);
        boxVsync.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				vsync = boxVsync.isSelected();
			}
		});
        // Full screen
        boxFscreen = new JCheckBox("Fullscreen");
        boxFscreen.setSelected(false);
        boxFscreen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				fscreen = boxFscreen.isSelected();
			}
		});
        // Buttons
        JButton btnAccept = new JButton("Accept");     
        btnAccept.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// Verify that heightmap file exists
				if (heightmap != "") {
					File f =  new File(heightmap);
					if (f.exists() && !f.isDirectory()) {
						updateSettings();
						setUserSelection(APPROVE_SELECTION);
					}
					else {
						JOptionPane.showMessageDialog(panelMain, "Water heightmap \"" + heightmap + "\" is not a file.");
					}
				}
				else {
					JOptionPane.showMessageDialog(panelMain, "Water heightmap not selected.");
				}
			}
		});
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				setUserSelection(EXIT_SELECTION);
			}
		});
        
        // Layout components
        gbc =  new GridBagConstraints();
        gbc.gridwidth = 4;
        panelMain.add(icon, gbc);
        
        // Checkboxes
        gbc = new GridBagConstraints();
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        panelMain.add(boxVsync, gbc);
        gbc.gridx = 2;
        panelMain.add(boxFscreen, gbc);
        
        // Res selector.
        gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.ipadx = 5;
        panelMain.add(new JLabel("Resolution:"), gbc);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panelMain.add(comboRes, gbc);
        
        // Height map chooser.
        gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.ipadx = 5;
        panelMain.add(new JLabel("Water:"), gbc);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panelMain.add(txtSelected, gbc);

        // Buttons
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0.5;
        panelMain.add(btnCancel, gbc);
        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridx = 2;
        panelMain.add(btnAccept, gbc);        
        
        // Start
        this.getContentPane().add(panelMain);
        panelMain.getRootPane().setDefaultButton(btnAccept);
        pack();
        setLocationRelativeTo(null);
        panelMain.setVisible(true);
        setVisible(true);
	}
	
	// Propagate information to settings object.
	public void updateSettings() {
		DisplayMode smode = modes[comboRes.getSelectedIndex()];
		settings.setWidth(smode.getWidth());
		settings.setHeight(smode.getHeight());
		settings.setFrequency(smode.getRefreshRate());
		settings.setVSync(vsync);
		settings.setFullscreen(fscreen);
	}
	
	public AppSettings getSettings() {
		return settings;
	}
	
	// Taken from JME3 settings dialog.
    public void setSelectionListener(SelectionListener sl) {
        selectionListener = sl;
    }
    
    private void setUserSelection(int selection) {
        selectionListener.onSelection(selection);
        dispose();
    }
	
	private class DisplayModeSorter implements Comparator<DisplayMode> {
        public int compare(DisplayMode a, DisplayMode b) {
            // Width
            if (a.getWidth() != b.getWidth()) {
                return (a.getWidth() > b.getWidth()) ? 1 : -1;
            }
            // Height
            if (a.getHeight() != b.getHeight()) {
                return (a.getHeight() > b.getHeight()) ? 1 : -1;
            }
            // Bit depth
            if (a.getBitDepth() != b.getBitDepth()) {
                return (a.getBitDepth() > b.getBitDepth()) ? 1 : -1;
            }
            // Refresh rate
            if (a.getRefreshRate() != b.getRefreshRate()) {
                return (a.getRefreshRate() > b.getRefreshRate()) ? 1 : -1;
            }
            // All fields are equal
            return 0;
        }
    }
}
