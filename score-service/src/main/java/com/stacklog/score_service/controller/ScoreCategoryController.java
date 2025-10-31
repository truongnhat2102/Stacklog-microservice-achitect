package com.stacklog.score_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stacklog.score_service.model.entities.ScoreCategory;
import com.stacklog.score_service.model.service.ScoreCategoryService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/category")
public class ScoreCategoryController {

    @Autowired
    private ScoreCategoryService scoreCategoryService;

    @GetMapping("/class/{classId}")
    public ResponseEntity<List<ScoreCategory>> getByClassId(@RequestHeader("Authorization") String token,
            @PathVariable(name = "classId") String classId) {
        List<ScoreCategory> list = scoreCategoryService.getAllByClassId(classId, token);
        if (list.isEmpty() || list == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/isreuse")
    public ResponseEntity<List<ScoreCategory>> getByReused(@RequestHeader("Authorization") String token) {
        List<ScoreCategory> list = scoreCategoryService.getAllByReused(token);
        if (list.isEmpty() || list == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping("/save")
    public ResponseEntity<ScoreCategory> saveScoreCategory(@RequestHeader("Authorization") String token,
            @RequestBody ScoreCategory e) {
        ScoreCategory newScoreCategory = scoreCategoryService.save(e, token);
        if (newScoreCategory == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(newScoreCategory);
    }

    @DeleteMapping("/delete/{scId}")
    public ResponseEntity<ScoreCategory> deleteScoreCategory(@RequestHeader("Authorization") String token,
            @PathVariable(name = "scId") String scId) {

        ScoreCategory sc = scoreCategoryService.delete(scId, token);
        if (sc == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(sc);

    }

}
