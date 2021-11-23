package com.jpaulmorrison.fbp.core.engine;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PacketAttributes implements Map<String, Object>, Serializable {

	private static final long serialVersionUID = 7035068984262340920L;

	/**
	 * UUID for none.
	 */
	public static final UUID ID_VALUE_NONE = new UUID(0, 0);

	/**
	 * The key for the Message ID. This is an automatically generated UUID
	 */
	public static final String ID = "id";

	/**
	 * The key for the message timestamp.
	 */
	public static final String TIMESTAMP = "timestamp";

	/**
	 * The key for the message content type.
	 */
	public static final String CONTENT_TYPE = "contentType";

	private static volatile IdGenerator idGenerator = new JavaIdGenerator();

	private final Map<String, Object> attr;

	public PacketAttributes(Map<String, Object> attr) {
		this(attr, null, null);
	}

	protected PacketAttributes(Map<String, Object> attr, UUID id, Long timestamp) {
		this.attr = (attr != null ? new HashMap<>(attr) : new HashMap<>());

		if (id == null) {
			this.attr.put(ID, idGenerator.generateId());
		} else if (id == ID_VALUE_NONE) {
			this.attr.remove(ID);
		} else {
			this.attr.put(ID, id);
		}

		if (timestamp == null) {
			this.attr.put(TIMESTAMP, System.currentTimeMillis());
		} else if (timestamp < 0) {
			this.attr.remove(TIMESTAMP);
		} else {
			this.attr.put(TIMESTAMP, timestamp);
		}
	}

	private PacketAttributes(PacketAttributes original, Set<String> keysToIgnore) {
		this.attr = new HashMap<>();
		original.attr.forEach((key, value) -> {
			if (!keysToIgnore.contains(key)) {
				this.attr.put(key, value);
			}
		});
	}

	public <T> T get(Object key, Class<T> type) {
		Object value = this.attr.get(key);
		if (value == null) {
			return null;
		}
		if (!type.isAssignableFrom(value.getClass())) {
			throw new IllegalArgumentException("Incorrect type specified for attribute '" + key + "'. Expected [" + type
					+ "] but actual type is [" + value.getClass() + "]");
		}
		return (T) value;
	}

	public UUID getId() {
		return get(ID, UUID.class);
	}

	public Long getTimestamp() {
		return get(TIMESTAMP, Long.class);
	}

	@Override
	public int size() {
		return this.attr.size();
	}

	@Override
	public boolean isEmpty() {
		return this.attr.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.attr.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.attr.containsValue(value);
	}

	@Override
	public Object get(Object key) {
		return this.attr.get(key);
	}

	@Override
	public Object put(String key, Object value) {
		return this.attr.put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return this.attr.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		this.attr.putAll(m);

	}

	@Override
	public void clear() {
		this.attr.clear();

	}

	@Override
	public Set<String> keySet() {
		return this.attr.keySet();
	}

	@Override
	public Collection<Object> values() {
		return this.attr.values();
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		return this.attr.entrySet();
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		Set<String> keysToIgnore = new HashSet<>();
		this.attr.forEach((key, value) -> {
			if (!(value instanceof Serializable)) {
				keysToIgnore.add(key);
			}
		});

		if (keysToIgnore.isEmpty()) {
			out.defaultWriteObject();
		} else {
			out.writeObject(new PacketAttributes(this, keysToIgnore));
		}
	}

}
