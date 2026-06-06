const express = require('express');
const router = express.Router();
const dbService = require('../services/dbService');
const authMiddleware = require('../middleware/auth');

// Fetch user's notifications
router.get('/', authMiddleware, async (req, res) => {
  try {
    let notifications = [];

    if (!dbService.isMock) {
      const result = await dbService.query(
        `SELECT n.*, u.username as sender_username, p.display_name as sender_display_name, p.avatar_url as sender_avatar_url,
                post.content as post_content, post.media_url as post_media_url, post.media_type as post_media_type
         FROM notifications n
         LEFT JOIN users u ON n.sender_id = u.id
         LEFT JOIN profiles p ON n.sender_id = p.user_id
         LEFT JOIN posts post ON n.target_id = post.id
         WHERE n.receiver_id = $1
         ORDER BY n.created_at DESC LIMIT 50`,
        [req.user.id]
      );
      notifications = result.rows;
    } else {
      const store = dbService.getMockStore();
      store.notifications = store.notifications || [];
      const userNotifications = store.notifications.filter(n => n.receiver_id === req.user.id);
      
      notifications = userNotifications.map(n => {
        const senderUser = store.users.find(u => u.id === n.sender_id) || {};
        const senderProfile = store.profiles[n.sender_id] || {};
        const post = store.posts.find(p => p.id === n.target_id) || {};
        
        return {
          ...n,
          sender_username: senderUser.username || 'pioneer',
          sender_display_name: senderProfile.display_name || 'Safari Pioneer',
          sender_avatar_url: senderProfile.avatar_url || '',
          post_content: post.content || '',
          post_media_url: post.media_url || '',
          post_media_type: post.media_type || 'text'
        };
      });
      // Sort by created_at desc
      notifications.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));
    }

    res.json(notifications);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Mark all as read
router.post('/mark-read', authMiddleware, async (req, res) => {
  try {
    if (!dbService.isMock) {
      await dbService.query(
        'UPDATE notifications SET is_read = TRUE WHERE receiver_id = $1',
        [req.user.id]
      );
    } else {
      const store = dbService.getMockStore();
      store.notifications = store.notifications || [];
      store.notifications.forEach(n => {
        if (n.receiver_id === req.user.id) {
          n.is_read = true;
        }
      });
    }
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
