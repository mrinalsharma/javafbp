package com.jpaulmorrison.fbp.core.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/*
 * provide additional abstraction over Packet API
 */
public class PacketAttributeAccessor {
	public static final String CORRELATION_ID = "correlationId";

	public static final String EXPIRATION_DATE = "expirationDate";
	
	public static final String SEQUENCE_NUMBER = "sequenceNumber";

	public static final String SEQUENCE_SIZE = "sequenceSize";
	
	public static final String SEQUENCE_DETAILS = "sequenceDetails";

	private PacketAttributes attrs;

	public PacketAttributeAccessor() {
		this(null);
	}

	public PacketAttributeAccessor(Packet<?> packet) {
		this.attrs = new PacketAttributes(packet != null ? packet.getAttributes() : null);
	}

	public Map<String, Object> toMap() {
		return new HashMap<String, Object>(this.attrs);
	}
	
	public void copyAttributes(Map<String, ?> attrToCopy) {
		if (attrs == null || this.attrs == attrs) {
			return;
		}
		attrToCopy.forEach((key, value) -> {
			setAttribute(key, value);
		});
	}

	public PacketAttributes getMessageAttributes() {
		return this.attrs;
	}

	protected Map<String, Object> getRawAttributes() {
		return this.attrs;
	}

	public Object getAttribute(String headerName) {
		return this.attrs.get(headerName);
	}

	protected void verifyType(String attrName, Object attrValue) {
		if (attrName != null && attrValue != null) {

			if (!(attrValue instanceof String)) {
				throw new IllegalArgumentException("'" + attrName + "' attribute value must be a String");
			}
		}
	}

	public void setAttribute(String name, Object value) {
		verifyType(name, value);
		if (value != null)
			this.getRawAttributes().put(name, value);
		else
			this.getRawAttributes().remove(name);

	}

	public void setAttributeIfAbsent(String name, Object value) {
		if (getAttribute(name) == null) {
			setAttribute(name, value);
		}
	}

	public void removeAttribute(String headerName) {
		setAttribute(headerName, null);
	}

	public void removeAttributes(String... headerPatterns) {
		List<String> headersToRemove = new ArrayList<>();
		for (String pattern : headerPatterns) {
			headersToRemove.add(pattern);
		}
		for (String headerToRemove : headersToRemove) {
			removeAttribute(headerToRemove);
		}
	}

	public String getDetailedLogMessage(Object payload) {
		return "headers=" + this.attrs.toString() + getDetailedPayloadLogMessage(payload);
	}
	
	public String getCorrelationId() {
		Optional<String> opt =  Optional.ofNullable((String)getAttribute(CORRELATION_ID));
		if(opt.isPresent())
		{
			return opt.get().toString();
		}
		
		return null ;
	}
	
	public int getSequenceNumber() {
		Number sequenceNumber = attrs.get(SEQUENCE_NUMBER, Number.class);
		return (sequenceNumber != null ? sequenceNumber.intValue() : 0);
	}
	
	public int getSequenceSize() {
		Number sequenceSize = attrs.get(SEQUENCE_SIZE, Number.class);
		return (sequenceSize != null ? sequenceSize.intValue() : 0);
	}

	protected String getDetailedPayloadLogMessage(Object payload) {
		if (payload instanceof String) {
			return " payload=" + payload;
		} else if (payload instanceof byte[]) {
			return " payload=byte[" + ((byte[]) payload).length + "]";
		} else {
			return " payload=" + payload;
		}
	}

}
