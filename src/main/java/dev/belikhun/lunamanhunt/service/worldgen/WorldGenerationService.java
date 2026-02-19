package dev.belikhun.lunamanhunt.service.worldgen;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorldGenerationService {
	private static final String PAPER_PROJECT = "paper";
	private static final String PAPER_PROJECT_URL = "https://fill.papermc.io/v3/projects/" + PAPER_PROJECT;
	private static final String PAPER_BUILDS_URL = PAPER_PROJECT_URL + "/versions/%s/builds";
	private static final String HISTORY_FILE = "history.properties";
	private static final Pattern PREPARE_SPAWN_PATTERN = Pattern.compile("Preparing spawn area: (\\d+)%");
	private static final String PAPER_CHANNEL_STABLE = "STABLE";
	private static final String PAPER_DOWNLOAD_KEY = "server:default";

	private final JavaPlugin plugin;
	private final Logger logger;
	private final ExecutorService executor;
	private final java.util.concurrent.ScheduledExecutorService progressTicker;
	private final Path generatorRoot;
	private final File worldContainer;
	private final String javaCommand;
	private final int maxMemoryMb;
	private final int maxConcurrentTasks;
	private final int startupTimeoutSeconds;
	private final String downloadUserAgent;
	private final GenerationHistory history;
	private final Path cacheBackupRoot;
	private final Path librariesBackupRoot;

	public WorldGenerationService(JavaPlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.logger = plugin.getLogger();
		this.maxConcurrentTasks = Math.max(1, plugin.getConfig().getInt("generator.maxConcurrentTasks", 2));
		this.executor = Executors.newFixedThreadPool(maxConcurrentTasks, runnable -> {
			Thread thread = new Thread(runnable, "lm-worldgen");
			thread.setDaemon(true);
			return thread;
		});
		this.progressTicker = Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable, "lm-worldgen-ticker");
			thread.setDaemon(true);
			return thread;
		});
		this.generatorRoot = plugin.getDataFolder().toPath().resolve("generator");
		this.worldContainer = plugin.getServer().getWorldContainer();
		this.javaCommand = plugin.getConfig().getString("generator.javaCommand", "java");
		this.maxMemoryMb = Math.max(64, plugin.getConfig().getInt("generator.maxMemoryMb", 512));
		this.startupTimeoutSeconds = Math.max(60, plugin.getConfig().getInt("generator.startupTimeoutSeconds", 180));
		this.downloadUserAgent = plugin.getConfig().getString(
			"generator.downloadUserAgent",
			"luna-manhunt/1.0 (https://github.com/Belikhun/LunaManhunt)"
		);
		this.history = new GenerationHistory(generatorRoot.resolve(HISTORY_FILE), logger);
		this.cacheBackupRoot = generatorRoot.resolve("paper-cache");
		this.librariesBackupRoot = generatorRoot.resolve("paper-libraries");
	}

	public CompletableFuture<GenerationHandle> generateWorlds(String matchId, String overworldName, String netherName,
			String endName, String version, Long seed, ProgressListener listener) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return generateWorldsBlocking(matchId, overworldName, netherName, endName, version, seed, listener);
			} catch (Exception ex) {
				String detail = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
				throw new WorldGenerationException("World generation failed: " + detail, ex);
			}
		}, executor);
	}

	private GenerationHandle generateWorldsBlocking(String matchId, String overworldName, String netherName,
			String endName, String version, Long seed, ProgressListener listener) throws Exception {
		Objects.requireNonNull(matchId, "matchId");
		Objects.requireNonNull(overworldName, "overworldName");
		Objects.requireNonNull(netherName, "netherName");
		Objects.requireNonNull(endName, "endName");
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(listener, "listener");

		GenerationProgressTracker tracker = new GenerationProgressTracker(version, listener, history, progressTicker);
		tracker.updateStage(GenerationStage.DOWNLOADING, 0.0);

		Files.createDirectories(generatorRoot);
		Path workDir = generatorRoot.resolve("match_" + matchId.toLowerCase());
		Process process = null;
		try {
			deleteDirectory(workDir);
			Files.createDirectories(workDir);
			restorePaperArtifacts(workDir, version);

			Path jarPath = ensureServerJar(version, tracker);
			tracker.updateStage(GenerationStage.SETUP, 0.0);
			writeEula(workDir);
			writeServerProperties(workDir, overworldName, seed);

			int serverPort = pickAvailablePort();
			int queryPort = pickAvailablePort();
			int rconPort = pickAvailablePort();
			updateServerPorts(workDir, serverPort, queryPort, rconPort);
			tracker.updateStage(GenerationStage.SETUP, 1.0);

			tracker.updateStage(GenerationStage.BOOTING, 0.0);
			ProcessBuilder builder = new ProcessBuilder(
				javaCommand,
				"-Xms" + maxMemoryMb + "M",
				"-Xmx" + maxMemoryMb + "M",
				"-jar",
				jarPath.toAbsolutePath().toString(),
				"nogui"
			);
			builder.directory(workDir.toFile());
			builder.redirectErrorStream(true);
			logger.info("[WorldGen] Command: " + String.join(" ", builder.command()));

			logger.info("[WorldGen] Starting generator for " + overworldName + " (" + version + ")");
			logger.info("[WorldGen] Work dir: " + workDir.toAbsolutePath());
			logger.info("[WorldGen] Jar: " + jarPath.toAbsolutePath());
			process = builder.start();
			Process finalProcess = process;

			AtomicBoolean doneSeen = new AtomicBoolean(false);
			CountDownLatch readyLatch = new CountDownLatch(1);
			Thread logThread = new Thread(() -> readProcessOutput(finalProcess, readyLatch, doneSeen, tracker, "OUT"),
				"lm-worldgen-stdout");
			Thread errThread = new Thread(() -> readProcessError(finalProcess, tracker),
				"lm-worldgen-stderr");
			logThread.setDaemon(true);
			errThread.setDaemon(true);
			logThread.start();
			errThread.start();

			boolean ready = readyLatch.await(startupTimeoutSeconds, TimeUnit.SECONDS);
			if (!ready) {
				logger.warning("[WorldGen] Generator did not report ready in time. Forcing stop.");
				process.destroyForcibly();
				throw new WorldGenerationException("Generator timed out after " + startupTimeoutSeconds + "s");
			}

			waitForDimensionFolders(workDir, overworldName, 30);

			logger.info("[WorldGen] Sending stop command to generator.");
			tracker.updateStage(GenerationStage.SAVING, 0.0);
			stopProcess(process);
			boolean exited = process.waitFor(30, TimeUnit.SECONDS);
			if (!exited) {
				logger.warning("[WorldGen] Generator did not stop in time. Forcing stop.");
				process.destroyForcibly();
			}
			tracker.updateStage(GenerationStage.SAVING, 1.0);

			tracker.updateStage(GenerationStage.REGISTERING, 0.0);
			moveGeneratedWorlds(workDir, overworldName, netherName, endName);
			logger.info("[WorldGen] Finished generator for " + overworldName + ".");
			return tracker.createHandle();
		} finally {
			if (process != null && process.isAlive()) {
				process.destroyForcibly();
			}
			backupPaperArtifacts(workDir, version);
			deleteDirectory(workDir);
		}
	}

	private void readProcessOutput(Process process, CountDownLatch readyLatch, AtomicBoolean doneSeen,
			GenerationProgressTracker tracker, String streamName) {
		try (BufferedReader reader = new BufferedReader(
			new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String trimmed = line.trim();
				logger.info("[WorldGen-" + streamName + "] " + trimmed);
				if (trimmed.contains("Starting minecraft server") || trimmed.contains("Starting Minecraft server")) {
					tracker.updateStage(GenerationStage.STARTING, 0.0);
				}
				if (trimmed.contains("Preparing spawn area")) {
					logger.info("[WorldGen] " + trimmed);
					Matcher matcher = PREPARE_SPAWN_PATTERN.matcher(trimmed);
					if (matcher.find()) {
						double percent = Math.min(100.0, Math.max(0.0, Double.parseDouble(matcher.group(1))));
						tracker.updateStage(GenerationStage.GENERATING, percent / 100.0);
					}
				}
				if (trimmed.contains("Done (") && doneSeen.compareAndSet(false, true)) {
					logger.info("[WorldGen] Server reported ready.");
					tracker.updateStage(GenerationStage.GENERATING, 1.0);
					readyLatch.countDown();
				}
				if (trimmed.contains("Stopping the server")) {
					logger.info("[WorldGen] Server stopping.");
				}
			}
		} catch (IOException ex) {
			logger.warning("[WorldGen] Failed to read generator output: " + ex.getMessage());
		}
	}

	private void readProcessError(Process process, GenerationProgressTracker tracker) {
		try (BufferedReader reader = new BufferedReader(
			new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String trimmed = line.trim();
				logger.warning("[WorldGen-ERR] " + trimmed);
				if (trimmed.contains("Starting minecraft server") || trimmed.contains("Starting Minecraft server")) {
					tracker.updateStage(GenerationStage.STARTING, 0.0);
				}
			}
		} catch (IOException ex) {
			logger.warning("[WorldGen] Failed to read generator error: " + ex.getMessage());
		}
	}

	private void stopProcess(Process process) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(
			new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
			writer.write("stop");
			writer.newLine();
			writer.flush();
		}
	}

	private void waitForDimensionFolders(Path workDir, String overworldName, int timeoutSeconds)
			throws InterruptedException {
		Path overworldSource = workDir.resolve(overworldName);
		Path[] netherCandidates = buildNetherCandidates(workDir, overworldSource, overworldName);
		Path[] endCandidates = buildEndCandidates(workDir, overworldSource, overworldName);
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
		boolean logged = false;

		while (System.nanoTime() < deadline) {
			if (findExisting(netherCandidates) != null && findExisting(endCandidates) != null) {
				return;
			}
			if (!logged) {
				logger.info("[WorldGen] Waiting for nether/end folders to be created.");
				logged = true;
			}
			Thread.sleep(1000L);
		}

		String netherList = joinCandidates(netherCandidates);
		String endList = joinCandidates(endCandidates);
		throw new WorldGenerationException("Generator did not create dimension folders in time: "
			+ netherList + " | " + endList);
	}

	private void moveGeneratedWorlds(Path workDir, String overworldName, String netherName, String endName) throws IOException {
		Path overworldSource = workDir.resolve(overworldName);
		Path netherSource = findExisting(buildNetherCandidates(workDir, overworldSource, overworldName));
		Path endSource = findExisting(buildEndCandidates(workDir, overworldSource, overworldName));

		Path overworldTarget = worldContainer.toPath().resolve(overworldName);
		Path netherTarget = worldContainer.toPath().resolve(netherName);
		Path endTarget = worldContainer.toPath().resolve(endName);

		if (!Files.exists(overworldSource)) {
			logger.warning("[WorldGen] Overworld folder missing: " + overworldSource);
			throw new WorldGenerationException("Missing overworld folder: " + overworldSource);
		}
		if (netherSource == null || !Files.exists(netherSource)) {
			String list = joinCandidates(buildNetherCandidates(workDir, overworldSource, overworldName));
			logger.warning("[WorldGen] Nether folder missing. Candidates: " + list);
			throw new WorldGenerationException("Missing nether folder. Candidates: " + list);
		}
		if (endSource == null || !Files.exists(endSource)) {
			String list = joinCandidates(buildEndCandidates(workDir, overworldSource, overworldName));
			logger.warning("[WorldGen] End folder missing. Candidates: " + list);
			throw new WorldGenerationException("Missing end folder. Candidates: " + list);
		}
		if (Files.exists(overworldTarget) || Files.exists(netherTarget) || Files.exists(endTarget)) {
			logger.warning("[WorldGen] Target world folder already exists.");
			logger.warning("[WorldGen] Overworld target exists: " + Files.exists(overworldTarget)
				+ " -> " + overworldTarget);
			logger.warning("[WorldGen] Nether target exists: " + Files.exists(netherTarget)
				+ " -> " + netherTarget);
			logger.warning("[WorldGen] End target exists: " + Files.exists(endTarget)
				+ " -> " + endTarget);
			throw new WorldGenerationException("Target world folder already exists in server root.");
		}

		Files.move(overworldSource, overworldTarget, StandardCopyOption.REPLACE_EXISTING);
		Files.move(netherSource, netherTarget, StandardCopyOption.REPLACE_EXISTING);
		Files.move(endSource, endTarget, StandardCopyOption.REPLACE_EXISTING);
	}

	private Path[] buildNetherCandidates(Path workDir, Path overworldSource, String overworldName) {
		return new Path[] {
			overworldSource.resolve("DIM-1"),
			overworldSource.resolve("dimensions").resolve("minecraft").resolve("the_nether"),
			workDir.resolve(overworldName + "_nether")
		};
	}

	private Path[] buildEndCandidates(Path workDir, Path overworldSource, String overworldName) {
		return new Path[] {
			overworldSource.resolve("DIM1"),
			overworldSource.resolve("dimensions").resolve("minecraft").resolve("the_end"),
			workDir.resolve(overworldName + "_the_end")
		};
	}

	private Path findExisting(Path[] candidates) {
		for (Path candidate : candidates) {
			if (Files.isDirectory(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	private String joinCandidates(Path[] candidates) {
		StringBuilder builder = new StringBuilder();
		for (Path candidate : candidates) {
			if (builder.length() > 0) {
				builder.append(" | ");
			}
			builder.append(candidate);
		}
		return builder.toString();
	}

	private void writeEula(Path workDir) throws IOException {
		Path eulaPath = workDir.resolve("eula.txt");
		Files.writeString(eulaPath, "eula=true" + System.lineSeparator(), StandardCharsets.UTF_8);
	}

	private void writeServerProperties(Path workDir, String overworldName, Long seed) throws IOException {
		Properties props = new Properties();
		props.setProperty("level-name", overworldName);
		if (seed != null) {
			props.setProperty("level-seed", String.valueOf(seed));
		}
		props.setProperty("level-type", "default");
		props.setProperty("generate-structures", "true");
		props.setProperty("spawn-protection", "0");
		props.setProperty("online-mode", "false");
		props.setProperty("enable-query", "false");
		props.setProperty("enable-rcon", "false");
		props.setProperty("allow-nether", "true");
		props.setProperty("hardcore", "false");
		props.setProperty("white-list", "false");
		props.setProperty("max-players", "1");
		props.setProperty("motd", "LunaManhunt Generator");
		props.setProperty("view-distance", "2");
		props.setProperty("simulation-distance", "2");

		Path propertiesPath = workDir.resolve("server.properties");
		try (var writer = Files.newBufferedWriter(propertiesPath, StandardCharsets.UTF_8)) {
			props.store(writer, "LunaManhunt World Generator");
		}
	}

	private void updateServerPorts(Path workDir, int serverPort, int queryPort, int rconPort) throws IOException {
		Path propertiesPath = workDir.resolve("server.properties");
		Properties props = new Properties();
		try (var reader = Files.newBufferedReader(propertiesPath, StandardCharsets.UTF_8)) {
			props.load(reader);
		}
		props.setProperty("server-port", String.valueOf(serverPort));
		props.setProperty("query.port", String.valueOf(queryPort));
		props.setProperty("rcon.port", String.valueOf(rconPort));
		props.setProperty("server-ip", "127.0.0.1");

		try (var writer = Files.newBufferedWriter(propertiesPath, StandardCharsets.UTF_8)) {
			props.store(writer, "LunaManhunt World Generator");
		}
	}

	private int pickAvailablePort() {
		try (var socket = new java.net.ServerSocket(0)) {
			return socket.getLocalPort();
		} catch (IOException ex) {
			throw new WorldGenerationException("Unable to pick available port", ex);
		}
	}

	private Path ensureServerJar(String version, GenerationProgressTracker tracker) throws IOException {
		String jarName = version.replace(".", "") + "-paper.jar";
		Path jarPath = generatorRoot.resolve(jarName);
		if (Files.exists(jarPath)) {
			tracker.updateStage(GenerationStage.DOWNLOADING, 1.0);
			return jarPath;
		}
		String buildsJson = readString(String.format(PAPER_BUILDS_URL, version));
		String serverUrl = extractPaperServerUrl(buildsJson, version);
		downloadTo(serverUrl, jarPath, tracker);
		tracker.updateStage(GenerationStage.DOWNLOADING, 1.0);
		return jarPath;
	}

	private String extractPaperServerUrl(String buildsJson, String version) {
		JsonValue root = JsonParser.parse(buildsJson);
		if (root.type == JsonType.OBJECT) {
			JsonObject obj = root.asObject();
			JsonValue ok = obj.get("ok");
			if (ok != null && ok.isBoolean() && !ok.asBoolean()) {
				String message = obj.getString("message");
				throw new WorldGenerationException("Paper downloads API error for " + version + ": "
					+ (message == null ? "Unknown error" : message));
			}
		}
		if (!root.isArray()) {
			throw new WorldGenerationException("Unexpected Paper downloads response for " + version);
		}
		for (JsonValue entry : root.asArray().values) {
			if (!entry.isObject()) {
				continue;
			}
			JsonObject build = entry.asObject();
			String channel = build.getString("channel");
			if (!PAPER_CHANNEL_STABLE.equals(channel)) {
				continue;
			}
			JsonObject downloads = build.getObject("downloads");
			if (downloads == null) {
				continue;
			}
			JsonObject serverDefault = downloads.getObject(PAPER_DOWNLOAD_KEY);
			if (serverDefault == null) {
				continue;
			}
			String url = serverDefault.getString("url");
			if (url != null && !url.isBlank()) {
				return unescapeUrl(url);
			}
		}
		throw new WorldGenerationException("No stable Paper build for version: " + version);
	}

	private String readString(String url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
		connection.setRequestProperty("User-Agent", downloadUserAgent);
		connection.setConnectTimeout((int) Duration.ofSeconds(15).toMillis());
		connection.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
		int status = connection.getResponseCode();
		if (status >= 400) {
			throw new WorldGenerationException("HTTP " + status + " while fetching " + url);
		}
		try (var reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line).append('\n');
			}
			return builder.toString();
		}
	}

	private void downloadTo(String url, Path target, GenerationProgressTracker tracker) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
		connection.setRequestProperty("User-Agent", downloadUserAgent);
		connection.setConnectTimeout((int) Duration.ofSeconds(15).toMillis());
		connection.setReadTimeout((int) Duration.ofSeconds(60).toMillis());
		int status = connection.getResponseCode();
		if (status >= 400) {
			throw new WorldGenerationException("HTTP " + status + " while downloading " + url);
		}
		Files.createDirectories(target.getParent());
		long totalSize = connection.getContentLengthLong();
		Path tempTarget = target.resolveSibling(target.getFileName().toString() + ".tmp");
		try (var input = connection.getInputStream()) {
			try (var output = Files.newOutputStream(tempTarget)) {
				byte[] buffer = new byte[8192];
				long downloaded = 0L;
				long lastUpdate = System.nanoTime();
				int read;
				while ((read = input.read(buffer)) >= 0) {
					output.write(buffer, 0, read);
					downloaded += read;
					if (totalSize > 0) {
						long now = System.nanoTime();
						if (TimeUnit.NANOSECONDS.toMillis(now - lastUpdate) >= 250) {
							double progress = Math.min(1.0, (double) downloaded / (double) totalSize);
							tracker.updateStage(GenerationStage.DOWNLOADING, progress);
							lastUpdate = now;
						}
					}
				}
			}
			Files.move(tempTarget, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private String unescapeUrl(String value) {
		return value.replace("\\/", "/");
	}

	private void deleteDirectory(Path path) {
		if (path == null || !Files.exists(path)) {
			return;
		}
		try {
			Files.walk(path)
				.sorted((a, b) -> b.compareTo(a))
				.forEach(p -> {
					try {
						Files.deleteIfExists(p);
					} catch (IOException ex) {
						logger.warning("[WorldGen] Failed to delete " + p + ": " + ex.getMessage());
					}
				});
		} catch (IOException ex) {
			logger.warning("[WorldGen] Failed to clean generator dir: " + ex.getMessage());
		}
	}

	private void restorePaperArtifacts(Path workDir, String version) {
		if (workDir == null) {
			return;
		}
		logger.info("[WorldGen] Restoring Paper cache/libraries for version " + version + ".");
		restoreDirectory(cacheBackupRoot.resolve(version), workDir.resolve("cache"));
		restoreDirectory(librariesBackupRoot.resolve(version), workDir.resolve("libraries"));
	}

	private void backupPaperArtifacts(Path workDir, String version) {
		if (workDir == null || !Files.exists(workDir)) {
			return;
		}
		logger.info("[WorldGen] Backing up Paper cache/libraries for version " + version + ".");
		backupDirectory(workDir.resolve("cache"), cacheBackupRoot.resolve(version));
		backupDirectory(workDir.resolve("libraries"), librariesBackupRoot.resolve(version));
	}

	private void backupDirectory(Path sourceDir, Path backupDir) {
		if (sourceDir == null || backupDir == null || !Files.exists(sourceDir)) {
			if (sourceDir != null && backupDir != null) {
				logger.info("[WorldGen] Skip backup (missing source): " + sourceDir);
			}
			return;
		}
		deleteDirectory(backupDir);
		try {
			Files.createDirectories(backupDir.getParent());
			Files.move(sourceDir, backupDir, StandardCopyOption.REPLACE_EXISTING);
			logger.info("[WorldGen] Backed up " + sourceDir + " -> " + backupDir);
		} catch (IOException ex) {
			logger.warning("[WorldGen] Failed to backup " + sourceDir + ": " + ex.getMessage());
		}
	}

	private void restoreDirectory(Path backupDir, Path targetDir) {
		if (backupDir == null || targetDir == null || !Files.exists(backupDir)) {
			if (backupDir != null && targetDir != null) {
				logger.info("[WorldGen] Skip restore (missing backup): " + backupDir);
			}
			return;
		}
		if (Files.exists(targetDir)) {
			logger.info("[WorldGen] Skip restore (target exists): " + targetDir);
			return;
		}
		try {
			copyDirectory(backupDir, targetDir);
			logger.info("[WorldGen] Restored " + backupDir + " -> " + targetDir);
		} catch (IOException ex) {
			logger.warning("[WorldGen] Failed to restore " + backupDir + ": " + ex.getMessage());
		}
	}

	private void copyDirectory(Path source, Path target) throws IOException {
		Files.walk(source).forEach(path -> {
			Path relative = source.relativize(path);
			Path destination = target.resolve(relative);
			try {
				if (Files.isDirectory(path)) {
					Files.createDirectories(destination);
				} else {
					Files.createDirectories(destination.getParent());
					Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (IOException ex) {
				throw new WorldGenerationException("Failed to copy " + path + " to " + destination, ex);
			}
		});
	}

	public static class WorldGenerationException extends RuntimeException {
		public WorldGenerationException(String message) {
			super(message);
		}

		public WorldGenerationException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public enum GenerationStage {
		DOWNLOADING,
		SETUP,
		BOOTING,
		STARTING,
		GENERATING,
		SAVING,
		REGISTERING,
		DONE
	}

	public static class GenerationProgress {
		private final GenerationStage stage;
		private final double stageProgress;
		private final double overallProgress;
		private final long etaSeconds;

		public GenerationProgress(GenerationStage stage, double stageProgress, double overallProgress, long etaSeconds) {
			this.stage = stage;
			this.stageProgress = stageProgress;
			this.overallProgress = overallProgress;
			this.etaSeconds = etaSeconds;
		}

		public GenerationStage getStage() {
			return stage;
		}

		public double getStageProgress() {
			return stageProgress;
		}

		public double getOverallProgress() {
			return overallProgress;
		}

		public long getEtaSeconds() {
			return etaSeconds;
		}
	}

	public interface ProgressListener {
		void onProgress(GenerationProgress progress);
	}

	public interface GenerationHandle {
		void beginRegistering();
		void updateRegistering(double progress);
		void completeRegistering();
	}

	private static class GenerationHistory {
		private final Path filePath;
		private final Logger logger;
		private final Properties properties = new Properties();
		private boolean loaded;

		GenerationHistory(Path filePath, Logger logger) {
			this.filePath = filePath;
			this.logger = logger;
		}

		synchronized double getAverageSeconds(String version, GenerationStage stage) {
			ensureLoaded();
			String key = buildKey(version, stage);
			String value = properties.getProperty(key);
			if (value == null) {
				return -1.0;
			}
			try {
				return Double.parseDouble(value);
			} catch (NumberFormatException ex) {
				return -1.0;
			}
		}

		synchronized void updateAverageSeconds(String version, GenerationStage stage, double seconds) {
			ensureLoaded();
			String key = buildKey(version, stage);
			double previous = getAverageSeconds(version, stage);
			double next = previous > 0 ? previous * 0.7 + seconds * 0.3 : seconds;
			properties.setProperty(key, String.format(java.util.Locale.US, "%.3f", next));
			save();
		}

		private void ensureLoaded() {
			if (loaded) {
				return;
			}
			loaded = true;
			if (!Files.exists(filePath)) {
				return;
			}
			try (var reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
				properties.load(reader);
			} catch (IOException ex) {
				logger.warning("[WorldGen] Failed to read history: " + ex.getMessage());
			}
		}

		private void save() {
			try {
				Files.createDirectories(filePath.getParent());
				try (var writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
					properties.store(writer, "LunaManhunt world generation history");
				}
			} catch (IOException ex) {
				logger.warning("[WorldGen] Failed to save history: " + ex.getMessage());
			}
		}

		private String buildKey(String version, GenerationStage stage) {
			return version + "." + stage.name() + ".avgSeconds";
		}
	}

	private static class GenerationProgressTracker {
		private final String version;
		private final ProgressListener listener;
		private final GenerationHistory history;
		private final java.util.concurrent.ScheduledExecutorService ticker;
		private final Object lock = new Object();
		private final java.util.LinkedHashMap<GenerationStage, Double> weights = new java.util.LinkedHashMap<>();
		private GenerationStage currentStage;
		private long stageStartNanos;
		private double stageProgress;
		private long lastEmitMillis;
		private java.util.concurrent.ScheduledFuture<?> tickerFuture;

		GenerationProgressTracker(String version, ProgressListener listener, GenerationHistory history,
				java.util.concurrent.ScheduledExecutorService ticker) {
			this.version = version;
			this.listener = listener;
			this.history = history;
			this.ticker = ticker;
			weights.put(GenerationStage.DOWNLOADING, 0.10);
			weights.put(GenerationStage.SETUP, 0.05);
			weights.put(GenerationStage.BOOTING, 0.10);
			weights.put(GenerationStage.STARTING, 0.10);
			weights.put(GenerationStage.GENERATING, 0.45);
			weights.put(GenerationStage.SAVING, 0.10);
			weights.put(GenerationStage.REGISTERING, 0.10);
			weights.put(GenerationStage.DONE, 0.00);
			this.tickerFuture = ticker.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
		}

		void updateStage(GenerationStage stage, double progress) {
			double clamped = Math.max(0.0, Math.min(1.0, progress));
			long nowNanos = System.nanoTime();
			synchronized (lock) {
				if (currentStage != stage) {
					if (currentStage != null) {
						double elapsedSeconds = (nowNanos - stageStartNanos) / 1_000_000_000.0;
						history.updateAverageSeconds(version, currentStage, elapsedSeconds);
					}
					currentStage = stage;
					stageStartNanos = nowNanos;
					stageProgress = clamped;
					emitProgress(nowNanos, true);
					if (stage == GenerationStage.DONE) {
						stopTicker();
					}
					return;
				}
				stageProgress = clamped;
				emitProgress(nowNanos, false);
			}
		}

		private void tick() {
			long nowNanos = System.nanoTime();
			synchronized (lock) {
				if (currentStage == null || currentStage == GenerationStage.DONE) {
					return;
				}
				emitProgress(nowNanos, true);
			}
		}

		private void stopTicker() {
			if (tickerFuture == null) {
				return;
			}
			tickerFuture.cancel(false);
			tickerFuture = null;
		}

		GenerationHandle createHandle() {
			return new GenerationHandle() {
				@Override
				public void beginRegistering() {
					updateStage(GenerationStage.REGISTERING, 0.0);
				}

				@Override
				public void updateRegistering(double progress) {
					updateStage(GenerationStage.REGISTERING, progress);
				}

				@Override
				public void completeRegistering() {
					updateStage(GenerationStage.DONE, 1.0);
				}
			};
		}

		private void emitProgress(long nowNanos, boolean force) {
			long nowMillis = TimeUnit.NANOSECONDS.toMillis(nowNanos);
			if (!force && nowMillis - lastEmitMillis < 200) {
				return;
			}
			lastEmitMillis = nowMillis;
			double overall = computeOverallProgress(nowNanos);
			long etaSeconds = estimateEtaSeconds(nowNanos);
			listener.onProgress(new GenerationProgress(currentStage, stageProgress, overall, etaSeconds));
		}

		private double computeOverallProgress(long nowNanos) {
			if (currentStage == null) {
				return 0.0;
			}
			double total = 0.0;
			for (var entry : weights.entrySet()) {
				GenerationStage stage = entry.getKey();
				double weight = entry.getValue();
				if (stage == currentStage) {
					double effective = getEffectiveStageProgress(nowNanos);
					total += weight * effective;
					break;
				}
				total += weight;
			}
			return Math.min(1.0, Math.max(0.0, total));
		}

		private long estimateEtaSeconds(long nowNanos) {
			if (currentStage == null) {
				return 0L;
			}
			double remaining = 0.0;
			boolean inCurrent = true;
			for (var entry : weights.entrySet()) {
				GenerationStage stage = entry.getKey();
				if (stage == GenerationStage.DONE) {
					continue;
				}
				if (stage == currentStage) {
					double hist = history.getAverageSeconds(version, stage);
					double elapsed = (nowNanos - stageStartNanos) / 1_000_000_000.0;
					double effectiveProgress = getEffectiveStageProgress(nowNanos);
					double estimate;
					if (effectiveProgress > 0.0) {
						estimate = elapsed * (1.0 - effectiveProgress) / effectiveProgress;
						if (hist > 0.0) {
							double histRemaining = Math.max(0.0, hist - elapsed);
							estimate = (estimate + histRemaining) / 2.0;
						}
					} else if (hist > 0.0) {
						estimate = hist;
					} else {
						estimate = 0.0;
					}
					remaining += Math.max(0.0, estimate);
					inCurrent = false;
					continue;
				}
				if (inCurrent) {
					continue;
				}
				double hist = history.getAverageSeconds(version, stage);
				if (hist > 0.0) {
					remaining += hist;
				}
			}
			return Math.max(0L, Math.round(remaining));
		}

		private double getEffectiveStageProgress(long nowNanos) {
			double effective = stageProgress;
			double hist = history.getAverageSeconds(version, currentStage);
			if (hist > 0.0) {
				double elapsed = (nowNanos - stageStartNanos) / 1_000_000_000.0;
				double inferred = Math.min(1.0, Math.max(0.0, elapsed / hist));
				effective = Math.max(effective, inferred);
			}
			return Math.min(1.0, Math.max(0.0, effective));
		}
	}

	private enum JsonType {
		OBJECT,
		ARRAY,
		STRING,
		NUMBER,
		BOOLEAN,
		NULL
	}

	private static class JsonValue {
		private final JsonType type;
		private final Object value;

		private JsonValue(JsonType type, Object value) {
			this.type = type;
			this.value = value;
		}

		boolean isObject() {
			return type == JsonType.OBJECT;
		}

		boolean isArray() {
			return type == JsonType.ARRAY;
		}

		boolean isString() {
			return type == JsonType.STRING;
		}

		boolean isBoolean() {
			return type == JsonType.BOOLEAN;
		}

		JsonObject asObject() {
			return (JsonObject) value;
		}

		JsonArray asArray() {
			return (JsonArray) value;
		}

		String asString() {
			return (String) value;
		}

		boolean asBoolean() {
			return (Boolean) value;
		}
	}

	private static class JsonObject {
		private final java.util.Map<String, JsonValue> values = new java.util.HashMap<>();

		JsonValue get(String key) {
			return values.get(key);
		}

		String getString(String key) {
			JsonValue value = values.get(key);
			return value != null && value.isString() ? value.asString() : null;
		}

		JsonObject getObject(String key) {
			JsonValue value = values.get(key);
			return value != null && value.isObject() ? value.asObject() : null;
		}
	}

	private static class JsonArray {
		private final java.util.List<JsonValue> values = new java.util.ArrayList<>();
	}

	private static class JsonParser {
		private final char[] input;
		private int index;

		private JsonParser(String source) {
			this.input = source.toCharArray();
		}

		static JsonValue parse(String source) {
			JsonParser parser = new JsonParser(source);
			JsonValue value = parser.readValue();
			parser.skipWhitespace();
			return value;
		}

		private JsonValue readValue() {
			skipWhitespace();
			if (index >= input.length) {
				throw new WorldGenerationException("Unexpected end of JSON");
			}
			char next = input[index];
			if (next == '{') {
				return new JsonValue(JsonType.OBJECT, readObject());
			}
			if (next == '[') {
				return new JsonValue(JsonType.ARRAY, readArray());
			}
			if (next == '\"') {
				return new JsonValue(JsonType.STRING, readString());
			}
			if (next == 't' || next == 'f') {
				return new JsonValue(JsonType.BOOLEAN, readBoolean());
			}
			if (next == 'n') {
				readNull();
				return new JsonValue(JsonType.NULL, null);
			}
			return new JsonValue(JsonType.NUMBER, readNumber());
		}

		private JsonObject readObject() {
			expect('{');
			JsonObject object = new JsonObject();
			skipWhitespace();
			if (peek('}')) {
				index++;
				return object;
			}
			while (true) {
				skipWhitespace();
				String key = readString();
				skipWhitespace();
				expect(':');
				JsonValue value = readValue();
				object.values.put(key, value);
				skipWhitespace();
				if (peek(',')) {
					index++;
					continue;
				}
				if (peek('}')) {
					index++;
					break;
				}
				throw new WorldGenerationException("Unexpected token in object");
			}
			return object;
		}

		private JsonArray readArray() {
			expect('[');
			JsonArray array = new JsonArray();
			skipWhitespace();
			if (peek(']')) {
				index++;
				return array;
			}
			while (true) {
				JsonValue value = readValue();
				array.values.add(value);
				skipWhitespace();
				if (peek(',')) {
					index++;
					continue;
				}
				if (peek(']')) {
					index++;
					break;
				}
				throw new WorldGenerationException("Unexpected token in array");
			}
			return array;
		}

		private String readString() {
			expect('\"');
			StringBuilder builder = new StringBuilder();
			while (index < input.length) {
				char ch = input[index++];
				if (ch == '\\') {
					if (index >= input.length) {
						throw new WorldGenerationException("Invalid escape sequence");
					}
					char esc = input[index++];
					switch (esc) {
						case '\\' -> builder.append('\\');
						case '"' -> builder.append('"');
						case '/' -> builder.append('/');
						case 'b' -> builder.append('\b');
						case 'f' -> builder.append('\f');
						case 'n' -> builder.append('\n');
						case 'r' -> builder.append('\r');
						case 't' -> builder.append('\t');
						case 'u' -> builder.append(readUnicode());
						default -> throw new WorldGenerationException("Invalid escape sequence");
					}
					continue;
				}
				if (ch == '\"') {
					return builder.toString();
				}
				builder.append(ch);
			}
			throw new WorldGenerationException("Unterminated string");
		}

		private char readUnicode() {
			if (index + 4 > input.length) {
				throw new WorldGenerationException("Invalid unicode escape");
			}
			int value = 0;
			for (int i = 0; i < 4; i++) {
				char ch = input[index++];
				int digit = Character.digit(ch, 16);
				if (digit < 0) {
					throw new WorldGenerationException("Invalid unicode escape");
				}
				value = (value << 4) + digit;
			}
			return (char) value;
		}

		private Boolean readBoolean() {
			if (matchLiteral("true")) {
				return Boolean.TRUE;
			}
			if (matchLiteral("false")) {
				return Boolean.FALSE;
			}
			throw new WorldGenerationException("Invalid boolean value");
		}

		private void readNull() {
			if (!matchLiteral("null")) {
				throw new WorldGenerationException("Invalid null value");
			}
		}

		private Double readNumber() {
			int start = index;
			if (peek('-')) {
				index++;
			}
			while (index < input.length && Character.isDigit(input[index])) {
				index++;
			}
			if (peek('.')) {
				index++;
				while (index < input.length && Character.isDigit(input[index])) {
					index++;
				}
			}
			if (peek('e') || peek('E')) {
				index++;
				if (peek('+') || peek('-')) {
					index++;
				}
				while (index < input.length && Character.isDigit(input[index])) {
					index++;
				}
			}
			String slice = new String(input, start, index - start);
			try {
				return Double.parseDouble(slice);
			} catch (NumberFormatException ex) {
				throw new WorldGenerationException("Invalid number value");
			}
		}

		private boolean matchLiteral(String literal) {
			int length = literal.length();
			if (index + length > input.length) {
				return false;
			}
			for (int i = 0; i < length; i++) {
				if (input[index + i] != literal.charAt(i)) {
					return false;
				}
			}
			index += length;
			return true;
		}

		private void skipWhitespace() {
			while (index < input.length && Character.isWhitespace(input[index])) {
				index++;
			}
		}

		private void expect(char ch) {
			if (index >= input.length || input[index] != ch) {
				throw new WorldGenerationException("Expected '" + ch + "'");
			}
			index++;
		}

		private boolean peek(char ch) {
			return index < input.length && input[index] == ch;
		}
	}
}
