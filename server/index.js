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
const connections = new Map();

app.use(express.json());

app.get('/', (req, res) => {
  res.json({
    name: 'Test Relay Server',
    status: 'ok',
    version: '1.0.0',
    description: 'Relay server for source-client internet sharing demo',
    stats: {
      sources: sourceSockets.size,
      clients: clientSockets.size,
      activeConnections: connections.size
    }
  });
});

app.get('/health', (req, res) => {
  res.json({ 
    status: 'ok', 
    timestamp: new Date().toISOString(),
    uptime: process.uptime()
  });
});

app.get('/stats', (req, res) => {
  res.json({
    sources: sourceSockets.size,
    clients: clientSockets.size,
    activeConnections: connections.size,
    timestamp: new Date().toISOString()
  });
});

function matchClientWithSource() {
  if (sourceSockets.size === 0 || clientSockets.size === 0) {
    return;
  }

  const sourceEntries = Array.from(sourceSockets.entries());
  const clientEntries = Array.from(clientSockets.entries());

  // Try to match first available source with first available client
  const [sourceId, sourceInfo] = sourceEntries[0];
  const [clientId, clientInfo] = clientEntries[0];

  if (!sourceInfo || !clientInfo) {
    return;
  }

  const sourceSocket = sourceInfo.socket;
  const clientSocket = clientInfo.socket;

  // Notify source about relay request
  sourceSocket.emit('relayRequest', {
    from: clientId,
    message: 'A client is asking for internet relay.'
  });

  // Notify client that relay is ready
  clientSocket.emit('status', {
    type: 'relay-ready',
    message: 'Waiting for source approval...',
    sourceId
  });

  console.log(`Matched: source=${sourceId} <-> client=${clientId}`);
}

io.on('connection', (socket) => {
  console.log('Device connected:', socket.id);

  socket.on('registerSource', () => {
    sourceSockets.set(socket.id, { socket });
    socket.emit('status', {
      type: 'source-registered',
      message: 'Registered as source. Waiting for client...'
    });
    console.log('Source registered:', socket.id);
    matchClientWithSource();
  });

  socket.on('registerClient', () => {
    clientSockets.set(socket.id, { socket });
    socket.emit('status', {
      type: 'client-registered',
      message: 'Registered as client. Waiting for source...'
    });
    console.log('Client registered:', socket.id);
    matchClientWithSource();
  });

  socket.on('acceptRelay', (payload = {}) => {
    const { sourceId, clientId } = payload;

    if (!sourceId || !clientId) {
      return;
    }

    const sourceSocket = io.sockets.sockets.get(sourceId);
    const clientSocket = io.sockets.sockets.get(clientId);

    if (sourceSocket && clientSocket) {
      // Create connection mapping
      connections.set(`${sourceId}-${clientId}`, {
        source: sourceId,
        client: clientId,
        createdAt: new Date()
      });

      // Notify both devices
      sourceSocket.emit('status', {
        type: 'relay-accepted',
        message: 'Relay approved. Connected to client.',
        clientId
      });

      clientSocket.emit('status', {
        type: 'relay-accepted',
        message: 'Source approved. Relay active.',
        sourceId
      });

      // Send peer info
      sourceSocket.emit('connectedPeer', {
        peerId: clientId,
        type: 'client'
      });

      clientSocket.emit('connectedPeer', {
        peerId: sourceId,
        type: 'source'
      });

      console.log(`Relay active: source=${sourceId} <-> client=${clientId}`);
    }
  });

  socket.on('sendData', (payload = {}) => {
    const { target, data } = payload;

    if (!target) {
      socket.emit('status', {
        type: 'error',
        message: 'No target specified.'
      });
      return;
    }

    const targetSocket = io.sockets.sockets.get(target);
    if (targetSocket) {
      targetSocket.emit('data', {
        from: socket.id,
        payload: data || payload,
        timestamp: new Date().toISOString()
      });
    } else {
      socket.emit('status', {
        type: 'error',
        message: 'Target peer not found.'
      });
    }
  });

  socket.on('ping', (payload, callback) => {
    if (typeof callback === 'function') {
      callback({ 
        ok: true, 
        timestamp: Date.now(), 
        from: socket.id, 
        payload 
      });
    }
  });

  socket.on('disconnect', () => {
    sourceSockets.delete(socket.id);
    clientSockets.delete(socket.id);
    
    // Clean up connections involving this socket
    for (const [key, conn] of connections.entries()) {
      if (conn.source === socket.id || conn.client === socket.id) {
        connections.delete(key);
      }
    }
    
    console.log('Device disconnected:', socket.id);
  });
});

server.listen(PORT, () => {
  console.log(`\n🚀 Test Relay Server running on http://localhost:${PORT}`);
  console.log(`📊 Check stats at http://localhost:${PORT}/stats\n`);
});
