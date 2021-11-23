class Packet {
    constructor() {
        this.attrs = {};
        this.content = null;
        this.chains = {};
        ;
    }

    addHeader(key, value) {
        if (typeof key !== 'string' || key == null || key === undefined) {
            throw new Error('Provide a valid String key.');
        }
        if (typeof value !== 'string') {
            throw new Error('Header values can only be String.');
        }
        this.attrs[key] = value;
    }

    removeHeader(key) {
        if (typeof key !== 'string' || key == null || key === undefined) {
            throw new Error('Provide a valid String key.');
        }
        this.attrs.delete(key);
    }

    copyHeaders(packet) {
        packet.attrs = new Map(this.attrs);
    }

    addContent(data) {
        if (typeof data !== 'string' || data == null || data === undefined) {
            throw new Error('Provide a valid String data.');
        }
        this.content = data;
    }

    attach(key, packet) {
        console.log("Called Attach");
        if (!this.chains.hasOwnProperty(key)) {
            
            this.chains[key] = [packet]
        }
        else {
            this.chains[key].push(packet);
        }
    }

    toJson() {

    }
}

module.exports = {
    Packet
}