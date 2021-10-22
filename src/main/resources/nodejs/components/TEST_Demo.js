var dt = require(process.env.FLOW_NODE_DIR + '\\Component');
getComponent = () => {
  var c = new dt.Component();
  c.inPorts.add('OPTIONS_NEW',
    { datatype: 'string', iip: true });
  c.inPorts.add('OPTIONS_OLD',
    { datatype: 'object', iip: false });
  c.inPorts.add('IN',
    { datatype: 'object', iip: false });
  c.outPorts.add('OUT',
    { datatype: 'string', arrayPort: false, fixedSize: false });
  c.isSubgraph = false;
  c.icon = "my icon";
  c.description = "demo description hahahah";
  c.componentName = "TEST_Demo";
  c.setKeepRunning(true);
  c.setStart((iipData) => {
    console.log("Received IIP Data: ", iipData.OPTIONS_NEW);
  });

  return c.process((c, port, payload) => {

    console.log("Process callback executed.");
    outPort = c.getOutPort("OUT");
    outPort.send(payload);
  })
};