module backup {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.base;

    requires jcifs.ng;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires static lombok;

    exports com.backup;
    exports com.backup.ui;

    opens com.backup.ui to javafx.fxml;
    opens com.backup.model to com.fasterxml.jackson.databind;
}