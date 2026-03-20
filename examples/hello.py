# Python 3 — Example for Hypervisor VM
# Run: docker_run.bat examples\hello.py

def greet(name):
    return f"Hello from Python 3 on Linux! 🐍  → {name}"

numbers = [1, 2, 3, 4, 5]
total = sum(numbers)
squared = [x**2 for x in numbers]

print(greet("HypervisorVM"))
print(f"Sum of {numbers} = {total}")
print(f"Squares: {squared}")
print(f"Running on Linux inside Docker! 🐧")
