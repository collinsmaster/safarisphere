const express = require('express');
const router = express.Router();
const dbService = require('../services/dbService');
const authMiddleware = require('../middleware/auth');

// 1. GET ALL COMMUNITIES
router.get('/', async (req, res) => {
  try {
    let communities = [];
    if (!dbService.isMock) {
      const commRes = await dbService.query('SELECT * FROM communities ORDER BY member_count DESC');
      communities = commRes.rows;
    } else {
      communities = [
        { id: 'com1', name: 'Savannah Photography 📷', handle: 'nature_lenses', description: 'Showcasing high resolution wild animal visuals and lighting grids.', member_count: 142 , banner_url: "https://images.unsplash.com/photo-1547471080-7cc2caa01a7e?auto=format&fit=crop&w=800&q=80"},
        { id: 'com2', name: 'Ambient Synthesis 🎹', handle: 'vibe_frequencies', description: 'Exchanging sound design models, chillwave loops, and campfire live synthesizers.', member_count: 89 }
      ];
    }
    res.json(communities);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 2. CREATE A COMMUNITY
router.post('/', authMiddleware, async (req, res) => {
  const { name, handle, description, category, isPrivate } = req.body;

  if (!name || !handle) {
    return res.status(400).json({ error: 'Please submit both community name and distinct handle.' });
  }

  try {
    const communityId = `com_${Date.now()}`;
    const newComm = {
      id: communityId,
      creator_id: req.user.id,
      name,
      handle,
      description: description || 'No description supplied.',
      category: category || 'General',
      is_private: !!isPrivate,
      member_count: 1,
      created_at: new Date().toISOString()
    };

    if (!dbService.isMock) {
      await dbService.query(
        'INSERT INTO communities (id, creator_id, name, handle, description, category, is_private) VALUES ($1, $2, $3, $4, $5, $6, $7)',
        [communityId, req.user.id, name, handle, description, category, !!isPrivate]
      );
      await dbService.query(
        'INSERT INTO community_members (community_id, user_id, role) VALUES ($1, $2, $3)',
        [communityId, req.user.id, 'creator']
      );
    }

    res.status(201).json({
      message: 'New digital ecosystem carved out! Invites ready.',
      community: newComm
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 3. JOIN A COMMUNITY
router.post('/:id/join', authMiddleware, async (req, res) => {
  const communityId = req.params.id;

  try {
    if (!dbService.isMock) {
      await dbService.query(
        'INSERT INTO community_members (community_id, user_id, role) VALUES ($1, $2, $3) ON CONFLICT (community_id, user_id) DO NOTHING',
        [communityId, req.user.id, 'member']
      );
      await dbService.query('UPDATE communities SET member_count = member_count + 1 WHERE id = $1', [communityId]);
    }
    res.json({ message: 'Success! Registered in community.', communityId });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
