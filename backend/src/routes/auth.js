const express = require('express');
const router = express.Router();
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const dbService = require('../services/dbService');
const authMiddleware = require('../middleware/auth');
const otpService = require('../services/otpService');

const JWT_SECRET = process.env.JWT_SECRET || 'safari_sphere_fallback_master_jwt_secret_99482';
const JWT_REFRESH_SECRET = process.env.JWT_REFRESH_SECRET || 'safari_sphere_fallback_master_refresh_secret_38291';

// Seed or helper mock user if we need to fall back
const getMockUser = (id) => {
  const store = dbService.getMockStore();
  return store.users.find(u => u.id === id);
};

// 0. CHECK USERNAME AVAILABILITY AND VALIDITY
router.get('/check-username', async (req, res) => {
  const { username } = req.query;
  if (!username) {
    return res.status(400).json({ available: false, error: 'Username parameter required.' });
  }

  const trimmed = username.trim();

  if (trimmed.length < 3) {
    return res.json({ available: false, error: 'Username must be at least 3 characters.' });
  }
  if (trimmed.length > 16) {
    return res.json({ available: false, error: 'Username must be 16 characters or less.' });
  }
  if (trimmed.includes(' ')) {
    return res.json({ available: false, error: 'Username must not contain spaces.' });
  }
  const allowedRegex = /^[a-zA-Z0-9_.]+$/;
  if (!allowedRegex.test(trimmed)) {
    return res.json({ available: false, error: 'Only letters, numbers, _ and . are allowed.' });
  }
  if (trimmed.startsWith('.')) {
    return res.json({ available: false, error: 'Username cannot start with a period.' });
  }
  if (trimmed.endsWith('.')) {
    return res.json({ available: false, error: 'Username cannot end with a period.' });
  }
  if (trimmed.includes('..')) {
    return res.json({ available: false, error: 'Username cannot contain consecutive periods.' });
  }

  try {
    if (!dbService.isMock) {
      const result = await dbService.query('SELECT id FROM users WHERE LOWER(username) = LOWER($1)', [trimmed]);
      if (result.rows.length > 0) {
        return res.json({ available: false, error: 'Username is already taken.' });
      }
    } else {
      const store = dbService.getMockStore();
      const exists = store.users.some(u => u.username.toLowerCase() === trimmed.toLowerCase());
      if (exists) {
        return res.json({ available: false, error: 'Username is already taken.' });
      }
    }
    return res.json({ available: true });
  } catch (err) {
    return res.status(500).json({ available: false, error: err.message });
  }
});

// 0.5 CHECK EMAIL AVAILABILITY AND VALIDITY
router.get('/check-email', async (req, res) => {
  const { email } = req.query;
  if (!email) {
    return res.status(400).json({ available: false, error: 'Email parameter required.' });
  }

  const trimmed = email.trim().toLowerCase();

  if (!trimmed.includes('@')) {
    return res.json({ available: false, error: 'Email must contain @ character.' });
  }

  if (trimmed.includes('@gmail.')) {
    if (!trimmed.endsWith('@gmail.com') && !trimmed.split('@')[1].endsWith('.com')) {
      return res.json({ available: false, error: 'Gmail address must end with .com' });
    }
  }

  // Basic regex check
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(trimmed)) {
    return res.json({ available: false, error: 'Invalid email address format.' });
  }

  try {
    if (!dbService.isMock) {
      const result = await dbService.query('SELECT id FROM users WHERE LOWER(email) = LOWER($1)', [trimmed]);
      if (result.rows.length > 0) {
        return res.json({ available: false, error: 'Email is already registered.' });
      }
    } else {
      const store = dbService.getMockStore();
      const exists = store.users.some(u => u.email.toLowerCase() === trimmed.toLowerCase());
      if (exists) {
        return res.json({ available: false, error: 'Email is already registered.' });
      }
    }
    return res.json({ available: true });
  } catch (err) {
    return res.status(500).json({ available: false, error: err.message });
  }
});

// 1. SIGNUP WITH STRICT USERNAME VALIDATION & OTP VERIFICATION
router.post('/signup', async (req, res) => {
  const { email, password, username, displayName, otp } = req.body;

  if (!email || !password || !username) {
    return res.status(400).json({ error: 'Please compile all relevant fields: email, password, username.' });
  }

  const trimmedUsername = username.trim();
  const trimmedEmail = email.trim().toLowerCase();

  // Username validation checks:
  if (trimmedUsername.length < 3 || trimmedUsername.length > 16) {
    return res.status(400).json({ error: 'Username must be between 3 and 16 characters.' });
  }
  if (trimmedUsername.includes(' ')) {
    return res.status(400).json({ error: 'Username must not contain spaces.' });
  }
  // Allow alphanumeric, underscores, and periods
  const allowedRegex = /^[a-zA-Z0-9_.]+$/;
  if (!allowedRegex.test(trimmedUsername)) {
    return res.status(400).json({ error: 'Username can only contain alphanumeric characters, underscores (_), and periods (.).' });
  }
  if (trimmedUsername.startsWith('.')) {
    return res.status(400).json({ error: 'Username must not start with a period.' });
  }
  if (trimmedUsername.endsWith('.')) {
    return res.status(400).json({ error: 'Username must not end with a period.' });
  }

  try {
    // Phase 1 check: uniqueness of email and username
    if (!dbService.isMock) {
      const existingUser = await dbService.query(
        'SELECT id, username, email FROM users WHERE email = $1 OR username = $2',
        [trimmedEmail, trimmedUsername]
      );
      if (existingUser.rows.length > 0) {
        const found = existingUser.rows[0];
        if (found.email.toLowerCase() === trimmedEmail) {
          return res.status(400).json({ error: 'Email already claimed by another explorer.' });
        }
        return res.status(400).json({ error: 'Username already taken. Please try another handle.' });
      }
    } else {
      const store = dbService.getMockStore();
      const emailExists = store.users.some(u => u.email.toLowerCase() === trimmedEmail);
      if (emailExists) {
        return res.status(400).json({ error: 'Email already claimed.' });
      }
      const usernameExists = store.users.some(u => u.username.toLowerCase() === trimmedUsername.toLowerCase());
      if (usernameExists) {
        return res.status(400).json({ error: 'Username already taken.' });
      }
    }

    // Step 2: OTP dispatch (if otp is missing)
    if (!otp) {
      console.log(`[Signup Flow] Registering pre-validation for ${trimmedEmail}. Issuing OTP...`);
      const otpResponse = await otpService.sendOTP(trimmedEmail);
      return res.status(200).json({
        requiresOtp: true,
        message: otpResponse.message,
        debugOtp: otpResponse.debugOtp || null
      });
    }

    // Step 3: Verified registration (if otp is supplied)
    const isOtpValid = await otpService.verifyOTP(trimmedEmail, otp);
    if (!isOtpValid) {
      return res.status(400).json({ error: 'Invalid or expired verification OTP. Please verify and try again.' });
    }

    // OTP succeeded! Produce account records
    const passwordHash = await bcrypt.hash(password, 10);
    let userId;

    if (!dbService.isMock) {
      // Postgres auto-generates UUID or we can generate one to ensure compatibility
      const { v4: uuidv4 } = require('uuid');
      userId = uuidv4();

      await dbService.query(
        'INSERT INTO users (id, email, password_hash, username) VALUES ($1, $2, $3, $4)',
        [userId, trimmedEmail, passwordHash, trimmedUsername]
      );
      await dbService.query(
        'INSERT INTO profiles (user_id, display_name, bio, xp, streak_count, mood_state, mood_emoji) VALUES ($1, $2, $3, $4, $5, $6, $7)',
        [userId, displayName || trimmedUsername, 'A fresh pioneer in Safari Sphere!', 100, 1, 'vibe', '🦁']
      );
      await dbService.query('INSERT INTO user_settings (user_id) VALUES ($1)', [userId]);
    } else {
      userId = `u_${Date.now()}`;
      const store = dbService.getMockStore();
      const newUser = { id: userId, email: trimmedEmail, username: trimmedUsername, password_hash: passwordHash, role: 'user' };
      const newProfile = {
        user_id: userId,
        display_name: displayName || trimmedUsername,
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

    // Launch active tokens
    const token = jwt.sign({ id: userId, username: trimmedUsername, email: trimmedEmail, role: 'user' }, JWT_SECRET, { expiresIn: '15h' });
    const refreshToken = jwt.sign({ id: userId }, JWT_REFRESH_SECRET, { expiresIn: '7d' });

    res.status(201).json({
      message: 'Explorer registered! Welcome to the sphere.',
      token,
      refreshToken,
      user: { id: userId, username: trimmedUsername, email: trimmedEmail, displayName: displayName || trimmedUsername, xp: 100 }
    });
  } catch (err) {
    console.error('[Signup Exception]', err);
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
      const profileRes = await dbService.query('SELECT p.*, u.email, u.username, u.created_at FROM profiles p JOIN users u ON p.user_id = u.id WHERE p.user_id = $1', [req.user.id]);
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
          avatar_url: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80',
          created_at: new Date().toISOString()
        };
        store.profiles[req.user.id] = profile;
      } else if (!profile.created_at) {
        profile.created_at = new Date().toISOString();
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
  const { 
    displayName, bio, avatarUrl, coverUrl, locationLabel, 
    moodState, moodEmoji, profileAnimationSetting,
    email, username, password, otp 
  } = req.body;

  try {
    let currentUserObj = null;
    let currentProfileObj = null;

    if (!dbService.isMock) {
      const userRes = await dbService.query('SELECT * FROM users WHERE id = $1', [req.user.id]);
      if (userRes.rows.length === 0) {
        return res.status(404).json({ error: 'User account not found.' });
      }
      currentUserObj = userRes.rows[0];

      const profileRes = await dbService.query('SELECT * FROM profiles WHERE user_id = $1', [req.user.id]);
      currentProfileObj = profileRes.rows[0] || {};
    } else {
      const store = dbService.getMockStore();
      currentUserObj = store.users.find(u => u.id === req.user.id);
      if (!currentUserObj) {
        return res.status(404).json({ error: 'User account not found.' });
      }
      currentProfileObj = store.profiles[req.user.id] || {};
    }

    // 1. Email verification trigger
    let targetEmail = currentUserObj.email;
    if (email && email.trim().toLowerCase() !== currentUserObj.email.toLowerCase()) {
      targetEmail = email.trim().toLowerCase();
      
      // Check email uniqueness
      if (!dbService.isMock) {
        const emailUniqueCheck = await dbService.query('SELECT id FROM users WHERE email = $1 AND id != $2', [targetEmail, req.user.id]);
        if (emailUniqueCheck.rows.length > 0) {
          return res.status(400).json({ error: 'This email is already claimed by another explorer.' });
        }
      } else {
        const store = dbService.getMockStore();
        const emailExists = store.users.some(u => u.id !== req.user.id && u.email.toLowerCase() === targetEmail);
        if (emailExists) {
          return res.status(400).json({ error: 'This email is already claimed by another explorer.' });
        }
      }

      // OTP Verification Gate
      if (!otp) {
        console.log(`[Profile Edit] Email change requested for user ${req.user.id} to ${targetEmail}. Issuing verification OTP...`);
        const otpResponse = await otpService.sendOTP(targetEmail);
        return res.status(200).json({
          requiresOtp: true,
          message: `Verification code sent to your new email ${targetEmail}!`,
          debugOtp: otpResponse.debugOtp || null
        });
      }

      // Verify OTP
      const isOtpValid = await otpService.verifyOTP(targetEmail, otp);
      if (!isOtpValid) {
        return res.status(400).json({ error: 'Invalid or expired verification OTP. Please verify and try again.' });
      }
    }

    // 2. Username change check & rules
    let targetUsername = currentUserObj.username;
    if (username && username.trim() !== currentUserObj.username) {
      targetUsername = username.trim();

      // Username constraints:
      if (targetUsername.length < 3 || targetUsername.length > 16) {
        return res.status(400).json({ error: 'Username must be between 3 and 16 characters.' });
      }
      if (targetUsername.includes(' ')) {
        return res.status(400).json({ error: 'Username must not contain spaces.' });
      }
      const allowedRegex = /^[a-zA-Z0-9_.]+$/;
      if (!allowedRegex.test(targetUsername)) {
        return res.status(400).json({ error: 'Username can only contain alphanumeric characters, underscores (_), and periods (.).' });
      }
      if (targetUsername.startsWith('.')) {
        return res.status(400).json({ error: 'Username must not start with a period.' });
      }
      if (targetUsername.endsWith('.')) {
        return res.status(400).json({ error: 'Username must not end with a period.' });
      }

      // Check username uniqueness
      if (!dbService.isMock) {
        const usernameUniqueCheck = await dbService.query('SELECT id FROM users WHERE username = $1 AND id != $2', [targetUsername, req.user.id]);
        if (usernameUniqueCheck.rows.length > 0) {
          return res.status(400).json({ error: 'Username already taken. Please choose another handle.' });
        }
      } else {
        const store = dbService.getMockStore();
        const usernameExists = store.users.some(u => u.id !== req.user.id && u.username.toLowerCase() === targetUsername.toLowerCase());
        if (usernameExists) {
          return res.status(400).json({ error: 'Username already taken. Please choose another handle.' });
        }
      }
    }

    // 3. Password hashing if updated
    let targetPasswordHash = currentUserObj.password_hash;
    if (password && password.trim().length > 0) {
      if (password.length < 6) {
        return res.status(400).json({ error: 'Password must be at least 6 characters long.' });
      }
      targetPasswordHash = await bcrypt.hash(password, 10);
    }

    // 4. Update operations on database
    if (!dbService.isMock) {
      // Begin transaction to update both tables
      await dbService.query('BEGIN');
      try {
        await dbService.query(
          `UPDATE users SET 
            email = $1, 
            username = $2, 
            password_hash = $3,
            updated_at = CURRENT_TIMESTAMP 
           WHERE id = $4`,
          [targetEmail, targetUsername, targetPasswordHash, req.user.id]
        );

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
          [
            displayName, 
            bio, 
            avatarUrl, 
            coverUrl, 
            locationLabel, 
            moodState, 
            moodEmoji, 
            profileAnimationSetting, 
            req.user.id
          ]
        );
        await dbService.query('COMMIT');
      } catch (txnErr) {
        await dbService.query('ROLLBACK');
        throw txnErr;
      }
    } else {
      const store = dbService.getMockStore();
      
      // Update User object
      currentUserObj.email = targetEmail;
      currentUserObj.username = targetUsername;
      currentUserObj.password_hash = targetPasswordHash;

      // Update Profile object
      store.profiles[req.user.id] = {
        ...currentProfileObj,
        user_id: req.user.id,
        display_name: displayName !== undefined ? displayName : currentProfileObj.display_name,
        bio: bio !== undefined ? bio : currentProfileObj.bio,
        avatar_url: avatarUrl || currentProfileObj.avatar_url,
        cover_url: coverUrl || currentProfileObj.cover_url,
        location_label: locationLabel !== undefined ? locationLabel : currentProfileObj.location_label,
        mood_state: moodState || currentProfileObj.mood_state,
        mood_emoji: moodEmoji || currentProfileObj.mood_emoji,
        profile_animation_setting: profileAnimationSetting || currentProfileObj.profile_animation_setting,
        updated_at: new Date().toISOString()
      };
    }

    res.json({ 
      success: true,
      message: 'Identity credentials successfully synthesized!',
      user: {
        id: req.user.id,
        email: targetEmail,
        username: targetUsername,
        displayName: displayName || (currentProfileObj && currentProfileObj.display_name) || targetUsername
      }
    });
  } catch (err) {
    console.error('[Profile Edit Exception]', err);
    res.status(500).json({ error: err.message });
  }
});

// 5.5 DELETE ACCOUNT ROUTE
router.post('/account/delete', authMiddleware, async (req, res) => {
  try {
    if (!dbService.isMock) {
      await dbService.query('DELETE FROM users WHERE id = $1', [req.user.id]);
      console.log(`[Delete Account] Cleaned account ID ${req.user.id} from database.`);
    } else {
      const store = dbService.getMockStore();
      delete store.profiles[req.user.id];
      delete store.messages[req.user.id];
      store.users = store.users.filter(u => u.id !== req.user.id);
      console.log(`[Delete Account] Cleaned account ID ${req.user.id} from memory.`);
    }
    res.json({ success: true, message: 'Your explorer registration has been successfully purged.' });
  } catch (err) {
    console.error('[Delete Account Exception]', err);
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
