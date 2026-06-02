const { Pool } = require('pg');
require('dotenv').config();

const useMock = process.env.USE_MOCK_DATABASE === 'true' || !process.env.DATABASE_URL;

let pool = null;

if (!useMock) {
  try {
    const config = {
      connectionString: process.env.DATABASE_URL,
      ssl: process.env.DB_SSL === 'true' || process.env.DATABASE_URL.includes('sslmode=require') ? {
        rejectUnauthorized: false
      } : false
    };

    pool = new Pool(config);
    console.log('[PostgreSQL] Database pool initialized successfully.');

    // Auto-initialize tables inside the production Postgres database!
    setTimeout(async () => {
      try {
        const fs = require('fs');
        const path = require('path');
        const schemaPath = path.join(__dirname, '../database/schema.sql');
        if (fs.existsSync(schemaPath)) {
          console.log('[PostgreSQL Auto-Init] Ensuring database tables are fully provisioned...');
          const schemaSql = fs.readFileSync(schemaPath, 'utf8');
          // pg Client executes multiple statements separated by semicolon cleanly when called as raw text
          await pool.query(schemaSql);
          console.log('[PostgreSQL Auto-Init] Database tables and performance indexes successfully verified/created!');
        }
      } catch (schemaErr) {
        console.error('[PostgreSQL Auto-Init Error] Failed to auto-provision tables:', schemaErr.message);
      }
    }, 1000);
  } catch (err) {
    console.error('[PostgreSQL] Error initializing pool:', err.message);
    console.log('[PostgreSQL] Falling back to in-memory mock database.');
    pool = null;
  }
} else {
  console.log('[PostgreSQL] Running in MOCK mode. Safe in-memory database will be utilized.');
}

// In-Memory Database Store for Mock Mode
const mockStore = {
  users: [],
  profiles: {},
  posts: [],
  comments: {},
  rooms: [],
  moments: [],
  messages: {},
  achievements: [
    { id: "pioneer", title: "Savanna Pioneer", description: "Join Safari Sphere during our alpha phase", xp_reward: 500, category: "social" },
    { id: "chatter", title: "Vybe Talker", description: "Send 50 messages in live Vybe Rooms", xp_reward: 200, category: "chat" },
    { id: "spark", title: "Sphere Spark", description: "Create your very first live post", xp_reward: 100, category: "content" }
  ],
  user_achievements: {},
  chats: []
};

module.exports = {
  isMock: !pool,
  
  query: async (text, params) => {
    if (pool) {
      return await pool.query(text, params);
    } else {
      console.log(`[PostgreSQL Mock Query] ${text.slice(0, 100)}...`);
      return { rows: [], rowCount: 0 };
    }
  },

  getMockStore: () => mockStore
};
