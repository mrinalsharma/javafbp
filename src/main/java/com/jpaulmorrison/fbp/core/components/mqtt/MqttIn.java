/*
 * JavaFBP - A Java Implementation of Flow-Based Programming (FBP)
 * Copyright (C) 2009, 2016 J. Paul Morrison
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, see the GNU Library General Public License v3
 * at https://www.gnu.org/licenses/lgpl-3.0.en.html for more details.
 */

package com.jpaulmorrison.fbp.core.components.mqtt;

import java.util.concurrent.CountDownLatch;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.jpaulmorrison.fbp.core.engine.*;

/**
 * Component to collate two or more streams of packets, based on a list of
 * control field lengths held in the CTLFIELDS IIP
 * 
 * Control fields in incoming IPs are assumed to be contiguous, starting at byte
 * 0
 * 
 * Input streams are assumed to be sorted on the same control fields, in
 * ascending order
 */
@ComponentDescription("Read data from MQTT compliant message broker.")
@OutPort("OUT")
@InPorts({ @InPort(value = "OPTIONS", type = Object.class, isIIP = true, uiSchema = "Mqtt.json") })
public class MqttIn extends Component {

	private InputPort optionsPort;

	private OutputPort outport;

	@Override
	protected void execute() {
		CountDownLatch latch = new CountDownLatch(1);
		Gson gson = new Gson();
		Packet<?> optp = optionsPort.receive();
		MqttOptions options = gson.fromJson((String) optp.getContent(), MqttOptions.class);
		String broker = "tcp://" + options.getServer() + ":" + options.getPort();
		String clientId = options.getClientId();
		drop(optp);
		try {
			MqttClient client = new MqttClient(broker, clientId, null);
			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(options.getUseCleanSession());
			connOpts.setUserName(options.getUserName());
			connOpts.setPassword(options.getPassword().toCharArray());
			connOpts.setKeepAliveInterval(options.getKeepAlive());
			System.out.println("Connecting to broker: " + broker);
			client.setCallback(new MqttCallback() {

				@Override
				public void messageArrived(String topic, MqttMessage message) throws Exception {
					System.out.println("Message has Arrived " + message.getPayload().toString());
					outport.send(create(message.getPayload()));
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken token) {

				}

				@Override
				public void connectionLost(Throwable cause) {
					System.out.println("MQTT connection lost.");
					latch.countDown();

				}
			});
			client.connect(connOpts);
			System.out.println("Connected");
			client.subscribe(options.getTopic(), options.getQos());
			while (true) {
				if (Thread.interrupted() || latch.getCount() == 0) {
					client.disconnect();
					System.out.println("Disconnected");
					break;
				}
			}
			// System.out.println("Publishing message: " + content);
			/*
			 * MqttMessage message = new MqttMessage(content.getBytes());
			 * message.setQos(qos); sampleClient.publish(topic, message);
			 */
		} catch (MqttException me) {
			System.out.println("reason " + me.getReasonCode());
			System.out.println("msg " + me.getMessage());
			System.out.println("cause " + me.getCause());
		}

	}

	@Override
	protected void openPorts() {
		optionsPort = openInput("OPTIONS");
		outport = openOutput("OUT");

	}

}