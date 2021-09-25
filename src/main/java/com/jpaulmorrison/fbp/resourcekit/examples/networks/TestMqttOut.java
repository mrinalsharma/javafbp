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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.jpaulmorrison.fbp.core.components.misc.GenerateTestData;
import com.jpaulmorrison.fbp.core.components.misc.JavaScriptFunction;
import com.jpaulmorrison.fbp.core.components.mqtt.MqttIn;
import com.jpaulmorrison.fbp.core.components.mqtt.MqttOut;
import com.jpaulmorrison.fbp.core.components.routing.Output;
import com.jpaulmorrison.fbp.core.engine.Network;

/**
 * Read file; write to other file
 * 
 */
public class TestMqttOut extends Network {

	@Override
	protected void define() {
		Path fileName = Path.of("src/main/resources/testdata/MqttOut.json");
		String iip;
		try {
			iip = Files.readString(fileName);
			connect(component("Generate", GenerateTestData.class), port("OUT"),component("MqttOut", MqttOut.class), port("IN"));
			initialize("100", component("Generate"), port("COUNT"));
			initialize(iip, component("MqttOut"), port("OPTIONS"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(final String[] argv) throws Throwable {
		new TestMqttOut().go();
	}
}
