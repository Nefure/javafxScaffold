module org.nefure.fxscaffold {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;

    requires transitive org.slf4j;
    requires org.yaml.snakeyaml;
    requires javafx.graphics;

    opens org.nefure.fxscaffold to javafx.fxml;
    opens org.nefure.fxscaffold.container to javafx.fxml;
    exports org.nefure.fxscaffold;
    exports org.nefure.fxscaffold.annotion;
    exports org.nefure.fxscaffold.container;
}