package com.tamvagbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tamva.risk")
public class RiskRulesProperties {

    private String rulesetVersion = "rules-2026.09.1";

    private Rule newDevice = new Rule("R001", 30, 24.0);
    private Rule newBeneficiary = new Rule("R002", 25, 24.0);

    private UnusualAmount unusualAmount =
            new UnusualAmount("R003", 25, 3.0, 1000.0, 5000.0, 20);

    private Velocity velocity =
            new Velocity("R004", 30, 4, 1.0);

    private AccountDrain accountDrain =
            new AccountDrain("R005", 20, 8000.0, 30);

    private Rule weakAuthentication =
            new Rule("R006", 15, 0.0);

    private DecisionPolicy decisionPolicy =
            new DecisionPolicy(40, 70, 90);

    public String getRulesetVersion() {
        return rulesetVersion;
    }

    public void setRulesetVersion(String rulesetVersion) {
        this.rulesetVersion = rulesetVersion;
    }

    public Rule getNewDevice() {
        return newDevice;
    }

    public void setNewDevice(Rule newDevice) {
        this.newDevice = newDevice;
    }

    public Rule getNewBeneficiary() {
        return newBeneficiary;
    }

    public void setNewBeneficiary(Rule newBeneficiary) {
        this.newBeneficiary = newBeneficiary;
    }

    public UnusualAmount getUnusualAmount() {
        return unusualAmount;
    }

    public void setUnusualAmount(UnusualAmount unusualAmount) {
        this.unusualAmount = unusualAmount;
    }

    public Velocity getVelocity() {
        return velocity;
    }

    public void setVelocity(Velocity velocity) {
        this.velocity = velocity;
    }

    public AccountDrain getAccountDrain() {
        return accountDrain;
    }

    public void setAccountDrain(AccountDrain accountDrain) {
        this.accountDrain = accountDrain;
    }

    public Rule getWeakAuthentication() {
        return weakAuthentication;
    }

    public void setWeakAuthentication(Rule weakAuthentication) {
        this.weakAuthentication = weakAuthentication;
    }

    public DecisionPolicy getDecisionPolicy() {
        return decisionPolicy;
    }

    public void setDecisionPolicy(DecisionPolicy decisionPolicy) {
        this.decisionPolicy = decisionPolicy;
    }

    public static class Rule {

        private String code;
        private int score;
        private double windowHours;

        public Rule() {
        }

        public Rule(String code, int score, double windowHours) {
            this.code = code;
            this.score = score;
            this.windowHours = windowHours;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public double getWindowHours() {
            return windowHours;
        }

        public void setWindowHours(double windowHours) {
            this.windowHours = windowHours;
        }
    }

    public static class UnusualAmount {

        private String code;
        private int score;
        private double multiplier;
        private double minimumAmount;
        private double newCustomerThreshold;
        private int newCustomerScore;

        public UnusualAmount() {
        }

        public UnusualAmount(
                String code,
                int score,
                double multiplier,
                double minimumAmount,
                double newCustomerThreshold,
                int newCustomerScore
        ) {
            this.code = code;
            this.score = score;
            this.multiplier = multiplier;
            this.minimumAmount = minimumAmount;
            this.newCustomerThreshold = newCustomerThreshold;
            this.newCustomerScore = newCustomerScore;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }

        public double getMinimumAmount() {
            return minimumAmount;
        }

        public void setMinimumAmount(double minimumAmount) {
            this.minimumAmount = minimumAmount;
        }

        public double getNewCustomerThreshold() {
            return newCustomerThreshold;
        }

        public void setNewCustomerThreshold(double newCustomerThreshold) {
            this.newCustomerThreshold = newCustomerThreshold;
        }

        public int getNewCustomerScore() {
            return newCustomerScore;
        }

        public void setNewCustomerScore(int newCustomerScore) {
            this.newCustomerScore = newCustomerScore;
        }
    }

    public static class Velocity {

        private String code;
        private int score;
        private int transactionCountThreshold;
        private double windowHours;

        public Velocity() {
        }

        public Velocity(
                String code,
                int score,
                int transactionCountThreshold,
                double windowHours
        ) {
            this.code = code;
            this.score = score;
            this.transactionCountThreshold = transactionCountThreshold;
            this.windowHours = windowHours;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public int getTransactionCountThreshold() {
            return transactionCountThreshold;
        }

        public void setTransactionCountThreshold(int transactionCountThreshold) {
            this.transactionCountThreshold = transactionCountThreshold;
        }

        public double getWindowHours() {
            return windowHours;
        }

        public void setWindowHours(double windowHours) {
            this.windowHours = windowHours;
        }
    }

    public static class AccountDrain {

        private String code;
        private int score;
        private double amountThreshold;
        private int minimumRiskScore;

        public AccountDrain() {
        }

        public AccountDrain(
                String code,
                int score,
                double amountThreshold,
                int minimumRiskScore
        ) {
            this.code = code;
            this.score = score;
            this.amountThreshold = amountThreshold;
            this.minimumRiskScore = minimumRiskScore;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public double getAmountThreshold() {
            return amountThreshold;
        }

        public void setAmountThreshold(double amountThreshold) {
            this.amountThreshold = amountThreshold;
        }

        public int getMinimumRiskScore() {
            return minimumRiskScore;
        }

        public void setMinimumRiskScore(int minimumRiskScore) {
            this.minimumRiskScore = minimumRiskScore;
        }
    }

    public static class DecisionPolicy {

        private int challengeThreshold;
        private int holdThreshold;
        private int blockThreshold;

        public DecisionPolicy() {
        }

        public DecisionPolicy(
                int challengeThreshold,
                int holdThreshold,
                int blockThreshold
        ) {
            this.challengeThreshold = challengeThreshold;
            this.holdThreshold = holdThreshold;
            this.blockThreshold = blockThreshold;
        }

        public int getChallengeThreshold() {
            return challengeThreshold;
        }

        public void setChallengeThreshold(int challengeThreshold) {
            this.challengeThreshold = challengeThreshold;
        }

        public int getHoldThreshold() {
            return holdThreshold;
        }

        public void setHoldThreshold(int holdThreshold) {
            this.holdThreshold = holdThreshold;
        }

        public int getBlockThreshold() {
            return blockThreshold;
        }

        public void setBlockThreshold(int blockThreshold) {
            this.blockThreshold = blockThreshold;
        }
    }
}