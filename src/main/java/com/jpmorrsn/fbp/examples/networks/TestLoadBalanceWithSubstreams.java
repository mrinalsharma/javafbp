package com.jpmorrsn.fbp.examples.networks;

/*
 * WARNING! 
 * 
 * I believe this will work reliably if the longest subtream will fit within the connections on the shortest path
 * between the LoadBalance process and the SubstreamSensitiveMerge.  
 * 
 * If this rule is violated, you get unpredictable deadlocks.  
 * This test seems to crash about 1 time in 5 or 6!  SlowPass uses a random
 * interval between 0 and 500 msecs. 
 * 
 * It is therefore safer to use LoadBalance and SubstreamSensitiveMerge in different networks*!
 * 
 * You can test this by changing the capacities marked with arrows.  
 * 
 * The substreams generated by GenSS comprise Open bracket, 5 IPs, Close bracket.  
 * 
 */
	import com.jpmorrsn.fbp.engine.Network;

	public class TestLoadBalanceWithSubstreams extends Network {
	  
	  @Override
	  protected void define() {
		boolean makeMergeSubstreamSensitive = true;
				
	    component("GenSS", com.jpmorrsn.fbp.examples.components.GenSS.class);
	    component("LoadBalance", com.jpmorrsn.fbp.components.LoadBalance.class);		    
	    component("Passthru0", com.jpmorrsn.fbp.examples.components.SlowPass.class);
	    component("Passthru1", com.jpmorrsn.fbp.examples.components.SlowPass.class);
	    component("Passthru2", com.jpmorrsn.fbp.components.Passthru.class);
	    component("SlowPass", com.jpmorrsn.fbp.examples.components.SlowPass.class);
	    component("Show", com.jpmorrsn.fbp.components.WriteToConsole.class);
	    component("Check", com.jpmorrsn.fbp.examples.components.CheckSequenceWithinSubstreams.class);
	    
	    connect(component("GenSS"), port("OUT"), component("LoadBalance"), port("IN"), 4);
	    connect(component("LoadBalance"), port("OUT[0]"), component("Passthru0"), port("IN"), 1);  // <---
	    connect(component("LoadBalance"), port("OUT[1]"), component("Passthru1"), port("IN"), 1);  // <---
	    connect(component("LoadBalance"), port("OUT[2]"), component("Passthru2"), port("IN"), 1);  // <---
	    
		if (makeMergeSubstreamSensitive) {
			component("SubstreamSensitiveMerge",
					com.jpmorrsn.fbp.components.SubstreamSensitiveMerge.class);
			
			connect(component("Passthru0"), port("OUT"),
					component("SubstreamSensitiveMerge"), port("IN[0]"), 2);  // <---
			connect(component("Passthru1"), port("OUT"),
					component("SubstreamSensitiveMerge"), port("IN[1]"), 2);  // <---
			connect(component("Passthru2"), port("OUT"),
					component("SubstreamSensitiveMerge"), port("IN[2]"), 2);  // <---
			
			connect(component("SubstreamSensitiveMerge"), port("OUT"),
					component("SlowPass"), port("IN"), 4);
		} else {
			connect(component("Passthru0"), port("OUT"), component("SlowPass"),
					port("IN"), 2);
			connect(component("Passthru1"), port("OUT"), component("SlowPass"),
					port("IN"), 2);
			connect(component("Passthru2"), port("OUT"), component("SlowPass"),
					port("IN"), 2);
		}
	    //connect(component("SlowPass"), port("OUT"), component("Show"), port("IN"), 4);
	    connect(component("SlowPass"), port("OUT"), component("Check"), port("IN"), 4);
	    connect(component("Check"), port("OUT"), component("Show"), port("IN"), 4);
	    
	    initialize("100", component("GenSS"), port("COUNT"));
	    
	  }

	  public static void main(final String[] argv) throws Exception {
	    new TestLoadBalanceWithSubstreams().go();
	  }

	}

 
