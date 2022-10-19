package io.github.andreaszks.speedportplusphonecalls;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListSelectionModel;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import okhttp3.Request;
import okhttp3.Response;

public class PhoneCallsPanel extends JPanel {

  private static final long serialVersionUID = 1L;

  private JTable callsTable;
  private DefaultTableModel callsTableModel;
  private final ButtonGroup buttonGroup = new ButtonGroup();
  private JRadioButton rdbtnMissed;
  private JRadioButton rdbtnTaken;
  private AbstractButton rdbtnDialed;

  private transient ArrayList<PhoneCall> missedCalls = new ArrayList<>();
  private transient ArrayList<PhoneCall> takenCalls = new ArrayList<>();
  private transient ArrayList<PhoneCall> dialedCalls = new ArrayList<>();

  /** Create the panel. */
  public PhoneCallsPanel() {

    JPanel panel = new JPanel();
    panel.setBorder(new LineBorder(new Color(0, 0, 0)));

    JScrollPane scrollPane = new JScrollPane();
    GroupLayout groupLayout = new GroupLayout(this);
    groupLayout.setHorizontalGroup(
        groupLayout
            .createParallelGroup(Alignment.LEADING)
            .addComponent(panel, GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
            .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE));
    groupLayout.setVerticalGroup(
        groupLayout
            .createParallelGroup(Alignment.LEADING)
            .addGroup(
                groupLayout
                    .createSequentialGroup()
                    .addComponent(
                        panel,
                        GroupLayout.PREFERRED_SIZE,
                        GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 259, Short.MAX_VALUE)));

    rdbtnMissed = new JRadioButton("Missed");
    rdbtnMissed.setSelected(true);
    rdbtnTaken = new JRadioButton("Taken");
    rdbtnDialed = new JRadioButton("Dialed");

    buttonGroup.add(rdbtnMissed);
    buttonGroup.add(rdbtnTaken);
    buttonGroup.add(rdbtnDialed);

    panel.add(rdbtnMissed);
    panel.add(rdbtnTaken);
    panel.add(rdbtnDialed);

    callsTableModel =
        new DefaultTableModel(
            null, new String[] {"id", "date", "time", "who", "port", "duration"}) {
          private static final long serialVersionUID = 1L;

          @Override
          public boolean isCellEditable(int row, int column) {
            // all cells false
            return false;
          }
        };

    callsTable = new JTable();
    callsTable.setModel(callsTableModel);
    callsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    initListeners();

    scrollPane.setViewportView(callsTable);
    setLayout(groupLayout);
  }

  private void initListeners() {
    ActionListener rbActionListener =
        e -> {
          if (e.getSource().equals(rdbtnMissed)) fillTable(0);
          else if (e.getSource().equals(rdbtnTaken)) fillTable(1);
          else if (e.getSource().equals(rdbtnDialed)) fillTable(2);
        };
    rdbtnMissed.addActionListener(rbActionListener);
    rdbtnTaken.addActionListener(rbActionListener);
    rdbtnDialed.addActionListener(rbActionListener);
  }

  public boolean updateTable(String routerIp, String sessionCookie, boolean csv) {
    missedCalls.clear();
    takenCalls.clear();
    dialedCalls.clear();
    try {
      String callsUrl = "http://" + routerIp + "/data/PhoneCalls.json";

      Request callsRequest =
          new Request.Builder()
              .url(callsUrl)
              .addHeader("Accept-Language", "en")
              .addHeader("Cookie", sessionCookie)
              .build();

      Response callsResponse = AppFrame.client.newCall(callsRequest).execute();

      JsonArray outerBodyCalls =
          AppFrame.gson.fromJson(callsResponse.body().string(), JsonArray.class);

      callsResponse.body().close();
      callsResponse.close();

      for (int ji = 9; ji < outerBodyCalls.size(); ji++) {
        // First 9 objects are router status objects appearing on all endpoints.
        // You can see the JSON format by accessing the above link while you are logged in.
        JsonObject outerObject = outerBodyCalls.get(ji).getAsJsonObject();
        JsonArray innerBody = outerObject.get("varvalue").getAsJsonArray();

        String id = innerBody.get(0).getAsJsonObject().get("varvalue").getAsString();
        String date = innerBody.get(1).getAsJsonObject().get("varvalue").getAsString();
        String time = innerBody.get(2).getAsJsonObject().get("varvalue").getAsString();
        String who = innerBody.get(3).getAsJsonObject().get("varvalue").getAsString();
        String port = innerBody.get(4).getAsJsonObject().get("varvalue").getAsString();
        String details = innerBody.get(5).getAsJsonObject().get("varvalue").getAsString();

        PhoneCall pc = new PhoneCall(id, date, time, who, port, details);

        if (outerObject.get("varid").getAsString().contentEquals("addmissedcalls"))
          missedCalls.add(pc);
        else if (outerObject.get("varid").getAsString().contentEquals("addtakencalls"))
          takenCalls.add(pc);
        else if (outerObject.get("varid").getAsString().contentEquals("adddialedcalls"))
          dialedCalls.add(pc);
      }

      if (csv) exportToCsvFile();

      if (rdbtnMissed.isSelected()) fillTable(0);
      else if (rdbtnTaken.isSelected()) fillTable(1);
      else if (rdbtnDialed.isSelected()) fillTable(2);

      return true;
    } catch (IOException e1) {
      e1.printStackTrace();
      return false;
    }
  }

  private void fillTable(int k) {
    callsTableModel.setRowCount(0);

    if (k == 0)
      for (PhoneCall pc : missedCalls)
        callsTableModel.addRow(
            new Object[] {pc.id, pc.date, pc.time, pc.who, pc.port, pc.duration});
    else if (k == 1)
      for (PhoneCall pc : takenCalls)
        callsTableModel.addRow(
            new Object[] {pc.id, pc.date, pc.time, pc.who, pc.port, pc.duration});
    else if (k == 2)
      for (PhoneCall pc : dialedCalls)
        callsTableModel.addRow(
            new Object[] {pc.id, pc.date, pc.time, pc.who, pc.port, pc.duration});

    callsTableModel.fireTableDataChanged();
    SwingTools.resizeColumns(callsTable);
  }

  private void exportToCsvFile() {
    File missedFile = new File("1missed.csv");
    File takenFile = new File("2taken.csv");
    File dialedFile = new File("3dialed.csv");

    List<File> files = Arrays.asList(missedFile, takenFile, dialedFile);

    for (int i = 0; i < 3; i++) {
      try (FileOutputStream fis = new FileOutputStream(files.get(i))) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,date,time,who,port,duration\n");
        if (i == 0) for (PhoneCall pc : missedCalls) sb.append(pc.toCsv() + "\n");
        else if (i == 1) for (PhoneCall pc : takenCalls) sb.append(pc.toCsv() + "\n");
        else if (i == 2) for (PhoneCall pc : dialedCalls) sb.append(pc.toCsv() + "\n");
        fis.write(sb.toString().getBytes());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
