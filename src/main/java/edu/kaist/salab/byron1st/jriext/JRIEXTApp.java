package edu.kaist.salab.byron1st.jriext;

/**
 * Created by byron1st on 2015. 11. 13..
 */

import edu.kaist.salab.byron1st.jriext.digraph.DOTGenApp;
import edu.kaist.salab.byron1st.jriext.digraph.JRIEXTGraph;
import edu.kaist.salab.byron1st.jriext.instrumentation.C;
import edu.kaist.salab.byron1st.jriext.instrumentation.MonitoringUnit;
import edu.kaist.salab.byron1st.jriext.instrumentation.InstApp;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.objectweb.asm.ClassReader;

import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class JRIEXTApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private GridPane mainGridPane;
    private GridPane rootPane;
    private ListView<String> jarListView;
    private Label instLabel;
    private CheckBox isDebugMode;
    private int mainCount = 1;
    private boolean isCached = false;

    private File classpath;
    private ArrayList<MonitoringUnit> monitoringUnitsForComponents;
    private ArrayList<MonitoringUnit> monitoringUnitsForConnectors;
    private HashMap<String, Boolean> isComponent = new HashMap<>();
    private ObservableList<String> jarFiles = FXCollections.observableArrayList();

    @Override
    public void start(Stage primaryStage) {
        ScrollPane windowPane = new ScrollPane();
        rootPane = getGridPane();
        ColumnConstraints columnConstraints = new ColumnConstraints(Double.MIN_VALUE, 600, Double.MAX_VALUE, Priority.ALWAYS, HPos.CENTER, true);
        RowConstraints rowConstraints1 = new RowConstraints(Double.MIN_VALUE, Control.USE_COMPUTED_SIZE, Double.MAX_VALUE, Priority.ALWAYS, VPos.TOP, true); // Classpath, CSV btn row
        RowConstraints rowConstraints2 = new RowConstraints(Double.MIN_VALUE, Control.USE_COMPUTED_SIZE, Double.MAX_VALUE, Priority.ALWAYS, VPos.TOP, true); // Classpath, CSV btn row
        RowConstraints rowConstraints3 = new RowConstraints(Double.MIN_VALUE, Control.USE_COMPUTED_SIZE, Double.MAX_VALUE, Priority.ALWAYS, VPos.TOP, true); // main class row
        rootPane.getColumnConstraints().add(columnConstraints);
        rootPane.getRowConstraints().addAll(rowConstraints1, rowConstraints2, rowConstraints3);

        GridPane topGridPane = getGridPane();
        Label classpathLabel = new Label("Classpath");
        Label classpathResult = new Label("Not selected");
        Button classpathBtn = new Button("Select");
        classpathBtn.setOnAction(event -> getClasspath(classpathResult));
        Label csvFileLabel = new Label("CSV File (Components)");
        Label csvFileForComponentsResult = new Label("Not selected");
        Button csvFileBtn = new Button("Select");
        csvFileBtn.setOnAction(event -> getCSVFileForComponents(csvFileForComponentsResult));
        Label csvFileForFieldLabel = new Label("CSV File (Connectors)");
        Label csvFileForConnectorsResult = new Label("Not selected");
        Button csvFileForFieldBtn = new Button("Select");
        csvFileForFieldBtn.setOnAction(event -> getCSVFileForConnectors(csvFileForConnectorsResult));
        instLabel = new Label();
        isDebugMode = new CheckBox("Run the Debug Mode");
        Button doInstrumentationBtn = new Button("Instrument code");
        doInstrumentationBtn.setOnAction(event -> doInstrumentation());
        topGridPane.add(classpathLabel, 0, 0);
        topGridPane.add(classpathBtn, 1, 0);
        topGridPane.add(classpathResult, 2, 0);
        topGridPane.add(csvFileLabel, 0, 1);
        topGridPane.add(csvFileBtn, 1, 1);
        topGridPane.add(csvFileForComponentsResult, 2, 1);
        topGridPane.add(csvFileForFieldLabel, 0, 2);
        topGridPane.add(csvFileForFieldBtn, 1, 2);
        topGridPane.add(csvFileForConnectorsResult, 2, 2);
        topGridPane.add(isDebugMode, 0, 3);
        topGridPane.add(doInstrumentationBtn, 0, 4);
        topGridPane.add(instLabel, 1, 4);
        TitledPane topPane = new TitledPane("Classpath and CSV file", topGridPane);
        topPane.setCollapsible(false);
        rootPane.add(topPane, 0, 0);

        GridPane jarsGridPane = getGridPane();
        jarListView = new ListView<>(jarFiles);
        jarListView.setPrefSize(500, 100);
        HBox jarBtnBox = new HBox(5);
        Button addJarBtn = new Button("Add");
        addJarBtn.setOnAction(event -> addJar());
        Button removeJarBtn = new Button("Remove");
        removeJarBtn.setOnAction(event -> removeJar());
        jarBtnBox.getChildren().addAll(addJarBtn, removeJarBtn);
        jarBtnBox.setAlignment(Pos.CENTER_LEFT);
        jarsGridPane.add(jarBtnBox, 0, 0);
        jarsGridPane.add(jarListView, 0, 1);
        TitledPane jarsPane = new TitledPane("External Jars", jarsGridPane);
        jarsPane.setCollapsible(false);
        rootPane.add(jarsPane, 0, 1);

        mainGridPane = getGridPane();
        Button mainBtn = new Button("+");
        mainBtn.setOnAction(event -> addMainClass());
        mainGridPane.add(mainBtn, 0, 0);
        TitledPane mainPane = new TitledPane("Main Classes", mainGridPane);
        mainPane.setCollapsible(false);
        rootPane.add(mainPane, 0, 2);

        windowPane.setContent(rootPane);
        Scene scene = new Scene(windowPane);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void addJar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Jar file", "*.jar"));
        File jarFile = fileChooser.showOpenDialog(null);
        if(jarFile != null) jarFiles.add(jarFile.getAbsolutePath());
    }

    private void removeJar() {
        int index = jarListView.getSelectionModel().getSelectedIndex();
        if(index != -1) jarListView.getItems().remove(index);
    }

    private static void showException(Exception e, String message) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        TextArea exceptionTraceArea = new TextArea(sw.toString());
        exceptionTraceArea.setEditable(false);
        exceptionTraceArea.setWrapText(true);
        exceptionTraceArea.setMaxHeight(Double.MAX_VALUE);
        exceptionTraceArea.setMaxWidth(Double.MAX_VALUE);
        GridPane.setVgrow(exceptionTraceArea, Priority.ALWAYS);
        GridPane.setHgrow(exceptionTraceArea, Priority.ALWAYS);

        GridPane exceptionPane = new GridPane();
        exceptionPane.setMaxWidth(Double.MAX_VALUE);
        exceptionPane.add(exceptionTraceArea, 0, 0);

        Alert errorDialog = new Alert(Alert.AlertType.ERROR);
        errorDialog.setTitle("An exception occurs!");
        errorDialog.setHeaderText(e.getClass().getName());
        errorDialog.setContentText(message);
        errorDialog.getDialogPane().setExpandableContent(exceptionPane);
        errorDialog.show();
    }

    private GridPane getGridPane() {
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(5));
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        return gridPane;
    }

    /**  Controllers  **/

    private void getClasspath(Label classpathResult) {
        DirectoryChooser chooser = new DirectoryChooser();
        File classpath = chooser.showDialog(null);
        if(classpath != null) {
            classpathResult.setText(classpath.getName());
            this.classpath = classpath;
        }
    }

    private void getCSVFileForComponents(Label csvFileResult) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV file", "*.csv"));
        File csvFile = chooser.showOpenDialog(null);
        monitoringUnitsForComponents = readCSVFile(csvFileResult, csvFile);
        monitoringUnitsForComponents.forEach(monitoringUnit -> isComponent.put(monitoringUnit.getId(), true));
    }

    private void getCSVFileForConnectors(Label csvFileForConnectorsResult) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV file", "*.csv"));
        File csvFile = chooser.showOpenDialog(null);
        monitoringUnitsForConnectors = readCSVFile(csvFileForConnectorsResult, csvFile);
        monitoringUnitsForConnectors.forEach(monitoringUnit -> isComponent.put(monitoringUnit.getId(), false));
    }

    private ArrayList<MonitoringUnit> readCSVFile(Label csvFileResult, File csvFile) {
        ArrayList<MonitoringUnit> monitoringUnits = null;
        if(csvFile != null) {
            try {
                monitoringUnits = InstApp.getMonitoringUnitsFromFile(csvFile.toPath());
                csvFileResult.setText(csvFile.getName());
            } catch (IOException e) {
                showException(e, "IOException during reading the CSV file.");
            }
        }
        return monitoringUnits;
    }

    private void doInstrumentation() {
        Path cachePath = Paths.get(InstApp.defaultDirName);
        if(Files.exists(cachePath))
            try {
                Files.walkFileTree(cachePath, new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                showException(e, "An exception occurs during emptying cache.");
            }

        try {
            ArrayList<MonitoringUnit> combinedList = monitoringUnitsForComponents;
            combinedList.addAll(monitoringUnitsForConnectors);
            InstApp.getInstance().instrument(classpath.toPath(), combinedList, isDebugMode.isSelected());
            instLabel.setText("Done");
        } catch (InstApp.NotValidInputException | NullPointerException | IOException e ) {
            showException(e, "An exception occurs during the instrumentation.");
        }

        isCached = true;
    }

    private void addMainClass() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Main Class", "*.class"));
        File mainClass = chooser.showOpenDialog(null);

        if(mainClass != null) {
            try {
                TargetProgram program = new TargetProgram(mainClass);
                mainGridPane.add(program.getHBox(), 0, mainCount++);
                rootPane.requestLayout();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class TargetProgram {
        private String mainClassName;
        private String mainClassNameString;
        private HBox box;
        private Button mainBtn;
        private Button generateBtn;
        private Button viewBtn;
        private Label nameLabel;
        private Label statusLabel;
        private Process process;
        private File logFile;
        private File logTempFile;
        private File viewFile;
        private JRIEXTGraph graph = new JRIEXTGraph();

        private final static String RUN = "Run";
        private final static String STOP = "Stop";

        TargetProgram(File mainClass) throws IOException {
            mainClassName = (new ClassReader(new FileInputStream(mainClass))).getClassName();
            mainClassNameString = mainClassName.replaceAll("/", "_");
            logFile = Paths.get(System.getProperty("user.dir"), mainClassNameString + "_output.txt").toFile();
            createBox();
        }

        private void createBox() {
            box = new HBox(5);
            nameLabel = new Label(mainClassName.substring(mainClassName.lastIndexOf("/")));
            statusLabel = new Label("::IDLE::");
            mainBtn = new Button(RUN);
            mainBtn.setOnAction(event -> doAction(mainBtn.getText()));
            generateBtn = new Button("Generate");
            generateBtn.setOnAction(event -> generate());
            generateBtn.setDisable(true);
            viewBtn = new Button("View");
            viewBtn.setOnAction(event -> view());
            viewBtn.setDisable(true);

            box.getChildren().addAll(nameLabel, mainBtn, generateBtn, viewBtn, statusLabel);
            box.setAlignment(Pos.CENTER_LEFT);
        }

        private void doAction(String text) {
            switch (text) {
                case RUN: run(); break;
                case STOP: stop(); break;
            }
        }

        private void run() {
            statusLabel.setText("::RUNNING::");
            mainBtn.setText(STOP);
            generateBtn.setDisable(true);
            viewBtn.setDisable(true);
            try {
                if(isCached) {
                    Path tempLogs = Paths.get(System.getProperty("user.dir"), mainClassNameString + "_temp.txt");
                    if(Files.notExists(tempLogs))
                        Files.createFile(tempLogs);
                    logTempFile = tempLogs.toFile();

                    String xbootclasspathCmd = InstApp.defaultDirName + ":";
                    Iterator<String> iterator = jarFiles.iterator();
                    while(iterator.hasNext()) {
                        xbootclasspathCmd += iterator.next();
                        if(iterator.hasNext()) xbootclasspathCmd += ":";
                    }

                    ProcessBuilder builder = new ProcessBuilder("java",
                            "-Xbootclasspath/p:" + xbootclasspathCmd,
                            mainClassName.replaceAll("/", "."));
                    builder.directory(new File(InstApp.defaultDirName));
                    builder.redirectErrorStream(true);
                    builder.redirectOutput(logTempFile);
                    process = builder.start();
                } else
                    throw new IOException();
            } catch (IOException e) {
                showException(e, "An exception occurs during the execution.");
            }
        }

        private void stop() {
            statusLabel.setText("::PREPARED::");
            mainBtn.setText(RUN);
            generateBtn.setDisable(false);
            process.destroy();

            if(!isDebugMode.isSelected())
                refineLogTemp(logTempFile, logFile);
        }

        private void generate() {
            statusLabel.setText("::GENERATED::");
            generateBtn.setDisable(true);
            viewBtn.setDisable(false);
            try {
                if(!isDebugMode.isSelected()) {
                    graph = GraphGenApp.getInstance().generate(logFile, isComponent);
                    viewFile = DOTGenApp.getInstance().generate(graph, process.hashCode() + "_" + mainClassNameString);
                } else {
                    try(BufferedReader br = Files.newBufferedReader(logTempFile.toPath());
                        BufferedWriter bw = Files.newBufferedWriter(logFile.toPath())) {
                        String line;
                        HashSet<String> store = new HashSet<>();
                        while((line = br.readLine()) != null)
                            store.add(line.replaceAll(",", "\n\t"));
                        for (String stack : store)
                            bw.write(stack + "\n");
                    }
                }
            } catch (IOException e) {
                showException(e, "An exception orrurs during generating the graph.");
            }
        }

        private void view() {
            try {
                Desktop.getDesktop().open(viewFile);
            } catch (IOException e) {
                showException(e, "An exception occurs during opening the view file.");
            }
        }

        HBox getHBox() {
            return box;
        }
    }

    static void refineLogTemp(File logTempFile, File logFile) {
        try(BufferedReader br = Files.newBufferedReader(logTempFile.toPath());
            BufferedWriter bw = Files.newBufferedWriter(logFile.toPath())) {
            String line;
            while((line = br.readLine()) != null) {
                if(line.equals("\n")) continue;
                int index = -2;
                int lastIndex;

                if((lastIndex = line.lastIndexOf(C.ENTER)) > 0) index = lastIndex;
                else if((lastIndex = line.lastIndexOf(C.EXIT)) > 0) index = lastIndex;

                if(index != -2) bw.write(line.substring(0, index) + "\n" + line.substring(index));
                else bw.write(line + "\n");
            }

            Files.delete(logTempFile.toPath());
        } catch (IOException e) {
            showException(e, "An exception occurs during verifying logs.");
        }
    }
}
