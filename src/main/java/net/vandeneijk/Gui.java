// TODO Fix: ExamplePic path en SearchPic path may not be the same.

package net.vandeneijk;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Cursor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.awt.*;
import java.io.File;
import java.net.MalformedURLException;
import java.util.List;


public class Gui extends Application {

    private static final double TEXT_FIELD_HEIGHT = 25;

    private static TreeItem<PicData> mTiRoot;
    private static boolean sProcessingActivated = false;
    private static List<List<PicData>> sPicDataListList;
    private static boolean sContainsExample;
    private static List<PicData> sTreeItemIndex;
    private static Map<PicData, TreeItem<PicData>> mTvHelperMapping;
    private static ProgressBar sPbPleaseWaitAnimation;
    private static Label sLblStatusBar;
    private static GridPane sGpStatusBar;
    private static Scene primaryScene;

    private static boolean mExampleTextFieldValid = false;
    private static boolean mSearchTextFieldValid = false;
    private static Button mBtnStartStop;
    private static TextField mTfExamplePath;
    private static TextField mTfSearchPath;
    private static Button mBtnExampleFile;
    private static Button mBtnExampleFolder;
    private static Button mBtnSearchFolder;
    private static Button mBtnClear;
    private static CheckBox mCbxTraverseExamplePath;
    private static CheckBox mCbxTraverseSearchPath;
    private static CheckBox mCbxDuplicatesWithoutExample;
    private static ComboBox<String> mCbAccuracy;
    private Button mBtnApplyChanges;
    private Label mLblApplyChangesCounter;
    private TreeView<PicData> mTvResults;
    private FlowPane mFlwpThumbnails;
    private Map<String, Integer> mAccuracyMap = new TreeMap<>(Collections.reverseOrder());
    private double mSceneToPrimaryStageDifferenceWidth;
    private double mSceneToPrimaryStageDifferenceHeight;



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
        final Insets DEFAULT_PADDING_WITHOUT_LEFT_AND_HALF_RIGHT = new Insets(DEFAULT_INSERTS,DEFAULT_INSERTS / 2,DEFAULT_INSERTS,0);
        final Insets DEFAULT_PADDING_HALF_LEFT = new Insets(DEFAULT_INSERTS,DEFAULT_INSERTS ,DEFAULT_INSERTS,DEFAULT_INSERTS / 2);
        final Insets DEFAULT_PADDING_WITHOUT_TOP_BOTTOM = new Insets(0,DEFAULT_INSERTS,0,DEFAULT_INSERTS);
        final double SCENE_WIDTH = 1024;
        final double SCENE_HEIGHT = 768;
        final double BUTTON_WIDTH = 102;
        final double BUTTON_HEIGHT = 25;
        final double BUTTON_H_GAP = 5;
        final double TEXT_FIELD_HEIGHT = 25;
        final double V_GAP = 10;

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
                } else mExampleTextFieldValid = true;
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
                } else mSearchTextFieldValid = true;
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
                selectorAndOptionsGuiFeedback();
                mTiRoot.getChildren().clear();
                mFlwpThumbnails.getChildren().clear();
                mLblApplyChangesCounter.setText("0");
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
                    mTiRoot.getChildren().clear();
                    mFlwpThumbnails.getChildren().clear();
                    mLblApplyChangesCounter.setText("0");

                    File examplePath = new File(mTfExamplePath.getText());
                    File searchPath = new File(mTfSearchPath.getText());
                    if (mCbAccuracy.getSelectionModel().getSelectedIndex() == -1) mCbAccuracy.getSelectionModel().select(0);
                    int allowedDeviation = mAccuracyMap.get(mCbAccuracy.getSelectionModel().getSelectedItem());
                    boolean traverseExamplePath = mCbxTraverseExamplePath.isSelected();
                    boolean traverseSearchPath = mCbxTraverseSearchPath.isSelected();
                    boolean duplicatesWithoutExample = mCbxDuplicatesWithoutExample.isSelected();

                    Thread processController = new Thread(new ProcessController(examplePath, searchPath, allowedDeviation, traverseExamplePath, traverseSearchPath, duplicatesWithoutExample));
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

        Label lblSpacer = new Label();
        lblSpacer.setMinSize(100,3);
        lblSpacer.setMaxSize(100,3);

        ObservableList<String> accuracyOptions = FXCollections.observableArrayList(mAccuracyMap.keySet());
        mCbAccuracy = new ComboBox<>(accuracyOptions);
        mCbAccuracy.setMinWidth(SCENE_WIDTH / 4 - (DEFAULT_INSERTS * 0.5));
        mCbAccuracy.setMaxWidth(SCENE_WIDTH / 4 - (DEFAULT_INSERTS * 0.5));
        mCbAccuracy.getSelectionModel().select(0);

        GridPane gpOptionsColumn1 = getVerticalStackedGridPane(mCbxTraverseExamplePath, mCbxTraverseSearchPath, mCbxDuplicatesWithoutExample, lblSpacer, mCbAccuracy);
        gpOptionsColumn1.setMinSize(SCENE_WIDTH / 4, TEXT_FIELD_HEIGHT * 2 + V_GAP * 2 + DEFAULT_INSERTS * 2 + BUTTON_HEIGHT);
        gpOptionsColumn1.setMaxSize(SCENE_WIDTH / 4, TEXT_FIELD_HEIGHT * 2 + V_GAP * 2 + DEFAULT_INSERTS * 2 + BUTTON_HEIGHT);
        gpOptionsColumn1.setPadding(DEFAULT_PADDING_WITHOUT_LEFT_AND_HALF_RIGHT);
        gpOptionsColumn1.setVgap(4);



        mBtnApplyChanges = new Button("Delete Marked Pictures");
        mBtnApplyChanges.setDisable(true);
        mBtnApplyChanges.setMinSize(SCENE_WIDTH / 4 - (DEFAULT_INSERTS * 1.5), BUTTON_HEIGHT);
        mBtnApplyChanges.setMaxSize(SCENE_WIDTH / 4 - (DEFAULT_INSERTS * 1.5), BUTTON_HEIGHT );

        mLblApplyChangesCounter = new Label("0");
        mLblApplyChangesCounter.setDisable(true);
        GridPane gpApplyChanges = getHorizontalStackedGridPane(mLblApplyChangesCounter);
        gpApplyChanges.setDisable(true);
        gpApplyChanges.setAlignment(Pos.CENTER_RIGHT);
        gpApplyChanges.setPadding(DEFAULT_PADDING_WITHOUT_TOP_BOTTOM);

        StackPane stkApplyChanges = new StackPane();
        stkApplyChanges.getChildren().addAll(mBtnApplyChanges, gpApplyChanges);

        GridPane gpOptionsColumn2 = getVerticalStackedGridPane(stkApplyChanges);
        gpOptionsColumn2.setMinSize(SCENE_WIDTH / 4, TEXT_FIELD_HEIGHT * 2 + V_GAP * 2 + DEFAULT_INSERTS * 2 + BUTTON_HEIGHT);
        gpOptionsColumn2.setMaxSize(SCENE_WIDTH / 4, TEXT_FIELD_HEIGHT * 2 + V_GAP * 2 + DEFAULT_INSERTS * 2 + BUTTON_HEIGHT);
        gpOptionsColumn2.setPadding(DEFAULT_PADDING_HALF_LEFT);
        gpOptionsColumn2.setAlignment(Pos.BOTTOM_CENTER);



        GridPane gpOptionsSection = getHorizontalStackedGridPane(gpOptionsColumn1, gpOptionsColumn2);



        // --------------------------------------------------------------------
        // GUI gpSelectorSection and gpOptionsSection are coupled in a horizontal GridPane.

        GridPane gpSelectorAndOptions = getHorizontalStackedGridPane(gpSelectorSection, gpOptionsSection);



        // --------------------------------------------------------------------
        // GUI tree section from here. TODO Move out of testing phase.

        mTiRoot = new TreeItem<PicData>();
        mTiRoot.setExpanded(true);

        mTvResults = new TreeView<>(mTiRoot);
        mTvResults.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<PicData>>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem<PicData>> observable, TreeItem<PicData> oldValue, TreeItem<PicData> newValue) {
                if (newValue != null) setThumbnail(newValue.getValue());
            }
        });

        double heightGpSelectorAndOptions = (V_GAP * 2) + (DEFAULT_INSERTS * 2) + (TEXT_FIELD_HEIGHT * 3) + BUTTON_HEIGHT;
        mTvResults.setMinSize(SCENE_WIDTH / 2 - 20, SCENE_HEIGHT - heightGpSelectorAndOptions);

        FlowPane fpResultTree = new FlowPane();
        fpResultTree.getChildren().add(mTvResults);



        // --------------------------------------------------------------------
        // GUI thumbnail in a ScrollPane and result management sections from here. TODO Move out of testing phase.

        mFlwpThumbnails = new FlowPane();
        mFlwpThumbnails.setOrientation(Orientation.HORIZONTAL);
        mFlwpThumbnails.setAlignment(Pos.TOP_LEFT);
        mFlwpThumbnails.setStyle("-fx-background-color: lightgrey");
        mFlwpThumbnails.setMinSize(SCENE_WIDTH / 2 - 25, SCENE_HEIGHT - heightGpSelectorAndOptions);
        mFlwpThumbnails.setHgap(5);
        mFlwpThumbnails.setVgap(5);

        ScrollPane sclpThumbnails = new ScrollPane(mFlwpThumbnails);
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
        // GUI status bar from here.

        sPbPleaseWaitAnimation = new ProgressBar();
        sPbPleaseWaitAnimation.setMinSize(0, 0);
        sPbPleaseWaitAnimation.setMaxSize(0, 0);

        sLblStatusBar = new Label();
        sLblStatusBar.setMinSize(SCENE_WIDTH - DEFAULT_INSERTS * 2, TEXT_FIELD_HEIGHT);

        sGpStatusBar = getHorizontalStackedGridPane(sPbPleaseWaitAnimation, sLblStatusBar);
        sGpStatusBar.setPadding(DEFAULT_PADDING_WITHOUT_TOP_BOTTOM);



        // --------------------------------------------------------------------
        // GUI final coupling of all sections.

        GridPane primaryRootNode = getVerticalStackedGridPane(gpSelectorAndOptions, gpResults, sGpStatusBar);
        primaryRootNode.setMinSize(SCENE_WIDTH, SCENE_HEIGHT);

        primaryScene = new Scene(primaryRootNode,SCENE_WIDTH,SCENE_HEIGHT);





        primaryStage.setTitle("SamePic v.0.1.0");

        primaryStage.setScene(primaryScene);
        primaryStage.sizeToScene();
        primaryStage.show();
        primaryStage.setMinWidth(primaryStage.getWidth());
        primaryStage.setMinHeight(primaryStage.getHeight());

        mSceneToPrimaryStageDifferenceWidth = primaryStage.getWidth() - primaryScene.getWidth();
        mSceneToPrimaryStageDifferenceHeight = primaryStage.getHeight() - primaryScene.getHeight();

        primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            mFlwpThumbnails.setMinWidth((SCENE_WIDTH / 2 - 25) + ((double) newVal - SCENE_WIDTH - mSceneToPrimaryStageDifferenceWidth));
            gpThumbnails.setMinWidth((SCENE_WIDTH / 2 - 10) + ((double) newVal - SCENE_WIDTH - mSceneToPrimaryStageDifferenceWidth));
            gpThumbnails.setMaxWidth((SCENE_WIDTH / 2 - 10) + ((double) newVal - SCENE_WIDTH - mSceneToPrimaryStageDifferenceWidth));
        });
        primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            mTvResults.setMinHeight((SCENE_HEIGHT - heightGpSelectorAndOptions) + ((double) newVal - SCENE_HEIGHT - mSceneToPrimaryStageDifferenceHeight));
            mFlwpThumbnails.setMinHeight((SCENE_HEIGHT - heightGpSelectorAndOptions) + ((double) newVal - SCENE_HEIGHT - mSceneToPrimaryStageDifferenceHeight));
            gpThumbnails.setMinHeight((SCENE_HEIGHT - heightGpSelectorAndOptions) + ((double) newVal - SCENE_HEIGHT - mSceneToPrimaryStageDifferenceHeight));
            gpThumbnails.setMaxHeight((SCENE_HEIGHT - heightGpSelectorAndOptions) + ((double) newVal - SCENE_HEIGHT - mSceneToPrimaryStageDifferenceHeight));
        });
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
                mTvResults.getSelectionModel().select(treeItem);

                if (event.isSecondaryButtonDown() && !examplePic && selectedPic) {  // TODO Make preselection unnecessary before context menu popup.
                    MenuItem miDeleteSingle = new MenuItem();
                    if (picData.isMarkedForDeletion()) miDeleteSingle.setText("Unmark For Deletion");
                    else miDeleteSingle.setText("Mark For Deletion");

                    miDeleteSingle.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            picData.setMarkedForDeletion(!picData.isMarkedForDeletion());
                            setThumbnail(picData);
                            setLblApplyChangesCounter();
                        }
                    });

                    SeparatorMenuItem miSeparator1 = new SeparatorMenuItem();

                    MenuItem miDeleteDirectoryMark = new MenuItem("Mark Directory For Deletion");
                    miDeleteDirectoryMark.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            Path parentPath = picData.getPath().getParent();
                            for (List<PicData> picDataList : sPicDataListList) {
                                picDataList.stream().filter(x -> x.getPath().getParent().equals(parentPath)).forEach(x -> x.setMarkedForDeletion(true));
                            }
                            setThumbnail(picData);
                            setLblApplyChangesCounter();
                        }
                    });

                    MenuItem miDeleteDirectoryUnmark = new MenuItem("Unmark Directory For Deletion");
                    miDeleteDirectoryUnmark.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            Path parentPath = picData.getPath().getParent();
                            for (List<PicData> picDataList : sPicDataListList) {
                                picDataList.stream().filter(x -> x.getPath().getParent().equals(parentPath)).forEach(x -> x.setMarkedForDeletion(false));
                            }
                            setThumbnail(picData);
                            setLblApplyChangesCounter();
                        }
                    });

                    SeparatorMenuItem miSeparator2 = new SeparatorMenuItem();

                    MenuItem miDeleteDuplicatesMark = new MenuItem("Mark Duplicates for Deletion");
                    miDeleteDuplicatesMark.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            for (List<PicData> picDataList : sPicDataListList) {
                                if (picDataList.contains(picData)) {
                                    for (PicData picDataMark : picDataList) {
                                        if (!picDataMark.isExamplePicture()) picDataMark.setMarkedForDeletion(true);
                                        setThumbnail(picDataMark);
                                    }
                                }
                            }
                            setLblApplyChangesCounter();
                        }
                    });

                    MenuItem miDeleteDuplicatesUnmark = new MenuItem("Unmark Duplicates for Deletion");
                    miDeleteDuplicatesUnmark.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            for (List<PicData> picDataList : sPicDataListList) {
                                if (picDataList.contains(picData)) {
                                    for (PicData picDataMark : picDataList) {
                                        if (!picDataMark.isExamplePicture()) picDataMark.setMarkedForDeletion(false);
                                        setThumbnail(picDataMark);
                                    }
                                }
                            }
                            setLblApplyChangesCounter();
                        }
                    });

                    ContextMenu contextMenu = new ContextMenu();
                    contextMenu.getItems().addAll(miDeleteSingle, miSeparator1, miDeleteDirectoryMark, miDeleteDirectoryUnmark, miSeparator2, miDeleteDuplicatesMark, miDeleteDuplicatesUnmark);
                    contextMenu.show(gpImage, event.getScreenX(), event.getScreenY());

                }
            }
        });

        if (examplePic) {
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
        } else {

        }



        if (selectedPic) {
            if (picData.isMarkedForDeletion()) gpImage.setStyle("-fx-background-color: red");
            else gpImage.setStyle("-fx-background-color: white");
        }
        else {
            if (picData.isMarkedForDeletion()) gpImage.setStyle("-fx-background-color: darkred");
            else gpImage.setStyle("-fx-background-color: grey");
        }

        return getVerticalStackedGridPane(gpImage);
    }



    // ========================================================================
    // ========================================================================
    // From here helper methods for maintaining the GUI.

    private static void selectorAndOptionsGuiFeedback() {
        if (mCbxDuplicatesWithoutExample.isSelected()) {
            if (mSearchTextFieldValid) mBtnStartStop.setDisable(false);
            else mBtnStartStop.setDisable(true);

            mBtnExampleFile.setDisable(true);
            mBtnExampleFolder.setDisable(true);
            mTfExamplePath.setDisable(true);
            mCbxTraverseExamplePath.setDisable(true);
            mCbAccuracy.setDisable(true);
            if (sProcessingActivated) {
                mBtnStartStop.setText("Stop");
                mTfSearchPath.setEditable(false);
                mBtnSearchFolder.setDisable(true);
                mBtnClear.setDisable(true);
                mCbxTraverseSearchPath.setDisable(true);
                mCbxDuplicatesWithoutExample.setDisable(true);
            } else {
                mBtnStartStop.setText("Start");
                mTfSearchPath.setEditable(true);
                mBtnSearchFolder.setDisable(false);
                mBtnClear.setDisable(false);
                mCbxTraverseSearchPath.setDisable(false);
                mCbxDuplicatesWithoutExample.setDisable(false);
            }
        } else {
            if (mExampleTextFieldValid && mSearchTextFieldValid) mBtnStartStop.setDisable(false);
            else mBtnStartStop.setDisable(true);

            mTfExamplePath.setDisable(false);

            mCbAccuracy.setDisable(false);
            if (sProcessingActivated) {
                mBtnStartStop.setText("Stop");
                mTfExamplePath.setEditable(false);
                mTfSearchPath.setEditable(false);
                mBtnExampleFile.setDisable(true);
                mBtnExampleFolder.setDisable(true);
                mBtnSearchFolder.setDisable(true);
                mBtnClear.setDisable(true);
                mCbAccuracy.setDisable(true);
                mCbxTraverseExamplePath.setDisable(true);
                mCbxTraverseSearchPath.setDisable(true);
                mCbxDuplicatesWithoutExample.setDisable(true);
            } else {
                mBtnStartStop.setText("Start");
                mTfExamplePath.setEditable(true);
                mTfSearchPath.setEditable(true);
                mBtnExampleFile.setDisable(false);
                mBtnExampleFolder.setDisable(false);
                mBtnSearchFolder.setDisable(false);
                mBtnClear.setDisable(false);
                mCbAccuracy.setDisable(false);
                mCbxTraverseExamplePath.setDisable(false);
                mCbxTraverseSearchPath.setDisable(false);
                mCbxDuplicatesWithoutExample.setDisable(false);
                if (mCbAccuracy.getSelectionModel().getSelectedIndex() == -1) mCbAccuracy.getSelectionModel().select(0);
            }
        }
    }

    private void setThumbnail(PicData picData) {
        List<PicData> thumbnailPicDataList = null;
        for (List<PicData> picDataList : sPicDataListList) {
            if (picDataList.contains(picData)) thumbnailPicDataList = picDataList;
        }

        mFlwpThumbnails.getChildren().clear();
        if (thumbnailPicDataList != null) {
            for (PicData picDataForThumbnail : thumbnailPicDataList) {
                boolean selectedPic = (picData.equals(picDataForThumbnail));
                boolean examplePic = (picDataForThumbnail.isExamplePicture());
                if (!picDataForThumbnail.isDoNotShowThumbnail()) mFlwpThumbnails.getChildren().add(getThumbnail(new Image(picDataForThumbnail.getUrl().toString(), 158, 158, true, false), selectedPic, examplePic, picDataForThumbnail));
            }
        }

    }

    public static void setTree(List<List<PicData>> pictureListList, boolean containsExample) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                sPicDataListList = pictureListList;
                sContainsExample = containsExample;
                mTvHelperMapping = new HashMap<>();
                mTiRoot.getChildren().clear();
                sTreeItemIndex = new ArrayList();

                if (containsExample) {
                    for (List<PicData> picDataList : pictureListList) {
                        TreeItem<PicData> firstLevel = new TreeItem<>();
                        for (int i = 0; i < picDataList.size(); i++) {
                            if (i == 0) {
                                firstLevel = new TreeItem<>(picDataList.get(0));
                                sTreeItemIndex.add(firstLevel.getValue());
                                mTvHelperMapping.put(firstLevel.getValue(), firstLevel);
                            }
                            else {
                                TreeItem<PicData> secondLevel = new TreeItem<>(picDataList.get(i));
                                firstLevel.getChildren().add(secondLevel);
                                sTreeItemIndex.add(secondLevel.getValue());
                                mTvHelperMapping.put(secondLevel.getValue(), secondLevel);
                            }
                        }
                        mTiRoot.getChildren().add(firstLevel);
                    }
                } else {
                    int count = 1;
                    for (List<PicData> picDataList : pictureListList) {
                        TreeItem<PicData> firstLevel;
                        try {
                            PicData administrativeExamplePic = new PicData(Paths.get("Collection" + count++), false, true);
                            picDataList.add(0, administrativeExamplePic);
                            firstLevel = new TreeItem<>(administrativeExamplePic);
                            sTreeItemIndex.add(firstLevel.getValue());
                            mTvHelperMapping.put(firstLevel.getValue(), firstLevel);
                        } catch (MalformedURLException mfuEx) {
                            continue;
                        }
                        for (int i = 1; i < picDataList.size(); i++) {
                            TreeItem<PicData> secondLevel = new TreeItem<>(picDataList.get(i));
                            firstLevel.getChildren().add(secondLevel);
                            sTreeItemIndex.add(secondLevel.getValue());
                            mTvHelperMapping.put(secondLevel.getValue(), secondLevel);
                        }
                        mTiRoot.getChildren().add(firstLevel);
                    }
                }
            }
        });
    }

    public void setLblApplyChangesCounter() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                for (List<PicData> picDataList : sPicDataListList) {
                    for (PicData picDataCounter : picDataList) {
                        if (picDataCounter.isMarkedForDeletion()) count++;
                    }
                }
                mLblApplyChangesCounter.setText("" + count);
                if (count > 0) mBtnApplyChanges.setDisable(false);
                else mBtnApplyChanges.setDisable(true);
            }
        });
    }

    public static void showPleaseWaitAnimation(boolean showPleaseWaitAnimation) {
        if (showPleaseWaitAnimation) {
            sPbPleaseWaitAnimation.setMinSize(TEXT_FIELD_HEIGHT / 1.5, TEXT_FIELD_HEIGHT / 1.5);
            sPbPleaseWaitAnimation.setMaxSize(TEXT_FIELD_HEIGHT / 1.5, TEXT_FIELD_HEIGHT / 1.5);
            sGpStatusBar.setHgap(10);

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    primaryScene.setCursor(Cursor.WAIT);
                }
            });

        } else {
            sPbPleaseWaitAnimation.setMinSize(0, 0);
            sPbPleaseWaitAnimation.setMaxSize(0, 0);
            sGpStatusBar.setHgap(0);

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    primaryScene.setCursor(Cursor.DEFAULT);
                }
            });
        }
    }

    public static void updateStatusBar(String string) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                sLblStatusBar.setText(string);
            }
        });
    }



    // ========================================================================
    // ========================================================================
    // From here getters and setters for non-private use.

    public static boolean isProcessingActivated() {
        return sProcessingActivated;
    }

    public static void setProcessingActivated(boolean processingActivated) {
        sProcessingActivated = processingActivated;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                selectorAndOptionsGuiFeedback();
            }
        });
    }
}
