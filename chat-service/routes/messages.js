const router = require('express').Router();
const ctrl = require('../controllers/messages.controller');
const auth = require('../middleware/auth');

router.post('/:boxId', auth, ctrl.send);
router.get('/:boxId', auth, ctrl.list);
router.put('/recall/:messageId', auth, ctrl.recall);
router.delete('/:messageId', auth, ctrl.remove);
// router.post('/read/:boxId', auth, ctrl.readReset);
router.post('/read/:boxId', auth, ctrl.markAsRead);

module.exports = router;
