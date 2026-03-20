/* C — Example for Hypervisor VM
   Run: docker_run.bat examples\hello.c */

#include <stdio.h>
#include <math.h>

int fibonacci(int n) {
    if (n <= 1) return n;
    return fibonacci(n-1) + fibonacci(n-2);
}

int main() {
    printf("Hello from C on Linux! 🐧\n");
    printf("Running inside Docker Hypervisor!\n\n");

    printf("Fibonacci sequence:\n");
    for (int i = 0; i < 10; i++) {
        printf("  fib(%d) = %d\n", i, fibonacci(i));
    }

    printf("\nSquare roots:\n");
    for (int i = 1; i <= 5; i++) {
        printf("  sqrt(%d) = %.4f\n", i, sqrt((double)i));
    }
    return 0;
}
