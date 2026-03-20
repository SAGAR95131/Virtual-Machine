// JavaScript (Node.js) — Example for Hypervisor VM
// Run: docker_run.bat examples\hello.js

console.log("Hello from JavaScript (Node.js) on Linux! 🐧");
console.log("Running inside Docker Hypervisor!\n");

// Async example
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));

async function main() {
    const languages = ["Custom VM", "Python", "C", "C++", "Java", "JavaScript"];
    console.log("Languages supported by HypervisorVM:");
    for (const lang of languages) {
        console.log(`  ✔ ${lang}`);
    }

    console.log("\nFibonacci (first 10):");
    let a = 0, b = 1;
    for (let i = 0; i < 10; i++) {
        process.stdout.write(a + " ");
        [a, b] = [b, a + b];
    }
    console.log();
}

main();
