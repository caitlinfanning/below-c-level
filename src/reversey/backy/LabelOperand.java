/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A class representing a label operand in an x86 instruction.
 *
 * @author Caitlin
 */
public class LabelOperand extends Operand {

    /**
     * The name of the label.
     */
    private final String name;

    /**
     * The label referred to by this operand.
     */
    private x86Label label;

    public LabelOperand(String name, x86Label label) {
        this.name = name;
        this.label = label;
    }

    public String getName() {
        return name;
    }

    @Override
    public BigInteger getValue(MachineState state) {
        if (label == null) {
            // FIXME: change this to a special exception type
            throw new RuntimeException("nonexistent label: " + this.name);
        }
        String num = "" + label.getLineNum();
        return new BigInteger(num);
    }

    @Override
    public MachineState updateState(MachineState currState, Optional<BigInteger> val, Map<String, Boolean> flags, boolean updateRIP) {
        if (val.isPresent()) {
            return currState.getNewState(val.get());
        } else {
            return currState.getNewState();
        }
    }

    @Override
    public Set<String> getUsedRegisters() {
        return new HashSet<String>();
    }

    @Override
    public void updateLabels(String labelName, x86Label label){
        if(this.name.equals(labelName)){
            this.label = label;
        }
    }
    
    @Override
    public String toString() {
        String res = "" + name;
        return res;
    }
}