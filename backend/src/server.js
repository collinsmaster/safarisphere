const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
require('dotenv').config();

const dbService = require('./services/dbService');

const app = express();
const server = http.createServer(app);

// Initialize Socket.IO with broad CORS settings for mobile/web clients
const io = new Server(server, {
  cors: {
    origin: process.env.CORS_ORIGIN || '*',
    methods: ['GET', 'POST']
  }
});

// Port configuration
const PORT = process.env.PORT || 8080;

// Security Middleware (Helmet & CORS)
app.use(helmet({
  contentSecurityPolicy: false // De-restrict for dev review templates if needed
}));
app.use(cors({
  origin: process.env.CORS_ORIGIN || '*',
  credentials: true
}));

// Request parsers
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Global Rate Limiting to prevent denial of service (DoS) attacks
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 500, // Limit each IP to 500 requests per window
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many requests from this IP. Please try again later.' }
});
app.use(limiter);

// ------------------------------------------------------------------------------
// SOCKET.IO REALTIME PRESENTATION STATE (Vybe Rooms & Live Messaging)
// ------------------------------------------------------------------------------
io.on('connection', (socket) => {
  console.log(`[Socket] Client connected: ${socket.id}`);

  // Participate in a Vybe Room
  socket.on('room:join', ({ roomId, userId, username }) => {
    socket.join(roomId);
    console.log(`[Socket] User ${username} (${userId}) joined room: ${roomId}`);
    
    // Broadcast join news to other room members
    io.to(roomId).emit('room:member_joined', {
      userId,
      username,
      joinedAt: new Date().toISOString()
    });
  });

  // Leave a Vybe Room
  socket.on('room:leave', ({ roomId, userId, username }) => {
    socket.leave(roomId);
    console.log(`[Socket] User ${username} (${userId}) left room: ${roomId}`);
    
    io.to(roomId).emit('room:member_left', {
      userId,
      username,
      leftAt: new Date().toISOString()
    });
  });

  // Real-time Text Message in a Vybe Room
  socket.on('room:message', ({ roomId, userId, username, text, moodState }) => {
    console.log(`[Socket] Message in room ${roomId} from ${username}: ${text}`);
    
    io.to(roomId).emit('room:message_received', {
      id: `m_${Date.now()}_${Math.random().toString(36).substr(2, 4)}`,
      roomId,
      userId,
      username,
      text,
      moodState,
      created_at: new Date().toISOString()
    });
  });

  // Typing Indicator (Direct or Group Message threads)
  socket.on('chat:typing', ({ chatId, username, isTyping }) => {
    socket.to(chatId).emit('chat:typing_state', {
      chatId,
      username,
      isTyping
    });
  });

  socket.on('disconnect', () => {
    console.log(`[Socket] Client disconnected: ${socket.id}`);
  });
});

// Store io in express settings for access inside Controllers
app.set('io', io);

// ------------------------------------------------------------------------------
// REST API VERSIONED ROUTING
// ------------------------------------------------------------------------------

// Root Index Status API
app.get('/', (req, res) => {
  res.json({
    app: 'Safari Sphere Social Backend',
    version: '1.0.0',
    phase: 'early alpha Foundation',
    status: 'online',
    tech: 'Express, PostgreSQL, Socket.IO, Gemini'
  });
});

// Health check endpoint
app.get('/health', async (req, res) => {
  res.json({
    status: 'healthy',
    uptime: process.uptime(),
    timestamp: new Date().toISOString(),
    database: {
      type: dbService.isMock ? 'Mock In-Memory' : 'Active PostgreSQL',
      connected: true
    },
    capabilities: {
      socket_io: true,
      google_gemini_ai: !!process.env.GEMINI_API_KEY
    }
  });
});

// Import versioned router endpoints
const authRoutes = require('./routes/auth');
const postsRoutes = require('./routes/posts');
const roomsRoutes = require('./routes/rooms');
const momentsRoutes = require('./routes/moments');
const chatRoutes = require('./routes/chat');
const aiRoutes = require('./routes/ai');
const communitiesRoutes = require('./routes/communities');
const gamificationRoutes = require('./routes/gamification');

app.use('/api/v1/auth', authRoutes);
app.use('/api/v1/posts', postsRoutes);
app.use('/api/v1/rooms', roomsRoutes);
app.use('/api/v1/moments', momentsRoutes);
app.use('/api/v1/chats', chatRoutes);
app.use('/api/v1/ai', aiRoutes);
app.use('/api/v1/communities', communitiesRoutes);
app.use('/api/v1/gamification', gamificationRoutes);

// ------------------------------------------------------------------------------
// GLOBAL EXCEPTION & FALLBACK MIDDLEWARES
// ------------------------------------------------------------------------------

// 404 Route Not Found
app.use((req, res, next) => {
  res.status(404).json({ error: 'Endpoint destination not found.' });
});

// Global Error Handler
app.use((err, req, res, next) => {
  console.error('[Error Boundary]', err.stack || err.message);
  res.status(err.status || 500).json({
    error: err.message || 'A critical server exception occurred.'
  });
});

// Start the HTTP Listening Server
server.listen(PORT, '0.0.0.0', () => {
  console.log(`================================================================`);
  console.log(` SAFARI SPHERE - Production Backend is live on port: ${PORT}`);
  console.log(` Environment: ${process.env.NODE_ENV || 'development'}`);
  console.log(` Database: ${dbService.isMock ? 'MEM MOCK' : 'REAL POSTGRES'}`);
  console.log(`================================================================`);
});

module.exports = { app, server };
