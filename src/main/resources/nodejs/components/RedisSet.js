var Redis = require(process.env.FLOW_NODE_DIR + '\\node_modules\\ioredis\\built\\index.js');

var dt = require(process.env.FLOW_NODE_DIR + '\\Component');
var redis = null;
var iip = null;
getComponent = () => {

  var c = new dt.Component();
  c.inPorts.add('OPTIONS', {datatype: 'object', iip: true, schema: 'RedisSet.json'});
  c.inPorts.add('IN', { datatype: 'string', iip: false });
  c.isSubgraph = false;
  c.icon = "my icon";
  c.description = "Redis client to set the key-value pair.";
  c.componentName = "RedisSet";
  c.setKeepRunning(true);
  c.setStart((iipData) => {
    console.log(JSON.stringify(iipData));
    iipData = JSON.parse(iipData.OPTIONS)
    console.log(JSON.stringify(iipData.URL));
    iip = iipData;
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
    console.log("Payload: " + payload.content);
    const content = JSON.parse(payload.content);
    if(typeof content.value === 'object'){
      content.value = JSON.stringify(content.value);
    }
    console.log("Key: ", content.key , " Msg: " , content.value);
    redis.set(content.key, content.value,"PX", iip.expiresIn);

  });
};

