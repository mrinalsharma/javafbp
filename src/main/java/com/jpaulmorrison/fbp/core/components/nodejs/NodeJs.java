package com.jpaulmorrison.fbp.core.components.nodejs;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import com.caoccao.javet.annotations.V8Function;
import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.NodeRuntime;
import com.caoccao.javet.interop.engine.IJavetEngine;
import com.caoccao.javet.interop.engine.JavetEnginePool;
import com.caoccao.javet.values.primitive.V8ValueString;
import com.caoccao.javet.values.reference.V8ValueObject;
import com.caoccao.javet.values.reference.V8ValuePromise;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpaulmorrison.fbp.core.engine.Component;
import com.jpaulmorrison.fbp.core.engine.ComponentDescription;
import com.jpaulmorrison.fbp.core.engine.InPort;
import com.jpaulmorrison.fbp.core.engine.InPorts;
import com.jpaulmorrison.fbp.core.engine.InputPort;
import com.jpaulmorrison.fbp.core.engine.NodeJsMeta;
import com.jpaulmorrison.fbp.core.engine.OutPort;
import com.jpaulmorrison.fbp.core.engine.OutPorts;
import com.jpaulmorrison.fbp.core.engine.OutputPort;
import com.jpaulmorrison.fbp.core.engine.Packet;

@ComponentDescription("NodeJS Engine")

@InPorts({ @InPort(value = "IN", description = "Incoming Packets") })
@OutPorts({ @OutPort(value = "OUT", description = "Outgoing Packets", optional = true) })
public class NodeJs extends Component {
	private V8ValueObject comp = null;
	private V8ValueObject v8ValueObject = null;
	private NodeRuntime nodeRuntime;
	private JavetEnginePool<NodeRuntime> javetEnginePool = null;
	private IJavetEngine<NodeRuntime> iJavetEngine = null;
	private OutputPort outport;
	private NodeJsMeta nodeJsMeta = null;
	private Map<String, String> iipData = new HashMap();
	private NodeJSEventLoopThread nodeJSEventLoopThread = null;
	private ProcessIncomingData processIncomingData = null;
	private CountDownLatch start = new CountDownLatch(1);  
	static String EXAMPLE_NODE_SCRIPT = "var http = require('http');\n" + ""
			+ "var server = http.createServer(function (request, response) {\n"
			+ " response.writeHead(200, {'Content-Type': 'text/plain'});\n" + " response.end(someJavaMethod());\n"
			+ "});\n" + "" + "server.listen(8000);\n" + "var hello = 'hello, How are you';"
			+ "console.log('Server running at https://127.0.0.1:8000/');" + "function handleIP(port,payload) {\r\n"
			+ "  port.send(port1,data);;   // The function returns the product of p1 and p2\r\n" + "}"
			+ "exports.handleIP = handleIP;";

	@V8Function(name = "send")
	public void send(String portName, String payload) {
		outport = openOutput(portName);
		outport.send(create(payload));
	}

	@V8Function(name = "log")
	public void log(final String... messages) {
		StringBuilder builder = new StringBuilder();
		for (String message : messages) {
			builder.append(message);
		}
		System.out.println("[INFO] " + builder.toString());
	}

	@V8Function(name = "error")
	public void error(final String... messages) {
		StringBuilder builder = new StringBuilder();
		for (String message : messages) {
			builder.append(message);
		}
		System.out.println("[ERROR] " + builder.toString());
	}

	@Override
	protected void execute() throws Exception {
		nodeJSEventLoopThread = new NodeJSEventLoopThread(inputPorts);
		nodeJSEventLoopThread.start();
		start.await();
		processIncomingData = new ProcessIncomingData(inputPorts);
		processIncomingData.start();
		waitForEnd_ProcessIncomingData_Thread();
	}

	private void waitForEnd_ProcessIncomingData_Thread() {
		while (true) {
			if (Thread.interrupted() || !processIncomingData.isAlive()) {
				processIncomingData.interrupt();
				nodeJSEventLoopThread.interrupt();
				break;
			}
		}
	}

	private static File createTemporaryScriptFile(final String script, final String name) throws IOException {
		File tempFile = File.createTempFile(name, ".js.tmp");
		PrintWriter writer = new PrintWriter(tempFile, "UTF-8");
		try {
			writer.print(script);
		} finally {
			writer.close();
		}
		return tempFile;
	}

	@Override
	protected void openPorts() {
		try {
			javetEnginePool = JaveteEnginePool.getJavetEnginePool();
			iJavetEngine = javetEnginePool.getEngine();
			nodeRuntime = iJavetEngine.getV8Runtime();
			v8ValueObject = nodeRuntime.createV8ValueObject();
			nodeRuntime.getGlobalObject().set("console", v8ValueObject);
			nodeRuntime.getGlobalObject().set("nativePorts", v8ValueObject);
			v8ValueObject.bind(this);
			String scriptDir = System.getenv("FLOW_NODE_DIR");
			nodeRuntime
					.getExecutor(new File(
							scriptDir + File.separator + getName().substring(getName().lastIndexOf("-") + 1) + ".js"))
					.execute(true);
			comp = nodeRuntime.getGlobalObject().invoke("getComponent");
			V8ValueString value = comp.invoke("getComponentMeta");
			String componentMeta = value.getValue();
			ObjectMapper mapper = new ObjectMapper();
			nodeJsMeta = mapper.readValue(componentMeta, NodeJsMeta.class);
			value.close();
		} catch (JavetException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

	}

	private class ProcessIncomingData extends Thread {
		HashMap<String, InputPort> inputPorts;

		public ProcessIncomingData(HashMap<String, InputPort> inputPorts) {
			super();
			this.inputPorts = inputPorts;
		}

		@Override
		public void run() {
			Map<String, InputPort> inputPorts = this.inputPorts.values().stream().filter(p -> {
				return nodeJsMeta.getInPorts().get(p.getName().substring(p.getName().lastIndexOf(".") + 1))
						.isIIP() == false;
			}).collect(Collectors.toMap(p -> p.getName().substring(p.getName().lastIndexOf(".") + 1), p -> p));
			InputPort ports[] = new InputPort[inputPorts.size()];
			while (true) {

				int portIndex;
				try {
					portIndex = findInputPortElementWithData(
							(new ArrayList<InputPort>(inputPorts.values())).toArray(ports));

					if (portIndex != -1) {
						InputPort port = ports[portIndex];
						String portDisplayName = port.getName();
						String portName = portDisplayName.substring(portDisplayName.lastIndexOf(".") + 1);
						Packet packet =  port.receive();
						comp.invoke("handleIP", portName, packet.getContent());
						drop(packet);
					}
					if (Thread.interrupted() || (!nodeJsMeta.isKeepRunning() && portIndex == -1)) {
						V8ValuePromise v8ValuePromise = comp.invoke("stop");
						while (v8ValuePromise.getState() == v8ValuePromise.STATE_PENDING) {
							Thread.sleep(1000);
						}
						comp.close();
						v8ValueObject.unbind(NodeJs.this);
						v8ValuePromise.close();
						v8ValueObject.close();
						nodeRuntime.resetContext();
						iJavetEngine.close();
						break;
					}
				} catch (InterruptedException | JavetException e1) {
					try {
						V8ValuePromise v8ValuePromise = comp.invoke("stop");
						while (v8ValuePromise.getState() == v8ValuePromise.STATE_PENDING) {
							System.out.println("Promise State: " + v8ValuePromise.getState());
						}
						comp.close();
						v8ValueObject.unbind(NodeJs.this);
						v8ValuePromise.close();
						v8ValueObject.close();
						nodeRuntime.resetContext();
						iJavetEngine.close();
					} catch (JavetException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}

	private class NodeJSEventLoopThread extends Thread {
		HashMap<String, InputPort> inputPorts;

		public NodeJSEventLoopThread(HashMap<String, InputPort> inputPorts) {
			super();
			this.inputPorts = inputPorts;
		}

		@Override
		public void run() {
			try {
				InputPort ports[] = new InputPort[this.inputPorts.size()];
				Map<String, String> iipPorts = this.inputPorts.values().stream().filter(p -> {
					return nodeJsMeta.getInPorts().get(p.getName().substring(p.getName().lastIndexOf(".") + 1))
							.isIIP() == true;
				}).collect(Collectors.toMap(p -> p.getName().substring(p.getName().lastIndexOf(".") + 1),
						p -> p.receive() == null ? "{}" : p.receive().getContent().toString()));
				if (iipPorts.size() > 0) {
					ObjectMapper mapper = new ObjectMapper();
					try {
						String jsonResult = mapper.writeValueAsString(iipPorts);
						comp.invoke("start", jsonResult);
						start.countDown();
						nodeRuntime.await();
					} catch (JsonProcessingException e) {
						e.printStackTrace();
						return;
					}
				} else {
					ObjectMapper mapper = new ObjectMapper();
					try {
						String jsonResult = mapper.writeValueAsString(new HashMap<String, String>());
						comp.invoke("start", jsonResult);
						start.countDown();
						nodeRuntime.await();
					} catch (JsonProcessingException e) {
						e.printStackTrace();
						return;
					}
				}
			} catch (JavetException e) {
				e.printStackTrace();
			}
			finally {
				start.countDown();
			}
		}
	}
}
