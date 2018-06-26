/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;

/**
 *
 * @author Caitlin
 */
public class TabState {
    private final X86Parser parser;
    private final Simulation simulator;
    private String fileName;
    private boolean isEdited;

    public TabState(X86Parser parser,
                    Simulation simulator,
                    String fileName) {
        this.parser = parser;
        this.simulator = simulator;
        this.fileName = fileName;
        this.isEdited = false;
    }

    public Simulation getSimulator() {
        return this.simulator;
    }

    public ObservableList<x86ProgramLine> getInstrList() {
        return this.simulator.getInstrList();
    }

    public X86Parser getParser() {
        return this.parser;
    }

    public String getFileName() {
        return this.fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public boolean getIsEdited(){
        return this.isEdited;
    }
    
    public void setIsEdited(boolean b){
        isEdited = b;
    }
}