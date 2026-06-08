const express = require('express');
const router = express.Router();
const dbService = require('../services/dbService');
const aiService = require('../services/aiService');
const authMiddleware = require('../middleware/auth');

// 1. FETCH ALL FEED POSTS WITH DISCOVERY FILTERING
router.get('/', authMiddleware, async (req, res) => {
  const { category, hashtag } = req.query;

  try {
    console.log(`[Posts] Fetching posts. Category: ${category}, Hashtag: ${hashtag}, UserId: ${req.user.id}`);
    
    let posts = [];

    if (!dbService.isMock) {
      let queryText = 'SELECT p.*, pr.display_name, pr.avatar_url, u.username, EXISTS(SELECT 1 FROM likes WHERE post_id = p.id AND user_id = $1) as has_liked FROM posts p LEFT JOIN profiles pr ON p.author_id = pr.user_id LEFT JOIN users u ON p.author_id = u.id';
      const params = [req.user.id];

      if (category) {
        queryText += ' WHERE p.vibe_category = $2';
        params.push(category);
      } else if (hashtag) {
        queryText += ' WHERE p.content ILIKE $2';
        params.push(`%#${hashtag}%`);
      }

      queryText += ' ORDER BY p.created_at DESC LIMIT 50';
      const feedRes = await dbService.query(queryText, params);
      console.log(`[Posts] DB returned ${feedRes.rows.length} posts.`);
      posts = feedRes.rows;
    } else {
      const store = dbService.getMockStore();
      let rawPosts = [...store.posts];

      if (category) {
        rawPosts = rawPosts.filter(p => p.vibe_category?.toLowerCase() === category.toLowerCase());
      }
      if (hashtag) {
        rawPosts = rawPosts.filter(p => p.content?.toLowerCase().includes(`#${hashtag.toLowerCase()}`));
      }

      posts = rawPosts.map(p => {
        const liked_by = p.liked_by || [];
        return {
          ...p,
          has_liked: liked_by.includes(req.user.id)
        };
      });
    }

    res.json(posts);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 2. CREATE A SOCIAL FEED POST WITH AI MODERATION SECURE CHECK
router.post('/', authMiddleware, async (req, res) => {
  const { content, mediaUrl, mediaType, vibeCategory, lat, lng, locationName } = req.body;

  if (!content && !mediaUrl) {
    return res.status(400).json({ error: 'Post content paragraph or media is required.' });
  }

  try {
    console.log(`[Posts] Creating post for user ${req.user.id}. Content: ${content?.substring(0, 30)}...`);

    // TRIGGER ACTUAL AI MODERATION HOOK
    if (content) {
      const moderation = await aiService.moderateContent('post', content);
      if (!moderation.isSafe) {
        return res.status(400).json({
          error: 'Content flagged as unsafe by SphereMate AI moderation filters.',
          reason: moderation.reason,
          toxicityScore: moderation.toxicityScore
        });
      }
    }

    const postId = `p_${Date.now()}`;
    const newPost = {
      id: postId,
      author_id: req.user.id,
      content: content || '',
      media_url: mediaUrl || null,
      media_type: mediaType || 'text',
      vibe_category: vibeCategory || 'General',
      lat: lat || null,
      lng: lng || null,
      location_name: locationName || null,
      likes_count: 0,
      comments_count: 0,
      reposts_count: 0,
      saves_count: 0,
      created_at: new Date().toISOString()
    };

    if (!dbService.isMock) {
      await dbService.query(
        `INSERT INTO posts 
          (id, author_id, content, media_url, media_type, vibe_category, lat, lng, location_name) 
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
        [postId, req.user.id, content || '', mediaUrl, mediaType, vibeCategory, lat, lng, locationName]
      );
    } else {
      const store = dbService.getMockStore();
      const profile = store.profiles[req.user.id] || {};
      
      const enrichedPost = {
        ...newPost,
        display_name: profile.display_name || req.user.username,
        username: req.user.username,
        avatar_url: profile.avatar_url || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80'
      };
      
      store.posts.unshift(enrichedPost);
    }

    res.status(201).json({
      message: 'Post propagated across the cosmic sphere!',
      post: newPost
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 3. TOGGLE LIKE STATUS OR COUNT
router.post('/:id/like', authMiddleware, async (req, res) => {
  const postId = req.params.id;

  try {
    if (!dbService.isMock) {
      const existingLike = await dbService.query('SELECT id FROM likes WHERE user_id = $1 AND post_id = $2', [req.user.id, postId]);
      
      if (existingLike.rows.length > 0) {
        // Unlike post
        await dbService.query('DELETE FROM likes WHERE user_id = $1 AND post_id = $2', [req.user.id, postId]);
        await dbService.query('UPDATE posts SET likes_count = GREATEST(0, likes_count - 1) WHERE id = $1', [postId]);
        return res.json({ liked: false, likes_count: 'decremented' });
      } else {
        // Like post
        await dbService.query('INSERT INTO likes (user_id, post_id) VALUES ($1, $2)', [req.user.id, postId]);
        await dbService.query('UPDATE posts SET likes_count = likes_count + 1 WHERE id = $1', [postId]);

        // Trigger notification
        const postRes = await dbService.query('SELECT author_id, content FROM posts WHERE id = $1', [postId]);
        if (postRes.rows.length > 0) {
          const postAuthor = postRes.rows[0].author_id;
          const contentPreview = postRes.rows[0].content ? postRes.rows[0].content.substring(0, 80) : '';
          
          if (postAuthor !== req.user.id) {
            const notifCheck = await dbService.query(
              'SELECT id FROM notifications WHERE receiver_id = $1 AND sender_id = $2 AND type = $3 AND target_id = $4',
              [postAuthor, req.user.id, 'like', postId]
            );
            if (notifCheck.rows.length === 0) {
              await dbService.query(
                `INSERT INTO notifications (receiver_id, sender_id, type, target_id, content_preview) 
                 VALUES ($1, $2, $3, $4, $5)`,
                [postAuthor, req.user.id, 'like', postId, contentPreview]
              );
            }
          }
        }
        return res.json({ liked: true, likes_count: 'incremented' });
      }
    } else {
      const store = dbService.getMockStore();
      const post = store.posts.find(p => p.id === postId);
      if (!post) return res.status(404).json({ error: 'Post not found in scope.' });

      post.liked_by = post.liked_by || [];
      const userIndex = post.liked_by.indexOf(req.user.id);
      if (userIndex >= 0) {
        // Unlike post
        post.liked_by.splice(userIndex, 1);
        post.likes_count = Math.max(0, post.likes_count - 1);
        return res.json({ liked: false, likes_count: post.likes_count });
      } else {
        // Like post
        post.liked_by.push(req.user.id);
        post.likes_count += 1;

        if (post.author_id !== req.user.id) {
          store.notifications = store.notifications || [];
          store.notifications.push({
            id: `n_${Date.now()}`,
            receiver_id: post.author_id,
            sender_id: req.user.id,
            type: 'like',
            target_id: postId,
            content_preview: post.content ? post.content.substring(0, 80) : '',
            is_read: false,
            created_at: new Date().toISOString()
          });
        }
        return res.json({ liked: true, likes_count: post.likes_count });
      }
    }
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 4. RETRIEVE THREADED COMMENTS
router.get('/:id/comments', async (req, res) => {
  const postId = req.params.id;

  try {
    let comments = [];

    if (!dbService.isMock) {
      const commRes = await dbService.query(
        'SELECT c.*, pr.display_name, pr.avatar_url, u.username FROM comments c JOIN profiles pr ON c.author_id = pr.user_id JOIN users u ON c.author_id = u.id WHERE c.post_id = $1 ORDER BY c.created_at ASC',
        [postId]
      );
      comments = commRes.rows;
    } else {
      const store = dbService.getMockStore();
      if (!store.comments[postId]) {
        store.comments[postId] = [
          { id: 'c1', post_id: postId, display_name: 'Pioneer Sam', username: 'sam_pioneer', content: 'Incredible shot! Pure magic here.', created_at: new Date().toISOString(), avatar_url: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80' },
          { id: 'c2', post_id: postId, display_name: 'Luna Wilde', username: 'luna_vibe', content: 'Indeed. Absolutely breathtaking.', created_at: new Date().toISOString(), avatar_url: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&q=80' }
        ];
      }
      comments = store.comments[postId];
    }

    res.json(comments);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 5. POST A COMMENT
router.post('/:id/comments', authMiddleware, async (req, res) => {
  const postId = req.params.id;
  const { content, parentId } = req.body;

  if (!content) {
    return res.status(400).json({ error: 'Blank comments are rejected.' });
  }

  try {
    const commentId = `c_${Date.now()}`;
    const commentObj = {
      id: commentId,
      post_id: postId,
      author_id: req.user.id,
      parent_id: parentId || null,
      content,
      created_at: new Date().toISOString()
    };

    if (!dbService.isMock) {
      await dbService.query(
        'INSERT INTO comments (id, post_id, author_id, parent_id, content) VALUES ($1, $2, $3, $4, $5)',
        [commentId, postId, req.user.id, parentId || null, content]
      );
      await dbService.query('UPDATE posts SET comments_count = comments_count + 1 WHERE id = $1', [postId]);

      // Create notification
      const postRes = await dbService.query('SELECT author_id FROM posts WHERE id = $1', [postId]);
      if (postRes.rows.length > 0) {
        const postAuthor = postRes.rows[0].author_id;
        if (postAuthor !== req.user.id) {
          await dbService.query(
            `INSERT INTO notifications (receiver_id, sender_id, type, target_id, content_preview) 
             VALUES ($1, $2, $3, $4, $5)`,
            [postAuthor, req.user.id, 'comment', postId, content.substring(0, 85)]
          );
        }
      }
    } else {
      const store = dbService.getMockStore();
      const profile = store.profiles[req.user.id] || {};
      
      const enrichedComment = {
        ...commentObj,
        display_name: profile.display_name || req.user.username,
        username: req.user.username,
        avatar_url: profile.avatar_url || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80'
      };

      if (!store.comments[postId]) {
        store.comments[postId] = [
          { id: 'c1', post_id: postId, display_name: 'Pioneer Sam', username: 'sam_pioneer', content: 'Incredible shot! Pure magic here.', created_at: new Date().toISOString(), avatar_url: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80' },
          { id: 'c2', post_id: postId, display_name: 'Luna Wilde', username: 'luna_vibe', content: 'Indeed. Absolutely breathtaking.', created_at: new Date().toISOString(), avatar_url: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&q=80' }
        ];
      }
      store.comments[postId].push(enrichedComment);
      
      const post = store.posts.find(p => p.id === postId);
      if (post) {
        post.comments_count += 1;

        if (post.author_id !== req.user.id) {
          store.notifications = store.notifications || [];
          store.notifications.push({
            id: `n_${Date.now()}`,
            receiver_id: post.author_id,
            sender_id: req.user.id,
            type: 'comment',
            target_id: postId,
            content_preview: content.substring(0, 85),
            is_read: false,
            created_at: new Date().toISOString()
          });
        }
      }
    }

    res.status(201).json({
      message: 'Comment published!',
      comment: commentObj
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
