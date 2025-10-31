const express = require('express');
const { createUser, updateUser, deleteUser, findByEmail, findByGroupId, findByRole, findById, findByNameOrEmail } = require('../controller/userController');
const { validateUser } = require('../middleware/validateUserMiddleware');

const router = express.Router();

// Create User (with validation middleware)
router.post('/', createUser);

// get user by id
router.get('/:userId', findById);

// Update User
router.put('/:id', updateUser);

// Soft Delete User
router.delete('/:id', deleteUser);

// find by email
router.get('/email/:email', findByEmail);

// find by name or email
router.get('/find/:searchWord', findByNameOrEmail);

// list by role
router.get('/role/:role', findByRole);

// find by groupId
router.get("/group/:groupId", findByGroupId);

module.exports = router;
