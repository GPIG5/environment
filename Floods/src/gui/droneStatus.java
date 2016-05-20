package gui;

import com.sun.org.apache.xpath.internal.operations.Bool;
import comms.Drone;
import comms.MeshServer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.net.Socket;

/**
 * Created by hoo on 19/05/2016.
 */
public class droneStatus extends JFrame {
    private JTable droneTable;
    private JButton closeButton;
    private JButton killButton;
    private JPanel topPanel;
    private static DefaultTableModel model;

    public droneStatus()  {


    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("droneStatus");
        frame.setContentPane(new droneStatus().topPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        Drone test = new Drone(new Socket(), new MeshServer());
        test.setUuid("abcd-1234");
        test.setBattery(99);
        addDrone(test);

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
        model.addRow(new Object[] {drone.getUuid(), drone.getBattery(), false});
    }

}
