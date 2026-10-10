const express = require('express');
const http = require('http');
const { Server } = require('socket.io');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST']
  }
});

const PORT = process.env.PORT || 3000;
const sourceSockets = new Map();
const clientSockets = new Map();
const relayMap = new Map();

app.use(express.json());

app.get('/', (req, res) => {
  res.json({
    name: 'Testt Relay Server',
    status: 'ok',
    version: '2.0.0',
    roles: ['source', 'client'],
    endpoints: ['/health', '/stats']
  });
});

app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString(), uptime: process.uptime() });
});

app.get('/stats', (req, res) => {
  res.json({
    sources: sourceSockets.size,
    clients: clientSockets.size,
    activeRelays: relayMap.size,
    timestamp: new Date().toISOString()
  });
});

function matchRelay() {
  if (sourceSockets.size === 0 || clientSockets.size === 0) return;

  const sourceEntry = Array.from(sourceSockets.entries())[0];
  const clientEntry = Array.from(clientSockets.entries())[0];

  if (!sourceEntry || !clientEntry) return;

  const [sourceId, sourceInfo] = sourceEntry;
  const [clientId, clientInfo] = clientEntry;

  sourceInfo.socket.emit('relayRequest', {
    from: clientId,
    message: 'A client is requesting internet relay.'
  });

  clientInfo.socket.emit('status', {
    type: 'relay-ready',
    message: 'Waiting for source approval...',
    sourceId
  });

  sourceInfo.socket.emit('status', {
    type: 'relay-ready',
    message: 'A client is ready to connect.',
    clientId
  });
}

io.on('connection', (socket) => {
  console.log('Connected:', socket.id);

  socket.on('registerSource', (metadata = {}) => {
    sourceSockets.set(socket.id, { socket, metadata });
    socket.emit('status', { type: 'source-registered', message: 'Source registered.' });
    matchRelay();
  });

  socket.on('registerClient', (metadata = {}) => {
    clientSockets.set(socket.id, { socket, metadata });
    socket.emit('status', { type: 'client-registered', message: 'Client registered.' });
    matchRelay();
  });

  socket.on('acceptRelay', (payload = {}) => {
    const { sourceId, clientId } = payload;
    if (!sourceId || !clientId) return;

    const s = io.sockets.sockets.get(sourceId);
    const c = io.sockets.sockets.get(clientId);

    if (!s || !c) return;

    const relayKey = `${sourceId}:${clientId}`;
    relayMap.set(relayKey, { sourceId, clientId, createdAt: Date.now() });

    s.emit('connectedPeer', { peerId: clientId, type: 'client' });
    c.emit('connectedPeer', { peerId: sourceId, type: 'source' });

    s.emit('status', { type: 'relay-accepted', message: 'Relay approved.', clientId });
    c.emit('status', { type: 'relay-accepted', message: 'Relay approved by source.', sourceId });
  });

  socket.on('packet', (payload = {}) => {
    const { target, data } = payload;
    if (!target) {
      socket.emit('status', { type: 'error', message: 'Target not specified.' });
      return;
    }

    const targetSocket = io.sockets.sockets.get(target);
    if (!targetSocket) {
      socket.emit('status', { type: 'error', message: 'Target peer not available.' });
      return;
    }

    targetSocket.emit('packet', {
      from: socket.id,
      data,
      receivedAt: Date.now()
    });
  });

  socket.on('ping', (payload, callback) => {
    if (typeof callback === 'function') {
      callback({ ok: true, ts: Date.now(), from: socket.id, payload });
    }
  });

  socket.on('disconnect', () => {
    sourceSockets.delete(socket.id);
    clientSockets.delete(socket.id);

    for (const [key, relay] of relayMap.entries()) {
      if (relay.sourceId === socket.id || relay.clientId === socket.id) {
        relayMap.delete(key);
      }
    }

    console.log('Disconnected:', socket.id);
  });
});

server.listen(PORT, () => {
  console.log(`Relay server running on http://localhost:${PORT}`);
});
