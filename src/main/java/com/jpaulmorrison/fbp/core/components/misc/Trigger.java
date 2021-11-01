package com.jpaulmorrison.fbp.core.components.misc;

import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

import com.jpaulmorrison.fbp.core.engine.Component;
import com.jpaulmorrison.fbp.core.engine.ComponentDescription;
import com.jpaulmorrison.fbp.core.engine.InPort;
import com.jpaulmorrison.fbp.core.engine.InPorts;
import com.jpaulmorrison.fbp.core.engine.InputPort;
import com.jpaulmorrison.fbp.core.engine.OutPort;
import com.jpaulmorrison.fbp.core.engine.OutputPort;
import com.jpaulmorrison.fbp.core.engine.Packet;

@ComponentDescription("Trigger an event")
@OutPort(value = "OUT", description = "Send 1 to the output port when timer is triggered.", type = Integer.class)
@InPorts({
		@InPort(value = "TIME", description = "Define at what time the time should fire.", type = Object.class, isIIP = true, uiSchema = "trigger.json") })
public class Trigger extends Component {

	private OutputPort outPort;

	private InputPort time;

	private TimerThread timerThread;

	@Override
	protected void execute() throws Exception {
		Packet rp = time.receive();
		if (rp == null) {
			return;
		}

		JSONObject timerOptions = new JSONObject((String) rp.getContent());;

		drop(rp);
		time.close();
		if (timerOptions != null) {
			timerThread = new TimerThread(timerOptions);
			timerThread.start();
		}

		while (true) {
			if (Thread.interrupted()) {
				timerThread.purgeTimer();
				timerThread.interrupt();
				break;
			}
		}

	}

	class TimerThread extends Thread {
		private JSONObject timerOptions;
		Timer timer;

		public TimerThread(JSONObject timerOptions) {
			super("TimerThread");
			this.timerOptions = timerOptions;
		}

		public void purgeTimer() {
			if (timer != null) {
				timer.purge();
				timer.cancel();
			}
		}

		@Override
		public void run() {
			JSONObject jsonRepeat = timerOptions;
			if (jsonRepeat != null) {
				long repeat = jsonRepeat.getLong("Repeat");
				TimerTask task = new TimerTask() {
					public void run() {
						try {
							outPort.send(create(1));
						} catch (Exception e) {
							System.out.println("Error while sending Data OUT");
						}
					}
				};
				timer = new Timer("Timer");
				timer.schedule(task, 0, repeat);

			}

		}

	}

	@Override
	protected void openPorts() {
		time = openInput("TIME");
		outPort = openOutput("OUT");

	}

}
