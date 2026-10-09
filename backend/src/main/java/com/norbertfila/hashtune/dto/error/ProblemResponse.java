package com.norbertfila.hashtune.dto.error;

import java.util.Map;

public record ProblemResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        Map<String, String> errors) {}
