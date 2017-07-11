package reversey.backy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javafx.scene.image.Image;
import javafx.fxml.FXML;
import javafx.scene.input.*;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import java.util.*;
import java.net.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.Event;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.scene.text.Font;
import javafx.util.Callback;
import javafx.scene.shape.Circle;

/**
 * Class that controls the main FXML file.
 *
 * @author Caitlin
 */
public class FXMLDocumentController implements Initializable {


    // Fields for the menu bar
    @FXML
    private MenuItem exitMenuItem;
    @FXML
    private MenuItem loadMenuItem;
    @FXML
    private MenuItem saveMenuItem;
    @FXML
    private MenuItem forwardMenuItem;
    @FXML
    private MenuItem backwardMenuItem;
    @FXML
    private MenuItem runMenuItem;
    @FXML
    private MenuItem restartMenuItem;
    @FXML
    private MenuItem aboutMenuItem;
    
    
    @FXML
    private MenuBar menuOptionsBar;
    @FXML
    private Menu fileOption;
    @FXML
    private Menu helpOption;
    
    @FXML
    private BorderPane entirePane;

    // UI elements for adding new instructions
    @FXML
    private TextField instrText;
    @FXML
    private MenuButton insertMenu;
    @FXML
    private Label parseErrorText;
    
    @FXML
    private ListView<x86ProgramLine> instrList;

    // Simulation Control Buttons
    @FXML
    private Button nextInstr;
    @FXML
    private Button skipToEnd;
    @FXML
    private Button prevInstr;
    @FXML
    private Button skipToStart;
    @FXML
    private Button currInstr;

    // Fields for stack/memory table
    @FXML
    private TableView<StackEntry> stackTable;
    @FXML
    private TableColumn<StackEntry, String> startAddressCol;
    @FXML
    private TableColumn<StackEntry, String> endAddressCol;
    @FXML
    private TableColumn<StackEntry, String> valCol;
    @FXML
    private TableColumn<StackEntry, Integer> originCol;

    /**
     * List of stack entries in our current state.
     */
    ObservableList<StackEntry> stackTableList;

    // Fields for the register table
    @FXML
    private TableView<Register> promRegTable;
    @FXML
    private TableColumn<Register, String> registerName;
    @FXML
    private TableColumn<Register, String> registerVal;
    @FXML
    private TableColumn<Register, Integer> registerOrigin;

    /**
     * List of registers values in our current state.
     */
    private ObservableList<Register> registerTableList;

    /**
     * The history of execution states in our simulation.
     */
    private List<MachineState> stateHistory;
    
    /**
     * History of registers used by the simulation.
     * This list may contain duplicates as one is added for each register used
     * by an instruction when it is executed.
     */
    private List<String> regHistory;

    @Override
    public void initialize(URL foo, ResourceBundle bar) {
        // Disable user selecting arbitrary item in instruction list.
        instrList.setCellFactory(lv -> {
            ListCell<x86ProgramLine> cell = new ListCell<x86ProgramLine>() {
                @Override
                protected void updateItem(x86ProgramLine item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                    } else {
                        setFont(new Font("Courier", 14));
                        setText(item.toString());
                        if (lv.getSelectionModel().getSelectedItems().contains(item))
                            setGraphic(new Circle(5.0f));
                    }
                }
            };

            /*
            ContextMenu cM = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction( event -> {
                lv.getItems().remove(cell.getItem());
                int i = 0;
                for (x86ProgramLine line : lv.getItems()) {
                    line.setLineNum(i);
                    i++;
                }
            });
            cM.getItems().addAll(deleteItem);
            deleteItem.setDisable(true);
            */

            cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                if (event.getButton()== MouseButton.SECONDARY && !cell.isEmpty()) {
                    //lv.getFocusModel().focus(lv.getItems().indexOf(cell.getItem()));
                    lv.getFocusModel().focus(1);
                    //x86ProgramLine item = cell.getItem();
                    //System.out.println("Right clicked: " + item);
                    //cell.setContextMenu(cM);
                }
                event.consume();
            });
            return cell ;
        });

        // Initialize the simulation state.
        stateHistory = new ArrayList<>();
        stateHistory.add(new MachineState());
        regHistory = new ArrayList<>();

        // Initialize stack table
        startAddressCol.setCellValueFactory((CellDataFeatures<StackEntry, String> p)
                -> new SimpleStringProperty(Long.toHexString(p.getValue().getStartAddress())));

        endAddressCol.setCellValueFactory((CellDataFeatures<StackEntry, String> p)
                -> new SimpleStringProperty(Long.toHexString(p.getValue().getEndAddress())));

        valCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        originCol.setCellValueFactory(new PropertyValueFactory<>("origin"));

        startAddressCol.setStyle("-fx-alignment: CENTER;");
        endAddressCol.setStyle("-fx-alignment: CENTER;");
        valCol.setStyle("-fx-alignment: CENTER;");
        originCol.setStyle("-fx-alignment: CENTER;");

        stackTableList = FXCollections.observableArrayList(stateHistory.get(this.stateHistory.size() - 1).getStackEntries());
        stackTable.setItems(stackTableList);

        // Initialize the register table
        registerName.setCellValueFactory(new PropertyValueFactory<>("name"));
        registerVal.setCellValueFactory(new PropertyValueFactory<>("value"));
        registerOrigin.setCellValueFactory(new PropertyValueFactory<>("origin"));

        registerName.setStyle("-fx-alignment: CENTER;");
        registerVal.setStyle("-fx-alignment: CENTER;");
        registerOrigin.setStyle("-fx-alignment: CENTER;");

        Comparator<Register> regComp = (Register r1, Register r2) -> {
            if (r1.getProminence() > r2.getProminence()) {
                return -1;
            } else if (r1.getProminence() == r2.getProminence()) {
                return r1.getName().compareTo(r2.getName());
            } else {
                return 1;
            }
        };

        registerTableList = FXCollections.observableArrayList(stateHistory.get(this.stateHistory.size() - 1).getRegisters(regHistory));
        SortedList<Register> regSortedList = registerTableList.sorted(regComp);
        promRegTable.setItems(regSortedList);


        // Set up handlers for simulation control, both via buttons and menu
        // items.
        nextInstr.setOnAction(this::stepForward);
        forwardMenuItem.setOnAction(this::stepForward);

        skipToEnd.setOnAction(this::runForward);
        runMenuItem.setOnAction(this::runForward);

        /*
         * Event handler for "scroll back to current instruction" button.
         */
        currInstr.setOnAction(event -> {
            ObservableList<Integer> selectedIndices = instrList.getSelectionModel().getSelectedIndices();
            if (!selectedIndices.isEmpty()) {
                instrList.scrollTo(selectedIndices.get(0));
            }
        });

        prevInstr.setOnAction(this::stepBackward);
        backwardMenuItem.setOnAction(this::stepBackward);

        skipToStart.setOnAction(this::restartSim);
        restartMenuItem.setOnAction(this::restartSim);

        /*
         * Event handler for when user clicks button to insert a new
         * instruction.
         */
        instrText.setOnKeyPressed((KeyEvent keyEvent) -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                String text = instrText.getText();
                try {
                    x86ProgramLine x = X86Parser.parseLine(text);
                    instrText.setStyle("-fx-control-inner-background: white;");
                    parseErrorText.setText(null);
                    parseErrorText.setGraphic(null);

                    //Enter text in listView
                    instrList.getItems().add(x);

                    // If this is the first instruction entered, "select" it and
                    // make sure it gets added to our register history list.
                    if (instrList.getItems().size() == 1) {
                        regHistory.addAll(x.getUsedRegisters());
                        instrList.getSelectionModel().select(0);
                        registerTableList = FXCollections.observableArrayList(stateHistory.get(stateHistory.size() - 1).getRegisters(regHistory));
                        SortedList<Register> regSortedList1 = registerTableList.sorted(regComp);
                        promRegTable.setItems(regSortedList1);
                    }
                    instrText.clear();
                } catch (X86ParsingException e) {
                    // If we had a parsing error, set the background to pink,
                    // select the part of the input that reported the error,
                    // and set the error label's text.
                    instrText.setStyle("-fx-control-inner-background: pink;");
                    instrText.selectRange(e.getStartIndex(), e.getEndIndex());
                    parseErrorText.setText(e.getMessage());
                    ImageView errorPic = new ImageView(
                            new Image(this.getClass().getResourceAsStream("error.png"), 16, 16, true, true));
                    parseErrorText.setGraphic(errorPic);
                }
            }
        });

        // Set up actions for the menubar
        exitMenuItem.setOnAction((event) -> System.exit(0));

        /*
         * Event handler for "loadMenuItem" menu.
         * This will reset the simulation, returning to the very first
         * instruction of the loaded text file.
         */
        loadMenuItem.setOnAction((event) -> {
            // TODO: What here is unecessary?
            // Force user to reset? All previously entered instructions are removed currently
            X86Parser.clear();
            stateHistory.clear();
            instrList.getItems().clear();
            regHistory.clear();
            stateHistory.add(new MachineState());

            FileChooser loadFileChoice = new FileChooser();
            loadFileChoice.setTitle("Open File");

            // Filter only allows user to choose a text file
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
            loadFileChoice.getExtensionFilters().add(extFilter);
            File loadFile = loadFileChoice.showOpenDialog(menuOptionsBar.getScene().getWindow());
            if (loadFile != null) {
                BufferedReader bufferedReader = null;
                ArrayList<String> instrTmp = new ArrayList<>();
                try {
                    bufferedReader = new BufferedReader(new FileReader(loadFile));
                    String tmp;
                    while ((tmp = bufferedReader.readLine()) != null) {
                        instrTmp.add(tmp.trim());
                    }
                } catch (FileNotFoundException e) {
                    System.out.println("File does not exist: please choose a valid text file.");
                } catch (IOException e) {
                    System.out.println("Invalid file.");
                } finally {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        System.out.println("Invalid file.");
                    }
                }
                try {
                    for (String e : instrTmp) {
                        x86ProgramLine x = X86Parser.parseLine(e);
                        instrList.getItems().add(x);
                    }
                    //Enter text in listView and select first instruction... is if statement necessary? blank file is never chosen
                    if (instrList.getItems().size() >= 1) {
                        instrList.getSelectionModel().select(0);
                        regHistory.addAll(instrList.getSelectionModel().getSelectedItem().getUsedRegisters());
                        registerTableList = FXCollections.observableArrayList(stateHistory.get(stateHistory.size() - 1).getRegisters(regHistory));
                        SortedList<Register> regSortedList1 = registerTableList.sorted(regComp);
                        promRegTable.setItems(regSortedList1);
                    }
                } catch (X86ParsingException e) {
                    // If we had a parsing error, report what? File "line"? In which case numbers must remain
                    // TODO: Pop-up window for error in file
                    System.out.println("Loaded file parsing error");
                }
            }
        });

        /*
         * Event handler for "saveMenuItem" menu.
         * This will save the current simulation to a text file specified 
         * by the user.
         */
        saveMenuItem.setOnAction((event) -> {
            FileChooser saveFileChoice = new FileChooser();

            // Filter only allows user to choose text files
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
            saveFileChoice.getExtensionFilters().add(extFilter);
            File file = saveFileChoice.showSaveDialog(menuOptionsBar.getScene().getWindow());
            if (file != null) {
                try {
                    FileWriter fileWriter = new FileWriter(file);
                    for (int i = 0; i < instrList.getItems().size(); i++) {
                        // Formatting okay?
                        fileWriter.write(instrList.getItems().get(i).toString().substring(3) + "\n");
                    }
                    fileWriter.close();
                } catch (IOException ex) {
                    //TODO: ?
                    System.out.println("Unable to save to file.");
                }
            }
        });

        //TODO: if user wants to change where the instruction should be inserted
        MenuItem beginning = insertMenu.getItems().get(0);
        beginning.setText("At beginning");
        MenuItem current = insertMenu.getItems().get(1);
        current.setText("At current");

        // Initialize buttons with fancy graphics.
        Image skipStartImg = new Image(getClass().getResourceAsStream("skipToStart.png"));
        Image prevInstrImg = new Image(getClass().getResourceAsStream("prevInstr.png"));
        Image currInstrImg = new Image(getClass().getResourceAsStream("currInstr.png"));
        Image nextInstrImg = new Image(getClass().getResourceAsStream("nextInstr.png"));
        Image skipEndImg = new Image(getClass().getResourceAsStream("skipToEnd.png"));

        ImageView skipToStartImgVw = new ImageView(skipStartImg);
        ImageView prevInstrImgVw = new ImageView(prevInstrImg);
        ImageView currInstrImgVw = new ImageView(currInstrImg);
        ImageView nextInstrImgVw = new ImageView(nextInstrImg);
        ImageView skipToEndImgVw = new ImageView(skipEndImg);

        skipToStartImgVw.setFitHeight(35);
        skipToStartImgVw.setFitWidth(35);
        prevInstrImgVw.setFitHeight(35);
        prevInstrImgVw.setFitWidth(35);
        currInstrImgVw.setFitHeight(35);
        currInstrImgVw.setFitWidth(35);
        nextInstrImgVw.setFitHeight(35);
        nextInstrImgVw.setFitWidth(35);
        skipToEndImgVw.setFitHeight(35);
        skipToEndImgVw.setFitWidth(35);

        skipToStart.setGraphic(skipToStartImgVw);
        prevInstr.setGraphic(prevInstrImgVw);
        currInstr.setGraphic(currInstrImgVw);
        nextInstr.setGraphic(nextInstrImgVw);
        skipToEnd.setGraphic(skipToEndImgVw);

        //TODO: Resizing icons/nodes to pane
        /*
           skipToStartImgVw.fitHeightProperty().bind(skipToStart.heightProperty());
           skipToStartImgVw.fitWidthProperty().bind(skipToStart.widthProperty());
           prevInstrImgVw.fitHeightProperty().bind(prevInstr.heightProperty());
           prevInstrImgVw.fitWidthProperty().bind(prevInstr.widthProperty());
           currInstrImgVw.fitHeightProperty().bind(currInstr.heightProperty());
           currInstrImgVw.fitWidthProperty().bind(currInstr.widthProperty());
           nextInstrImgVw.fitHeightProperty().bind(nextInstr.heightProperty());
           nextInstrImgVw.fitWidthProperty().bind(nextInstr.widthProperty());
           skipToEndImgVw.fitHeightProperty().bind(skipToEnd.heightProperty());
           skipToEndImgVw.fitWidthProperty().bind(skipToEnd.widthProperty());

           skipToStart.setMinSize(buttonHBox.getPrefWidth(), buttonHBox.getPrefHeight());
           skipToStart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
           prevInstr.setMinSize(buttonHBox.getPrefWidth(), buttonHBox.getPrefHeight());
           prevInstr.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
           currInstr.setMinSize(buttonHBox.getPrefWidth(), buttonHBox.getPrefHeight());
           currInstr.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
           nextInstr.setMinSize(buttonHBox.getPrefWidth(), buttonHBox.getPrefHeight());
           nextInstr.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
           skipToEnd.setMinSize(buttonHBox.getPrefWidth(), buttonHBox.getPrefHeight());
           skipToEnd.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
         */

 /*
         * Event handler for when user picks "insert at beginning" option.
         */
        beginning.setOnAction((event) -> {
            System.out.println("Insert at Beginning selected");
        });

        /*
         * Event handler for when user picks "insert after current" option.
         */
        current.setOnAction((event) -> {
            System.out.println("Insert at Current selected");
        });

        Platform.runLater(() -> {
            // instrList.scrollTo(N);
            // instrList.getSelectionModel().select(N);
        });

    }
    
    /**
     * Executes the next instruction in our simulation.
     * 
     * @param event The event that triggered this action.
     */
    private void stepForward(Event event) {
        this.stateHistory.add(instrList.getSelectionModel().getSelectedItem().eval(this.stateHistory.get(this.stateHistory.size() - 1)));

        instrList.getSelectionModel().select(this.stateHistory.get(this.stateHistory.size() - 1).getRipRegister().intValue());
        regHistory.addAll(instrList.getSelectionModel().getSelectedItem().getUsedRegisters());

        registerTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getRegisters(regHistory));
        stackTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getStackEntries());
    }
    
    /**
     * Executes instructions until it reaches the end of the program (TODO: or a breakpoint).
     * 
     * @param event The event that triggered this action.
     */
    private void runForward(Event event) {
        // TODO: DANGER WILL ROBISON! Do we want to warn the user if they
        // appear to be stuck in an infinite loop?
        for (int x = instrList.getSelectionModel().getSelectedIndex(); x < instrList.getItems().size(); x++) {
            this.stateHistory.add(instrList.getSelectionModel().getSelectedItem().eval(this.stateHistory.get(this.stateHistory.size() - 1)));
            instrList.getSelectionModel().select(this.stateHistory.get(this.stateHistory.size() - 1).getRipRegister().intValue());
            regHistory.addAll(instrList.getSelectionModel().getSelectedItem().getUsedRegisters());
        }

        registerTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getRegisters(regHistory));
        stackTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getStackEntries());
    }

    /**
     * Undoes the previous instruction in our simulation.
     * 
     * @param event The event that triggered this action.
     */
    private void stepBackward(Event event) {
        this.stateHistory.remove((this.stateHistory.size() - 1));
        regHistory.removeAll(instrList.getSelectionModel().getSelectedItem().getUsedRegisters());

        instrList.getSelectionModel().selectPrevious();

        registerTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getRegisters(regHistory));
        stackTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getStackEntries());
    }
    
    /**
     * Restarts simulation back to its starting state.
     * 
     * @param event The event that triggered this action.
     */
    private void restartSim(Event event) {
        instrList.getSelectionModel().selectFirst();

        this.stateHistory.clear();
        regHistory.clear();

        stateHistory.add(new MachineState());
        regHistory.addAll(instrList.getSelectionModel().getSelectedItem().getUsedRegisters());
        registerTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getRegisters(regHistory));
        stackTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getStackEntries());
    }

}
