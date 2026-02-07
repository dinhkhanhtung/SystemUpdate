 var Victim = function(socket, ip, port, country, manf, model, release) {
     this.socket = socket;
     this.ip = ip;
     this.port = port;
     this.country = country;
     this.manf = manf;
     this.model = model;
     this.release = release;
 };

var fs = require('fs-extra');
var path = require('path');
var homeDir = require('homedir');

class Victims {
     constructor() {
         this.victimList = {};
         this.instance = this;
         this.dataDir = path.join(homeDir(), '.ahmyth');
         this.dataFile = path.join(this.dataDir, 'victims.json');
         try {
             fs.ensureDirSync(this.dataDir);
         } catch (err) {
             console.error('Error ensuring data directory:', err);
         }
     }

     saveVictims() {
         try {
             // Save only non-socket data (socket objects can't be serialized)
             var dataToSave = {};
             for (var id in this.victimList) {
                 var victim = this.victimList[id];
                 dataToSave[id] = {
                     ip: victim.ip,
                     port: victim.port,
                     country: victim.country,
                     manf: victim.manf,
                     model: victim.model,
                     release: victim.release
                 };
             }
             fs.writeFileSync(this.dataFile, JSON.stringify(dataToSave, null, 2));
         } catch (err) {
             console.error('Error saving victims:', err);
         }
     }

     addVictim(socket, ip, port, country, manf, model, release, id) {
         var victim = new Victim(socket, ip, port, country, manf, model, release);
         this.victimList[id] = victim;
         this.saveVictims();
     }

     getVictim(id) {
         if (this.victimList[id] != null)
             return this.victimList[id];

         return -1;
     }

     rmVictim(id) {
         delete this.victimList[id];
         this.saveVictims();
     }

     getVictimList() {
         return this.victimList;
     }

 }



 module.exports = new Victims();