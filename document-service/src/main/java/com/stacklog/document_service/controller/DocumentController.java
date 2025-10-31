package com.stacklog.document_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stacklog.document_service.model.entities.Document;
import com.stacklog.document_service.model.service.DocumentService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping(path = "")
public class DocumentController {

    @Autowired
    DocumentService documentService;

    @GetMapping(value = { "", "/" })
    public ResponseEntity<List<Document>> getDocument(@RequestHeader("Authorization") String token) {
        List<Document> lists = documentService.getAllByUserId(token);

        if (lists.isEmpty() || lists == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().body(lists);
    }

    @GetMapping("/groups/{gId}")
    public ResponseEntity<List<Document>> getDocumentByGroupId(@RequestHeader("Authorization") String token,
            @PathVariable(name = "gId", required = false) String gId) {
        List<Document> lists = documentService.getByGroupId(gId, token);
        if (lists.isEmpty() || lists == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().body(lists);
    }

    @PostMapping("/save")
    public ResponseEntity<Document> postMethodName(@RequestHeader("Authorization") String token,
            @RequestBody Document e) {
        Document document = documentService.save(e, token);
        if (document == null) {
            return ResponseEntity.badRequest().body(null);
        }
        return ResponseEntity.ok().body(document);
    }

    @DeleteMapping("/delete/{dId}")
    public ResponseEntity<String> removeDocument(@RequestHeader("Authorization") String token,
            @PathVariable(name = "dId", required = false) String dId) {
        Document document = documentService.delete(dId, token);
        if (document == null) {
            return ResponseEntity.badRequest().body("Delete failed");
        }
        return ResponseEntity.ok().body("Delete success");
    }

}
