package com.jpaulmorrison.fbp.core.runtime;

import com.caoccao.javet.enums.JSRuntimeType;
import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.NodeRuntime;
import com.caoccao.javet.interop.engine.IJavetEngine;
import com.caoccao.javet.interop.engine.JavetEnginePool;
import com.caoccao.javet.values.primitive.V8ValueString;
import com.caoccao.javet.values.reference.V8ValueObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpaulmorrison.fbp.core.components.nodejs.NodeJs;
import com.jpaulmorrison.fbp.core.engine.Component;
import com.jpaulmorrison.fbp.core.engine.ComponentDescription;
import com.jpaulmorrison.fbp.core.engine.InPort;
import com.jpaulmorrison.fbp.core.engine.InPorts;
import com.jpaulmorrison.fbp.core.engine.Network;
import com.jpaulmorrison.fbp.core.engine.NodeJsMeta;
import com.jpaulmorrison.fbp.core.engine.OutPort;
import com.jpaulmorrison.fbp.core.engine.OutPorts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.Boolean;
import java.lang.Exception;
import java.lang.Iterable;
import java.lang.Override;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final public class FlowRuntime {

	public static class Util {

		public static class JSONObjectKeysIterable implements Iterable {
			JSONObject mObject;

			JSONObjectKeysIterable(JSONObject o) {
				mObject = o;
			}

			public java.util.Iterator<String> iterator() {
				return mObject.keys();
			}
		}

		public static abstract class Predicate<Item> {
			protected abstract boolean apply(Item i);
		}

		public static <Item> List<Item> filter(List<Item> in, Predicate<Item> f) {
			List<Item> out = new ArrayList<Item>(in.size());
			for (Item inObj : in) {
				if (f.apply(inObj)) {
					out.add(inObj);
				}
			}
			return out;
		}

		public static String stringFromStream(InputStream stream) throws UnsupportedEncodingException, IOException {
			BufferedReader in = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
			StringBuilder buf = new StringBuilder();
			String str;
			while ((str = in.readLine()) != null) {
				buf.append(str);
			}
			in.close();
			return buf.toString();
		}
	}

	public static class ComponentLibrary {

		private HashMap<String, Class> mComponents = new HashMap<String, Class>();
		private JSONArray jsonArray;
		private String nodeJsModulePath;

		public ComponentLibrary(String nodeJsModulePath) throws Exception {
			try {
				System.out.println("Instance Created");
				this.nodeJsModulePath = nodeJsModulePath;
				jsonArray = load();

			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		public JSONArray getJSON() {
			return jsonArray;
		}

		private JSONArray load() throws JSONException {

			Class[] classes = findAllClassesInPackages("com.jpaulmorrison.fbp.core.components");
			JSONArray components = buildComponentListFromClasses(classes);
			JSONArray nodeComponrnts = buildComponentListFromNodeJSModules(nodeJsModulePath);
			for (int index = 0; index < nodeComponrnts.length(); index++) {
				components.put(nodeComponrnts.getJSONObject(index));
			}

			return components;
		}

		public Class[] findAllClassesInPackages(String... packageNames) {
			final List<Class> result = new LinkedList<Class>();
			for (String packageName : packageNames) {
				result.addAll(findAllClassesUsingClassLoader(packageName));
			}
			return result.toArray(new Class<?>[result.size()]);
		}

		public List<Class> findAllClassesUsingClassLoader(String packageName) {
			List<Class> classes = new ArrayList<Class>();
			File fbpJarFile = null;
			JarFile jf = null;
			try {
				ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
				URL packageURL;
				ArrayList<String> names = new ArrayList<String>();
				String packageNameWithSeperator = packageName.replace(".", "/");
				packageURL = classLoader.getResource(packageNameWithSeperator);
				System.out.println("Package protocol : " + packageURL.getProtocol());
				if (packageURL.getProtocol().equals("jar")) {
					String jarFileName;

					Enumeration<JarEntry> jarEntries;
					String entryName;
					jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
					jarFileName = jarFileName.substring(5, jarFileName.indexOf("!"));
					jf = new JarFile(jarFileName);
					jarEntries = jf.entries();
					JarEntry flowJar;
					while (jarEntries.hasMoreElements()) {
						flowJar = jarEntries.nextElement();
						if ((flowJar.getName().endsWith(".class"))) {
							String className = flowJar.getName().replaceAll("/", "\\.");
							String someFbpClass = className.substring(0, className.lastIndexOf('.'));
							Class clas = Class.forName(someFbpClass);
							if (Component.class.isAssignableFrom(clas)) {
								classes.add(clas);
							}
						}
					}

				} else {
					Files.walk(Paths.get(packageURL.toURI())).filter(path -> {
						if (path.toFile().getAbsolutePath().endsWith(".class")) {
							return true;
						} else
							return false;
					}).forEach(path -> {
						String className = path.toFile().getAbsolutePath().replace(File.separator, ".");
						String someFbpClassWithPacageName = className.substring(className.indexOf(packageName));
						someFbpClassWithPacageName = someFbpClassWithPacageName.substring(0,
								someFbpClassWithPacageName.lastIndexOf('.'));
						Class clas = null;
						try {
							clas = Class.forName(someFbpClassWithPacageName);
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
						if (clas != null && Component.class.isAssignableFrom(clas)) {
							classes.add(clas);
						}
					});
				}
			} catch (IOException | ClassNotFoundException | URISyntaxException e) {
				e.printStackTrace();
			} finally {
				try {
					if (jf != null)
						jf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			return classes;
		}

		private JSONArray buildComponentListFromClasses(Class<?>[] classes) {
			List<JSONObject> jsonComponents = new ArrayList<JSONObject>();
			JSONArray components = new JSONArray();
			for (Class clazz : classes) {
				mComponents.put(clazz.getSimpleName(), clazz);
				try {
					jsonComponents.add(getComponentInfoJson(clazz));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			jsonComponents.sort(new Comparator<JSONObject>() {

				@Override
				public int compare(JSONObject o1, JSONObject o2) {
					try {
						return ((String) o1.get("name")).compareTo((String) o2.get("name"));
					} catch (JSONException e) {
						e.printStackTrace();
					}
					return 0;
				}

			});
			for (JSONObject jsonObject : jsonComponents) {
				components.put(jsonObject);
			}
			return components;
		}

		private JSONArray buildComponentListFromNodeJSModules(String nodeJsModulePath) {
			List<JSONObject> jsonComponents = new ArrayList<JSONObject>();
			JSONArray components = new JSONArray();
			File dir = new File(nodeJsModulePath);
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File f, String name) {
					// We want to find only .js files
					return name.endsWith(".js");
				}
			};
			try (JavetEnginePool<NodeRuntime> javetEnginePool = new JavetEnginePool<NodeRuntime>()) {
				javetEnginePool.getConfig().setJSRuntimeType(JSRuntimeType.Node);
				try (IJavetEngine<NodeRuntime> iJavetEngine = javetEnginePool.getEngine()) {
					V8ValueObject v8ValueObject = null;
					NodeRuntime nodeRuntime = iJavetEngine.getV8Runtime();
					nodeRuntime.resetContext();
					File[] files = dir.listFiles(filter);
					if (files == null || files.length == 0)
						return components;
					for (File file : files) {
						v8ValueObject = nodeRuntime.createV8ValueObject();
						nodeRuntime.getGlobalObject().set("console", v8ValueObject);
						nodeRuntime.getGlobalObject().set("nativePorts", v8ValueObject);
						v8ValueObject.bind(this);
						V8ValueObject obj = nodeRuntime.getExecutor(file).execute(true);
						V8ValueObject comp = nodeRuntime.getGlobalObject().invoke("getComponent");
						V8ValueString value = comp.invoke("getComponentMeta");
						try {
							jsonComponents.add(getNodeJsComponentInfo(value.getValue()));
						} catch (JSONException e) {
							e.printStackTrace();
						}
						value.close();
						comp.close();
						obj.close();
					}
					jsonComponents.sort(new Comparator<JSONObject>() {

						@Override
						public int compare(JSONObject o1, JSONObject o2) {
							try {
								return ((String) o1.get("name")).compareTo((String) o2.get("name"));
							} catch (JSONException e) {
								e.printStackTrace();
							}
							return 0;
						}

					});
					for (JSONObject jsonObject : jsonComponents) {
						mComponents.put(jsonObject.getString("name"), NodeJs.class);
						components.put(jsonObject);
					}
					return components;
				}
			} catch (JavetException e1) {
				System.out.println("Error while working with Javet EnginePool. " + e1.getMessage());
			}
			return components;

		}

		public Map<String, Class> getComponents() {
			return mComponents;
		}

		public Class getComponent(String componentName) {
			return mComponents.get(componentName);
		}

		private static List<InPort> getInports(Class comp) {
			ArrayList<InPort> ret = new ArrayList<InPort>();
			InPort p = (InPort) comp.getAnnotation(InPort.class);
			if (p != null) {
				ret.add(p);
			}
			InPorts ports = (InPorts) comp.getAnnotation(InPorts.class);
			if (ports != null) {
				for (InPort ip : ports.value()) {
					ret.add(ip);
				}
			}
			return ret;
		}

		private static List<OutPort> getOutports(Class comp) {
			ArrayList<OutPort> ret = new ArrayList<OutPort>();
			OutPort p = (OutPort) comp.getAnnotation(OutPort.class);
			if (p != null) {
				ret.add(p);
			}
			OutPorts ports = (OutPorts) comp.getAnnotation(OutPorts.class);
			if (ports != null) {
				for (OutPort op : ports.value()) {
					ret.add(op);
				}
			}
			return ret;
		}

		private static String getDescription(Class comp) {
			String description = "";
			ComponentDescription a = (ComponentDescription) comp.getAnnotation(ComponentDescription.class);
			if (a != null) {
				description = a.value();
			}
			return description;
		}

		private static String getIcon(Class comp) {
			String icon = "";
			ComponentDescription a = (ComponentDescription) comp.getAnnotation(ComponentDescription.class);
			if (a != null) {
				icon = a.icon();
			}
			return icon;
		}

		private static String getUISchema(Class comp) {
			String uiSchema = "{}";
			ComponentDescription a = (ComponentDescription) comp.getAnnotation(ComponentDescription.class);
			if (a != null) {
				uiSchema = a.uiSchema();
			}
			return uiSchema;
		}

		public JSONObject getNodeJsComponentInfo(String componentMeta) throws JSONException {
			JSONObject def = new JSONObject();
			ObjectMapper mapper = new ObjectMapper();
			NodeJsMeta nodeJsMeta = null;
			try {
				nodeJsMeta = mapper.readValue(componentMeta, NodeJsMeta.class);
			} catch (JsonProcessingException e) {
				System.out.println("Error during parsing of NodeJS Component.");
				return def;
			}
			// Top-level
			def.put("id", UUID.nameUUIDFromBytes(nodeJsMeta.getName().getBytes()));
			def.put("name", nodeJsMeta.getName());
			def.put("description", nodeJsMeta.getDescription());
			def.put("subgraph", false); // TODO: support subgraphs
			def.put("icon", nodeJsMeta.getIcon());
			def.put("uiSchema", nodeJsMeta.getSchema());
			// InPorts
			JSONArray inPorts = new JSONArray();
			for (Map.Entry<String, NodeJsMeta.InPort> entry : nodeJsMeta.getInPorts().entrySet()) {
				JSONObject portInfo = new JSONObject();
				portInfo.put("id", entry.getValue().getName());
				portInfo.put("type", entry.getValue().getDatatype());
				portInfo.put("description", entry.getValue().getDescription());
				portInfo.put("addressable", entry.getValue().isAddressable());
				portInfo.put("required", entry.getValue().isRequired());
				portInfo.put("isIIP", entry.getValue().isIIP());
				portInfo.put("uiSchema", entry.getValue().getSchema());
				inPorts.put(portInfo);
			}
			def.put("inPorts", inPorts);

			JSONArray outPorts = new JSONArray();
			for (Map.Entry<String, NodeJsMeta.OutPort> entry : nodeJsMeta.getOutPorts().entrySet()) {
				JSONObject portInfo = new JSONObject();
				portInfo.put("id", entry.getValue().getName());
				portInfo.put("type", entry.getValue().getDatatype());
				portInfo.put("description", entry.getValue().getDescription());
				portInfo.put("addressable", entry.getValue().isAddressable());
				portInfo.put("required", entry.getValue().isRequired());
				outPorts.put(portInfo);
			}
			def.put("outPorts", outPorts);

			return def;
		}

		public JSONObject getComponentInfoJson(Class<?> componentClass) throws JSONException {
			// Top-level
			JSONObject def = new JSONObject();
			def.put("id", UUID.nameUUIDFromBytes(componentClass.getSimpleName().getBytes()));
			def.put("name", componentClass.getSimpleName());
			def.put("description", getDescription(componentClass));
			def.put("subgraph", false); // TODO: support subgraphs
			def.put("icon", getIcon(componentClass));
			def.put("uiSchema", getUISchema(componentClass));
			// InPorts
			JSONArray inPorts = new JSONArray();
			for (InPort port : ComponentLibrary.getInports(componentClass)) {
				JSONObject portInfo = new JSONObject();
				portInfo.put("id", port.value());
				portInfo.put("type", ComponentLibrary.mapPortType(port.type()));
				portInfo.put("description", port.description());
				portInfo.put("addressable", port.arrayPort());
				portInfo.put("required", !port.optional());
				if (port.uiSchema().endsWith("json")) {
					ClassLoader classLoader = getClass().getClassLoader();
					URL resource = classLoader.getResource("uischema/" + port.uiSchema());
					if (resource == null) {
						throw new IllegalArgumentException("file not found! " + port.uiSchema());
					} else {

						// failed if files have whitespaces or special characters
						// return new File(resource.getFile());

						try {
							Path path = null;
							String jsonData = null;
							if (resource.getProtocol().equals("jar")) {
								InputStream isJSON = resource.openStream();
								BufferedReader reader = new BufferedReader(new InputStreamReader(isJSON));
								String strCurrentLine;
								StringBuffer jsonDataBuilder = new StringBuffer();
								while ((strCurrentLine = reader.readLine()) != null) {
									jsonDataBuilder.append(strCurrentLine);
								}
								jsonData = jsonDataBuilder.toString();
							} else {
								path = Paths.get(new File(resource.toURI()).getAbsolutePath());
								jsonData = Files.readString(path);
							}
							portInfo.put("uiSchema", jsonData);
						} catch (IOException | URISyntaxException e) {
							System.out.println(
									"Error" + e.getMessage() + " happened while reading file " + port.uiSchema());
						}
					}

				} else {
					portInfo.put("uiSchema", port.uiSchema());
				}
				if (port.uiValidate().endsWith("json")) {
					ClassLoader classLoader = getClass().getClassLoader();
					URL resource = classLoader.getResource(port.uiSchema());
					if (resource == null) {
						throw new IllegalArgumentException("file not found! " + port.uiSchema());
					} else {

						// failed if files have whitespaces or special characters
						// return new File(resource.getFile());

						try {
							Path path = Paths.get(new File(resource.toURI()).getAbsolutePath());
							String jsonData = Files.readString(path);
							portInfo.put("uiValidate", jsonData);
						} catch (IOException | URISyntaxException e) {
							System.out.println(
									"Error " + e.getMessage() + " happened while reading file " + port.uiValidate());
						}
					}

				} else {
					portInfo.put("uiValidate", port.uiValidate());
				}
				portInfo.put("isIIP", port.isIIP());
				inPorts.put(portInfo);
			}
			def.put("inPorts", inPorts);

			// OutPorts
			JSONArray outPorts = new JSONArray();
			for (OutPort port : ComponentLibrary.getOutports(componentClass)) {
				JSONObject portInfo = new JSONObject();
				portInfo.put("id", port.value());
				portInfo.put("type", ComponentLibrary.mapPortType(port.type()));
				portInfo.put("description", port.description());
				portInfo.put("addressable", port.arrayPort());
				portInfo.put("required", !port.optional());
				outPorts.put(portInfo);
			}
			def.put("outPorts", outPorts);

			return def;
		}

		// Return a FBP type string for a
		static String mapPortType(Class javaType) {
			if (javaType == String.class) {
				return "string";
			} else if (javaType == Boolean.class) {
				return "boolean";
			} else if (javaType == java.util.Hashtable.class) {
				return "object";
			} else if (javaType == Object.class) {
				return "object";
			} else if (javaType == Integer.class) {
				return "number";
			} else {
				// Default
				return "any";
			}
		}

	}

	public static class Definition {

		public static class Connection {
			public Connection() {
			}

			public String srcNode;
			public String srcPort;
			public String tgtNode;
			public String tgtPort;
		}

		public static class IIP {
			public String tgtNode;
			public String tgtPort;
			public Object data;
		}

		public Map<String, String> nodes; // id -> className
		public List<Connection> connections;
		public List<IIP> iips;

		public Definition() {
			nodes = new HashMap();
			connections = new ArrayList<Connection>();
			iips = new ArrayList<IIP>();
		}

		public void loadFromJson(String json) throws JSONException {
			JSONTokener tokener = new JSONTokener(json);
			JSONObject root = new JSONObject(tokener);

			// Nodes
			JSONObject processes = root.getJSONObject("processes");
			Iterable<String> nodeNames = new Util.JSONObjectKeysIterable(processes);
			for (String name : nodeNames) {
				JSONObject node = processes.getJSONObject(name);
				addNode(name, node.getString("component"));
			}

			// Connections
			JSONArray connections = root.getJSONArray("connections");
			for (int i = 0; i < connections.length(); i++) {
				JSONObject conn = connections.getJSONObject(i);
				JSONObject src = conn.optJSONObject("src");
				JSONObject tgt = conn.getJSONObject("tgt");
				if (src == null) {
					try {
						addInitial(tgt.getString("process"), tgt.getString("port"), conn.getString("data"));
					} catch (JSONException e) {
						// Try fetching the data as JSON Object
						addInitial(tgt.getString("process"), tgt.getString("port"), conn.getJSONObject("data"));
					}
				} else {
					addEdge(src.getString("process"), src.getString("port"), tgt.getString("process"),
							tgt.getString("port"));
				}
			}

		}

		public void addNode(String id, String component) {
			this.nodes.put(id, component);
		}

		public void removeNode(String id) {
			this.nodes.remove(id);
		}

		public void addEdge(final String src, final String _srcPort, final String tgt, final String _tgtPort) {
			this.connections.add(new Definition.Connection() {
				{
					srcNode = src;
					srcPort = _srcPort;
					tgtNode = tgt;
					tgtPort = _tgtPort;
				}
			});
		}

		public void removeEdge(final String src, final String srcPort, final String tgt, final String tgtPort) {
			// PERFORMANCE: linear complexity
			this.connections = Util.filter(this.connections, new Util.Predicate<Connection>() {
				@Override
				public boolean apply(Connection in) {
					boolean equals = in.srcNode == src && in.srcPort == srcPort && in.tgtNode == tgt
							&& in.tgtPort == tgtPort;
					return !equals;
				}
			});
		}

		public void addInitial(final String tgt, final String _tgtPort, final Object _data) {
			this.iips.add(new Definition.IIP() {
				{
					tgtNode = tgt;
					tgtPort = _tgtPort;
					data = _data;
				}
			});
		}

		public void removeInitial(final String tgt, final String tgtPort) {
			// PERFORMANCE: linear complexity
			this.iips = Util.filter(this.iips, new Util.Predicate<IIP>() {
				@Override
				public boolean apply(IIP in) {
					boolean equals = in.tgtNode == tgt && in.tgtPort == tgtPort;
					return !equals;
				}
			});
		}

	}

	public static class RuntimeNetwork extends Network {

		static final String copyright = "";
		private Definition mDefinition;
		private ComponentLibrary mLibrary;

		public RuntimeNetwork(ComponentLibrary lib, Definition def) {
			mLibrary = lib;
			mDefinition = def;
		}

		@Override
		public void define() {
			final boolean debug = true;

			// Add nodes
			for (Map.Entry<String, String> entry : mDefinition.nodes.entrySet()) {
				System.out.println(String.format("%s(%s)", entry.getKey(), entry.getValue()));
				Class cls = mLibrary.getComponent(entry.getValue());
				if (cls.equals(NodeJs.class))
					component(entry.getKey() + "-" + entry.getValue(), cls);
				else
					component(entry.getKey(), cls);
			}

			// Connect
			for (Definition.Connection conn : mDefinition.connections) {
				if (mLibrary.getComponent(mDefinition.nodes.get(conn.srcNode)).equals(NodeJs.class)) {
					conn.srcNode = conn.srcNode + "-" + mDefinition.nodes.get(conn.srcNode);
				}
				if (mLibrary.getComponent(mDefinition.nodes.get(conn.tgtNode)).equals(NodeJs.class)) {
					conn.tgtNode = conn.tgtNode + "-" + mDefinition.nodes.get(conn.tgtNode);
				}
				final String srcPort = conn.srcPort.toUpperCase();
				final String tgtPort = conn.tgtPort.toUpperCase();
				System.out.println(String.format("%s %s -> %s %s", conn.srcNode, srcPort, tgtPort, conn.tgtNode));
				connect(component(conn.srcNode), port(srcPort), component(conn.tgtNode), port(tgtPort));
			}

			// Add IIPs
			for (Definition.IIP iip : mDefinition.iips) {
				final String tgtPort = iip.tgtPort.toUpperCase();
				if (mLibrary.getComponent(mDefinition.nodes.get(iip.tgtNode)).equals(NodeJs.class)) {
					iip.tgtNode = iip.tgtNode + "-" + mDefinition.nodes.get(iip.tgtNode);
				}
				System.out.println(String.format("'%s' -> %s %s", iip.data.toString(), tgtPort, iip.tgtNode));
				initialize(iip.data, component(iip.tgtNode), port(tgtPort));
			}

		}

		static public void startNetwork(ComponentLibrary lib, Definition def) throws Exception {
			FlowRuntime.RuntimeNetwork net = new FlowRuntime.RuntimeNetwork(lib, def);
			net.go();
		}

	}

	public static class FlowhubApi {
		private String endpoint;

		public static FlowhubApi create() {
			return new FlowhubApi("http://api.flowhub.io"); // TODO: support HTTPS
		}

		FlowhubApi(String e) {
			endpoint = e;
		}

		// Returns null on failure
		public String registerRuntime(final String runtimeId, final String userId, final String label,
				final String address) throws Exception {
			JSONObject payload = new JSONObject() {
				{
					put("id", runtimeId);
					put("user", userId);
					put("label", label);
					put("address", address);
					put("protocol", "websocket");
					put("type", "javafbp");
					put("secret", "9129923"); // TEMP: currently not used
				}
			};

			int response = makeRequestSync("PUT", endpoint + "/runtimes/" + runtimeId, payload);
			if (response == 201 || response == 200) {
				return runtimeId;
			} else {
				return null;
			}
		}

		public String pingRuntime(final String runtimeId) throws Exception {
			int response = makeRequestSync("POST", endpoint + "/runtimes/" + runtimeId, null);
			if (response == 201 || response == 200) {
				return runtimeId;
			} else {
				return null;
			}
		}

		private int makeRequestSync(String method, String url, JSONObject payload) throws Exception {
			URL obj = new URL(url);

			java.net.HttpURLConnection con = (java.net.HttpURLConnection) obj.openConnection();
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestMethod(method);
			con.setDoInput(true);
			con.setDoOutput(true);
			con.connect();
			java.io.DataOutputStream wr = new java.io.DataOutputStream(con.getOutputStream());

			if (payload != null) {
				wr.writeBytes(payload.toString());
			}
			wr.flush();
			wr.close();

			final int responseCode = con.getResponseCode();

			System.out.println(method + " " + url);
			if (payload != null) {
				System.out.println(payload.toString());
			}
			System.out.println("Response Code : " + responseCode);

			java.io.InputStream s = (responseCode > 400) ? con.getErrorStream() : con.getInputStream();
			java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(s));
			StringBuffer response = new StringBuffer();
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			System.out.println(response.toString());

			return responseCode;
		}

	}
}