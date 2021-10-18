package com.jpaulmorrison.fbp.core.engine;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NodeJsMeta {
	private String className;
	private boolean subgraph;
	private String icon;
	private String description;
	private String name;
	private String schema;
	private boolean mustRun;
	private boolean selfStarting;
	private boolean keepRunning;
	private Map<String, InPort> inPorts = new HashMap<>();
	private Map<String, OutPort> OutPorts = new HashMap<>();

	public NodeJsMeta() {
		super();
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public boolean isSubgraph() {
		return subgraph;
	}

	public void setSubgraph(boolean subgraph) {
		this.subgraph = subgraph;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, InPort> getInPorts() {
		return inPorts;
	}

	public void setInPorts(Map<String, InPort> inPorts) {
		this.inPorts = inPorts;
	}

	public Map<String, OutPort> getOutPorts() {
		return OutPorts;
	}

	public void setOutPorts(Map<String, OutPort> outPorts) {
		OutPorts = outPorts;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}
	
	public boolean isMustRun() {
		return mustRun;
	}

	public void setMustRun(boolean mustRun) {
		this.mustRun = mustRun;
	}

	public boolean isSelfStarting() {
		return selfStarting;
	}

	public void setSelfStarting(boolean selfStarting) {
		this.selfStarting = selfStarting;
	}
    
	public boolean isKeepRunning() {
		return keepRunning;
	}

	public void setKeepRunning(boolean keepRunning) {
		this.keepRunning = keepRunning;
	}


	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class Port {
		private String name;
		private String description;
		private boolean addressable;
		private String datatype;
		private String schema;
		private String type;
		private boolean required;
		private boolean isIIP;
		private boolean arrayPort;
		private boolean fixedSize;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public boolean isAddressable() {
			return addressable;
		}

		public void setAddressable(boolean addressable) {
			this.addressable = addressable;
		}

		public String getDatatype() {
			return datatype;
		}

		public void setDatatype(String datatype) {
			this.datatype = datatype;
		}

		public String getSchema() {
			return schema;
		}

		public void setSchema(String schema) {
			this.schema = schema;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public boolean isRequired() {
			return required;
		}

		public void setRequired(boolean required) {
			this.required = required;
		}

		public boolean isIIP() {
			return isIIP;
		}

		public void setIIP(boolean isIIP) {
			this.isIIP = isIIP;
		}

		public boolean isArrayPort() {
			return arrayPort;
		}

		public void setArrayPort(boolean arrayPort) {
			this.arrayPort = arrayPort;
		}

		public boolean isFixedSize() {
			return fixedSize;
		}

		public void setFixedSize(boolean fixedSize) {
			this.fixedSize = fixedSize;
		}

	}

	public static class OutPort extends Port {

		public OutPort() {
			super();
		}

	}

	public static class InPort extends Port {

		public InPort() {
			super();
		}

	}
}
