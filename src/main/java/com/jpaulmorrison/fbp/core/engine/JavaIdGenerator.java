package com.jpaulmorrison.fbp.core.engine;

import java.util.UUID;

public class JavaIdGenerator implements IdGenerator {

	@Override
	public UUID generateId() {
		return UUID.randomUUID();
	}

}
