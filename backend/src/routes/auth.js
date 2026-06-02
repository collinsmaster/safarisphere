const express = require('express');
const router = express.Router();
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const dbService = require('../services/dbService');
const authMiddleware = require('../middleware/auth');

const JWT_SECRET = process.env.JWT_SECRET || 'safari_sphere_fallback_master_jwt_secret_99482';
const JWT_REFRESH_SECRET = process.env.JWT_REFRESH_SECRET || 'safari_sphere_fallback_master_refresh_secret_38291';

// Seed or helper mock user if we need to fall back
const getMockUser = (id) => {
  const store = dbService.getMockStore();
  return store.users.find(u => u.id === id);
};

// 1. SIGNUP
router.post('/signup', async (req, res) => {
  const { email, password, username, displayName } = req.body;

  if (!email || !password || !username) {
    return res.status(400).json({ error: 'Please compile all relevant fields: email, password, username.' });
  }

  try {
    const passwordHash = await bcrypt.hash(password, 10);
    const userId = `u_${Date.now()}`;

    // Under Postgres:
    if (!dbService.isMock) {
      const existingUser = await dbService.query('SELECT id FROM users WHERE email = $1 OR username = $2', [email, username]);
      if (existingUser.rows.length > 0) {
        return res.status(400).json({ error: 'Email or Username already claimed by another explorer.' });
      }

      await dbService.query(
        'INSERT INTO users (id, email, password_hash, username) VALUES ($1, $2, $3, $4)',
        [userId, email, passwordHash, username]
      );
      await dbService.query(
        'INSERT INTO profiles (user_id, display_name, bio, xp, streak_count, mood_state, mood_emoji) VALUES ($1, $2, $3, $4, $5, $6, $7)',
        [userId, displayName || username, 'A fresh pioneer in Safari Sphere!', 100, 1, 'vibe', '🦁']
      );
      await dbService.query('INSERT INTO user_settings (user_id) VALUES ($1)', [userId]);
    } else {
      // Memory Mock Save:
      const store = dbService.getMockStore();
      const userExists = store.users.some(u => u.email === email || u.username === username);
      if (userExists) {
        return res.status(400).json({ error: 'Email or Username already claimed.' });
      }

      const newUser = { id: userId, email, username, password_hash: passwordHash, role: 'user' };
      const newProfile = {
        user_id: userId,
        display_name: displayName || username,
        bio: 'A fresh pioneer in Safari Sphere!',
        avatar_url: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80',
        cover_url: '',
        location_label: 'Savana Valley',
        website: '',
        mood_state: 'Chill',
        mood_emoji: '🦁',
        profile_animation_setting: 'glow',
        xp: 100,
        streak_count: 1,
        last_active_at: new Date().toISOString()
      };

      store.users.push(newUser);
      store.profiles[userId] = newProfile;
    }

    // Launch initial tokens
    const token = jwt.sign({ id: userId, username, email, role: 'user' }, JWT_SECRET, { expiresIn: '15m' });
    const refreshToken = jwt.sign({ id: userId }, JWT_REFRESH_SECRET, { expiresIn: '7d' });

    res.status(201).json({
      message: 'Explorer registered! Welcome to the sphere.',
      token,
      refreshToken,
      user: { id: userId, username, email, displayName: displayName || username, xp: 100 }
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 2. LOGIN
router.post('/login', async (req, res) => {
  const { username, password } = req.body;

  if (!username || !password) {
    return res.status(400).json({ error: 'Please submit both credentials (username & password)' });
  }

  try {
    let user = null;
    let profile = null;

    if (!dbService.isMock) {
      const userRes = await dbService.query('SELECT * FROM users WHERE username = $1 OR email = $1', [username]);
      if (userRes.rows.length === 0) {
        return res.status(401).json({ error: 'Incorrect username/email or password.' });
      }
      user = userRes.rows[0];
      const profileRes = await dbService.query('SELECT * FROM profiles WHERE user_id = $1', [user.id]);
      profile = profileRes.rows[0];
    } else {
      const store = dbService.getMockStore();
      user = store.users.find(u => u.username === username || u.email === username);
      if (!user) {
        // Fallback or seed a fast tester account so they can play inside the mobile emulator instantly!
        user = {
          id: 'u_tester',
          username: 'savannah_lion',
          email: 'lion@sphere.io',
          password_hash: await bcrypt.hash('password123', 10),
          role: 'user'
        };
        profile = {
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
        };
        store.users.push(user);
        store.profiles[user.id] = profile;
      } else {
        profile = store.profiles[user.id];
      }
    }

    const isValidPassword = await bcrypt.compare(password, user.password_hash);
    if (!isValidPassword) {
      return res.status(401).json({ error: 'Incorrect username/email or password.' });
    }

    const token = jwt.sign({ id: user.id, username: user.username, email: user.email, role: user.role }, JWT_SECRET, { expiresIn: '15h' });
    const refreshToken = jwt.sign({ id: user.id }, JWT_REFRESH_SECRET, { expiresIn: '7d' });

    res.json({
      message: 'Safari entrance cleared!',
      token,
      refreshToken,
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
        displayName: profile?.display_name || user.username,
        avatarUrl: profile?.avatar_url,
        xp: profile?.xp || 100,
        streak_count: profile?.streak_count || 1
      }
    });

  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 3. REFRESH JWT ACCESS TOKEN
router.post('/refresh', async (req, res) => {
  const { refreshToken } = req.body;
  if (!refreshToken) {
    return res.status(400).json({ error: 'Refresh credentials required.' });
  }

  try {
    const decoded = jwt.verify(refreshToken, JWT_REFRESH_SECRET);
    const userId = decoded.id;

    let userObj = null;
    if (!dbService.isMock) {
      const userRes = await dbService.query('SELECT * FROM users WHERE id = $1', [userId]);
      if (userRes.rows.length > 0) userObj = userRes.rows[0];
    } else {
      userObj = getMockUser(userId) || { id: userId, username: 'pioneer', email: 'hello@safari.com', role: 'user' };
    }

    if (!userObj) {
      return res.status(403).json({ error: 'Explorer profile not found for this token.' });
    }

    const token = jwt.sign(
      { id: userObj.id, username: userObj.username, email: userObj.email, role: userObj.role },
      JWT_SECRET,
      { expiresIn: '15m' }
    );

    res.json({ token });
  } catch (err) {
    res.status(403).json({ error: 'Session credentials expired. Please login again.' });
  }
});

// 4. RETRIEVE PROFILE Details
router.get('/profile', authMiddleware, async (req, res) => {
  try {
    let profile = null;

    if (!dbService.isMock) {
      const profileRes = await dbService.query('SELECT p.*, u.email, u.username FROM profiles p JOIN users u ON p.user_id = u.id WHERE p.user_id = $1', [req.user.id]);
      if (profileRes.rows.length > 0) {
        profile = profileRes.rows[0];
      }
    } else {
      const store = dbService.getMockStore();
      profile = store.profiles[req.user.id];
      if (!profile) {
        profile = {
          user_id: req.user.id,
          display_name: req.user.username,
          bio: 'Enthusiastic Safari Pioneer!',
          xp: 210,
          streak_count: 3,
          location_label: 'Camp Delta',
          mood_state: 'Chilling',
          mood_emoji: '🌴',
          avatar_url: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80'
        };
        store.profiles[req.user.id] = profile;
      }
    }

    if (!profile) {
      return res.status(404).json({ error: 'Explorer profiles not initialized yet.' });
    }

    res.json(profile);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 5. UPDATE PROFILE LAYER
router.post('/profile/edit', authMiddleware, async (req, res) => {
  const { displayName, bio, avatarUrl, coverUrl, locationLabel, moodState, moodEmoji, profileAnimationSetting } = req.body;

  try {
    if (!dbService.isMock) {
      await dbService.query(
        `UPDATE profiles SET 
          display_name = COALESCE($1, display_name),
          bio = COALESCE($2, bio),
          avatar_url = COALESCE($3, avatar_url),
          cover_url = COALESCE($4, cover_url),
          location_label = COALESCE($5, location_label),
          mood_state = COALESCE($6, mood_state),
          mood_emoji = COALESCE($7, mood_emoji),
          profile_animation_setting = COALESCE($8, profile_animation_setting),
          updated_at = CURRENT_TIMESTAMP
         WHERE user_id = $9`,
        [displayName, bio, avatarUrl, coverUrl, locationLabel, moodState, moodEmoji, profileAnimationSetting, req.user.id]
      );
    } else {
      const store = dbService.getMockStore();
      const prof = store.profiles[req.user.id] || {};
      
      store.profiles[req.user.id] = {
        ...prof,
        user_id: req.user.id,
        display_name: displayName || prof.display_name,
        bio: bio !== undefined ? bio : prof.bio,
        avatar_url: avatarUrl || prof.avatar_url,
        cover_url: coverUrl || prof.cover_url,
        location_label: locationLabel || prof.location_label,
        mood_state: moodState || prof.mood_state,
        mood_emoji: moodEmoji || prof.mood_emoji,
        profile_animation_setting: profileAnimationSetting || prof.profile_animation_setting,
        updated_at: new Date().toISOString()
      };
    }

    res.json({ message: 'Identity credentials successfully synthesized!' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 6. GET ALL OTHER EXPLORERS (For Safari Chat)
router.get('/explorers', authMiddleware, async (req, res) => {
  try {
    let explorers = [];
    if (!dbService.isMock) {
      const result = await dbService.query(
        `SELECT p.user_id as id, p.display_name, p.mood_state, p.mood_emoji, p.avatar_url, p.xp, u.username 
         FROM profiles p 
         JOIN users u ON p.user_id = u.id 
         WHERE p.user_id != $1 
         ORDER BY p.xp DESC`,
        [req.user.id]
      );
      explorers = result.rows;
    } else {
      const store = dbService.getMockStore();
      explorers = Object.values(store.profiles)
        .filter(p => p.user_id !== req.user.id)
        .map(p => {
          const userObj = store.users.find(u => u.id === p.user_id) || {};
          return {
            id: p.user_id,
            display_name: p.display_name,
            mood_state: p.mood_state,
            mood_emoji: p.mood_emoji,
            avatar_url: p.avatar_url,
            xp: p.xp,
            username: userObj.username || 'unknown'
          };
        });
    }
    res.json(explorers);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
