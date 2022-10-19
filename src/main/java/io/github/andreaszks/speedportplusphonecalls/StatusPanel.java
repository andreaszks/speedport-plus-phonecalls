package io.github.andreaszks.speedportplusphonecalls;

import java.io.IOException;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import com.google.gson.JsonArray;

import okhttp3.Request;
import okhttp3.Response;

public class StatusPanel extends JPanel {

  private static final long serialVersionUID = 1L;

  private DefaultTableModel statusTableModel;
  private JTable statusTable;

  /** Create the panel. */
  public StatusPanel() {

    JScrollPane scrollPane = new JScrollPane();
    GroupLayout groupLayout = new GroupLayout(this);
    groupLayout.setHorizontalGroup(
        groupLayout
            .createParallelGroup(Alignment.LEADING)
            .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE));
    groupLayout.setVerticalGroup(
        groupLayout
            .createParallelGroup(Alignment.LEADING)
            .addComponent(
                scrollPane, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE));

    statusTableModel =
        new DefaultTableModel(null, new String[] {"Key", "Value"}) {
          private static final long serialVersionUID = 1L;

          @Override
          public boolean isCellEditable(int row, int column) {
            // all cells false
            return false;
          }
        };

    statusTable = new JTable();
    statusTable.setModel(statusTableModel);
    statusTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    scrollPane.setViewportView(statusTable);
    setLayout(groupLayout);
  }

  public void updateTable(String routerIp) {
    String statusJson = "";
    Request request =
        new Request.Builder()
            .url("http://" + routerIp + "/data/Status.json")
            .addHeader("Accept-Language", "en")
            .build();

    try {
      Response response = AppFrame.client.newCall(request).execute();
      statusJson = response.body().string();

      response.body().close();
      response.close();
    } catch (IOException e1) {
      e1.printStackTrace();
    }

    JsonArray outerBody = AppFrame.gson.fromJson(statusJson, JsonArray.class);

    statusTableModel.setRowCount(0);

    for (int i = 9; i < outerBody.size(); i++) {
      statusTableModel.addRow(
          new Object[] {
            outerBody.get(i).getAsJsonObject().get("varid").getAsString(),
            outerBody.get(i).getAsJsonObject().get("varvalue").getAsString()
          });
    }

    statusTableModel.fireTableDataChanged();
    SwingTools.resizeColumns(statusTable);
  }
}
