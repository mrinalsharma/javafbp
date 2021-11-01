var http = require('http');
var webPush = require('web-push');
var dt = require(process.env.FLOW_NODE_DIR + '\\Component');
let server = null;
let server2 = null;

var vapidKeys = {
  publicKey: '',
  privateKey: ''
};

getComponent = () => {

  var c = new dt.Component();
  c.inPorts.add('OPTIONS', {
    datatype: 'string', iip: true, schema: {
      "schema": {
        "title": "WebPush form",
        "type": "object",
        "required": [
          "vapiPublicKey",
          "vapiPrivateKey",
          "firebaseApiKey"
        ],
        "properties": {
          "vapiPublicKey": {
            "type": "string",
            "title": "VAPI Public Key",
            "default": "",
            "contentEncoding": "base64"
          },
          "vapiPrivateKey": {
            "type": "string",
            "title": "VAPI Private Key",
            "default": "",
            "contentEncoding": "base64"
          },
          "firebaseApiKey": {
            "type": "string",
            "title": "Firebase API key",
            "default": ""
          }
        }
      }
    }
  });
  
  c.inPorts.add('IN', {datatype: 'string', iip: false });
  c.isSubgraph = false;
  c.icon = "my icon";
  c.description = "Send WebPush";
  c.componentName = "TEST_WebPush";
  c.setKeepRunning(true);
  c.setStart((iipData) => {
    console.log(JSON.stringify(iipData));
    iipData = JSON.parse(iipData.OPTIONS)
    vapidKeys.publicKey = iipData.vapiPublicKey;
    vapidKeys.privateKey = iipData.vapiPrivateKey;
    webPush.setVapidDetails(
      'mailto:web-push-book@localhost',
      vapidKeys.publicKey,
      vapidKeys.privateKey
    );
    webPush.setGCMAPIKey(iipData.firebaseApiKey);
  })

  return c.process((c, port, payload) => {
    payload = JSON.parse(payload);
    console.log("Payload: " + payload);
    webPush.sendNotification(payload.sub,payload.msg)
      .then(success => { console.log("SUCCESS:" + success.toString()) })
      .catch(error => { console.log("Error: " + error.toString()) });
  });
};

