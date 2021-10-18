var http = require('http');
var dt = require(process.env.FLOW_NODE_DIR + '\\Component');
let host = 'localhost';
let port = 8001;
let server = null;
let server2 = null;


getComponent = () => {


  var c = new dt.Component();
  c.outPorts.add('OUT',
    { datatype: 'string', arrayPort: false, fixedSize: false });
  c.isSubgraph = false;
  c.icon = "my icon";
  c.description = "demo description hahahah";
  c.componentName = "demo";
  c.setKeepRunning(true);
  c.setStart((iipData) => {
    const requestListener = function (req, res) {
      if (req.method === 'POST') {
        let body = '';
        req.on('data', chunk => {
          body += chunk.toString(); // convert Buffer to string
        });
        req.on('end', () => {
          outPort = c.getOutPort("OUT");
          outPort.send(body);
          res.writeHead(200);
          res.end("My first server!");
        });
      } else {
        res.writeHead(200);
        res.end("My first server!");
      }

    };
    server = http.createServer(requestListener);
    server.listen(port, host, () => {
      console.log(`Server is running on http://${host}:${port}`);
    });
    console.log("Started.1");
    server2 = http.createServer(requestListener);
    server2.listen(8788, host, () => {
      console.log(`Server is running on http://${host}:${8788}`);
    });
    console.log("Started.2");
  })

  c.setStop(() => {
    return new Promise((resolve, reject) => {
      server.close((err) => {
        resolve("SUCCESS");
      })
    }).then(() => {
      return new Promise((resolve, reject) => {
        server2.close((err) => {
          resolve("SUCCESS2");
        })
      })
    });
  })

  return c.process((c, port, payload) => {
    console.log("Process callback executed.");
    outPort = c.getOutPort("OUT");
    outPort.send(payload);
  });
};

