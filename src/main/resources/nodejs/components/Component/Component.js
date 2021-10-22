var ports = require('./Ports')
var process = require('process')

process.on('uncaughtException', err => {
  console.error('There was an uncaught error',err.toString());
}) 

process.on('unhandledRejection', (reason, promise) => {
  console.log('Unhandled Rejection at:', 'reason:');
});

'use strict';
class Component {
  constructor() {
    this.inPorts = new ports.InPorts();
    this.outPorts = new ports.OutPorts();
    this.className = "NodeJS.class";
    this.isSubgraph = false;
    // Set the default component icon and description
    this.icon = '';
    this.description = '';
    this.schema = {}

    /** @type {string|null} */
    this.componentName = null;
    /** @type {string|null} */
    this.baseDir = null;
    this.mustRun = false;
    this.selfStarting = false;
    this.keepRunning = false;
    this.handleStart = () => {};
    this.handleStop = () => { return new Promise((resolve,reject) => {resolve("SUCCESS")})}
  }

  /**
   * @param {StartFunction} handleStart - Starting function
   * @returns {this}
   */
  setStart(handleStart) {
    if (typeof handleStart !== 'function') {
      throw new Error('Start handler must be a function');
    }
    this.handleStart = handleStart;
    return this;
  }

    /**
   * @param {boolean} keepRunning - Keep running even if no data on InputPort
   * @returns {this}
   */
  setKeepRunning(keepRunning)
  {
    this.keepRunning = keepRunning;
  }

    /**
   * @param {string} iipData - receives IIP data as a Key-value pair
   */
  start(iipData)
  {      
    this.handleStart(JSON.parse(iipData));
  }

    /**
   * @param {StopFunction} handleStop - Stopping function
   * @returns {this}
   */

  setStop(handleStop) {
    if (typeof handleStop !== 'function') {
      throw new Error('Process handler must be a function');
    }
    this.handleStop = handleStop;
    return this;
  }

   /**
   * @returns {Promise}
   */
  stop()
  {
    return this.handleStop();
  }

  getDescription() { return this.description; }

  getPortsJson() {
    //console.log(this.InPorts);
    return (JSON.stringify({
      InPorts: this.inPorts.ports,
      OutPorts: this.outPorts.ports,
    }));
  }

  getInPort(name) {
    return this.inPorts.ports[name];
  }

  getOutPort(name) {
    return this.outPorts.ports[name];
  }

  getComponentMeta() {
    return (JSON.stringify({
      name: this.componentName,
      schema: JSON.stringify(this.schema),
      description: this.description,
      className: this.className,
      subgraph: this.subgraph,
      icon: this.icon,
      mustRun: this.mustRun,
      selfStarting: this.selfStarting,
      inPorts: this.inPorts.ports,
      outPorts: this.outPorts.ports,
      keepRunning: this.keepRunning
    }));
  }

  isReady() { return true; }

  isSubgraph() { return false; }

  /**
   * @param {string} icon - Updated icon for the component
   */
  setIcon(icon) {
    this.icon = icon;
  }

  getIcon() { return this.icon; }

  // Sets process handler function
  /**
   * @param {ProcessingFunction} handle - Processing function
   * @returns {this}
   */



  // Sets process handler function
  /**
   * @param {ProcessingFunction} handle - Processing function
   * @returns {this}
   */
  process(handle) {
    if (typeof handle !== 'function') {
      throw new Error('Process handler must be a function');
    }
    this.handle = handle;
    return this;
  }

  // ### Handling IP objects
  //
  // The component has received an Information Packet. Call the
  // processing function so that firing pattern preconditions can
  // be checked and component can do processing as needed.
  /**
   * @param {payload} content
   * @param {InPort} port
   * @returns {void}
   */
  handleIP(port, payload) {
    this.handle(this, port, payload);
  };
}
Component.description = '';
Component.icon = null;
module.exports.Component = Component;