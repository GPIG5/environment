package gui;

import com.sun.corba.se.spi.ior.ObjectKey;
import comms.Drone;
import comms.MeshServer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.net.Socket;
import java.util.AbstractMap;
import java.util.HashMap;

/**
 * Created by hoo on 19/05/2016.
 */
public class DroneStatus extends JFrame {
    private JTable droneTable;
    private JButton closeButton;
    private JButton killButton;
    private JPanel topPanel;
    private static DefaultTableModel model;
    private static JFrame frame;
    private static AbstractMap<String, Drone> drones = new HashMap<>();
    private static final Object lock = new Object();

    public DroneStatus()  {

        killButton.addActionListener(click -> killDrones());
        closeButton.addActionListener(click -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
    }

    public static void start() {
        frame = new JFrame("DroneStatus");
        frame.setContentPane(new DroneStatus().topPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void createUIComponents() {

        model = new DefaultTableModel() {
            String[] columns = {"Drone ID", "Battery Level", "Kill"};

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

        droneTable = new JTable(model);
    }

    public static void addDrone(Drone drone) {
        synchronized (lock) {
            model.addRow(new Object[]{drone.getUuid(), drone.getBattery(), false});
            drones.put(drone.getUuid(), drone);
        }
    }

    public static void removeDrone(Drone drone) {
        synchronized (lock) {
            int ind = -1;
            int i = 0;
            while (i != model.getRowCount() && ind != i) {
                if (model.getValueAt(i, 0).equals(drone.getUuid())) {
                    ind = i;
                } else {
                    ++i;
                }
            }
            drones.remove((String) model.getValueAt(i, 0));
            model.removeRow(ind);
        }
    }

    private static void killDrones() {
        synchronized (lock) {
            for (int i = 0; i != model.getRowCount(); ++i) {
                String uuid = (String) model.getValueAt(i, 0);
                drones.get(uuid).setKillComms((boolean) model.getValueAt(i, 2));
            }
        }
    }

}
