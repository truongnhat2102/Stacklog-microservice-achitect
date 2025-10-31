const router = require('express').Router();
const ctrl = require('../controllers/boxes.controller');
const auth = require('../middleware/auth');

router.post('/', auth, ctrl.create);
router.post('/:boxId/members', auth, ctrl.addMembers);
router.delete('/:boxId/delete/:memberId', auth, ctrl.deleteMember);
router.get('/', auth, ctrl.listByUser);
router.delete('/:boxId', auth, ctrl.delete);

module.exports = router;
