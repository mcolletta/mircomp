module io.github.mcolletta.mirdaw {
	requires java.desktop;
	requires java.logging;
    requires javafx.graphics;
    requires javafx.controls;
	requires javafx.media;
	requires javafx.swing;
    requires javafx.fxml;

    requires org.apache.groovy;

	// requires org.graalvm.polyglot;
	// requires org.graalvm.python.embedding;

	opens io.github.mcolletta.mirdaw to javafx.fxml, org.apache.groovy;

    exports io.github.mcolletta.mirdaw;	
}



