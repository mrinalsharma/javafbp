var Redis = require(process.env.FLOW_NODE_DIR + '\\node_modules\\ioredis\\built\\index.js');

var dt = require(process.env.FLOW_NODE_DIR + '\\Component');
var redis = null;
getComponent = () => {

  var c = new dt.Component();
  c.inPorts.add('OPTIONS', {datatype: 'string', iip: true, schema: "RedisGet.json"});
  c.inPorts.add('IN', { datatype: 'string', iip: false });
  c.outPorts.add('OUT',{ datatype: 'string', arrayPort: false, fixedSize: false });
  c.isSubgraph = false;
  c.icon = "my icon";
  c.description = "Redis client to get the key-value pair.";
  c.componentName = "RedisGet";
  c.setKeepRunning(true);
  c.setStart((iipData) => {
    console.log(JSON.stringify(iipData));
    iipData = JSON.parse(iipData.OPTIONS)
    console.log(JSON.stringify(iipData.URL));
    if (iipData.connectWithUrl === true) {
      redis = new Redis(iipData.URL);
    }
    else if (iipData.host !== null && iipData.port !== null && iipData.username === null && iipData.password === null) {
      redis = new Redis(iipData.port, iipData.host);
    } else if (iipData.host === null && iipData.port !== null) {
      redis = new Redis(iipData.port);
    }
    else if (iipData.host === null && iipData.port === null) {
      redis = new Redis();
    }
    else {
      redis = new Redis({
        port: iipData.port, // Redis port
        host: iipData.host, // Redis host
        family: 4, // 4 (IPv4) or 6 (IPv6)
        username: iipData.username,
        password: iipData.password,
        db: 0,
      });
    }

  })

  return c.process((c, port, payload) => {
    console.log("Payload: " + payload);
    payload = JSON.parse(payload);
    redis.get(payload.key, (err, value) => {
        console.log("Key: ", payload.key , " Msg: " , value);
        outPort = c.getOutPort("OUT");
        outPort.send(JSON.stringify({key:payload.key, value:value}));
      });
  });
};

