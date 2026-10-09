package com.norbertfila.hashtune.dto.response;

public record ReindexAllResponse(int scheduled, int alreadyProcessing, int awaitingConfirmation) {}
