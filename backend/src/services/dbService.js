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
  posts: [
    {
      id: "p1",
      author_id: "u1",
      display_name: "Safari Explorer",
      username: "explorer_prime",
      avatar_url: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
      content: "Vibe check from inside the savanna! Exploring the edge of the wilderness has never felt this alive. 🌅",
      media_url: "https://images.unsplash.com/photo-1547471080-7cc2caa01a7e?auto=format&fit=crop&w=800&q=80",
      media_type: "image",
      vibe_category: "Adventurer",
      likes_count: 34,
      comments_count: 5,
      reposts_count: 3,
      saves_count: 12,
      created_at: new Date().toISOString()
    },
    {
      id: "p2",
      author_id: "u2",
      display_name: "Luna Wilde",
      username: "luna_vibe",
      avatar_url: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&q=80",
      content: "A beautiful night under the stars in Safari Sphere. Let's make today count! ✨ #Vybes",
      media_url: null,
      media_type: "text",
      vibe_category: "Dreamer",
      likes_count: 142,
      comments_count: 23,
      reposts_count: 11,
      saves_count: 45,
      created_at: new Date().toISOString()
    }
  ],
  comments: {},
  rooms: [
    {
      id: "r1",
      host_id: "u1",
      host_name: "Safari Explorer",
      title: "Savanna Beats & Visuals",
      description: "Chill electronic beats playing live under a cosmic sky projection. Join and hang out!",
      theme: "neon-sunset",
      active_members_count: 12,
      max_members: 50,
      is_temporary: true,
      expires_at: new Date(Date.now() + 3600000).toISOString()
    },
    {
      id: "r2",
      host_id: "u2",
      host_name: "Luna Wilde",
      title: "Night Safari Chat & Stories",
      description: "Sharing creepy campfire stories from deep inside the wild. Microphone open for speakers!",
      theme: "cyber-desert",
      active_members_count: 8,
      max_members: 30,
      is_temporary: true,
      expires_at: new Date(Date.now() + 7200)
    }
  ],
  moments: [
    {
      id: "m1",
      creator_id: "u1",
      username: "explorer_prime",
      media_url: "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?auto=format&fit=crop&w=500&q=80",
      caption: "Vibrant Wildebeest Watch! 🌾",
      mood_tag: "Energetic",
      created_at: new Date().toISOString(),
      views_count: 89
    },
    {
      id: "m2",
      creator_id: "u2",
      username: "luna_vibe",
      media_url: "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=500&q=80",
      caption: "Serene Shore Lines 🌅",
      mood_tag: "Relaxing",
      created_at: new Date().toISOString(),
      views_count: 154
    }
  ],
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
