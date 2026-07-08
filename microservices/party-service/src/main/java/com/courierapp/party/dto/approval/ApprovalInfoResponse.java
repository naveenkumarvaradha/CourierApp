package com.courierapp.party.dto.approval;

import java.util.List;

public record ApprovalInfoResponse(int currentLevel, int maxLevel, List<String> approvers, String summary) {}
