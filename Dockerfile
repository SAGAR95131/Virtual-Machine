# ─────────────────────────────────────────────────────────────────────────────
#  Dockerfile  —  Hypervisor VM  ·  Multi-Language Runtime on Linux
#
#  Host OS  : Ubuntu 22.04 (Jammy) — Linux
#  Languages : Custom VM | Python 3 | C (gcc) | C++ (g++) | Java | JavaScript
#  Hypervisor: Docker via Hyper-V / WSL2
#
#  Build :  docker build -t hypervisor-vm .
#  Run   :  docker run --rm hypervisor-vm                    ← interactive menu
#            docker run --rm hypervisor-vm program.vm         ← run custom VM file
#            docker run --rm -v "%cd%:/app/code" hypervisor-vm code/hello.py
#            docker run --rm -v "%cd%:/app/code" hypervisor-vm code/main.c
#            docker run --rm -v "%cd%:/app/code" hypervisor-vm code/app.js
# ─────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-jammy AS builder

LABEL maintainer="SAGAR95131"
LABEL description="Hypervisor VM — Multi-Language Runtime on Linux"
LABEL version="2.0"

WORKDIR /build

# Copy all Java source files
COPY src/ ./src/

# Compile all Java sources including HypervisorVM
RUN mkdir -p out && \
    javac -d out \
        src/Instruction.java \
        src/StackMemory.java \
        src/ProgramLoader.java \
        src/InstructionParser.java \
        src/Interpreter.java \
        src/AITranslator.java \
        src/LanguageRunner.java \
        src/HypervisorVM.java \
        src/GUI.java \
        src/VM.java && \
    echo "✔ [Build] All Java sources compiled successfully."

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy AS runtime

LABEL maintainer="SAGAR95131"
LABEL description="Hypervisor VM — Multi-Language Runtime on Linux"

WORKDIR /app

# ── Install ALL language runtimes ─────────────────────────────────────────────
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        # Python 3
        python3 \
        python3-pip \
        # C and C++ compilers (GCC/G++)
        gcc \
        g++ \
        build-essential \
        # Node.js (JavaScript)
        nodejs \
        npm \
        # Utilities
        curl \
        wget \
        vim \
        && rm -rf /var/lib/apt/lists/*

# Create 'python' alias → python3
RUN ln -sf /usr/bin/python3 /usr/bin/python

# Copy compiled Java classes
COPY --from=builder /build/out ./out/

# Copy programs
COPY program.vm ./program.vm

# Create a sample directory for user code
RUN mkdir -p /app/code

# ── Create Linux run script ───────────────────────────────────────────────────
RUN echo '#!/bin/bash\n\
echo ""\n\
echo "🐧 Hypervisor VM — Linux Runtime Info"\n\
echo "═══════════════════════════════════════════"\n\
echo "  OS:          $(uname -srm)"\n\
echo "  Hostname:    $(hostname)"\n\
echo "  Java:        $(java -version 2>&1 | head -1)"\n\
echo "  Python:      $(python3 --version)"\n\
echo "  GCC:         $(gcc --version | head -1)"\n\
echo "  G++:         $(g++ --version | head -1)"\n\
echo "  Node.js:     $(node --version)"\n\
echo "═══════════════════════════════════════════"\n\
echo ""\n\
' > /app/sysinfo.sh && chmod +x /app/sysinfo.sh

# ── Entrypoint: HypervisorVM ──────────────────────────────────────────────────
ENTRYPOINT ["/bin/bash", "-c", "bash /app/sysinfo.sh && java -cp /app/out HypervisorVM \"$@\"", "--"]

# Default: run program.vm
CMD ["program.vm"]
