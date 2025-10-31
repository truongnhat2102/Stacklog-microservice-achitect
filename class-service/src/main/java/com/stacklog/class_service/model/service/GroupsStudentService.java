package com.stacklog.class_service.model.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.stacklog.class_service.model.entities.Classes;
import com.stacklog.class_service.model.entities.GroupStudent;
import com.stacklog.class_service.model.entities.Groupss;
import com.stacklog.class_service.model.repo.ClassesRepo;
import com.stacklog.class_service.model.repo.GroupsStudentRepo;
import com.stacklog.core_service.model.service.IService;
import com.stacklog.core_service.utils.CommonFunction;
import com.stacklog.core_service.utils.kafka.KafkaProducer;
import com.stacklog.core_service.utils.redis.RedisService;

import jakarta.transaction.Transactional;

@Service
public class GroupsStudentService implements IService<GroupStudent> {

    private static final String NAME_SERVICE = "class-service";

    private static final String KAFKA_TOPIC_UPDATE = "class-service.groupstudent.updated";
    private static final String KAFKA_TOPIC_CREATE = "class-service.groupstudent.created";

    @Autowired
    GroupsStudentRepo groupsStudentRepo;

    @Autowired
    ClassesRepo classesRepo;

    @Autowired
    KafkaProducer<GroupStudent> kafkaGroupStudentProducer;

    // @Autowired SimpMessagingTemplate messagingTemplate;

    RedisService<GroupStudent> redisGroupStudentService;

    public GroupsStudentService(RedisService<GroupStudent> redisGroupStudentService) {
        this.redisGroupStudentService = redisGroupStudentService;
    }

    @Override
    public GroupStudent delete(String groupId, String token) {
        GroupStudent gs = getByGroupId(groupId, token);
        if (gs != null) {
            groupsStudentRepo.delete(gs);
        }

        return gs;
    }

    @Override
    public List<GroupStudent> getAllByUserId(String token) {
        List<GroupStudent> groupStudents = redisGroupStudentService.getAll(token, NAME_SERVICE);
        if (groupStudents.isEmpty()) {
            groupStudents = groupsStudentRepo.findByUserId(redisGroupStudentService.getCurrentUserId(token));
            redisGroupStudentService.saveListToRedis(groupStudents, token, NAME_SERVICE);
        }

        return groupStudents;
    }

    public GroupStudent getByGroupId(String groupId, String token) {
        String userId = redisGroupStudentService.getCurrentUserId(token);
        return groupsStudentRepo.findByGroupsGroupsIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException(
                        "GroupStudent not found for groupId=" + groupId + " and userId=" + userId));
    }

    public List<GroupStudent> getByGroupIdNhat(String groupId, String token) {
        return groupsStudentRepo.findAllByGroupGroupId(groupId);
    }

    @Override
    public GroupStudent getById(String id, String token) {
        GroupStudent groupStudent = redisGroupStudentService.getById(id, token, NAME_SERVICE);
        if (groupStudent == null) {
            groupStudent = groupsStudentRepo.findById(id).orElseThrow();
            redisGroupStudentService.saveToRedis(groupStudent, token, NAME_SERVICE);
        }
        return groupStudent;
    }

    @Override
    public GroupStudent save(GroupStudent e, String token) {
        boolean isCreate = (e.getGroupStudentId() == null || !groupsStudentRepo.existsById(e.getGroupStudentId()));
        GroupStudent newGroupStudent = saveToDB(e, token);
        if (newGroupStudent == null) {
            return null;
        }

        if (isCreate) {
            kafkaGroupStudentProducer.sendMessage(newGroupStudent, KAFKA_TOPIC_CREATE);
        } else {
            kafkaGroupStudentProducer.sendMessage(newGroupStudent, KAFKA_TOPIC_UPDATE);
        }

        redisGroupStudentService.saveToRedis(newGroupStudent, token, NAME_SERVICE);

        // messagingTemplate.convertAndSend("topic/class-service");

        return newGroupStudent;
    }

    @Transactional
    private GroupStudent saveToDB(GroupStudent e, String token) {
        e.setUpdateAt(CommonFunction.getCurrentTime());
        e.setUpdateBy(redisGroupStudentService.getCurrentUserId(token));
        if (e.getGroupStudentId() == null) {
            e.setCreatedAt(CommonFunction.getCurrentTime());
            e.setCreatedBy(redisGroupStudentService.getCurrentUserId(token));
            e.setGroupStudentId(UUID.randomUUID().toString());
        }
        return groupsStudentRepo.save(e);
    }

    public GroupStudent joinClass(String classId, String token) {
        Classes classes = classesRepo.findById(classId).orElse(null);
        Groupss groupss = classes.getGroups().stream().filter(g -> g.getGroupsName().equals("unassigned")).findFirst()
                .get();
        GroupStudent groupStudent = new GroupStudent();
        groupStudent.setGroups(groupss);
        groupStudent.setUserId(redisGroupStudentService.getCurrentUserId(token));

        return save(groupStudent, token);
    }

    public GroupStudent getByGroupIdAndStudentId(String oldGroupId, String studentId) {
        return groupsStudentRepo.findByGroupsGroupsIdAndUserId(oldGroupId, studentId)
                .orElseThrow(() -> new RuntimeException(
                        "GroupStudent not found for groupId=" + oldGroupId + " and userId=" + studentId));
    }

    public Boolean checkExistGroupStudent(Classes classes, String token) {
        return classes.getGroups().stream().anyMatch(g -> g.getGroupStudents().stream()
                .anyMatch(gs -> gs.getUserId().equals(redisGroupStudentService.getCurrentUserId(token))));
    }

    

}
