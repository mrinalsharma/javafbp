package com.jpaulmorrison.fbp.core.engine;

import java.util.UUID;

@FunctionalInterface
public interface IdGenerator {

	/**
	 * Generate a new identifier.
	 * @return the generated identifier
	 */
	public UUID generateId();

}
