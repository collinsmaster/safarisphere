const express = require('express');
const router = express.Router();
const aiService = require('../services/aiService');
const dbService = require('../services/dbService');
const authMiddleware = require('../middleware/auth');

// 1. REPRODUCE CHAT CHANNELS directly with SphereMate
router.post('/chat', authMiddleware, async (req, res) => {
  const { message } = req.body;

  if (!message) {
    return res.status(400).json({ error: 'Please enter a message for SphereMate.' });
  }

  try {
    const systemMessage = "You are SphereMate, the official advanced digital guide for the Safari Sphere social workspace. Your voice is futuristic, encouraging, highly collaborative, elegant, and playful. Talk to the explorer about the wild digital savannas, their creative progress, how to discover communities, and how to stay vibing. Keep responses conversational and modern.";
    const responseText = await aiService.generateResponse(systemMessage, message);

    // Save AI interaction in database if available
    if (!dbService.isMock) {
      await dbService.query(
        'INSERT INTO ai_insights (user_id, insight_type, content) VALUES ($1, $2, $3)',
        [req.user.id, 'conversation', JSON.stringify({ user_message: message, ai_response: responseText })]
      );
    }

    res.json({
      sender: 'SphereMate AI',
      role: 'companion',
      content: responseText,
      created_at: new Date().toISOString()
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 2. REQUEST SOCIAL CAPTION GENERATOR
router.post('/suggest-caption', authMiddleware, async (req, res) => {
  const { mood, topic } = req.body;

  try {
    const suggestions = await aiService.suggestCaption(mood || 'Excited', topic || 'Exploring wildlife');
    res.json({ suggestions });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 3. COMPILE CURRENT USER VIBE SUMMARY
router.get('/vibe-summary', authMiddleware, async (req, res) => {
  try {
    let posts = [];
    let displayName = req.user.username;

    if (!dbService.isMock) {
      const postsRes = await dbService.query('SELECT content FROM posts WHERE author_id = $1 LIMIT 5', [req.user.id]);
      posts = postsRes.rows;
      const profRes = await dbService.query('SELECT display_name FROM profiles WHERE user_id = $1', [req.user.id]);
      if (profRes.rows.length > 0) displayName = profRes.rows[0].display_name;
    } else {
      const store = dbService.getMockStore();
      posts = store.posts.filter(p => p.author_id === req.user.id);
      const prof = store.profiles[req.user.id];
      if (prof) displayName = prof.display_name;
    }

    const summaryStr = await aiService.generateVibeSummary(displayName, posts);
    res.json({ vibeSummary: summaryStr });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
