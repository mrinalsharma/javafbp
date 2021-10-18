package com.jpaulmorrison.fbp.core.components.nodejs;

import com.caoccao.javet.enums.JSRuntimeType;
import com.caoccao.javet.interop.NodeRuntime;
import com.caoccao.javet.interop.engine.JavetEnginePool;

public class JaveteEnginePool {
	private static JavetEnginePool<NodeRuntime> enginePool = null;

	public static synchronized JavetEnginePool<NodeRuntime> getJavetEnginePool() {
		if (enginePool == null) {
			enginePool = new JavetEnginePool<>();
			enginePool.getConfig().setJSRuntimeType(JSRuntimeType.Node);
		}
		return enginePool;
	}

}
