const { Pool } = require('pg');
require('dotenv').config();

const useMock = process.env.USE_MOCK_DATABASE === 'true' || !process.env.DATABASE_URL;

let pool = null;

if (!useMock) {
  try {
    const config = {
      connectionString: process.env.DATABASE_URL,
      ssl: {
        rejectUnauthorized: false
      }
    };

    pool = new Pool(config);
    console.log('[PostgreSQL] Database pool initialized successfully.');

    // Add logging to catch failures
    pool.on('error', (err, client) => {
      console.error('[PostgreSQL] Unexpected error on idle client', err);
    });

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
    console.error('[PostgreSQL] Error initializing pool:', err); // Log the full error object
    console.log('[PostgreSQL] Falling back to in-memory mock database.');
    pool = null;
  }
} else {
  console.log('[PostgreSQL] Running in MOCK mode. Safe in-memory database will be utilized.');
}

// In-Memory Database Store for Mock Mode
const bcrypt = require('bcryptjs');
const mockStore = {
  users: [
    {
      id: 'u_tester',
      username: 'savannah_lion',
      email: 'lion@sphere.io',
      password_hash: bcrypt.hashSync('password123', 10),
      role: 'user'
    }
  ],
  profiles: {
    'u_tester': {
      user_id: 'u_tester',
      display_name: 'Savannah Lion 🦁',
      bio: 'Roaming the cosmic savannas in search of tech gold.',
      avatar_url: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80',
      cover_url: '',
      location_label: 'Delta Sector',
      website: 'https://safarisphere.app',
      mood_state: 'Vibing',
      mood_emoji: '🦁',
      profile_animation_setting: 'ripple',
      xp: 450,
      streak_count: 5
    }
  },
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
