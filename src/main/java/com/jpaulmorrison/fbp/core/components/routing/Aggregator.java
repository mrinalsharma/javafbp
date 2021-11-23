package com.jpaulmorrison.fbp.core.components.routing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.jpaulmorrison.fbp.core.engine.Component;
import com.jpaulmorrison.fbp.core.engine.ComponentDescription;
import com.jpaulmorrison.fbp.core.engine.FlowError;
import com.jpaulmorrison.fbp.core.engine.InPort;
import com.jpaulmorrison.fbp.core.engine.InPorts;
import com.jpaulmorrison.fbp.core.engine.InputPort;
import com.jpaulmorrison.fbp.core.engine.OutPort;
import com.jpaulmorrison.fbp.core.engine.OutputPort;
import com.jpaulmorrison.fbp.core.engine.Packet;
import com.jpaulmorrison.fbp.core.engine.PacketAttributeAccessor;

/*
 * Aggregator rely on 3 properties
 * 1. Coorelation: Which messages belong together
 * 2. Completeness Condition:  When are we ready to publish the result and create a Aggregated Packet
 * 3. How do we combine the received packets to create an Aggregated Packet.
 */
@ComponentDescription("Aggregate a stream into Single packet based on some criteria")
@OutPort(value = "OUT")
@InPorts({ @InPort("IN"),
		@InPort(value = "OPTIONS", type = Object.class, isIIP = true, uiSchema = "aggregate.json", optional = true) })
public class Aggregator extends Component {
	private InputPort inPort;
	private OutputPort outPort;
	private InputPort optionsPort;
	private InMemoryAggregates aggregates = new InMemoryAggregates();
	@Override
	protected void execute() throws Exception {
		Packet<?> p = inPort.receive();
		PacketAttributeAccessor packetAttributeAccessor = new PacketAttributeAccessor(p);
		if (packetAttributeAccessor.getCorrelationId() != null) {
			Aggregate aggregate = aggregates.getAggregate(packetAttributeAccessor.getCorrelationId());
			if (aggregate == null) {
				aggregate = new InMemoryAggregate(new PacketCountStrategy(3), packetAttributeAccessor.getCorrelationId());
				aggregate.addPacket(p);
				aggregates.addAggregate(aggregate);
			} else {
				aggregate.addPacket(p);
			}
			if (aggregate.isComplete()) {
				outPort.send(aggregate.getResultPacket());
			}
			
		} else {
			FlowError.complain("Packet dosen't have correlationId to facilitate aggregation.");
		}
	}

	@Override
	protected void openPorts() {
		inPort = openInput("IN");
		optionsPort = openInput("OPTIONS");
		outPort = openOutput("OUT");
		ignorePacketCountError = true;
	}

	static class InMemoryAggregates implements Aggregates {
		Map<String, Aggregate> aggregateMap = new HashMap<String, Aggregate>();

		@Override
		public void addAggregate(Aggregate aggtegate) {
			aggregateMap.putIfAbsent(aggtegate.getId(), aggtegate);

		}

		@Override
		public void removeAggregate(Aggregate aggtegate) {
			aggregateMap.remove(aggtegate.getId());
		}

		@Override
		public Aggregate getAggregate(String id) {
			return aggregateMap.getOrDefault(id, null);

		}
	}

	static class InMemoryAggregate implements Aggregate {
		private AggregateCompleteStrategy strategy = null;
		private String id = null;
		private List<Packet<?>> aggregate = new ArrayList<Packet<?>>();

		public InMemoryAggregate(String id) {
			this(new PacketCountStrategy(3), id);
		}

		public InMemoryAggregate(AggregateCompleteStrategy strategy, String id) {
			super();
			this.strategy = strategy;
			this.id = id;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public void addPacket(Packet<?> p) {
			aggregate.add(p);
		}

		@Override
		public boolean isComplete() {
			return strategy.isComplete(this);
		}

		@Override
		public Iterator<Packet<?>> iterator() {
			return aggregate.iterator();
		}

		@Override
		public Packet<?> getResultPacket() {
			aggregate = aggregate.stream().sorted(new Comparator<Packet<?>>() {

				@Override
				public int compare(Packet<?> o1, Packet<?> o2) {
					return  (int) (Long.parseLong((String)o1.getAttribute("sequenceNumber")) - Long.parseLong((String)o2.getAttribute("sequenceNumber")));
				}
				
			}).collect(Collectors.toList());
			Packet<?> packet = aggregate.get(0);
			for (int i = 1; i < aggregate.size(); i++) {
				packet.attach((String) packet.getAttribute("correlationId"), aggregate.get(i));
			}
			return packet;
		}

	}

	static class PacketCountStrategy implements AggregateCompleteStrategy {
		private int count = 0;

		public PacketCountStrategy(int count) {
			super();
			this.count = count;
		}

		@Override
		public boolean isComplete(Aggregate aggregate) {
			Iterator iterator = aggregate.iterator();
			int packetCount = 0;
			while (iterator.hasNext()) {
				iterator.next();
				packetCount++;
			}
			return packetCount >= this.count;
		}

	}

	static interface Aggregates {

		public void addAggregate(Aggregate aggtegate);

		public void removeAggregate(Aggregate aggtegate);

		public Aggregate getAggregate(String id);

	}

	static interface Aggregate {
		public String getId();

		public void addPacket(Packet<?> p);

		public boolean isComplete();

		public Iterator<Packet<?>> iterator();

		public Packet<?> getResultPacket();
	}

	static interface AggregateCompleteStrategy {
		public boolean isComplete(Aggregate aggregate);
	}

}
