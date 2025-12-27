package io.github.kambaa.javafxdemo;

import static io.github.kambaa.javafxdemo.Utils.getJavaVersion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainController {

  private Stage primaryStage;

  public void setPrimaryStage(Stage stage) {
    this.primaryStage = stage;
  }

  @FXML
  private Button jdkDirButton;

  @FXML
  private Button certFileSelectButton;

  @FXML
  private Button doOperationButton;

  @FXML
  private PasswordField storePasswordField;

  @FXML
  private TextArea textArea;

  @FXML
  private Button resetButton;

  @FXML
  private Button testButton;

  private File javaExecutable;
  private File keytoolExecutable;
  private File certFile;
  private File cacertFile;
  private Path sslPokePath;

  public static final String JDK_DIR_BTN_DEFAULT_TEXT = "Select JDK Dir";
  public static final String CERT_FILE_BTN_DEFAULT_TEXT = "Select Certificate";

  @FXML
  private void handleSelectJdkDirectory() {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle("Choose a JDK main directory");

    // Set initial directory (optional)
    File initialDir = new File(System.getProperty("user.home"));
    if (initialDir.exists()) {
      directoryChooser.setInitialDirectory(initialDir);
    }
    File selectedDirectory = directoryChooser.showDialog(primaryStage);

    if (!checkDirExists(selectedDirectory)) {
      textArea.appendText("❌ Invalid directory given");
      return;
    }

    textArea.appendText("Selected JDK Main Dir: " + selectedDirectory.getAbsolutePath());

    boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

    File javaExe = new File(selectedDirectory, "bin/" + (isWindows ? "java.exe" : "java"));
    File keytoolExe = new File(selectedDirectory, "bin/" + (isWindows ? "keytool.exe" : "keytool"));

    if (!javaExe.exists() || !javaExe.canExecute()) {
      textArea.appendText(
          System.lineSeparator() + "❌ java executable not found or not executable.");
      return;
    }

    textArea.appendText(System.lineSeparator() + "✅ java executable found");

    if (!keytoolExe.exists() || !keytoolExe.canExecute()) {
      textArea.appendText(System.lineSeparator() + "❌ keytool not found or not executable.");
      return;
    }

    textArea.appendText(System.lineSeparator() + "✅ Detected keytool");
    keytoolExecutable = keytoolExe;
    javaExecutable = javaExe;

    String jdkVersion = getJavaVersion(javaExe);
    if (jdkVersion == null) {
      textArea.appendText(System.lineSeparator() + "❌ Could not determine Java version.");
      return;
    }

    textArea.appendText(System.lineSeparator() + "✅ Detected JDK version: " + jdkVersion);

    File cacerts;
    if (jdkVersion.startsWith("1.8")) {
      cacerts = new File(selectedDirectory, "jre/lib/security/cacerts");
    } else {
      cacerts = new File(selectedDirectory, "lib/security/cacerts");
    }

    if (cacerts.exists() && cacerts.canWrite()) {
      textArea.appendText(
          System.lineSeparator() + "✅ All good! 'cacerts' found at: " + cacerts.getAbsolutePath());
      // jdkDirButton.setDisable(true);
      jdkDirButton.setText("✅ " + selectedDirectory.getName());
      jdkDirButton.setTooltip(new Tooltip(selectedDirectory.getAbsolutePath()));

      cacertFile = cacerts;
      isReadyForOperation();
    } else {
      textArea.appendText(System.lineSeparator() + "❌ 'cacerts' not found or writable: "
                          + cacerts.getAbsolutePath());
    }

  }

  @FXML
  private void handleSelectCertFile() {
    FileChooser certFileChooser = new FileChooser();
    certFileChooser.setTitle("Choose a certificate(*.crt) file");
    certFileChooser.getExtensionFilters().addAll(
        new FileChooser.ExtensionFilter("Certificate Files", "*.crt"));
    File selectedFile = certFileChooser.showOpenDialog(primaryStage);
    if (selectedFile != null && selectedFile.exists() && selectedFile.canRead()) {
      certFile = selectedFile;
      textArea.appendText(System.lineSeparator() + "✅ Selected Certificate File: "
                          + selectedFile.getAbsolutePath());
      // certFileSelectButton.setDisable(true);
      certFileSelectButton.setText("✅ " + selectedFile.getName());
      certFileSelectButton.setTooltip(new Tooltip(selectedFile.getAbsolutePath()));
      isReadyForOperation();
    } else {
      textArea.appendText(System.lineSeparator() + "❌ No Certificate File selected");
    }
  }

  private static boolean checkDirExists(File file) {
    return null != file && file.isDirectory();
  }

  private boolean isEverythingReadyForCertImport() {
    return null != javaExecutable &&
           null != keytoolExecutable &&
           null != certFile &&
           null != cacertFile;
  }

  private void isReadyForOperation() {
    if (isEverythingReadyForCertImport()) {
      doOperationButton.setVisible(true);
      storePasswordField.setVisible(true);
    }
    resetButton.setVisible(true);
    testButton.setVisible(true);
  }

  @FXML
  private void handleReset() {
    cacertFile = null;
    certFile = null;
    javaExecutable = null;
    keytoolExecutable = null;
    textArea.setText("");
    doOperationButton.setVisible(false);
    storePasswordField.setVisible(false);
    certFileSelectButton.setText(
        CERT_FILE_BTN_DEFAULT_TEXT);//certFileSelectButton.getText().replace("✅ ", "")
    certFileSelectButton.setDisable(false);
    jdkDirButton.setText(JDK_DIR_BTN_DEFAULT_TEXT);//jdkDirButton.getText().replace("✅ ", "")
    jdkDirButton.setDisable(false);
    resetButton.setVisible(false);
    testButton.setVisible(false);
  }

  @FXML
  private void handleCertSaveToTrustStore() {
    String password = storePasswordField.getText();
    if (null == password || password.isEmpty()) {
      password = "changeit";
    }
    String keytoolCommand = this.keytoolExecutable.getAbsolutePath();

    String fileName = this.certFile.getName(); // Get full file name with extension
    int lastDotIndex = fileName.lastIndexOf('.'); // Find last dot position
    String nameWithoutExtension =
        (lastDotIndex == -1) ? fileName : fileName.substring(0, lastDotIndex);

    String[] command = {
        keytoolCommand,
        "-importcert",
        "-file", this.certFile.getAbsolutePath(),
        "-alias", nameWithoutExtension,
        "-keystore", this.cacertFile.getAbsolutePath(),
        "-storepass", password,
        "-noprompt"
    };

    ProcessBuilder processBuilder = new ProcessBuilder(command);
    try {
      Process process = processBuilder.start();

      // Read the process output
      StringBuilder output = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          output.append(line).append("\n");
        }
      }
      int exitCode = process.waitFor();
      textArea.appendText(
          System.lineSeparator() + "✅ Starting keytool cert importing operation...");
      textArea.appendText(System.lineSeparator() + output);
      textArea.appendText(System.lineSeparator() + (exitCode == 0 ? "✅" : "❌")
                          + " Keytool process exited with code: " + exitCode);
    } catch (IOException | InterruptedException e) {
      textArea.appendText(System.lineSeparator() + "❌ Exception thrown when executing keytool.");
      textArea.appendText(e.getMessage());
    }
  }

  @FXML
  private void handleTest() {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Enter Ip/Hostname and Port To Check SSL");
    dialog.setHeaderText("i.e: github.com or google.com.tr:443");
    // dialog.setContentText("");

    Optional<String> result = dialog.showAndWait();

    result.ifPresent(url -> {

      String[] parts = url.trim().split(":");

      if (parts.length < 1) {
        textArea.appendText(
            System.lineSeparator()
            + "❌ Test Input Failed(host:port check failed). Enter ip/hostname:port");
        return;
      }

      String host = parts[0];
      int port = 443;
      try {
        port = parts.length > 1 ? Integer.parseInt(parts[1]) : port;
        if (port < 1 || port > 65535) {
          throw new NumberFormatException("port number should be between 1 and 65535");
        }
      } catch (NumberFormatException e) {
        textArea.appendText(
            System.lineSeparator()
            + "❌ Test Input Failed(Not a valid port number). Enter ip/hostname:port");
        return;
      }

      textArea.appendText(
          String.format("%s Testing SSL on selected JDK via SSLPoke on given Host: %s; Port: %d",
              System.lineSeparator(),
              host, port));

      try {
        copySSLPokeToTemp();
      } catch (IOException e) {
        textArea.appendText(
            System.lineSeparator() + "❌ Exception thrown when Copying SSLPoke Class To Temp.");
        textArea.appendText(e.getMessage());
        return;
      }

      testHostAndPortViaSslPoke(this.sslPokePath, host, port);

    });
  }

  private void testHostAndPortViaSslPoke(Path sslPokeTmpPath, String host, int port) {
    // keytool -printcert -sslserver host:port -rfc
    String[] command1 = {
        this.keytoolExecutable.getAbsolutePath(),
        "-printcert",
        "-sslserver",
        host + ":" + String.valueOf(port),
        "-rfc"
    };
    ProcessBuilder processBuilder = new ProcessBuilder(command1);
    processBuilder.redirectErrorStream(true);
    try {
      Process process = processBuilder.start();

      // Read the process output
      StringBuilder output = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          output.append(line).append("\n");
        }
      }
      int exitCode = process.waitFor();
      textArea.appendText(
          System.lineSeparator() + String.format("Getting certificate from %s:%s", host,
              port));
      textArea.appendText(
          System.lineSeparator() + String.format("Running Command: %s",
              String.join(" ", processBuilder.command())));
      textArea.appendText(System.lineSeparator() + (exitCode == 0 ? "✅" : "❌")
                          + " Getting certificate process exited with code: " + exitCode);
      textArea.appendText(System.lineSeparator() + output);

    } catch (IOException | InterruptedException e) {
      textArea.appendText(
          System.lineSeparator() + "❌ Exception thrown when getting certificates.");
      textArea.appendText(e.getMessage());
      return;
    }

    // java -cp <that-directory> SSLPoke host port
    String[] command = {
        this.javaExecutable.getAbsolutePath(),
        "-cp", sslPokeTmpPath.toString(),
        "SSLPoke",
        host,
        String.valueOf(port)
    };
    processBuilder = new ProcessBuilder(command);
    processBuilder.redirectErrorStream(true);
    try {
      Process process = processBuilder.start();

      // Read the process output
      StringBuilder output = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          output.append(line).append("\n");
        }
      }
      int exitCode = process.waitFor();
      textArea.appendText(
          System.lineSeparator() + String.format("✅ Starting java SSLPoke %s %s", host, port));
      textArea.appendText(
          System.lineSeparator() + String.format("Running Command: %s",
              String.join(" ", processBuilder.command())));
      textArea.appendText(System.lineSeparator() + (exitCode == 0 ? "✅" : "❌")
                          + " SSLPoke process exited with code: " + exitCode);
      textArea.appendText(System.lineSeparator() + output);

    } catch (IOException | InterruptedException e) {
      textArea.appendText(System.lineSeparator() + "❌ Exception thrown when executing SSLPoke.");
      textArea.appendText(e.getMessage());
    }
  }

  public void copySSLPokeToTemp() throws IOException {
    if (null != this.sslPokePath) {
      return;
    }
    // 1️⃣ Load from resources
    InputStream in = getClass().getResourceAsStream("/SSLPoke.class.file");

    if (in == null) {
      throw new FileNotFoundException("SSLPoke.class.file not found in resources");
    }

    // 2️⃣ Create temp directory
    Path tempDir = Files.createTempDirectory("sslpoke");

    // 3️⃣ Copy to filesystem
    Path target = tempDir.resolve("SSLPoke.class");
    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);

    tempDir.toFile().deleteOnExit();
    tempDir.resolve("SSLPoke.class").toFile().deleteOnExit();
    this.sslPokePath = tempDir;
  }
}
