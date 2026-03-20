# ─────────────────────────────────────────────────────────────────────────────
#  Dockerfile  —  Custom Stack-Based VM  (Linux / Headless CLI)
#
#  Build:   docker build -t custom-vm .
#  Run:     docker run --rm custom-vm
#  Run custom program:
#           docker run --rm -v "%cd%":/app/programs custom-vm programs/myprogram.vm
# ─────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-jammy AS builder

LABEL maintainer="Custom VM Project"
LABEL description="Stack-Based Virtual Machine — Language VM on Linux"

WORKDIR /build

# Copy all Java source files
COPY src/ ./src/

# Compile all sources into /build/out
RUN mkdir -p out && \
    javac -d out \
        src/Instruction.java \
        src/StackMemory.java \
        src/ProgramLoader.java \
        src/InstructionParser.java \
        src/Interpreter.java \
        src/AITranslator.java \
        src/LanguageRunner.java \
        src/GUI.java \
        src/VM.java && \
    echo "[Build] ✔ Compilation successful."

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

# Copy compiled classes from builder stage
COPY --from=builder /build/out ./out/

# Copy the default program
COPY program.vm ./program.vm

# Install Python, GCC, G++ so the LanguageRunner tab works if needed
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        python3 \
        python3-pip \
        gcc \
        g++ \
        && rm -rf /var/lib/apt/lists/*

# Create alias so 'python' works (Ubuntu uses python3 by default)
RUN ln -sf /usr/bin/python3 /usr/bin/python

# Default entrypoint: run VM in CLI mode with program.vm
# Pass a different .vm file as Docker CMD to override:
#   docker run --rm custom-vm myprogram.vm
ENTRYPOINT ["java", "-cp", "out", "VM"]
CMD ["program.vm"]
