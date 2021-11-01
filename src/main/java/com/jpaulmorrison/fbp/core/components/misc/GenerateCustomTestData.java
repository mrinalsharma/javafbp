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

package com.jpaulmorrison.fbp.core.components.misc;

import com.jpaulmorrison.fbp.core.engine.Component;
import com.jpaulmorrison.fbp.core.engine.ComponentDescription;
import com.jpaulmorrison.fbp.core.engine.InPort;
import com.jpaulmorrison.fbp.core.engine.InPorts;
import com.jpaulmorrison.fbp.core.engine.InputPort;
import com.jpaulmorrison.fbp.core.engine.OutPort;
import com.jpaulmorrison.fbp.core.engine.OutPorts;
import com.jpaulmorrison.fbp.core.engine.OutputPort;
import com.jpaulmorrison.fbp.core.engine.Packet;

/**
 * Component to accept custom IIP and Data and pass it on to the next component
 * out to IIP_OUT and DATA_OUT ports.
 */
@ComponentDescription("Generates custom IIP_ and Data")
@OutPorts({ @OutPort(value = "OUT", description = "Generated Data", type = String.class) })
@InPorts({ @InPort(value = "DATA", description = "Custom data that need to be passsed to next component", type = String.class, isIIP = true) })
public class GenerateCustomTestData extends Component {

	private OutputPort dataOutPort;
	private InputPort data;

	@Override
	protected void execute() {
		Packet<?> ctp = data.receive();
		if (ctp == null) {
			return;
		}
		data.close();
		dataOutPort.send(create(ctp.getContent()));
		drop(ctp);

	}

	@Override
	protected void openPorts() {
		dataOutPort = openOutput("OUT");
		data = openInput("DATA");
	}
}
