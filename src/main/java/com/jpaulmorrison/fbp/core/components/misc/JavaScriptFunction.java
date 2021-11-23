package com.jpaulmorrison.fbp.core.components.misc;

import org.json.JSONObject;

import com.eclipsesource.v8.V8;
import com.jpaulmorrison.fbp.core.engine.Component;
import com.jpaulmorrison.fbp.core.engine.ComponentDescription;
import com.jpaulmorrison.fbp.core.engine.InPort;
import com.jpaulmorrison.fbp.core.engine.InPorts;
import com.jpaulmorrison.fbp.core.engine.InputPort;
import com.jpaulmorrison.fbp.core.engine.OutPort;
import com.jpaulmorrison.fbp.core.engine.OutPorts;
import com.jpaulmorrison.fbp.core.engine.OutputPort;
import com.jpaulmorrison.fbp.core.engine.Packet;

@ComponentDescription("V8Runtime Engine")

@InPorts({
		@InPort(value = "CODE", description = "Write JavaScript code. Variable param would have the data passed by previous component. Name of the function should be Execute.", type = Object.class, isIIP = true, uiSchema = "JavaScriptFunction.json"),
		@InPort(value = "IN", description = "Incoming Packets") })
@OutPorts({ @OutPort(value = "OUT", description = "Outgoing Packets", optional = true) })
public class JavaScriptFunction extends Component {
	private InputPort inport;
	private OutputPort outport;
	private InputPort code;

	@Override
	protected void execute() throws Exception {
		Packet rp = code.receive();
		if (rp == null) {
			return;
		}
		JSONObject object = new JSONObject((String) rp.getContent());
		String body = object.getString("function");
		String function = createJavascriptFunction(body);
		drop(rp);
		Packet ip = inport.receive();
		V8 v8 = V8.createV8Runtime();
		v8.executeObjectScript(function);
		Object result = v8.executeJSFunction("execute", ip.getContent());
		drop(ip);
		outport.send(create(result.toString()));

	}

	private String createJavascriptFunction(String body) {
		String function = "function execute(payload)\r\n"
				+ "{\r\n"
				+ "try {\r\n"
				+ body
				+ "} catch (error) {\r\n"
				+ "  console.error(error);\r\n"
				+ "}\r\n"
				+ "}";
		return function;
	}

	@Override
	protected void openPorts() {
		inport = openInput("IN");
		outport = openOutput("OUT");
		code = openInput("CODE");
	}

}
