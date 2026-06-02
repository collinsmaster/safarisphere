const express = require('express');
const router = express.Router();
const dbService = require('../services/dbService');
const authMiddleware = require('../middleware/auth');

// 1. GET ACTIVE MOMENTS (STORY BARS)
router.get('/', async (req, res) => {
  try {
    let moments = [];
    if (!dbService.isMock) {
      const momentRes = await dbService.query(
        'SELECT m.*, u.username FROM moments m JOIN users u ON m.creator_id = u.id WHERE m.expires_at > CURRENT_TIMESTAMP ORDER BY m.created_at DESC'
      );
      moments = momentRes.rows;
    } else {
      moments = dbService.getMockStore().moments;
    }
    res.json(moments);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 2. CREATE A MOMENT
router.post('/', authMiddleware, async (req, res) => {
  const { mediaUrl, mediaType, caption, moodTag } = req.body;

  if (!mediaUrl) {
    return res.status(400).json({ error: 'Please submit a media URL (image/video/mood canvas).' });
  }

  try {
    const momentId = `m_${Date.now()}`;
    const expiresAt = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(); // 24 hours from now

    const newMoment = {
      id: momentId,
      creator_id: req.user.id,
      media_url: mediaUrl,
      media_type: mediaType || 'image',
      caption: caption || '',
      mood_tag: moodTag || 'Calm',
      expires_at: expiresAt,
      created_at: new Date().toISOString(),
      views_count: 0
    };

    if (!dbService.isMock) {
      await dbService.query(
        'INSERT INTO moments (id, creator_id, media_url, media_type, caption, mood_tag, expires_at) VALUES ($1, $2, $3, $4, $5, $6, $7)',
        [momentId, req.user.id, mediaUrl, mediaType || 'image', caption || '', moodTag || 'Calm', expiresAt]
      );
    } else {
      const store = dbService.getMockStore();
      const enrichedMoment = {
        ...newMoment,
        username: req.user.username
      };
      store.moments.unshift(enrichedMoment);
    }

    res.status(201).json({
      message: 'Moment posted to the horizon! It will survive 24 hours.',
      moment: newMoment
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 3. TRACK A MOMENT VIEW OR REACTION
router.post('/:id/view', authMiddleware, async (req, res) => {
  const momentId = req.params.id;
  const { reactionEmoji } = req.body;

  try {
    if (!dbService.isMock) {
      await dbService.query(
        'INSERT INTO moment_views (moment_id, viewer_id, reaction_emoji) VALUES ($1, $2, $3) ON CONFLICT (moment_id, viewer_id) DO UPDATE SET reaction_emoji = EXCLUDED.reaction_emoji',
        [momentId, req.user.id, reactionEmoji || null]
      );
      await dbService.query('UPDATE moments SET views_count = views_count + 1 WHERE id = $1', [momentId]);
    } else {
      const store = dbService.getMockStore();
      const mom = store.moments.find(m => m.id === momentId);
      if (mom) {
        mom.views_count += 1;
      }
    }
    res.json({ message: 'Moment view registered!', momentId });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
