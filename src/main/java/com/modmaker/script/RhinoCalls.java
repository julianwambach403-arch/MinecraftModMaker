package com.modmaker.script;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/**
 * Invokes JavaScript callback functions directly. We intentionally do not use Rhino's
 * automatic interface adapters: they are backed by {@code java.lang.reflect.Proxy}
 * classes which the sandbox {@code ClassShutter} rightfully refuses to expose.
 */
public final class RhinoCalls {
	private RhinoCalls() {
	}

	public static void call(ScriptManager manager, Function function, Object... args) {
		Context context = Context.enter();
		try {
			Scriptable scope = function.getParentScope();
			Object[] jsArgs = new Object[args.length];
			for (int i = 0; i < args.length; i++) {
				jsArgs[i] = Context.javaToJS(args[i], scope);
			}
			function.call(context, scope, scope, jsArgs);
		} catch (Exception e) {
			manager.reportRuntimeError(e);
		} finally {
			Context.exit();
		}
	}
}
