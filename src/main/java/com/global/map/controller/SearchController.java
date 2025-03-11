package com.global.map.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseBody;

import com.global.map.service.SearchService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/map/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/item")
    @ResponseBody
    public List<Map<String, Object>> searchHospitals(
            @RequestParam("keyword") String keyword) {

        return searchService.searchItems(keyword);
    }
}
