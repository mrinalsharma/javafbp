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

import com.jpaulmorrison.fbp.core.components.routing.Aggregator;
import com.jpaulmorrison.fbp.core.components.routing.ConcatStreams;
import com.jpaulmorrison.fbp.core.components.routing.Discard;
import com.jpaulmorrison.fbp.core.components.routing.Output;
import com.jpaulmorrison.fbp.core.components.routing.Passthru;
import com.jpaulmorrison.fbp.core.components.routing.Splitter;
import com.jpaulmorrison.fbp.core.components.routing.Splitter1;
import com.jpaulmorrison.fbp.core.engine.Network;

import java.io.File;

import com.jpaulmorrison.fbp.core.components.io.ReadFile;
import com.jpaulmorrison.fbp.core.components.misc.GenerateCustomTestData;
import com.jpaulmorrison.fbp.core.components.misc.GenerateTestData;
import com.jpaulmorrison.fbp.core.components.nodejs.NodeJs;

/**
 * This network is similar to the one called Deadlock, but you will notice that
 * the port numbers line up!
 * 
 * The additional Passthru's don't make any difference!
 * 
 */

public class TestAggregator {
	
	public static class TestStreamAggregator extends Network {
		private String stringSplitter = "{\n"
				+ "  \"splitUsing\": \"String\",\n"
				+ "  \"stream\": false,\n"
				+ "  \"pattern\": \"and\"\n"
				+ "}";

		@Override
		protected void define() {
			// component("MONITOR", Monitor.class);
			// tracing = true;

			component("Generate", GenerateCustomTestData.class);
			component("Splitter", Splitter.class);
			component("Aggregator", Aggregator.class);
			component("Output", Output.class);
			component("TEST_Demo", NodeJs.class);
			connect("Generate.OUT", "Splitter.IN");
			connect("Splitter.OUT", "Aggregator.IN");
			connect("Aggregator.OUT", "TEST_Demo.IN");
			connect("TEST_Demo.OUT", "Output.IN");
			initialize(stringSplitter, component("Splitter"), port("OPTIONS"));
			initialize("hello and How are you and Thank you", component("Generate"), port("DATA"));
			initialize("abcde",	component("TEST_Demo"), port("OPTIONS_NEW"));
		}
	}
		
	public static class TestJSONSplitter extends Network {
		private String stringJSONSplitterData = "{\n"
				+ "        \"cartPhase\": \"ORDER_COMPLETE\",\n"
				+ "        \"currency\": \"USD\",\n"
				+ "        \"subtotal\": 35.98,\n"
				+ "        \"discountAmount\": 0,\n"
				+ "        \"taxAmount\": 0,\n"
				+ "        \"grandTotal\": 35.98,\n"
				+ "        \"orderId\": \"123ABC\",\n"
				+ "        \"emailAddress\": \"example@example.com\",\n"
				+ "        \"cartUrl\": \"http://brontogear.com/\",\n"
				+ "        \"shippingAmount\": 7.99,\n"
				+ "        \"shippingDate\": \"05 - 18 - 2018\",\n"
				+ "        \"shippingDetails\": \"FedEx\",\n"
				+ "        \"shippingTrackingUrl\": \"http://fedex.com/tracking/NIeX3KYLcPhgRzKy\",\n"
				+ "        \"lineItems\": [{\n"
				+ "                \"sku\": \"576879\",\n"
				+ "                \"name\": \"Shirt\",\n"
				+ "                \"description\": \"A super great description of the product\",\n"
				+ "                \"category\": \"Shirts > T-Shirts > Blue\",\n"
				+ "                \"other\": \"This can be any string value you like\",\n"
				+ "                \"unitPrice\": 11.99,\n"
				+ "                \"salePrice\": 11.99,\n"
				+ "                \"quantity\": 2,\n"
				+ "                \"totalPrice\": 23.98,\n"
				+ "                \"imageUrl\": \"http://brontogear.com/a/p/shirt.jpeg\",\n"
				+ "                \"productUrl\": \"http://brontogear.com/index.php/shirt.html\"\n"
				+ "              },\n"
				+ "             {\n"
				+ "                 \"sku\": \"1112296\",\n"
				+ "                 \"name\": \"Fleece Jacket\",\n"
				+ "                 \"description\": \"A well appointed Fleece jacket\",\n"
				+ "                 \"category\": \"Jackets > Winter > Fleece\",\n"
				+ "                 \"other\": \"This can be any string value you like\",\n"
				+ "                 \"unitPrice\": 65.99,\n"
				+ "                 \"salePrice\": 55.50,\n"
				+ "                 \"quantity\": 1,\n"
				+ "                 \"totalPrice\": 55.50,\n"
				+ "                 \"imageUrl\": \"http://brontogear.com/a/p/fleece.jpeg\",\n"
				+ "                 \"productUrl\": \"http://brontogear.com/index.php/fleece.html\"     \n"
				+ "            }\n"
				+ "        ]\n"
				+ "    }";
		private String stringJSONSplitter = "{\n"
				+ "  \"splitUsing\": \"JSON\""
				+ "}";

		@Override
		protected void define() {
			// component("MONITOR", Monitor.class);
			// tracing = true;

			component("Generate", GenerateCustomTestData.class);
			component("Splitter", Splitter.class);
			component("Aggregator", Aggregator.class);
			component("Output", Output.class);
			component("TEST_Demo", NodeJs.class);
			connect("Generate.OUT", "Splitter.IN");
			connect("Splitter.OUT", "Aggregator.IN");
			connect("Aggregator.OUT", "TEST_Demo.IN");
			connect("TEST_Demo.OUT", "Output.IN");
			initialize(stringJSONSplitterData, component("Generate"), port("DATA"));
			initialize(stringJSONSplitter, component("Splitter"), port("OPTIONS"));
		}
	}

	public static void main(final String[] argv) throws Exception {
		new TestStreamAggregator().go();
		//new TestJSONSplitter().go();
	}
}
