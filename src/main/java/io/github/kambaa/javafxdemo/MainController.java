package io.github.kambaa.javafxdemo;

import static io.github.kambaa.javafxdemo.Utils.getJavaVersion;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
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

  private File javaExecutable;
  private File keytoolExecutable;
  private File certFile;
  private File cacertFile;

  @FXML
  private void handleSelectJdkDirectory() {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle("Choose a directory");

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

    textArea.appendText("Selected Dir: " + selectedDirectory.getAbsolutePath());

    boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

    File javaExe = new File(selectedDirectory, "bin/" + (isWindows ? "java.exe" : "java"));
    File keytoolExe = new File(selectedDirectory, "bin/" + (isWindows ? "keytool.exe" : "keytool"));

    if (!javaExe.exists() || !javaExe.canExecute()) {
      textArea.appendText(System.lineSeparator() + "❌ java executable not found or not executable.");
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
      textArea.appendText(System.lineSeparator() + "✅ All good! 'cacerts' found at: " + cacerts.getAbsolutePath());
      jdkDirButton.setDisable(true);
      jdkDirButton.setText("✅ " + jdkDirButton.getText());
      cacertFile = cacerts;
      isReadyForOperation();
    } else {
      textArea.appendText(System.lineSeparator() + "❌ 'cacerts' not found or writable: " + cacerts.getAbsolutePath());
    }

  }

  @FXML
  private void handleSelectCertFile() {
    FileChooser certFileChooser = new FileChooser();
    certFileChooser.setTitle("Choose a certificate file");
    certFileChooser.getExtensionFilters().addAll(
        new FileChooser.ExtensionFilter("Certificate Files", "*.crt"));
    File selectedFile = certFileChooser.showOpenDialog(primaryStage);
    if (selectedFile != null && selectedFile.exists() && selectedFile.canRead()) {
      certFile = selectedFile;
      textArea.appendText(System.lineSeparator() + "✅ Found Certificate File: " + selectedFile.getAbsolutePath());
      certFileSelectButton.setDisable(true);
      certFileSelectButton.setText("✅ " + certFileSelectButton.getText());
      isReadyForOperation();
    } else {
      textArea.appendText(System.lineSeparator() + "❌ No Certificate File selected");
    }
  }

  private static boolean checkDirExists(File file) {
    return null != file && file.isDirectory();
  }

  private void isReadyForOperation() {
    if (jdkDirButton.isDisabled() && certFileSelectButton.isDisabled()) {
      doOperationButton.setVisible(true);
      storePasswordField.setVisible(true);
    }
    resetButton.setVisible(true);
  }

  @FXML
  private void handleReset() {
    cacertFile = null;
    certFile = null;
    textArea.setText("");
    keytoolExecutable = null;
    doOperationButton.setVisible(false);
    storePasswordField.setVisible(false);
    certFileSelectButton.setText(certFileSelectButton.getText().replace("✅ ", ""));
    certFileSelectButton.setDisable(false);
    jdkDirButton.setText(jdkDirButton.getText().replace("✅ ", ""));
    jdkDirButton.setDisable(false);
    resetButton.setVisible(false);
  }

  @FXML
  private void handleCertSaveToTrustStore() {
    String password = storePasswordField.getText();
    if (null == password || password.isEmpty()) {
      password = "changeit";
    }
    // Detect OS and adjust command accordingly
    boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
    String keytoolCommand = this.keytoolExecutable.getAbsolutePath();

    String fileName = this.certFile.getName(); // Get full file name with extension
    int lastDotIndex = fileName.lastIndexOf('.'); // Find last dot position
    String nameWithoutExtension = (lastDotIndex == -1) ? fileName : fileName.substring(0, lastDotIndex);

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
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          output.append(line).append("\n");
        }
      }
      int exitCode = process.waitFor();
      textArea.appendText(System.lineSeparator() + "✅ Starting keytool cert importing operation...");
      textArea.appendText(System.lineSeparator() + output);
      textArea.appendText(System.lineSeparator() + (exitCode==0 ? "✅" : "❌") + " Keytool process exited with code: " + exitCode );
    } catch (IOException | InterruptedException e) {
      textArea.appendText(System.lineSeparator() + "❌ Exception thrown when executing keytool.");
      textArea.appendText(e.getMessage());
    }
  }
}
