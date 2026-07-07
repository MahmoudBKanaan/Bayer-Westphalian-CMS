import { spawn } from "node:child_process";

const cacheId = `${Date.now()}-${process.pid}`;
const npmCommand = process.platform === "win32" ? "npm.cmd" : "npm";

const child = spawn(npmCommand, ["run", "dev", "--", "--host", "127.0.0.1"], {
  env: {
    ...process.env,
    VITE_CACHE_DIR: `../.vite-cache-e2e-${cacheId}`,
  },
  shell: process.platform === "win32",
  stdio: "inherit",
});

const forwardSignal = (signal) => {
  if (!child.killed) {
    child.kill(signal);
  }
};

process.on("SIGINT", forwardSignal);
process.on("SIGTERM", forwardSignal);

child.on("exit", (code, signal) => {
  if (signal != null) {
    process.kill(process.pid, signal);
    return;
  }

  process.exit(code ?? 0);
});
