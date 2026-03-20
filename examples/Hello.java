// Java — Example for Hypervisor VM
// Run: docker_run.bat examples\Hello.java

public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello from Java on Linux! 🐧");
        System.out.println("Running inside Docker Hypervisor!\n");

        // Factorial
        System.out.println("Factorials:");
        for (int i = 1; i <= 10; i++) {
            System.out.printf("  %2d! = %d%n", i, factorial(i));
        }
    }

    static long factorial(int n) {
        long result = 1;
        for (int i = 2; i <= n; i++) result *= i;
        return result;
    }
}
