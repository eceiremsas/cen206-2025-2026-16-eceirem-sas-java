package com.eceirem.kidsplanner;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

/**
 * Starts the project's real MySQL container (docker compose) for tests.
 *
 * <p>No fakes, no skips: if Docker is not available, tests fail with a clear message.</p>
 */
public final class DockerComposeMysql {
  private DockerComposeMysql() {
  }

  /** Repo root contains docker-compose.yml; tests run with cwd kidsplanner-app/. */
  private static final Path REPO_ROOT = Paths.get("..").toAbsolutePath().normalize();
  private static final String READY_JDBC_URL =
      "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
          + "&connectTimeout=2000&socketTimeout=2000";
  private static final String READY_USER = "root";
  private static final String READY_PASS = "kidsplanner";

  public static void ensureRunning() {
    if (isPortOpen("127.0.0.1", 3306, 150)) {
      waitForJdbcReady();
      return;
    }

    runDockerComposeUpDb();
    waitForPort("127.0.0.1", 3306, 30_000);
    waitForJdbcReady();
  }

  private static void waitForJdbcReady() {
    long timeoutMs = 30_000;
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
      try (Connection c = DriverManager.getConnection(READY_JDBC_URL, READY_USER, READY_PASS);
          Statement st = c.createStatement()) {
        st.execute("SELECT 1");
        return;
      } catch (Exception e) {
        // not ready yet
      }
      try {
        Thread.sleep(250);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        throw new AssertionError("Interrupted while waiting for MySQL JDBC readiness.", ie);
      }
    }
    throw new AssertionError("MySQL port is open but JDBC handshake is not ready within " + timeoutMs + "ms.");
  }

  private static void runDockerComposeUpDb() {
    try {
      ProcessBuilder pb = new ProcessBuilder("docker", "compose", "up", "-d", "db");
      pb.directory(REPO_ROOT.toFile());
      pb.redirectErrorStream(true);
      Process p = pb.start();
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      Thread drain = new Thread(() -> {
        try (InputStream in = p.getInputStream()) {
          byte[] buf = new byte[4096];
          int r;
          while ((r = in.read(buf)) != -1) {
            bout.write(buf, 0, r);
          }
        } catch (Exception ignore) {
          // ignore
        }
      }, "docker-compose-output-drain");
      drain.setDaemon(true);
      drain.start();

      boolean finished = p.waitFor(30, TimeUnit.SECONDS);
      if (!finished) {
        p.destroyForcibly();
        throw new AssertionError("Timed out while running `docker compose up -d db` (30s). Is Docker Desktop running?");
      }
      int code = p.exitValue();
      try {
        drain.join(1000);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
      if (code != 0) {
        String out = new String(bout.toByteArray(), StandardCharsets.UTF_8);
        throw new AssertionError("Failed to start MySQL via `docker compose up -d db` (exit " + code + "). Output:\n" + out);
      }
    } catch (Exception e) {
      throw new AssertionError(
          "Could not start MySQL via Docker Compose. Make sure Docker Desktop is running and `docker compose` works.",
          e);
    }
  }

  private static void waitForPort(String host, int port, long timeoutMs) {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
      if (isPortOpen(host, port, 250)) {
        return;
      }
      try {
        Thread.sleep(250);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        throw new AssertionError("Interrupted while waiting for MySQL to start.", ie);
      }
    }
    throw new AssertionError("MySQL did not become reachable on " + host + ":" + port + " within " + timeoutMs + "ms.");
  }

  private static boolean isPortOpen(String host, int port, int connectTimeoutMs) {
    try (Socket s = new Socket()) {
      s.connect(new InetSocketAddress(host, port), connectTimeoutMs);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}

