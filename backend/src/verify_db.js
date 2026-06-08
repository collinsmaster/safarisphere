const { Pool } = require('pg');

// Assuming DATABASE_URL is set in environment secrets
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
});

async function verifyTables() {
  const tableNames = [
    'users', 'profiles', 'user_settings', 'follows', 'communities', 
    'community_members', 'posts', 'comments', 'likes', 'reposts', 
    'saves', 'rooms', 'room_members', 'moments', 'moment_views', 
    'chats', 'chat_members', 'messages', 'achievements', 
    'user_achievements', 'notifications', 'media_assets', 'reports', 
    'moderation_actions', 'ai_insights', 'analytics_events'
  ];

  console.log('Verifying database tables...');

  try {
    for (const tableName of tableNames) {
      const res = await pool.query(
        'SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = $1)',
        [tableName]
      );
      
      if (res.rows[0].exists) {
        console.log(`Table '${tableName}' exists.`);
      } else {
        console.error(`Table '${tableName}' is MISSING.`);
      }
    }

    // Check for posts
    const postsRes = await pool.query('SELECT * FROM posts');
    console.log(`Total posts in database: ${postsRes.rows.length}`);
    if (postsRes.rows.length > 0) {
      console.log('Sample post:', JSON.stringify(postsRes.rows[0]));
    }

    // Check for users
    const usersRes = await pool.query('SELECT COUNT(*) FROM users');
    console.log(`Total users in database: ${usersRes.rows[0].count}`);

    // Check for profiles
    const profilesRes = await pool.query('SELECT COUNT(*) FROM profiles');
    console.log(`Total profiles in database: ${profilesRes.rows[0].count}`);
  } catch (err) {
    console.error('Error during verification:', err);
  } finally {
    await pool.end();
  }
}

verifyTables();
