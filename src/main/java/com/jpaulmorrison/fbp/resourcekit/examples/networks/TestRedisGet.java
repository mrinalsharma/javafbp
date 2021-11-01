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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpaulmorrison.fbp.core.components.misc.GenerateCustomTestData;
import com.jpaulmorrison.fbp.core.components.nodejs.NodeJs;
import com.jpaulmorrison.fbp.core.components.routing.Output;
import com.jpaulmorrison.fbp.core.engine.Network;

/**
 * Read file; write to other file
 * 
 */
public class TestRedisGet extends Network {
	private final String url = "{\n"
			+ "  \"host\": \"localhost\",\n"
			+ "  \"port\": 6379,\n"
			+ "  \"username\": \"\",\n"
			+ "  \"password\": \"noneed\",\n"
			+ "  \"connectWithUrl\": true,\n"
			+ "  \"URL\": \"redis://localhost:6379\",\n"
			+ "  \"ConnectWithUrl\": true,\n"
			+ " \"expiresIn\": 60000 \n"
			+ "}";
	private final String data = "{\n"
			+ "\"key\":\"123\"\n"
			+ "}";

	@Override
	protected void define() {
		component("Generate", GenerateCustomTestData.class);
		component("1234-RedisGet", NodeJs.class);
		connect(component("Generate"), port("OUT"), component("1234-RedisGet"), port("IN"));
		connect(component("1234-RedisGet"), port("OUT"), component("Output", Output.class), port("IN"));
		ObjectMapper mapper = new ObjectMapper();
		initialize(data, component("Generate"), port("DATA"));
		initialize(url, component("1234-RedisGet"), port("OPTIONS"));
	}

	public static void main(final String[] argv) throws Throwable {
		TestRedisGet redisNetwork = new TestRedisGet();
		Thread runNetwork = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					redisNetwork.go();
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
