package com.modmaker.script;

import com.modmaker.ModMaker;
import com.modmaker.ModMakerPaths;
import com.modmaker.script.api.PlayerApi;
import com.modmaker.script.api.ScriptApi;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * Loads and runs user JavaScript files with the bundled Rhino engine.
 *
 * <p>Scripts are plain files in {@code modmaker/scripts/} and can be hot-reloaded at any
 * time from the GUI or with {@code /modmaker reload}. Errors never crash the game; they are
 * collected and shown in the script editor and the chat.
 */
public final class ScriptManager {
	private static final ScriptManager INSTANCE = new ScriptManager();
	private static final int MAX_ERRORS = 30;

	private static final String PRELUDE = """
			var console = { log: function(m) { mm.log(String(m)); } };
			""";

	private final List<String> errors = new CopyOnWriteArrayList<>();
	private final List<TickTask> tickTasks = new CopyOnWriteArrayList<>();
	private final List<ScriptApi.ChatCallback> chatHandlers = new CopyOnWriteArrayList<>();
	private final List<ScriptApi.JoinCallback> joinHandlers = new CopyOnWriteArrayList<>();
	private volatile boolean initialized;
	private long tickCounter;

	private record TickTask(int interval, ScriptApi.TickCallback callback) {
	}

	private ScriptManager() {
	}

	public static ScriptManager get() {
		return INSTANCE;
	}

	public void init() {
		if (initialized) return;
		initialized = true;
		if (!ContextFactory.hasExplicitGlobal()) {
			ContextFactory.initGlobal(new SandboxContextFactory());
		}
		reload();
	}

	/** Restrict scripts to the ModMaker API; no arbitrary Java access from JS. */
	private static final class SandboxContextFactory extends ContextFactory {
		@Override
		protected Context makeContext() {
			Context context = super.makeContext();
			context.setLanguageVersion(Context.VERSION_ES6);
			context.setInterpretedMode(true);
			context.setClassShutter(className ->
					className.startsWith("com.modmaker.script.api.")
							|| className.equals("java.lang.String")
							|| className.equals("java.lang.Object"));
			return context;
		}
	}

	// ------------------------------------------------------------------ loading

	public synchronized void reload() {
		errors.clear();
		tickTasks.clear();
		chatHandlers.clear();
		joinHandlers.clear();
		ScriptEvents.clear();
		tickCounter = 0;

		List<Path> files = listScripts();
		for (Path file : files) {
			runFile(file);
		}
		ModMaker.LOGGER.info("ModMaker loaded {} script(s), {} error(s)", files.size(), errors.size());
	}

	public List<Path> listScripts() {
		Path dir = ModMakerPaths.scripts();
		if (!Files.isDirectory(dir)) return List.of();
		try (Stream<Path> stream = Files.list(dir)) {
			return stream.filter(p -> p.getFileName().toString().endsWith(".js")).sorted().toList();
		} catch (IOException e) {
			ModMaker.LOGGER.error("ModMaker could not list scripts", e);
			return List.of();
		}
	}

	private void runFile(Path file) {
		String source;
		try {
			source = Files.readString(file);
		} catch (IOException e) {
			addError(file.getFileName() + ": " + e.getMessage());
			return;
		}

		Context context = Context.enter();
		try {
			Scriptable scope = context.initSafeStandardObjects();
			ScriptableObject.putProperty(scope, "mm", Context.javaToJS(new ScriptApi(this), scope));
			context.evaluateString(scope, PRELUDE, "prelude", 1, null);
			context.evaluateString(scope, source, file.getFileName().toString(), 1, null);
		} catch (RhinoException e) {
			addError(file.getFileName() + " (Zeile " + e.lineNumber() + "): " + e.details());
		} catch (Exception e) {
			addError(file.getFileName() + ": " + e);
		} finally {
			Context.exit();
		}
	}

	// ------------------------------------------------------------------ handler registration (called by ScriptApi)

	public void addTickTask(int intervalTicks, ScriptApi.TickCallback callback) {
		tickTasks.add(new TickTask(Math.max(1, intervalTicks), callback));
	}

	public void addChatHandler(ScriptApi.ChatCallback callback) {
		chatHandlers.add(callback);
	}

	public void addJoinHandler(ScriptApi.JoinCallback callback) {
		joinHandlers.add(callback);
	}

	// ------------------------------------------------------------------ event dispatch (called by EventBridge)

	public void tick(MinecraftServer server) {
		tickCounter++;
		for (TickTask task : tickTasks) {
			if (tickCounter % task.interval() == 0) {
				try {
					task.callback().run();
				} catch (Exception e) {
					reportRuntimeError(e);
				}
			}
		}
	}

	public void fireChat(ServerPlayer player, String message) {
		for (ScriptApi.ChatCallback handler : chatHandlers) {
			try {
				handler.run(new PlayerApi(player), message);
			} catch (Exception e) {
				reportRuntimeError(e);
			}
		}
	}

	public void fireJoin(ServerPlayer player) {
		for (ScriptApi.JoinCallback handler : joinHandlers) {
			try {
				handler.run(new PlayerApi(player));
			} catch (Exception e) {
				reportRuntimeError(e);
			}
		}
	}

	// ------------------------------------------------------------------ errors

	public void reportRuntimeError(Exception e) {
		String message = e instanceof RhinoException rhino
				? "Laufzeitfehler (Zeile " + rhino.lineNumber() + "): " + rhino.details()
				: "Laufzeitfehler: " + e;
		addError(message);
	}

	private void addError(String message) {
		ModMaker.LOGGER.warn("ModMaker script error: {}", message);
		errors.add(message);
		while (errors.size() > MAX_ERRORS) {
			errors.remove(0);
		}
	}

	public List<String> errors() {
		return new ArrayList<>(errors);
	}
}
