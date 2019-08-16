/**
 * Class that builds and maintains the GUI. 'Under the hood' operations are kept out of this class as much as possible.
 *
 * @author Robert van den Eijk
 */

package net.vandeneijk;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;
import javafx.stage.*;
import javafx.scene.Cursor;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.awt.*;
import java.net.MalformedURLException;
import java.util.List;


public class Gui extends Application {

    // Default GUI values for the entire class.
    private static final double SCENE_WIDTH = 1024;
    private static final double TEXT_FIELD_HEIGHT = 25;
    private static final double DEFAULT_H_GAP = 10;
    private static final double DEFAULT_INSERTS = 10;


    // Variables related directly to the nodes for the main GUI.
    private static TextField sTfExamplePath;
    private static TextField sTfSearchPath;
    private static Button sBtnExampleFile;
    private static Button sBtnExampleFolder;
    private static Button sBtnSearchFolder;
    private static Button sBtnClear;
    private static Button sBtnStartStop;
    private static CheckBox sCbxTraverseExamplePath;
    private static CheckBox sCbxTraverseSearchPath;
    private static CheckBox sCbxDuplicatesWithoutExample;
    private static ComboBox<String> sCbAccuracy;
    private static Button sBtnApplyChanges;
    private static Label sLblApplyChangesCounter;
    private static TreeItem<PicData> sTiRoot;
    private static TreeView<PicData> sTvResults;
    private static FlowPane sFpThumbnails;
    private ContextMenu mContextMenu;
    private static Label sLblFilePath;
    private static Label sLblPictureStats;
    private static ProgressBar sPbPleaseWaitAnimation;
    private static Label sLblStatusBar;
    private static double sStatusBarWidth;
    private static GridPane sGpStatusBar;
    private Label mLblInterfaceBlockingOverlay;
    private GridPane mPrimaryRootNode;
    private static Scene sPrimaryScene;
    private static double sNodesExtraWidth;


    // Variables related directly to the nodes for the deletion dialog.
    private Stage mDialogStage;


    // Variables that help to keep track of certain program states.
    private static boolean sExampleTextFieldValid;
    private static boolean sSearchTextFieldValid;
    private static boolean sProcessingActivated;
    private static boolean sDeletionActivated;


    // Variables that store the results send by ProcessController before it becomes eligible for garbage collection.
    private static List<List<PicData>> sPicDataListList;
    private static boolean sContainsExample;


    // Variables that help smoothing out the user experience GUI wise.
    private static Map<PicData, TreeItem<PicData>> mTvHelperMapping;
    private double mSceneToPrimaryStageDifferenceWidth;
    private double mSceneToPrimaryStageDifferenceHeight;



    void startGui() {
        Application.launch();
    }

    /**
     * GUI setup in this method. The layout of this method is roughly from small/child objects to larger/parent objects.
     * @param primaryStage
     */
    public void start(Stage primaryStage) {
        // --------------------------------------------------------------------
        // Default GUI values.

        final Insets DEFAULT_PADDING_LRTB = new Insets(DEFAULT_INSERTS,DEFAULT_INSERTS,DEFAULT_INSERTS,DEFAULT_INSERTS); // LRTB = Left, Right, Top, Bottom (lowercase would be half sized).
        final Insets DEFAULT_PADDING_lRTB = new Insets(DEFAULT_INSERTS,DEFAULT_INSERTS ,DEFAULT_INSERTS,DEFAULT_INSERTS / 2);
        final Insets DEFAULT_PADDING_rTB = new Insets(DEFAULT_INSERTS,DEFAULT_INSERTS / 2,DEFAULT_INSERTS,0);
        final Insets DEFAULT_PADDING_LR = new Insets(0,DEFAULT_INSERTS,0,DEFAULT_INSERTS);
        final double SCENE_HEIGHT = 768;
        final double BUTTON_WIDTH = 102;
        final double BUTTON_HEIGHT = 25;
        final double BUTTON_H_GAP = 5;
        final double DEFAULT_V_GAP = 10;
        final double METADATA_HEIGHT = TEXT_FIELD_HEIGHT * 2;



        // --------------------------------------------------------------------
        // Other local variables at the start of the method for better overview and edibility.

        Map<String, Integer> mAccuracyMap = new TreeMap<>(Collections.reverseOrder());
        mAccuracyMap.put("Accuracy 100,0%", 0);
        mAccuracyMap.put("Accuracy  99,9%", 1);
        mAccuracyMap.put("Accuracy  99,7%", 3);
        mAccuracyMap.put("Accuracy  99,4%", 6);
        mAccuracyMap.put("Accuracy  98.8%", 12);
        mAccuracyMap.put("Accuracy  97.6%", 24);
        mAccuracyMap.put("Accuracy  95.3%", 48);
        mAccuracyMap.put("Accuracy  90,6%", 96);
        mAccuracyMap.put("Accuracy  81,2%", 192);



        // --------------------------------------------------------------------
        // GUI file/path selector and process operation section nodes from here.

        sTfExamplePath = getTextField("example path", (SCENE_WIDTH / 2) - (2 * DEFAULT_INSERTS), TEXT_FIELD_HEIGHT);
        sTfExamplePath.setOnKeyPressed(event -> {
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
        sTfExamplePath.focusedProperty().addListener((ov, oldV, newV) -> {
            if (!newV) { // focus lost
                File file = new File(sTfExamplePath.getText());
                sExampleTextFieldValid = file.exists();
                updateGuiSelectorAndOptionNodes();
            }
        });



        sTfSearchPath = getTextField("search path", (SCENE_WIDTH / 2) - (2 * DEFAULT_INSERTS), TEXT_FIELD_HEIGHT);
        sTfSearchPath.setOnKeyPressed(event -> {
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
        sTfSearchPath.focusedProperty().addListener((ov, oldV, newV) -> {
            if (!newV) { // focus lost
                File file = new File(sTfSearchPath.getText());
                sSearchTextFieldValid = file.exists();
                updateGuiSelectorAndOptionNodes();
            }
        });



        sBtnExampleFile = getButton("Example File", BUTTON_WIDTH, BUTTON_HEIGHT);
        class BtnExampleFile {
            private void btnPress() {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Open Example File");
                File file = fileChooser.showOpenDialog(primaryStage);
                if (file != null) {
                    sTfExamplePath.setText(file.toString());
                    sExampleTextFieldValid = true;
                } else sExampleTextFieldValid = false;
                updateGuiSelectorAndOptionNodes();
            }
        }
        sBtnExampleFile.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                new BtnExampleFile().btnPress();
            }
        });
        sBtnExampleFile.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE)  {
                    new BtnExampleFile().btnPress();
                }
            }
        });



        sBtnExampleFolder = getButton("Example Folder", BUTTON_WIDTH, BUTTON_HEIGHT);
        class BtnExampleFolder {
            private void btnPress() {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Open Example Folder");
                File file = directoryChooser.showDialog(primaryStage);
                if (file != null) {
                    sTfExamplePath.setText(file.toString());
                    sExampleTextFieldValid = true;
                } else sExampleTextFieldValid = false;
                updateGuiSelectorAndOptionNodes();
            }
        }
        sBtnExampleFolder.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                new BtnExampleFolder().btnPress();
            }
        });
        sBtnExampleFolder.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE)  {
                    new BtnExampleFolder().btnPress();
                }
            }
        });



        sBtnSearchFolder = getButton("Search Folder", BUTTON_WIDTH, BUTTON_HEIGHT);
        class BtnSearchFolder {
            private void btnPress(){
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Open Search Folder");
                File file = directoryChooser.showDialog(primaryStage);
                if (file != null) {
                    sTfSearchPath.setText(file.toString());
                    sSearchTextFieldValid = true;
                } else sSearchTextFieldValid = false;
                updateGuiSelectorAndOptionNodes();
            }
        }
        sBtnSearchFolder.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                new BtnSearchFolder().btnPress();
            }
        });
        sBtnSearchFolder.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE)  {
                    new BtnSearchFolder().btnPress();
                }
            }
        });



        GridPane gpSelectorButtonRow = getHorizontalStackedGridPane(sBtnExampleFile, sBtnExampleFolder, sBtnSearchFolder);
        gpSelectorButtonRow.setHgap(BUTTON_H_GAP);



        sBtnClear = getButton("Clear", BUTTON_WIDTH /2, BUTTON_HEIGHT);
        class BtnClear {
            private void btnPress() {
                sTfExamplePath.setText("");
                sExampleTextFieldValid = false;
                sTfSearchPath.setText("");
                sSearchTextFieldValid = false;
                updateGuiSelectorAndOptionNodes();
                sFpThumbnails.getChildren().clear();
                sTvResults.setVisible(false);
                sLblApplyChangesCounter.setText("0");
                sLblFilePath.setText("");
                sLblPictureStats.setText("");
            }
        }
        sBtnClear.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                new BtnClear().btnPress();
            }
        });
        sBtnClear.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER  || event.getCode() == KeyCode.SPACE)  {
                    new BtnClear().btnPress();
                }
            }
        });



        sBtnStartStop = getButton("Start", BUTTON_WIDTH, BUTTON_HEIGHT);
        sBtnStartStop.setDisable(true);
        class BtnStartStop {
            private void btnPress() {
                sProcessingActivated = !sProcessingActivated;
                if (sProcessingActivated) {
                    updateGuiSelectorAndOptionNodes();
                    sFpThumbnails.getChildren().clear();
                    sTvResults.setVisible(false);
                    sLblApplyChangesCounter.setText("0");
                    sLblFilePath.setText("");
                    sLblPictureStats.setText("");

                    File examplePath = new File(sTfExamplePath.getText());
                    File searchPath = new File(sTfSearchPath.getText());

                    if (sCbAccuracy.getSelectionModel().getSelectedIndex() == -1) sCbAccuracy.getSelectionModel().select(0);
                    int allowedDeviation = mAccuracyMap.get(sCbAccuracy.getSelectionModel().getSelectedItem());

                    boolean traverseExamplePath = sCbxTraverseExamplePath.isSelected();
                    boolean traverseSearchPath = sCbxTraverseSearchPath.isSelected();
                    boolean useExamplePath = !sCbxDuplicatesWithoutExample.isSelected();

                    Thread processController = new Thread(new ProcessController(examplePath, searchPath, allowedDeviation, traverseExamplePath, traverseSearchPath, useExamplePath));
                    processController.setDaemon(true);
                    processController.start();
                } else {
                    updateGuiSelectorAndOptionNodes();
                }
            }
        }
        sBtnStartStop.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                new BtnStartStop().btnPress();
            }
        });
        sBtnStartStop.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER  || event.getCode() == KeyCode.SPACE)  {
                    new BtnStartStop().btnPress();
                }
            }
        });



        GridPane gpOperationButtonRow = getHorizontalStackedGridPane(sBtnClear, sBtnStartStop);
        gpOperationButtonRow.setHgap(BUTTON_H_GAP);



        GridPane gpAllButtonsRow = getHorizontalStackedGridPane(gpSelectorButtonRow, gpOperationButtonRow);
        gpAllButtonsRow.setHgap((SCENE_WIDTH / 2) - (DEFAULT_INSERTS * 2) - (BUTTON_WIDTH * 4.5) - (BUTTON_H_GAP * 3));



        GridPane gpSelectorSection = getVerticalStackedGridPane(sTfExamplePath, sTfSearchPath, gpAllButtonsRow);
        gpSelectorSection.setPadding(DEFAULT_PADDING_LRTB);
        gpSelectorSection.setVgap(DEFAULT_V_GAP);



        // --------------------------------------------------------------------
        // GUI options section nodes from here.

        sCbxTraverseExamplePath = new CheckBox("Traverse example path");
        sCbxTraverseExamplePath.setSelected(true);
        sCbxTraverseSearchPath = new CheckBox("Traverse search path");
        sCbxTraverseSearchPath.setSelected(true);
        sCbxDuplicatesWithoutExample = new CheckBox("Search for duplicates without example.");
        sCbxDuplicatesWithoutExample.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                updateGuiSelectorAndOptionNodes();
            }
        });



        Label lblSpacer = new Label(); // TODO Spacer is a shortcut to create a nice distance between checkboxes and combobox. Maybe create a more resilient solution.
        lblSpacer.setMinSize(100,3);
        lblSpacer.setMaxSize(100,3);



        ObservableList<String> accuracyOptions = FXCollections.observableArrayList(mAccuracyMap.keySet());
        sCbAccuracy = new ComboBox<>(accuracyOptions);
        sCbAccuracy.setMinWidth((SCENE_WIDTH / 4) - (DEFAULT_INSERTS * 0.5));
        sCbAccuracy.setMaxWidth((SCENE_WIDTH / 4) - (DEFAULT_INSERTS * 0.5));
        sCbAccuracy.getSelectionModel().select(0);



        GridPane gpOptionsColumn1 = getVerticalStackedGridPane(sCbxTraverseExamplePath, sCbxTraverseSearchPath, sCbxDuplicatesWithoutExample, lblSpacer, sCbAccuracy);
        gpOptionsColumn1.setPadding(DEFAULT_PADDING_rTB);
        gpOptionsColumn1.setVgap(4); // Do not standardize this value.



        // --------------------------------------------------------------------
        // GUI credits and ApplyChanges from here:

        Label lblCredits = getLabel("developed by:\nRobert van den Eijk\ncopyright 2019", (SCENE_WIDTH / 4) - (DEFAULT_INSERTS * 1.5), (DEFAULT_V_GAP * 2) + (TEXT_FIELD_HEIGHT * 2) - DEFAULT_V_GAP, Pos.CENTER);
        lblCredits.setStyle("-fx-text-fill: darkgrey");



        sBtnApplyChanges = getButton("Delete Marked Pictures", (SCENE_WIDTH / 4) - (DEFAULT_INSERTS * 1.5), BUTTON_HEIGHT);
        sBtnApplyChanges.setDisable(true);
        class BtnApplyChanges {
            private void btnPress() {
                disablePrimaryGui(true);
                mDialogStage = new Stage();

                Label lblWarning = getLabel("Are you sure? All items will be permanently deleted!", 300, 50, Pos.CENTER);



                Button btnYes = getButton("Yes", BUTTON_WIDTH, BUTTON_HEIGHT);
                class BtnYes {
                    private void btnPress() {
                        Thread delete = new Thread(new Delete(sPicDataListList));
                        delete.setDaemon(false);
                        delete.start();
                        disablePrimaryGui(false);
                        sTvResults.getSelectionModel().select(0);
                        sLblFilePath.setText("");
                        sLblPictureStats.setText("");
                    }
                }
                btnYes.setOnMousePressed(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        new BtnYes().btnPress();
                    }
                });
                btnYes.setOnKeyPressed(new EventHandler<KeyEvent>() {
                    @Override
                    public void handle(KeyEvent event) {
                        if (event.getCode() == KeyCode.ENTER  || event.getCode() == KeyCode.SPACE)  {
                            new BtnYes().btnPress();
                        }
                    }
                });



                Button btnNo = getButton("No", BUTTON_WIDTH, BUTTON_HEIGHT);
                class BtnNo {
                    private void btnPress() {
                        disablePrimaryGui(false);
                    }
                }
                btnNo.setOnMousePressed(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        new BtnNo().btnPress();
                    }
                });
                btnNo.setOnKeyPressed(new EventHandler<KeyEvent>() {
                    @Override
                    public void handle(KeyEvent event) {
                        if (event.getCode() == KeyCode.ENTER  || event.getCode() == KeyCode.SPACE)  {
                            new BtnNo().btnPress();
                        }
                    }
                });



                GridPane gpDecide = getHorizontalStackedGridPane(btnYes, btnNo);
                gpDecide.setAlignment(Pos.CENTER);
                gpDecide.setHgap(25);



                GridPane dialogRootNode = getVerticalStackedGridPane(lblWarning, gpDecide);

                Scene dialogScene = new Scene(dialogRootNode,300, 100);

                mDialogStage.setScene(dialogScene);
                mDialogStage.resizableProperty().setValue(Boolean.FALSE);
                mDialogStage.show();
                mDialogStage.setOnHiding(new EventHandler<WindowEvent>() {
                    @Override
                    public void handle(WindowEvent event) {
                        disablePrimaryGui(false);
                    }
                });
            }
        }
        sBtnApplyChanges.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                new BtnApplyChanges().btnPress();
            }
        });
        sBtnApplyChanges.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER  || event.getCode() == KeyCode.SPACE)  {
                    new BtnApplyChanges().btnPress();
                }
            }
        });



        sLblApplyChangesCounter = new Label("0");
        sLblApplyChangesCounter.setDisable(true); // Because it's used in a StackPane over a button, setting it disabled keeps the button clickable. The greyed out look makes it less prominent which is nice too.



        GridPane gpApplyChanges = getHorizontalStackedGridPane(sLblApplyChangesCounter);
        gpApplyChanges.setDisable(true);
        gpApplyChanges.setAlignment(Pos.CENTER_RIGHT);
        gpApplyChanges.setPadding(DEFAULT_PADDING_LR);



        StackPane stkApplyChanges = new StackPane();
        stkApplyChanges.getChildren().addAll(sBtnApplyChanges, gpApplyChanges);



        GridPane gpOptionsColumn2 = getVerticalStackedGridPane(lblCredits, stkApplyChanges);
        gpOptionsColumn2.setPadding(DEFAULT_PADDING_lRTB);
        gpOptionsColumn2.setVgap(DEFAULT_V_GAP);



        GridPane gpOptionsSection = getHorizontalStackedGridPane(gpOptionsColumn1, gpOptionsColumn2);



        // --------------------------------------------------------------------
        // GUI gpSelectorSection and gpOptionsSection are coupled in a horizontal GridPane.

        GridPane gpSelectorAndOptions = getHorizontalStackedGridPane(gpSelectorSection, gpOptionsSection);



        // --------------------------------------------------------------------
        // GUI tree section from here.

        sTiRoot = new TreeItem<>();
        sTiRoot.setExpanded(true);



        sTvResults = new TreeView<>(sTiRoot);
        sTvResults.setVisible(false);
        sTvResults.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) setThumbnails(newValue.getValue());
        });



        double totalHeightGpSelectorAndOptionsAndStatusBar = (DEFAULT_V_GAP * 2) + (DEFAULT_INSERTS * 2) + (TEXT_FIELD_HEIGHT * 3) + BUTTON_HEIGHT;
        sTvResults.setMinSize(SCENE_WIDTH / 2 - 20, SCENE_HEIGHT - totalHeightGpSelectorAndOptionsAndStatusBar);



        FlowPane fpResultTree = new FlowPane();
        fpResultTree.getChildren().add(sTvResults);
        fpResultTree.setStyle("-fx-background-color: white");



        // --------------------------------------------------------------------
        // GUI thumbnail in a ScrollPane and result management sections from here.

        sFpThumbnails = new FlowPane();
        sFpThumbnails.setOrientation(Orientation.HORIZONTAL);
        sFpThumbnails.setMinSize(SCENE_WIDTH / 2 - 25, SCENE_HEIGHT - totalHeightGpSelectorAndOptionsAndStatusBar - METADATA_HEIGHT - DEFAULT_V_GAP);
        sFpThumbnails.setAlignment(Pos.TOP_LEFT);
        sFpThumbnails.setHgap(DEFAULT_H_GAP / 2);
        sFpThumbnails.setVgap(DEFAULT_V_GAP / 2);
        sFpThumbnails.setStyle("-fx-background-color: lightgrey");

        ScrollPane spThumbnails = new ScrollPane(sFpThumbnails);
        spThumbnails.setStyle("-fx-background-color: lightgrey");

        GridPane gpThumbnails = getHorizontalStackedGridPane(spThumbnails);
        gpThumbnails.setMinSize(SCENE_WIDTH / 2 - 10, SCENE_HEIGHT - totalHeightGpSelectorAndOptionsAndStatusBar - METADATA_HEIGHT - DEFAULT_V_GAP);

        spThumbnails.prefWidthProperty().bind(gpThumbnails.widthProperty());
        spThumbnails.prefHeightProperty().bind(gpThumbnails.heightProperty());




        // --------------------------------------------------------------------
        // GUI thumbnail metadata here.

        sLblFilePath = getLabel("", SCENE_WIDTH / 2 - 10, TEXT_FIELD_HEIGHT, Pos.CENTER);
        sLblPictureStats = getLabel("", SCENE_WIDTH / 2 - 10, TEXT_FIELD_HEIGHT, Pos.CENTER);



        GridPane gpMetaData = getVerticalStackedGridPane(sLblFilePath, sLblPictureStats);
        gpMetaData.setStyle("-fx-background-color: lightgrey");



        // --------------------------------------------------------------------
        // GUI thumbnail and metadata are coupled here.

        GridPane gpThumbnailsWithMetaData = getVerticalStackedGridPane(gpThumbnails, gpMetaData);
        gpThumbnailsWithMetaData.setVgap(DEFAULT_V_GAP);



        // --------------------------------------------------------------------
        // GUI tree and thumbnail sections are coupled here.

        GridPane gpResults = getHorizontalStackedGridPane(fpResultTree, gpThumbnailsWithMetaData);
        gpResults.setPadding(DEFAULT_PADDING_LR);
        gpResults.setHgap(DEFAULT_H_GAP);



        // --------------------------------------------------------------------
        // GUI status bar from here.

        sPbPleaseWaitAnimation = new ProgressBar();
        sPbPleaseWaitAnimation.setMinSize(0, 0);
        sPbPleaseWaitAnimation.setMaxSize(0, 0);


        sStatusBarWidth = SCENE_WIDTH - (DEFAULT_INSERTS * 2);
        sLblStatusBar = getLabel("", sStatusBarWidth, TEXT_FIELD_HEIGHT, Pos.CENTER_LEFT);


        sGpStatusBar = getHorizontalStackedGridPane(sPbPleaseWaitAnimation, sLblStatusBar);
        sGpStatusBar.setPadding(DEFAULT_PADDING_LR);



        // --------------------------------------------------------------------
        // GUI final coupling of all sections.

        mPrimaryRootNode = getVerticalStackedGridPane(gpSelectorAndOptions, gpResults, sGpStatusBar);



        mLblInterfaceBlockingOverlay = getLabel("", SCENE_WIDTH, SCENE_HEIGHT, Pos.CENTER);
        mLblInterfaceBlockingOverlay.setDisable(true);



        StackPane primaryStack = new StackPane();
        primaryStack.getChildren().addAll(mPrimaryRootNode, mLblInterfaceBlockingOverlay);



        sPrimaryScene = new Scene(primaryStack,SCENE_WIDTH,SCENE_HEIGHT);

        primaryStage.setTitle("SamePic v.1.0.0");
        primaryStage.getIcons().add(new Image("icons/SamePic48.png"));
        primaryStage.setScene(sPrimaryScene);
        primaryStage.sizeToScene();
        primaryStage.setOnCloseRequest(event -> sProcessingActivated = false);
        primaryStage.show();

        primaryStage.setMinWidth(primaryStage.getWidth());
        primaryStage.setMinHeight(primaryStage.getHeight());
        mSceneToPrimaryStageDifferenceWidth = primaryStage.getWidth() - sPrimaryScene.getWidth();
        mSceneToPrimaryStageDifferenceHeight = primaryStage.getHeight() - sPrimaryScene.getHeight();

        primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> { // Makes nodes resize correctly in width when primaryStage is resized by user.
            sNodesExtraWidth = (double) newVal - SCENE_WIDTH - mSceneToPrimaryStageDifferenceWidth;
            sFpThumbnails.setMinWidth((SCENE_WIDTH / 2 - 25) + sNodesExtraWidth);
            gpThumbnails.setMinWidth((SCENE_WIDTH / 2 - 10) + sNodesExtraWidth);
            gpThumbnails.setMaxWidth((SCENE_WIDTH / 2 - 10) + sNodesExtraWidth);
            sLblFilePath.setMinWidth((SCENE_WIDTH / 2 - 10) + sNodesExtraWidth);
            sLblFilePath.setMaxWidth((SCENE_WIDTH / 2 - 10) + sNodesExtraWidth);
            sLblPictureStats.setMinWidth((SCENE_WIDTH / 2 - 10) + sNodesExtraWidth);
            sLblPictureStats.setMaxWidth((SCENE_WIDTH / 2 - 10) + sNodesExtraWidth);
            sLblStatusBar.setMinWidth(sStatusBarWidth + sNodesExtraWidth);
            sLblStatusBar.setMaxWidth(sStatusBarWidth + sNodesExtraWidth);
        });
        primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> { // Makes nodes resize correctly in height when primaryStage is resized by user.
            double nodesExtraHeight = (double) newVal - SCENE_HEIGHT - mSceneToPrimaryStageDifferenceHeight;
            sTvResults.setMinHeight((SCENE_HEIGHT - totalHeightGpSelectorAndOptionsAndStatusBar) + (nodesExtraHeight));
            sFpThumbnails.setMinHeight((SCENE_HEIGHT - totalHeightGpSelectorAndOptionsAndStatusBar - METADATA_HEIGHT - DEFAULT_V_GAP) + (nodesExtraHeight));
            gpThumbnails.setMinHeight((SCENE_HEIGHT - totalHeightGpSelectorAndOptionsAndStatusBar - METADATA_HEIGHT - DEFAULT_V_GAP) + (nodesExtraHeight));
            gpThumbnails.setMaxHeight((SCENE_HEIGHT - totalHeightGpSelectorAndOptionsAndStatusBar - METADATA_HEIGHT - DEFAULT_V_GAP) + (nodesExtraHeight));
        });
    }



    // ========================================================================
    // ========================================================================
    // Helper methods for building the GUI:

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



    // ========================================================================
    // ========================================================================
    // Helper methods for maintaining the GUI:

    /**
     * Updates the GUI gpSelectorSection and gpOptionsSection. The nested if-statements are a branch-selector for various scenarios.
     */
    private static void updateGuiSelectorAndOptionNodes() {
        if (sDeletionActivated) { // During deletion all buttons, checkboxes and fields will be disabled.
            sTfSearchPath.setEditable(false);
            sTfExamplePath.setEditable(false);
            sBtnExampleFile.setDisable(true);
            sBtnExampleFolder.setDisable(true);
            sBtnSearchFolder.setDisable(true);
            sBtnClear.setDisable(true);
            sBtnStartStop.setDisable(true);
            sCbxTraverseExamplePath.setDisable(true);
            sCbxTraverseSearchPath.setDisable(true);
            sCbxDuplicatesWithoutExample.setDisable(true);
            sCbAccuracy.setDisable(true);
            sBtnApplyChanges.setDisable(true);
            sTvResults.setVisible(false);
            sFpThumbnails.getChildren().clear();
        } else { // When not deleting, the app will set buttons, checkboxes and text fields as needed. Read comments below for more info.
            if (sCbxDuplicatesWithoutExample.isSelected()) { // When not using an example.

                if (sSearchTextFieldValid) { // When text field with search path is valid.
                    sTfSearchPath.setStyle("-fx-text-fill: black;");
                    sBtnStartStop.setDisable(false);
                } else { // When text field with search path is NOT valid.
                    sTfSearchPath.setStyle("-fx-text-fill: red;");
                    sBtnStartStop.setDisable(true);
                }

                if (sExampleTextFieldValid) sTfExamplePath.setStyle("-fx-text-fill: black;"); // Sets the text of the example path to black if the path is valid. This is useful when coming from a situation where example path is used and paths were NOT forked (example and search path both in red).

                sTfExamplePath.setDisable(true);
                sBtnExampleFile.setDisable(true);
                sBtnExampleFolder.setDisable(true);
                sCbxTraverseExamplePath.setDisable(true);
                sCbAccuracy.setDisable(true);

                if (sProcessingActivated) { // When an instance of ProcessController exists.
                    sTfSearchPath.setEditable(false);
                    sBtnSearchFolder.setDisable(true);
                    sBtnClear.setDisable(true);
                    sBtnStartStop.setText("Stop");
                    sCbxTraverseSearchPath.setDisable(true);
                    sCbxDuplicatesWithoutExample.setDisable(true);
                } else { // When an instance of ProcessController does NOT exist.
                    sTfSearchPath.setEditable(true);
                    sBtnSearchFolder.setDisable(false);
                    sBtnClear.setDisable(false);
                    sBtnStartStop.setText("Start");
                    sCbxTraverseSearchPath.setDisable(false);
                    sCbxDuplicatesWithoutExample.setDisable(false);
                }
            } else { // When using an example.
                if (sExampleTextFieldValid && sSearchTextFieldValid) { // When text fields with example and search paths are valid.
                    if (checkIfPathsAreForks()) { // When example and search path are different forks. This is a must to prevent comparing a file against itself.
                        sTfExamplePath.setStyle("-fx-text-fill: black;");
                        sTfSearchPath.setStyle("-fx-text-fill: black;");
                        sBtnStartStop.setDisable(false);
                    } else { // When example and search path are NOT different forks.
                        sTfExamplePath.setStyle("-fx-text-fill: red;");
                        sTfSearchPath.setStyle("-fx-text-fill: red;");
                        sBtnStartStop.setDisable(true);
                    }
                } else { // When text fields with example and search paths are NOT valid.
                    if (sExampleTextFieldValid) sTfExamplePath.setStyle("-fx-text-fill: black;");
                    else sTfExamplePath.setStyle("-fx-text-fill: red;");
                    if (sSearchTextFieldValid) sTfSearchPath.setStyle("-fx-text-fill: black;");
                    else sTfSearchPath.setStyle("-fx-text-fill: red;");
                    sBtnStartStop.setDisable(true);
                }

                sTfExamplePath.setDisable(false);
                sCbAccuracy.setDisable(false);

                if (sProcessingActivated) { // When an instance of ProcessController exists.
                    sTfExamplePath.setEditable(false);
                    sTfSearchPath.setEditable(false);
                    sBtnExampleFile.setDisable(true);
                    sBtnExampleFolder.setDisable(true);
                    sBtnSearchFolder.setDisable(true);
                    sBtnClear.setDisable(true);
                    sBtnStartStop.setText("Stop");
                    sCbxTraverseExamplePath.setDisable(true);
                    sCbxTraverseSearchPath.setDisable(true);
                    sCbxDuplicatesWithoutExample.setDisable(true);
                    sCbAccuracy.setDisable(true);
                } else { // When an instance of ProcessController does NOT exist.
                    sTfExamplePath.setEditable(true);
                    sTfSearchPath.setEditable(true);
                    sBtnExampleFile.setDisable(false);
                    sBtnExampleFolder.setDisable(false);
                    sBtnSearchFolder.setDisable(false);
                    sBtnClear.setDisable(false);
                    sBtnStartStop.setText("Start");
                    sCbxTraverseExamplePath.setDisable(false);
                    sCbxTraverseSearchPath.setDisable(false);
                    sCbxDuplicatesWithoutExample.setDisable(false);
                    sCbAccuracy.setDisable(false);

                    if (sCbAccuracy.getSelectionModel().getSelectedIndex() == -1) sCbAccuracy.getSelectionModel().select(0); // To prevent the accuracy combobox from resetting to a state without value.
                }
            }
        }
    }

    /**
     * Checks if the example path and the search path are different forks. If both paths are (partly) on the same fork some files will be checked
     * against itself which is undesirable.
     *
     * @return
     */
    private static boolean checkIfPathsAreForks() {
        File fileExample = new File(sTfExamplePath.getText());
        File fileSearch = new File(sTfSearchPath.getText());
        while (fileExample.getParent() != null) {
            if (fileExample.getPath().equals(fileSearch.getPath())) return false;
            fileExample = new File(fileExample.getParent());
        }

        fileExample = new File(sTfExamplePath.getText());
        fileSearch = new File(sTfSearchPath.getText());
        while (fileSearch.getParent() != null) {
            if (fileSearch.getPath().equals(fileExample.getPath())) return false;
            fileSearch = new File(fileSearch.getParent());
        }

        return true;
    }

    /**
     * This method takes an example picture in the form of a PicData parameter. It looks in sPicDataListList for a List that contains this example
     * picture. Thumbnails are made from all the PicData objects in this List with the help of the getThumbnail method. Those thumbnails are added
     * to mFpThumbnails.
     *
     * @param picData
     */
    private void setThumbnails(PicData picData) {
        List<PicData> thumbnailPicDataList = null;
        for (List<PicData> picDataList : sPicDataListList) {
            if (picDataList.contains(picData)) thumbnailPicDataList = picDataList;
        }

        sFpThumbnails.getChildren().clear();

        if (thumbnailPicDataList != null) {
            for (PicData picDataForThumbnail : thumbnailPicDataList) {
                boolean selectedPic = (picData.equals(picDataForThumbnail));
                boolean examplePic = (picDataForThumbnail.isExamplePicture());
                if (!picDataForThumbnail.isDoNotShowThumbnail()) sFpThumbnails.getChildren().add(getThumbnail(new Image(picDataForThumbnail.getUrl().toString(), 158, 158, true, false), selectedPic, examplePic, picDataForThumbnail));
            }
        }
    }

    /**
     * This method returns a GridPane containing a thumbnail and functionality to that thumbnail such as color coding, labeling and context menu.
     *
     * @param image
     * @param selectedPic
     * @param examplePic
     * @param picData
     * @return
     */
    private GridPane getThumbnail(Image image, boolean selectedPic, boolean examplePic, PicData picData) {
        ImageView ivImage = new ImageView(image);

        StackPane stack = new StackPane();
        stack.getChildren().add(ivImage);

        GridPane gpImage = getHorizontalStackedGridPane(stack);
        gpImage.setMinSize(158, 158);
        gpImage.setMaxSize(158, 158);
        gpImage.setAlignment(Pos.CENTER);
        gpImage.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                TreeItem<PicData> treeItem = mTvHelperMapping.get(picData);
                treeItem.setExpanded(true);
                sTvResults.getSelectionModel().select(treeItem);

                if (mContextMenu != null) mContextMenu.hide();



                if (event.isSecondaryButtonDown()) { // Handles the creation of a context menu.
                    MenuItem miDeleteSingle = new MenuItem();
                    if (picData.isMarkedForDeletion()) miDeleteSingle.setText("Remove mark for Deletion");
                    else miDeleteSingle.setText("Mark for Deletion");
                    miDeleteSingle.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            picData.setMarkedForDeletion(!picData.isMarkedForDeletion());
                            setThumbnails(picData);
                            setLblApplyChangesCounter();
                        }
                    });



                    SeparatorMenuItem miSeparator1 = new SeparatorMenuItem();



                    MenuItem miDeleteDuplicatesMark = new MenuItem("Mark duplicates for Deletion");
                    miDeleteDuplicatesMark.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            for (List<PicData> picDataList : sPicDataListList) {
                                if (picDataList.contains(picData)) {
                                    for (PicData picDataMark : picDataList) {
                                        if (!picDataMark.isExamplePicture()) picDataMark.setMarkedForDeletion(true);
                                        setThumbnails(picDataMark);
                                    }
                                }
                            }
                            setLblApplyChangesCounter();
                        }
                    });



                    MenuItem miDeleteDuplicatesUnmark = new MenuItem("Remove mark duplicates for Deletion");
                    miDeleteDuplicatesUnmark.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            for (List<PicData> picDataList : sPicDataListList) {
                                if (picDataList.contains(picData)) {
                                    for (PicData picDataMark : picDataList) {
                                        if (!picDataMark.isExamplePicture()) picDataMark.setMarkedForDeletion(false);
                                        setThumbnails(picDataMark);
                                    }
                                }
                            }
                            setLblApplyChangesCounter();
                        }
                    });



                    SeparatorMenuItem miSeparator2 = new SeparatorMenuItem();



                    MenuItem miDeleteDirectoryMark = new MenuItem("Mark directory for Deletion");
                    miDeleteDirectoryMark.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            Path parentPath = picData.getPath().getParent();
                            for (List<PicData> picDataList : sPicDataListList) {
                                picDataList.stream().filter(x -> x.getPath().getParent().equals(parentPath)).forEach(x -> x.setMarkedForDeletion(true));
                            }
                            setThumbnails(picData);
                            setLblApplyChangesCounter();
                        }
                    });



                    MenuItem miDeleteDirectoryUnmark = new MenuItem("Remove mark directory for Deletion");
                    miDeleteDirectoryUnmark.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            Path parentPath = picData.getPath().getParent();
                            for (List<PicData> picDataList : sPicDataListList) {
                                picDataList.stream().filter(x -> x.getPath().getParent().equals(parentPath)).forEach(x -> x.setMarkedForDeletion(false));
                            }
                            setThumbnails(picData);
                            setLblApplyChangesCounter();
                        }
                    });



                    SeparatorMenuItem miSeparator3 = new SeparatorMenuItem();



                    MenuItem miShowInExplorer = new MenuItem("Show in Viewer");
                    miShowInExplorer.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            try {
                                Desktop.getDesktop().open(picData.getPath().toFile());
                            } catch (IOException ioEx) {
                                // Ignore for now.
                            }
                        }
                    });



                    mContextMenu = new ContextMenu();

                    if (examplePic) mContextMenu.getItems().addAll(miShowInExplorer);
                    else mContextMenu.getItems().addAll(miDeleteSingle, miSeparator1, miDeleteDuplicatesMark, miDeleteDuplicatesUnmark, miSeparator2, miDeleteDirectoryMark, miDeleteDirectoryUnmark, miSeparator3, miShowInExplorer);

                    mContextMenu.show(mPrimaryRootNode, event.getScreenX(), event.getScreenY());
                }
            }
        });

        if (examplePic) { // Creates a label that is stacked on top of the thumbnail when this thumbnail is of an example picture.
            Label lblExamplePic = new Label("Example Pic");
            lblExamplePic.setRotate(30);
            lblExamplePic.setStyle("-fx-background-color: palegreen; -fx-font-size: 30; -fx-effect: dropshadow(three-pass-box, black, 10, 0, 0, 0);");

            stack.getChildren().add(lblExamplePic);

            gpImage.setOnMouseEntered(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    lblExamplePic.setVisible(false);
                }
            });
            gpImage.setOnMouseExited(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    lblExamplePic.setVisible(true);
                }
            });
        }

        if (selectedPic) { // Creates color indication that this picture is selected. It also displays some metadata of the selected picture.
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    sLblFilePath.setText(picData.getPath().toString());
                    String match = String.format("%.2f", picData.getAccuracy());
                    sLblPictureStats.setText("Dimensions : " + picData.getPictureWidth() + "x" + picData.getPictureHeight() + "   Size : " + picData.getPictureFileSize()/1024 + " kB   Match : " + "  " + match + " %");
                }
            });

            if (picData.isMarkedForDeletion()) gpImage.setStyle("-fx-background-color: red");
            else gpImage.setStyle("-fx-background-color: white");
        } else { // Creates color indication that this picture is NOT selected.
            if (picData.isMarkedForDeletion()) gpImage.setStyle("-fx-background-color: darkred");
            else gpImage.setStyle("-fx-background-color: grey");
        }

        return getVerticalStackedGridPane(gpImage);
    }

    /**
     * This method builds a tree with example pictures that can be clicked. It will be called by ProcessController after it has found results or after
     * deletion of marked pictures.
     *
     * @param pictureListList
     * @param containsExample
     */
    static void setTree(List<List<PicData>> pictureListList, boolean containsExample) {
        if (pictureListList.size() == 0) sTvResults.setVisible(false); // Prevents a know Java bug from showing up in the GUI in the form of an orphan triangle.

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                sPicDataListList = pictureListList;
                sContainsExample = containsExample;
                mTvHelperMapping = new HashMap<>();
                sTiRoot.getChildren().clear();
                sTvResults.setVisible(true);



                if (containsExample) { // If containsExample, the tree will be build with example pictures which will also be shown as thumbnail.
                    for (List<PicData> picDataList : pictureListList) {
                        TreeItem<PicData> firstLevel = new TreeItem<>();

                        for (int i = 0; i < picDataList.size(); i++) {
                            if (i == 0) {
                                firstLevel = new TreeItem<>(picDataList.get(0));
                                mTvHelperMapping.put(firstLevel.getValue(), firstLevel);
                            } else {
                                TreeItem<PicData> secondLevel = new TreeItem<>(picDataList.get(i));
                                firstLevel.getChildren().add(secondLevel);
                                mTvHelperMapping.put(secondLevel.getValue(), secondLevel);
                            }
                        }

                        sTiRoot.getChildren().add(firstLevel);
                    }
                } else { // When no example, the tree will be build with an artificial example picture to be used as handle. This handle will NOT be shown as thumbnail.
                    int count = 1;

                    for (List<PicData> picDataList : pictureListList) {
                        TreeItem<PicData> firstLevel;
                        try {
                            PicData administrativeExamplePic = new PicData(Paths.get(picDataList.get(0).getPath().toString()), false, true);
                            picDataList.add(0, administrativeExamplePic);
                            firstLevel = new TreeItem<>(administrativeExamplePic);
                            mTvHelperMapping.put(firstLevel.getValue(), firstLevel);
                        } catch (MalformedURLException mfuEx) {
                            continue;
                        }

                        for (int i = 1; i < picDataList.size(); i++) {
                            TreeItem<PicData> secondLevel = new TreeItem<>(picDataList.get(i));
                            firstLevel.getChildren().add(secondLevel);
                            mTvHelperMapping.put(secondLevel.getValue(), secondLevel);
                        }

                        sTiRoot.getChildren().add(firstLevel);
                    }
                }
            }
        });
    }

    /**
     * Updates a counter stacked on mBtnApplyChanges that indicates the number of files marked for deletion.
     */
    private static void setLblApplyChangesCounter() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                for (List<PicData> picDataList : sPicDataListList) {
                    for (PicData picDataCounter : picDataList) {
                        if (picDataCounter.isMarkedForDeletion() && !picDataCounter.isDoNotShowThumbnail()) count++;
                    }
                }
                sLblApplyChangesCounter.setText("" + count);
                if (count > 0) sBtnApplyChanges.setDisable(false);
                else sBtnApplyChanges.setDisable(true);
            }
        });
    }

    /**
     * Creates or stoppes an animation in the status bar that indicates there is something being processed in the background. It also changes the
     * cursor correspondingly.
     *
     * @param showPleaseWaitAnimation
     */
    static void showPleaseWaitAnimation(boolean showPleaseWaitAnimation) {
        if (showPleaseWaitAnimation) {
            sPbPleaseWaitAnimation.setMinSize(TEXT_FIELD_HEIGHT / 1.5, TEXT_FIELD_HEIGHT / 1.5);
            sPbPleaseWaitAnimation.setMaxSize(TEXT_FIELD_HEIGHT / 1.5, TEXT_FIELD_HEIGHT / 1.5);
            sGpStatusBar.setHgap(DEFAULT_H_GAP);
            sStatusBarWidth = SCENE_WIDTH - (DEFAULT_INSERTS * 2) - (TEXT_FIELD_HEIGHT / 1.5) - DEFAULT_H_GAP;
            sLblStatusBar.setMinWidth(sStatusBarWidth + sNodesExtraWidth);
            sLblStatusBar.setMaxWidth(sStatusBarWidth + sNodesExtraWidth);

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    sPrimaryScene.setCursor(Cursor.WAIT);
                }
            });

        } else {
            sPbPleaseWaitAnimation.setMinSize(0, 0);
            sPbPleaseWaitAnimation.setMaxSize(0, 0);
            sGpStatusBar.setHgap(0);
            sStatusBarWidth = SCENE_WIDTH - (DEFAULT_INSERTS * 2);
            sLblStatusBar.setMinWidth(sStatusBarWidth + sNodesExtraWidth);
            sLblStatusBar.setMaxWidth(sStatusBarWidth + sNodesExtraWidth);

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    sPrimaryScene.setCursor(Cursor.DEFAULT);
                }
            });
        }
    }

    static void updateStatusBar(String string) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                sLblStatusBar.setText(string);
            }
        });
    }

    /**
     * This method is called to enable or disable and grey out the main GUI completely without changing anything underlying in GUI. This is done by enabling/disabling
     * a label stacked on the primaryRootNode with some color and transparency.
     *
     * @param value
     */
    private void disablePrimaryGui(boolean value) {
        if (value) {
            mLblInterfaceBlockingOverlay.setDisable(false);
            mLblInterfaceBlockingOverlay.setStyle("-fx-background-color: rgba(255, 255, 255, 0.75);");
        } else {
            mLblInterfaceBlockingOverlay.setDisable(true);
            mLblInterfaceBlockingOverlay.setStyle("-fx-background-color: rgba(255, 255, 255, 0);");
            mDialogStage.close();
        }
    }



    // ========================================================================
    // ========================================================================
    // From here getters and setters for non-private use.

    static boolean isProcessingActivated() {
        return sProcessingActivated;
    }

    static void setProcessingActivated(boolean processingActivated) {
        sProcessingActivated = processingActivated;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                updateGuiSelectorAndOptionNodes();
            }
        });
    }

    static void setDeletionActivated(boolean deletionActivated) {
        sDeletionActivated = deletionActivated;
        showPleaseWaitAnimation(deletionActivated);

        if (!deletionActivated) {
            setTree(sPicDataListList, sContainsExample);
            setLblApplyChangesCounter();
        }

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                updateGuiSelectorAndOptionNodes();
            }
        });
    }
}