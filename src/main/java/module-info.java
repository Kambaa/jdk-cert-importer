module io.github.kambaa.javafxdemo {
  requires javafx.controls;
  requires javafx.fxml;

  opens io.github.kambaa.javafxdemo to javafx.fxml;
  exports io.github.kambaa.javafxdemo;
}