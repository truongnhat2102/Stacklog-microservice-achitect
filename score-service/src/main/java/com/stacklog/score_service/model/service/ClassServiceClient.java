package com.stacklog.score_service.model.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@FeignClient(name = "class-service", url = "http://classservice:2003", path = "")
public interface ClassServiceClient {

    @GetMapping("/groupstudent/find/{groupId}")
    List<GroupStudent> getGroupStudent(@RequestHeader("Authorization") String token,
            @PathVariable(name = "groupId") String groupId);

}

@JsonIgnoreProperties(ignoreUnknown = true)
class Groupss {
    private String groupsId; // đổi tên khớp với JSON trả về

    public String getGroupsId() {
        return groupsId;
    }

    public void setGroupsId(String groupsId) {
        this.groupsId = groupsId;
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class GroupStudent {
    private String groupStudentId; // đổi tên khớp với JSON trả về

    private String userId;

    private String groupsId;

    public String getGroupsId() {
        return groupsId;
    }

    public void setGroupsId(String groupsId) {
        this.groupsId = groupsId;
    }

    public String getGroupStudentId() {
        return groupStudentId;
    }

    public void setGroupStudentId(String groupStudentId) {
        this.groupStudentId = groupStudentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
