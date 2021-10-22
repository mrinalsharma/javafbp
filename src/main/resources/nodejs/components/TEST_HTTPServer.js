var http = require('http');
var dt = require(process.env.FLOW_NODE_DIR + '\\Component');
let server = null;
let server2 = null;

getComponent = () => {

  var c = new dt.Component();
  c.inPorts.add('OPTIONS',{ datatype: 'object', iip: true ,schema:{
    "schema": {
      "type": "object",
      "properties": {
        "host1": {
          "type": "string",
          "title": "Hostname1",
          "description": "Hostname where server is executing."
        },
         "port1": {
          "type": "string",
          "title": "Port1",
          "description": "Port naumber where server will listen."
        },
         "host2": {
          "type": "string",
          "title": "Hostname2",
          "description": "Hostname where server is executing."
        },
         "port2": {
          "type": "string",
          "title": "Port2",
          "description": "Port naumber where server will listen."
        }
      }
    }
  }});
  c.outPorts.add('OUT',
    { datatype: 'string', arrayPort: false, fixedSize: false });
  c.isSubgraph = false;
  c.icon = "my icon";
  c.description = "demo description hahahah";
  c.componentName = "TEST_HTTPServer";
  c.setKeepRunning(true);
  c.setStart((iipData) => {
    console.log(JSON.stringify(iipData));
    iipData = JSON.parse(iipData.OPTIONS)
    console.log(iipData.host1);
    const host1 = iipData.host1;
    const port1 = iipData.port1;
    const host2 = iipData.host2;
    const port2 = iipData.port2;
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
          res.end("My first flow Node server!");
        });
      } else {
        res.writeHead(200);
        res.end("My first flow Node server!");
      }

    };
    server = http.createServer(requestListener);
    console.log("Server1 to listen on ", iipData.host1,":",iipData.port1);
    server.listen(port1, host1, () => {
      console.log(`Server is running on http://${host1}:${port1}`);
    });
    console.log("Started.1");
    server2 = http.createServer(requestListener);
    console.log("Server2 to listen on ", iipData.host2,":",iipData.port2);
    server2.listen(port2, host2, () => {
      console.log(`Server is running on http://${host2}:${port2}`);
    });
    console.log("Started.2");
  })

  c.setStop(() => {
    return new Promise((resolve, reject) => {
      server.close((err) => {
        console.log("server 1 stopped");
        resolve("SUCCESS");
      })
    }).then(() => {
      return new Promise((resolve, reject) => {
        server2.close((err) => {
          console.log("server 2 stopped");
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

