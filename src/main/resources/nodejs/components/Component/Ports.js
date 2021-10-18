const { BasePort } = require('./BasePort.js');
var InPort = require('./InPort');
var OutPort = require('./OutPort');
/**
 * @typedef {import("./BasePort.js").BaseOptions} PortOptions
 */

// Ports collection classes for NoFlo components. These are
// used to hold a set of input or output ports of a component.
class Ports {

  constructor() {
    this.ports = new Map();
  }

  add(name, options = {}) {
    if ((name === 'add') || (name === 'remove')) {
      throw new Error('Add and remove are restricted port names');
    }

    /* eslint-disable no-useless-escape */
    if (!name.match(/^[A-Z0-9_\.\/]+$/)) {
      throw new Error(`Port names can only contain uppercase alphanumeric characters and underscores. '${name}' not allowed`);
    }

    // Remove previous implementation
    if (this.ports[name]) { this.remove(name); }

    this.ports[name] = new BasePort(options);
    this.ports[name].name = name;
    this.ports[name].native = nativePorts;
    return this; // chainable
  }

  /**
   * @param {string} name
   */
  remove(name) {
    if (!this.ports[name]) { throw new Error(`Port ${name} not defined`); }
    delete this.ports[name];
    delete this[name];
    return this; // chainable
  }

  getPort(name) {
    if (!this.ports[name]) { throw new Error(`Port ${name} not defined`); }
    return this[name];
  }
}

/**
 * @typedef {{ [key: string]: InPort|import("Component/InPort.js").PortOptions }} InPortsOptions
 */
class InPorts extends Ports {
  /**
   * @param {InPortsOptions} [ports]
   */
  constructor() {
    super();
  }
}

/**
 * @typedef {{ [key: string]: OutPort|import("Component/OutPort.js").PortOptions }} OutPortsOptions
 */
class OutPorts extends Ports {
  /**
   * @param {OutPortsOptions} [ports]
   */
  constructor() {
    super();;
  }

}
exports.OutPorts = OutPorts;
exports.InPorts = InPorts;
exports.Ports = Ports;