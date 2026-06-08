const express = require('express');
const router = express.Router();
const dbService = require('../services/dbService');
const authMiddleware = require('../middleware/auth');
const { v4: uuidv4 } = require('uuid');

// 1. GET ALL DIRECT MESSAGE THREADS
router.get('/', authMiddleware, async (req, res) => {
  try {
    let chats = [];
    if (!dbService.isMock) {
      const chatsRes = await dbService.query(
        `SELECT c.*, cm2.user_id as peer_id, pr.display_name as peer_name, pr.avatar_url as peer_avatar 
         FROM chats c 
         JOIN chat_members cm1 ON c.id = cm1.chat_id 
         JOIN chat_members cm2 ON c.id = cm2.chat_id AND cm2.user_id != $1
         JOIN profiles pr ON cm2.user_id = pr.user_id
         WHERE cm1.user_id = $1`,
        [req.user.id]
      );
      chats = chatsRes.rows;
    } else {
      const store = dbService.getMockStore();
      chats = store.chats.filter(c => c.members?.includes(req.user.id));
    }
    res.json(chats);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 2. INITIATE A CONVERSATION THREAD
router.post('/', authMiddleware, async (req, res) => {
  const { recipientId, isGroup, groupName } = req.body;

  if (!recipientId && !isGroup) {
    return res.status(400).json({ error: 'Please choose a chat recipient or initiate a dynamic group.' });
  }

  try {
    const chatId = uuidv4();

    if (!dbService.isMock) {
      // Create chat thread
      await dbService.query(
        'INSERT INTO chats (id, is_group, group_name) VALUES ($1, $2, $3)',
        [chatId, !!isGroup, groupName || null]
      );

      // Join current user
      await dbService.query(
        'INSERT INTO chat_members (chat_id, user_id) VALUES ($1, $2)',
        [chatId, req.user.id]
      );

      // Join recipient if peer chat
      if (recipientId) {
        await dbService.query(
          'INSERT INTO chat_members (chat_id, user_id) VALUES ($1, $2)',
          [chatId, recipientId]
        );
      }
    } else {
      const store = dbService.getMockStore();
      const profile = store.profiles[recipientId] || {};
      
      const newChat = {
        id: chatId,
        is_group: !!isGroup,
        group_name: groupName || null,
        peer_name: profile.display_name || 'Safari Explorer',
        peer_avatar: profile.avatar_url || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80',
        members: [req.user.id, recipientId],
        created_at: new Date().toISOString()
      };
      store.chats.push(newChat);
    }

    res.status(201).json({
      message: 'Secure chat pipeline initiated.',
      chatId,
      peer_id: recipientId
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 3. FETCH MESSAGE HISTORY IN A THREAD
router.get('/:id', authMiddleware, async (req, res) => {
  const chatId = req.params.id;

  try {
    let messages = [];

    if (!dbService.isMock) {
      const msgRes = await dbService.query(
        `SELECT m.*, pr.display_name as sender_name, pr.avatar_url as sender_avatar 
         FROM messages m 
         JOIN profiles pr ON m.sender_id = pr.user_id 
         WHERE m.chat_id = $1 
         ORDER BY m.created_at ASC`,
        [chatId]
      );
      messages = msgRes.rows;
    } else {
      const store = dbService.getMockStore();
      messages = store.messages[chatId] || [
        { id: 'm_seed1', chat_id: chatId, sender_id: req.user.id, sender_name: 'You', content: 'Safari check! Connecting from the camp. 🦁', created_at: new Date().toISOString() }
      ];
    }

    res.json(messages);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 4. SEND DIRECT MESSAGE
router.post('/:id', authMiddleware, async (req, res) => {
  const chatId = req.params.id;
  const { content, attachmentUrl, attachmentType, e2eeEncrypted } = req.body;

  if (!content && !attachmentUrl) {
    return res.status(400).json({ error: 'Cannot send blank messages.' });
  }

  try {
    const messageId = uuidv4();
    const newMsg = {
      id: messageId,
      chat_id: chatId,
      sender_id: req.user.id,
      content,
      attachment_url: attachmentUrl || null,
      attachment_type: attachmentType || null,
      e2ee_encrypted: !!e2eeEncrypted,
      created_at: new Date().toISOString(),
      is_read: false
    };

    if (!dbService.isMock) {
      await dbService.query(
        `INSERT INTO messages 
          (id, chat_id, sender_id, content, attachment_url, attachment_type, e2ee_encrypted) 
         VALUES ($1, $2, $3, $4, $5, $6, $7)`,
        [messageId, chatId, req.user.id, content, attachmentUrl || null, attachmentType || null, !!e2eeEncrypted]
      );
    } else {
      const store = dbService.getMockStore();
      const profile = store.profiles[req.user.id] || {};
      
      const enrichedMsg = {
        ...newMsg,
        sender_name: profile.display_name || req.user.username,
        sender_avatar: profile.avatar_url || ''
      };

      if (!store.messages[chatId]) store.messages[chatId] = [];
      store.messages[chatId].push(enrichedMsg);
    }

    // Capture express server socket interface and dispatch real-time events
    const io = req.app.get('io');
    if (io) {
      io.to(chatId).emit('chat:message_received', newMsg);
    }

    res.status(201).json({
      message: 'Direct dispatch sent.',
      msg: newMsg
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
