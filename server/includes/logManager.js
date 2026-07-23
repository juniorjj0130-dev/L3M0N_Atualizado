const db = require('./databaseGateway');

async function writeAdminLog(type, message) {
    if (db.usePrisma) {
        const current = await db.prisma.admin.findFirst();
        const logs = Array.isArray(current?.logs) ? current.logs : [];
        logs.push({ time: new Date().toISOString(), type: type.name, message });
        await db.prisma.admin.upsert({
            where: { username: current?.username || 'admin' },
            update: { logs },
            create: { username: 'admin', password: '', loginToken: '', logs, ipLog: [] },
        });
        return;
    }

    db.maindb.get('admin.logs').push({
        time: new Date(),
        type: type.name,
        message,
    }).write();
}

module.exports = {
    log: async (type, message) => {
        await writeAdminLog(type, message);
        console.log(type.name, message);
    },
    getLogs: async () => {
        if (db.usePrisma) {
            const admin = await db.prisma.admin.findFirst();
            return Array.isArray(admin?.logs) ? admin.logs : [];
        }
        return db.maindb.get('admin.logs').sortBy('time').reverse().value();
    },
};