const express = require('express');
const router = express.Router();
const dbService = require('../services/dbService');
const authMiddleware = require('../middleware/auth');

// 1. GET ALL ACTIVE LIVE VYBE ROOMS
router.get('/', async (req, res) => {
  try {
    let rooms = [];
    if (!dbService.isMock) {
      const roomRes = await dbService.query(
        'SELECT r.*, pr.display_name as host_name FROM rooms r JOIN profiles pr ON r.host_id = pr.user_id WHERE r.expires_at > CURRENT_TIMESTAMP OR r.expires_at IS NULL ORDER BY r.active_members_count DESC'
      );
      rooms = roomRes.rows;
    } else {
      rooms = dbService.getMockStore().rooms;
    }
    res.json(rooms);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 2. CREATE A BRAND NEW VYBE ROOM
router.post('/', authMiddleware, async (req, res) => {
  const { title, description, theme, isTemporary, maxMembers } = req.body;

  if (!title) {
    return res.status(400).json({ error: 'Please supply a captivating room title.' });
  }

  try {
    const roomId = `r_${Date.now()}`;
    const expiresAt = isTemporary !== false ? new Date(Date.now() + 12 * 60 * 60 * 1000).toISOString() : null; // 12 hours max

    const newRoom = {
      id: roomId,
      host_id: req.user.id,
      title,
      description: description || 'No description provided yet.',
      theme: theme || 'neon-sunset',
      is_temporary: isTemporary !== false,
      active_members_count: 1,
      max_members: maxMembers || 50,
      expires_at: expiresAt,
      created_at: new Date().toISOString()
    };

    if (!dbService.isMock) {
      await dbService.query(
        'INSERT INTO rooms (id, host_id, title, description, theme, is_temporary, max_members, expires_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)',
        [roomId, req.user.id, title, description, theme, isTemporary !== false, maxMembers || 50, expiresAt]
      );
      await dbService.query(
        'INSERT INTO room_members (room_id, user_id, status_badge) VALUES ($1, $2, $3)',
        [roomId, req.user.id, 'host']
      );
    } else {
      const store = dbService.getMockStore();
      const profile = store.profiles[req.user.id] || {};
      
      const enrichedRoom = {
        ...newRoom,
        host_name: profile.display_name || req.user.username
      };
      store.rooms.push(enrichedRoom);
    }

    res.status(201).json({
      message: 'Vybe Room successfully opened to the wilderness!',
      room: newRoom
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 3. JOIN VYBE ROOM (REST state + dispatch socket triggers)
router.post('/:id/join', authMiddleware, async (req, res) => {
  const roomId = req.params.id;

  try {
    if (!dbService.isMock) {
      const roomCheck = await dbService.query('SELECT id FROM rooms WHERE id = $1', [roomId]);
      if (roomCheck.rows.length === 0) {
        return res.status(404).json({ error: 'Room does not exist or has expired.' });
      }

      await dbService.query(
        'INSERT INTO room_members (room_id, user_id, status_badge) VALUES ($1, $2, $3) ON CONFLICT (room_id, user_id) DO NOTHING',
        [roomId, req.user.id, 'listener']
      );
      await dbService.query('UPDATE rooms SET active_members_count = active_members_count + 1 WHERE id = $1', [roomId]);
    } else {
      const store = dbService.getMockStore();
      const room = store.rooms.find(r => r.id === roomId);
      if (!room) return res.status(404).json({ error: 'Room does not exist.' });
      room.active_members_count += 1;
    }

    // Capture express server socket interface and dispatch
    const io = req.app.get('io');
    if (io) {
      io.to(roomId).emit('room:member_joined', {
        userId: req.user.id,
        username: req.user.username,
        joinedAt: new Date().toISOString()
      });
    }

    res.json({ message: 'Welcome to the Vybe Room!', roomId });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 4. LEAVE VYBE ROOM
router.post('/:id/leave', authMiddleware, async (req, res) => {
  const roomId = req.params.id;

  try {
    if (!dbService.isMock) {
      await dbService.query('DELETE FROM room_members WHERE room_id = $1 AND user_id = $2', [roomId, req.user.id]);
      await dbService.query('UPDATE rooms SET active_members_count = GREATEST(0, active_members_count - 1) WHERE id = $1', [roomId]);
    } else {
      const store = dbService.getMockStore();
      const room = store.rooms.find(r => r.id === roomId);
      if (room) {
        room.active_members_count = Math.max(0, room.active_members_count - 1);
      }
    }

    const io = req.app.get('io');
    if (io) {
      io.to(roomId).emit('room:member_left', {
        userId: req.user.id,
        username: req.user.username,
        leftAt: new Date().toISOString()
      });
    }

    res.json({ message: 'Safely exited the Vybe Room.', roomId });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
