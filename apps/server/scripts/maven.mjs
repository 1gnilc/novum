import { spawnSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const serverDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const commonArgs = ['--batch-mode', '--no-transfer-progress'];

const tasks = {
  build: [[...commonArgs, '-DskipTests', 'package']],
  clean: [[...commonArgs, 'clean']],
  dev: [
    [...commonArgs, '-DskipTests', 'install'],
    [...commonArgs, '-pl', 'novum-bootstrap', 'spring-boot:run'],
  ],
  test: [[...commonArgs, 'test']],
  typecheck: [[...commonArgs, '-DskipTests', 'compile']],
  verify: [[...commonArgs, 'verify']],
};

function getTaskInvocations(task) {
  const invocations = tasks[task];

  if (!invocations) {
    throw new Error(
      `Unknown Maven task "${task}". Expected one of: ${Object.keys(tasks).join(', ')}`,
    );
  }

  return invocations.map((args) => [...args]);
}

function resolveMavenExecutable() {
  const isWindows = process.platform === 'win32';
  const wrapper = join(serverDir, isWindows ? 'mvnw.cmd' : 'mvnw');

  if (existsSync(wrapper)) {
    return {
      command: isWindows ? String.raw`.\mvnw.cmd` : wrapper,
      shell: isWindows,
      source: 'wrapper',
    };
  }

  return {
    command: isWindows ? 'mvn.cmd' : 'mvn',
    shell: isWindows,
    source: 'system',
  };
}

function runTask(task) {
  const executable = resolveMavenExecutable();

  for (const args of getTaskInvocations(task)) {
    const result = spawnSync(executable.command, args, {
      cwd: serverDir,
      env: process.env,
      shell: executable.shell,
      stdio: 'inherit',
    });

    if (result.error) {
      const hint =
        executable.source === 'system'
          ? 'Install Maven or add mvn to PATH, or add Maven Wrapper files to apps/server.'
          : 'Check that the Maven Wrapper can be executed on this platform.';
      process.stderr.write(
        `Failed to start Maven: ${result.error.message}\n${hint}\n`,
      );
      return 1;
    }

    if (result.status !== 0) {
      return result.status ?? 1;
    }
  }

  return 0;
}

function main() {
  const task = process.argv[2];

  if (!task) {
    process.stderr.write(
      `Usage: node scripts/maven.mjs <${Object.keys(tasks).join('|')}>\n`,
    );
    return 1;
  }

  try {
    return runTask(task);
  } catch (error) {
    process.stderr.write(`${error.message}\n`);
    return 1;
  }
}

process.exitCode = main();
