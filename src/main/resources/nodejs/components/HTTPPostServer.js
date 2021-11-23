var http = require('http');
const url = require('url');
var dt = require(process.env.FLOW_NODE_DIR + '\\Component');
let server = null;
let server2 = null;

getComponent = () => {

  var c = new dt.Component();
  c.inPorts.add('OPTIONS', {
    datatype: 'object', iip: true, schema: {
      "schema": {
        "type": "object",
        "properties": {
          "host1": {
            "type": "string",
            "title": "Hostname",
            "description": "Hostname where server is executing."
          },
          "port1": {
            "type": "string",
            "title": "Port",
            "description": "Port naumber where server will listen."
          },
          "URI": {
            "type": "string",
            "title": "URI",
            "description": "Resource URI."
          }
        }
      }
    }
  });
  c.outPorts.add('OUT',
    { datatype: 'string', arrayPort: false, fixedSize: false });
  c.isSubgraph = false;
  c.icon = "my icon";
  c.description = "demo description hahahah";
  c.componentName = "HTTPPostServer";
  c.setKeepRunning(true);
  c.setStart((iipData) => {
    console.log(JSON.stringify(iipData));
    iipData = JSON.parse(iipData.OPTIONS)
    const host1 = iipData.host1;
    const port1 = iipData.port1;
    const uri =   iipData.URI;
    const requestListener = function (req, res) {
      var request = {
        headers:new Map(),
        body:'',
        queryParams:new Map(),
      };
      
      if (req.method === 'POST' && req.url.includes(uri)) {
        request.headers = req.headers;
        request.queryParams = url.parse(req.url,true).query;
        req.on('data', chunk => {
          request.body += chunk.toString(); // convert Buffer to string
        });
        req.on('end', () => {
          new Promise((resolve, reject) => {
            if (request.body != null) {
              resolve("SUCCESS");
            }
            else {
              reject("FAILED");
            }
          }).then(status => {
            let packet = new dt.Packet();
            packet.addHeader("correlationId","12345678");
            packet.content = JSON.stringify(request);
            console.log("Send data ", status);
            console.log("Print Body ", request.body);
            outPort = c.getOutPort("OUT");
            outPort.send(JSON.stringify(packet));
          }).catch(status => { console.log("Send data ", status); });
          res.end("CREATED");
        });
      } else {
        res.statusCode = 404;
        res.end("Resource not found!");
      }

    };
    server = http.createServer(requestListener);
    console.log("Server to listen on ", iipData.host1, ":", iipData.port1);
    server.listen(port1, host1, () => {
      console.log(`Server is running on http://${host1}:${port1}`);
    });
    console.log("Started");
  })

  c.setStop(() => {
    return new Promise((resolve, reject) => {
      server.close((err) => {
        console.log("server stopped");
        resolve("SUCCESS");
      })
    })
  })

  return c.process((c, port, payload) => {

  });
};

