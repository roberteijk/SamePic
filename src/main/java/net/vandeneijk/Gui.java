package net.vandeneijk;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;


public class Gui extends Application {

    private static TreeItem<String> mTiRoot;
    private boolean mExampleTextFieldValid = false;
    private boolean mSearchTextFieldValid = false;
    private Button mBtnStartStop;
    private TextField mTfExamplePath;
    private TextField mTfSearchPath;
    private Button mBtnExampleFile;
    private Button mBtnExampleFolder;
    private Button mBtnSearchFolder;
    private Button mBtnClear;
    private CheckBox mCbxTraverseExamplePath;
    private CheckBox mCbxTraverseSearchPath;
    private CheckBox mCbxDuplicatesWithoutExample;

    private static boolean sProcessingActivated = false;

    public void startGui() {
        Application.launch();
    }

    /**
     * GUI setup in this method. The layout of this method is from small/child objects to larger/parent objects.
     * @param primaryStage
     */

    public void start(Stage primaryStage) {
        // --------------------------------------------------------------------
        // Default GUI values.

        final double DEFAULT_INSERTS = 10;
        final Insets DEFAULT_PADDING = new Insets(DEFAULT_INSERTS,DEFAULT_INSERTS,DEFAULT_INSERTS,DEFAULT_INSERTS);
        final Insets DEFAULT_PADDING_WITHOUT_TOP_BOTTOM = new Insets(0,DEFAULT_INSERTS,0,DEFAULT_INSERTS);
        final double SCENE_WIDTH = 1024;
        final double SCENE_HEIGHT = 768;
        final double BUTTON_WIDTH = 102;
        final double BUTTON_HEIGHT = 25;
        final double BUTTON_H_GAP = 5;
        final double TEXT_FIELD_HEIGHT = 25;
        final double V_GAP = 10;



        // --------------------------------------------------------------------
        // GUI file and path selector section nodes from here.

        mTfExamplePath = getTextField("example path", (SCENE_WIDTH / 2) - (2 * DEFAULT_INSERTS), TEXT_FIELD_HEIGHT);
        mTfExamplePath.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER)  {
                try {
                    Robot robot = new Robot();
                    robot.keyPress(java.awt.event.KeyEvent.VK_TAB);
                    robot.keyRelease(java.awt.event.KeyEvent.VK_TAB);
                } catch (AWTException awtEx) {
                    awtEx.printStackTrace(); // TODO Better exception handling.
                }
            }
        });
        mTfExamplePath.focusedProperty().addListener((ov, oldV, newV) -> {
            if (!newV) { // focus lost
                File file = new File(mTfExamplePath.getText());
                if (!file.exists()) {
                    mTfExamplePath.setStyle("-fx-text-fill: red;");
                    mExampleTextFieldValid = false;
                    mBtnStartStop.setDisable(true);
                }
                else mExampleTextFieldValid = true;
                selectorAndOptionsGuiFeedback();
            } else {
                mTfExamplePath.setStyle("-fx-text-fill: black;");
            }
        });


        mTfSearchPath = getTextField("search path", (SCENE_WIDTH / 2) - (2 * DEFAULT_INSERTS), TEXT_FIELD_HEIGHT);
        mTfSearchPath.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER)  {
                try {
                    Robot robot = new Robot();
                    robot.keyPress(java.awt.event.KeyEvent.VK_TAB);
                    robot.keyRelease(java.awt.event.KeyEvent.VK_TAB);
                } catch (AWTException awtEx) {
                    awtEx.printStackTrace(); // TODO Better exception handling.
                }
            }
        });
        mTfSearchPath.focusedProperty().addListener((ov, oldV, newV) -> {
            if (!newV) { // focus lost
                File file = new File(mTfSearchPath.getText());
                if (!file.exists()) {
                    mTfSearchPath.setStyle("-fx-text-fill: red;");
                    mSearchTextFieldValid = false;
                    mBtnStartStop.setDisable(true);
                }
                else mSearchTextFieldValid = true;
                selectorAndOptionsGuiFeedback();
            } else {
                mTfSearchPath.setStyle("-fx-text-fill: black;");
            }
        });



        mBtnExampleFile = getButton("Example File", BUTTON_WIDTH, BUTTON_HEIGHT);
        class BtnExampleFile {
            private void btnPress() {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Open Example File");
                File file = fileChooser.showOpenDialog(primaryStage);
                if (file != null) {
                    mTfExamplePath.setText(file.toString());
                    mExampleTextFieldValid = true;
                } else mExampleTextFieldValid = false;
                selectorAndOptionsGuiFeedback();
                mTfExamplePath.setStyle("-fx-text-fill: black;");
            }
        }
        mBtnExampleFile.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                new BtnExampleFile().btnPress();
            }
        });
        mBtnExampleFile.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE)  {
                    new BtnExampleFile().btnPress();
                }
            }
        });

        mBtnExampleFolder = getButton("Example Folder", BUTTON_WIDTH, BUTTON_HEIGHT);
        class BtnExampleFolder {
            private void btnPress() {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Open Example Folder");
                File file = directoryChooser.showDialog(primaryStage);
                if (file != null) {
                    mTfExamplePath.setText(file.toString());
                    mExampleTextFieldValid = true;
                } else mExampleTextFieldValid = false;
                selectorAndOptionsGuiFeedback();
                mTfExamplePath.setStyle("-fx-text-fill: black;");
            }
        }
        mBtnExampleFolder.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                new BtnExampleFolder().btnPress();
            }
        });
        mBtnExampleFolder.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE)  {
                    new BtnExampleFolder().btnPress();
                }
            }
        });

        mBtnSearchFolder = getButton("Search Folder", BUTTON_WIDTH, BUTTON_HEIGHT);
        class BtnSearchFolder {
            private void btnPress(){
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Open Search Folder");
                File file = directoryChooser.showDialog(primaryStage);
                if (file != null) {
                    mTfSearchPath.setText(file.toString());
                    mSearchTextFieldValid = true;
                } else mSearchTextFieldValid = false;
                selectorAndOptionsGuiFeedback();
                mTfSearchPath.setStyle("-fx-text-fill: black;");
            }
        }
        mBtnSearchFolder.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                new BtnSearchFolder().btnPress();
            }
        });
        mBtnSearchFolder.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE)  {
                    new BtnSearchFolder().btnPress();
                }
            }
        });

        GridPane gpSelectorButtonRow = getHorizontalStackedGridPane(mBtnExampleFile, mBtnExampleFolder, mBtnSearchFolder);
        gpSelectorButtonRow.setMinSize(BUTTON_WIDTH * 3 + BUTTON_H_GAP * 2, BUTTON_HEIGHT);
        gpSelectorButtonRow.setMaxSize(BUTTON_WIDTH * 3 + BUTTON_H_GAP * 2, BUTTON_HEIGHT);
        gpSelectorButtonRow.setHgap(BUTTON_H_GAP);
        gpSelectorButtonRow.setAlignment(Pos.CENTER_LEFT);



        mBtnClear = getButton("Clear", BUTTON_WIDTH /2, BUTTON_HEIGHT);
        class BtnClear {
            private void btnPress() {
                mTfExamplePath.setText("");
                mExampleTextFieldValid = false;
                mTfSearchPath.setText("");
                mSearchTextFieldValid = false;
                mBtnStartStop.setDisable(true);
                // TODO Also clear tree and thumbnail sections.
            }
        }
        mBtnClear.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                new BtnClear().btnPress();
            }
        });
        mBtnClear.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER  || event.getCode() == KeyCode.SPACE)  {
                    new BtnClear().btnPress();
                }
            }
        });

        mBtnStartStop = getButton("Start", BUTTON_WIDTH, BUTTON_HEIGHT);
        mBtnStartStop.setDisable(true);
        class BtnStartStopResume {
            private void btnPress() {
                sProcessingActivated = !sProcessingActivated;
                if (sProcessingActivated) {
                    selectorAndOptionsGuiFeedback();

                    File examplePath = new File(mTfExamplePath.getText());
                    File searchPath = new File(mTfSearchPath.getText());
                    boolean traverseExamplePath = mCbxTraverseExamplePath.isSelected();
                    boolean traverseSearchPath = mCbxTraverseSearchPath.isSelected();
                    boolean duplicatesWithoutExample = mCbxDuplicatesWithoutExample.isSelected();

                    Thread processController = new Thread(new ProcessController(examplePath, searchPath, traverseExamplePath, traverseSearchPath, duplicatesWithoutExample));
                    processController.setDaemon(true);
                    processController.start();
                } else {
                    selectorAndOptionsGuiFeedback();
                }
            }
        }
        mBtnStartStop.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                new BtnStartStopResume().btnPress();
            }
        });
        mBtnStartStop.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER  || event.getCode() == KeyCode.SPACE)  {
                    new BtnStartStopResume().btnPress();
                }
            }
        });

        GridPane gpOperationButtonRow = getHorizontalStackedGridPane(mBtnClear, mBtnStartStop);
        gpOperationButtonRow.setMinSize(BUTTON_WIDTH * 1.5 + BUTTON_H_GAP, BUTTON_HEIGHT);
        gpOperationButtonRow.setMaxSize(BUTTON_WIDTH * 1.5 + BUTTON_H_GAP, BUTTON_HEIGHT);
        gpOperationButtonRow.setHgap(BUTTON_H_GAP);
        gpOperationButtonRow.setAlignment(Pos.CENTER_RIGHT);



        GridPane gpAllButtonsRow = getHorizontalStackedGridPane(gpSelectorButtonRow, gpOperationButtonRow);
        gpAllButtonsRow.setHgap(SCENE_WIDTH / 2 - DEFAULT_INSERTS * 2 - BUTTON_WIDTH * 4.5 - BUTTON_H_GAP * 3);



        GridPane gpSelectorSection = getVerticalStackedGridPane(mTfExamplePath, mTfSearchPath, gpAllButtonsRow);
        gpSelectorSection.setPadding(DEFAULT_PADDING);
        gpSelectorSection.setVgap(V_GAP);



        // --------------------------------------------------------------------
        // GUI options section nodes from here.

        mCbxTraverseExamplePath = new CheckBox("Traverse example path");
        mCbxTraverseExamplePath.setSelected(true);
        mCbxTraverseSearchPath = new CheckBox("Traverse search path");
        mCbxTraverseSearchPath.setSelected(true);
        mCbxDuplicatesWithoutExample = new CheckBox("Search for duplicates without example.");
        mCbxDuplicatesWithoutExample.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                selectorAndOptionsGuiFeedback();
            }
        });

        GridPane gpOptionsColumn1 = getVerticalStackedGridPane(mCbxTraverseExamplePath, mCbxTraverseSearchPath, mCbxDuplicatesWithoutExample);
        gpOptionsColumn1.setMinSize(SCENE_WIDTH / 4, TEXT_FIELD_HEIGHT * 2 + V_GAP * 2 + DEFAULT_INSERTS * 2 + BUTTON_HEIGHT);
        gpOptionsColumn1.setMaxSize(SCENE_WIDTH / 4, TEXT_FIELD_HEIGHT * 2 + V_GAP * 2 + DEFAULT_INSERTS * 2 + BUTTON_HEIGHT);
        gpOptionsColumn1.setPadding(DEFAULT_PADDING);
        gpOptionsColumn1.setVgap(5);



        CheckBox cbxOption4 = new CheckBox("Test Option 4");
        CheckBox cbxOption5 = new CheckBox("Test Option 5");
        CheckBox cbxOption6 = new CheckBox("Test Option 6");

        GridPane gpOptionsColumn2 = getVerticalStackedGridPane(cbxOption4, cbxOption5, cbxOption6);
        gpOptionsColumn2.setMinSize(SCENE_WIDTH / 4, TEXT_FIELD_HEIGHT * 2 + V_GAP * 2 + DEFAULT_INSERTS * 2 + BUTTON_HEIGHT);
        gpOptionsColumn2.setMaxSize(SCENE_WIDTH / 4, TEXT_FIELD_HEIGHT * 2 + V_GAP * 2 + DEFAULT_INSERTS * 2 + BUTTON_HEIGHT);
        gpOptionsColumn2.setPadding(DEFAULT_PADDING);
        gpOptionsColumn2.setVgap(5);



        GridPane gpOptionsSection = getHorizontalStackedGridPane(gpOptionsColumn1, gpOptionsColumn2);



        // --------------------------------------------------------------------
        // GUI gpSelectorSection and gpOptionsSection are coupled in a horizontal GridPane.

        GridPane gpSelectorAndOptions = getHorizontalStackedGridPane(gpSelectorSection, gpOptionsSection);



        // --------------------------------------------------------------------
        // GUI tree section from here. TODO Move out of testing phase.

        mTiRoot = new TreeItem<>("Found results");
        mTiRoot.setExpanded(true);

        TreeItem<String> tiExample1 = new TreeItem<>("Example 1");

        TreeItem<String> tiSubExampleA = new TreeItem<>("Subexample A");

        TreeItem<String> tiSubExampleB = new TreeItem<>("Subexample B");

        tiExample1.getChildren().addAll(tiSubExampleA, tiSubExampleB);

        mTiRoot.getChildren().add(tiExample1);

        TreeItem<String> tiExample2 = new TreeItem<>("Example 2");

        mTiRoot.getChildren().add(tiExample2);

        TreeItem<String> tiExample3 = new TreeItem<>("Example 3");

        mTiRoot.getChildren().add(tiExample3);


        TreeView<String> tvResults = new TreeView<>(mTiRoot);
        double heightGpSelectorAndOptions = (V_GAP * 2) + (DEFAULT_INSERTS * 2) + (TEXT_FIELD_HEIGHT * 3) + BUTTON_HEIGHT;
        tvResults.setMinSize(SCENE_WIDTH / 2 - 20, SCENE_HEIGHT - heightGpSelectorAndOptions);

        FlowPane fpResultTree = new FlowPane();
        fpResultTree.getChildren().add(tvResults);



        // --------------------------------------------------------------------
        // GUI thumbnail in a ScrollPane and result management sections from here. TODO Move out of testing phase.

        FlowPane flwpThumbnails = new FlowPane();
        flwpThumbnails.setOrientation(Orientation.HORIZONTAL);
        flwpThumbnails.setAlignment(Pos.TOP_LEFT);
        flwpThumbnails.setStyle("-fx-background-color: lightgrey");
        flwpThumbnails.setMinSize(SCENE_WIDTH / 2 - 25, SCENE_HEIGHT - heightGpSelectorAndOptions);
        flwpThumbnails.setHgap(5);
        flwpThumbnails.setVgap(5);

        for (int i = 0; i < 16; i++) { // TODO Remove test object generation routine.
            try {
                File file = new File("D:\\UserFiles\\Robert\\OneDrive\\Coding_SymbLink\\Current\\SamePic\\src\\main\\resources\\IMG_2352-59[EC1V1].jpg");
                URL url = file.toURI().toURL();
                flwpThumbnails.getChildren().add(getThumbnail(new Image(url.toString(), 158, 158, true, false)));
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            }
        }



        ScrollPane sclpThumbnails = new ScrollPane(flwpThumbnails);
        sclpThumbnails.setStyle("-fx-background-color: lightgrey");

        GridPane gpThumbnails = getHorizontalStackedGridPane(sclpThumbnails);
        gpThumbnails.setMinSize(SCENE_WIDTH / 2 - 10, heightGpSelectorAndOptions);

        sclpThumbnails.prefWidthProperty().bind(gpThumbnails.widthProperty());
        sclpThumbnails.prefHeightProperty().bind(gpThumbnails.heightProperty());



        // --------------------------------------------------------------------
        // GUI tree and thumbnail sections are coupled here.

        GridPane gpResults = getHorizontalStackedGridPane(fpResultTree, gpThumbnails);
        gpResults.setPadding(DEFAULT_PADDING_WITHOUT_TOP_BOTTOM);
        gpResults.setHgap(10);




        // --------------------------------------------------------------------
        // GUI status bar from here. // TODO Move out of testing phase.

        Label lblStatusBar = new Label("Status updates here. Loading... ;)");
        lblStatusBar.setMinSize(SCENE_WIDTH - DEFAULT_INSERTS * 2, TEXT_FIELD_HEIGHT);

        GridPane gpStatusBar = getVerticalStackedGridPane(lblStatusBar);
        gpStatusBar.setPadding(DEFAULT_PADDING_WITHOUT_TOP_BOTTOM);



        // --------------------------------------------------------------------
        // GUI final coupling of all sections.

        GridPane primaryRootNode = getVerticalStackedGridPane(gpSelectorAndOptions, gpResults, gpStatusBar);
        primaryRootNode.setMinSize(SCENE_WIDTH, SCENE_HEIGHT);

        Scene primaryScene = new Scene(primaryRootNode,SCENE_WIDTH,SCENE_HEIGHT);

        primaryStage.setTitle("SamePic v.0.1.0");

        primaryStage.setScene(primaryScene);
        primaryStage.sizeToScene();
        primaryStage.show();
        primaryStage.setMinWidth(primaryStage.getWidth());
        primaryStage.setMinHeight(primaryStage.getHeight());
    }



    // ========================================================================
    // ========================================================================
    // From here helper methods for building the GUI

    private Button getButton(String text, double width, double height) {
        Button button = new Button();
        button.setMinSize(width, height);
        button.setMaxSize(width, height);
        button.setTextAlignment(TextAlignment.CENTER);
        button.setText(text);

        return button;
    }

    private Label getLabel(String text, double width, double height, Pos alignment) {
        Label label = new Label();
        label.setMinSize(width, height);
        label.setMaxSize(width, height);
        label.setAlignment(alignment);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setText(text);

        return label;
    }

    private TextField getTextField(String text, double width, double height) {
        Tooltip tooltip = new Tooltip(text);

        TextField textField = new TextField();
        textField.setMinSize(width, height);
        textField.setMaxSize(width, height);
        //textField.setText(text);
        textField.setTooltip(tooltip);

        return textField;
    }

    private GridPane getHorizontalStackedGridPane(Node... nodes) {
        GridPane gridPane = new GridPane();
        for (int i = 0; i < nodes.length; i++) {
            gridPane.add(nodes[i], i , 0);
        }

        return gridPane;
    }

    private GridPane getVerticalStackedGridPane(Node... nodes) {
        GridPane gridPane = new GridPane();
        for (int i = 0; i < nodes.length; i++) {
            gridPane.add(nodes[i], 0 , i);
        }

        return gridPane;
    }

    private GridPane getThumbnail(Image image) {
        ImageView ivImage = new ImageView(image);
        GridPane gpImage = getHorizontalStackedGridPane(ivImage);
        gpImage.setMinSize(158, 158);
        gpImage.setMaxSize(158, 158);
        gpImage.setAlignment(Pos.CENTER);
        gpImage.setStyle("-fx-background-color: white");

        return getVerticalStackedGridPane(gpImage);
    }



    // ========================================================================
    // ========================================================================
    // From here helper methods for maintaining the GUI.

    private void selectorAndOptionsGuiFeedback() {
        if (mCbxDuplicatesWithoutExample.isSelected()) {
            if (mSearchTextFieldValid) mBtnStartStop.setDisable(false);
            else mBtnStartStop.setDisable(true);

            mBtnExampleFile.setDisable(true);
            mBtnExampleFolder.setDisable(true);
            mTfExamplePath.setDisable(true);
            mCbxTraverseExamplePath.setDisable(true);
            if (sProcessingActivated) {
                mBtnStartStop.setText("Stop");
                mTfSearchPath.setEditable(false);
                mBtnSearchFolder.setDisable(true);
                mBtnClear.setDisable(true);
            } else {
                mBtnStartStop.setText("Start");
                mTfSearchPath.setEditable(true);
                mBtnSearchFolder.setDisable(false);
                mBtnClear.setDisable(false);
            }
        } else {
            if (mExampleTextFieldValid && mSearchTextFieldValid) mBtnStartStop.setDisable(false);
            else mBtnStartStop.setDisable(true);

            mTfExamplePath.setDisable(false);
            mCbxTraverseExamplePath.setDisable(false);

            if (sProcessingActivated) {
                mBtnStartStop.setText("Stop");
                mTfExamplePath.setEditable(false);
                mTfSearchPath.setEditable(false);
                mBtnExampleFile.setDisable(true);
                mBtnExampleFolder.setDisable(true);
                mBtnSearchFolder.setDisable(true);
                mBtnClear.setDisable(true);
            } else {
                mBtnStartStop.setText("Start");
                mTfExamplePath.setEditable(true);
                mTfSearchPath.setEditable(true);
                mBtnExampleFile.setDisable(false);
                mBtnExampleFolder.setDisable(false);
                mBtnSearchFolder.setDisable(false);
                mBtnClear.setDisable(false);
            }
        }
    }


    // ========================================================================
    // ========================================================================
    // From here getters and setters for non-private use.

    public static boolean isProcessingActivated() {
        return sProcessingActivated;
    }
}
