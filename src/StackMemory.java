import java.util.ArrayDeque;
import java.util.Deque;

/**
 * StackMemory.java
 * The operand stack used by the VM. ArrayDeque gives O(1) push/pop.
 * toArray() exposes stack contents bottom→top for the GUI visualizer.
 */
public class StackMemory {

    private final Deque<Double> stack;
    private int peakDepth;

    public StackMemory() {
        this.stack = new ArrayDeque<>();
        this.peakDepth = 0;
    }

    public void push(double value) {
        stack.push(value);
        if (stack.size() > peakDepth)
            peakDepth = stack.size();
    }

    public double pop() {
        if (stack.isEmpty())
            throw new VMException("Stack underflow — nothing to pop.");
        return stack.pop();
    }

    public double peek() {
        if (stack.isEmpty())
            throw new VMException("Stack is empty.");
        return stack.peek();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public int size() {
        return stack.size();
    }

    public int getPeakDepth() {
        return peakDepth;
    }

    public void clear() {
        stack.clear();
    }

    /**
     * Return all stack values as a double array, index 0 = bottom, last = top.
     * Used by the GUI StackVisualizer to render the current state.
     */
    public double[] toArray() {
        Double[] arr = stack.toArray(new Double[0]);
        double[] result = new double[arr.length];
        // ArrayDeque.toArray() puts head (top of stack) first → reverse it
        for (int i = 0; i < arr.length; i++) {
            result[i] = arr[arr.length - 1 - i];
        }
        return result;
    }

    @Override
    public String toString() {
        return "Stack(top→bottom): " + stack;
    }

    /** Runtime error thrown by the VM on stack violations. */
    public static class VMException extends RuntimeException {
        public VMException(String msg) {
            super(msg);
        }
    }
}
