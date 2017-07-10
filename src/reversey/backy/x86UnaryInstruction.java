package reversey.backy;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

@FunctionalInterface
interface UnaryX86Operation {
    MachineState apply(MachineState state, Operand dest);
}

/**
 * Class representing an x86 instruction with a single operand.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
public class x86UnaryInstruction extends x86Instruction {
    /**
     * The function that this instruction performs.
     */
    private UnaryX86Operation operation;

    /**
     * An optional predicate to be used with conditional instructions.
     */
    private Optional<Predicate<MachineState>> conditionCheck = Optional.empty();

    /**
     * @param instType The type of operation performed by the instruction.
     * @param destOp Operand representing the destination of the instruction.
     * @param size Number of bytes this instruction works on.
     */
    public x86UnaryInstruction(InstructionType instType, Operand destOp, OpSize size, int line) {
        this.type = instType;
        this.destination = destOp;
        this.opSize = size;
        this.lineNum = line;

        switch (instType) {
            case INC:
                this.operation = this::inc;
                break;
            case DEC:
                this.operation = this::dec;
                break;
            case NEG:
                this.operation = this::neg;
                break;
            case NOT:
                this.operation = this::not;
                break;
            case SETE:
            case SETNE:
            case SETS:
            case SETNS:
            case SETG:
            case SETGE:
            case SETL:
            case SETLE:
                this.conditionCheck = Optional.of(conditions.get(instType.name().toLowerCase().substring(3)));
                this.operation = this::set;
                break;
            case JE:
            case JNE:
            case JS:
            case JNS:
            case JG:
            case JGE:
            case JL:
            case JLE:
                this.conditionCheck = Optional.of(conditions.get(instType.name().toLowerCase().substring(1)));
                this.operation = this::jump;
                break;
            case JMP:
                this.conditionCheck = Optional.of(conditions.get("jmp"));
                this.operation = this::jump;
                break;
            case PUSH:
                this.operation = this::push;
                break;
            case POP:
                this.operation = this::pop;
                break;
            case CALL:
                this.operation = this::call;
                break;
            default:
                throw new RuntimeException("unsupported instr type: " + instType);
        }
    }

    public MachineState inc(MachineState state, Operand dest) {
        BigInteger result = dest.getValue(state).add(BigInteger.ONE);

        Map<String, Boolean> flags = new HashMap<String, Boolean>();
        flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

        result = truncate(result);
        setSignAndZeroFlags(result, flags);

        return dest.updateState(state, Optional.of(result), flags, true);
    }

    public MachineState dec(MachineState state, Operand dest) {
        BigInteger result = dest.getValue(state).subtract(BigInteger.ONE);

        Map<String, Boolean> flags = new HashMap<String, Boolean>();
        flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

        result = truncate(result);
        setSignAndZeroFlags(result, flags);

        return dest.updateState(state, Optional.of(result), flags, true);
    }

    public MachineState neg(MachineState state, Operand dest) {
        BigInteger orig = dest.getValue(state);
        BigInteger result = orig.negate();

        Map<String, Boolean> flags = new HashMap<String, Boolean>();
        flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

        result = truncate(result);
        setSignAndZeroFlags(result, flags);
        flags.put("cf", orig.compareTo(BigInteger.ZERO) != 0);

        return dest.updateState(state, Optional.of(result), flags, true);
    }

    public MachineState not(MachineState state, Operand dest) {
        BigInteger result = dest.getValue(state).not();
        Map<String, Boolean> flags = new HashMap<String, Boolean>();
        return dest.updateState(state, Optional.of(result), flags, true);
    }

    public MachineState push(MachineState state, Operand src) {
        Map<String, Boolean> flags = new HashMap<String, Boolean>();

        // step 1: subtract 8 from rsp
        RegOperand rsp = new RegOperand("rsp", OpSize.QUAD);
        MachineState tmp = rsp.updateState(state, Optional.of(rsp.getValue(state).subtract(BigInteger.valueOf(8))), flags, false);

        // step 2: store src operand value in (%rsp)
        MemoryOperand dest = new MemoryOperand("rsp", null, 1, 0, this.opSize);

        return dest.updateState(tmp, Optional.of(src.getValue(tmp)), flags, true);
    }

    public MachineState pop(MachineState state, Operand dest) {
        Map<String, Boolean> flags = new HashMap<String, Boolean>();

        // step 1: store (%rsp) value in dest operand 
        MemoryOperand src = new MemoryOperand("rsp", null, 1, 0, this.opSize);
        MachineState tmp = dest.updateState(state, Optional.of(src.getValue(state)), flags, true);

        // step 2: add 8 to rsp
        RegOperand rsp = new RegOperand("rsp", OpSize.QUAD);

        return rsp.updateState(tmp, Optional.of(rsp.getValue(tmp).add(BigInteger.valueOf(8))), flags, false);
    }

    public MachineState set(MachineState state, Operand dest) {
        assert this.conditionCheck.isPresent();
        BigInteger result = this.conditionCheck.get().test(state) ? BigInteger.ONE : BigInteger.ZERO;
        Map<String, Boolean> flags = new HashMap<String, Boolean>();
        return dest.updateState(state, Optional.of(result), flags, true);
    }

    public MachineState jump(MachineState state, Operand dest) {
        assert this.conditionCheck.isPresent();
        Map<String, Boolean> flags = new HashMap<String, Boolean>();
        if(this.conditionCheck.get().test(state)){
            return dest.updateState(state, Optional.of(dest.getValue(state)), flags, false); 
        } else {
            return dest.updateState(state, Optional.empty(), flags, true); 
        }
    }
    
    public MachineState call(MachineState state, Operand dest) {
        Map<String, Boolean> flags = new HashMap<String, Boolean>();
        
        // step 1: subtract 8 from rsp
        RegOperand rsp = new RegOperand("rsp", OpSize.QUAD);
        MachineState tmp = rsp.updateState(state, Optional.of(rsp.getValue(state).subtract(BigInteger.valueOf(8))), flags, false);
        
        BigInteger returnAddr = tmp.getRipRegister().add(BigInteger.ONE);

        // step 2: store return address in (%rsp)
        MemoryOperand rspMemOperand = new MemoryOperand("rsp", null, 1, 0, this.opSize);
        tmp = rspMemOperand.updateState(tmp, Optional.of(returnAddr), flags, false);

        // return new state with rip set to beginning of callee
        return dest.updateState(tmp, Optional.of(dest.getValue(state)), flags, false); 
    }

    @Override
    public MachineState eval(MachineState state) {
        return operation.apply(state, this.destination);
    }

    @Override
    public Set<String> getUsedRegisters() {
        return destination.getUsedRegisters();
    }
    
    @Override
    public void updateLabels(String labelName, x86Label label){
        destination.updateLabels(labelName, label);
    }
    
    @Override
    public String toString() {
        return lineNum + ": \t" + getInstructionTypeString() + " " + destination.toString();
    }
}

