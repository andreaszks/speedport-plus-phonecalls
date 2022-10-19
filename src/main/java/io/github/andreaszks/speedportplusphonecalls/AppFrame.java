package io.github.andreaszks.speedportplusphonecalls;

import java.awt.Color;
import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;

import com.google.gson.Gson;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AppFrame extends JFrame {

  private static final long serialVersionUID = 1L;

  static final OkHttpClient client = new OkHttpClient();
  static final Gson gson = new Gson();

  private static String routerIp = "speedport.ip";
  private static String username = "admin";
  private static String password = "";

  private static boolean credentialsAdded = false;
  private static long loggedInSince = 0;
  private static String sessionCookie = "";
  private static boolean csv = false;

  private static JPanel contentPane = new JPanel();
  private static JTabbedPane tabs = new JTabbedPane(SwingConstants.TOP);
  private static StatusPanel statusPanel = new StatusPanel();
  private static PhoneCallsPanel callsPanel = new PhoneCallsPanel();

  private static JButton btnRefresh = new JButton("Refresh");

  /** Launch the application. */
  public static void main(String[] args) {
    EventQueue.invokeLater(
        new Runnable() {
          public void run() {
            try {
              UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

              if (readCredentials() || showLoginForm("Login")) {
                AppFrame frame = new AppFrame();
                updateTables();
                frame.setVisible(true);
              }

            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        });
  }

  /** Create the frame. */
  public AppFrame() {

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setBounds(100, 20, 450, 700);
    setTitle("Speedport Plus PhoneCalls");

    setContentPane(contentPane);

    JPanel refreshPanel = new JPanel();
    refreshPanel.setBorder(new LineBorder(new Color(0, 0, 0)));

    tabs.add(statusPanel, "Status");
    tabs.add(callsPanel, "PhoneCalls");

    GroupLayout gl_contentPane = new GroupLayout(contentPane);
    gl_contentPane.setHorizontalGroup(
        gl_contentPane
            .createParallelGroup(Alignment.LEADING)
            .addComponent(refreshPanel, GroupLayout.DEFAULT_SIZE, 434, Short.MAX_VALUE)
            .addComponent(tabs, GroupLayout.DEFAULT_SIZE, 434, Short.MAX_VALUE));
    gl_contentPane.setVerticalGroup(
        gl_contentPane
            .createParallelGroup(Alignment.LEADING)
            .addGroup(
                gl_contentPane
                    .createSequentialGroup()
                    .addComponent(
                        refreshPanel,
                        GroupLayout.PREFERRED_SIZE,
                        GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(tabs, GroupLayout.DEFAULT_SIZE, 620, Short.MAX_VALUE)));

    refreshPanel.add(btnRefresh);
    contentPane.setLayout(gl_contentPane);
    initListeners();
  }

  private void initListeners() {
    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            client.connectionPool().evictAll();
          }
        });
    btnRefresh.addActionListener(e -> updateTables());
  }

  private static boolean login() {
    RequestBody formBody =
        new FormBody.Builder()
            .add("showpw", "0")
            .add("username", username)
            .add("password", password)
            .build();
    Request loginRequest =
        new Request.Builder()
            .url("http://" + routerIp + "/data/Login.json")
            .addHeader("Accept-Language", "en")
            .post(formBody)
            .build();

    try {
      Response response = client.newCall(loginRequest).execute();
      if (response.headers().get("Set-Cookie").length() < 43) return false;

      sessionCookie = response.headers().get("Set-Cookie").substring(0, 43);

      response.body().close();
      response.close();

    } catch (IOException e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(
          null, "Failed to communicate with the Router", "Error", JOptionPane.ERROR_MESSAGE);
      System.exit(1);
      return false;
    }

    credentialsAdded = true;
    loggedInSince = Instant.now().getEpochSecond();
    return true;
  }

  @SuppressWarnings("deprecation")
  private static boolean showLoginForm(String title) {
    JTextField txtUsername = new JTextField(username);
    JPasswordField txtPassword = new JPasswordField();

    Object[] loginFields = {"Username:", txtUsername, "Password:", txtPassword};

    int option =
        JOptionPane.showConfirmDialog(null, loginFields, title, JOptionPane.OK_CANCEL_OPTION);

    if (option == JOptionPane.OK_OPTION) {
      username = txtUsername.getText();
      password = txtPassword.getText(); // no need of char array, form builder needs string
      if (!login()) {
        showLoginForm("Wrong Credentials, try again");
      } else return true;
    }
    return false;
  }

  private static void updateTables() {
    if (credentialsAdded && Instant.now().getEpochSecond() - loggedInSince > 55) {
      login();
    }
    if (credentialsAdded) {
      callsPanel.updateTable(routerIp, sessionCookie, csv);
    }
    statusPanel.updateTable(routerIp);
  }

  private static boolean readCredentials() {
    File credentialsFile = new File("login.txt");

    if (credentialsFile.exists() && credentialsFile.length() > 0) {

      try (BufferedReader br = new BufferedReader(new FileReader(credentialsFile))) {
        String line = "";

        while ((line = br.readLine()) != null) {
          if (line.startsWith("username=")) username = line.substring("username=".length());
          else if (line.startsWith("password=")) password = line.substring("password=".length());
          else if (line.startsWith("ip=")) routerIp = line.substring("ip=".length());
          else if (line.startsWith("csv=true")) csv = true;
        }

      } catch (FileNotFoundException e) {
        return false;
      } catch (IOException e) {
        e.printStackTrace();
        return false;
      }

    } else return false;

    return (login());
  }
}
