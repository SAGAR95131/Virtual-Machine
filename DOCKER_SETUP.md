# 🐧 Running Custom VM on Linux via Docker

Your VM will run inside a **real Linux container** powered by Docker (which uses
the **Hyper-V** or **WSL2** hypervisor on Windows under the hood).

---

## Step 1 — Install Docker Desktop

1. Download Docker Desktop for Windows:  
   👉 https://www.docker.com/products/docker-desktop/

2. Run the installer (keep defaults — enable WSL2 backend when asked).

3. After install, **restart your PC**.

4. Open Docker Desktop and wait until it shows **"Engine running"**.

---

## Step 2 — Build the Linux Image (one time)

Open a terminal in this project folder and run:

```bat
docker_build.bat
```

This will:
- Pull the Ubuntu 22.04 + Java 17 base image from Docker Hub
- Compile all your Java source files inside Linux
- Install Python, GCC, G++ inside the Linux container
- Tag the image as `custom-vm`

---

## Step 3 — Run Your VM on Linux

```bat
docker_run.bat
```

This launches your VM **inside a Linux container**, runs `program.vm`, and
streams the output back to your Windows terminal.

### Run a custom .vm file:

```bat
docker_run.bat myprogram.vm
```

---

## What's Happening Under the Hood

```
┌─────────────────────────────────────────────────┐
│  Your VM IDE (Java CLI — program.vm)             │  ← your app
├─────────────────────────────────────────────────┤
│  JVM 17 (inside Ubuntu 22.04 Linux)             │  ← runs your bytecode
├─────────────────────────────────────────────────┤
│  Docker Linux Container                          │  ← isolated Linux OS
├─────────────────────────────────────────────────┤
│  Hyper-V / WSL2 Hypervisor  ← THIS IS IT ✅     │  ← real hypervisor layer
├─────────────────────────────────────────────────┤
│  Windows 11 (Host OS)                           │
├─────────────────────────────────────────────────┤
│  Physical Hardware (CPU, RAM)                   │
└─────────────────────────────────────────────────┘
```

Your language-based VM now lives **on top of a real hypervisor**! 🎉

---

## Useful Docker Commands

| Command | What it does |
|---|---|
| `docker images` | List all built images |
| `docker ps` | List running containers |
| `docker run --rm -it custom-vm bash` | Open a Linux shell inside your container |
| `docker rmi custom-vm` | Remove the image |
| `docker build -t custom-vm .` | Rebuild after code changes |
