package io.github.kambaa.javafxdemo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

class Utils {
  private Utils() {
  }

  public static String getJavaVersion(File javaExecutable) {
    try {
      ProcessBuilder builder = new ProcessBuilder(javaExecutable.getAbsolutePath(), "-version");
      builder.redirectErrorStream(true); // combine stderr and stdout
      Process process = builder.start();

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.toLowerCase().contains("version")) {
            // e.g. java version "1.8.0_381" or openjdk version "11.0.22"
            String[] parts = line.split("\"");
            if (parts.length >= 2) {
              return parts[1]; // The version string
            }
          }
        }
      }

      process.waitFor();
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
    return null;
  }
}
