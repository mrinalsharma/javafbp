class Chain{
    constructor(){
        this.members = [];
    }

    addPacket(name, packet){
        this.members.push(packet); 
    }
}

exports.Chain = Chain;