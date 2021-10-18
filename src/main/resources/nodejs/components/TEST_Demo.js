var http = require('http');
var dt = require(process.env.FLOW_NODE_DIR + '\\Component');
getComponent = () => {
  var c = new dt.Component();
  c.inPorts.add('OPTIONS_NEW',
    { datatype: 'object', iip: true });
  c.inPorts.add('OPTIONS_OLD',
    { datatype: 'object', iip: true });
  c.inPorts.add('IN',
    { datatype: 'object', iip: false });
  c.outPorts.add('OUT',
    { datatype: 'string', arrayPort: false, fixedSize: false });
  c.isSubgraph = false;
  c.icon = "my icon";
  c.description = "demo description hahahah";
  c.componentName = "demo";
  return c.process((c, port, payload) => {
    console.log("Process callback executed.");
    outPort = c.getOutPort("OUT");
    outPort.send(payload);
  });
};