// The list of valid datatypes for ports.
const validTypes = [
  'string',
  'number',
  'object',
  'boolean',
];

class BasePort {
  constructor(options) {
    // Options holds all options of the current port
    // We default to the `all` type if no explicit datatype
    // was provided
    this.datatype = options.datatype || 'all';
    // Normalize the legacy `integer` type to `int`.
    if (this.datatype === 'integer') { this.datatype = 'int'; }

    // By default ports are not required for graph execution
    this.required = options.required || false;
    this.addressable = options.addressable || false;
    this.iip = options.iip || false;

    // Ensure datatype defined for the port is valid
    if (validTypes.indexOf(this.datatype) === -1) {
      throw new Error(`Invalid port datatype '${datatype}' specified, valid are ${validTypes.join(', ')}`);
    }
    // Ensure schema defined for the port is valid
    if(options.schema != null && (typeof  options.schema === "string") && options.schema.endsWith('json') === true)
    {
      //console.log("****************")
      this.schema = options.schema;
    }
    else 
    {
      this.schema = JSON.stringify(options.schema || {});
    }

    /*     if (this.schema && (this.schema.indexOf('/') === -1)) {
          throw new Error(`Invalid port schema '${schema}' specified. Should be URL or MIME type`);
        } */

    this.arrayPort = options.arrayPort || false;
    if (this.arrayPort === true) {
      this.fixedSize = options.fixedSize || false;
    }
    // Description
    this.description = options.description || '';
    // Name of the port
    /** @type {string|null} */
    this.name = options.name || null;
    this.native = options.native || null;
  }

  setNative(native) {
    this.native = native;
  }

  send(data) {
    this.native.send(this.name, data);
  }

  /**
   * @returns {string}
   */
  getDataType() { return this.datatype || 'all'; }

  getSchema() { return this.schema || null; }

  getDescription() { return this.description; }
}
exports.BasePort = BasePort;
