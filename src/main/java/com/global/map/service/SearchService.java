
package com.global.map.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchClient elasticsearchClient;

    public List<Map<String, Object>> searchItems(String keyword) {
        try {
            String[] keywords = keyword.split(" ");

            List<Set<String>> resultIdSets = new ArrayList<>();

            for (String k : keywords) {
                var searchResponse = elasticsearchClient.search(s -> {
                    var searchBuilder = s.index("item").size(100000);

                    searchBuilder = searchBuilder.query(q -> q.multiMatch(m -> m
                        .query(k)
                        .fields("NAME", "ADDRESS", "CATEGORY_NAME", "DEPT_NAME")
                        .operator(Operator.And)
                    ));

                    return searchBuilder;
                }, Map.class);

                Set<String> ids = searchResponse.hits().hits().stream()
                        .map(hit -> hit.id())
                        .collect(Collectors.toSet());

                resultIdSets.add(ids);
            }

            // 모든 결과의 교집합 구하기
            Set<String> finalIds = resultIdSets.get(0);
            for (int i = 1; i < resultIdSets.size(); i++) {
                finalIds.retainAll(resultIdSets.get(i));
            }

            // 교집합에 속하는 문서들 다시 조회
            if (finalIds.isEmpty()) {
                return List.of();
            }

            var finalSearchResponse = elasticsearchClient.search(s -> s
                    .index("item")
                    .size(100000)
                    .query(q -> q.bool(b -> b
                        .must(m -> m.ids(v -> v.values(new ArrayList<>(finalIds))))
                        .mustNot(mn -> mn.term(t -> t.field("CATEGORY_NAME").value("의원")))
                        .mustNot(mn -> mn.term(t -> t.field("CATEGORY_NAME").value("치과의원")))
                    )),
                    Map.class
            );

            return finalSearchResponse.hits().hits().stream()
                    .map(hit -> (Map<String, Object>) hit.source())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("검색 실패", e);
        }
    }
}
