package com.example.quips.analytics.controller;

import com.example.quips.analytics.dto.UserActivityDTO;
import com.example.quips.analytics.dto.UserReferralDTO;
import com.example.quips.analytics.service.*;
import com.example.quips.transaction.dto.UserTransactionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private CycleAnalyticsService cycleAnalyticsService;
    @Autowired
    private TokenAnalyticsService tokenAnalyticsService;
    @Autowired
    private TransactionGraphService transactionGraphService;
    @Autowired
    private TopUsersTransactionService topUsersTransactionService;
    @Autowired
    private ReferralRewardsService referralRewardsService;
    @Autowired
    private PhaseTimelineService phaseTimelineService;
    @Autowired
    private TransactionActivityService transactionActivityService;

    @Autowired
    private UserActivityService userActivityService;

    @GetMapping("/cycle-status")
    public ResponseEntity<?> getCycleStatus() {
        return ResponseEntity.ok(cycleAnalyticsService.getCycleStatus());
    }

    @GetMapping("/token-status")
    public ResponseEntity<?> getTokenStatus() {
        return ResponseEntity.ok(tokenAnalyticsService.getTokenStatus());
    }

    @GetMapping("/transaction-graph")
    public ResponseEntity<?> getTransactionGraph() {
        return ResponseEntity.ok(transactionGraphService.getTransactionGraph());
    }

    @GetMapping("/user-activity/{userId}")
    public ResponseEntity<UserActivityDTO> getUserActivity(@PathVariable Long userId) {
        UserActivityDTO activity = userActivityService.calculateUserActivity(userId);
        return ResponseEntity.ok(activity);
    }

    @GetMapping("/user-activity")
    public ResponseEntity<List<UserActivityDTO>> getAllUsersActivity() {
        List<UserActivityDTO> activities = userActivityService.calculateAllUsersActivity();
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/top-users-transactions")
    public ResponseEntity<?> getTopUsersByTransactions() {
        return ResponseEntity.ok(topUsersTransactionService.getTopUsersByTransactions());
    }

    // Nuevo endpoint para obtener los usuarios que más han enviado transacciones
    @GetMapping("/top-senders")
    public ResponseEntity<List<UserTransactionDTO>> getTopSenders() {
        return ResponseEntity.ok(topUsersTransactionService.getTopSenders());
    }

    @GetMapping("/top-receivers")
    public ResponseEntity<List<UserTransactionDTO>> getTopReceivers() {
        return ResponseEntity.ok(topUsersTransactionService.getTopReceivers());
    }
    @GetMapping("/top-users-referrals")
    public ResponseEntity<List<UserReferralDTO>> getTopUsersByReferrals() {
        List<UserReferralDTO> topUsers = referralRewardsService.getTopUsersByReferrals();
        return ResponseEntity.ok(topUsers);
    }

    @GetMapping("/phase-timeline")
    public ResponseEntity<?> getPhaseTimeline() {
        return ResponseEntity.ok(phaseTimelineService.getPhaseTimeline());
    }

    @GetMapping("/transaction-activity")
    public ResponseEntity<?> getTransactionActivityByHour() {
        return ResponseEntity.ok(transactionActivityService.getTransactionActivityByHour());
    }
}
