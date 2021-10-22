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
 * You should have d a copy of the GNU Library General Public
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@InPorts({ @InPort(value = "OPTIONS", type = Object.class, isIIP = true, uiSchema = "Mqtt.json"),
		@InPort(value = "IN", description = "Incoming Packets") })
public class MqttOut extends Component {

	private InputPort optionsPort;
	private InputPort inputPort;

	@Override
	protected void execute() {
		MqttClient client = null;
		try {
			CountDownLatch latch = new CountDownLatch(1);
			Packet<?> optp = optionsPort.receive();
			ObjectMapper mapper = new ObjectMapper();
			MqttOptions options = mapper.readValue((String) optp.getContent(), MqttOptions.class);
			String broker = "tcp://" + options.getServer() + ":" + options.getPort();
			String clientId = options.getClientId();
			drop(optp);
			client = new MqttClient(broker, clientId, null);
			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(options.getUseCleanSession());
			connOpts.setUserName(options.getUserName());
			connOpts.setPassword(options.getPassword().toCharArray());
			connOpts.setKeepAliveInterval(options.getKeepAlive());
			System.out.println("Connecting to broker: " + broker);
			client.setCallback(new MqttCallback() {

				@Override
				public void messageArrived(String topic, MqttMessage message) throws Exception {

				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken token) {
					System.out.println("Message Delivery completed.");
				}

				@Override
				public void connectionLost(Throwable cause) {
					System.out.println("MQTT connection lost.");
					latch.countDown();

				}
			});
			client.connect(connOpts);
			System.out.println("Connected");
			InputPort[] ports = new Connection[1];
			ports[0] = inputPort;
			while (true) {
				if (Thread.interrupted() || latch.getCount() == 0) {
					client.disconnect();
					System.out.println("Disconnected");
					break;
				}
				if (findInputPortElementWithData(ports) == 0) {
					Packet<?> p = inputPort.receive();
					System.out.println("Publishing message: " + p);
					MqttMessage message = new MqttMessage(p.getContent().toString().getBytes());
					message.setQos(options.getQos());
					client.publish(options.getTopic(), message);
					drop(p);
				} else {
					Thread.sleep(1000);
				}
			}
		} catch (MqttException | InterruptedException | JsonProcessingException me) {
			try {
				client.disconnect();
			} catch (MqttException e) {
				System.out.println("Error during MQTT Disconnect" + e.getMessage());
			}
			System.out.println("Disconnected");
			System.out.println("MQTT error" + me.getMessage());
			System.out.println("MQTT Error cause " + me.getCause());
		}

	}

	@Override
	protected void openPorts() {
		optionsPort = openInput("OPTIONS");
		inputPort = openInput("IN");

	}

}