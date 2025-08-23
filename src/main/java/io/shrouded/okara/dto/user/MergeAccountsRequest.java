package io.shrouded.okara.dto.user;

public record MergeAccountsRequest(
    String anonymousUserToken,
    String newUserToken
) {}
