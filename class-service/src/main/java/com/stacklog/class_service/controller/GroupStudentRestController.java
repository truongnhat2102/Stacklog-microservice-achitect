package com.stacklog.class_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stacklog.class_service.model.entities.GroupStudent;
import com.stacklog.class_service.model.service.GroupService;
import com.stacklog.class_service.model.service.GroupsStudentService;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping(path = "/groupstudent")
public class GroupStudentRestController {

    @Autowired
    GroupsStudentService groupsStudentService;

    @Autowired
    GroupService groupService;

    @GetMapping("")
    public ResponseEntity<List<GroupStudent>> getGroupStudentByUserId(@RequestHeader("Authorization") String token) {
        List<GroupStudent> groupStudents = groupsStudentService.getAllByUserId(token);
        return ResponseEntity.ok().body(groupStudents);
    }

    @GetMapping("/find/{groupId}")
    public ResponseEntity<List<GroupStudent>> getGroupStudentByGroupId(@RequestHeader("Authorization") String token,
            @PathVariable("groupId") String groupId) {
        List<GroupStudent> groupStudents = groupsStudentService.getByGroupIdNhat(groupId, token);
        return ResponseEntity.ok().body(groupStudents);
    }
    

    @PostMapping("")
    public ResponseEntity<GroupStudent> saveGroupStudent(@RequestHeader("Authorization") String token,
            @RequestBody GroupStudent groupStudent) {
        GroupStudent newGroupStudent = groupsStudentService.save(groupStudent, token);

        return ResponseEntity.ok().body(newGroupStudent);
    }

    @DeleteMapping("")
    public ResponseEntity<GroupStudent> deleteGroupStudent(@RequestHeader("Authorization") String token,
            @RequestParam(name = "groupId") String groupId) {
        groupsStudentService.delete(groupId, token);
        return ResponseEntity.ok().body(null);
    }

    @PutMapping("/kick/{studentId}")
    public ResponseEntity<GroupStudent> kickGroup(@RequestHeader("Authorization") String token,
            @RequestBody OldGroupDTO oldGroupDTO,
            @PathVariable(name = "studentId", required = false) String studentId) {
        GroupStudent gs = new GroupStudent();
        gs = groupsStudentService.getByGroupIdAndStudentId(oldGroupDTO.getOldGroupId(), studentId);
        gs.setGroups(groupService.getById(oldGroupDTO.getUnassignedGroupId(), token));
        groupsStudentService.save(gs, token);
        return ResponseEntity.ok().body(null);

    }

    @PutMapping("/leave")
    public ResponseEntity<GroupStudent> leaveGroup(@RequestHeader("Authorization") String token,
            @RequestBody OldGroupDTO oldGroupDTO) {
        GroupStudent gs = new GroupStudent();
        gs = groupsStudentService.getByGroupId(oldGroupDTO.getOldGroupId(), token);
        gs.setGroups(groupService.getById(oldGroupDTO.getUnassignedGroupId(), token));
        groupsStudentService.save(gs, token);
        return ResponseEntity.ok().body(null);

    }
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class OldGroupDTO {
    private String classId;
    private String oldGroupId;
    private String unassignedGroupId;
}
