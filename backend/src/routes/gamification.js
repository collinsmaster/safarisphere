const express = require('express');
const router = express.Router();
const dbService = require('../services/dbService');
const authMiddleware = require('../middleware/auth');

// 1. GET ALL AVAILABLE SYSTEM ACHIEVEMENTS & CURRENT USER UNLOCKED
router.get('/achievements', authMiddleware, async (req, res) => {
  try {
    let achievements = [];
    let unlockedIds = [];

    if (!dbService.isMock) {
      const achRes = await dbService.query('SELECT * FROM achievements');
      achievements = achRes.rows;

      const userAchRes = await dbService.query('SELECT achievement_id FROM user_achievements WHERE user_id = $1', [req.user.id]);
      unlockedIds = userAchRes.rows.map(r => r.achievement_id);
    } else {
      const store = dbService.getMockStore();
      achievements = store.achievements;
      unlockedIds = store.user_achievements[req.user.id] || ['pioneer'];
    }

    const merged = achievements.map(a => ({
      ...a,
      isUnlocked: unlockedIds.includes(a.id)
    }));

    res.json(merged);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 2. AWARD XP FOR USER ENGAGEMENT
router.post('/add-xp', authMiddleware, async (req, res) => {
  const { xpAmount, reason } = req.body;

  if (!xpAmount || xpAmount <= 0) {
    return res.status(400).json({ error: 'Please submit a positive integer for XP rewards.' });
  }

  try {
    let currentXp = 0;

    if (!dbService.isMock) {
      const profileRes = await dbService.query('UPDATE profiles SET xp = xp + $1 WHERE user_id = $2 RETURNING xp', [xpAmount, req.user.id]);
      if (profileRes.rows.length > 0) currentXp = profileRes.rows[0].xp;
      
      await dbService.query(
        'INSERT INTO analytics_events (user_id, event_type, xp_gained, payload) VALUES ($1, $2, $3, $4)',
        [req.user.id, 'xp_earned', xpAmount, JSON.stringify({ reason })]
      );
    } else {
      const store = dbService.getMockStore();
      const profile = store.profiles[req.user.id] || { xp: 100 };
      profile.xp = (profile.xp || 100) + xpAmount;
      currentXp = profile.xp;
    }

    res.json({
      message: `Success! +${xpAmount} XP earned for: "${reason || 'Engaging with the Sphere'}"`,
      currentXp
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 3. GENERATE LEADERBOARD SCORES
router.get('/leaderboard', async (req, res) => {
  try {
    let leaderboard = [];

    if (!dbService.isMock) {
      const leadRes = await dbService.query(
        'SELECT p.user_id, p.display_name, p.avatar_url, p.xp, u.username FROM profiles p JOIN users u ON p.user_id = u.id ORDER BY p.xp DESC LIMIT 20'
      );
      leaderboard = leadRes.rows;
    } else {
      const store = dbService.getMockStore();
      // Generate some mock leaderboard entries
      leaderboard = Object.values(store.profiles).map(pro => {
        const matchingUser = store.users.find(u => u.id === pro.user_id);
        return {
          user_id: pro.user_id,
          display_name: pro.display_name,
          avatar_url: pro.avatar_url,
          xp: pro.xp,
          username: matchingUser ? matchingUser.username : 'explorer_prime'
        };
      }).sort((a,b) => b.xp - a.xp);
    }

    res.json(leaderboard);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
