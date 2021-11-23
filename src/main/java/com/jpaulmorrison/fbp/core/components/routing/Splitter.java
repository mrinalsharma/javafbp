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

package com.jpaulmorrison.fbp.core.components.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpaulmorrison.fbp.core.engine.Component;
import com.jpaulmorrison.fbp.core.engine.ComponentDescription;
import com.jpaulmorrison.fbp.core.engine.InPort;
import com.jpaulmorrison.fbp.core.engine.InPorts;
import com.jpaulmorrison.fbp.core.engine.InputPort;
import com.jpaulmorrison.fbp.core.engine.JavaIdGenerator;
import com.jpaulmorrison.fbp.core.engine.OutPort;
import com.jpaulmorrison.fbp.core.engine.OutputPort;
import com.jpaulmorrison.fbp.core.engine.Packet;
import com.jpaulmorrison.fbp.core.engine.PacketAttributeAccessor;

/**
 * Component to split an input stream or message into multiple output messages, based on length, string pattern or Iterating Splitters.
 */
@ComponentDescription("Splits a stream into multiple output messages based on Split strategy")
@OutPort(value = "OUT")
@InPorts({ @InPort("IN"), @InPort(value = "OPTIONS", type = Object.class, isIIP = true, uiSchema = "splitter.json") })
public class Splitter extends Component {

	private InputPort inport;
	private InputPort optionsPort;
	private OutputPort outport;

	private String data;

	@Override
	protected void execute() {
		ignorePacketCountError = true;
		Packet<?> iipPkt = optionsPort.receive();
		ObjectMapper mapper = new ObjectMapper();
		try {
			IIP iip = mapper.readValue((String) iipPkt.getContent(), IIP.class);

			Packet<?> strPacket = inport.receive();
			data = (String) strPacket.getContent();
			PacketAttributeAccessor packetAttributeAccessor =  new PacketAttributeAccessor();
			packetAttributeAccessor.copyAttributes(strPacket.getAttributes());
			Iterator<String> iterator = null;
			if (iip.getSplitUsing().equals(SplitUsing.Fixed_Length.getSplitUsing())) {
				iterator = new LengthMatcherSplitterIterator(data, iip.length);
			} else if (iip.getSplitUsing().equals(SplitUsing.STRING.getSplitUsing())) {
				iterator = new StringMatcherSplitterIterator(data, iip.pattern);
			}else if (iip.getSplitUsing().equals(SplitUsing.JSON.getSplitUsing())) {
				iterator = new JsonSplitterIterator(data);
			}
			int sequenceNumber = 0;
			if(packetAttributeAccessor.getCorrelationId() == null)
			{
				packetAttributeAccessor.setAttribute(PacketAttributeAccessor.CORRELATION_ID, new JavaIdGenerator().generateId().toString());
			}
			while (iterator.hasNext()) {
				String split = iterator.next();
				packetAttributeAccessor.setAttribute(PacketAttributeAccessor.SEQUENCE_NUMBER, String.valueOf(++sequenceNumber));
				outport.send(create(split,packetAttributeAccessor.toMap()));
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void openPorts() {

		inport = openInput("IN");
		optionsPort = openInput("OPTIONS");
		outport = openOutput("OUT");

	}

	abstract class SplitterIterator<E> implements SplitterIteratorStrategy<E> {

		private String data;

		public SplitterIterator(String data) {
			this.data = data;
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public E next() {
			return null;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	/*
	 * Split JSON Object tree into New JSON objects with all the primitive values of
	 * root in addition to JSON object in root.
	 */
	class JsonSplitterIterator extends SplitterIterator<String> {

		JSONObject root = null;
		Iterator<String> jsonObjectskeys;
		Map<String, Object> jsonComplexchilds = new HashMap();
		List<String> primitiveKeys = new ArrayList();
		JSONObject newMessageRoot = null;

		public JsonSplitterIterator(String data) {
			super(data);
			root = new JSONObject(data);
			for (String key : root.keySet()) {
				if (root.optJSONObject(key) != null) {
					jsonComplexchilds.put(key, root.getJSONObject(key));
				} else if (root.optJSONArray(key) != null) {
					jsonComplexchilds.put(key, root.getJSONArray(key));
				} else {
					primitiveKeys.add(key);
				}

			}
			jsonObjectskeys = jsonComplexchilds.keySet().iterator();
		}

		@Override
		public boolean hasNext() {
			return jsonObjectskeys.hasNext();
		}

		@Override
		public String next() {
			String jsonComplexChildKey = jsonObjectskeys.next();
			newMessageRoot = new JSONObject();
			for (String key : primitiveKeys) {
				newMessageRoot.put(key, root.get(key));
			}
			if (root.optJSONObject(jsonComplexChildKey) != null) {
				newMessageRoot.put(jsonComplexChildKey, root.getJSONObject(jsonComplexChildKey));
			} else if (root.optJSONArray(jsonComplexChildKey) != null) {
				newMessageRoot.put(jsonComplexChildKey, root.getJSONArray(jsonComplexChildKey));
			}

			return newMessageRoot.toString();
		}

	}

	class StringMatcherSplitterIterator extends SplitterIterator<String> {
		private Pattern pattern = null;
		private Matcher matcher = null;
		private int matchedEndIndex = 0;
		private int stringBeginIndex = 0;

		public StringMatcherSplitterIterator(String data, String patternString) {
			super(data);
			pattern = Pattern.compile(patternString);
			matcher = pattern.matcher(data);
			boolean matched = matcher.find();
			if (matched) {
				matchedEndIndex = matcher.end();
				matcher.start();
				stringBeginIndex = 0;
				matcher.reset();
			}
		}

		@Override
		public boolean hasNext() {
			if (data.length() == 0)
				return false;
			boolean matched = matcher.find();
			if (matched) {
				matchedEndIndex = matcher.end();
				matcher.start();
			} else if (data.length() != matchedEndIndex) {
				matched = true;
				matchedEndIndex = data.length();
			}
			return matched;
		}

		@Override
		public String next() {
			String subStr = null;
			if (stringBeginIndex != matchedEndIndex) {
				subStr = data.substring(stringBeginIndex, matchedEndIndex);
				stringBeginIndex = matchedEndIndex;
			}
			return subStr;
		}
	}

	class LengthMatcherSplitterIterator extends SplitterIterator<String> {
		private int matcher;
		private int currLoc = 0;

		public LengthMatcherSplitterIterator(String data, int matcher) {
			super(data);
			this.matcher = matcher;
		}

		@Override
		public boolean hasNext() {
			if (getData().length() == currLoc) {
				return false;
			} else {
				return true;
			}
		}

		@Override
		public String next() {
			String ret = null;
			if (getData().length() == 0 || currLoc >= getData().length())
				return ret;
			else {
				int length = Math.min(getData().length() - currLoc, matcher);
				ret = getData().substring(currLoc, currLoc + length);
				currLoc += length;
			}
			return ret;

		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	static private class IIP {
		private String splitUsing;
		private int length;
		private String pattern;
		private boolean stream;

		public IIP() {
			super();
		}

		@JsonProperty("splitUsing")
		public String getSplitUsing() {
			return splitUsing;
		}

		@JsonProperty("splitUsing")
		public void setSplitUsing(String splitUsing) {
			this.splitUsing = splitUsing;
		}

		@JsonProperty("length")
		public int getLength() {
			return length;
		}

		@JsonProperty("length")
		public void setLength(int length) {
			this.length = length;
		}

		@JsonProperty("pattern")
		public String getPattern() {
			return pattern;
		}

		@JsonProperty("pattern")
		public void setPattern(String pattern) {
			this.pattern = pattern;
		}

		@JsonProperty("stream")
		public boolean isStream() {
			return stream;
		}

		@JsonProperty("stream")
		public void setStream(boolean stream) {
			this.stream = stream;
		}
	}

	enum SplitUsing {
		Fixed_Length("Fixed Length"), STRING("String"), JSON("JSON");

		private final String splitUsing;

		private SplitUsing(String splitUsing) {
			this.splitUsing = splitUsing;
		}

		public String getSplitUsing() {
			return splitUsing;
		}

	}
}
