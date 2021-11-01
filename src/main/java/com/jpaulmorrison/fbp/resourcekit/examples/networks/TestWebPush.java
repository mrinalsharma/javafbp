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

package com.jpaulmorrison.fbp.resourcekit.examples.networks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpaulmorrison.fbp.core.components.misc.GenerateCustomTestData;
import com.jpaulmorrison.fbp.core.components.nodejs.NodeJs;
import com.jpaulmorrison.fbp.core.engine.Network;

/**
 * Read file; write to other file
 * 
 */
public class TestWebPush extends Network {
	private final String subscription  = "{\n"
			+ "\"sub\":{\n"
			+ "    \"endpoint\": \"https://fcm.googleapis.com/fcm/send/eYAaLckNNSo:APA91bF68lSHpvCWrf7vG1Qo-W-qIOt8P6VqXDy3M3S0-oHxqa3mV_sTkK_AwM6OwSVRks4DIPXLfu-DXeAi95hiCVB4h5cF1JC7dW3NdDa1El0kF0veoZ38K7wcY2HYRxPA7eageKd0\",\n"
			+ "    \"expirationTime\": null,\n"
			+ "    \"keys\": {\n"
			+ "        \"p256dh\": \"BIgUAE_GE9TobV9EiIQSSSuqQDP-mkAIuKPlM30eugZf21gQYU6zEpM3wpl7Qc_BkA7Wu6U3M1xPdpIDiVCEX9o\",\n"
			+ "        \"auth\": \"cLVdVbstLd7qDUkmjf-EKA\"\n"
			+ "    }\n"
			+ "},\n"
			+ "\"msg\":\"Hello from Flow\"\n"
			+ "}";
	private final String vapidKeys = "{\n"
			+ "  \"vapiPublicKey\": \"BEl62iUYgUivxIkv69yViEuiBIa-Ib9-SkvMeAtA3LFgDzkrxZJjSgSnfckjBJuBkr3qBUYIHBQFLXYp5Nksh8U\",\n"
			+ "  \"vapiPrivateKey\": \"UUxI4O8-FbRouAevSmBQ6o18hgE4nSG3qwvJTfKc-ls\",\n"
			+ "  \"firebaseApiKey\": \"AIzaSyAq_xyQyvysD-dqO64teCLMa29q_rszA6s\"\n"
			+ "}";
	@Override
	protected void define() {
		component("Generate", GenerateCustomTestData.class);
		component("1234-GoogleWebPush", NodeJs.class);
		connect(component("Generate"), port("OUT"), component("1234-GoogleWebPush"), port("IN"));
		ObjectMapper mapper = new ObjectMapper();
		initialize(subscription, component("Generate"), port("DATA"));
		initialize(vapidKeys,component("1234-GoogleWebPush"), port("OPTIONS"));
		/*
		 * initialize( System.getProperty("user.dir") + File.separator +
		 * "src/main/resources/testdata/testdata.txt".replace("/", File.separator),
		 * component("Read"), port("SOURCE"));
		 */
		// initialize("function execute(param) {return param + \"hello\";}",
		// component("JavaScriptV8Runtime"), port("CODE"));
	}

	public static void main(final String[] argv) throws Throwable {
		TestWebPush webPush = new TestWebPush();
		Thread runNetwork = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					webPush.go();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		});
		Thread exitThread = new Thread(new Runnable() {

			@Override
			public void run() {
				for (int time = 0; time < 100; time++) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				runNetwork.interrupt();
			}
		});

		runNetwork.start();
		exitThread.start();
		runNetwork.join();
		exitThread.join();

	}
}
